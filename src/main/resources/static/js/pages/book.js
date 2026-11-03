import { mountShell } from '../core/shell.js';
import { api } from '../core/api.js';
import { requireSignIn, me } from '../core/session.js';
import { $, $$, h, money, fmtDate, duration, params, notice, setBusy, setFieldError, clearFieldErrors,
  BERTHS, FOODS, GENDERS, setTitle } from '../core/ui.js';

const q = params();
const journey = {
  trainNumber: q.get('train') || '',
  from: (q.get('from') || '').toUpperCase(),
  to: (q.get('to') || '').toUpperCase(),
  date: q.get('date') || '',
  classCode: (q.get('class') || '').toUpperCase(),
  quota: q.get('quota') === 'TQ' ? 'TQ' : 'GN',
};
const DRAFT = `rrs.draft.${journey.trainNumber}.${journey.date}.${journey.classCode}`;
const NAME = /^[A-Za-z][A-Za-z .']{1,39}$/;
const root = $('#book-root');

let quote;
let travellers = [];
let saved = [];
let profile;

function backToSearch() {
  const u = new URLSearchParams({ from: journey.from, to: journey.to, date: journey.date, quota: journey.quota });
  return `/trains.html?${u}`;
}

function blank() {
  return { name: '', age: '', gender: '', berthPreference: 'NONE', food: quote?.pantry ? 'VEG' : null };
}

function saveDraft() {
  try { sessionStorage.setItem(DRAFT, JSON.stringify({ travellers, mobile: $('#mobile')?.value })); } catch { /* ignore */ }
}

async function start() {
  try {
    [quote, profile, saved] = await Promise.all([
      api('/bookings/quote', { method: 'POST', body: { ...journey, passengers: 1 } }),
      me({ fresh: true }),
      api('/me/travellers').catch(() => []),
    ]);
  } catch (err) {
    root.replaceChildren(h('div', { class: 'empty-state' }, h('h1', { class: 'section-title' }, 'This journey cannot be booked.'),
      h('p', {}, err.message), h('a', { class: 'btn', href: backToSearch() }, 'Back to trains')));
    return;
  }
  setTitle(`Book ${quote.train.number} ${quote.from.code} to ${quote.to.code}`);
  let draft = {};
  try { draft = JSON.parse(sessionStorage.getItem(DRAFT) || '{}'); } catch { /* ignore */ }
  travellers = Array.isArray(draft.travellers) && draft.travellers.length ? draft.travellers : [blank()];
  renderTravellerStep(draft.mobile || profile?.phone || '');
  refreshQuote();
}

// ------------------------------------------------------------------ journey and fare panels

function journeyPanel() {
  const a = quote.availability;
  return h('section', { class: 'panel', 'aria-labelledby': 'journey-title' },
    h('h2', { id: 'journey-title' }, `${quote.train.number} ${quote.train.name}`),
    h('div', { class: 'trip-line' },
      h('div', {}, h('div', { class: 't' }, quote.from.time), h('div', { class: 'small' }, h('b', {}, quote.from.code), ` ${quote.from.name}`), h('div', { class: 'small muted' }, fmtDate(quote.from.date))),
      h('div', { class: 'mid-line' }, duration(quote.durationMinutes)),
      h('div', { class: 'end' }, h('div', { class: 't' }, quote.to.time), h('div', { class: 'small' }, h('b', {}, quote.to.code), ` ${quote.to.name}`), h('div', { class: 'small muted' }, fmtDate(quote.to.date)))),
    h('dl', { class: 'facts' },
      h('div', {}, h('dt', {}, 'Class'), h('dd', {}, `${quote.classLabel} (${quote.classCode})`)),
      h('div', {}, h('dt', {}, 'Quota'), h('dd', {}, quote.quota === 'TQ' ? 'Tatkal' : 'General')),
      h('div', {}, h('dt', {}, 'Distance'), h('dd', {}, `${quote.distanceKm} km`)),
      h('div', {}, h('dt', {}, 'Right now'), h('dd', {}, a.text))));
}

function farePanel() {
  const t = quote.total;
  const rows = [['Base fare', t.base], ['Reservation charge', t.reservation], ['Superfast charge', t.superfast],
    ['Tatkal charge', t.tatkal], ['Catering', t.catering], ['GST', t.gst]].filter(([, v]) => v > 0);
  return h('section', { class: 'panel', id: 'fare-panel', 'aria-labelledby': 'fare-title' },
    h('h2', { id: 'fare-title' }, 'Fare'),
    h('table', { class: 'fare-lines' }, h('tbody', {},
      rows.map(([k, v]) => h('tr', {}, h('td', {}, k), h('td', { class: 'num' }, money(v)))),
      h('tr', { class: 'total' }, h('td', {}, `Total for ${quote.passengers}`), h('td', { class: 'num' }, money(t.total))))),
    h('p', { class: 'small muted' }, `${money(quote.perPassenger.total)} per traveller. Children under 5 travel free without a berth.`));
}

async function refreshQuote() {
  try {
    quote = await api('/bookings/quote', { method: 'POST', body: { ...journey, passengers: Math.max(1, travellers.length) } });
    const old = $('#fare-panel');
    if (old) old.replaceWith(farePanel());
  } catch { /* keep the last quote */ }
}

// ------------------------------------------------------------------ step 1: travellers

function setStep(n) {
  [1, 2, 3].forEach((i) => {
    const li = $(`#s-${i}`);
    if (i === n) li.setAttribute('aria-current', 'step'); else li.removeAttribute('aria-current');
    li.classList.toggle('done', i < n);
  });
}

function select(id, label, options, value, { required = false, help = '' } = {}) {
  const sel = h('select', { class: 'select', id, required, 'aria-describedby': `${id}-help` },
    required && !value ? h('option', { value: '' }, 'Choose') : null,
    Object.entries(options).map(([v, l]) => h('option', { value: v, selected: v === value }, l)));
  return h('div', { class: 'field' }, h('label', { for: id }, label), h('div', { class: 'select-wrap' }, sel),
    h('span', { class: 'help', id: `${id}-help`, 'data-hint': help }, help));
}

function travellerRow(t, i) {
  const berths = Object.fromEntries(quote.berthPreferences.map((b) => [b, BERTHS[b]]));
  const row = h('div', { class: 'traveller', dataset: { index: i } },
    h('div', { class: 'n', 'aria-hidden': 'true' }, i + 1),
    h('div', { class: 'field f-name' }, h('label', { for: `t${i}-name` }, `Traveller ${i + 1} name`),
      h('input', { class: 'input', id: `t${i}-name`, value: t.name, autocomplete: 'off', maxlength: 40, required: true, 'aria-describedby': `t${i}-name-help` }),
      h('span', { class: 'help', id: `t${i}-name-help` }, '')),
    h('div', { class: 'field' }, h('label', { for: `t${i}-age` }, 'Age'),
      h('input', { class: 'input', id: `t${i}-age`, value: t.age, inputmode: 'numeric', maxlength: 3, required: true, 'aria-describedby': `t${i}-age-help` }),
      h('span', { class: 'help', id: `t${i}-age-help` }, '')),
    select(`t${i}-gender`, 'Gender', GENDERS, t.gender, { required: true }),
    select(`t${i}-berth`, quote.classCode.match(/CC|EC|2S/) ? 'Seat' : 'Berth', berths, t.berthPreference),
    quote.pantry ? select(`t${i}-food`, 'Meal', FOODS, t.food || 'VEG') : h('div'),
    travellers.length > 1
      ? h('button', { type: 'button', class: 'btn btn--quiet remove', 'aria-label': `Remove traveller ${i + 1}`, onclick: () => {
        readRows(); travellers.splice(i, 1); renderRows(); refreshQuote(); saveDraft();
      } }, 'Remove')
      : h('div'));
  row.addEventListener('input', () => { readRows(); saveDraft(); });
  row.addEventListener('change', () => { readRows(); saveDraft(); });
  return row;
}

function readRows() {
  $$('.traveller').forEach((row) => {
    const i = Number(row.dataset.index);
    travellers[i] = {
      name: $(`#t${i}-name`).value,
      age: $(`#t${i}-age`).value,
      gender: $(`#t${i}-gender`).value,
      berthPreference: $(`#t${i}-berth`).value,
      food: quote.pantry ? $(`#t${i}-food`).value : null,
    };
  });
}

function renderRows() {
  $('#rows').replaceChildren(...travellers.map(travellerRow));
  $('#add').disabled = travellers.length >= quote.maxPassengers;
  $('#add').textContent = travellers.length >= quote.maxPassengers ? `Up to ${quote.maxPassengers} travellers` : 'Add a traveller';
  renderChips();
}

function renderChips() {
  const box = $('#saved');
  if (!box) return;
  box.replaceChildren();
  if (!saved.length) return;
  const names = new Set(travellers.map((t) => t.name.trim().toLowerCase()));
  box.append(h('span', { class: 'small muted' }, 'Saved travellers:'));
  saved.forEach((s) => {
    box.append(h('button', { type: 'button', class: 'chip', disabled: names.has(s.name.toLowerCase()) || false,
      onclick: () => {
        readRows();
        const emptyIndex = travellers.findIndex((t) => !t.name.trim());
        const t = { name: s.name, age: String(s.age), gender: s.gender,
          berthPreference: quote.berthPreferences.includes(s.berthPreference) ? s.berthPreference : 'NONE',
          food: quote.pantry ? (s.food || 'VEG') : null };
        if (emptyIndex >= 0) travellers[emptyIndex] = t;
        else if (travellers.length < quote.maxPassengers) travellers.push(t);
        renderRows(); refreshQuote(); saveDraft();
      } }, `${s.name}, ${s.age}`));
  });
}

function renderTravellerStep(mobile) {
  setStep(1);
  const form = h('form', { id: 'travellers-form', novalidate: true, class: 'stack' },
    h('section', { class: 'panel', 'aria-labelledby': 'trav-title' },
      h('h2', { id: 'trav-title' }, 'Travellers'),
      h('p', { class: 'small muted' }, 'Names as on a photo ID. Travellers aged 60 or more, and women aged 45 or more, are offered lower berths first.'),
      h('div', { class: 'saved-chips', id: 'saved' }),
      h('div', { id: 'rows' }),
      h('div', { class: 'row', style: null }, h('button', { type: 'button', class: 'btn', id: 'add' }, 'Add a traveller'))),
    h('section', { class: 'panel', 'aria-labelledby': 'contact-title' },
      h('h2', { id: 'contact-title' }, 'Contact and options'),
      h('div', { class: 'field' }, h('label', { for: 'mobile' }, 'Mobile number for journey alerts'),
        h('input', { class: 'input', id: 'mobile', type: 'tel', inputmode: 'numeric', maxlength: 10, autocomplete: 'tel-national', value: mobile, 'aria-describedby': 'mobile-help' }),
        h('span', { class: 'help', id: 'mobile-help' }, '')),
      h('div', { class: 'stack' },
        h('label', { class: 'check' }, h('input', { type: 'checkbox', id: 'only-cnf' }), ' Book only if every traveller gets a confirmed berth'),
        h('label', { class: 'check' }, h('input', { type: 'checkbox', id: 'save-trav', checked: true }), ' Save these travellers for next time'))),
    h('div', { id: 'step1-error' }),
    h('div', { class: 'row' }, h('a', { class: 'btn', href: backToSearch() }, 'Back to trains'),
      h('button', { type: 'submit', class: 'btn btn--primary btn--lg' }, 'Continue to review')));

  root.replaceChildren(h('div', { class: 'two-col' },
    h('div', { class: 'stack' }, journeyPanel(), form),
    h('aside', { class: 'aside-sticky' }, farePanel())));
  $('#add').addEventListener('click', () => {
    readRows();
    if (travellers.length < quote.maxPassengers) travellers.push(blank());
    renderRows(); refreshQuote(); saveDraft();
    $(`#t${travellers.length - 1}-name`)?.focus();
  });
  renderRows();
  form.addEventListener('submit', (e) => {
    e.preventDefault();
    readRows();
    if (validate()) renderReview();
  });
}

function validate() {
  const form = $('#travellers-form');
  clearFieldErrors(form);
  const bad = [];
  travellers.forEach((t, i) => {
    if (!NAME.test(t.name.trim())) bad.push([`#t${i}-name`, 'Use 2 to 40 letters; spaces, dots and apostrophes are allowed.']);
    const age = Number(t.age);
    if (!t.age || !Number.isInteger(age) || age > 120) bad.push([`#t${i}-age`, 'Enter an age in years.']);
    else if (age < 5) bad.push([`#t${i}-age`, 'Children under 5 travel free without a berth. Leave them off the ticket.']);
    if (!t.gender) bad.push([`#t${i}-gender`, 'Choose a gender.']);
  });
  const names = travellers.map((t) => t.name.trim().toLowerCase());
  names.forEach((n, i) => { if (n && names.indexOf(n) !== i) bad.push([`#t${i}-name`, 'This traveller is already on the ticket.']); });
  if (!/^[6-9]\d{9}$/.test($('#mobile').value.trim())) bad.push(['#mobile', 'Enter a 10-digit Indian mobile number.']);
  bad.forEach(([sel, msg]) => setFieldError($(sel), msg));
  if (bad.length) { $(bad[0][0]).focus(); return false; }
  return true;
}

// ------------------------------------------------------------------ step 2: review

function renderReview() {
  setStep(2);
  const mobile = $('#mobile').value.trim();
  const onlyCnf = $('#only-cnf').checked;
  const saveTrav = $('#save-trav').checked;
  saveDraft();
  const a = quote.availability;
  const expect = a.status === 'AVAILABLE'
    ? (a.count >= travellers.length ? 'Berths are free now. They are confirmed when you hold them on the next step.' : `Only ${a.count} berths are free now; the rest of the group would join RAC or the waiting list.`)
    : a.status === 'RAC' ? 'The class is full. Your group joins RAC, sharing side berths until full berths free up.'
    : a.status === 'WAITLIST' ? `The class is full. Your group joins the waiting list at about ${a.text}.`
    : 'This class cannot be booked right now.';
  const tbody = h('tbody', {}, travellers.map((t, i) => h('tr', {},
    h('td', { 'data-label': '#' }, i + 1), h('td', { 'data-label': 'Name' }, t.name.trim()),
    h('td', { 'data-label': 'Age' }, t.age), h('td', { 'data-label': 'Gender' }, GENDERS[t.gender]),
    h('td', { 'data-label': 'Preference' }, BERTHS[t.berthPreference] || 'No preference'),
    quote.pantry ? h('td', { 'data-label': 'Meal' }, FOODS[t.food] || '') : null)));
  const pay = h('button', { type: 'button', class: 'btn btn--primary btn--lg' }, `Hold berths and pay ${money(quote.total.total)}`);
  const errBox = h('div');
  root.replaceChildren(h('div', { class: 'two-col' },
    h('div', { class: 'stack' }, journeyPanel(),
      h('section', { class: 'panel', 'aria-labelledby': 'rev-title' },
        h('h2', { id: 'rev-title' }, 'Check the details'),
        h('div', { class: 'table-wrap' }, h('table', { class: 'table table--stack' },
          h('thead', {}, h('tr', {}, h('th', {}, '#'), h('th', {}, 'Name'), h('th', {}, 'Age'), h('th', {}, 'Gender'), h('th', {}, 'Preference'), quote.pantry ? h('th', {}, 'Meal') : null)),
          tbody)),
        h('p', { class: 'small' }, `Alerts go to ${mobile}. ${onlyCnf ? 'Booking only if all berths confirm.' : ''}`)),
      h('section', { class: 'panel', 'aria-labelledby': 'what-title' },
        h('h2', { id: 'what-title' }, 'What happens next'),
        h('p', {}, expect),
        h('p', { class: 'small muted' }, `Berths are held for 10 minutes while you pay. ${quote.quota === 'TQ' ? 'Confirmed Tatkal tickets are not refunded on cancellation.' : 'Cancelling a confirmed ticket 48 hours or more before departure costs a flat charge per traveller; closer to departure the deduction grows.'} `,
          h('a', { href: '/refunds.html', target: '_blank' }, 'Refund rules'))),
      errBox,
      h('div', { class: 'row' }, h('button', { type: 'button', class: 'btn', onclick: () => { renderTravellerStep(mobile); $('#only-cnf').checked = onlyCnf; $('#save-trav').checked = saveTrav; } }, 'Edit travellers'), pay)),
    h('aside', { class: 'aside-sticky' }, farePanel())));
  $('#rev-title').focus?.();
  window.scrollTo({ top: 0 });

  pay.addEventListener('click', async () => {
    setBusy(pay, true, 'Holding berths');
    errBox.replaceChildren();
    try {
      const booking = await api('/bookings', { method: 'POST', body: {
        ...journey, mobile, onlyIfConfirmed: onlyCnf, saveTravellers: saveTrav,
        passengers: travellers.map((t) => ({ name: t.name.trim(), age: Number(t.age), gender: t.gender,
          berthPreference: t.berthPreference, food: t.food || null })),
      } });
      try { sessionStorage.removeItem(DRAFT); } catch { /* ignore */ }
      location.assign(`/pay.html?pnr=${booking.pnr}`);
    } catch (err) {
      setBusy(pay, false);
      const fields = Object.keys(err.fieldErrors || {});
      if (fields.length) {
        renderTravellerStep(mobile);
        const map = (f) => {
          const m = f.match(/^passengers\[(\d+)]\.(\w+)$/);
          if (m) return $(`#t${m[1]}-${{ name: 'name', age: 'age', gender: 'gender', berthPreference: 'berth', food: 'food' }[m[2]]}`);
          return f === 'mobile' ? $('#mobile') : null;
        };
        let first = null;
        for (const [f, msg] of Object.entries(err.fieldErrors)) {
          const el = map(f);
          if (el) { setFieldError(el, msg); first = first || el; }
        }
        first?.focus();
        return;
      }
      errBox.replaceChildren(notice('error', 'Nothing was booked.', err.message,
        err.code === 'REGRET' || err.code === 'NOT_ALL_CONFIRMED' ? h('a', { class: 'btn', href: backToSearch() }, 'Choose another train or class') : null));
      errBox.querySelector('.notice')?.scrollIntoView({ block: 'center' });
    }
  });
}

if (requireSignIn()) {
  mountShell();
  start();
}
