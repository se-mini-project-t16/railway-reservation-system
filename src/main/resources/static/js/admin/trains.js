import { mountAdmin } from './shell.js';
import { api } from '../core/api.js';
import { $, h, runningDays, notice, setBusy } from '../core/ui.js';

let trains = [];

function paint() {
  const f = $('#tf').value.trim().toLowerCase();
  const rows = trains.filter((t) => !f || [t.number, t.name, t.originCode, t.originName, t.terminusCode, t.terminusName].join(' ').toLowerCase().includes(f));
  const list = $('#list');
  if (!rows.length) { list.replaceChildren(h('p', { class: 'muted' }, trains.length ? 'No train matches that filter.' : 'No trains yet. Add the first one.')); return; }
  list.replaceChildren(h('div', { class: 'table-wrap' }, h('table', { class: 'table table--stack' },
    h('thead', {}, h('tr', {}, h('th', {}, 'Train'), h('th', {}, 'Route'), h('th', {}, 'Runs'), h('th', {}, 'Classes'),
      h('th', { class: 'num' }, 'Bookings'), h('th', {}, 'Status'), h('th', {}, h('span', { class: 'visually-hidden' }, 'Actions')))),
    h('tbody', {}, rows.map((t) => {
      const toggle = h('button', { type: 'button', class: 'btn btn--quiet' }, t.active ? 'Stop bookings' : 'Take bookings');
      toggle.addEventListener('click', async () => {
        setBusy(toggle, true, 'Saving');
        try {
          await api(`/admin/trains/${t.number}/status`, { method: 'PATCH', body: { active: !t.active } });
          t.active = !t.active;
          paint();
        } catch (err) {
          setBusy(toggle, false);
          list.prepend(notice('error', 'Status not changed.', err.message));
        }
      });
      return h('tr', { class: t.active ? null : 'is-muted' },
        h('td', { 'data-label': 'Train' }, h('a', { class: 'strong', href: `/admin/train-edit.html?no=${t.number}` }, `${t.number} ${t.name}`), h('div', { class: 'small muted' }, t.typeLabel)),
        h('td', { 'data-label': 'Route' }, `${t.originCode} ${t.departure} to ${t.terminusCode} ${t.arrival}`, t.days > 1 ? h('span', { class: 'plus-day' }, `+${t.days - 1}`) : null),
        h('td', { 'data-label': 'Runs' }, runningDays(t.runningDays)),
        h('td', { 'data-label': 'Classes' }, t.classes.join(' ')),
        h('td', { 'data-label': 'Bookings', class: 'num' }, t.bookings),
        h('td', { 'data-label': 'Status' }, h('span', { class: `st ${t.active ? 'st--ok' : 'st--closed'}` }, t.active ? 'Taking bookings' : 'Stopped')),
        h('td', { 'data-label': 'Actions' }, h('div', { class: 'row' },
          h('a', { class: 'btn btn--quiet', href: `/admin/train-edit.html?no=${t.number}` }, 'Edit'),
          h('a', { class: 'btn btn--quiet', href: `/admin/chart.html?train=${t.number}` }, 'Chart'), toggle)));
    })))));
}

async function load() {
  try {
    trains = await api('/admin/trains');
    paint();
  } catch (err) {
    $('#list').replaceChildren(notice('error', 'Trains could not load.', err.message, h('button', { type: 'button', class: 'btn', onclick: load }, 'Try again')));
  }
}

$('#tf').addEventListener('input', paint);
mountAdmin('/admin/trains.html').then((ok) => { if (ok) load(); });
