import { mountShell } from '../core/shell.js';
import { api, ApiError, setToken } from '../core/api.js';
import { signIn, signInWithGoogle, safeNext, isSignedIn } from '../core/session.js';
import { renderGoogleButton } from '../core/google.js';
import { $, h, params, notice, setBusy, setFieldError, clearFieldErrors, showFieldErrors, tabs, setTitle } from '../core/ui.js';

const q = params();
const next = safeNext(q.get('next'), '/');

if (isSignedIn() && !q.get('reason')) location.replace(next);
mountShell();

const select = tabs($('[role="tablist"]'), (tab) => setTitle(tab.id === 'tab-up' ? 'Create an account' : 'Sign in'));
if (q.get('mode') === 'register') select('tab-up');

const note = $('#auth-note');
if (q.get('reason') === 'expired') {
  note.append(notice('info', 'Your session ended after 15 minutes without activity.', 'Sign in again to carry on where you were.'));
} else if (next.startsWith('/book.html')) {
  note.append(notice('info', 'Sign in to book.', 'Your chosen train, class and date are kept for when you return.'));
}
if (note.childElementCount) note.classList.add('auth-note');

const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

// ------------------------------------------------------------------ Google (linked accounts only)

let googleBusy = false;
async function googleCredential(credential) {
  if (googleBusy) return;
  googleBusy = true;
  const err = $('#g-error');
  err.replaceChildren();
  try {
    await signInWithGoogle(credential);
    location.assign(next);
  } catch (ex) {
    googleBusy = false;
    const title = ex.code === 'GOOGLE_NOT_LINKED' ? 'This Google account is not linked yet.' : 'Could not sign you in with Google.';
    err.append(notice('error', title, ex.message));
  }
}

// Shown before drawing so Google can size the button to the form; hidden again if Google sign-in is off
$('#google-signin').hidden = false;
renderGoogleButton($('#google-btn'), googleCredential).then((shown) => { $('#google-signin').hidden = !shown; });

$('#signin').addEventListener('submit', async (e) => {
  e.preventDefault();
  const form = e.currentTarget;
  clearFieldErrors(form);
  const email = $('#si-email').value.trim();
  const password = $('#si-password').value;
  const err = $('#si-error');
  err.replaceChildren();
  let bad = false;
  if (!EMAIL.test(email)) { setFieldError($('#si-email'), 'Enter the email you registered with.'); bad = true; }
  if (!password) { setFieldError($('#si-password'), 'Enter your password.'); bad = true; }
  if (bad) { form.querySelector('[aria-invalid="true"]').focus(); return; }
  const btn = form.querySelector('[type="submit"]');
  setBusy(btn, true, 'Signing in');
  try {
    await signIn(email, password);
    location.assign(next);
  } catch (ex) {
    setBusy(btn, false);
    const msg = ex instanceof ApiError && ex.status === 401
      ? 'That email and password do not match, or the account is locked for a few minutes after repeated attempts.'
      : ex.message;
    err.append(notice('error', 'Could not sign you in.', msg));
  }
});

// ------------------------------------------------------------------ create account: details, then the emailed code

let pendingEmail = '';
let resendTimer;

function resendCountdown(seconds) {
  const btn = $('#v-resend');
  clearInterval(resendTimer);
  let left = Math.max(0, Math.round(seconds));
  const tick = () => {
    btn.disabled = left > 0;
    btn.textContent = left > 0 ? `Send a new code (${left}s)` : 'Send a new code';
    if (left-- <= 0) clearInterval(resendTimer);
  };
  tick();
  resendTimer = setInterval(tick, 1000);
}

function showVerify(started) {
  pendingEmail = started.email;
  $('#signup').hidden = true;
  $('#verify').hidden = false;
  $('#panel-up h1').textContent = 'Check your email';
  $('#verify-sent').replaceChildren('We sent a 6-digit code to ', h('span', { class: 'strong' }, started.email),
    `. It works for ${Math.round(started.expiresInSeconds / 60)} minutes.`);
  $('#v-code').value = '';
  $('#v-error').replaceChildren();
  $('#v-status').textContent = '';
  clearFieldErrors($('#verify'));
  resendCountdown(started.resendInSeconds);
  $('#v-code').focus();
}

