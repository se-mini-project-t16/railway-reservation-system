import { mountAdmin } from './shell.js';
import { api } from '../core/api.js';
import { $, $$, h, notice, setBusy, params, setTitle } from '../core/ui.js';

const number = (params().get('no') || '').replace(/\D/g, '');
const root = $('#edit-root');
const TYPES = { RAJDHANI: 'Rajdhani', SHATABDI: 'Shatabdi', VANDE_BHARAT: 'Vande Bharat', DURONTO: 'Duronto', GATIMAAN: 'Gatimaan',
  JAN_SHATABDI: 'Jan Shatabdi', SUPERFAST: 'Superfast', MAIL_EXPRESS: 'Mail/Express' };
const CLASSES = { '1A': 'First AC', '2A': 'AC 2 Tier', '3A': 'AC 3 Tier', '3E': 'AC 3 Economy', SL: 'Sleeper', CC: 'AC Chair Car', EC: 'Executive Chair Car', '2S': 'Second Sitting' };
const DAYS = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];

let stations = [];
let uid = 0;

const sel = (options, value, attrs = {}) => h('div', { class: 'select-wrap' }, h('select', { class: 'select', ...attrs },
  Object.entries(options).map(([v, l]) => h('option', { value: v, selected: v === value }, l))));

function classRow(c = { code: 'SL', coaches: 1 }) {
  const id = ++uid;
  const row = h('div', { class: 'class-row' },
    h('div', { class: 'field' }, h('label', { for: `cc-${id}` }, 'Class'), sel(CLASSES, c.code, { id: `cc-${id}`, 'data-k': 'code' })),
    h('div', { class: 'field' }, h('label', { for: `cn-${id}` }, 'Coaches'), h('input', { class: 'input', id: `cn-${id}`, type: 'number', min: 1, max: 24, value: c.coaches, 'data-k': 'coaches' })),
    h('button', { type: 'button', class: 'btn btn--quiet', onclick: () => row.remove() }, 'Remove'));
  return row;
}

function stopRow(s = {}) {
  const id = ++uid;
  const stationOptions = Object.fromEntries([['', 'Choose a station'], ...stations.map((st) => [st.code, `${st.code} ${st.name}`])]);
  const row = h('div', { class: 'stop-row' },
    h('div', { class: 'n', 'aria-hidden': 'true' }, ''),
    h('div', { class: 'field f-station' }, h('label', { for: `ss-${id}` }, 'Station'), sel(stationOptions, s.code || '', { id: `ss-${id}`, 'data-k': 'code' })),
    h('div', { class: 'field' }, h('label', { for: `sa-${id}` }, 'Arrives'), h('input', { class: 'input', id: `sa-${id}`, type: 'time', value: s.arrival || '', 'data-k': 'arrival' })),
    h('div', { class: 'field' }, h('label', { for: `sd-${id}` }, 'Departs'), h('input', { class: 'input', id: `sd-${id}`, type: 'time', value: s.departure || '', 'data-k': 'departure' })),
    h('div', { class: 'field' }, h('label', { for: `sy-${id}` }, 'Day'), h('input', { class: 'input', id: `sy-${id}`, type: 'number', min: 1, max: 4, value: s.day || 1, 'data-k': 'day' })),
    h('div', { class: 'field' }, h('label', { for: `sk-${id}` }, 'Km'), h('input', { class: 'input', id: `sk-${id}`, type: 'number', min: 0, max: 5000, value: s.distanceKm ?? '', 'data-k': 'distanceKm' })),
    h('div', { class: 'row' },
      h('button', { type: 'button', class: 'btn btn--quiet', 'aria-label': 'Move stop up', onclick: () => { if (row.previousElementSibling) row.parentElement.insertBefore(row, row.previousElementSibling); renumber(); } }, 'Up'),
      h('button', { type: 'button', class: 'btn btn--quiet', onclick: () => { row.remove(); renumber(); } }, 'Remove')));
  return row;
}

