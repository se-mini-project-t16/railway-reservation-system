import { mountShell } from '../core/shell.js';
import { api } from '../core/api.js';
import { requireSignIn } from '../core/session.js';
import { $, h, money, fmtDate, params, notice, setBusy, statusChip, setTitle } from '../core/ui.js';

const pnr = (params().get('pnr') || '').replace(/\D/g, '');
const root = $('#pay-root');
let timer;

function summary(b) {
  return h('section', { class: 'panel', 'aria-labelledby': 'sum-title' },
    h('h2', { id: 'sum-title' }, 'Your held booking'),
    h('dl', { class: 'facts' },
      h('div', {}, h('dt', {}, 'PNR'), h('dd', {}, b.pnr)),
      h('div', {}, h('dt', {}, 'Train'), h('dd', {}, `${b.train.number} ${b.train.name}`)),
      h('div', {}, h('dt', {}, 'Journey'), h('dd', {}, `${b.from.code} ${b.from.time} to ${b.to.code} ${b.to.time}`)),
      h('div', {}, h('dt', {}, 'Date'), h('dd', {}, fmtDate(b.from.date))),
      h('div', {}, h('dt', {}, 'Class'), h('dd', {}, `${b.classLabel}, ${b.quotaLabel}`))),
    h('table', { class: 'table', style: null },
      h('thead', {}, h('tr', {}, h('th', {}, 'Traveller'), h('th', {}, 'Held as'))),
      h('tbody', {}, b.passengers.map((p) => h('tr', {}, h('td', {}, `${p.name}, ${p.age}`), h('td', {}, statusChip(p.currentStatus)))))),
    h('table', { class: 'fare-lines' }, h('tbody', {},
      h('tr', { class: 'total' }, h('td', {}, 'Amount to pay'), h('td', { class: 'num' }, money(b.fare.total))))));
}

function closed(b) {
  const why = b.status === 'EXPIRED' ? 'The 10 minute hold ran out before payment, so the berths went back on sale.'
    : 'Payment was not completed and the berths were released.';
  const refunded = (b.payments || []).filter((p) => p.status === 'REFUNDED');
  root.replaceChildren(h('div', { class: 'empty-state' },
    h('h1', { class: 'section-title' }, 'This booking can no longer be paid.'), h('p', {}, why),
    refunded.map((p) => h('p', {}, `Your payment of ${money(p.amount)} (${p.txnRef}) arrived after the hold ended and was refunded in full.`)),
    h('a', { class: 'btn btn--primary', href: `/trains.html?from=${b.from.code}&to=${b.to.code}&date=${b.journeyDate}&quota=${b.quota}` }, 'Search this journey again')));
}

// ------------------------------------------------------------------ Razorpay Checkout

const CHECKOUT_JS = 'https://checkout.razorpay.com/v1/checkout.js';
let checkoutLoading;

function loadCheckout() {
  if (window.Razorpay) return Promise.resolve(window.Razorpay);
  if (!checkoutLoading) {
    checkoutLoading = new Promise((resolve, reject) => {
      const s = document.createElement('script');
      s.src = CHECKOUT_JS;
      s.async = true;
      s.onload = () => (window.Razorpay ? resolve(window.Razorpay) : reject(new Error('Razorpay did not load.')));
      s.onerror = () => { checkoutLoading = null; reject(new Error('Razorpay could not be loaded. Check your connection and try again.')); };
      document.head.append(s);
    });
  }
  return checkoutLoading;
}

