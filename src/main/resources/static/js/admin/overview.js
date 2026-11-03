import { mountAdmin } from './shell.js';
import { api } from '../core/api.js';
import { $, h, money, fmtDate, fmtDateLong, notice } from '../core/ui.js';

async function load() {
  const out = $('#ov');
  try {
    const o = await api('/admin/overview');
    $('#ov-sub').textContent = `${fmtDateLong(o.today)}. Figures come from bookings made on this system.`;
    const stat = (value, label) => h('div', {}, h('b', {}, value), h('span', {}, label));
    const max = Math.max(1, ...o.lastWeek.map((d) => d.revenue));
    const bars = h('div', { class: 'bars', role: 'img', 'aria-label': `Revenue by day: ${o.lastWeek.map((d) => `${fmtDate(d.date)} ${money(d.revenue)}`).join(', ')}` },
      o.lastWeek.map((d) => h('div', { class: 'bar' }, h('b', {}, d.revenue ? money(d.revenue) : ''), h('i', { style: null, dataset: { h: String(Math.round((d.revenue / max) * 100)) } }))));
    const labels = h('div', { class: 'bars-labels', 'aria-hidden': 'true' }, o.lastWeek.map((d) => h('span', {}, fmtDate(d.date, { weekday: 'short', day: 'numeric' }))));
    const week = h('section', { 'aria-labelledby': 'wk' },
      h('h2', { id: 'wk', class: 'section-title' }, 'Revenue, last seven days'), bars, labels,
      h('div', { class: 'table-wrap' }, h('table', { class: 'table', style: null },
        h('thead', {}, h('tr', {}, h('th', {}, 'Day'), h('th', { class: 'num' }, 'Bookings'), h('th', { class: 'num' }, 'Travellers'), h('th', { class: 'num' }, 'Revenue'), h('th', { class: 'num' }, 'Refunds'))),
        h('tbody', {}, [...o.lastWeek].reverse().map((d) => h('tr', {}, h('td', {}, fmtDate(d.date)), h('td', { class: 'num' }, d.bookings),
          h('td', { class: 'num' }, d.travellers), h('td', { class: 'num' }, money(d.revenue)), h('td', { class: 'num' }, money(d.refunds))))))));
    const side = h('div', { class: 'stack-lg' },
      h('section', { 'aria-labelledby': 'mix' }, h('h2', { id: 'mix', class: 'section-title' }, 'Travellers by class, this week'),
        o.classMix.length ? h('table', { class: 'table' }, h('tbody', {}, o.classMix.map((c) => h('tr', {}, h('td', {}, `${c.label} (${c.code})`), h('td', { class: 'num strong' }, c.travellers)))))
          : h('p', { class: 'muted' }, 'No tickets booked this week yet.')),
      h('section', { 'aria-labelledby': 'up' }, h('h2', { id: 'up', class: 'section-title' }, 'Departing in the next 48 hours'),
        o.upcoming.length ? h('table', { class: 'table' },
          h('thead', {}, h('tr', {}, h('th', {}, 'Train'), h('th', {}, 'Leaves origin'), h('th', { class: 'num' }, 'Travellers'), h('th', {}, ''))),
          h('tbody', {}, o.upcoming.map((u) => h('tr', {},
            h('td', {}, h('span', { class: 'strong' }, u.trainNumber), h('div', { class: 'small muted' }, u.trainName)),
            h('td', {}, `${fmtDate(u.startDate)} ${u.departure} ${u.originCode}`, h('div', { class: 'small muted' }, u.chartPrepared ? 'Chart prepared' : 'Chart pending')),
            h('td', { class: 'num' }, u.travellers),
            h('td', {}, h('a', { href: `/admin/chart.html?train=${u.trainNumber}&date=${u.startDate}` }, 'Chart'))))))
          : h('p', { class: 'muted' }, 'No booked travellers depart in the next 48 hours.')));
    out.replaceChildren(
      h('div', { class: 'stats' },
        stat(o.bookingsToday, 'Tickets booked today'), stat(o.travellersToday, 'Travellers booked today'),
        stat(money(o.revenueToday), 'Collected today'), stat(money(o.refundsToday), 'Refunded today'),
        stat(o.cancellationsToday, 'Cancellations today'), stat(o.pendingPayments, 'Holds awaiting payment'),
        stat(`${o.activeTrains} of ${o.totalTrains}`, 'Trains taking bookings'), stat(o.passengers, 'Passenger accounts')),
      h('div', { class: 'admin-grid' }, week, side));
    out.querySelectorAll('.bar i').forEach((i) => { i.style.height = `${i.dataset.h}%`; });
  } catch (err) {
    out.replaceChildren(notice('error', 'The overview could not load.', err.message, h('button', { type: 'button', class: 'btn', onclick: load }, 'Try again')));
  }
}

mountAdmin('/admin/').then((ok) => { if (ok) load(); });
