// The persistent header and footer on every passenger page.
import { $, h, icon, LOGO } from './ui.js';
import { me, signOut, isSignedIn } from './session.js';

const NAV = [
  ['/', 'Find trains'],
  ['/pnr.html', 'PNR status'],
  ['/train.html', 'Train schedule'],
  ['/station.html', 'Station board'],
  ['/bookings.html', 'My trips'],
];

function currentMode() {
  const set = document.documentElement.getAttribute('data-mode');
  if (set) return set;
  return matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
}

export function themeToggle() {
  const btn = h('button', { type: 'button', class: 'icon-btn' });
  const paint = () => {
    const dark = currentMode() === 'dark';
    btn.replaceChildren(icon(dark ? 'sun' : 'moon'));
    btn.setAttribute('aria-label', dark ? 'Switch to light mode' : 'Switch to dark mode');
    btn.title = btn.getAttribute('aria-label');
  };
  btn.addEventListener('click', () => {
    const next = currentMode() === 'dark' ? 'light' : 'dark';
    document.documentElement.setAttribute('data-mode', next);
    try { localStorage.setItem('rrs.mode', next); } catch { /* ignore */ }
    paint();
  });
  paint();
  return btn;
}

function wordmark() {
  const a = h('a', { href: '/', class: 'wordmark', 'aria-label': 'Railway Reservation System, home' });
  a.innerHTML = LOGO;
  a.append(h('span', {}, 'Railway Reservation'));
  return a;
}

async function accountArea() {
  const wrap = h('div', { class: 'account' });
  if (!isSignedIn()) {
    const next = encodeURIComponent(location.pathname + location.search);
    const onAuthPage = location.pathname === '/signin.html';
    wrap.append(h('a', { class: 'btn', href: onAuthPage ? '/signin.html' : `/signin.html?next=${next}` }, 'Sign in'));
    return wrap;
  }
  const profile = await me();
  const btn = h('button', { type: 'button', class: 'icon-btn', 'aria-haspopup': 'true', 'aria-expanded': 'false',
    'aria-label': profile ? `Account: ${profile.name}` : 'Account' }, icon('user'));
  const menu = h('div', { class: 'account-menu', hidden: true });
  if (profile) menu.append(h('div', { class: 'who' }, h('strong', {}, profile.name), h('span', {}, profile.email)));
  menu.append(h('a', { href: '/bookings.html' }, 'My trips'), h('a', { href: '/account.html' }, 'Account and travellers'));
  if (profile && profile.role === 'ADMIN') menu.append(h('a', { href: '/admin/' }, 'Operations console'));
  menu.append(h('button', { type: 'button', onclick: () => { signOut(); location.assign('/'); } }, 'Sign out'));
  const toggle = (open) => {
    menu.hidden = !open;
    btn.setAttribute('aria-expanded', String(open));
  };
  btn.addEventListener('click', () => toggle(menu.hidden));
  document.addEventListener('click', (e) => { if (!wrap.contains(e.target)) toggle(false); });
  wrap.addEventListener('keydown', (e) => { if (e.key === 'Escape') { toggle(false); btn.focus(); } });
  wrap.append(btn, menu);
  return wrap;
}

export async function mountShell() {
  const header = $('header.site-header');
  if (header) {
    const nav = h('nav', { class: 'nav', id: 'site-nav', 'aria-label': 'Main' });
    const here = location.pathname.replace(/index\.html$/, '');
    for (const [href, text] of NAV) {
      nav.append(h('a', { href, 'aria-current': here === href ? 'page' : null }, text));
    }
    const menuBtn = h('button', { type: 'button', class: 'icon-btn menu-btn', 'aria-controls': 'site-nav',
      'aria-expanded': 'false', 'aria-label': 'Menu' }, icon('menu'));
    menuBtn.addEventListener('click', () => {
      const open = !nav.classList.contains('is-open');
      nav.classList.toggle('is-open', open);
      menuBtn.setAttribute('aria-expanded', String(open));
      menuBtn.replaceChildren(icon(open ? 'close' : 'menu'));
    });
    const tools = h('div', { class: 'nav-tools' }, themeToggle(), await accountArea(), menuBtn);
    header.replaceChildren(h('div', { class: 'container' }, wordmark(), nav, tools));
  }
  const footer = $('footer.site-footer');
  if (footer) {
    const links = h('nav', { 'aria-label': 'Footer' },
      h('a', { href: '/' }, 'Find trains'), h('a', { href: '/pnr.html' }, 'PNR status'),
      h('a', { href: '/train.html' }, 'Train schedule'), h('a', { href: '/refunds.html' }, 'Refund rules'),
      h('a', { href: '/bookings.html' }, 'My trips'));
    footer.replaceChildren(h('div', { class: 'container' }, wordmark(), links,
      h('p', { class: 'notice-line' }, 'Timetables and fares are close approximations. Payments run in Razorpay test mode, so no real money moves, and tickets issued here are not valid for travel.')));
  }
}
