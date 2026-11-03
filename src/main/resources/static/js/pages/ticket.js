import { mountShell } from '../core/shell.js';
import { api } from '../core/api.js';
import { requireSignIn } from '../core/session.js';
import { $, $$, h, icon, money, fmtDate, fmtDateLong, fmtDateTime, duration, params, notice, setBusy, statusChip,
  bookingStatusChip, BERTHS, GENDERS, setTitle } from '../core/ui.js';

const q = params();
const pnr = (q.get('pnr') || '').replace(/\D/g, '');
const root = $('#ticket-root');

/** Admin pages reuse this renderer with a different API base. */
export function renderTicket(container, b, { apiBase = '/bookings', onChange, flash } = {}) {
  const paid = b.payments.find((p) => p.status === 'SUCCESS');
  const actions = h('div', { class: 'ticket-actions' });
  const pdf = h('button', { type: 'button', class: 'btn' }, icon('download'), 'Download PDF');
  pdf.addEventListener('click', async () => {
    setBusy(pdf, true, 'Preparing PDF');
    try {
      const res = await api(`/bookings/${b.pnr}/ticket.pdf`, { raw: true });
      const url = URL.createObjectURL(await res.blob());
      const a = h('a', { href: url, download: `ticket-${b.pnr}.pdf` });
      document.body.append(a); a.click(); a.remove();
      setTimeout(() => URL.revokeObjectURL(url), 2000);
    } catch (err) {
      container.prepend(notice('error', 'The PDF could not be downloaded.', err.message));
    } finally { setBusy(pdf, false); }
  });
  actions.append(pdf, h('button', { type: 'button', class: 'btn', onclick: () => window.print() }, icon('print'), 'Print'));
  if (b.cancellable) {
    actions.append(h('button', { type: 'button', class: 'btn btn--danger', onclick: () => openCancel(b, apiBase, onChange) }, 'Cancel travellers'));
  }

  const chart = b.chartPrepared ? 'Chart prepared.' : `Chart prepared at ${fmtDateTime(b.chartAt)}.`;
  const head = h('div', { class: 'ticket-head' },
    h('div', {},
      h('div', { class: 'small muted' }, 'PNR'),
      h('div', { class: 'pnr-big' }, b.pnr),
      h('div', { class: 'row', style: null }, bookingStatusChip(b.status, b.statusLabel), h('span', { class: 'small muted' }, chart))),
    actions);

  const journey = h('section', { class: 'panel', 'aria-labelledby': 'jt' },
    h('h2', { id: 'jt' }, h('a', { href: `/train.html?no=${b.train.number}` }, `${b.train.number} ${b.train.name}`)),
    h('div', { class: 'trip-line' },
      h('div', {}, h('div', { class: 't' }, b.from.time), h('div', { class: 'small' }, h('b', {}, b.from.code), ` ${b.from.name}`), h('div', { class: 'small muted' }, fmtDateLong(b.from.date))),
      h('div', { class: 'mid-line' }, `${duration(b.durationMinutes)}, ${b.distanceKm} km`),
      h('div', { class: 'end' }, h('div', { class: 't' }, b.to.time), h('div', { class: 'small' }, h('b', {}, b.to.code), ` ${b.to.name}`), h('div', { class: 'small muted' }, fmtDateLong(b.to.date)))),
    h('dl', { class: 'facts' },
      h('div', {}, h('dt', {}, 'Class'), h('dd', {}, `${b.classLabel} (${b.classCode})`)),
      h('div', {}, h('dt', {}, 'Quota'), h('dd', {}, b.quotaLabel)),
      h('div', {}, h('dt', {}, 'Booked by'), h('dd', {}, b.bookedBy)),
      h('div', {}, h('dt', {}, 'Mobile'), h('dd', {}, b.contactMobile))));

  const travellers = h('section', { class: 'panel', 'aria-labelledby': 'tt' },
    h('h2', { id: 'tt' }, 'Travellers'),
    h('div', { class: 'table-wrap' }, h('table', { class: 'table table--stack' },
      h('thead', {}, h('tr', {}, h('th', {}, 'Name'), h('th', {}, 'Age'), h('th', {}, 'Booked as'), h('th', {}, 'Status now'), h('th', { class: 'num' }, 'Fare'))),
      h('tbody', {}, b.passengers.map((p) => h('tr', { class: p.currentStatus.status === 'CAN' ? 'is-muted' : null },
        h('td', { 'data-label': 'Name' }, h('span', { class: 'strong' }, p.name), h('div', { class: 'small muted' }, `${GENDERS[p.gender]}. ${BERTHS[p.berthPreference]}${p.food ? `. ${p.food === 'NON_VEG' ? 'Non-veg' : p.food === 'VEG' ? 'Veg' : 'No food'}` : ''}`)),
        h('td', { 'data-label': 'Age' }, p.age),
        h('td', { 'data-label': 'Booked as' }, p.bookingStatus.text),
        h('td', { 'data-label': 'Status now' }, statusChip(p.currentStatus)),
        h('td', { 'data-label': 'Fare', class: 'num' }, money(p.fare), p.refund != null ? h('div', { class: 'small muted' }, `${money(p.refund)} refunded`) : null)))))));

  const f = b.fare;
  const fareRows = [['Base fare', f.base], ['Reservation charge', f.reservation], ['Superfast charge', f.superfast],
    ['Tatkal charge', f.tatkal], ['Catering', f.catering], ['GST', f.gst]].filter(([, v]) => v > 0);
  const fare = h('section', { class: 'panel', 'aria-labelledby': 'ft' }, h('h2', { id: 'ft' }, 'Fare and payment'),
    h('table', { class: 'fare-lines' }, h('tbody', {},
      fareRows.map(([k, v]) => h('tr', {}, h('td', {}, k), h('td', { class: 'num' }, money(v)))),
      h('tr', { class: 'total' }, h('td', {}, paid ? 'Paid' : 'Total'), h('td', { class: 'num' }, money(f.total))),
      b.refundTotal > 0 ? h('tr', {}, h('td', {}, 'Refunded'), h('td', { class: 'num' }, money(b.refundTotal))) : null)),
    paid ? h('dl', { class: 'facts', style: null },
      h('div', {}, h('dt', {}, 'Transaction'), h('dd', {}, paid.txnRef)),
      h('div', {}, h('dt', {}, 'Paid with'), h('dd', {}, paid.instrument)),
      h('div', {}, h('dt', {}, 'Paid on'), h('dd', {}, fmtDateTime(paid.at)))) : null);

  const history = b.cancellations.length ? h('section', { class: 'panel', 'aria-labelledby': 'ct' },
    h('h2', { id: 'ct' }, 'Cancellations'),
    h('table', { class: 'table' }, h('thead', {}, h('tr', {}, h('th', {}, 'When'), h('th', {}, 'By'), h('th', {}, 'Travellers'), h('th', { class: 'num' }, 'Refund'))),
      h('tbody', {}, b.cancellations.map((c) => h('tr', {}, h('td', {}, fmtDateTime(c.at)),
        h('td', {}, { PASSENGER: 'You', ADMIN: 'Railway staff', AUTO_WAITLIST: 'Automatic, still waitlisted at chart' }[c.reason] || c.reason),
        h('td', {}, c.passengers), h('td', { class: 'num' }, money(c.refund))))))) : null;

  const notes = h('section', { class: 'panel panel--flat' }, h('h2', {}, 'Before you travel'),
    h('p', { class: 'small' }, 'Carry a photo ID matching one traveller on this ticket. RAC travellers share a side berth. A ticket still fully waitlisted when the chart is prepared is cancelled automatically and refunded, less a clerkage charge.'),
    h('p', { class: 'small' }, h('a', { href: '/refunds.html' }, 'Refund rules')));

  container.replaceChildren(...[flash, head, h('div', { class: 'ticket-grid' },
    h('div', { class: 'stack' }, journey, travellers, history), h('div', { class: 'stack' }, fare, notes))].filter(Boolean));
}

