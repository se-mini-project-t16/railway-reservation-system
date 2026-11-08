// Google's sign-in button (Google Identity Services). The page only receives a signed ID token;
// the server verifies it before anything happens. Loaded only on pages whose policy allows it.
import { api } from './api.js';

const SCRIPT = 'https://accounts.google.com/gsi/client';
let options;
let loading;
let onCredential = () => {};

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
 * Draws Google's button into `el`. `handler(credential)` runs with the ID token after the
 * person picks an account. Returns false when Google sign-in is off or could not load.
 */
export async function renderGoogleButton(el, handler, { text = 'signin_with' } = {}) {
  const clientId = await googleClientId();
  if (!clientId) return false;
  let google;
  try { google = await loadScript(); } catch { return false; }
  onCredential = handler;
  google.accounts.id.initialize({
    client_id: clientId,
    callback: (response) => onCredential(response.credential),
    ux_mode: 'popup',
    auto_select: false,
    cancel_on_tap_outside: true,
    context: 'signin',
  });
  el.replaceChildren();
  const width = Math.min(400, Math.max(200, Math.floor(el.getBoundingClientRect().width || 320)));
  google.accounts.id.renderButton(el, {
    type: 'standard', theme: dark() ? 'filled_black' : 'outline', size: 'large', shape: 'rectangular',
    text, logo_alignment: 'left', width,
  });
  return true;
}
