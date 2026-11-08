// Small DOM, formatting and feedback helpers shared by every page.

export const $ = (sel, root = document) => root.querySelector(sel);
export const $$ = (sel, root = document) => Array.from(root.querySelectorAll(sel));

/**
 * Builds an element. Text is always set as text, never parsed as HTML.
 * h('a', { href: '/x', class: 'btn' }, 'Label', childNode)
 */
export function h(tag, attrs = {}, ...children) {
  const el = document.createElement(tag);
  for (const [k, v] of Object.entries(attrs || {})) {
    if (v === null || v === undefined || v === false) continue;
    if (k === 'class') el.className = v;
    else if (k === 'text') el.textContent = v;
    else if (k.startsWith('on') && typeof v === 'function') el.addEventListener(k.slice(2), v);
    else if (k === 'dataset') Object.assign(el.dataset, v);
    else if (v === true) el.setAttribute(k, '');
    else el.setAttribute(k, String(v));
  }
  // A table that scrolls sideways on a phone must be reachable by keyboard to scroll it (WCAG 2.1.1)
  if (el.classList.contains('table-wrap') && !el.hasAttribute('tabindex')) el.setAttribute('tabindex', '0');
  for (const c of children.flat(Infinity)) {
    if (c === null || c === undefined || c === false) continue;
    el.append(c instanceof Node ? c : document.createTextNode(String(c)));
  }
  return el;
}

/** Inline SVG icons drawn for this site (one set, stroke icons, always decorative). */
const ICONS = {
  swap: '<path d="M7 4v16M7 4 3 8M7 4l4 4M17 20V4M17 20l-4-4M17 20l4-4"/>',
  sun: '<circle cx="12" cy="12" r="4"/><path d="M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4"/>',
  moon: '<path d="M20 14.5A8 8 0 0 1 9.5 4a8 8 0 1 0 10.5 10.5Z"/>',
  menu: '<path d="M3 6h18M3 12h18M3 18h18"/>',
  close: '<path d="M5 5l14 14M19 5 5 19"/>',
  user: '<circle cx="12" cy="8" r="4"/><path d="M4 21c1.5-4 4.5-6 8-6s6.5 2 8 6"/>',
  download: '<path d="M12 3v12M7 10l5 5 5-5M4 21h16"/>',
  print: '<path d="M6 9V3h12v6M6 17H3v-6h18v6h-3M6 14h12v7H6z"/>',
  chevronL: '<path d="M15 5l-7 7 7 7"/>',
  chevronR: '<path d="M9 5l7 7-7 7"/>',
};

export function icon(name) {
  const span = document.createElement('span');
  span.innerHTML = `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="square" stroke-linejoin="miter" aria-hidden="true" focusable="false">${ICONS[name] || ''}</svg>`;
  return span.firstChild;
}

export const LOGO = '<svg viewBox="0 0 28 28" aria-hidden="true" focusable="false"><rect x="1" y="1" width="26" height="26" fill="none" stroke="currentColor" stroke-width="2"/><path d="M6 10h16M6 18h16" stroke="currentColor" stroke-width="2"/><path d="M10 6v16M18 6v16" stroke="currentColor" stroke-width="2"/></svg>';

// ------------------------------------------------------------------ formatting

const inr = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 });
export const money = (n) => inr.format(Number(n || 0));

const DAYS = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];

/** Parses an ISO date (yyyy-mm-dd) as a local calendar date. */
export function parseDate(iso) {
  const [y, m, d] = String(iso).slice(0, 10).split('-').map(Number);
  return new Date(y, m - 1, d);
}

export function isoDate(date) {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
}

export function addDays(iso, n) {
  const d = parseDate(iso);
  d.setDate(d.getDate() + n);
  return isoDate(d);
}

export function todayIso() { return isoDate(new Date()); }

export function fmtDate(iso, opts = { weekday: 'short', day: 'numeric', month: 'short' }) {
  return parseDate(iso).toLocaleDateString('en-IN', opts);
}

