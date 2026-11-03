import { mountAdmin } from './shell.js';
import { api } from '../core/api.js';
import { renderTicket } from '../pages/ticket.js';
import { $, h, notice, params, setTitle } from '../core/ui.js';

const pnr = (params().get('pnr') || '').replace(/\D/g, '');
const root = $('#ticket-root');

async function load(flash) {
  try {
    const b = await api(`/admin/bookings/${pnr}`);
    setTitle(`PNR ${b.pnr}`);
    const contact = h('p', { class: 'small muted' }, `Account: ${b.bookedBy}, ${b.contactEmail}. Created ${new Date(b.createdAt).toLocaleString('en-IN')}.`);
    renderTicket(root, b, { apiBase: '/admin/bookings', onChange: (updated, note) => load(note), flash });
    root.prepend(contact);
  } catch (err) {
    root.replaceChildren(notice('error', 'This booking could not load.', err.message, h('a', { class: 'btn', href: '/admin/bookings.html' }, 'All bookings')));
  }
}

mountAdmin('/admin/bookings.html').then((ok) => { if (ok) load(); });