function render(b, prepared) {
  const payBtn = h('button', { type: 'submit', class: 'btn btn--primary btn--lg' }, `Pay ${money(b.fare.total)}`);
  const result = h('div', { 'aria-live': 'polite' });
  const testNote = h('div');
  const countdown = h('span', { class: 'countdown', id: 'countdown', role: 'timer', 'aria-live': 'off' }, '');
  const releaseBtn = h('button', { type: 'button', class: 'btn btn--danger' }, 'Release the berths');

  const form = h('form', { class: 'panel stack', novalidate: true, 'aria-labelledby': 'pay-title' },
    h('div', { class: 'row row--between' }, h('h1', { id: 'pay-title', class: 'section-title' }, 'Pay for your booking'),
      h('div', { class: 'small' }, 'Berths held for ', countdown)),
    testNote,
    h('p', {}, 'You pay in Razorpay\'s secure payment window, with whichever of card, net banking, wallet or UPI it offers you. Your card number, UPI PIN and bank password go to Razorpay only; this site never sees them.'),
    h('p', { class: 'small muted' }, 'If you close the window without paying, your berths stay held until the timer runs out.'),
    result,
    h('div', { class: 'row row--between' }, releaseBtn, payBtn));

  root.replaceChildren(h('div', { class: 'two-col' }, form, h('aside', { class: 'aside-sticky' }, summary(b))));

  if (prepared.order?.testMode) {
    testNote.replaceChildren(notice('info', 'Razorpay test mode: no real money moves.',
      'In the payment window choose Netbanking and any bank, then press Success on Razorpay\'s test bank page. Failure shows what a declined payment looks like.'));
  }
  if (prepared.error?.code === 'PAYMENTS_UNAVAILABLE') {
    payBtn.disabled = true;
    result.replaceChildren(notice('error', 'Online payment is unavailable.', prepared.error.message));
  }

  let paying = false;
  const due = new Date(b.paymentDueAt).getTime();
  const tick = () => {
    const left = Math.max(0, Math.round((due - Date.now()) / 1000));
    countdown.textContent = `${String(Math.floor(left / 60)).padStart(2, '0')}:${String(left % 60).padStart(2, '0')}`;
    countdown.classList.toggle('is-low', left <= 60);
    if (left === 0) {
      clearInterval(timer);
      if (paying) return; // a payment in progress is settled by the server, which checks Razorpay before releasing anything
      payBtn.disabled = true;
      result.replaceChildren(notice('error', 'The hold has run out.', 'Your berths are being released. Search again to book.'));
      setTimeout(load, 3000);
    }
  };
  tick();
  timer = setInterval(tick, 1000);

  const done = (outcome) => {
    paying = false;
    setBusy(payBtn, false);
    releaseBtn.disabled = false;
    if (outcome) result.replaceChildren(outcome);
  };

  async function confirmPayment(resp) {
    setBusy(payBtn, true, 'Confirming payment');
    try {
      const res = await api(`/payments/${b.pnr}/confirm`, { method: 'POST', body: {
        orderId: resp.razorpay_order_id, paymentId: resp.razorpay_payment_id, signature: resp.razorpay_signature } });
      if (res.outcome === 'SUCCESS') {
        clearInterval(timer);
        location.assign(`/ticket.html?pnr=${b.pnr}&paid=1`);
        return;
      }
      if (res.outcome === 'REFUNDED') {
        clearInterval(timer);
        done(notice('error', 'Your payment was refunded.', res.message));
        setTimeout(load, 6000);
        return;
      }
      done(notice(res.outcome === 'PENDING' ? 'info' : 'error',
        res.outcome === 'PENDING' ? 'Waiting for Razorpay.' : 'The payment did not go through.', res.message));
    } catch (err) {
      if (err.code === 'HOLD_EXPIRED' || err.code === 'PAYMENT_CLOSED') { load(); return; }
      done(notice('error', 'We could not confirm the payment.', `${err.message} If money left your account, your ticket is booked or the amount refunded automatically within a few minutes.`));
    }
  }

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    if (paying) return;
    paying = true;
    result.replaceChildren();
    setBusy(payBtn, true, 'Opening Razorpay');
    releaseBtn.disabled = true;
    let order = prepared.order;
    let Razorpay;
    try {
      [order, Razorpay] = await Promise.all([order || api(`/payments/${b.pnr}/order`, { method: 'POST' }), loadCheckout()]);
      prepared.order = order;
    } catch (err) {
      if (err.code === 'ALREADY_PAID') { location.replace(`/ticket.html?pnr=${b.pnr}`); return; }
      if (err.code === 'HOLD_EXPIRED' || err.code === 'PAYMENT_CLOSED') { load(); return; }
      done(notice('error', 'Payment could not start.', err.message));
      return;
    }
    let settled = false;
    const rzp = new Razorpay({
      key: order.keyId,
      order_id: order.orderId,
      amount: order.amount,
      currency: order.currency,
      name: order.name,
      description: order.description,
      prefill: order.prefill,
      notes: { pnr: b.pnr },
      theme: { color: '#1f5ed9' },
      // Razorpay closes its window when the hold runs out, so nobody pays for released berths
      timeout: Math.max(60, order.holdSecondsLeft),
      retry: { enabled: true },
      handler: (resp) => { settled = true; confirmPayment(resp); },
      modal: {
        confirm_close: true,
        ondismiss: () => { if (!settled) done(notice('info', 'Payment window closed.', 'Nothing was charged. You can pay again while the berths are held.')); },
      },
    });
    rzp.on('payment.failed', (resp) => {
      const id = resp?.error?.metadata?.payment_id;
      if (id) api(`/payments/${b.pnr}/failure`, { method: 'POST', body: { paymentId: id } }).catch(() => {});
      result.replaceChildren(notice('error', 'That attempt did not go through.',
        `${resp?.error?.description || 'The payment was declined.'} You can try again in the Razorpay window or choose another method.`));
    });
    setBusy(payBtn, true, 'Waiting for payment');
    rzp.open();
  });

  const dialog = h('dialog', { class: 'dialog', 'aria-labelledby': 'rel-title' },
    h('div', { class: 'dialog-head' }, h('h2', { id: 'rel-title' }, 'Release these berths?')),
    h('div', { class: 'dialog-body' }, h('p', {}, `Nothing has been charged. PNR ${b.pnr} will be closed and its berths go back on sale.`)),
    h('div', { class: 'dialog-foot' },
      h('button', { type: 'button', class: 'btn', onclick: () => dialog.close() }, 'Keep them'),
      h('button', { type: 'button', class: 'btn btn--danger', id: 'confirm-release' }, 'Release the berths')));
  document.body.append(dialog);
  releaseBtn.addEventListener('click', () => dialog.showModal());
  $('#confirm-release').addEventListener('click', async (e) => {
    setBusy(e.currentTarget, true, 'Releasing');
    try {
      const after = await api(`/payments/${b.pnr}/abandon`, { method: 'POST' });
      clearInterval(timer);
      dialog.close();
      if (after.status === 'BOOKED') { location.assign(`/ticket.html?pnr=${b.pnr}&paid=1`); return; }
      load();
    } catch (err) {
      setBusy(e.currentTarget, false);
      dialog.close();
      result.replaceChildren(notice('error', 'Could not release the berths.', err.message));
    }
  });
}

