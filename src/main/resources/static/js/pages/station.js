import { mountShell } from '../core/shell.js';
import { api } from '../core/api.js';
import { stationCombobox } from '../core/stations.js';
import { $, h, todayIso, addDays, fmtDateLong, params, notice, setFieldError, clearFieldErrors, setTitle } from '../core/ui.js';

mountShell();

const q = params();
const board = $('#st-board');
const dateInput = $('#st-date');
dateInput.min = addDays(todayIso(), -1);
dateInput.max = addDays(todayIso(), 60);
dateInput.value = q.get('date') || todayIso();

async function load(code, date) {
  board.replaceChildren(h('p', { class: 'muted' }, 'Loading departures.'));
  try {
    const b = await api(`/stations/${code}/departures?date=${date}`);
    $('#st-title').textContent = `Leaving ${b.station.name}`;
    $('#st-sub').textContent = `${fmtDateLong(date)}. ${b.departures.length} scheduled departure${b.departures.length === 1 ? '' : 's'} from the timetable.`;
    setTitle(`${b.station.name} departures`);
    if (!b.departures.length) {
      board.replaceChildren(h('p', {}, 'No train in the timetable leaves this station on this day.'));
      return;
    }
    board.replaceChildren(h('div', { class: 'table-wrap' }, h('table', { class: 'board' },
      h('thead', {}, h('tr', {}, h('th', {}, 'Departs'), h('th', {}, 'Train'), h('th', { class: 'hide-sm' }, 'To'), h('th', { class: 'hide-sm' }, 'Arrives'))),
      h('tbody', {}, b.departures.map((d) => h('tr', {},
        h('td', {}, h('span', { class: 'time' }, d.departure)),
        h('td', {}, h('a', { class: 'dest', href: `/train.html?no=${d.number}&from=${code}&date=${date}` }, d.name), h('span', { class: 'train-no' }, `${d.number}, ${d.typeLabel}`),
          h('span', { class: 'to-sm' }, `To ${d.terminusCity}`)),
        h('td', { class: 'hide-sm' }, h('span', { class: 'dest' }, d.terminusCity), h('span', { class: 'train-no' }, d.terminusName)),
        h('td', { class: 'hide-sm' }, d.terminusDay > 1 ? `${d.arrivalAtTerminus}, day ${d.terminusDay}` : d.arrivalAtTerminus,
          d.starts ? null : h('span', { class: 'train-no' }, 'Passing through'))))))));
  } catch (err) {
    board.replaceChildren(notice('error', 'We could not load this station.', err.message));
  }
}

stationCombobox($('#from')).then((combo) => {
  const code = (q.get('code') || 'MAS').toUpperCase();
  combo.set(code, { silent: true });
  load(code, dateInput.value);
  $('#st-form').addEventListener('submit', (e) => {
    e.preventDefault();
    clearFieldErrors(e.currentTarget);
    if (!combo.value) { setFieldError($('#from'), 'Choose a station from the list.'); $('#from').focus(); return; }
    history.replaceState(null, '', `/station.html?code=${combo.value}&date=${dateInput.value}`);
    load(combo.value, dateInput.value);
  });
});
