import { mountShell } from '../core/shell.js';
import { api } from '../core/api.js';
import { requireSignIn } from '../core/session.js';
import { $, $$, h, money, fmtDate, params, notice, bookingStatusChip } from '../core/ui.js';

const tab = ['upcoming', 'past', 'cancelled'].includes(params().get('tab')) ? params().get('tab') : 'upcoming';
const out = $('#trips');

function minutesLeft(iso) {
  return Math.max(0, Math.round((new Date(iso).getTime() - Date.now()) / 60000));
}

function row(b) {
  const pending = b.status === 'PAYMENT_PENDING';
  const href = pending ? `/pay.html?pnr=${b.pnr}` : `/ticket.html?pnr=${b.pnr}`;
  return h('tr', {},
    h('td', { 'data-label': 'Date' }, h('span', { class: 'strong' }, fmtDate(b.from.date)), h('div', { class: 'small muted' }, `${b.from.time} to ${b.to.time}`)),
    h('td', { 'data-label': 'Journey' }, h('span', { class: 'strong' }, `${b.from.code} to ${b.to.code}`), h('div', { class: 'small muted' }, `${b.from.city} to ${b.to.city}`)),
    h('td', { 'data-label': 'Train' }, `${b.train.number} ${b.train.name}`, h('div', { class: 'small muted' }, `${b.classCode}, ${b.quota === 'TQ' ? 'Tatkal' : 'General'}`)),
    h('td', { 'data-label': 'Travellers' }, b.seatSummary, h('div', { class: 'small muted' }, `${b.activeTravellers} of ${b.travellers} travelling`)),
    h('td', { 'data-label': 'Status' }, bookingStatusChip(b.status, b.statusLabel),
      pending ? h('div', { class: 'small' }, `${minutesLeft(b.paymentDueAt)} min left to pay`) : null),
    h('td', { 'data-label': 'Amount', class: 'num' }, money(b.totalFare), b.refundTotal ? h('div', { class: 'small muted' }, `${money(b.refundTotal)} refunded`) : null),
    h('td', { 'data-label': 'PNR' }, h('a', { href, class: pending ? 'btn btn--primary' : 'btn' }, pending ? 'Pay now' : b.pnr)));
}

async function load() {
  $$('.tabs a').forEach((a) => (a.dataset.tab === tab ? a.setAttribute('aria-current', 'page') : a.removeAttribute('aria-current')));
  try {
    const list = await api(`/bookings?scope=${tab}`);
    if (!list.length) {
      const text = { upcoming: ['No upcoming journeys.', 'Tickets you book appear here until the train arrives.'],
        past: ['No past journeys yet.', 'Completed journeys move here after arrival.'],
        cancelled: ['Nothing cancelled.', 'Fully cancelled tickets and their refunds are kept here.'] }[tab];
      out.replaceChildren(h('div', { class: 'empty-state' }, h('h2', {}, text[0]), h('p', {}, text[1]), h('a', { class: 'btn btn--primary', href: '/' }, 'Find trains')));
      return;
    }
    out.replaceChildren(h('div', { class: 'table-wrap' }, h('table', { class: 'table table--stack' },
      h('thead', {}, h('tr', {}, h('th', {}, 'Date'), h('th', {}, 'Journey'), h('th', {}, 'Train'), h('th', {}, 'Travellers'),
        h('th', {}, 'Status'), h('th', { class: 'num' }, 'Amount'), h('th', {}, 'PNR'))),
      h('tbody', {}, list.map(row)))));
  } catch (err) {
    out.replaceChildren(notice('error', 'We could not load your trips.', err.message, h('button', { type: 'button', class: 'btn', onclick: load }, 'Try again')));
  }
}

if (requireSignIn()) {
  mountShell();
  load();
}
