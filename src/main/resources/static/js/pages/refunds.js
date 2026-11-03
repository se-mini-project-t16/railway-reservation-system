import { mountShell } from '../core/shell.js';
import { api } from '../core/api.js';
import { $, h, money, notice } from '../core/ui.js';

mountShell();

async function load() {
  const out = $('#rules');
  try {
    const p = await api('/policies/refunds');
    out.replaceChildren(
      h('section', { class: 'stack', 'aria-labelledby': 'cnf-title' },
        h('h2', { id: 'cnf-title', class: 'section-title' }, 'Confirmed tickets'),
        h('p', {}, 'The deduction per traveller is the larger of the percentage below and the flat charge for the class.'),
        h('div', { class: 'table-wrap' }, h('table', { class: 'table' },
          h('thead', {}, h('tr', {}, h('th', {}, 'Cancelled'), h('th', { class: 'num' }, 'Deduction'))),
          h('tbody', {}, p.confirmedRules.map((r) => h('tr', {}, h('td', {}, r.description),
            h('td', { class: 'num strong' }, r.deductionPercent === '100' ? 'All of it' : r.deductionPercent === '0' ? 'Flat charge only' : `${r.deductionPercent}%`))))))),
      h('section', { class: 'stack', 'aria-labelledby': 'flat-title', style: null },
        h('h2', { id: 'flat-title', class: 'section-title' }, 'Flat charge by class'),
        h('div', { class: 'table-wrap' }, h('table', { class: 'table' },
          h('thead', {}, h('tr', {}, h('th', {}, 'Class'), h('th', { class: 'num' }, 'Per traveller'))),
          h('tbody', {}, p.flatCharges.map((c) => h('tr', {}, h('td', {}, `${c.label} (${c.code})`), h('td', { class: 'num' }, money(c.flatCancellationCharge)))))))),
      h('section', { class: 'stack', 'aria-labelledby': 'other-title' },
        h('h2', { id: 'other-title', class: 'section-title' }, 'RAC, waiting list and Tatkal'),
        h('dl', { class: 'facts' },
          h('div', {}, h('dt', {}, 'RAC or waitlisted'), h('dd', {}, `Full refund less ${money(p.waitlistClerkage)} per traveller, up to 30 minutes before departure.`)),
          h('div', {}, h('dt', {}, 'Still waitlisted at chart'), h('dd', {}, `Cancelled automatically, refunded less ${money(p.waitlistClerkage)}.`)),
          h('div', {}, h('dt', {}, 'Tatkal'), h('dd', {}, p.tatkalRule)),
          h('div', {}, h('dt', {}, 'Unpaid holds'), h('dd', {}, 'Released after 10 minutes. Nothing is charged.')))));
    out.className = 'stack-lg';
  } catch (err) {
    out.replaceChildren(notice('error', 'We could not load the refund rules.', err.message, h('button', { type: 'button', class: 'btn', onclick: load }, 'Try again')));
  }
}

load();