$('#signup').addEventListener('submit', async (e) => {
  e.preventDefault();
  const form = e.currentTarget;
  clearFieldErrors(form);
  const body = {
    name: $('#su-name').value.trim(),
    email: $('#su-email').value.trim(),
    phone: $('#su-phone').value.replace(/\D/g, ''),
    password: $('#su-password').value,
  };
  const err = $('#su-error');
  err.replaceChildren();
  const problems = [];
  if (body.name.length < 2) problems.push(['#su-name', 'Enter your full name.']);
  if (!EMAIL.test(body.email)) problems.push(['#su-email', 'Enter a valid email address.']);
  if (!/^[6-9]\d{9}$/.test(body.phone)) problems.push(['#su-phone', 'Enter a 10-digit Indian mobile number.']);
  if (body.password.length < 8) problems.push(['#su-password', 'Use at least 8 characters.']);
  if (problems.length) {
    problems.forEach(([sel, m]) => setFieldError($(sel), m));
    $(problems[0][0]).focus();
    return;
  }
  const btn = form.querySelector('[type="submit"]');
  setBusy(btn, true, 'Sending code');
  try {
    showVerify(await api('/users/register', { method: 'POST', body, redirectOn401: false }));
  } catch (ex) {
    if (ex.status === 409) {
      setFieldError($('#su-email'), 'An account with this email already exists. Sign in instead.');
      $('#su-email').focus();
      return;
    }
    if (ex.code === 'EMAIL_NOT_SENT') {
      setFieldError($('#su-email'), ex.message);
      $('#su-email').focus();
      return;
    }
    const map = { name: '#su-name', email: '#su-email', phone: '#su-phone', password: '#su-password' };
    if (!showFieldErrors(form, ex.fieldErrors, (f) => (map[f] ? $(map[f]) : null))) {
      err.append(notice('error', 'Could not send the code.', ex.message));
    }
  } finally {
    setBusy(btn, false);
  }
});

$('#v-code').addEventListener('input', (e) => { e.target.value = e.target.value.replace(/\D/g, '').slice(0, 6); });

$('#verify').addEventListener('submit', async (e) => {
  e.preventDefault();
  const form = e.currentTarget;
  clearFieldErrors(form);
  $('#v-error').replaceChildren();
  const code = $('#v-code').value.trim();
  if (!/^\d{6}$/.test(code)) { setFieldError($('#v-code'), 'Enter the 6-digit code from the email.'); $('#v-code').focus(); return; }
  const btn = form.querySelector('[type="submit"]');
  setBusy(btn, true, 'Creating account');
  try {
    const { token } = await api('/users/register/verify', { method: 'POST', body: { email: pendingEmail, code }, redirectOn401: false });
    setToken(token);
    try { sessionStorage.removeItem('rrs.me'); } catch { /* ignore */ }
    clearInterval(resendTimer);
    location.assign(next);
  } catch (ex) {
    setBusy(btn, false);
    if (ex.code === 'CODE_INVALID') { setFieldError($('#v-code'), ex.message); $('#v-code').select(); return; }
    if (ex.code === 'CODE_EXPIRED' || ex.code === 'CODE_USED_UP') { resendCountdown(0); setFieldError($('#v-code'), ex.message); $('#v-resend').focus(); return; }
    if (ex.status === 409) { $('#v-error').append(notice('error', 'This email already has an account.', 'Sign in instead.')); return; }
    $('#v-error').append(notice('error', 'Could not create the account.', ex.message));
  }
});

$('#v-resend').addEventListener('click', async (e) => {
  const btn = e.currentTarget;
  $('#v-error').replaceChildren();
  setBusy(btn, true, 'Sending');
  try {
    const started = await api('/users/register/resend', { method: 'POST', body: { email: pendingEmail }, redirectOn401: false });
    setBusy(btn, false);
    showVerify(started);
    $('#v-status').textContent = 'A new code is on its way. Only the newest code works.';
  } catch (ex) {
    setBusy(btn, false);
    if (ex.code === 'RESEND_TOO_SOON') { resendCountdown(Number((ex.message.match(/(\d+) seconds/) || [])[1]) || 60); }
    $('#v-error').append(notice('error', 'No new code was sent.', ex.message));
  }
});

$('#v-back').addEventListener('click', () => {
  clearInterval(resendTimer);
  $('#verify').hidden = true;
  $('#signup').hidden = false;
  $('#panel-up h1').textContent = 'Create an account';
  $('#su-email').focus();
});
