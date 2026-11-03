import { mountAdmin } from './shell.js';
import { api } from '../core/api.js';
import { $, $$, h, notice, setBusy } from '../core/ui.js';

async function load() {
  const list = $('#list');
  try {
    const rules = await api('/admin/rules');
    const msg = h('div', { role: 'status' });
    const save = h('button', { type: 'submit', class: 'btn btn--primary' }, 'Save rules');
    const form = h('form', { novalidate: true, class: 'stack' },
      h('div', { class: 'table-wrap' }, h('table', { class: 'table' },
        h('thead', {}, h('tr', {}, h('th', {}, 'Applies from'), h('th', {}, 'Deduction (%)'), h('th', {}, 'At least the class flat charge'), h('th', {}, 'Shown to passengers as'))),
        h('tbody', {}, rules.map((r) => h('tr', { dataset: { id: r.id } },
          h('td', { class: 'strong' }, r.minHoursBefore ? `${r.minHoursBefore} hours before departure` : 'Any time before departure'),
          h('td', {}, h('input', { class: 'input', type: 'number', min: 0, max: 100, step: '0.5', value: r.deductionPercent, 'data-k': 'pct', 'aria-label': `Deduction percent from ${r.minHoursBefore} hours` })),
          h('td', {}, h('label', { class: 'check' }, h('input', { type: 'checkbox', checked: r.flatMinimum, 'data-k': 'flat' }), ' Yes')),
          h('td', {}, h('input', { class: 'input', maxlength: 80, value: r.description, 'data-k': 'desc', 'aria-label': `Description for ${r.minHoursBefore} hours` }))))))),
      h('p', { class: 'small muted' }, 'RAC and waitlisted tickets always lose only the clerkage charge, and confirmed Tatkal tickets are never refunded.'),
      msg, h('div', { class: 'row' }, save));
    form.addEventListener('submit', async (e) => {
      e.preventDefault();
      msg.replaceChildren();
      const body = $$('tbody tr', form).map((tr) => ({
        id: Number(tr.dataset.id),
        deductionPercent: Number(tr.querySelector('[data-k="pct"]').value),
        flatMinimum: tr.querySelector('[data-k="flat"]').checked,
        description: tr.querySelector('[data-k="desc"]').value.trim(),
      }));
      if (body.some((r) => !(r.deductionPercent >= 0 && r.deductionPercent <= 100) || r.description.length < 5)) {
        msg.append(notice('error', 'Check the rules.', 'Each deduction is 0 to 100 and each rule needs a description.'));
        return;
      }
      setBusy(save, true, 'Saving');
      try {
        await api('/admin/rules', { method: 'PUT', body });
        msg.append(notice('success', 'Rules saved.', 'The next cancellation uses them.'));
      } catch (err) {
        msg.append(notice('error', 'Not saved.', err.message));
      } finally { setBusy(save, false); }
    });
    list.replaceChildren(form);
  } catch (err) {
    list.replaceChildren(notice('error', 'Rules could not load.', err.message, h('button', { type: 'button', class: 'btn', onclick: load }, 'Try again')));
  }
}

mountAdmin('/admin/rules.html').then((ok) => { if (ok) load(); });
