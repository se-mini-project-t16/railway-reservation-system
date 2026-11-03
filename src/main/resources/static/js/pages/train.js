import { mountShell } from '../core/shell.js';
import { api } from '../core/api.js';
import { isSignedIn } from '../core/session.js';
import { $, h, money, fmtDate, todayIso, addDays, duration, daysStrip, runningDays, params, notice, setTitle } from '../core/ui.js';

mountShell();

const q = params();
const root = $('#train-root');
const no = (q.get('no') || '').replace(/\D/g, '');

// ------------------------------------------------------------------ all trains

async function listAll() {
  setTitle('Train schedule');
  try {
    const trains = await api('/trains');
    const filter = h('input', { class: 'input', id: 'tfilter', type: 'search', placeholder: 'Number, name or station', autocomplete: 'off' });
    const tbody = h('tbody');
    const paint = () => {
      const f = filter.value.trim().toLowerCase();
      const rows = trains.filter((t) => !f || [t.number, t.name, t.originCode, t.originName, t.terminusCode, t.terminusName].join(' ').toLowerCase().includes(f));
      tbody.replaceChildren(...rows.map((t) => h('tr', {},
        h('td', { 'data-label': 'Train' }, h('a', { href: `/train.html?no=${t.number}`, class: 'strong' }, `${t.number} ${t.name}`), h('div', { class: 'small muted' }, t.typeLabel)),
        h('td', { 'data-label': 'From' }, h('span', { class: 'strong' }, t.departure), ` ${t.originName}`),
        h('td', { 'data-label': 'To' }, h('span', { class: 'strong' }, t.arrival), ` ${t.terminusName}`, t.arrivalDay > 1 ? h('span', { class: 'plus-day' }, `+${t.arrivalDay - 1}`) : null),
        h('td', { 'data-label': 'Runs' }, runningDays(t.runningDays)),
        h('td', { 'data-label': 'Classes' }, t.classes.join(' ')),
        h('td', { 'data-label': 'Distance', class: 'num' }, `${t.distanceKm} km`))));
      if (!rows.length) tbody.replaceChildren(h('tr', {}, h('td', { colspan: 6, class: 'muted' }, 'No train matches that.')));
    };
    filter.addEventListener('input', paint);
    paint();
    root.replaceChildren(
      h('div', { class: 'page-head' }, h('div', {}, h('h1', {}, 'Train schedule'), h('p', {}, `${trains.length} trains in the timetable. Open one for its stops, coaches and availability.`)),
        h('div', { class: 'field' }, h('label', { for: 'tfilter' }, 'Find a train'), filter)),
      h('div', { class: 'table-wrap' }, h('table', { class: 'table table--stack' },
        h('thead', {}, h('tr', {}, h('th', {}, 'Train'), h('th', {}, 'From'), h('th', {}, 'To'), h('th', {}, 'Runs'), h('th', {}, 'Classes'), h('th', { class: 'num' }, 'Distance'))),
        tbody)));
  } catch (err) {
    root.replaceChildren(notice('error', 'We could not load the timetable.', err.message, h('button', { type: 'button', class: 'btn', onclick: listAll }, 'Try again')));
  }
}

// ------------------------------------------------------------------ one train

function rake(t) {
  const strip = h('div', { class: 'rake', role: 'img', 'aria-label': `Coaches: ${t.classes.map((c) => `${c.coaches} ${c.label}`).join(', ')}` });
  strip.append(h('span', { class: 'loco', 'aria-hidden': 'true' }, 'Loco'));
  const order = ['FIRST_AC', '1A', '2A', '3A', '3E', 'EC', 'CC', 'SL', '2S'];
  const prefix = { '1A': 'H', '2A': 'A', '3A': 'B', '3E': 'M', SL: 'S', CC: 'C', EC: 'E', '2S': 'D' };
  [...t.classes].sort((a, b) => order.indexOf(a.code) - order.indexOf(b.code)).forEach((c) => {
    for (let i = 1; i <= c.coaches; i++) {
      strip.append(h('span', { class: ['1A', '2A', '3A', '3E', 'CC', 'EC'].includes(c.code) ? 'ac' : null, 'aria-hidden': 'true' }, `${prefix[c.code]}${i}`));
    }
  });
  if (t.pantry) strip.append(h('span', { 'aria-hidden': 'true' }, 'PC'));
  return strip;
}

function routeTable(t, from, to) {
  const fromSeq = t.stops.find((s) => s.code === from)?.seq;
  const toSeq = t.stops.find((s) => s.code === to)?.seq;
  return h('div', { class: 'table-wrap' }, h('table', { class: 'table route-table' },
    h('thead', {}, h('tr', {}, h('th', {}, 'Code'), h('th', {}, 'Station'), h('th', {}, 'Arrives'), h('th', {}, 'Departs'),
      h('th', { class: 'num' }, 'Halt'), h('th', { class: 'num' }, 'Day'), h('th', { class: 'num' }, 'Km'))),
    h('tbody', {}, t.stops.map((s) => {
      const inTrip = fromSeq && toSeq && s.seq >= fromSeq && s.seq <= toSeq;
      return h('tr', { class: [s.seq === 1 || s.seq === t.stops.length ? 'is-end' : '', fromSeq && !inTrip ? 'is-muted' : ''].join(' ').trim() || null },
        h('td', { class: 'code' }, s.code),
        h('td', {}, h('a', { href: `/station.html?code=${s.code}` }, s.name), h('div', { class: 'small muted' }, s.city)),
        h('td', {}, s.arrival || 'Starts'), h('td', {}, s.departure || 'Ends'),
        h('td', { class: 'num' }, s.haltMinutes ? `${s.haltMinutes} min` : ''),
        h('td', { class: 'num' }, s.day), h('td', { class: 'num' }, s.distanceKm));
    }))));
}

