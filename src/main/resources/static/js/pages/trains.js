import { mountShell } from '../core/shell.js';
import { api } from '../core/api.js';
import { isSignedIn } from '../core/session.js';
import { setupSearchForm } from '../core/searchform.js';
import { $, $$, h, money, fmtDate, fmtDateLong, addDays, todayIso, duration, daysStrip, runningDays, params, notice, setTitle } from '../core/ui.js';

mountShell();

const q = params();
const search = {
  from: (q.get('from') || '').toUpperCase(),
  to: (q.get('to') || '').toUpperCase(),
  date: q.get('date') || addDays(todayIso(), 1),
  quota: q.get('quota') === 'TQ' ? 'TQ' : 'GN',
  nearby: q.get('nearby') === 'true',
};
const filters = {
  sort: q.get('sort') || 'dep',
  cls: (q.get('cls') || q.get('class') || '').split(',').filter(Boolean),
  dep: (q.get('dep') || '').split(',').filter(Boolean),
  type: (q.get('type') || '').split(',').filter(Boolean),
  avl: q.get('avl') === '1',
};

const results = $('#results');
let data = null;

// ------------------------------------------------------------------ query bar and date strip

const form = $('#search');
setupSearchForm(form, { defaults: { from: search.from, to: search.to, date: search.date } }).then(() => {
  $('#quota').value = search.quota;
  $('#nearby-field').value = search.nearby ? 'true' : '';
});

function searchUrl(over = {}) {
  const s = { ...search, ...over };
  const u = new URLSearchParams({ from: s.from, to: s.to, date: s.date, quota: s.quota });
  if (s.nearby) u.set('nearby', 'true');
  return `/trains.html?${u}`;
}

function renderDateStrip() {
  const strip = $('#date-strip');
  const today = todayIso();
  const last = addDays(today, 60);
  let start = addDays(search.date, -3);
  if (start < today) start = today;
  const cells = [];
  for (let d = start; cells.length < 7 && d <= last; d = addDays(d, 1)) cells.push(d);
  strip.replaceChildren(...cells.map((d) => h('a', { href: searchUrl({ date: d }), 'aria-current': d === search.date ? 'date' : null },
    h('span', {}, fmtDate(d, { weekday: 'short' })), fmtDate(d, { day: 'numeric', month: 'short' }))));
}

// ------------------------------------------------------------------ filters (kept in the URL)

function syncUrl() {
  const u = new URLSearchParams(location.search);
  const set = (k, v) => (v ? u.set(k, v) : u.delete(k));
  set('sort', filters.sort === 'dep' ? '' : filters.sort);
  set('cls', filters.cls.join(','));
  u.delete('class');
  set('dep', filters.dep.join(','));
  set('type', filters.type.join(','));
  set('avl', filters.avl ? '1' : '');
  history.replaceState(null, '', `${location.pathname}?${u}`);
}

function setupFilters() {
  const classes = new Map();
  const types = new Set();
  data.results.forEach((r) => {
    r.classes.forEach((c) => classes.set(c.code, c.label));
    types.add(r.typeLabel);
  });
  const fc = $('#f-class');
  fc.replaceChildren(h('legend', {}, 'Class'));
  [...classes.entries()].forEach(([code, label]) => {
    const box = h('input', { type: 'checkbox', value: code, checked: filters.cls.includes(code) });
    fc.append(h('label', { class: 'check' }, box, ` ${label} (${code})`));
  });
  const ft = $('#f-type');
  ft.replaceChildren(h('legend', {}, 'Train type'));
  [...types].sort().forEach((t) => {
    ft.append(h('label', { class: 'check' }, h('input', { type: 'checkbox', value: t, checked: filters.type.includes(t) }), ` ${t}`));
  });
  $$('#f-time input').forEach((i) => { i.checked = filters.dep.includes(i.value); });
  $('#f-avl').checked = filters.avl;
  $('#f-nearby').checked = search.nearby;
  $('#sort').value = filters.sort;
}

