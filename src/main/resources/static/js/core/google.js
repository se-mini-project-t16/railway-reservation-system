// Google's sign-in button (Google Identity Services). The page only receives a signed ID token;
// the server verifies it before anything happens. Loaded only on pages whose policy allows it.
import { api } from './api.js';

const SCRIPT = 'https://accounts.google.com/gsi/client';
const READY_TIMEOUT_MS = 10000;
let options;
let loading;
let initialisedFor;
let onCredential = () => {};
const drawn = new Map(); // container -> button text, redrawn when the theme changes

/** The Google client ID, or null when Google sign-in is switched off on the server. */
export async function googleClientId() {
  if (options === undefined) {
    try { options = await api('/users/sign-in-options', { redirectOn401: false }); } catch { options = {}; }
  }
  return options.googleClientId || null;
}

function loadScript() {
  if (window.google?.accounts?.id) return Promise.resolve(window.google);
  if (!loading) {
    loading = new Promise((resolve, reject) => {
      const s = document.createElement('script');
      s.src = SCRIPT;
      s.async = true;
      s.onload = () => (window.google?.accounts?.id ? resolve(window.google) : reject(new Error('Google sign-in did not load.')));
      s.onerror = () => { loading = null; reject(new Error('Google sign-in could not be loaded.')); };
      document.head.append(s);
    });
  }
  return loading;
}

function dark() {
  const mode = document.documentElement.getAttribute('data-mode');
  if (mode) return mode === 'dark';
  return window.matchMedia('(prefers-color-scheme: dark)').matches;
}

/**
 * Draws (or redraws) Google's button and resolves true once Google's own frame has loaded.
 * Until then the slot stays hidden, so Google's unstyled placeholder never flashes on screen.
 */
function draw(google, el, text) {
  el.classList.remove('is-ready');
  el.replaceChildren();
  const width = Math.min(400, Math.max(200, Math.floor(el.getBoundingClientRect().width || 320)));
  google.accounts.id.renderButton(el, {
    type: 'standard', theme: dark() ? 'filled_black' : 'outline', size: 'large', shape: 'rectangular',
    text, logo_alignment: 'left', width,
  });
  return new Promise((resolve) => {
    const done = (ok) => { clearTimeout(timer); observer.disconnect(); if (ok) el.classList.add('is-ready'); resolve(ok); };
    const watchFrame = (frame) => frame.addEventListener('load', () => done(true), { once: true });
    const timer = setTimeout(() => done(false), READY_TIMEOUT_MS);
    const observer = new MutationObserver(() => {
      const frame = el.querySelector('iframe');
      if (frame) { observer.disconnect(); watchFrame(frame); }
    });
    const frame = el.querySelector('iframe');
    if (frame) watchFrame(frame); else observer.observe(el, { childList: true, subtree: true });
  });
}

let themeWatch = false;
function redrawOnThemeChange(google) {
  if (themeWatch) return;
  themeWatch = true;
  const redraw = () => drawn.forEach((text, el) => { if (el.isConnected) draw(google, el, text); else drawn.delete(el); });
  new MutationObserver(redraw).observe(document.documentElement, { attributes: true, attributeFilter: ['data-mode'] });
  window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', redraw);
}

/**
 * Draws Google's button into `el`. `handler(credential)` runs with the ID token after the
 * person picks an account. Resolves false when Google sign-in is off or Google's button could
 * not be shown, so the caller can hide the option instead of showing a broken one.
 */
export async function renderGoogleButton(el, handler, { text = 'signin_with' } = {}) {
  const clientId = await googleClientId();
  if (!clientId) return false;
  let google;
  try { google = await loadScript(); } catch { return false; }
  onCredential = handler;
  if (initialisedFor !== clientId) {
    google.accounts.id.initialize({
      client_id: clientId,
      callback: (response) => onCredential(response.credential),
      ux_mode: 'popup',
      auto_select: false,
      cancel_on_tap_outside: true,
      context: 'signin',
    });
    initialisedFor = clientId;
  }
  drawn.set(el, text);
  redrawOnThemeChange(google);
  return draw(google, el, text);
}