function checker(t, preset) {
  const stopOpts = (pick) => t.stops.map((s) => h('option', { value: s.code, selected: s.code === pick }, `${s.name} (${s.code})`));
  const f = h('select', { class: 'select', id: 'c-from' }, stopOpts(preset.from || t.stops[0].code).slice(0, -1));
  const to = h('select', { class: 'select', id: 'c-to' }, stopOpts(preset.to || t.stops.at(-1).code).slice(1));
  const date = h('input', { class: 'input', type: 'date', id: 'c-date', min: todayIso(), max: addDays(todayIso(), 60), value: preset.date || addDays(todayIso(), 1) });
  const cls = h('select', { class: 'select', id: 'c-class' }, t.classes.map((c) => h('option', { value: c.code }, `${c.label} (${c.code})`)));
  const quota = h('select', { class: 'select', id: 'c-quota' }, h('option', { value: 'GN' }, 'General'), h('option', { value: 'TQ' }, 'Tatkal'));
  const out = h('div', { 'aria-live': 'polite' });
  const wrap = (id, label, el) => h('div', { class: 'field' }, h('label', { for: id }, label), el.tagName === 'SELECT' ? h('div', { class: 'select-wrap' }, el) : el);
  const form = h('form', { class: 'panel', novalidate: true, 'aria-labelledby': 'chk-title' },
    h('h2', { id: 'chk-title' }, 'Check availability on this train'),
    h('div', { class: 'toolbar' }, wrap('c-from', 'From', f), wrap('c-to', 'To', to), wrap('c-date', 'From date', date), wrap('c-class', 'Class', cls), wrap('c-quota', 'Quota', quota),
      h('button', { type: 'submit', class: 'btn btn--primary' }, 'Check')),
    out);
  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    out.replaceChildren(h('p', { class: 'muted' }, 'Checking.'));
    try {
      const cal = await api(`/trains/${t.number}/availability?from=${f.value}&to=${to.value}&class=${cls.value}&quota=${quota.value}&date=${date.value}&days=6`);
      out.replaceChildren(
        h('div', { class: 'table-wrap' }, h('table', { class: 'table' },
          h('thead', {}, h('tr', {}, h('th', {}, 'Date'), h('th', {}, 'Availability'), h('th', {}, ''))),
          h('tbody', {}, cal.days.map((d) => {
            const ok = d.runs && ['AVAILABLE', 'RAC', 'WAITLIST'].includes(d.availability.status);
            const target = `/book.html?${new URLSearchParams({ train: t.number, from: cal.from, to: cal.to, date: d.date, class: cal.classCode, quota: cal.quota })}`;
            return h('tr', { class: d.runs ? null : 'is-muted' }, h('td', {}, fmtDate(d.date)),
              h('td', { class: 'strong' }, d.runs ? d.availability.text : 'Does not run'),
              h('td', {}, ok ? h('a', { class: 'btn', href: isSignedIn() ? target : `/signin.html?next=${encodeURIComponent(target)}` }, 'Book') : ''));
          })))),
        h('p', { class: 'small' }, `Fare ${money(cal.fare.total)} per traveller.`));
    } catch (err) {
      out.replaceChildren(notice('error', 'Could not check availability.', err.message));
    }
  });
  return form;
}

async function showTrain() {
  try {
    const t = await api(`/trains/${no}`);
    setTitle(`${t.number} ${t.name}`);
    const from = (q.get('from') || '').toUpperCase();
    const to = (q.get('to') || '').toUpperCase();
    const head = h('div', { class: 'page-head' },
      h('div', {}, h('h1', {}, `${t.number} ${t.name}`),
        h('p', {}, `${t.typeLabel}. ${runningDays(t.runningDays)}. ${t.distanceKm} km in ${duration(t.durationMinutes)}.${t.pantry ? ' Pantry car.' : ''}${t.active ? '' : ' Not taking reservations.'}`),
        daysStrip(t.runningDays)),
      h('a', { class: 'btn', href: '/train.html' }, 'All trains'));
    root.replaceChildren(head,
      h('section', { 'aria-labelledby': 'rake-title' }, h('h2', { id: 'rake-title', class: 'section-title' }, 'Coaches'), rake(t)),
      checker(t, { from, to, date: q.get('date') }),
      h('section', { 'aria-labelledby': 'route-title', style: null }, h('h2', { id: 'route-title', class: 'section-title', style: null }, 'Route'), routeTable(t, from, to)));
  } catch (err) {
    root.replaceChildren(err.status === 404
      ? h('div', { class: 'empty-state' }, h('h1', { class: 'section-title' }, `There is no train ${no}.`), h('a', { class: 'btn', href: '/train.html' }, 'See all trains'))
      : notice('error', 'We could not load this train.', err.message, h('button', { type: 'button', class: 'btn', onclick: showTrain }, 'Try again')));
  }
}

if (no) showTrain(); else listAll();
