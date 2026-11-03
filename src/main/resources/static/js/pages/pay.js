import { mountShell } from '../core/shell.js';
import { api } from '../core/api.js';
import { requireSignIn } from '../core/session.js';
import { $, h, money, fmtDate, params, notice, setBusy, setFieldError, clearFieldErrors, statusChip, tabs, setTitle } from '../core/ui.js';

const pnr = (params().get('pnr') || '').replace(/\D/g, '');
const root = $('#pay-root');
let booking;
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
  root.replaceChildren(h('div', { class: 'empty-state' },
    h('h1', { class: 'section-title' }, 'This booking can no longer be paid.'), h('p', {}, why),
    h('a', { class: 'btn btn--primary', href: `/trains.html?from=${b.from.code}&to=${b.to.code}&date=${b.journeyDate}&quota=${b.quota}` }, 'Search this journey again')));
}

function field(id, labelText, attrs = {}, hint = '') {
  return h('div', { class: 'field' }, h('label', { for: id }, labelText),
    h('input', { class: 'input input--lg', id, 'aria-describedby': `${id}-help`, ...attrs }),
    h('span', { class: 'help', id: `${id}-help`, 'data-hint': hint }, hint));
}

function brand(n) {
  if (/^4/.test(n)) return 'Visa';
  if (/^(5[1-5]|2[2-7])/.test(n)) return 'Mastercard';
  if (/^(60|65|81|82|508)/.test(n)) return 'RuPay';
  if (/^3[47]/.test(n)) return 'American Express';
  return '';
}

