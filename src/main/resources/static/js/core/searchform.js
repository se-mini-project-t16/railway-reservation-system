// The journey search form used on the home page and the results page.
import { icon, todayIso, addDays, fmtDate, setFieldError, clearFieldErrors } from './ui.js';
import { stationCombobox } from './stations.js';

const LAST = 'rrs.lastSearch';

export async function setupSearchForm(form, { defaults = {} } = {}) {
  const date = form.querySelector('[name="date"], #date');
  const today = todayIso();
  date.min = today;
  date.max = addDays(today, 60);

  let last = {};
  try { last = JSON.parse(localStorage.getItem(LAST) || '{}'); } catch { /* ignore */ }
  const initial = { ...last, ...defaults };

  const fromInput = form.querySelector('#from');
  const toInput = form.querySelector('#to');
  const [from, to] = await Promise.all([stationCombobox(fromInput), stationCombobox(toInput)]);
  if (initial.from) from.set(initial.from, { silent: true });
  if (initial.to) to.set(initial.to, { silent: true });
  date.value = initial.date && initial.date >= today && initial.date <= date.max ? initial.date : addDays(today, 1);
  if (initial.quota === 'TQ') { const tq = form.querySelector('#q-tq'); if (tq) tq.checked = true; }
  if (initial.class) { const cls = form.querySelector('[name="class"]'); if (cls) cls.value = initial.class; }

  const swap = form.querySelector('#swap');
  if (swap) {
    swap.append(icon('swap'));
    swap.addEventListener('click', () => {
      const a = from.value, b = to.value;
      from.set(b, { silent: true });
      to.set(a, { silent: true });
    });
  }

  form.addEventListener('submit', (e) => {
    clearFieldErrors(form);
    const problems = [];
    if (!from.value) problems.push([fromInput, 'Choose a departure station from the list.']);
    if (!to.value) problems.push([toInput, 'Choose an arrival station from the list.']);
    if (from.value && from.value === to.value) problems.push([toInput, 'Choose a different arrival station.']);
    if (!date.value || date.value < today) problems.push([date, 'Choose today or a later date.']);
    else if (date.value > date.max) problems.push([date, `Reservations open 60 days ahead, up to ${fmtDate(date.max)}.`]);
    if (problems.length) {
      e.preventDefault();
      problems.forEach(([input, msg]) => setFieldError(input, msg));
      problems[0][0].focus();
      return;
    }
    try {
      localStorage.setItem(LAST, JSON.stringify({
        from: from.value, to: to.value, date: date.value,
        quota: form.querySelector('[name="quota"]:checked')?.value,
        class: form.querySelector('[name="class"]')?.value,
      }));
    } catch { /* ignore */ }
  });
  return { from, to, date };
}