$('#filters-body').addEventListener('change', (e) => {
  const t = e.target;
  if (t.id === 'f-nearby') {
    location.assign(searchUrl({ nearby: t.checked }));
    return;
  }
  filters.sort = $('#sort').value;
  filters.cls = $$('#f-class input:checked').map((i) => i.value);
  filters.dep = $$('#f-time input:checked').map((i) => i.value);
  filters.type = $$('#f-type input:checked').map((i) => i.value);
  filters.avl = $('#f-avl').checked;
  syncUrl();
  renderResults();
});

$('#filters-toggle').addEventListener('click', (e) => {
  const open = !$('#filters').classList.contains('is-open');
  $('#filters').classList.toggle('is-open', open);
  e.currentTarget.setAttribute('aria-expanded', String(open));
});

// ------------------------------------------------------------------ results

function band(time) {
  const hour = Number(time.slice(0, 2));
  return String(hour < 6 ? 0 : hour < 12 ? 1 : hour < 18 ? 2 : 3);
}

function bookable(c) {
  return ['AVAILABLE', 'RAC', 'WAITLIST'].includes(c.status);
}

function lowestFare(r) {
  const fares = r.classes.filter((c) => c.fare != null).map((c) => c.fare);
  return fares.length ? Math.min(...fares) : Infinity;
}

function visible() {
  let list = data.results.filter((r) => {
    if (filters.dep.length && !filters.dep.includes(band(r.from.time))) return false;
    if (filters.type.length && !filters.type.includes(r.typeLabel)) return false;
    if (filters.cls.length && !r.classes.some((c) => filters.cls.includes(c.code))) return false;
    if (filters.avl && !r.classes.some((c) => c.status === 'AVAILABLE' && (!filters.cls.length || filters.cls.includes(c.code)))) return false;
    return true;
  });
  const arrKey = (r) => `${r.to.date}T${r.to.time}`;
  const sorters = {
    dep: (a, b) => a.from.time.localeCompare(b.from.time),
    arr: (a, b) => arrKey(a).localeCompare(arrKey(b)),
    dur: (a, b) => a.durationMinutes - b.durationMinutes,
    fare: (a, b) => lowestFare(a) - lowestFare(b),
  };
  list = [...list].sort(sorters[filters.sort] || sorters.dep);
  return list;
}

function endPoint(p, align) {
  const plus = p.date > search.date ? h('span', { class: 'plus-day', 'aria-label': `arrives ${fmtDate(p.date)}` }, `+${dayDiff(search.date, p.date)}`) : null;
  return h('div', { class: align === 'end' ? 'end' : null },
    h('div', { class: 't' }, p.time, plus),
    h('div', { class: 's' }, h('b', {}, p.code), ` ${p.name}`),
    h('div', { class: 's muted' }, fmtDate(p.date)));
}

function dayDiff(a, b) {
  return Math.round((new Date(b) - new Date(a)) / 86400000);
}

function classCell(r, c, panelId) {
  const avail = h('span', { class: `avail ${c.status}` }, c.status === 'RAC' ? h('span', { class: 'rac-mark' }, c.text) : c.text);
  const btn = h('button', { type: 'button', class: 'class-cell', 'aria-expanded': 'false', 'aria-controls': panelId,
    'data-code': c.code, disabled: c.status === 'NOT_OFFERED' },
    h('span', { class: 'code' }, c.code, h('span', {}, c.label)),
    h('span', { class: 'fare' }, c.fare != null ? money(c.fare) : 'No fare'),
    avail);
  return btn;
}

function bookUrl(r, classCode, date) {
  const u = new URLSearchParams({ train: r.number, from: r.from.code, to: r.to.code, date, class: classCode, quota: search.quota });
  return `/book.html?${u}`;
}

