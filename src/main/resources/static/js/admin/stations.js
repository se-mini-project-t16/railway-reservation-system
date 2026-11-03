import { mountAdmin } from './shell.js';
import { api } from '../core/api.js';
import { $, h, notice, setBusy, setFieldError, clearFieldErrors, showFieldErrors } from '../core/ui.js';

async function load() {
  const list = $('#list');
  try {
    const stations = await api('/admin/stations');
    list.replaceChildren(h('div', { class: 'table-wrap' }, h('table', { class: 'table table--stack' },
      h('thead', {}, h('tr', {}, h('th', {}, 'Code'), h('th', {}, 'Station'), h('th', {}, 'City'), h('th', {}, 'State'))),
      h('tbody', {}, stations.map((s) => h('tr', {}, h('td', { 'data-label': 'Code', class: 'strong' }, s.code),
        h('td', { 'data-label': 'Station' }, h('a', { href: `/station.html?code=${s.code}` }, s.name)),
        h('td', { 'data-label': 'City' }, s.city), h('td', { 'data-label': 'State' }, s.state)))))));
  } catch (err) {
    list.replaceChildren(notice('error', 'Stations could not load.', err.message, h('button', { type: 'button', class: 'btn', onclick: load }, 'Try again')));
  }
}

$('#sf').addEventListener('submit', async (e) => {
  e.preventDefault();
  const form = e.currentTarget;
  clearFieldErrors(form);
  const msg = $('#s-msg');
  msg.replaceChildren();
  const body = { code: $('#s-code').value.trim().toUpperCase(), name: $('#s-name').value.trim(), city: $('#s-city').value.trim(), state: $('#s-state').value.trim() };
  const bad = [];
  if (!/^[A-Z]{2,5}$/.test(body.code)) bad.push(['#s-code', 'Use 2 to 5 capital letters.']);
  if (body.name.length < 3) bad.push(['#s-name', 'Enter the station name.']);
  if (body.city.length < 2) bad.push(['#s-city', 'Enter the city.']);
  if (body.state.length < 2) bad.push(['#s-state', 'Enter the state.']);
  if (bad.length) { bad.forEach(([s, m]) => setFieldError($(s), m)); $(bad[0][0]).focus(); return; }
  const btn = form.querySelector('[type="submit"]');
  setBusy(btn, true, 'Adding');
  try {
    await api('/admin/stations', { method: 'POST', body });
    form.reset();
    msg.append(notice('success', `${body.code} added.`, 'It can now be used on a route.'));
    try { sessionStorage.removeItem('rrs.stations.v1'); } catch { /* ignore */ }
    load();
  } catch (err) {
    if (err.code === 'DUPLICATE_STATION') { setFieldError($('#s-code'), err.message); $('#s-code').focus(); }
    else if (!showFieldErrors(form, err.fieldErrors, (f) => $(`#s-${f}`))) msg.append(notice('error', 'Not added.', err.message));
  } finally { setBusy(btn, false); }
});

mountAdmin('/admin/stations.html').then((ok) => { if (ok) load(); });
