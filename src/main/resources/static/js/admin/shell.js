// The operations console shell: its own bar and tabs, and a gate that only lets ADMIN accounts in.
// The server enforces the role on every /api/v1/admin route; this gate only shapes the page.
import { $, h, LOGO } from '../core/ui.js';
import { me, signOut, requireSignIn } from '../core/session.js';
import { themeToggle } from '../core/shell.js';

const TABS = [
  ['/admin/', 'Overview'],
  ['/admin/trains.html', 'Trains'],
  ['/admin/bookings.html', 'Bookings'],
  ['/admin/chart.html', 'Reservation chart'],
  ['/admin/stations.html', 'Stations'],
  ['/admin/users.html', 'Users'],
  ['/admin/rules.html', 'Refund rules'],
];

/** Mounts the shell; resolves to the admin profile, or null when the visitor may not use the console. */
export async function mountAdmin(active) {
  if (!requireSignIn()) return null;
  const profile = await me({ fresh: true });
  const bar = $('header.admin-bar');
  const wm = h('a', { href: '/admin/', class: 'wordmark', 'aria-label': 'Operations console home' });
  wm.innerHTML = LOGO;
  wm.append(h('span', {}, 'Operations console'));
  const out = h('button', { type: 'button', class: 'btn', onclick: () => { signOut(); location.assign('/'); } }, 'Sign out');
  out.classList.add('admin-out');
  bar.replaceChildren(h('div', { class: 'container' }, wm,
    h('span', { class: 'who', style: null }, profile ? profile.email : ''),
    h('div', { class: 'nav-tools' }, h('a', { class: 'btn admin-out', href: '/' }, 'Passenger site'), themeToggle(), out)));
  const tabs = $('nav.admin-tabs');
  if (!profile || profile.role !== 'ADMIN') {
    tabs.remove();
    $('main').replaceChildren(h('div', { class: 'container page' }, h('div', { class: 'empty-state' },
      h('h1', { class: 'section-title' }, 'This console is for operations staff.'),
      h('p', {}, `You are signed in as ${profile ? profile.email : 'a passenger'}. Sign in with an administrator account to continue.`),
      h('div', { class: 'row' }, h('a', { class: 'btn btn--primary', href: '/' }, 'Go to the passenger site'),
        h('button', { type: 'button', class: 'btn', onclick: () => { signOut(); location.assign(`/signin.html?next=${encodeURIComponent(location.pathname)}`); } }, 'Use another account')))));
    return null;
  }
  tabs.replaceChildren(h('div', { class: 'container', style: null },
    ...TABS.map(([href, text]) => h('a', { href, 'aria-current': href === active ? 'page' : null }, text))));
  return profile;
}
