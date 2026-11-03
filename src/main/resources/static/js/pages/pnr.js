import { mountShell } from '../core/shell.js';
import { api } from '../core/api.js';
import { isSignedIn } from '../core/session.js';
import { $, h, fmtDateLong, fmtDateTime, params, notice, setFieldError, clearFieldErrors, statusChip, bookingStatusChip, setTitle } from '../core/ui.js';

mountShell();

const form = $('#pnr-form');
const input = $('#pnr');
const out = $('#pnr-result');

async function check(pnr) {
  out.replaceChildren(h('div', { class: 'skeleton-row' }));
  try {
    const s = await api(`/pnr/${pnr}`);
    setTitle(`PNR ${s.pnr}`);
    const chart = s.chartPrepared ? 'Chart prepared' : `Chart not prepared yet. Expected ${fmtDateTime(s.chartAt)}.`;
    out.replaceChildren(h('section', { class: 'panel', 'aria-labelledby': 'res-title', style: null },
      h('div', { class: 'row row--between' },
        h('h2', { id: 'res-title' }, `${s.train.number} ${s.train.name}`), bookingStatusChip(s.status, s.statusLabel)),
      h('dl', { class: 'facts' },
        h('div', {}, h('dt', {}, 'From'), h('dd', {}, `${s.from.name} (${s.from.code}) ${s.from.time}`)),
        h('div', {}, h('dt', {}, 'To'), h('dd', {}, `${s.to.name} (${s.to.code}) ${s.to.time}`)),
        h('div', {}, h('dt', {}, 'Journey date'), h('dd', {}, fmtDateLong(s.journeyDate))),
        h('div', {}, h('dt', {}, 'Class and quota'), h('dd', {}, `${s.classLabel}, ${s.quota === 'TQ' ? 'Tatkal' : 'General'}`)),
        h('div', {}, h('dt', {}, 'Chart'), h('dd', {}, chart))),
      h('div', { class: 'table-wrap' }, h('table', { class: 'table' },
        h('thead', {}, h('tr', {}, h('th', {}, 'Traveller'), h('th', {}, 'Booked as'), h('th', {}, 'Status now'))),
        h('tbody', {}, s.passengers.map((p) => h('tr', {}, h('td', {}, `Traveller ${p.serial}`),
          h('td', {}, p.bookingStatus.text), h('td', {}, statusChip(p.currentStatus))))))),
      h('p', { class: 'small muted' }, isSignedIn()
        ? h('a', { href: `/ticket.html?pnr=${s.pnr}` }, 'Open the full ticket (if it is yours)')
        : ['Booked this ticket? ', h('a', { href: `/signin.html?next=${encodeURIComponent(`/ticket.html?pnr=${s.pnr}`)}` }, 'Sign in'), ' for names, berths and cancellation.'])));
  } catch (err) {
    out.replaceChildren(err.status === 404 || err.status === 400
      ? notice('error', 'No booking found for this PNR.', 'Check the 10 digits and try again. A booking appears here once it is paid.')
      : notice('error', 'We could not check this PNR.', err.message, h('button', { type: 'button', class: 'btn', onclick: () => check(pnr) }, 'Try again')));
  }
}

form.addEventListener('submit', (e) => {
  e.preventDefault();
  clearFieldErrors(form);
  const pnr = input.value.replace(/\D/g, '');
  if (pnr.length !== 10) {
    setFieldError(input, 'A PNR is exactly 10 digits.');
    input.focus();
    return;
  }
  history.replaceState(null, '', `/pnr.html?pnr=${pnr}`);
  check(pnr);
});

const initial = (params().get('pnr') || '').replace(/\D/g, '');
if (initial) {
  input.value = initial;
  if (initial.length === 10) check(initial);
  else setFieldError(input, 'A PNR is exactly 10 digits.');
}
