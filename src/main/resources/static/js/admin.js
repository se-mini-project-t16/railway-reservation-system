/**
 * Railway Reservation System - Operations Command Center Logic (RRS-F-015-EXT)
 * Compliant with Hallmark Wayfind Platform and Role-Based Security RRS-SR-004.
 */

(function () {
  'use strict';

  // --- Auth State ---
  function getStoredToken() {
    return localStorage.getItem('rrs_token');
  }

  function getStoredUser() {
    try {
      const u = localStorage.getItem('rrs_user');
      return u ? JSON.parse(u) : null;
    } catch (e) {
      return null;
    }
  }

  function saveAuth(token, user) {
    if (token) localStorage.setItem('rrs_token', token);
    if (user) localStorage.setItem('rrs_user', typeof user === 'string' ? user : JSON.stringify(user));
  }

  function clearAuth() {
    localStorage.removeItem('rrs_token');
    localStorage.removeItem('rrs_user');
  }

  function isAdminUser() {
    const token = getStoredToken();
    const user = getStoredUser();
    if (!token) return false;
    if (user && user.role === 'ROLE_ADMIN') return true;
    try {
      const parts = token.split('.');
      if (parts.length === 3) {
        const payload = JSON.parse(atob(parts[1]));
        return payload.role === 'ROLE_ADMIN';
      }
    } catch (e) {
      return false;
    }
    return false;
  }

  async function authFetch(url, options = {}) {
    const token = getStoredToken();
    const headers = options.headers ? { ...options.headers } : {};
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }
    return fetch(url, { ...options, headers });
  }

  // --- Theme Management ---
  const themeToggleBtn = document.getElementById('adminThemeToggleBtn');
  const themeIconSun = document.getElementById('adminThemeIconSun');
  const themeIconMoon = document.getElementById('adminThemeIconMoon');

  function updateThemeUI(mode) {
    if (!themeToggleBtn) return;
    if (mode === 'dark') {
      if (themeIconSun) themeIconSun.style.display = 'inline-block';
      if (themeIconMoon) themeIconMoon.style.display = 'none';
    } else {
      if (themeIconSun) themeIconSun.style.display = 'none';
      if (themeIconMoon) themeIconMoon.style.display = 'inline-block';
    }
  }

  function initTheme() {
    const current = document.documentElement.getAttribute('data-mode') || 'dark';
    updateThemeUI(current);
    if (themeToggleBtn) {
      themeToggleBtn.addEventListener('click', () => {
        const active = document.documentElement.getAttribute('data-mode') || 'dark';
        const next = active === 'dark' ? 'light' : 'dark';
        document.documentElement.setAttribute('data-mode', next);
        localStorage.setItem('rrs_theme', next);
        updateThemeUI(next);
      });
    }
  }

  // --- Clock ---
  function initClock() {
    const clockEl = document.getElementById('adminLiveClock');
    if (!clockEl) return;
    const update = () => {
      const now = new Date();
      clockEl.textContent = now.toTimeString().split(' ')[0] + ' UTC' + (now.getTimezoneOffset() <= 0 ? '+' : '-') + String(Math.abs(now.getTimezoneOffset() / 60)).padStart(2, '0');
    };
    update();
    setInterval(update, 1000);
  }

  // --- Flash Messages ---
  const flashContainer = document.getElementById('adminFlashContainer');
  function showFlash(message, type = 'info') {
    if (!flashContainer) return;
    const banner = document.createElement('div');
    banner.className = `alert alert-${type}`;
    banner.style.padding = '0.75rem 1rem';
    banner.style.marginBottom = '0.75rem';
    banner.style.borderRadius = '4px';
    banner.style.border = '1px solid var(--color-rule)';
    banner.style.fontSize = '0.85rem';
    banner.style.display = 'flex';
    banner.style.justifyContent = 'space-between';
    banner.style.alignItems = 'center';

    if (type === 'danger') {
      banner.style.background = 'var(--color-paper-2)';
      banner.style.borderLeft = '3px solid var(--color-danger)';
    } else if (type === 'success') {
      banner.style.background = 'var(--color-paper-2)';
      banner.style.borderLeft = '3px solid var(--color-success)';
    } else {
      banner.style.background = 'var(--color-paper-2)';
      banner.style.borderLeft = '3px solid var(--color-accent)';
    }

    const text = document.createElement('span');
    text.textContent = message;
    banner.appendChild(text);

    const closeBtn = document.createElement('button');
    closeBtn.type = 'button';
    closeBtn.textContent = '×';
    closeBtn.style.background = 'none';
    closeBtn.style.border = 'none';
    closeBtn.style.cursor = 'pointer';
    closeBtn.style.fontSize = '1.2rem';
    closeBtn.style.color = 'var(--color-muted)';
    closeBtn.addEventListener('click', () => banner.remove());
    banner.appendChild(closeBtn);

    flashContainer.appendChild(banner);
    setTimeout(() => { if (banner.parentElement) banner.remove(); }, 6000);
  }

  // --- Tab Navigation ---
  function initTabs() {
    const tabs = document.querySelectorAll('.admin-nav-tab');
    const contents = document.querySelectorAll('.tab-content');

    tabs.forEach(tab => {
      tab.addEventListener('click', () => {
        const targetId = tab.getAttribute('data-tab');
        tabs.forEach(t => t.classList.remove('active'));
        contents.forEach(c => c.style.display = 'none');

        tab.classList.add('active');
        const target = document.getElementById(targetId);
        if (target) target.style.display = 'flex';

        if (targetId === 'tabFleet') loadFleetData();
        if (targetId === 'tabMatrix') populateMatrixDropdowns();
        if (targetId === 'tabChart') populateChartDropdowns();
      });
    });

    document.getElementById('btnOverviewAddTrain')?.addEventListener('click', () => {
      document.getElementById('btnTabFleet')?.click();
      openAddTrainModal();
    });
    document.getElementById('btnOverviewViewMatrix')?.addEventListener('click', () => {
      document.getElementById('btnTabMatrix')?.click();
    });
    document.getElementById('btnOverviewGenerateChart')?.addEventListener('click', () => {
      document.getElementById('btnTabChart')?.click();
    });
    document.getElementById('btnRefreshStats')?.addEventListener('click', loadDashboardStats);
  }

  // --- Auth Barrier ---
  const authBarrier = document.getElementById('adminAuthBarrier');
  const adminLoginForm = document.getElementById('adminLoginForm');
  const adminAuthEmail = document.getElementById('adminAuthEmail');
  const adminAuthPassword = document.getElementById('adminAuthPassword');
  const adminAuthSpinner = document.getElementById('adminAuthSpinner');
  const adminUserBadge = document.getElementById('adminUserBadge');
  const adminLogoutBtn = document.getElementById('adminLogoutBtn');

  function checkAdminAccess() {
    if (isAdminUser()) {
      if (authBarrier) authBarrier.style.display = 'none';
      const user = getStoredUser();
      if (adminUserBadge && user) {
        adminUserBadge.textContent = user.email || 'ROLE_ADMIN';
      }
      loadDashboardStats();
      loadFleetData();
    } else {
      if (authBarrier) authBarrier.style.display = 'flex';
    }
  }

  if (adminLoginForm) {
    adminLoginForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const email = adminAuthEmail.value.trim();
      const password = adminAuthPassword.value;

      if (adminAuthSpinner) adminAuthSpinner.style.display = 'inline-block';

      try {
        const res = await fetch('/api/v1/users/login', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ email, password })
        });

        if (res.ok) {
          const data = await res.json();
          saveAuth(data.token, data);
          if (data.role !== 'ROLE_ADMIN') {
            showFlash('Authenticated successfully, but this account lacks ROLE_ADMIN privileges.', 'danger');
            clearAuth();
            return;
          }
          showFlash('Authenticated as Operations Administrator.', 'success');
          checkAdminAccess();
        } else {
          const err = await res.json().catch(() => ({}));
          showFlash(err.message || 'Authentication failed. Please verify admin credentials.', 'danger');
        }
      } catch (err) {
        showFlash('Error contacting authentication gateway: ' + err.message, 'danger');
      } finally {
        if (adminAuthSpinner) adminAuthSpinner.style.display = 'none';
      }
    });
  }

  if (adminLogoutBtn) {
    adminLogoutBtn.addEventListener('click', () => {
      clearAuth();
      window.location.href = 'index.html';
    });
  }

  // --- Dashboard Stats (TAB 1) ---
  async function loadDashboardStats() {
    try {
      const res = await authFetch('/api/v1/admin/stats');
      if (!res.ok) return;
      const stats = await res.json();

      document.getElementById('kpiTodayBookings').textContent = String(stats.todayBookings ?? 0);
      document.getElementById('kpiTodayRevenue').textContent = '₹' + Number(stats.todayRevenue ?? 0).toFixed(2);
      document.getElementById('kpiActiveFleet').textContent = String(stats.activeFleetTrains ?? 0);
      document.getElementById('kpiAvgOccupancy').textContent = Number(stats.averageOccupancyRate ?? 0).toFixed(1) + '%';

      document.getElementById('kpiTotalBookings').textContent = String(stats.totalBookingsAllTime ?? 0);
      document.getElementById('kpiTotalRevenue').textContent = '₹' + Number(stats.totalRevenueAllTime ?? 0).toFixed(2);
      document.getElementById('kpiTodayCancellations').textContent = String(stats.totalCancellationsToday ?? 0);
      document.getElementById('kpiTodayRefunds').textContent = '₹' + Number(stats.totalRefundsToday ?? 0).toFixed(2);
    } catch (e) {
      console.error('Error fetching admin stats', e);
    }
  }

  // --- Fleet Management (TAB 2) ---
  let cachedFleet = [];

  async function loadFleetData() {
    const tbody = document.getElementById('fleetTableBody');
    if (!tbody) return;

    try {
      const res = await authFetch('/api/v1/admin/trains');
      if (!res.ok) {
        tbody.innerHTML = '<tr><td colspan="7" class="p-6 text-center text-muted">Failed to load fleet data.</td></tr>';
        return;
      }

      cachedFleet = await res.json();
      renderFleetTable(cachedFleet);
    } catch (e) {
      tbody.innerHTML = `<tr><td colspan="7" class="p-6 text-center text-muted">Error loading fleet: ${e.message}</td></tr>`;
    }
  }

  function renderFleetTable(trains) {
    const tbody = document.getElementById('fleetTableBody');
    if (!tbody) return;
    tbody.innerHTML = '';

    if (trains.length === 0) {
      tbody.innerHTML = '<tr><td colspan="7" class="p-6 text-center text-muted">No scheduled train corridors found.</td></tr>';
      return;
    }

    trains.forEach(t => {
      const tr = document.createElement('tr');
      tr.className = 'hover:bg-paper-2';

      // Classes badge list
      const classBadges = (t.classes || []).map(c =>
        `<span class="badge" style="background: var(--color-paper); border: 1px solid var(--color-rule); font-family: var(--font-mono); font-size: 0.7rem; padding: 1px 5px; margin-right: 3px;">${c.className} (${c.totalSeats} seats @ ₹${c.fare})</span>`
      ).join('');

      tr.innerHTML = `
        <td class="p-3 font-mono font-bold text-ink">${t.trainNo}</td>
        <td class="p-3 font-semibold text-ink">${t.name}</td>
        <td class="p-3">${t.sourceStation} &rarr; ${t.destinationStation}</td>
        <td class="p-3 font-mono text-muted">${t.departureTime || '-'} &rarr; ${t.arrivalTime || '-'}</td>
        <td class="p-3">${classBadges || '<span class="text-muted">None</span>'}</td>
        <td class="p-3 font-mono">${(t.stops && t.stops.length) ? t.stops.length + ' stops' : 'Direct'}</td>
        <td class="p-3 text-right">
          <div class="inline-flex gap-1.5">
            <button type="button" class="btn btn-sm btn-outline btn-edit-train" data-id="${t.id}" style="padding: 2px 8px; font-size: 0.75rem;">Edit</button>
            <button type="button" class="btn btn-sm btn-danger btn-delete-train" data-id="${t.id}" data-no="${t.trainNo}" style="padding: 2px 8px; font-size: 0.75rem;">Delete</button>
          </div>
        </td>
      `;

      tbody.appendChild(tr);
    });

    tbody.querySelectorAll('.btn-edit-train').forEach(btn => {
      btn.addEventListener('click', () => {
        const id = parseInt(btn.getAttribute('data-id'), 10);
        const train = cachedFleet.find(t => t.id === id);
        if (train) openEditTrainModal(train);
      });
    });

    tbody.querySelectorAll('.btn-delete-train').forEach(btn => {
      btn.addEventListener('click', () => {
        const id = parseInt(btn.getAttribute('data-id'), 10);
        const no = btn.getAttribute('data-no');
        handleDeleteTrain(id, no);
      });
    });
  }

  async function handleDeleteTrain(id, trainNo) {
    if (!window.confirm(`Are you sure you want to delete train corridor #${trainNo}?\n\nNote: Trains with active bookings cannot be deleted.`)) {
      return;
    }

    try {
      const res = await authFetch(`/api/v1/admin/trains/${id}`, { method: 'DELETE' });
      if (res.status === 204 || res.ok) {
        showFlash(`Train #${trainNo} deleted successfully.`, 'success');
        loadFleetData();
        loadDashboardStats();
      } else {
        const err = await res.json().catch(() => ({}));
        showFlash(err.message || `Failed to delete train #${trainNo}.`, 'danger');
      }
    } catch (e) {
      showFlash('Error deleting train: ' + e.message, 'danger');
    }
  }

  // --- Add/Edit Train Modal ---
  const trainFormModal = document.getElementById('trainFormModal');
  const btnCloseTrainModalBtn = document.getElementById('btnCloseTrainModalBtn');
  const btnCancelTrainModalBtn = document.getElementById('btnCancelTrainModalBtn');
  const btnOpenAddTrainModal = document.getElementById('btnOpenAddTrainModal');
  const trainForm = document.getElementById('trainForm');
  const editTrainId = document.getElementById('editTrainId');
  const formTrainNo = document.getElementById('formTrainNo');
  const formTrainName = document.getElementById('formTrainName');
  const formSourceStation = document.getElementById('formSourceStation');
  const formDestStation = document.getElementById('formDestStation');
  const formDepartureTime = document.getElementById('formDepartureTime');
  const formArrivalTime = document.getElementById('formArrivalTime');
  const formClassesContainer = document.getElementById('formClassesContainer');
  const trainModalModeText = document.getElementById('trainModalModeText');
  const saveTrainSpinner = document.getElementById('saveTrainSpinner');

  function closeTrainModal() {
    if (trainFormModal) trainFormModal.style.display = 'none';
  }

  if (btnCloseTrainModalBtn) btnCloseTrainModalBtn.addEventListener('click', closeTrainModal);
  if (btnCancelTrainModalBtn) btnCancelTrainModalBtn.addEventListener('click', closeTrainModal);
  if (btnOpenAddTrainModal) btnOpenAddTrainModal.addEventListener('click', openAddTrainModal);

  function openAddTrainModal() {
    editTrainId.value = '';
    trainModalModeText.textContent = 'Add New Train Corridor';
    formTrainNo.value = '';
    formTrainName.value = '';
    formSourceStation.value = '';
    formDestStation.value = '';
    formDepartureTime.value = '06:00:00';
    formArrivalTime.value = '12:00:00';
    renderFormClassRows([
      { className: 'CC', totalSeats: 75, fare: 450.0 },
      { className: '2S', totalSeats: 120, fare: 180.0 }
    ]);
    trainFormModal.style.display = 'flex';
  }

  function openEditTrainModal(t) {
    editTrainId.value = String(t.id);
    trainModalModeText.textContent = `Edit Train Corridor #${t.trainNo}`;
    formTrainNo.value = t.trainNo || '';
    formTrainName.value = t.name || '';
    formSourceStation.value = t.sourceStation || '';
    formDestStation.value = t.destinationStation || '';
    formDepartureTime.value = t.departureTime || '06:00:00';
    formArrivalTime.value = t.arrivalTime || '12:00:00';
    renderFormClassRows(t.classes && t.classes.length > 0 ? t.classes : [{ className: 'CC', totalSeats: 100, fare: 500.0 }]);
    trainFormModal.style.display = 'flex';
  }

  function renderFormClassRows(classes) {
    if (!formClassesContainer) return;
    formClassesContainer.innerHTML = '';

    const defaultClasses = ['CC', 'EC', '1A', '2A', '3A', 'SL', '2S'];

    classes.forEach((c, idx) => {
      const row = document.createElement('div');
      row.className = 'grid grid-cols-3 gap-2 items-center p-2 bg-paper-2 rounded border border-rule';

      row.innerHTML = `
        <div>
          <label class="text-xs text-muted">Class Code</label>
          <input type="text" class="form-control text-xs font-mono form-class-name" value="${c.className}" required>
        </div>
        <div>
          <label class="text-xs text-muted">Total Capacity</label>
          <input type="number" min="1" max="1000" class="form-control text-xs font-mono form-class-seats" value="${c.totalSeats}" required>
        </div>
        <div>
          <label class="text-xs text-muted">Base Fare (₹)</label>
          <input type="number" min="1" step="0.5" class="form-control text-xs font-mono form-class-fare" value="${c.fare}" required>
        </div>
      `;
      formClassesContainer.appendChild(row);
    });

    const addClassBtn = document.createElement('button');
    addClassBtn.type = 'button';
    addClassBtn.className = 'btn btn-sm btn-outline';
    addClassBtn.style.alignSelf = 'flex-start';
    addClassBtn.textContent = '+ Add Class';
    addClassBtn.addEventListener('click', () => {
      renderFormClassRows([...classes, { className: '3A', totalSeats: 64, fare: 850.0 }]);
    });
    formClassesContainer.appendChild(addClassBtn);
  }

  if (trainForm) {
    trainForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const isEdit = Boolean(editTrainId.value);

      const classRows = formClassesContainer.querySelectorAll('.grid');
      const classDtos = [];
      classRows.forEach(r => {
        const nameInput = r.querySelector('.form-class-name');
        const seatsInput = r.querySelector('.form-class-seats');
        const fareInput = r.querySelector('.form-class-fare');
        if (nameInput && seatsInput && fareInput) {
          classDtos.push({
            className: nameInput.value.trim().toUpperCase(),
            totalSeats: parseInt(seatsInput.value, 10),
            fare: parseFloat(fareInput.value)
          });
        }
      });

      if (classDtos.length === 0) {
        showFlash('Please configure at least one coach class quota.', 'warning');
        return;
      }

      const payload = {
        trainNo: formTrainNo.value.trim(),
        name: formTrainName.value.trim(),
        sourceStation: formSourceStation.value.trim(),
        destinationStation: formDestStation.value.trim(),
        departureTime: formDepartureTime.value.length === 5 ? formDepartureTime.value + ':00' : formDepartureTime.value,
        arrivalTime: formArrivalTime.value.length === 5 ? formArrivalTime.value + ':00' : formArrivalTime.value,
        classes: classDtos
      };

      if (saveTrainSpinner) saveTrainSpinner.style.display = 'inline-block';

      try {
        const url = isEdit ? `/api/v1/admin/trains/${editTrainId.value}` : '/api/v1/admin/trains';
        const method = isEdit ? 'PUT' : 'POST';

        const res = await authFetch(url, {
          method,
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(payload)
        });

        if (res.ok) {
          showFlash(`Train #${payload.trainNo} ${isEdit ? 'updated' : 'created'} successfully.`, 'success');
          closeTrainModal();
          loadFleetData();
          loadDashboardStats();
        } else {
          const err = await res.json().catch(() => ({}));
          showFlash(err.message || 'Failed to save train configuration.', 'danger');
        }
      } catch (err) {
        showFlash('Error saving train: ' + err.message, 'danger');
      } finally {
        if (saveTrainSpinner) saveTrainSpinner.style.display = 'none';
      }
    });
  }

  // --- 2D Coach Seat Matrix (TAB 3) ---
  const matrixTrainSelect = document.getElementById('matrixTrainSelect');
  const matrixDateInput = document.getElementById('matrixDateInput');
  const matrixClassSelect = document.getElementById('matrixClassSelect');
  const btnLoadMatrix = document.getElementById('btnLoadMatrix');
  const coachFloorplanContainer = document.getElementById('coachFloorplanContainer');

  function populateMatrixDropdowns() {
    if (!matrixTrainSelect) return;
    matrixTrainSelect.innerHTML = '';

    if (cachedFleet.length === 0) {
      matrixTrainSelect.innerHTML = '<option value="">No trains available</option>';
      return;
    }

    cachedFleet.forEach(t => {
      const opt = document.createElement('option');
      opt.value = t.id;
      opt.textContent = `${t.trainNo} - ${t.name} (${t.sourceStation} -> ${t.destinationStation})`;
      matrixTrainSelect.appendChild(opt);
    });

    if (!matrixDateInput.value) {
      const today = new Date().toISOString().split('T')[0];
      matrixDateInput.value = today;
    }

    updateMatrixClassDropdown();
    matrixTrainSelect.addEventListener('change', updateMatrixClassDropdown);
  }

  function updateMatrixClassDropdown() {
    if (!matrixClassSelect) return;
    matrixClassSelect.innerHTML = '';
    const trainId = parseInt(matrixTrainSelect.value, 10);
    const train = cachedFleet.find(t => t.id === trainId);
    if (!train || !train.classes || train.classes.length === 0) {
      matrixClassSelect.innerHTML = '<option value="">No classes</option>';
      return;
    }

    train.classes.forEach(c => {
      const opt = document.createElement('option');
      opt.value = c.className;
      opt.textContent = `${c.className} (${c.totalSeats} seats)`;
      matrixClassSelect.appendChild(opt);
    });
  }

  if (btnLoadMatrix) {
    btnLoadMatrix.addEventListener('click', async () => {
      const trainId = matrixTrainSelect.value;
      const date = matrixDateInput.value;
      const className = matrixClassSelect.value;
      if (!trainId || !date) {
        showFlash('Please select both a train and journey date.', 'warning');
        return;
      }

      coachFloorplanContainer.innerHTML = '<div class="p-8 text-center text-muted">Calculating visual 2D coach floorplan...</div>';

      try {
        const url = `/api/v1/admin/trains/${trainId}/seat-matrix?date=${encodeURIComponent(date)}&className=${encodeURIComponent(className)}`;
        const res = await authFetch(url);
        if (!res.ok) {
          const err = await res.json().catch(() => ({}));
          coachFloorplanContainer.innerHTML = `<div class="p-8 text-center text-muted">Failed to load matrix: ${err.message || 'Error'}</div>`;
          return;
        }

        const data = await res.json();
        render2DCoachMatrix(data);
      } catch (err) {
        coachFloorplanContainer.innerHTML = `<div class="p-8 text-center text-muted">Error loading matrix: ${err.message}</div>`;
      }
    });
  }

  function render2DCoachMatrix(matrix) {
    document.getElementById('matrixTrainHeader').textContent = `${matrix.trainName} (#${matrix.trainNo}) - Coach ${matrix.coachCode}`;
    document.getElementById('matrixRouteHeader').textContent = `Journey Date: ${matrix.journeyDate} | Coach Class: ${matrix.className}`;
    document.getElementById('matrixTotalSeats').textContent = String(matrix.totalSeats);
    document.getElementById('matrixBookedSeats').textContent = String(matrix.bookedSeatsCount);
    document.getElementById('matrixVacantSeats').textContent = String(matrix.availableSeatsCount);
    document.getElementById('matrixOccupancyRate').textContent = matrix.occupancyRate.toFixed(1) + '%';

    coachFloorplanContainer.innerHTML = '';

    const cols = matrix.columns || 4;
    const rows = matrix.rows || 1;
    const seats = matrix.seats || [];

    // Group seats by row
    const rowMap = {};
    seats.forEach(s => {
      const r = s.row || 1;
      if (!rowMap[r]) rowMap[r] = [];
      rowMap[r].push(s);
    });

    // Coach boundary frame
    const coachFrame = document.createElement('div');
    coachFrame.style.display = 'flex';
    coachFrame.style.flexDirection = 'column';
    coachFrame.style.gap = '0.5rem';
    coachFrame.style.border = '2px solid var(--color-rule)';
    coachFrame.style.borderRadius = '8px';
    coachFrame.style.padding = '1rem';
    coachFrame.style.background = 'var(--color-paper-2)';

    // Coach entrance marker
    const entranceTop = document.createElement('div');
    entranceTop.className = 'font-mono text-xs uppercase text-muted';
    entranceTop.style.display = 'flex';
    entranceTop.style.justifyContent = 'space-between';
    entranceTop.style.borderBottom = '1px dashed var(--color-rule)';
    entranceTop.style.paddingBottom = '0.5rem';
    entranceTop.innerHTML = '<span>&larr; Coach Vestibule Entrance</span><span>Window Side &rarr;</span>';
    coachFrame.appendChild(entranceTop);

    // Rows
    for (let r = 1; r <= rows; r++) {
      const rowSeats = rowMap[r] || [];
      const rowEl = document.createElement('div');
      rowEl.className = 'coach-row';

      const rowLabel = document.createElement('span');
      rowLabel.className = 'font-mono text-xs text-muted';
      rowLabel.style.width = '24px';
      rowLabel.textContent = `R${r}`;
      rowEl.appendChild(rowLabel);

      rowSeats.forEach((seat, idx) => {
        // Aisle gap in middle
        if (cols === 5 && idx === 3) {
          const aisle = document.createElement('div');
          aisle.className = 'aisle-gap';
          aisle.textContent = '||';
          rowEl.appendChild(aisle);
        } else if (cols === 4 && idx === 2) {
          const aisle = document.createElement('div');
          aisle.className = 'aisle-gap';
          aisle.textContent = '||';
          rowEl.appendChild(aisle);
        }

        const cell = document.createElement('div');
        cell.className = `seat-cell ${seat.status === 'BOOKED' ? 'booked' : 'available'}`;

        const noSpan = document.createElement('strong');
        noSpan.textContent = seat.seatIndex;
        cell.appendChild(noSpan);

        const typeSpan = document.createElement('span');
        typeSpan.style.fontSize = '0.6rem';
        typeSpan.textContent = seat.berthType ? seat.berthType.substring(0, 3) : '';
        cell.appendChild(typeSpan);

        if (seat.status === 'BOOKED') {
          const tip = document.createElement('div');
          tip.className = 'seat-tooltip';
          tip.innerHTML = `<strong>PNR: ${seat.pnr || 'N/A'}</strong><br>${seat.passengerName || 'Passenger'} (${seat.passengerAge || '-'}, ${seat.passengerGender || '-'})<br>Seat: ${seat.seatNumber}`;
          cell.appendChild(tip);
        }

        rowEl.appendChild(cell);
      });

      coachFrame.appendChild(rowEl);
    }

    coachFloorplanContainer.appendChild(coachFrame);
  }

  // --- Reservation Chart Manifest (TAB 4) ---
  const chartTrainSelect = document.getElementById('chartTrainSelect');
  const chartDateInput = document.getElementById('chartDateInput');
  const btnLoadChart = document.getElementById('btnLoadChart');
  const btnPrintChart = document.getElementById('btnPrintChart');
  const btnExportChartCsv = document.getElementById('btnExportChartCsv');
  const chartTableBody = document.getElementById('chartTableBody');

  function populateChartDropdowns() {
    if (!chartTrainSelect) return;
    chartTrainSelect.innerHTML = '';

    if (cachedFleet.length === 0) {
      chartTrainSelect.innerHTML = '<option value="">No trains available</option>';
      return;
    }

    cachedFleet.forEach(t => {
      const opt = document.createElement('option');
      opt.value = t.id;
      opt.textContent = `${t.trainNo} - ${t.name} (${t.sourceStation} -> ${t.destinationStation})`;
      chartTrainSelect.appendChild(opt);
    });

    if (!chartDateInput.value) {
      chartDateInput.value = new Date().toISOString().split('T')[0];
    }
  }

  if (btnLoadChart) {
    btnLoadChart.addEventListener('click', async () => {
      const trainId = chartTrainSelect.value;
      const date = chartDateInput.value;
      if (!trainId || !date) {
        showFlash('Please select a train and journey date.', 'warning');
        return;
      }

      chartTableBody.innerHTML = '<tr><td colspan="7" class="p-6 text-center text-muted">Compiling passenger reservation manifest...</td></tr>';

      try {
        const res = await authFetch(`/api/v1/admin/trains/${trainId}/chart?date=${encodeURIComponent(date)}`);
        if (!res.ok) {
          const err = await res.json().catch(() => ({}));
          chartTableBody.innerHTML = `<tr><td colspan="7" class="p-6 text-center text-muted">Failed to generate chart: ${err.message || 'Error'}</td></tr>`;
          return;
        }

        const data = await res.json();
        renderReservationChart(data);
      } catch (e) {
        chartTableBody.innerHTML = `<tr><td colspan="7" class="p-6 text-center text-muted">Error generating manifest: ${e.message}</td></tr>`;
      }
    });
  }

  function renderReservationChart(data) {
    document.getElementById('manifestTrainTitle').textContent = `Train #${data.trainNo} - ${data.trainName}`;
    document.getElementById('manifestTrainSubtitle').textContent = `Route: ${data.sourceStation} -> ${data.destinationStation} on ${data.journeyDate} at ${data.departureTime || 'Scheduled'}`;
    document.getElementById('manifestPassengerCount').textContent = `Total Passengers: ${data.totalPassengers}`;

    chartTableBody.innerHTML = '';
    const entries = data.entries || [];

    if (entries.length === 0) {
      chartTableBody.innerHTML = '<tr><td colspan="7" class="p-6 text-center text-muted">No confirmed passengers reserved for this train departure date.</td></tr>';
      return;
    }

    entries.forEach(e => {
      const tr = document.createElement('tr');
      tr.className = 'hover:bg-paper-2';
      tr.innerHTML = `
        <td class="p-3 font-mono font-bold text-ink">${e.pnr}</td>
        <td class="p-3 font-mono font-semibold">${e.className}</td>
        <td class="p-3 font-mono text-accent font-bold">${e.seatNumber}</td>
        <td class="p-3 font-semibold text-ink">${e.passengerName}</td>
        <td class="p-3 font-mono text-muted">${e.age ?? '-'} yrs / ${e.gender ?? '-'}</td>
        <td class="p-3"><span class="badge" style="background: var(--color-success-bg); color: var(--color-success); border: 1px solid var(--color-rule); font-size: 0.7rem; padding: 2px 6px;">CONFIRMED</span></td>
        <td class="p-3 text-muted">${e.boardingStation} &rarr; ${e.destinationStation}</td>
      `;
      chartTableBody.appendChild(tr);
    });
  }

  if (btnPrintChart) {
    btnPrintChart.addEventListener('click', () => {
      window.print();
    });
  }

  if (btnExportChartCsv) {
    btnExportChartCsv.addEventListener('click', async () => {
      const trainId = chartTrainSelect.value;
      const date = chartDateInput.value;
      if (!trainId || !date) {
        showFlash('Please select a train and date to export CSV.', 'warning');
        return;
      }

      try {
        const res = await authFetch(`/api/v1/admin/trains/${trainId}/chart/csv?date=${encodeURIComponent(date)}`);
        if (!res.ok) {
          showFlash('Failed to generate CSV export.', 'danger');
          return;
        }

        const blob = await res.blob();
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `reservation-chart-train-${trainId}-${date}.csv`;
        document.body.appendChild(a);
        a.click();
        a.remove();
        window.URL.revokeObjectURL(url);
        showFlash('Reservation manifest CSV downloaded successfully.', 'success');
      } catch (err) {
        showFlash('Error downloading CSV: ' + err.message, 'danger');
      }
    });
  }

  // --- Initialize on DOMContentLoaded ---
  document.addEventListener('DOMContentLoaded', () => {
    initTheme();
    initClock();
    initTabs();
    checkAdminAccess();
  });
})();