async function openPanel(r, c, panel, cellBtn, card) {
  $$('.class-cell', card).forEach((b) => b.setAttribute('aria-expanded', String(b === cellBtn && cellBtn.getAttribute('aria-expanded') !== 'true')));
  if (cellBtn.getAttribute('aria-expanded') !== 'true') { panel.hidden = true; return; }
  panel.hidden = false;
  panel.replaceChildren(h('p', { class: 'muted' }, `Loading ${c.label} availability.`));
  try {
    const cal = await api(`/trains/${r.number}/availability?from=${r.from.code}&to=${r.to.code}&class=${c.code}&quota=${search.quota}&date=${search.date}&days=6`);
    let chosen = cal.days.find((d) => d.date === r.from.date && d.runs) || cal.days.find((d) => d.runs);
    const grid = h('div', { class: 'calendar', role: 'group', 'aria-label': 'Availability by date' });
    const book = h('a', { class: 'btn btn--primary' });
    const note = h('p', { class: 'small muted' });
    const paint = () => {
      grid.replaceChildren(...cal.days.map((d) => {
        const cell = h('div', { class: d === chosen ? 'is-current' : null },
          h('div', { class: 'd' }, fmtDate(d.date)),
          d.runs ? h('div', { class: 'strong' }, d.availability.text) : h('div', { class: 'muted' }, 'Does not run'));
        if (d.runs && bookable(d.availability)) {
          const pick = h('button', { type: 'button', class: 'btn btn--quiet', 'aria-pressed': String(d === chosen) }, 'Choose');
          pick.addEventListener('click', () => { chosen = d; paint(); });
          cell.append(pick);
        }
        return cell;
      }));
      const a = chosen?.availability;
      const ok = a && bookable(a);
      book.textContent = ok ? `Book for ${fmtDate(chosen.date)}` : 'Not bookable';
      if (ok) {
        const target = bookUrl(r, c.code, chosen.date);
        book.href = isSignedIn() ? target : `/signin.html?next=${encodeURIComponent(target)}`;
        book.removeAttribute('aria-disabled');
      } else {
        book.removeAttribute('href');
        book.setAttribute('aria-disabled', 'true');
      }
      note.textContent = !a ? '' : a.status === 'WAITLIST' ? 'You will join the waiting list. It moves as other travellers cancel; a waitlisted e-ticket is refunded automatically if it does not clear before the chart.'
        : a.status === 'RAC' ? 'RAC gives you a shared side berth now and a full berth if one frees up.'
        : a.status === 'NOT_OPEN' ? `Tatkal for this date ${a.text.toLowerCase()} the day before.`
        : a.status === 'REGRET' ? 'The waiting list is full. Try another class, quota or date.' : '';
    };
    paint();
    const f = cal.fare;
    const lines = [['Base fare', f.base], ['Reservation', f.reservation], ['Superfast', f.superfast], ['Tatkal', f.tatkal], ['Catering', f.catering], ['GST', f.gst]]
      .filter(([, v]) => v > 0)
      .map(([k, v]) => h('tr', {}, h('td', {}, k), h('td', { class: 'num' }, money(v))));
    panel.replaceChildren(
      h('div', { class: 'row row--between' }, h('strong', {}, `${c.label}, ${search.quota === 'TQ' ? 'Tatkal' : 'General'} quota`), h('span', { class: 'small muted' }, `${r.distanceKm} km`)),
      grid,
      h('div', { class: 'class-panel-foot' },
        h('div', {}, h('div', { class: 'price' }, `${money(f.total)} `, h('span', { class: 'small muted mid' }, 'per traveller')),
          h('details', {}, h('summary', { class: 'small' }, 'Fare breakdown'), h('table', { class: 'fare-lines' }, h('tbody', {}, lines)))),
        book),
      note);
  } catch (err) {
    panel.replaceChildren(notice('error', 'We could not load availability for this class.', err.message,
      h('button', { type: 'button', class: 'btn', onclick: () => { cellBtn.setAttribute('aria-expanded', 'false'); openPanel(r, c, panel, cellBtn, card); } }, 'Try again')));
  }
}

