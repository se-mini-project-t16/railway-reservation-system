// One way to talk to the server: JSON in and out, the session token attached,
// sliding renewal picked up, and errors turned into ApiError with a stable code.

const TOKEN_KEY = 'rrs.token';

export class ApiError extends Error {
  constructor(status, code, message, fieldErrors) {
    super(message || 'Something went wrong.');
    this.status = status;
    this.code = code || 'ERROR';
    this.fieldErrors = fieldErrors || {};
  }
}

export function getToken() {
  try { return localStorage.getItem(TOKEN_KEY); } catch { return null; }
}

export function setToken(token) {
  try {
    if (token) localStorage.setItem(TOKEN_KEY, token);
    else localStorage.removeItem(TOKEN_KEY);
  } catch { /* storage blocked */ }
}

/**
 * @param {string} path  API path after /api/v1
 * @param {{method?: string, body?: any, signal?: AbortSignal, raw?: boolean, redirectOn401?: boolean}} opts
 */
export async function api(path, opts = {}) {
  const { method = 'GET', body, signal, raw = false, redirectOn401 = true } = opts;
  const headers = { Accept: raw ? '*/*' : 'application/json' };
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  const token = getToken();
  if (token) headers.Authorization = `Bearer ${token}`;

  let res;
  try {
    res = await fetch(`/api/v1${path}`, { method, headers, signal, body: body === undefined ? undefined : JSON.stringify(body) });
  } catch (e) {
    if (e.name === 'AbortError') throw e;
    throw new ApiError(0, 'NETWORK', 'We could not reach the server. Check your connection and try again.');
  }

  const renewed = res.headers.get('X-Auth-Token');
  if (renewed) setToken(renewed);

  if (res.status === 401) {
    let payload = {};
    try { payload = await res.json(); } catch { /* empty */ }
    setToken(null);
    try { sessionStorage.removeItem('rrs.me'); } catch { /* ignore */ }
    if (redirectOn401 && !path.startsWith('/users/')) {
      const next = location.pathname + location.search;
      const reason = payload.code === 'SESSION_EXPIRED' ? '&reason=expired' : '';
      location.assign(`/signin.html?next=${encodeURIComponent(next)}${reason}`);
    }
    throw new ApiError(401, payload.code || 'SIGN_IN_REQUIRED', payload.message || 'Sign in to continue.');
  }

  if (!res.ok) {
    let payload = {};
    try { payload = await res.json(); } catch { /* not JSON */ }
    throw new ApiError(res.status, payload.code, payload.message || `Request failed (${res.status}).`, payload.fieldErrors);
  }
  if (raw) return res;
  if (res.status === 204) return null;
  return res.json();
}
