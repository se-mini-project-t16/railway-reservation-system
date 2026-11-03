import { mountShell } from '../core/shell.js';
import { api } from '../core/api.js';
import { requireSignIn, me } from '../core/session.js';
import { $, h, icon, notice, setBusy, setFieldError, clearFieldErrors, showFieldErrors, GENDERS, BERTHS, FOODS, fmtDate } from '../core/ui.js';

const NAME = /^[A-Za-z][A-Za-z .']{1,39}$/;

async function loadProfile() {
  const p = await me({ fresh: true });
  if (!p) return;
  $('#member').textContent = `Signed in as ${p.email}. Member since ${fmtDate(p.memberSince.slice(0, 10), { month: 'long', year: 'numeric' })}.`;
  $('#p-name').value = p.name;
  $('#p-email').value = p.email;
  $('#p-phone').value = p.phone;
}

$('#profile').addEventListener('submit', async (e) => {
  e.preventDefault();
  const form = e.currentTarget;
  clearFieldErrors(form);
  $('#p-msg').replaceChildren();
  const body = { name: $('#p-name').value.trim(), phone: $('#p-phone').value.replace(/\D/g, '') };
  if (body.name.length < 2) { setFieldError($('#p-name'), 'Enter your name.'); $('#p-name').focus(); return; }
  if (!/^[6-9]\d{9}$/.test(body.phone)) { setFieldError($('#p-phone'), 'Enter a 10-digit Indian mobile number.'); $('#p-phone').focus(); return; }
  const btn = form.querySelector('[type="submit"]');
  setBusy(btn, true, 'Saving');
  try {
    await api('/me', { method: 'PUT', body });
    try { sessionStorage.removeItem('rrs.me'); } catch { /* ignore */ }
    $('#p-msg').append(h('p', { class: 'small' }, 'Profile saved.'));
  } catch (err) {
    if (!showFieldErrors(form, err.fieldErrors, (f) => ({ name: $('#p-name'), phone: $('#p-phone') }[f]))) {
      $('#p-msg').append(notice('error', 'Not saved.', err.message));
    }
  } finally { setBusy(btn, false); }
});

$('#password').addEventListener('submit', async (e) => {
  e.preventDefault();
  const form = e.currentTarget;
  clearFieldErrors(form);
  $('#pw-msg').replaceChildren();
  const body = { currentPassword: $('#pw-cur').value, newPassword: $('#pw-new').value };
  if (!body.currentPassword) { setFieldError($('#pw-cur'), 'Enter your current password.'); $('#pw-cur').focus(); return; }
  if (body.newPassword.length < 8) { setFieldError($('#pw-new'), 'Use at least 8 characters.'); $('#pw-new').focus(); return; }
  const btn = form.querySelector('[type="submit"]');
  setBusy(btn, true, 'Changing');
  try {
    await api('/me/password', { method: 'POST', body });
    form.reset();
    $('#pw-msg').append(h('p', { class: 'small' }, 'Password changed.'));
  } catch (err) {
    if (err.code === 'WRONG_PASSWORD') { setFieldError($('#pw-cur'), err.message); $('#pw-cur').focus(); }
    else if (err.code === 'SAME_PASSWORD') { setFieldError($('#pw-new'), err.message); $('#pw-new').focus(); }
    else $('#pw-msg').append(notice('error', 'Password not changed.', err.message));
  } finally { setBusy(btn, false); }
});

// ------------------------------------------------------------------ saved travellers

function sel(id, label, options, value) {
  return h('div', { class: 'field' }, h('label', { for: id }, label),
    h('div', { class: 'select-wrap' }, h('select', { class: 'select', id }, Object.entries(options).map(([v, l]) => h('option', { value: v, selected: v === value }, l)))),
    h('span', { class: 'help' }, ''));
}

function editor(t) {
  const dialog = h('dialog', { class: 'dialog', 'aria-labelledby': 'tr-title' });
  const form = h('form', { novalidate: true },
    h('div', { class: 'dialog-head' }, h('h2', { id: 'tr-title' }, t ? 'Edit traveller' : 'Add a traveller'),
      h('button', { type: 'button', class: 'icon-btn', 'aria-label': 'Close', onclick: () => dialog.close() }, icon('close'))),
    h('div', { class: 'dialog-body stack' },
      h('div', { class: 'field' }, h('label', { for: 'tr-name' }, 'Name'), h('input', { class: 'input', id: 'tr-name', maxlength: 40, value: t?.name || '', 'aria-describedby': 'tr-name-help' }), h('span', { class: 'help', id: 'tr-name-help' }, '')),
      h('div', { class: 'field' }, h('label', { for: 'tr-age' }, 'Age'), h('input', { class: 'input', id: 'tr-age', inputmode: 'numeric', maxlength: 3, value: t?.age ?? '', 'aria-describedby': 'tr-age-help' }), h('span', { class: 'help', id: 'tr-age-help' }, '')),
      sel('tr-gender', 'Gender', GENDERS, t?.gender || 'FEMALE'),
      sel('tr-berth', 'Berth preference', BERTHS, t?.berthPreference || 'NONE'),
      sel('tr-food', 'Meal on trains with catering', FOODS, t?.food || 'VEG'),
      h('div', { id: 'tr-err' })),
    h('div', { class: 'dialog-foot' }, h('button', { type: 'button', class: 'btn', onclick: () => dialog.close() }, 'Cancel'),
      h('button', { type: 'submit', class: 'btn btn--primary' }, 'Save traveller')));
  dialog.append(form);
  document.body.append(dialog);
  dialog.addEventListener('close', () => dialog.remove());
  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    clearFieldErrors(form);
    const body = { name: $('#tr-name').value.trim(), age: Number($('#tr-age').value), gender: $('#tr-gender').value,
      berthPreference: $('#tr-berth').value, food: $('#tr-food').value };
    let bad = false;
    if (!NAME.test(body.name)) { setFieldError($('#tr-name'), 'Use 2 to 40 letters.'); bad = true; }
    if (!Number.isInteger(body.age) || body.age < 5 || body.age > 120) { setFieldError($('#tr-age'), 'Enter an age from 5 to 120.'); bad = true; }
    if (bad) { form.querySelector('[aria-invalid="true"]').focus(); return; }
    const btn = form.querySelector('[type="submit"]');
    setBusy(btn, true, 'Saving');
    try {
      await api(t ? `/me/travellers/${t.id}` : '/me/travellers', { method: t ? 'PUT' : 'POST', body });
      dialog.close();
      loadTravellers();
    } catch (err) {
      setBusy(btn, false);
      $('#tr-err').replaceChildren(notice('error', 'Not saved.', err.message));
    }
  });
  dialog.showModal();
}

