// Station list and an accessible station combobox (WAI-ARIA combobox with a listbox popup).
import { api } from './api.js';
import { h } from './ui.js';

const KEY = 'rrs.stations.v1';
let cache;

export async function stations() {
  if (cache) return cache;
  try {
    const stored = sessionStorage.getItem(KEY);
    if (stored) return (cache = JSON.parse(stored));
  } catch { /* ignore */ }
  cache = await api('/stations');
  try { sessionStorage.setItem(KEY, JSON.stringify(cache)); } catch { /* ignore */ }
  return cache;
}

export function label(st) {
  return `${st.name} (${st.code})`;
}

function rank(st, q) {
  const code = st.code.toLowerCase();
  const name = st.name.toLowerCase();
  const city = st.city.toLowerCase();
  if (code === q) return 0;
  if (code.startsWith(q)) return 1;
  if (city === q) return 2;
  if (name.startsWith(q) || city.startsWith(q)) return 3;
  if (name.split(/\s+/).some((w) => w.startsWith(q))) return 4;
  if (name.includes(q) || city.includes(q)) return 5;
  return 99;
}

export function search(list, query, limit = 8) {
  const q = query.trim().toLowerCase().replace(/\(([a-z]+)\)$/, '$1');
  if (!q) return [];
  return list.map((s) => [rank(s, q), s]).filter(([r]) => r < 99)
    .sort((a, b) => a[0] - b[0] || a[1].name.localeCompare(b[1].name))
    .slice(0, limit).map(([, s]) => s);
}

/**
 * Upgrades a text input into a station picker. The visible input keeps the
 * station's name; a hidden input with the original `name` carries its code,
 * so the form still submits codes. Without JavaScript the plain input posts
 * whatever was typed (a code works).
 */
export async function stationCombobox(input, { onChange } = {}) {
  const list = await stations();
  const name = input.name;
  const hidden = h('input', { type: 'hidden', name });
  input.removeAttribute('name');
  input.after(hidden);
  input.setAttribute('role', 'combobox');
  input.setAttribute('aria-autocomplete', 'list');
  input.setAttribute('aria-expanded', 'false');
  input.setAttribute('autocomplete', 'off');
  const listId = `${input.id}-list`;
  input.setAttribute('aria-controls', listId);
  const box = h('ul', { id: listId, class: 'combo-list', role: 'listbox', hidden: true });
  input.parentElement.classList.add('combo');
  input.after(box);

  let options = [];
  let active = -1;

  function set(st, { silent = false } = {}) {
    hidden.value = st ? st.code : '';
    input.value = st ? label(st) : input.value;
    input.dataset.code = st ? st.code : '';
    input.removeAttribute('aria-invalid');
    if (!silent && onChange) onChange(st);
  }

  function close() {
    box.hidden = true;
    input.setAttribute('aria-expanded', 'false');
    input.removeAttribute('aria-activedescendant');
    active = -1;
  }

  function render() {
    box.replaceChildren();
    options = search(list, input.value);
    if (!options.length) {
      if (input.value.trim().length > 1) {
        box.append(h('li', { class: 'empty', role: 'option', 'aria-disabled': 'true' }, 'No station matches. Try a code such as MAS or NDLS.'));
        box.hidden = false;
        input.setAttribute('aria-expanded', 'true');
      } else {
        close();
      }
      return;
    }
    options.forEach((st, i) => {
      const li = h('li', { id: `${listId}-${i}`, role: 'option', 'aria-selected': i === active ? 'true' : 'false' },
        h('span', { class: 'code' }, st.code),
        h('span', {}, st.name, h('span', { class: 'where' }, `${st.city}, ${st.state}`)));
      li.addEventListener('mousedown', (e) => { e.preventDefault(); set(st); close(); });
      box.append(li);
    });
    box.hidden = false;
    input.setAttribute('aria-expanded', 'true');
  }

  function move(delta) {
    if (box.hidden) render();
    if (!options.length) return;
    active = (active + delta + options.length) % options.length;
    box.querySelectorAll('[role="option"]').forEach((li, i) => li.setAttribute('aria-selected', i === active ? 'true' : 'false'));
    input.setAttribute('aria-activedescendant', `${listId}-${active}`);
    box.children[active]?.scrollIntoView({ block: 'nearest' });
  }

  input.addEventListener('input', () => { hidden.value = ''; input.dataset.code = ''; active = -1; render(); });
  input.addEventListener('focus', () => { if (input.value && !hidden.value) render(); });
  input.addEventListener('keydown', (e) => {
    if (e.key === 'ArrowDown') { e.preventDefault(); move(1); }
    else if (e.key === 'ArrowUp') { e.preventDefault(); move(-1); }
    else if (e.key === 'Enter' && !box.hidden && active >= 0) { e.preventDefault(); set(options[active]); close(); }
    else if (e.key === 'Escape') { close(); }
    else if (e.key === 'Tab' && !box.hidden && active >= 0) { set(options[active]); close(); }
  });
  input.addEventListener('blur', () => {
    setTimeout(close, 0);
    if (!hidden.value && input.value.trim()) {
      const best = search(list, input.value, 2);
      const exact = best.find((s) => s.code.toLowerCase() === input.value.trim().toLowerCase());
      if (exact || best.length === 1) set(exact || best[0]);
    }
  });

  return {
    set(code, opts) {
      const st = list.find((s) => s.code === String(code || '').toUpperCase());
      if (st) set(st, opts); else { hidden.value = ''; input.value = ''; }
    },
    get value() { return hidden.value; },
    get station() { return list.find((s) => s.code === hidden.value) || null; },
    input,
  };
}