function render(b, banks) {
  const tablist = h('div', { class: 'tabs', role: 'tablist', 'aria-label': 'Payment method' },
    h('button', { type: 'button', role: 'tab', id: 'tab-upi', 'aria-selected': 'true', 'aria-controls': 'p-upi' }, 'UPI'),
    h('button', { type: 'button', role: 'tab', id: 'tab-card', 'aria-selected': 'false', 'aria-controls': 'p-card', tabindex: '-1' }, 'Card'),
    h('button', { type: 'button', role: 'tab', id: 'tab-nb', 'aria-selected': 'false', 'aria-controls': 'p-nb', tabindex: '-1' }, 'Net banking'));

  const upi = h('div', { role: 'tabpanel', id: 'p-upi', 'aria-labelledby': 'tab-upi', class: 'stack' },
    field('upi', 'UPI ID', { autocomplete: 'off', placeholder: 'name@bank', inputmode: 'email' }, 'The request appears in your UPI app.'));
  const cardNo = h('input', { class: 'input input--lg', id: 'card-no', inputmode: 'numeric', autocomplete: 'cc-number', maxlength: 23, 'aria-describedby': 'card-no-help' });
  const cardHelp = h('span', { class: 'help', id: 'card-no-help', 'data-hint': '' }, '');
  cardNo.addEventListener('input', () => {
    const digits = cardNo.value.replace(/\D/g, '').slice(0, 19);
    cardNo.value = digits.replace(/(\d{4})(?=\d)/g, '$1 ');
    cardHelp.dataset.hint = brand(digits);
    if (!cardHelp.classList.contains('is-error')) cardHelp.textContent = brand(digits);
  });
  const expiry = h('input', { class: 'input input--lg', id: 'card-exp', inputmode: 'numeric', autocomplete: 'cc-exp', placeholder: 'MM/YY', maxlength: 5, 'aria-describedby': 'card-exp-help' });
  expiry.addEventListener('input', () => {
    const d = expiry.value.replace(/\D/g, '').slice(0, 4);
    expiry.value = d.length > 2 ? `${d.slice(0, 2)}/${d.slice(2)}` : d;
  });
  const card = h('div', { role: 'tabpanel', id: 'p-card', 'aria-labelledby': 'tab-card', class: 'stack', hidden: true },
    h('div', { class: 'field' }, h('label', { for: 'card-no' }, 'Card number'), cardNo, cardHelp),
    h('div', { class: 'two-up' },
      h('div', { class: 'field' }, h('label', { for: 'card-exp' }, 'Expiry'), expiry, h('span', { class: 'help', id: 'card-exp-help' }, '')),
      field('card-cvv', 'Security code', { inputmode: 'numeric', autocomplete: 'cc-csc', maxlength: 4, type: 'password' })),
    field('card-name', 'Name on card', { autocomplete: 'cc-name' }));
  const bankList = h('fieldset', { class: 'seg bank-grid' }, h('legend', { class: 'visually-hidden' }, 'Bank'),
    Object.entries(banks).map(([code, name], i) => [
      h('input', { type: 'radio', name: 'bank', id: `bank-${code}`, value: code, checked: i === 0 }),
      h('label', { for: `bank-${code}` }, name)]));
  const nb = h('div', { role: 'tabpanel', id: 'p-nb', 'aria-labelledby': 'tab-nb', hidden: true }, bankList,
    h('p', { class: 'small muted', style: null }, 'You would normally be sent to your bank to sign in.'));

  let method = 'UPI';
  const payBtn = h('button', { type: 'submit', class: 'btn btn--primary btn--lg' }, `Pay ${money(b.fare.total)}`);
  const result = h('div');
  const countdown = h('span', { class: 'countdown', id: 'countdown', role: 'timer', 'aria-live': 'off' }, '');
  const releaseBtn = h('button', { type: 'button', class: 'btn btn--danger' }, 'Release the berths');

  const form = h('form', { class: 'panel', novalidate: true, 'aria-labelledby': 'pay-title' },
    h('div', { class: 'row row--between' }, h('h1', { id: 'pay-title', class: 'section-title' }, 'Pay for your booking'),
      h('div', { class: 'small' }, 'Berths held for ', countdown)),
    h('p', { class: 'notice notice--info small' }, 'This is a simulated payment. No money moves, and card numbers are never stored: only the last four digits are kept on the receipt.'),
    h('div', { class: 'pay-methods' }, tablist, upi, card, nb),
    result,
    h('div', { class: 'row row--between' }, releaseBtn, payBtn));

  root.replaceChildren(h('div', { class: 'two-col' }, form, h('aside', { class: 'aside-sticky' }, summary(b))));
  tabs(tablist, (tab) => { method = { 'tab-upi': 'UPI', 'tab-card': 'CARD', 'tab-nb': 'NETBANKING' }[tab.id]; clearFieldErrors(form); });

  const due = new Date(b.paymentDueAt).getTime();
  const tick = () => {
    const left = Math.max(0, Math.round((due - Date.now()) / 1000));
    countdown.textContent = `${String(Math.floor(left / 60)).padStart(2, '0')}:${String(left % 60).padStart(2, '0')}`;
    countdown.classList.toggle('is-low', left <= 60);
    if (left === 0) {
      clearInterval(timer);
      payBtn.disabled = true;
      result.replaceChildren(notice('error', 'The hold has run out.', 'Your berths are being released. Search again to book.'));
      setTimeout(load, 3000);
    }
  };
  tick();
  timer = setInterval(tick, 1000);

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    clearFieldErrors(form);
    result.replaceChildren();
    const body = { method };
    const bad = [];
    if (method === 'UPI') {
      body.upiId = $('#upi').value.trim();
      if (!/^[A-Za-z0-9._-]{2,64}@[A-Za-z]{2,32}$/.test(body.upiId)) bad.push(['#upi', 'Enter a UPI ID like name@bank.']);
    } else if (method === 'CARD') {
      body.cardNumber = cardNo.value.replace(/\s/g, '');
      body.cardExpiry = expiry.value;
      body.cardCvv = $('#card-cvv').value;
      body.cardName = $('#card-name').value.trim();
      if (!/^\d{13,19}$/.test(body.cardNumber)) bad.push(['#card-no', 'Enter the card number.']);
      if (!/^(0[1-9]|1[0-2])\/\d{2}$/.test(body.cardExpiry)) bad.push(['#card-exp', 'Enter the expiry as MM/YY.']);
      if (!/^\d{3,4}$/.test(body.cardCvv)) bad.push(['#card-cvv', 'Enter the 3 or 4 digit code.']);
      if (body.cardName.length < 2) bad.push(['#card-name', 'Enter the name on the card.']);
    } else {
      body.bankCode = form.querySelector('input[name="bank"]:checked')?.value;
    }
    if (bad.length) { bad.forEach(([s, m]) => setFieldError($(s), m)); $(bad[0][0]).focus(); return; }
    setBusy(payBtn, true, 'Processing');
    releaseBtn.disabled = true;
    try {
      const res = await api(`/payments/${b.pnr}`, { method: 'POST', body });
      if (res.outcome === 'SUCCESS') {
        clearInterval(timer);
        location.assign(`/ticket.html?pnr=${b.pnr}&paid=1`);
        return;
      }
      setBusy(payBtn, false);
      releaseBtn.disabled = false;
      result.replaceChildren(notice('error', 'The payment did not go through.', `${res.message} Try again or use another method.`));
    } catch (err) {
      setBusy(payBtn, false);
      releaseBtn.disabled = false;
      const map = { INVALID_UPI: '#upi', INVALID_CARD: '#card-no', INVALID_EXPIRY: '#card-exp', CARD_EXPIRED: '#card-exp', INVALID_CVV: '#card-cvv', INVALID_CARD_NAME: '#card-name' };
      if (map[err.code]) { setFieldError($(map[err.code]), err.message); $(map[err.code]).focus(); return; }
      if (err.code === 'HOLD_EXPIRED' || err.code === 'PAYMENT_CLOSED') { load(); return; }
      result.replaceChildren(notice('error', 'The payment did not go through.', err.message));
    }
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
      await api(`/payments/${b.pnr}/abandon`, { method: 'POST' });
      clearInterval(timer);
      dialog.close();
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
    const [b, banks] = await Promise.all([api(`/bookings/${pnr}`), api('/payments/banks')]);
    booking = b;
    setTitle(`Pay for PNR ${b.pnr}`);
    if (b.status === 'BOOKED' || b.status === 'PARTIALLY_CANCELLED' || b.status === 'CANCELLED') { location.replace(`/ticket.html?pnr=${b.pnr}`); return; }
    if (b.status !== 'PAYMENT_PENDING') { closed(b); return; }
    render(b, banks);
  } catch (err) {
    root.replaceChildren(notice('error', 'We could not load this booking.', err.message, h('button', { type: 'button', class: 'btn', onclick: load }, 'Try again')));
  }
}

if (requireSignIn()) {
  mountShell();
  load();
}
void booking;