function openCancel(b, apiBase, onChange) {
  const cancellable = b.passengers.filter((p) => p.cancellable);
  const preview = h('div', { 'aria-live': 'polite' }, h('p', { class: 'muted small' }, 'Choose who to cancel to see the refund.'));
  const confirm = h('button', { type: 'button', class: 'btn btn--danger', disabled: true }, 'Cancel and refund');
  const boxes = cancellable.map((p) => h('input', { type: 'checkbox', value: p.id }));
  const dialog = h('dialog', { class: 'dialog dialog--wide', 'aria-labelledby': 'cx-title' },
    h('div', { class: 'dialog-head' }, h('h2', { id: 'cx-title' }, 'Cancel travellers'),
      h('button', { type: 'button', class: 'icon-btn', 'aria-label': 'Close', onclick: () => dialog.close() }, icon('close'))),
    h('div', { class: 'dialog-body stack' },
      h('p', {}, `${b.train.number} ${b.train.name}, ${b.from.code} to ${b.to.code}, ${fmtDate(b.from.date)}.`),
      h('fieldset', { class: 'stack', style: null }, h('legend', { class: 'label' }, 'Travellers'),
        cancellable.map((p, i) => h('label', { class: 'check' }, boxes[i], ` ${p.name} (${p.currentStatus.text})`))),
      preview),
    h('div', { class: 'dialog-foot' }, h('button', { type: 'button', class: 'btn', onclick: () => dialog.close() }, 'Keep the ticket'), confirm));
  document.body.append(dialog);
  dialog.addEventListener('close', () => dialog.remove());

  let chosen = [];
  async function refresh() {
    chosen = boxes.filter((x) => x.checked).map((x) => Number(x.value));
    confirm.disabled = !chosen.length;
    if (!chosen.length) { preview.replaceChildren(h('p', { class: 'muted small' }, 'Choose who to cancel to see the refund.')); return; }
    preview.replaceChildren(h('p', { class: 'muted small' }, 'Working out the refund.'));
    try {
      const pv = await api(`${apiBase}/${b.pnr}/cancel/preview`, { method: 'POST', body: { passengerIds: chosen } });
      preview.replaceChildren(h('table', { class: 'table' },
        h('thead', {}, h('tr', {}, h('th', {}, 'Traveller'), h('th', {}, 'Rule'), h('th', { class: 'num' }, 'Fare'), h('th', { class: 'num' }, 'Deduction'), h('th', { class: 'num' }, 'Refund'))),
        h('tbody', {}, pv.lines.map((l) => h('tr', {}, h('td', {}, l.name), h('td', { class: 'small' }, l.rule),
          h('td', { class: 'num' }, money(l.fare)), h('td', { class: 'num' }, money(l.deduction)), h('td', { class: 'num' }, money(l.refund))))),
        h('tfoot', {}, h('tr', {}, h('td', { colspan: 4, class: 'strong' }, 'Refund to your payment method'), h('td', { class: 'num strong' }, money(pv.totalRefund))))));
      confirm.textContent = `Cancel and refund ${money(pv.totalRefund)}`;
    } catch (err) {
      preview.replaceChildren(notice('error', 'This cannot be cancelled.', err.message));
      confirm.disabled = true;
    }
  }
  boxes.forEach((x) => x.addEventListener('change', refresh));
  confirm.addEventListener('click', async () => {
    setBusy(confirm, true, 'Cancelling');
    try {
      const updated = await api(`${apiBase}/${b.pnr}/cancel`, { method: 'POST', body: { passengerIds: chosen } });
      dialog.close();
      const refunded = updated.refundTotal - b.refundTotal;
      onChange?.(updated, notice('success', `${chosen.length} traveller${chosen.length > 1 ? 's' : ''} cancelled.`,
        `${money(refunded)} goes back to the original payment method.`));
    } catch (err) {
      setBusy(confirm, false);
      preview.replaceChildren(notice('error', 'Nothing was cancelled.', err.message));
    }
  });
  dialog.showModal();
}

