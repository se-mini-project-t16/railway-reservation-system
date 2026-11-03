import { mountShell } from '../core/shell.js';
import { api, ApiError } from '../core/api.js';
import { signIn, safeNext, isSignedIn } from '../core/session.js';
import { $, params, notice, setBusy, setFieldError, clearFieldErrors, showFieldErrors, tabs, setTitle } from '../core/ui.js';

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
  setBusy(btn, true, 'Creating account');
  try {
    await api('/users/register', { method: 'POST', body, redirectOn401: false });
    await signIn(body.email, body.password);
    location.assign(next);
  } catch (ex) {
    setBusy(btn, false);
    if (ex.status === 409) {
      setFieldError($('#su-email'), 'An account with this email already exists. Sign in instead.');
      $('#su-email').focus();
      return;
    }
    const map = { name: '#su-name', email: '#su-email', phone: '#su-phone', password: '#su-password' };
    if (!showFieldErrors(form, ex.fieldErrors, (f) => (map[f] ? $(map[f]) : null))) {
      err.append(notice('error', 'Could not create the account.', ex.message));
    }
  }
});
