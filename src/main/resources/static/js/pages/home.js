import { mountShell } from '../core/shell.js';
import { api } from '../core/api.js';
import { $, $$, h, todayIso, addDays } from '../core/ui.js';
import { setupSearchForm } from '../core/searchform.js';

mountShell();

// ------------------------------------------------------------------ the departure board

const board = $('#board');
const NAMES = { MAS: 'Chennai Central', SBC: 'KSR Bengaluru City', NDLS: 'New Delhi', CSMT: 'Mumbai CSMT', HWH: 'Howrah Junction', SC: 'Secunderabad Junction' };

function nowHm() {
  const d = new Date();
  return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
}

async function loadBoard(code) {
  $('#board-station').textContent = NAMES[code] || code;
  board.replaceChildren(h('p', { class: 'muted' }, 'Loading departures.'));
  try {
    const today = todayIso();
    const data = await api(`/stations/${code}/departures?date=${today}`);
    let rows = data.departures.filter((d) => d.departure >= nowHm());
    let note = 'Scheduled departures for the rest of today, from the timetable.';
    if (rows.length < 5) {
      const next = await api(`/stations/${code}/departures?date=${addDays(today, 1)}`);
      rows = rows.concat(next.departures.map((d) => ({ ...d, tomorrow: true })));
      note = rows.some((d) => !d.tomorrow) ? 'Scheduled departures for tonight and tomorrow morning.' : 'Nothing else leaves today. These are tomorrow\'s first departures.';
    }
    $('#board-note').textContent = note;
    if (rows.length === 0) {
      board.replaceChildren(h('p', { class: 'muted' }, 'No trains in the timetable leave this station on these days.'));
      return;
    }
    const tbody = h('tbody');
    rows.slice(0, 5).forEach((d) => {
      tbody.append(h('tr', {},
        h('td', {}, h('span', { class: 'time' }, d.departure), d.tomorrow ? h('span', { class: 'train-no' }, 'Tomorrow') : null),
        h('td', {}, h('a', { href: `/train.html?no=${d.number}`, class: 'dest' }, d.name), h('span', { class: 'train-no' }, d.number),
          h('span', { class: 'to-sm' }, `To ${d.terminusCity}`)),
        h('td', { class: 'hide-sm' }, h('span', { class: 'dest' }, d.terminusCity))));
    });
    board.replaceChildren(h('table', { class: 'board' },
      h('thead', {}, h('tr', {}, h('th', { scope: 'col' }, 'Departs'), h('th', { scope: 'col' }, 'Train'), h('th', { scope: 'col', class: 'hide-sm' }, 'To'))),
      tbody),
      h('p', { class: 'small' }, h('a', { href: `/station.html?code=${code}` }, 'Full station board')));
  } catch (err) {
    board.replaceChildren(h('p', {}, 'We could not load the departure board. ', h('button', { type: 'button', class: 'btn btn--quiet', onclick: () => loadBoard(code) }, 'Try again')));
  }
}

const cells = $$('#board-cells button');
cells.forEach((b) => b.addEventListener('click', () => {
  cells.forEach((c) => c.setAttribute('aria-pressed', String(c === b)));
  loadBoard(b.dataset.code);
}));

// ------------------------------------------------------------------ routes, counted from tomorrow's timetable

const ROUTES = [['MAS', 'SBC', 'Chennai', 'Bengaluru'], ['CSMT', 'PUNE', 'Mumbai', 'Pune'], ['NDLS', 'BSB', 'Delhi', 'Varanasi'],
  ['HWH', 'NDLS', 'Kolkata', 'Delhi'], ['MMCT', 'NDLS', 'Mumbai', 'Delhi'], ['SBC', 'TVC', 'Bengaluru', 'Thiruvananthapuram']];

async function loadRoutes() {
  const body = $('#routes');
  const date = addDays(todayIso(), 1);
  body.replaceChildren(...ROUTES.map(([f, t, fc, tc]) => {
    const info = h('td', { class: 'muted', 'data-label': 'Trains' }, 'Checking');
    const link = `/trains.html?from=${f}&to=${t}&date=${date}&quota=GN`;
    api(`/trains/search?from=${f}&to=${t}&date=${date}`).then((r) => {
      const n = r.results.length;
      info.textContent = n ? `${n} train${n > 1 ? 's' : ''} tomorrow, first at ${r.results[0].from.time}` : 'No trains tomorrow';
      info.classList.remove('muted');
    }).catch(() => { info.textContent = 'Timetable unavailable'; });
    return h('tr', {},
      h('td', { 'data-label': 'From' }, h('span', { class: 'city' }, fc), h('div', { class: 'small muted' }, f)),
      h('td', { 'data-label': 'To' }, h('span', { class: 'city' }, tc), h('div', { class: 'small muted' }, t)),
      info,
      h('td', { class: 'num', 'data-label': 'Search' }, h('a', { class: 'btn', href: link }, 'See trains')));
  }));
}

if ($('#search')) {
  setupSearchForm($('#search'));
  loadBoard('MAS');
  loadRoutes();
}