function journeyCard(r) {
  const panelId = `panel-${r.number}`;
  const panel = h('div', { class: 'class-panel', id: panelId, hidden: true });
  const card = h('article', { class: 'journey', 'aria-labelledby': `t-${r.number}` });
  const classes = r.classes.filter((c) => !filters.cls.length || filters.cls.includes(c.code));
  const cells = h('div', { class: 'classes' }, classes.map((c) => {
    const cell = classCell(r, c, panelId);
    cell.addEventListener('click', () => openPanel(r, c, panel, cell, card));
    return cell;
  }));
  const trainLink = `/train.html?no=${r.number}&from=${r.from.code}&to=${r.to.code}&date=${search.date}`;
  const meta = [r.typeLabel, runningDays(r.runningDays), r.pantry ? 'Pantry car' : null].filter(Boolean).join('. ');
  card.append(
    h('div', { class: 'journey-top' },
      h('div', {},
        h('div', { class: 'journey-name', id: `t-${r.number}` }, h('a', { href: trainLink }, `${r.number} ${r.name}`)),
        h('div', { class: 'journey-meta' }, `${meta}.`),
        daysStrip(r.runningDays)),
      h('div', { class: 'timeline' },
        endPoint(r.from),
        h('div', { class: 'line' }, duration(r.durationMinutes), h('br'), r.stopsBetween === 0 ? 'Non-stop' : `${r.stopsBetween} stop${r.stopsBetween > 1 ? 's' : ''}`),
        endPoint(r.to, 'end'))),
    cells, panel);
  return card;
}

function renderResults() {
  const list = visible();
  results.setAttribute('aria-busy', 'false');
  const quota = search.quota === 'TQ' ? 'Tatkal quota' : 'General quota';
  $('#results-sub').textContent = `${fmtDateLong(search.date)}. ${quota}. ${list.length} of ${data.results.length} train${data.results.length === 1 ? '' : 's'}.`;
  if (data.results.length === 0) {
    const sameCity = !search.nearby;
    results.replaceChildren(h('div', { class: 'empty-state' },
      h('h2', {}, `No trains run between these stations on ${fmtDate(search.date, { weekday: 'long' })}.`),
      h('p', {}, 'Some trains run only on certain days, and some call only at another station in the same city.'),
      h('div', { class: 'row' },
        h('a', { class: 'btn', href: searchUrl({ date: addDays(search.date, 1) }) }, 'Try the next day'),
        sameCity ? h('a', { class: 'btn', href: searchUrl({ nearby: true }) }, 'Include stations in the same city') : null,
        h('a', { class: 'btn btn--quiet', href: searchUrl({ from: search.to, to: search.from }) }, 'Search the return journey'))));
    return;
  }
  if (list.length === 0) {
    results.replaceChildren(h('div', { class: 'empty-state' }, h('h2', {}, 'No trains match these filters.'),
      h('button', { type: 'button', class: 'btn', onclick: () => {
        Object.assign(filters, { cls: [], dep: [], type: [], avl: false });
        setupFilters(); syncUrl(); renderResults();
      } }, 'Clear filters')));
    return;
  }
  results.replaceChildren(...list.map(journeyCard));
}

async function load() {
  renderDateStrip();
  if (!search.from || !search.to) {
    results.setAttribute('aria-busy', 'false');
    results.replaceChildren(h('div', { class: 'empty-state' }, h('h2', {}, 'Choose where you are travelling.'), h('p', {}, 'Pick a departure and an arrival station above.')));
    return;
  }
  try {
    const u = new URLSearchParams({ from: search.from, to: search.to, date: search.date, quota: search.quota });
    if (search.nearby) u.set('nearby', 'true');
    data = await api(`/trains/search?${u}`);
    const title = `${data.from.name} to ${data.to.name}`;
    $('#results-title').textContent = title;
    setTitle(`${data.from.code} to ${data.to.code}, ${fmtDate(search.date)}`);
    setupFilters();
    renderResults();
  } catch (err) {
    results.setAttribute('aria-busy', 'false');
    results.replaceChildren(notice('error', 'We could not load trains for this search.', err.message,
      h('button', { type: 'button', class: 'btn', onclick: load }, 'Try again')));
  }
}

load();
