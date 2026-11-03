import { mountAdmin } from './shell.js';
import { api } from '../core/api.js';
import { $, h, notice, setBusy, params } from '../core/ui.js';

const page = Math.max(0, Number(params().get('page') || 0));

async function load() {
  const list = $('#list');
  try {
    const res = await api(`/admin/users?page=${page}&size=25`);
    list.replaceChildren(
      h('div', { class: 'table-wrap' }, h('table', { class: 'table table--stack' },
        h('thead', {}, h('tr', {}, h('th', {}, 'Name'), h('th', {}, 'Email'), h('th', {}, 'Mobile'), h('th', {}, 'Role'), h('th', {}, 'Sign-in'), h('th', {}, 'Joined'))),
        h('tbody', {}, res.items.map((u) => {
          let state;
          if (u.locked) {
            const unlock = h('button', { type: 'button', class: 'btn' }, 'Unlock');
            unlock.addEventListener('click', async () => {
              setBusy(unlock, true, 'Unlocking');
              try { await api(`/admin/users/${u.id}/unlock`, { method: 'POST' }); load(); } catch (err) { setBusy(unlock, false); list.prepend(notice('error', 'Not unlocked.', err.message)); }
            });
            state = h('div', { class: 'row' }, h('span', { class: 'st st--failed' }, 'Locked'), unlock);
          } else {
            state = h('span', { class: 'small' }, u.failedAttempts ? `${u.failedAttempts} recent wrong password${u.failedAttempts > 1 ? 's' : ''}` : 'Normal');
          }
          return h('tr', {}, h('td', { 'data-label': 'Name', class: 'strong' }, u.name), h('td', { 'data-label': 'Email' }, u.email),
            h('td', { 'data-label': 'Mobile' }, u.phone), h('td', { 'data-label': 'Role' }, u.role === 'ADMIN' ? 'Administrator' : 'Passenger'),
            h('td', { 'data-label': 'Sign-in' }, state), h('td', { 'data-label': 'Joined' }, new Date(u.createdAt).toLocaleDateString('en-IN')));
        })))),
      h('div', { class: 'pager' }, h('span', {}, `${res.total} accounts. Page ${res.page + 1} of ${Math.max(1, res.pages)}.`),
        h('div', { class: 'row' },
          res.page > 0 ? h('a', { class: 'btn', href: `/admin/users.html?page=${res.page - 1}` }, 'Previous') : null,
          res.page + 1 < res.pages ? h('a', { class: 'btn', href: `/admin/users.html?page=${res.page + 1}` }, 'Next') : null)));
  } catch (err) {
    list.replaceChildren(notice('error', 'Users could not load.', err.message, h('button', { type: 'button', class: 'btn', onclick: load }, 'Try again')));
  }
}

mountAdmin('/admin/users.html').then((ok) => { if (ok) load(); });