async function load() {
  clearInterval(timer);
  if (pnr.length !== 10) {
    root.replaceChildren(h('div', { class: 'empty-state' }, h('h1', { class: 'section-title' }, 'No booking to pay for.'), h('a', { class: 'btn', href: '/bookings.html' }, 'My trips')));
    return;
  }
  try {
    // Also picks up a payment made earlier whose result never reached us (for example, a closed tab)
    const b = await api(`/payments/${pnr}/sync`, { method: 'POST' });
    setTitle(`Pay for PNR ${b.pnr}`);
    if (b.status === 'BOOKED' || b.status === 'PARTIALLY_CANCELLED' || b.status === 'CANCELLED') { location.replace(`/ticket.html?pnr=${b.pnr}`); return; }
    if (b.status !== 'PAYMENT_PENDING') { closed(b); return; }
    // The Razorpay order is prepared now, so the button opens Checkout at once
    const prepared = {};
    try { prepared.order = await api(`/payments/${b.pnr}/order`, { method: 'POST' }); } catch (err) {
      if (err.code === 'ALREADY_PAID') { location.replace(`/ticket.html?pnr=${b.pnr}`); return; }
      if (err.code === 'HOLD_EXPIRED' || err.code === 'PAYMENT_CLOSED') { closed({ ...b, status: 'EXPIRED' }); return; }
      prepared.error = err;
    }
    document.querySelectorAll('dialog').forEach((d) => d.remove());
    render(b, prepared);
  } catch (err) {
    root.replaceChildren(notice('error', 'We could not load this booking.', err.message, h('button', { type: 'button', class: 'btn', onclick: load }, 'Try again')));
  }
}

if (requireSignIn()) {
  mountShell();
  load();
}
