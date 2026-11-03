import { mountAdmin } from './shell.js';
import { api } from '../core/api.js';
import { $, h, money, fmtDate, fmtDateTime, notice, params, bookingStatusChip } from '../core/ui.js';

const form = $('#bf');
const q = params();
['pnr', 'email', 'train', 'date', 'status'].forEach((k) => { if (q.get(k)) form.elements[k].value = q.get(k); });
const page = Math.max(0, Number(q.get('page') || 0));

function url(over = {}) {
  const u = new URLSearchParams();
  ['pnr', 'email', 'train', 'date', 'status'].forEach((k) => { const v = (over[k] ?? form.elements[k].value).trim(); if (v) u.set(k, v); });
  if (over.page) u.set('page', over.page);
  return `/admin/bookings.html?${u}`;
}

form.addEventListener('submit', (e) => { e.preventDefault(); location.assign(url()); });

async function load() {
  const list = $('#list');
  const u = new URLSearchParams(location.search);
  u.set('page', page);
  u.set('size', 25);
  try {
    const res = await api(`/admin/bookings?${u}`);
    if (!res.items.length) {
      list.replaceChildren(h('div', { class: 'empty-state' }, h('h2', {}, 'No bookings match.'), h('p', {}, 'Loosen a filter, or clear them all.'), h('a', { class: 'btn', href: '/admin/bookings.html' }, 'Clear filters')));
      return;
    }
    list.replaceChildren(
      h('div', { class: 'table-wrap' }, h('table', { class: 'table table--stack' },
        h('thead', {}, h('tr', {}, h('th', {}, 'PNR'), h('th', {}, 'Journey'), h('th', {}, 'Train'), h('th', {}, 'Booked by'),
          h('th', { class: 'num' }, 'Travellers'), h('th', {}, 'Status'), h('th', { class: 'num' }, 'Amount'), h('th', {}, 'Created'))),
        h('tbody', {}, res.items.map((b) => h('tr', {},
          h('td', { 'data-label': 'PNR' }, h('a', { class: 'strong', href: `/admin/booking.html?pnr=${b.pnr}` }, b.pnr)),
          h('td', { 'data-label': 'Journey' }, `${b.from} to ${b.to}`, h('div', { class: 'small muted' }, `${fmtDate(b.journeyDate)} ${b.departure}`)),
          h('td', { 'data-label': 'Train' }, `${b.trainNumber} ${b.trainName}`, h('div', { class: 'small muted' }, `${b.classCode}, ${b.quota}`)),
          h('td', { 'data-label': 'Booked by' }, b.bookedBy, h('div', { class: 'small muted' }, b.email)),
          h('td', { 'data-label': 'Travellers', class: 'num' }, `${b.activeTravellers} of ${b.travellers}`),
          h('td', { 'data-label': 'Status' }, bookingStatusChip(b.status, b.statusLabel)),
          h('td', { 'data-label': 'Amount', class: 'num' }, money(b.totalFare), b.refundTotal ? h('div', { class: 'small muted' }, `${money(b.refundTotal)} refunded`) : null),
          h('td', { 'data-label': 'Created' }, fmtDateTime(b.createdAt))))))),
      h('div', { class: 'pager' },
        h('span', {}, `${res.total} booking${res.total === 1 ? '' : 's'}. Page ${res.page + 1} of ${Math.max(1, res.pages)}.`),
        h('div', { class: 'row' },
          res.page > 0 ? h('a', { class: 'btn', href: url({ page: String(res.page - 1) }) }, 'Previous') : null,
          res.page + 1 < res.pages ? h('a', { class: 'btn', href: url({ page: String(res.page + 1) }) }, 'Next') : null)));
  } catch (err) {
    list.replaceChildren(notice('error', 'Bookings could not load.', err.message, h('button', { type: 'button', class: 'btn', onclick: load }, 'Try again')));
  }
}

mountAdmin('/admin/bookings.html').then((ok) => { if (ok) load(); });
