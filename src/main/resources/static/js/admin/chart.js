import { mountAdmin } from './shell.js';
import { api } from '../core/api.js';
import { $, h, fmtDate, fmtDateTime, todayIso, addDays, params, notice, setBusy, GENDERS } from '../core/ui.js';

const q = params();
const out = $('#chart');
const trainSel = $('#c-train');
const classSel = $('#c-class');
const dateInput = $('#c-date');
let trains = [];

function fillClasses(number, wanted) {
  const t = trains.find((x) => x.number === number);
  classSel.replaceChildren(...(t ? t.classes : []).map((c) => h('option', { value: c, selected: c === wanted }, c)));
}

function occupantList(berthLabel, occ) {
  return h('div', { class: 'detail', role: 'region', 'aria-label': `Berth ${berthLabel}` },
    h('div', { class: 'row row--between' }, h('strong', {}, `Berth ${berthLabel}`),
      h('button', { type: 'button', class: 'btn btn--quiet', onclick: (e) => e.currentTarget.closest('.detail').remove() }, 'Close')),
    h('table', { class: 'table' },
      h('thead', {}, h('tr', {}, h('th', {}, 'PNR'), h('th', {}, 'Traveller'), h('th', {}, 'From'), h('th', {}, 'To'))),
      h('tbody', {}, occ.map((o) => h('tr', {},
        h('td', {}, h('a', { href: `/admin/booking.html?pnr=${o.pnr}` }, o.pnr)),
        h('td', {}, `${o.name}, ${o.age}, ${GENDERS[o.gender]}`, o.bookingStatus === 'PAYMENT_PENDING' ? h('div', { class: 'small muted' }, 'Held, awaiting payment') : null),
        h('td', {}, o.from), h('td', {}, o.to))))));
}

function queueTable(title, rows) {
  return h('section', { 'aria-label': title }, h('h2', { class: 'section-title' }, title),
    rows.length ? h('div', { class: 'table-wrap' }, h('table', { class: 'table' },
      h('thead', {}, h('tr', {}, h('th', { class: 'num' }, 'No.'), h('th', {}, 'PNR'), h('th', {}, 'Traveller'), h('th', {}, 'From'), h('th', {}, 'To'), h('th', {}, 'List'))),
      h('tbody', {}, rows.map((r) => h('tr', {}, h('td', { class: 'num strong' }, r.number),
        h('td', {}, h('a', { href: `/admin/booking.html?pnr=${r.pnr}` }, r.pnr)), h('td', {}, `${r.name}, ${r.age}`),
        h('td', {}, r.from), h('td', {}, r.to), h('td', {}, r.status)))))) : h('p', { class: 'muted' }, 'Nobody from this system is on this list.'));
}