export const fmtDateLong = (iso) => fmtDate(iso, { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' });

export function fmtDateTime(isoDateTime) {
  if (!isoDateTime) return '';
  const d = new Date(isoDateTime);
  return d.toLocaleString('en-IN', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit', hour12: false });
}

export function duration(minutes) {
  const h = Math.floor(minutes / 60);
  const m = minutes % 60;
  return h ? `${h}h ${String(m).padStart(2, '0')}m` : `${m}m`;
}

/** Running days such as "Daily", "Except Wed" or "Mon Wed Fri". */
export function runningDays(mask) {
  if (mask === '1111111') return 'Daily';
  const on = [...mask].map((c, i) => (c === '1' ? DAYS[i] : null)).filter(Boolean);
  const off = [...mask].map((c, i) => (c === '0' ? DAYS[i] : null)).filter(Boolean);
  if (off.length <= 2) return `Except ${off.join(' and ')}`;
  return on.join(' ');
}

export function daysStrip(mask) {
  const wrap = h('span', { class: 'days' }, h('small', { class: 'visually-hidden' }, `Runs ${runningDays(mask)}`));
  ['M', 'T', 'W', 'T', 'F', 'S', 'S'].forEach((d, i) => {
    wrap.append(h('span', { class: mask[i] === '1' ? 'on' : null, 'aria-hidden': 'true' }, d));
  });
  return wrap;
}

export const GENDERS = { MALE: 'Male', FEMALE: 'Female', TRANSGENDER: 'Transgender' };
export const BERTHS = {
  NONE: 'No preference', LOWER: 'Lower', MIDDLE: 'Middle', UPPER: 'Upper',
  SIDE_LOWER: 'Side lower', SIDE_UPPER: 'Side upper', WINDOW: 'Window', AISLE: 'Aisle',
};
export const FOODS = { VEG: 'Veg', NON_VEG: 'Non-veg', NO_FOOD: 'No food' };

/** CSS class for a traveller status chip from its status code. */
export function statusChip(view) {
  const cls = { CNF: 'st--cnf', RAC: 'st--rac', WL: 'st--wl', CAN: 'st--can' }[view.status] || '';
  return h('span', { class: `st ${cls}` }, view.text);
}

export function bookingStatusChip(status, label) {
  const cls = {
    BOOKED: 'st--ok', PARTIALLY_CANCELLED: 'st--rac', CANCELLED: 'st--can',
    PAYMENT_PENDING: 'st--pending', PAYMENT_FAILED: 'st--failed', EXPIRED: 'st--closed',
  }[status] || '';
  return h('span', { class: `st ${cls}` }, label);
}

// ------------------------------------------------------------------ feedback

let toastRegion;
export function toast(message, kind = 'info', ms = 5000) {
  if (!toastRegion) {
    toastRegion = h('div', { class: 'toasts', role: 'status', 'aria-live': 'polite' });
    document.body.append(toastRegion);
  }
  const note = h('div', { class: `notice notice--${kind}` }, message);
  toastRegion.append(note);
  setTimeout(() => note.remove(), ms);
}

/** A notice block for a region: error, empty or success, with an optional action. */
export function notice(kind, title, text, action) {
  return h('div', { class: `notice notice--${kind}`, role: kind === 'error' ? 'alert' : null },
    h('strong', {}, title), text ? h('p', {}, text) : null,
    action ? h('div', { class: 'row' }, action) : null);
}

export function setBusy(button, busy, busyLabel) {
  if (!button) return;
  if (busy) {
    button.dataset.label = button.textContent;
    if (busyLabel) button.textContent = busyLabel;
    button.setAttribute('aria-busy', 'true');
    button.disabled = true;
  } else {
    if (button.dataset.label) button.textContent = button.dataset.label;
    button.removeAttribute('aria-busy');
    button.disabled = false;
  }
}

/**
 * Shows server field errors next to their inputs. `map` turns a server field
 * path (passengers[0].name) into an input element, or null.
 */
export function showFieldErrors(form, fieldErrors, map) {
  clearFieldErrors(form);
  let first = null;
  for (const [field, message] of Object.entries(fieldErrors || {})) {
    const input = map ? map(field) : form.querySelector(`[name="${CSS.escape(field)}"]`);
    if (!input) continue;
    setFieldError(input, message);
    first = first || input;
  }
  if (first) first.focus();
  return Boolean(first);
}

export function setFieldError(input, message) {
  input.setAttribute('aria-invalid', 'true');
  const help = document.getElementById(input.getAttribute('aria-describedby')?.split(' ')[0] || '')
    || input.closest('.field')?.querySelector('.help');
  if (help) {
    help.textContent = message;
    help.classList.add('is-error');
  }
}

export function clearFieldErrors(root) {
  $$('[aria-invalid="true"]', root).forEach((el) => el.removeAttribute('aria-invalid'));
  $$('.help.is-error', root).forEach((el) => {
    el.classList.remove('is-error');
    el.textContent = el.dataset.hint || '';
  });
}

export function params() {
  return new URLSearchParams(location.search);
}

export function setTitle(text) {
  document.title = `${text} | Railway Reservation System`;
}

/** Wires an ARIA tablist: click or arrow keys switch panels. Returns select(id). */
export function tabs(tablist, onChange) {
  const tabEls = $$('[role="tab"]', tablist);
  const select = (tab, focus = false) => {
    tabEls.forEach((t) => {
      const on = t === tab;
      t.setAttribute('aria-selected', String(on));
      t.tabIndex = on ? 0 : -1;
      const panel = document.getElementById(t.getAttribute('aria-controls'));
      if (panel) panel.hidden = !on;
    });
    if (focus) tab.focus();
    if (onChange) onChange(tab);
  };
  tabEls.forEach((t, i) => {
    t.addEventListener('click', () => select(t));
    t.addEventListener('keydown', (e) => {
      const d = e.key === 'ArrowRight' ? 1 : e.key === 'ArrowLeft' ? -1 : 0;
      if (d) { e.preventDefault(); select(tabEls[(i + d + tabEls.length) % tabEls.length], true); }
    });
  });
  return (id) => { const t = tabEls.find((x) => x.id === id); if (t) select(t); };
}