function renumber() {
  $$('#stops .stop-row').forEach((r, i, all) => {
    r.querySelector('.n').textContent = i + 1;
    r.querySelector('[data-k="arrival"]').disabled = i === 0;
    r.querySelector('[data-k="departure"]').disabled = i === all.length - 1;
  });
}

function collect() {
  const runningDays = DAYS.map((_, i) => ($(`#day-${i}`).checked ? '1' : '0')).join('');
  const val = (row, k) => row.querySelector(`[data-k="${k}"]`).value;
  return {
    number: $('#t-number').value.trim(),
    name: $('#t-name').value.trim(),
    type: $('#t-type').value,
    runningDays,
    pantry: $('#t-pantry').checked,
    active: $('#t-active').checked,
    classes: $$('#classes .class-row').map((r) => ({ code: val(r, 'code'), coaches: Number(val(r, 'coaches')) })),
    stops: $$('#stops .stop-row').map((r, i, all) => ({
      code: val(r, 'code'),
      arrival: i === 0 ? '' : val(r, 'arrival'),
      departure: i === all.length - 1 ? '' : val(r, 'departure'),
      day: Number(val(r, 'day')) || 1,
      distanceKm: Number(val(r, 'distanceKm')),
    })),
  };
}

function render(t, bookings) {
  setTitle(t ? `Edit ${t.number}` : 'Add a train');
  const days = t ? t.runningDays : '1111111';
  const msg = h('div', { id: 'msg', role: 'status' });
  const form = h('form', { novalidate: true, class: 'stack-lg' },
    h('div', { class: 'page-head' }, h('div', {}, h('h1', {}, t ? `${t.number} ${t.name}` : 'Add a train'),
      h('p', {}, t ? (bookings ? `${bookings} bookings on record: the route and timings are locked, and coaches can only be added.` : 'No bookings yet, so everything can be changed.') : 'Set the route in order, starting at 0 km.')),
      h('a', { class: 'btn', href: '/admin/trains.html' }, 'All trains')),
    h('section', { class: 'panel', 'aria-labelledby': 'basics' }, h('h2', { id: 'basics' }, 'Service'),
      h('div', { class: 'toolbar' },
        h('div', { class: 'field' }, h('label', { for: 't-number' }, 'Number'), h('input', { class: 'input', id: 't-number', inputmode: 'numeric', maxlength: 5, value: t?.number || '', disabled: Boolean(t) })),
        h('div', { class: 'field grow' }, h('label', { for: 't-name' }, 'Name'), h('input', { class: 'input', id: 't-name', maxlength: 80, value: t?.name || '' })),
        h('div', { class: 'field' }, h('label', { for: 't-type' }, 'Type'), sel(TYPES, t?.type || 'SUPERFAST', { id: 't-type' }))),
      h('fieldset', { class: 'seg days-picker' }, h('legend', {}, 'Leaves its origin on'),
        DAYS.map((d, i) => [h('input', { type: 'checkbox', id: `day-${i}`, checked: days[i] === '1' }), h('label', { for: `day-${i}` }, d)])),
      h('div', { class: 'row', style: null },
        h('label', { class: 'check' }, h('input', { type: 'checkbox', id: 't-pantry', checked: Boolean(t?.pantry) }), ' Pantry car'),
        h('label', { class: 'check' }, h('input', { type: 'checkbox', id: 't-active', checked: t ? t.active : true }), ' Taking bookings'))),
    h('section', { class: 'panel', 'aria-labelledby': 'cls' }, h('h2', { id: 'cls' }, 'Classes and coaches'),
      h('div', { id: 'classes' }, (t ? t.classes : [{ code: 'SL', coaches: 8 }, { code: '3A', coaches: 3 }]).map(classRow)),
      h('button', { type: 'button', class: 'btn', onclick: () => $('#classes').append(classRow()) }, 'Add a class')),
    h('section', { class: 'panel', 'aria-labelledby': 'rt' }, h('h2', { id: 'rt' }, 'Route'),
      h('p', { class: 'small muted' }, 'Day 1 is the day the train leaves its origin. A stop reached after midnight is day 2.'),
      h('div', { id: 'stops' }, (t ? t.stops.map((s) => ({ code: s.code, arrival: s.arrival, departure: s.departure, day: s.day, distanceKm: s.distanceKm })) : [{ distanceKm: 0 }, {}]).map(stopRow)),
      h('button', { type: 'button', class: 'btn', onclick: () => { $('#stops').append(stopRow()); renumber(); } }, 'Add a stop')),
    msg,
    h('div', { class: 'row row--between' },
      t ? h('button', { type: 'button', class: 'btn btn--danger', id: 'del' }, 'Delete train') : h('span'),
      h('button', { type: 'submit', class: 'btn btn--primary btn--lg' }, t ? 'Save changes' : 'Create train')));
  root.replaceChildren(form);
  renumber();

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    msg.replaceChildren();
    const body = collect();
    const problems = [];
    if (!/^\d{5}$/.test(body.number)) problems.push('The train number is 5 digits.');
    if (body.name.length < 3) problems.push('Give the train a name.');
    if (!body.runningDays.includes('1')) problems.push('Choose at least one running day.');
    if (!body.classes.length) problems.push('Add at least one class.');
    if (body.stops.length < 2) problems.push('A route needs at least two stops.');
    if (body.stops.some((s) => !s.code)) problems.push('Every stop needs a station.');
    if (problems.length) { msg.append(notice('error', 'Check these first.', problems.join(' '))); msg.scrollIntoView({ block: 'center' }); return; }
    const btn = form.querySelector('[type="submit"]');
    setBusy(btn, true, 'Saving');
    try {
      const saved = await api(t ? `/admin/trains/${t.number}` : '/admin/trains', { method: t ? 'PUT' : 'POST', body });
      if (!t) { location.assign(`/admin/train-edit.html?no=${saved.number}&created=1`); return; }
      setBusy(btn, false);
      msg.append(notice('success', 'Saved.', 'The next search uses these details.'));
    } catch (err) {
      setBusy(btn, false);
      const fields = Object.entries(err.fieldErrors || {}).map(([k, v]) => `${k}: ${v}`).join(' ');
      msg.append(notice('error', 'Not saved.', fields || err.message));
    }
    msg.scrollIntoView({ block: 'center' });
  });

  $('#del')?.addEventListener('click', () => {
    const dialog = h('dialog', { class: 'dialog', 'aria-labelledby': 'del-t' },
      h('div', { class: 'dialog-head' }, h('h2', { id: 'del-t' }, `Delete ${t.number}?`)),
      h('div', { class: 'dialog-body' }, h('p', {}, 'The train and its route are removed for good. Trains with tickets on record cannot be deleted; stop their bookings instead.')),
      h('div', { class: 'dialog-foot' }, h('button', { type: 'button', class: 'btn', onclick: () => dialog.close() }, 'Keep it'),
        h('button', { type: 'button', class: 'btn btn--danger', id: 'del-go' }, 'Delete train')));
    document.body.append(dialog);
    dialog.addEventListener('close', () => dialog.remove());
    dialog.showModal();
    $('#del-go').addEventListener('click', async (ev) => {
      setBusy(ev.currentTarget, true, 'Deleting');
      try {
        await api(`/admin/trains/${t.number}`, { method: 'DELETE' });
        location.assign('/admin/trains.html');
      } catch (err) {
        dialog.close();
        msg.replaceChildren(notice('error', 'Not deleted.', err.message));
      }
    });
  });
}

async function load() {
  try {
    stations = await api('/admin/stations');
    if (!number) { render(null, 0); return; }
    const [t, list] = await Promise.all([api(`/admin/trains/${number}`), api('/admin/trains')]);
    render(t, list.find((x) => x.number === number)?.bookings || 0);
    if (params().get('created')) $('#msg').append(notice('success', 'Train created.', 'It appears in search results on the days it runs.'));
  } catch (err) {
    root.replaceChildren(notice('error', 'This train could not load.', err.message, h('a', { class: 'btn', href: '/admin/trains.html' }, 'All trains')));
  }
}

mountAdmin('/admin/trains.html').then((ok) => { if (ok) load(); });
