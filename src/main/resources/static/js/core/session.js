// Who is signed in. The server decides access; this only shapes the page.
import { api, getToken, setToken } from './api.js';

const ME_KEY = 'rrs.me';

export function isSignedIn() {
  return Boolean(getToken());
}

export async function me({ fresh = false } = {}) {
  if (!getToken()) return null;
  if (!fresh) {
    try {
      const cached = sessionStorage.getItem(ME_KEY);
      if (cached) return JSON.parse(cached);
    } catch { /* ignore */ }
  }
  try {
    const profile = await api('/me', { redirectOn401: false });
    try { sessionStorage.setItem(ME_KEY, JSON.stringify(profile)); } catch { /* ignore */ }
    return profile;
  } catch {
    return null;
  }
}

export async function signIn(email, password) {
  const { token } = await api('/users/login', { method: 'POST', body: { email, password }, redirectOn401: false });
  setToken(token);
  try { sessionStorage.removeItem(ME_KEY); } catch { /* ignore */ }
  return me({ fresh: true });
}

/** Signs in with a Google account the owner has already linked to their account here. */
export async function signInWithGoogle(credential) {
  const { token } = await api('/users/login/google', { method: 'POST', body: { credential }, redirectOn401: false });
  setToken(token);
  try { sessionStorage.removeItem(ME_KEY); } catch { /* ignore */ }
  return me({ fresh: true });
}

export function signOut() {
  setToken(null);
  try { sessionStorage.removeItem(ME_KEY); } catch { /* ignore */ }
}

/** Sends a guest to sign in and back here afterwards. Returns false when the page should stop. */
export function requireSignIn() {
  if (getToken()) return true;
  location.replace(`/signin.html?next=${encodeURIComponent(location.pathname + location.search)}`);
  return false;
}

/** Only a safe, same-site path may be used as the place to return to. */
export function safeNext(raw, fallback = '/') {
  if (!raw || !raw.startsWith('/') || raw.startsWith('//') || raw.includes('://')) return fallback;
  return raw;
}