async function loadTravellers() {
  const box = $('#travellers');
  try {
    const list = await api('/me/travellers');
    if (!list.length) {
      box.replaceChildren(h('p', { class: 'muted' }, 'No saved travellers yet. They are also saved when you book with the option ticked.'));
      return;
    }
    box.replaceChildren(h('table', { class: 'table table--stack' },
      h('thead', {}, h('tr', {}, h('th', {}, 'Name'), h('th', {}, 'Age'), h('th', {}, 'Gender'), h('th', {}, 'Berth'), h('th', {}, h('span', { class: 'visually-hidden' }, 'Actions')))),
      h('tbody', {}, list.map((t) => h('tr', {},
        h('td', { 'data-label': 'Name', class: 'strong' }, t.name), h('td', { 'data-label': 'Age' }, t.age),
        h('td', { 'data-label': 'Gender' }, GENDERS[t.gender]), h('td', { 'data-label': 'Berth' }, BERTHS[t.berthPreference]),
        h('td', { 'data-label': 'Actions' }, h('div', { class: 'row' },
          h('button', { type: 'button', class: 'btn btn--quiet', onclick: () => editor(t) }, 'Edit'),
          h('button', { type: 'button', class: 'btn btn--quiet', onclick: async (e) => {
            setBusy(e.currentTarget, true, 'Removing');
            try { await api(`/me/travellers/${t.id}`, { method: 'DELETE' }); loadTravellers(); } catch (err) { box.prepend(notice('error', 'Not removed.', err.message)); }
          } }, `Remove ${t.name}`))))))));
  } catch (err) {
    box.replaceChildren(notice('error', 'We could not load saved travellers.', err.message, h('button', { type: 'button', class: 'btn', onclick: loadTravellers }, 'Try again')));
  }
}

$('#add-trav').addEventListener('click', () => editor(null));

if (requireSignIn()) {
  mountShell();
  loadProfile();
  loadTravellers();
}