async function load() {
  const train = trainSel.value;
  const date = dateInput.value;
  const cls = classSel.value;
  history.replaceState(null, '', `/admin/chart.html?train=${train}&date=${date}&class=${cls}`);
  out.replaceChildren(h('div', { class: 'skeleton-row' }));
  try {
    const c = await api(`/admin/chart?train=${train}&date=${date}&class=${cls}`);
    const t = trains.find((x) => x.number === train);
    const detailSlot = h('div', { 'aria-live': 'polite' });
    const coaches = c.coaches.map((coach) => h('section', { class: 'coach', 'aria-label': `Coach ${coach.coach}` },
      h('h3', {}, coach.coach),
      h('div', { class: 'berths' }, coach.berths.map((b) => {
        const label = `${coach.coach} ${b.berth} ${b.type}`;
        if (b.occupants.length) {
          const whole = b.occupants.some((o) => o.from === t?.originCode && o.to === t?.terminusCode);
          const cell = h('button', { type: 'button', class: `berth ${whole ? 'is-booked' : 'is-part'}`, 'aria-label': `${label}, booked by ${b.occupants.map((o) => o.name).join(' and ')}` },
            b.berth, h('small', {}, b.type));
          cell.addEventListener('click', () => { detailSlot.replaceChildren(occupantList(label, b.occupants)); detailSlot.scrollIntoView({ block: 'nearest' }); });
          return cell;
        }
        return h('span', { class: `berth ${b.otherChannel ? 'is-other' : ''}`, title: `${label}, ${b.otherChannel ? 'sold through other channels' : 'free'}` },
          b.berth, h('small', {}, b.type));
      }))));
    out.replaceChildren(
      h('dl', { class: 'facts' },
        h('div', {}, h('dt', {}, 'Departure'), h('dd', {}, `${c.trainNumber} ${c.trainName}, ${fmtDate(c.startDate)}`)),
        h('div', {}, h('dt', {}, 'Chart'), h('dd', {}, c.chartPrepared ? 'Prepared' : `Prepared at ${fmtDateTime(c.chartAt)}`)),
        h('div', {}, h('dt', {}, `${c.classLabel} berths`), h('dd', {}, `${c.capacity}, of which ${c.tatkalBerths} Tatkal`)),
        h('div', {}, h('dt', {}, 'Booked on this system'), h('dd', {}, `${c.systemTravellers} travellers`)),
        h('div', {}, h('dt', {}, 'Other channels'), h('dd', {}, `${c.otherChannelBerths} berths, RAC ${c.otherChannelRac}, WL ${c.otherChannelWl}`)),
        h('div', {}, h('dt', {}, 'Free end to end'), h('dd', {}, c.freeWholeRun))),
      h('div', { class: 'legend', style: null },
        h('span', {}, h('i', { 'aria-hidden': 'true' }), 'Free'),
        h('span', {}, h('i', { class: 'is-booked', 'aria-hidden': 'true' }), 'Booked here, whole route'),
        h('span', {}, h('i', { class: 'is-part', 'aria-hidden': 'true' }), 'Booked here, part of the route'),
        h('span', {}, h('i', { class: 'is-other', 'aria-hidden': 'true' }), 'Sold through other channels (simulated demand)')),
      detailSlot,
      ...coaches,
      h('div', { class: 'admin-grid' }, queueTable('RAC', c.rac), queueTable('Waiting list', c.waitlist)));
  } catch (err) {
    out.replaceChildren(notice('error', 'The chart could not load.', err.message, h('button', { type: 'button', class: 'btn', onclick: load }, 'Try again')));
  }
}

$('#cf').addEventListener('submit', (e) => { e.preventDefault(); load(); });
trainSel.addEventListener('change', () => fillClasses(trainSel.value));
$('#csv').addEventListener('click', async (e) => {
  const btn = e.currentTarget;
  setBusy(btn, true, 'Preparing');
  try {
    const res = await api(`/admin/chart.csv?train=${trainSel.value}&date=${dateInput.value}&class=${classSel.value}`, { raw: true });
    const href = URL.createObjectURL(await res.blob());
    const a = h('a', { href, download: `chart-${trainSel.value}-${dateInput.value}-${classSel.value}.csv` });
    document.body.append(a); a.click(); a.remove();
    setTimeout(() => URL.revokeObjectURL(href), 2000);
  } catch (err) {
    out.prepend(notice('error', 'The CSV could not be prepared.', err.message));
  } finally { setBusy(btn, false); }
});

async function start() {
  try {
    trains = await api('/admin/trains');
  } catch (err) {
    out.replaceChildren(notice('error', 'Trains could not load.', err.message));
    return;
  }
  const wanted = q.get('train') || trains[0]?.number;
  trainSel.replaceChildren(...trains.map((t) => h('option', { value: t.number, selected: t.number === wanted }, `${t.number} ${t.name}`)));
  fillClasses(trainSel.value, q.get('class'));
  dateInput.min = addDays(todayIso(), -3);
  dateInput.max = addDays(todayIso(), 60);
  dateInput.value = q.get('date') || todayIso();
  load();
}

mountAdmin('/admin/chart.html').then((ok) => { if (ok) start(); });