async function load(flash) {
  if (pnr.length !== 10) {
    root.replaceChildren(h('div', { class: 'empty-state' }, h('h1', { class: 'section-title' }, 'No ticket chosen.'), h('a', { class: 'btn', href: '/bookings.html' }, 'My trips')));
    return;
  }
  try {
    const b = await api(`/bookings/${pnr}`);
    if (b.status === 'PAYMENT_PENDING') { location.replace(`/pay.html?pnr=${pnr}`); return; }
    setTitle(`PNR ${b.pnr}`);
    if (!flash && q.get('paid') === '1') {
      flash = notice('success', 'Payment received. Your ticket is booked.', 'A copy is in My trips. Download the PDF to keep it offline.');
      history.replaceState(null, '', `/ticket.html?pnr=${pnr}`);
    }
    renderTicket(root, b, { onChange: (updated, note) => renderTicket(root, updated, { onChange: (u2, n2) => load(n2), flash: note }), flash });
  } catch (err) {
    root.replaceChildren(err.status === 404
      ? h('div', { class: 'empty-state' }, h('h1', { class: 'section-title' }, 'We could not find that ticket in your account.'),
        h('p', {}, 'Check the PNR, or use PNR status to see a booking made from another account.'), h('a', { class: 'btn', href: `/pnr.html?pnr=${pnr}` }, 'Check PNR status'))
      : notice('error', 'We could not load this ticket.', err.message, h('button', { type: 'button', class: 'btn', onclick: () => load() }, 'Try again')));
  }
}

if (location.pathname === '/ticket.html' && requireSignIn()) {
  mountShell();
  load();
}
void $$;
