/**
 * Railway Reservation System - Booking UI & UX Logic
 * Compliant with SRS RRS-NF-003, SAD Section 4.6, and TC-UX-01.
 * Strictly uses safe DOM APIs (textContent, createElement) to prevent XSS.
 */

(function () {
  'use strict';

  // --- State ---
  let selectedTrain = null;
  let selectedClass = null;
  let activeSearchDate = null;
  let passengerRowCount = 0;
  let startTime = Date.now();

  // --- DOM Elements ---
  const flashContainer = document.getElementById('flashContainer');
  const timerDisplay = document.getElementById('timerDisplay');

  // Search Form
  const trainSearchForm = document.getElementById('trainSearchForm');
  const srcInput = document.getElementById('srcInput');
  const dstInput = document.getElementById('dstInput');
  const dateInput = document.getElementById('dateInput');
  const srcGroup = document.getElementById('srcGroup');
  const dstGroup = document.getElementById('dstGroup');
  const dateGroup = document.getElementById('dateGroup');
  const srcError = document.getElementById('srcError');
  const dstError = document.getElementById('dstError');
  const dateError = document.getElementById('dateError');
  const srcSuggestions = document.getElementById('srcSuggestions');
  const dstSuggestions = document.getElementById('dstSuggestions');
  const chipDateToday = document.getElementById('chipDateToday');
  const chipDateTomorrow = document.getElementById('chipDateTomorrow');
  const chipDateWeekend = document.getElementById('chipDateWeekend');
  const swapStationsBtn = document.getElementById('swapStationsBtn');
  const searchSubmitBtn = document.getElementById('searchSubmitBtn');
  const searchBtnSpinner = document.getElementById('searchBtnSpinner');
  const searchBtnText = document.getElementById('searchBtnText');

  // Results Section
  const resultsSection = document.getElementById('resultsSection');
  const resultsCount = document.getElementById('resultsCount');
  const resultsRouteSummary = document.getElementById('resultsRouteSummary');
  const trainsListContainer = document.getElementById('trainsListContainer');

  // Route & Halts Modal (RRS-F-004-UI)
  const routeModal = document.getElementById('routeModal');
  const routeModalTrainName = document.getElementById('routeModalTrainName');
  const routeModalSubtitle = document.getElementById('routeModalSubtitle');
  const routeModalLoading = document.getElementById('routeModalLoading');
  const routeModalTableContainer = document.getElementById('routeModalTableContainer');
  const closeRouteModalBtn = document.getElementById('closeRouteModalBtn');
  const closeRouteModalBottomBtn = document.getElementById('closeRouteModalBottomBtn');

  // Booking Section
  const bookingSection = document.getElementById('bookingSection');
  const recapTrainName = document.getElementById('recapTrainName');
  const recapRoute = document.getElementById('recapRoute');
  const recapClass = document.getElementById('recapClass');
  const recapFare = document.getElementById('recapFare');
  const passengerBookingForm = document.getElementById('passengerBookingForm');
  const userIdInput = document.getElementById('userIdInput');
  const userIdError = document.getElementById('userIdError');
  const quickRegisterBtn = document.getElementById('quickRegisterBtn');
  const passengersTable = document.getElementById('passengersTable');
  const passengerRowsContainer = document.getElementById('passengerRowsContainer');
  const addPassengerBtn = document.getElementById('addPassengerBtn');
  const totalFareDisplay = document.getElementById('totalFareDisplay');
  const passengerCountSummary = document.getElementById('passengerCountSummary');
  const cancelBookingStepBtn = document.getElementById('cancelBookingStepBtn');
  const confirmBookingBtn = document.getElementById('confirmBookingBtn');
  const bookingBtnSpinner = document.getElementById('bookingBtnSpinner');
  const bookingBtnText = document.getElementById('bookingBtnText');

  // Commercial Fare Breakdown [RRS-F-007-EXT]
  const breakdownBaseFare = document.getElementById('breakdownBaseFare');
  const breakdownSuperfastCharge = document.getElementById('breakdownSuperfastCharge');
  const breakdownReservationFee = document.getElementById('breakdownReservationFee');
  const breakdownGst = document.getElementById('breakdownGst');
  const breakdownSeniorRow = document.getElementById('breakdownSeniorRow');
  const breakdownSeniorDiscount = document.getElementById('breakdownSeniorDiscount');
  const seniorCountBadge = document.getElementById('seniorCountBadge');
  const farePassengerCount = document.getElementById('farePassengerCount');

  // Confirmation Section
  const confirmationSection = document.getElementById('confirmationSection');
  const confirmationHeading = document.getElementById('confirmationHeading');
  const confirmationSubtitle = document.getElementById('confirmationSubtitle');
  const confirmedPnrDisplay = document.getElementById('confirmedPnrDisplay');
  const copyPnrBtn = document.getElementById('copyPnrBtn');
  const confTrainDetails = document.getElementById('confTrainDetails');
  const confRouteDetails = document.getElementById('confRouteDetails');
  const confJourneyDate = document.getElementById('confJourneyDate');
  const confTimings = document.getElementById('confTimings');
  const confClassDetails = document.getElementById('confClassDetails');
  const confTotalFare = document.getElementById('confTotalFare');
  const confPaymentStatus = document.getElementById('confPaymentStatus');
  const confirmedPassengersBody = document.getElementById('confirmedPassengersBody');
  const bookAnotherBtn = document.getElementById('bookAnotherBtn');

  // PNR Modal
  const openPnrModalBtn = document.getElementById('openPnrModalBtn');
  const closePnrModalBtn = document.getElementById('closePnrModalBtn');
  const pnrModal = document.getElementById('pnrModal');
  const pnrSearchForm = document.getElementById('pnrSearchForm');
  const pnrInput = document.getElementById('pnrInput');
  const pnrInputError = document.getElementById('pnrInputError');
  const pnrResultContainer = document.getElementById('pnrResultContainer');
  const pnrSearchSubmitBtn = document.getElementById('pnrSearchSubmitBtn');
  const pnrBtnSpinner = document.getElementById('pnrBtnSpinner');

  // Login Modal
  const openLoginModalBtn = document.getElementById('openLoginModalBtn');
  const closeLoginModalBtn = document.getElementById('closeLoginModalBtn');
  const loginModal = document.getElementById('loginModal');
  const loginForm = document.getElementById('loginForm');
  const loginEmail = document.getElementById('loginEmail');
  const loginPassword = document.getElementById('loginPassword');
  const loginStatus = document.getElementById('loginStatus');
  const loginSubmitBtn = document.getElementById('loginSubmitBtn');
  const loginSubmitText = document.getElementById('loginSubmitText');
  const loginSpinner = document.getElementById('loginSpinner');

  // Booking History Modal (RRS-F-014)
  const openHistoryModalBtn = document.getElementById('openHistoryModalBtn');
  const closeHistoryModalBtn = document.getElementById('closeHistoryModalBtn');
  const historyModal = document.getElementById('historyModal');
  const historyListContainer = document.getElementById('historyListContainer');
  const historyLoading = document.getElementById('historyLoading');
  const historyEmpty = document.getElementById('historyEmpty');
  const historyFilterAll = document.getElementById('historyFilterAll');
  const historyFilterUpcoming = document.getElementById('historyFilterUpcoming');
  const historyFilterPast = document.getElementById('historyFilterPast');

  // --- Usability Timer (TC-UX-01) ---
  setInterval(() => {
    const elapsedSeconds = Math.floor((Date.now() - startTime) / 1000);
    const mins = String(Math.floor(elapsedSeconds / 60)).padStart(2, '0');
    const secs = String(elapsedSeconds % 60).padStart(2, '0');
    timerDisplay.textContent = `${mins}:${secs}`;
    if (elapsedSeconds > 300) {
      timerDisplay.style.color = '#ef4444'; // Red if > 5 minutes
    }
  }, 1000);

  // --- Theme Management (Hallmark Wayfind Platform) ---
  const themeToggleBtn = document.getElementById('themeToggleBtn');
  const themeIconSun = document.getElementById('themeIconSun');
  const themeIconMoon = document.getElementById('themeIconMoon');
  const themeToggleText = document.getElementById('themeToggleText');

  function updateThemeUI(mode) {
    document.documentElement.setAttribute('data-mode', mode);
    if (themeIconSun && themeIconMoon && themeToggleText) {
      if (mode === 'dark') {
        themeIconSun.style.display = 'inline-block';
        themeIconMoon.style.display = 'none';
        themeToggleText.textContent = 'Light';
      } else {
        themeIconSun.style.display = 'none';
        themeIconMoon.style.display = 'inline-block';
        themeToggleText.textContent = 'Dark';
      }
    }
  }

  const currentTheme = document.documentElement.getAttribute('data-mode') || 'light';
  updateThemeUI(currentTheme);

  if (themeToggleBtn) {
    themeToggleBtn.addEventListener('click', () => {
      const activeMode = document.documentElement.getAttribute('data-mode') === 'dark' ? 'dark' : 'light';
      const newMode = activeMode === 'dark' ? 'light' : 'dark';
      localStorage.setItem('rrs_theme', newMode);
      updateThemeUI(newMode);
    });
  }

  // --- Flash Notification Helpers (SAD Section 4.6) ---
  function showFlash(message, type = 'info', autoDismiss = true) {
    // type: 'success' | 'error' | 'warning' | 'info'
    const banner = document.createElement('div');
    banner.className = `flash-banner ${type}`;
    banner.setAttribute('role', 'alert');

    const msgSpan = document.createElement('span');
    msgSpan.textContent = message;
    banner.appendChild(msgSpan);

    const closeBtn = document.createElement('button');
    closeBtn.className = 'flash-close-btn';
    closeBtn.type = 'button';
    closeBtn.setAttribute('aria-label', 'Close notification');
    closeBtn.textContent = '×';
    closeBtn.addEventListener('click', () => banner.remove());
    banner.appendChild(closeBtn);

    flashContainer.appendChild(banner);

    // Scroll to top so user sees the notification
    window.scrollTo({ top: 0, behavior: 'smooth' });

    if (autoDismiss && type !== 'error') {
      setTimeout(() => {
        if (banner.parentElement) {
          banner.remove();
        }
      }, 6000);
    }
  }

  function clearFlashes() {
    while (flashContainer.firstChild) {
      flashContainer.removeChild(flashContainer.firstChild);
    }
  }

  // --- Station Catalog & Master Data Autocomplete (RRS-F-004-UI) ---
  let masterStations = [];

  const fallbackStations = [
    { code: 'MAS', name: 'Chennai Central', city: 'Chennai', state: 'Tamil Nadu' },
    { code: 'SBC', name: 'KSR Bengaluru', city: 'Bengaluru', state: 'Karnataka' },
    { code: 'NDLS', name: 'New Delhi', city: 'Delhi', state: 'Delhi' },
    { code: 'MMCT', name: 'Mumbai Central', city: 'Mumbai', state: 'Maharashtra' },
    { code: 'CSMT', name: 'Chhatrapati Shivaji Terminus', city: 'Mumbai', state: 'Maharashtra' },
    { code: 'HWH', name: 'Howrah Junction', city: 'Kolkata', state: 'West Bengal' },
    { code: 'PUNE', name: 'Pune Junction', city: 'Pune', state: 'Maharashtra' },
    { code: 'CBE', name: 'Coimbatore Junction', city: 'Coimbatore', state: 'Tamil Nadu' },
    { code: 'HYB', name: 'Hyderabad Deccan', city: 'Hyderabad', state: 'Telangana' },
    { code: 'ADI', name: 'Ahmedabad Junction', city: 'Ahmedabad', state: 'Gujarat' },
    { code: 'CNB', name: 'Kanpur Central', city: 'Kanpur', state: 'Uttar Pradesh' },
    { code: 'PRYJ', name: 'Prayagraj Junction', city: 'Prayagraj', state: 'Uttar Pradesh' },
    { code: 'DDU', name: 'Pt. Deen Dayal Upadhyaya', city: 'Mughalsarai', state: 'Uttar Pradesh' },
    { code: 'GAYA', name: 'Gaya Junction', city: 'Gaya', state: 'Bihar' },
    { code: 'DHN', name: 'Dhanbad Junction', city: 'Dhanbad', state: 'Jharkhand' },
    { code: 'ASN', name: 'Asansol Junction', city: 'Asansol', state: 'West Bengal' },
    { code: 'KPD', name: 'Katpadi Junction', city: 'Vellore', state: 'Tamil Nadu' },
    { code: 'JTJ', name: 'Jolarpettai Junction', city: 'Jolarpettai', state: 'Tamil Nadu' },
    { code: 'AJJ', name: 'Arakkonam Junction', city: 'Arakkonam', state: 'Tamil Nadu' },
    { code: 'ST', name: 'Surat', city: 'Surat', state: 'Gujarat' },
    { code: 'BRC', name: 'Vadodara Junction', city: 'Vadodara', state: 'Gujarat' },
    { code: 'RTM', name: 'Ratlam Junction', city: 'Ratlam', state: 'Madhya Pradesh' },
    { code: 'KOTA', name: 'Kota Junction', city: 'Kota', state: 'Rajasthan' },
    { code: 'LNL', name: 'Lonavala', city: 'Lonavala', state: 'Maharashtra' },
    { code: 'KJT', name: 'Karjat Junction', city: 'Karjat', state: 'Maharashtra' }
  ];

  async function loadMasterStations() {
    try {
      const res = await fetch('/api/v1/trains/stations');
      if (res.ok) {
        masterStations = await res.json();
      } else {
        masterStations = fallbackStations;
      }
    } catch (_) {
      masterStations = fallbackStations;
    }
  }
  loadMasterStations();

  function setupStationAutocomplete(inputElem, listElem) {
    if (!inputElem || !listElem) return;
    let activeIndex = -1;

    function renderList(query = '') {
      while (listElem.firstChild) {
        listElem.removeChild(listElem.firstChild);
      }
      activeIndex = -1;

      const q = query.trim().toLowerCase();
      const list = (masterStations && masterStations.length > 0) ? masterStations : fallbackStations;
      const filtered = q
        ? list.filter(s =>
            (s.code && s.code.toLowerCase().includes(q)) ||
            (s.name && s.name.toLowerCase().includes(q)) ||
            (s.city && s.city.toLowerCase().includes(q)) ||
            (s.state && s.state.toLowerCase().includes(q))
          ).slice(0, 7)
        : list.slice(0, 7);

      if (filtered.length === 0) {
        const noItem = document.createElement('li');
        noItem.className = 'autocomplete-item';
        noItem.style.color = 'var(--color-muted)';
        noItem.style.cursor = 'default';
        noItem.textContent = 'No matching railway stations found.';
        listElem.appendChild(noItem);
        listElem.style.display = 'block';
        inputElem.setAttribute('aria-expanded', 'true');
        return;
      }

      filtered.forEach((stn, idx) => {
        const item = document.createElement('li');
        item.className = 'autocomplete-item';
        item.setAttribute('role', 'option');
        item.setAttribute('id', `${inputElem.id}-opt-${idx}`);
        item.setAttribute('aria-selected', 'false');

        const leftDiv = document.createElement('div');
        leftDiv.className = 'autocomplete-item-left';

        const codeSpan = document.createElement('span');
        codeSpan.className = 'station-code-pill';
        codeSpan.textContent = stn.code;
        leftDiv.appendChild(codeSpan);

        const nameSpan = document.createElement('span');
        nameSpan.className = 'station-name-text';
        nameSpan.textContent = stn.name;
        leftDiv.appendChild(nameSpan);

        item.appendChild(leftDiv);

        if (stn.state) {
          const metaSpan = document.createElement('span');
          metaSpan.className = 'station-meta-text';
          metaSpan.textContent = stn.state;
          item.appendChild(metaSpan);
        }

        item.addEventListener('mousedown', (e) => {
          e.preventDefault();
          selectStation(stn);
        });

        listElem.appendChild(item);
      });

      listElem.style.display = 'block';
      inputElem.setAttribute('aria-expanded', 'true');
    }

    function selectStation(stn) {
      inputElem.value = `${stn.name} (${stn.code})`;
      closeList();
      validateStationInputs();
    }

    function closeList() {
      listElem.style.display = 'none';
      inputElem.setAttribute('aria-expanded', 'false');
      activeIndex = -1;
    }

    function updateActiveHighlight(items) {
      items.forEach((item, idx) => {
        if (idx === activeIndex) {
          item.classList.add('active');
          item.setAttribute('aria-selected', 'true');
          item.scrollIntoView({ block: 'nearest' });
          inputElem.setAttribute('aria-activedescendant', item.id);
        } else {
          item.classList.remove('active');
          item.setAttribute('aria-selected', 'false');
        }
      });
    }

    inputElem.addEventListener('focus', () => {
      renderList(inputElem.value);
    });

    inputElem.addEventListener('input', () => {
      renderList(inputElem.value);
    });

    inputElem.addEventListener('keydown', (e) => {
      const items = listElem.querySelectorAll('.autocomplete-item[role="option"]');
      if (listElem.style.display === 'none' || items.length === 0) {
        if (e.key === 'ArrowDown') {
          renderList(inputElem.value);
          e.preventDefault();
        }
        return;
      }

      if (e.key === 'ArrowDown') {
        e.preventDefault();
        activeIndex = (activeIndex + 1) % items.length;
        updateActiveHighlight(items);
      } else if (e.key === 'ArrowUp') {
        e.preventDefault();
        activeIndex = (activeIndex - 1 + items.length) % items.length;
        updateActiveHighlight(items);
      } else if (e.key === 'Enter') {
        if (activeIndex >= 0 && activeIndex < items.length) {
          e.preventDefault();
          items[activeIndex].dispatchEvent(new MouseEvent('mousedown'));
        }
      } else if (e.key === 'Escape') {
        e.preventDefault();
        closeList();
      }
    });

    document.addEventListener('click', (e) => {
      if (!inputElem.contains(e.target) && !listElem.contains(e.target)) {
        closeList();
      }
    });
  }

  setupStationAutocomplete(srcInput, srcSuggestions);
  setupStationAutocomplete(dstInput, dstSuggestions);

  // --- Date Helpers & Quick Date Chips (RRS-F-004-UI) ---
  function formatDateYMD(d) {
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${y}-${m}-${day}`;
  }

  const today = new Date();
  const minDateStr = formatDateYMD(today);
  dateInput.min = minDateStr;

  const tomorrow = new Date();
  tomorrow.setDate(today.getDate() + 1);
  dateInput.value = formatDateYMD(tomorrow);

  function getUpcomingWeekend() {
    const d = new Date();
    const dayOfWeek = d.getDay(); // 0 is Sun, 6 is Sat
    let daysUntilSat = (6 - dayOfWeek + 7) % 7;
    if (daysUntilSat === 0 && dayOfWeek !== 6) daysUntilSat = 7;
    d.setDate(d.getDate() + daysUntilSat);
    return d;
  }

  function setActiveDateChip(btn) {
    document.querySelectorAll('.date-chip').forEach(c => c.classList.remove('active'));
    if (btn) btn.classList.add('active');
  }

  if (chipDateToday) {
    chipDateToday.addEventListener('click', () => {
      dateInput.value = formatDateYMD(new Date());
      dateInput.dispatchEvent(new Event('input'));
      setActiveDateChip(chipDateToday);
    });
  }
  if (chipDateTomorrow) {
    chipDateTomorrow.addEventListener('click', () => {
      const d = new Date();
      d.setDate(d.getDate() + 1);
      dateInput.value = formatDateYMD(d);
      dateInput.dispatchEvent(new Event('input'));
      setActiveDateChip(chipDateTomorrow);
    });
    chipDateTomorrow.classList.add('active');
  }
  if (chipDateWeekend) {
    chipDateWeekend.addEventListener('click', () => {
      dateInput.value = formatDateYMD(getUpcomingWeekend());
      dateInput.dispatchEvent(new Event('input'));
      setActiveDateChip(chipDateWeekend);
    });
  }

  // --- Station Swap Action with Smooth Rotation (RRS-F-004-UI) ---
  swapStationsBtn.addEventListener('click', () => {
    swapStationsBtn.classList.add('is-rotating');
    const temp = srcInput.value;
    srcInput.value = dstInput.value;
    dstInput.value = temp;

    setTimeout(() => {
      swapStationsBtn.classList.remove('is-rotating');
    }, 260);

    if (srcSuggestions) srcSuggestions.style.display = 'none';
    if (dstSuggestions) dstSuggestions.style.display = 'none';

    validateStationInputs();
  });

  // --- Quick Popular Route Chips ---
  document.querySelectorAll('.chip[data-src]').forEach(chip => {
    chip.addEventListener('click', () => {
      srcInput.value = chip.getAttribute('data-src') || '';
      dstInput.value = chip.getAttribute('data-dst') || '';
      validateStationInputs();
      trainSearchForm.requestSubmit();
    });
  });

  // --- Real-time Station Validation ---
  function validateStationInputs() {
    let isValid = true;
    const srcVal = srcInput.value.trim();
    const dstVal = dstInput.value.trim();

    if (!srcVal) {
      srcGroup.classList.add('has-error');
      srcInput.classList.add('is-invalid');
      srcError.textContent = 'Departure station is required.';
      isValid = false;
    } else {
      srcGroup.classList.remove('has-error');
      srcInput.classList.remove('is-invalid');
    }

    if (!dstVal) {
      dstGroup.classList.add('has-error');
      dstInput.classList.add('is-invalid');
      dstError.textContent = 'Destination station is required.';
      isValid = false;
    } else {
      dstGroup.classList.remove('has-error');
      dstInput.classList.remove('is-invalid');
    }

    if (srcVal && dstVal && srcVal.toLowerCase() === dstVal.toLowerCase()) {
      dstGroup.classList.add('has-error');
      dstInput.classList.add('is-invalid');
      dstError.textContent = 'Source and destination stations cannot be the same.';
      isValid = false;
    }

    return isValid;
  }

  srcInput.addEventListener('input', validateStationInputs);
  srcInput.addEventListener('change', validateStationInputs);
  dstInput.addEventListener('input', validateStationInputs);
  dstInput.addEventListener('change', validateStationInputs);
  dateInput.addEventListener('input', () => {
    if (!dateInput.value) {
      dateGroup.classList.add('has-error');
      dateInput.classList.add('is-invalid');
      dateError.textContent = 'Journey date is required.';
    } else if (dateInput.value < minDateStr) {
      dateGroup.classList.add('has-error');
      dateInput.classList.add('is-invalid');
      dateError.textContent = 'Journey date cannot be in the past.';
    } else {
      dateGroup.classList.remove('has-error');
      dateInput.classList.remove('is-invalid');
    }
  });

  // --- Skeleton Loading State (RRS-F-004-UI) ---
  function renderSkeletonLoading(src, dst, date) {
    while (trainsListContainer.firstChild) {
      trainsListContainer.removeChild(trainsListContainer.firstChild);
    }
    resultsSection.style.display = 'block';
    resultsCount.textContent = '...';
    resultsRouteSummary.textContent = `${src} ➔ ${dst} on ${date}`;

    for (let i = 0; i < 2; i++) {
      const card = document.createElement('div');
      card.className = 'skeleton-card';
      card.innerHTML = `
        <div style="display: flex; justify-content: space-between; align-items: center; border-bottom: var(--rule-hair) solid var(--color-rule); padding-bottom: 0.85rem;">
          <div class="skeleton-box" style="width: 200px; height: 24px;"></div>
          <div class="skeleton-box" style="width: 120px; height: 26px;"></div>
        </div>
        <div style="display: flex; gap: 2rem; align-items: center; margin: 0.5rem 0;">
          <div class="skeleton-box" style="width: 90px; height: 32px;"></div>
          <div class="skeleton-box" style="flex: 1; height: 4px;"></div>
          <div class="skeleton-box" style="width: 90px; height: 32px;"></div>
        </div>
        <div style="display: grid; grid-template-columns: repeat(auto-fill, minmax(130px, 1fr)); gap: 0.75rem;">
          <div class="skeleton-box" style="height: 60px;"></div>
          <div class="skeleton-box" style="height: 60px;"></div>
          <div class="skeleton-box" style="height: 60px;"></div>
        </div>
      `;
      trainsListContainer.appendChild(card);
    }
  }

  // --- Search Form Submission ---
  trainSearchForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    clearFlashes();

    const isStationsValid = validateStationInputs();
    let isDateValid = true;

    if (!dateInput.value) {
      dateGroup.classList.add('has-error');
      dateInput.classList.add('is-invalid');
      dateError.textContent = 'Journey date is required.';
      isDateValid = false;
    } else if (dateInput.value < minDateStr) {
      dateGroup.classList.add('has-error');
      dateInput.classList.add('is-invalid');
      dateError.textContent = 'Journey date cannot be in the past.';
      isDateValid = false;
    }

    if (!isStationsValid || !isDateValid) {
      showFlash('Please resolve the errors highlighted in the form before searching.', 'warning');
      return;
    }

    const src = srcInput.value.trim();
    const dst = dstInput.value.trim();
    const date = dateInput.value.trim();

    // Set loading state & guard duplicate clicks
    searchSubmitBtn.disabled = true;
    searchBtnSpinner.style.display = 'inline-block';
    searchBtnText.textContent = 'Searching...';

    // Show crisp skeleton cards while waiting
    renderSkeletonLoading(src, dst, date);

    // Hide downstream sections
    bookingSection.style.display = 'none';
    confirmationSection.style.display = 'none';

    try {
      const url = `/api/v1/trains/search?src=${encodeURIComponent(src)}&dst=${encodeURIComponent(dst)}&date=${encodeURIComponent(date)}`;
      const response = await fetch(url, { headers: { 'Accept': 'application/json' } });

      if (!response.ok) {
        let errMessage = 'Failed to fetch trains.';
        try {
          const errData = await response.json();
          if (errData && errData.message) {
            errMessage = errData.message;
          }
        } catch (_) {}
        showFlash(`Search failed (${response.status}): ${errMessage}`, 'error');
        return;
      }

      const trains = await response.json();
      activeSearchDate = date;
      renderSearchResults(trains, src, dst, date);

    } catch (err) {
      showFlash(`Network or server error while connecting to railway API: ${err.message}`, 'error');
    } finally {
      searchSubmitBtn.disabled = false;
      searchBtnSpinner.style.display = 'none';
      searchBtnText.textContent = 'Find Trains';
    }
  });

  // --- Render Search Results ---
  function renderSearchResults(trains, src, dst, date) {
    while (trainsListContainer.firstChild) {
      trainsListContainer.removeChild(trainsListContainer.firstChild);
    }

    resultsSection.style.display = 'block';
    resultsCount.textContent = String(trains.length);
    resultsRouteSummary.textContent = `${src} ➔ ${dst} on ${date}`;

    if (!trains || trains.length === 0) {
      const emptyState = document.createElement('div');
      emptyState.className = 'empty-state';
      emptyState.style.padding = '2.5rem 1.5rem';
      emptyState.style.textAlign = 'center';

      emptyState.innerHTML = `
        <svg width="44" height="44" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" style="margin: 0 auto 1rem auto; display: block; color: var(--color-muted);" aria-hidden="true">
          <circle cx="12" cy="12" r="10"/>
          <line x1="4.93" y1="4.93" x2="19.07" y2="19.07"/>
        </svg>
        <h3 style="font-size: 1.15rem; margin-bottom: 0.5rem; color: var(--color-ink);">No Direct Trains Scheduled</h3>
        <p style="color: var(--color-muted); font-size: 0.875rem; max-width: 500px; margin: 0 auto 1.25rem auto;">
          We could not find direct trains operating between <strong>${escapeHtml(src)}</strong> and <strong>${escapeHtml(dst)}</strong> on <strong>${escapeHtml(date)}</strong>.
        </p>
        <div style="display: flex; justify-content: center; gap: 0.75rem; flex-wrap: wrap;">
          <button type="button" class="btn btn-outline btn-sm" id="emptySwapAndSearchBtn">
            ⇄ Swap Origin &amp; Destination
          </button>
        </div>
      `;

      trainsListContainer.appendChild(emptyState);

      const emptySwapBtn = emptyState.querySelector('#emptySwapAndSearchBtn');
      if (emptySwapBtn) {
        emptySwapBtn.addEventListener('click', () => {
          swapStationsBtn.click();
          trainSearchForm.requestSubmit();
        });
      }

      showFlash(`No trains found operating between ${src} and ${dst} on ${date}.`, 'info');
      return;
    }

    showFlash(`Found ${trains.length} train(s) operating on ${date}. Select a class to proceed with booking.`, 'success');

    trains.forEach(train => {
      const card = document.createElement('div');
      card.className = 'train-card';

      // Header
      const cardHeader = document.createElement('div');
      cardHeader.className = 'train-card-header';

      const identityDiv = document.createElement('div');
      identityDiv.className = 'train-identity';

      const trainHeading = document.createElement('h3');
      trainHeading.textContent = train.name || 'Express Train';

      const trainBadge = document.createElement('span');
      trainBadge.className = 'train-number-badge';
      trainBadge.textContent = `#${train.trainNo || 'N/A'}`;
      trainHeading.appendChild(trainBadge);
      identityDiv.appendChild(trainHeading);
      cardHeader.appendChild(identityDiv);

      // Route Stops & Timetable Button (RRS-F-004-UI)
      const routeBtn = document.createElement('button');
      routeBtn.type = 'button';
      routeBtn.className = 'btn-view-route';
      routeBtn.setAttribute('aria-haspopup', 'dialog');
      routeBtn.setAttribute('aria-label', `View route stops and timetable for ${train.name || 'train'}`);
      routeBtn.innerHTML = `
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <circle cx="12" cy="12" r="10"/>
          <polyline points="12 6 12 12 16 14"/>
        </svg>
        <span>View Route &amp; Halts</span>
      `;
      routeBtn.addEventListener('click', () => openRouteModal(train));
      cardHeader.appendChild(routeBtn);

      card.appendChild(cardHeader);

      // Timings
      const timingsDiv = document.createElement('div');
      timingsDiv.className = 'train-timings';

      // Dep
      const depDiv = document.createElement('div');
      depDiv.className = 'time-point';
      const depTime = document.createElement('div');
      depTime.className = 'time';
      depTime.textContent = formatTime(train.departureTime);
      const depStn = document.createElement('div');
      depStn.className = 'station';
      depStn.textContent = train.sourceStation;
      depDiv.appendChild(depTime);
      depDiv.appendChild(depStn);
      timingsDiv.appendChild(depDiv);

      // Separator
      const sepDiv = document.createElement('div');
      sepDiv.className = 'time-separator';
      const sepLine = document.createElement('div');
      sepLine.className = 'time-separator-line';
      sepDiv.appendChild(sepLine);
      timingsDiv.appendChild(sepDiv);

      // Arr
      const arrDiv = document.createElement('div');
      arrDiv.className = 'time-point';
      const arrTime = document.createElement('div');
      arrTime.className = 'time';
      arrTime.textContent = formatTime(train.arrivalTime);
      const arrStn = document.createElement('div');
      arrStn.className = 'station';
      arrStn.textContent = train.destinationStation;
      arrDiv.appendChild(arrTime);
      arrDiv.appendChild(arrStn);
      timingsDiv.appendChild(arrDiv);

      card.appendChild(timingsDiv);
      arrStn.className = 'station';
      arrStn.textContent = train.destinationStation;
      arrDiv.appendChild(arrTime);
      arrDiv.appendChild(arrStn);
      timingsDiv.appendChild(arrDiv);

      card.appendChild(timingsDiv);

      // Classes Grid
      const classesTitle = document.createElement('div');
      classesTitle.style.fontSize = '0.85rem';
      classesTitle.style.fontWeight = '600';
      classesTitle.style.color = 'var(--text-muted)';
      classesTitle.textContent = 'SELECT TRAVEL CLASS & AVAILABLE SEATS:';
      card.appendChild(classesTitle);

      const classesGrid = document.createElement('div');
      classesGrid.className = 'classes-grid';

      if (train.classes && train.classes.length > 0) {
        train.classes.forEach(cls => {
          const classCard = document.createElement('div');
          classCard.className = 'class-card';
          classCard.tabIndex = 0;
          classCard.setAttribute('role', 'button');
          classCard.setAttribute('aria-label', `Select ${cls.className} class for ₹${cls.fare}`);

          const topRow = document.createElement('div');
          topRow.className = 'class-name';
          const nameSpan = document.createElement('span');
          nameSpan.textContent = cls.className;
          topRow.appendChild(nameSpan);

          const liveRefreshBtn = document.createElement('button');
          liveRefreshBtn.type = 'button';
          liveRefreshBtn.className = 'btn btn-secondary btn-sm';
          liveRefreshBtn.style.padding = '0.1rem 0.4rem';
          liveRefreshBtn.style.fontSize = '0.7rem';
          liveRefreshBtn.title = 'Refresh live availability from backend';
          liveRefreshBtn.textContent = 'Check';
          liveRefreshBtn.addEventListener('click', async (e) => {
            e.stopPropagation();
            await checkLiveAvailability(train.trainId, cls.className, date, seatsBadge);
          });
          topRow.appendChild(liveRefreshBtn);

          classCard.appendChild(topRow);

          const fareDiv = document.createElement('div');
          fareDiv.className = 'fare';
          fareDiv.textContent = `₹${cls.fare}`;
          classCard.appendChild(fareDiv);

          const seatsBadge = document.createElement('span');
          const isAvailable = cls.availableSeats > 0;
          seatsBadge.className = `seats-badge ${isAvailable ? 'available' : 'unavailable'}`;
          seatsBadge.textContent = isAvailable ? `Seats: ${cls.availableSeats} Available` : 'Waitlist / Full (0)';
          classCard.appendChild(seatsBadge);

          // Select class for booking
          classCard.addEventListener('click', () => {
            document.querySelectorAll('.class-card').forEach(c => c.classList.remove('selected'));
            classCard.classList.add('selected');

            const token = getStoredToken();
            if (!token || isTokenExpired(token)) {
              // Guest discovery: store pending selection and prompt login with auto-resume [RRS-SR-006]
              sessionStorage.setItem('pendingBookingState', JSON.stringify({
                train: train,
                cls: cls,
                date: activeSearchDate
              }));
              openLoginModal('Please sign in to proceed with booking. Your selected train and coach will be preserved.');
              return;
            }

            openBookingSection(train, cls);
          });

          classesGrid.appendChild(classCard);
        });
      } else {
        const noClasses = document.createElement('div');
        noClasses.textContent = 'No classes defined for this train.';
        noClasses.style.color = 'var(--text-muted)';
        classesGrid.appendChild(noClasses);
      }

      card.appendChild(classesGrid);
      trainsListContainer.appendChild(card);
    });

    resultsSection.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }

  // --- Real-time availability endpoint call ---
  async function checkLiveAvailability(trainId, className, date, badgeElement) {
    badgeElement.textContent = 'Checking...';
    try {
      const url = `/api/v1/trains/${trainId}/availability?class=${encodeURIComponent(className)}&date=${encodeURIComponent(date)}`;
      const res = await fetch(url);
      if (res.ok) {
        const data = await res.json();
        const avail = data.availableSeats;
        badgeElement.className = `seats-badge ${avail > 0 ? 'available' : 'unavailable'}`;
        badgeElement.textContent = avail > 0 ? `Seats: ${avail} Available` : 'Waitlist / Full (0)';
        showFlash(`Live update for ${className}: ${avail} seat(s) available on ${date}.`, 'info');
      } else {
        badgeElement.textContent = 'Check failed';
      }
    } catch (e) {
      badgeElement.textContent = 'Error';
    }
  }

  // --- Open Booking Section (Step 3) ---
  function openBookingSection(train, cls) {
    selectedTrain = train;
    selectedClass = cls;
    updateAuthNavUI();

    recapTrainName.textContent = `${train.name} (#${train.trainNo})`;
    recapRoute.textContent = `${train.sourceStation} (${formatTime(train.departureTime)}) ➔ ${train.destinationStation} (${formatTime(train.arrivalTime)}) &bull; ${activeSearchDate}`;
    recapClass.textContent = cls.className;
    recapFare.textContent = `₹${cls.fare}`;

    // Reset passengers table with 1 default row
    while (passengerRowsContainer.firstChild) {
      passengerRowsContainer.removeChild(passengerRowsContainer.firstChild);
    }
    passengerRowCount = 0;
    addPassengerRow();

    updateTotalFare();

    bookingSection.style.display = 'block';
    confirmationSection.style.display = 'none';
    bookingSection.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }

  // --- Passenger Row Management ---
  function addPassengerRow() {
    if (passengerRowCount >= 6) {
      showFlash('A maximum of 6 passengers can be booked per reservation.', 'warning');
      return;
    }

    passengerRowCount++;
    const rowId = passengerRowCount;

    const tr = document.createElement('tr');
    tr.id = `passengerRow_${rowId}`;

    // # index
    const tdIndex = document.createElement('td');
    tdIndex.textContent = String(rowId);
    tdIndex.className = 'row-index';
    tr.appendChild(tdIndex);

    // Full name
    const tdName = document.createElement('td');
    const nameInput = document.createElement('input');
    nameInput.type = 'text';
    nameInput.className = 'form-control passenger-name';
    nameInput.placeholder = 'e.g. John Doe';
    nameInput.required = true;
    nameInput.setAttribute('aria-label', `Passenger ${rowId} full name`);
    tdName.appendChild(nameInput);
    tr.appendChild(tdName);

    // Age
    const tdAge = document.createElement('td');
    const ageInput = document.createElement('input');
    ageInput.type = 'number';
    ageInput.className = 'form-control passenger-age';
    ageInput.placeholder = 'Age';
    ageInput.min = '1';
    ageInput.max = '120';
    ageInput.required = true;
    ageInput.setAttribute('aria-label', `Passenger ${rowId} age`);
    tdAge.appendChild(ageInput);
    tr.appendChild(tdAge);

    // Gender
    const tdGender = document.createElement('td');
    const genderSelect = document.createElement('select');
    genderSelect.className = 'form-control passenger-gender';
    genderSelect.setAttribute('aria-label', `Passenger ${rowId} gender`);

    ['Male', 'Female', 'Other'].forEach(opt => {
      const option = document.createElement('option');
      option.value = opt;
      option.textContent = opt;
      genderSelect.appendChild(option);
    });
    tdGender.appendChild(genderSelect);
    tr.appendChild(tdGender);

    // Berth Preference
    const tdBerth = document.createElement('td');
    const berthSelect = document.createElement('select');
    berthSelect.className = 'form-control passenger-berth';
    berthSelect.setAttribute('aria-label', `Passenger ${rowId} berth preference`);
    const berthOptions = [
      { val: 'NO_PREFERENCE', label: 'No Preference' },
      { val: 'LOWER', label: 'Lower Berth' },
      { val: 'MIDDLE', label: 'Middle Berth' },
      { val: 'UPPER', label: 'Upper Berth' },
      { val: 'SIDE_LOWER', label: 'Side Lower' },
      { val: 'SIDE_UPPER', label: 'Side Upper' },
      { val: 'WINDOW', label: 'Window Seat' }
    ];
    berthOptions.forEach(b => {
      const opt = document.createElement('option');
      opt.value = b.val;
      opt.textContent = b.label;
      berthSelect.appendChild(opt);
    });
    tdBerth.appendChild(berthSelect);
    tr.appendChild(tdBerth);

    // Meal Choice
    const tdMeal = document.createElement('td');
    const mealSelect = document.createElement('select');
    mealSelect.className = 'form-control passenger-meal';
    mealSelect.setAttribute('aria-label', `Passenger ${rowId} meal choice`);
    const mealOptions = [
      { val: 'NO_FOOD', label: 'No Food' },
      { val: 'VEG', label: 'Vegetarian' },
      { val: 'NON_VEG', label: 'Non-Veg' }
    ];
    mealOptions.forEach(m => {
      const opt = document.createElement('option');
      opt.value = m.val;
      opt.textContent = m.label;
      mealSelect.appendChild(opt);
    });
    tdMeal.appendChild(mealSelect);
    tr.appendChild(tdMeal);

    // Senior Citizen Discount Checkbox
    const tdSenior = document.createElement('td');
    tdSenior.className = 'senior-checkbox-wrapper';
    const seniorCheck = document.createElement('input');
    seniorCheck.type = 'checkbox';
    seniorCheck.className = 'senior-checkbox passenger-senior-check';
    seniorCheck.title = 'Available for age 60 and above (40% concession on base fare)';
    seniorCheck.setAttribute('aria-label', `Passenger ${rowId} senior citizen concession`);
    seniorCheck.disabled = true; // disabled until age >= 60
    tdSenior.appendChild(seniorCheck);
    tr.appendChild(tdSenior);

    // Listeners on age to toggle senior concession checkbox and re-quote fare
    ageInput.addEventListener('input', () => {
      const ageVal = parseInt(ageInput.value, 10);
      if (!isNaN(ageVal) && ageVal >= 60) {
        seniorCheck.disabled = false;
      } else {
        seniorCheck.disabled = true;
        seniorCheck.checked = false;
      }
      updateTotalFare();
    });

    seniorCheck.addEventListener('change', () => {
      updateTotalFare();
    });

    // Action (Remove)
    const tdAction = document.createElement('td');
    tdAction.style.textAlign = 'center';
    if (passengerRowCount > 1) {
      const removeBtn = document.createElement('button');
      removeBtn.type = 'button';
      removeBtn.className = 'btn btn-danger btn-sm';
      removeBtn.textContent = '✕';
      removeBtn.title = 'Remove passenger';
      removeBtn.setAttribute('aria-label', `Remove passenger ${rowId}`);
      removeBtn.addEventListener('click', () => {
        tr.remove();
        reindexPassengerRows();
        updateTotalFare();
      });
      tdAction.appendChild(removeBtn);
    } else {
      tdAction.textContent = '-';
    }
    tr.appendChild(tdAction);

    passengerRowsContainer.appendChild(tr);
    updateTotalFare();
  }

  function reindexPassengerRows() {
    const rows = passengerRowsContainer.querySelectorAll('tr');
    passengerRowCount = rows.length;
    rows.forEach((row, idx) => {
      const indexCell = row.querySelector('.row-index');
      if (indexCell) {
        indexCell.textContent = String(idx + 1);
      }
    });
  }

  addPassengerBtn.addEventListener('click', addPassengerRow);

  let fareQuoteDebounceTimer = null;

  function updateTotalFare() {
    if (!selectedClass) return;
    const rows = passengerRowsContainer.querySelectorAll('tr');
    const count = rows.length;
    if (farePassengerCount) farePassengerCount.textContent = String(count);

    const unitFare = parseFloat(selectedClass.fare) || 0;
    const isAc = (selectedClass.className || '').toUpperCase().includes('AC') ||
                 (selectedClass.className || '').toUpperCase().includes('CC') ||
                 (selectedClass.className || '').toUpperCase().includes('EC') ||
                 (selectedClass.className || '').toUpperCase().includes('1A') ||
                 (selectedClass.className || '').toUpperCase().includes('2A') ||
                 (selectedClass.className || '').toUpperCase().includes('3A');

    // Immediate optimistic local calculation
    let resFeePerSeat = isAc ? 40.00 : 20.00;
    let sfChargePerSeat = isAc ? 30.00 : 15.00;
    if (unitFare <= resFeePerSeat + sfChargePerSeat) {
      resFeePerSeat = 0;
      sfChargePerSeat = 0;
    }
    const taxablePerSeat = unitFare - resFeePerSeat - sfChargePerSeat;
    const basePerSeat = isAc ? Math.round((taxablePerSeat / 1.05) * 100) / 100 : taxablePerSeat;
    const gstPerSeat = isAc ? Math.round((taxablePerSeat - basePerSeat) * 100) / 100 : 0;

    let totalBase = 0;
    let totalSf = 0;
    let totalRes = 0;
    let totalGst = 0;
    let totalDiscount = 0;
    let seniorCount = 0;

    const passengerDtos = [];
    rows.forEach(r => {
      const ageInput = r.querySelector('.passenger-age');
      const seniorCheck = r.querySelector('.passenger-senior-check');
      const age = parseInt(ageInput ? ageInput.value : '', 10) || 0;
      const isSenior = age >= 60 && seniorCheck && seniorCheck.checked;

      totalBase += basePerSeat;
      totalSf += sfChargePerSeat;
      totalRes += resFeePerSeat;
      totalGst += gstPerSeat;

      if (isSenior) {
        seniorCount++;
        const disc = Math.round(basePerSeat * 0.40 * 100) / 100;
        totalDiscount += disc;
      }

      passengerDtos.push({
        name: (r.querySelector('.passenger-name') || {}).value || 'Passenger',
        age: age || 30,
        gender: (r.querySelector('.passenger-gender') || {}).value || 'Male',
        berthPreference: (r.querySelector('.passenger-berth') || {}).value || 'NO_PREFERENCE',
        mealPreference: (r.querySelector('.passenger-meal') || {}).value || 'NO_FOOD',
        seniorConcession: isSenior
      });
    });

    const localTotal = (totalBase + totalSf + totalRes + totalGst - totalDiscount).toFixed(2);
    if (breakdownBaseFare) breakdownBaseFare.textContent = `₹${totalBase.toFixed(2)}`;
    if (breakdownSuperfastCharge) breakdownSuperfastCharge.textContent = `₹${totalSf.toFixed(2)}`;
    if (breakdownReservationFee) breakdownReservationFee.textContent = `₹${totalRes.toFixed(2)}`;
    if (breakdownGst) breakdownGst.textContent = `₹${totalGst.toFixed(2)}`;

    if (totalDiscount > 0) {
      if (breakdownSeniorRow) breakdownSeniorRow.style.display = 'flex';
      if (breakdownSeniorDiscount) breakdownSeniorDiscount.textContent = `-₹${totalDiscount.toFixed(2)}`;
      if (seniorCountBadge) seniorCountBadge.textContent = String(seniorCount);
    } else {
      if (breakdownSeniorRow) breakdownSeniorRow.style.display = 'none';
    }

    totalFareDisplay.textContent = `₹${localTotal}`;
    passengerCountSummary.textContent = `${count} Passenger${count > 1 ? 's' : ''}`;

    // Debounced authoritative server quote
    clearTimeout(fareQuoteDebounceTimer);
    fareQuoteDebounceTimer = setTimeout(async () => {
      try {
        const quoteRes = await fetch('/api/v1/bookings/fare-quote', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            classId: selectedClass.classId,
            trainId: selectedTrain ? selectedTrain.trainId : null,
            journeyDate: activeSearchDate,
            passengers: passengerDtos
          })
        });
        if (quoteRes.ok) {
          const quote = await quoteRes.json();
          if (breakdownBaseFare) breakdownBaseFare.textContent = `₹${parseFloat(quote.baseFare).toFixed(2)}`;
          if (breakdownSuperfastCharge) breakdownSuperfastCharge.textContent = `₹${parseFloat(quote.superfastCharge).toFixed(2)}`;
          if (breakdownReservationFee) breakdownReservationFee.textContent = `₹${parseFloat(quote.reservationFee).toFixed(2)}`;
          if (breakdownGst) breakdownGst.textContent = `₹${parseFloat(quote.gst).toFixed(2)}`;

          if (quote.seniorDiscount && parseFloat(quote.seniorDiscount) > 0) {
            if (breakdownSeniorRow) breakdownSeniorRow.style.display = 'flex';
            if (breakdownSeniorDiscount) breakdownSeniorDiscount.textContent = `-₹${parseFloat(quote.seniorDiscount).toFixed(2)}`;
            if (seniorCountBadge) seniorCountBadge.textContent = String(quote.eligibleSeniorCount);
          } else {
            if (breakdownSeniorRow) breakdownSeniorRow.style.display = 'none';
          }

          totalFareDisplay.textContent = `₹${parseFloat(quote.totalFare).toFixed(2)}`;
          passengerCountSummary.textContent = `${count} Passenger${count > 1 ? 's' : ''}`;
        }
      } catch (_) {
        // Optimistic local calculation preserved
      }
    }, 150);
  }

  cancelBookingStepBtn.addEventListener('click', () => {
    bookingSection.style.display = 'none';
    resultsSection.scrollIntoView({ behavior: 'smooth' });
  });

  // --- Existing-user authentication helpers (RRS-F-002, RRS-F-010) ---
  function decodeJwtPayload(token) {
    try {
      const part = token.split('.')[1];
      if (!part) return null;
      const base64 = part.replace(/-/g, '+').replace(/_/g, '/');
      const padded = base64 + '='.repeat((4 - (base64.length % 4)) % 4);
      return JSON.parse(atob(padded));
    } catch (_) {
      return null;
    }
  }

  function isTokenExpired(token) {
    const payload = decodeJwtPayload(token);
    if (!payload || typeof payload.exp !== 'number') {
      return true;
    }
    return payload.exp * 1000 <= Date.now();
  }

  function clearStoredToken() {
    try {
      sessionStorage.removeItem('jwtToken');
    } catch (_) {
      // Storage may be unavailable.
    }
    try {
      localStorage.removeItem('jwtToken');
    } catch (_) {
      // Storage may be unavailable.
    }
  }

  function getStoredToken() {
    let token = null;
    try {
      token = sessionStorage.getItem('jwtToken') || localStorage.getItem('jwtToken');
    } catch (_) {
      return null;
    }
    if (!token) return null;
    if (isTokenExpired(token)) {
      clearStoredToken();
      return null;
    }
    return token;
  }

  // --- Session inactivity timeout (RRS-SR-005, TC-SEC-05) ---
  // The server re-issues a fresh 15-minute token (X-Auth-Token) on every authenticated
  // request, so the stored token only runs out after 15 minutes without activity.
  const SESSION_TIMEOUT_NOTICE = 'Your session timed out after 15 minutes of inactivity. Please sign in again.';
  let sessionTimeoutTimer = null;

  function storeToken(token) {
    try {
      sessionStorage.setItem('jwtToken', token);
    } catch (_) {
      return false;
    }
    scheduleSessionTimeout(token);
    return true;
  }

  function scheduleSessionTimeout(token) {
    if (sessionTimeoutTimer) clearTimeout(sessionTimeoutTimer);
    sessionTimeoutTimer = null;
    const payload = decodeJwtPayload(token);
    if (!payload || typeof payload.exp !== 'number') return;
    const msLeft = payload.exp * 1000 - Date.now();
    sessionTimeoutTimer = setTimeout(handleSessionTimeout, Math.max(msLeft, 0));
  }

  function handleSessionTimeout() {
    if (sessionTimeoutTimer) clearTimeout(sessionTimeoutTimer);
    sessionTimeoutTimer = null;
    clearStoredToken();
    openLoginModal(SESSION_TIMEOUT_NOTICE);
    showFlash('Session timed out due to inactivity. Please sign in again.', 'warning', false);
  }

  async function authFetch(url, token, options = {}) {
    const headers = Object.assign({}, options.headers, { 'Authorization': `Bearer ${token}` });
    const res = await fetch(url, Object.assign({}, options, { headers }));
    const renewed = res.headers.get('X-Auth-Token');
    if (renewed) storeToken(renewed);
    return res;
  }

  function userIdFromToken(token) {
    const payload = decodeJwtPayload(token);
    return payload && Number.isInteger(payload.userId) ? payload.userId : null;
  }

  async function signIn(email, password) {
    const res = await fetch('/api/v1/users/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password })
    });

    if (!res.ok) {
      return { ok: false, status: res.status };
    }

    const data = await res.json();
    if (!data || !data.token) {
      return { ok: false, status: 0 };
    }

    if (!storeToken(data.token)) {
      return { ok: false, status: 0 };
    }

    // The JWT already carries userId. Decoding it is a display convenience only;
    // the server remains the authority for authentication and ownership.
    const userId = userIdFromToken(data.token);
    if (userId && userIdInput) {
      userIdInput.value = userId;
      userIdInput.classList.remove('is-invalid');
      userIdError.style.display = 'none';
    }

    return { ok: true, status: 200 };
  }

  function openLoginModal(message) {
    if (!loginModal) return;
    if (loginStatus) loginStatus.textContent = message || '';
    loginModal.classList.add('active');
    if (loginEmail) loginEmail.focus();
  }

  function closeLoginModal() {
    if (loginModal) loginModal.classList.remove('active');
  }

  // --- Quick Guest / Passenger Account Creation ---
  quickRegisterBtn.addEventListener('click', async () => {
    quickRegisterBtn.disabled = true;
    quickRegisterBtn.textContent = 'Generating account...';

    const randomSuffix = Math.floor(1000 + Math.random() * 9000);
    const regPayload = {
      name: 'Demo Passenger',
      email: `demo${randomSuffix}@railwaydemo.com`,
      phone: `987654${randomSuffix}`,
      password: `Password@${randomSuffix}`
    };

    try {
      const res = await fetch('/api/v1/users/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(regPayload)
      });

      if (res.ok) {
        const data = await res.json();
        userIdInput.value = data.userId;
        userIdInput.classList.remove('is-invalid');
        userIdError.style.display = 'none';

        // Establish a genuine JWT session for this demo account. The protected
        // full-ticket and PDF endpoints require this token.
        const loginResult = await signIn(regPayload.email, regPayload.password);
        if (!loginResult.ok) {
          console.warn('Demo account created but sign-in failed with status', loginResult.status);
        }

        // Also pre-fill the primary passenger row to streamline the demo journey
        const firstRow = passengerRowsContainer.querySelector('tr');
        if (firstRow) {
          const nameInput = firstRow.querySelector('.passenger-name');
          const ageInput = firstRow.querySelector('.passenger-age');
          const genderSelect = firstRow.querySelector('.passenger-gender');

          if (nameInput) {
            nameInput.value = data.name || 'Demo Passenger';
            nameInput.classList.remove('is-invalid');
          }
          if (ageInput) {
            ageInput.value = '28';
            ageInput.classList.remove('is-invalid');
          }
          if (genderSelect) {
            genderSelect.value = 'Male';
          }
        }

        showFlash(`Demo account #${data.userId} registered and passenger details populated.`, 'success');
      } else {
        showFlash('Could not auto-generate demo user. Please manually enter an existing User ID.', 'warning');
      }
    } catch (err) {
      showFlash('Failed to reach user registration endpoint. Please enter your User ID manually.', 'error');
    } finally {
      quickRegisterBtn.disabled = false;
      quickRegisterBtn.textContent = 'Auto-Fill Demo Account';
    }
  });

  // --- Confirm Booking Submission ---
  passengerBookingForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    clearFlashes();

    // 1. Verify Authentication [RRS-SR-006]
    const token = getStoredToken();
    if (!token || isTokenExpired(token)) {
      openLoginModal('Please sign in to complete your booking.');
      return;
    }

    const userIdVal = (userIdInput && userIdInput.value) ? userIdInput.value.trim() : '1';

    // 2. Validate Passengers
    const passengerRows = passengerRowsContainer.querySelectorAll('tr');
    if (passengerRows.length === 0) {
      showFlash('At least one passenger must be added.', 'warning');
      return;
    }

    const passengers = [];
    let hasPassengerError = false;

    passengerRows.forEach((row, idx) => {
      const nameInput = row.querySelector('.passenger-name');
      const ageInput = row.querySelector('.passenger-age');
      const genderSelect = row.querySelector('.passenger-gender');

      const nameVal = nameInput.value.trim();
      const ageVal = parseInt(ageInput.value, 10);
      const genderVal = genderSelect.value;

      if (!nameVal) {
        nameInput.classList.add('is-invalid');
        hasPassengerError = true;
      } else {
        nameInput.classList.remove('is-invalid');
      }

      if (isNaN(ageVal) || ageVal < 1 || ageVal > 120) {
        ageInput.classList.add('is-invalid');
        hasPassengerError = true;
      } else {
        ageInput.classList.remove('is-invalid');
      }

      const berthSelect = row.querySelector('.passenger-berth');
      const mealSelect = row.querySelector('.passenger-meal');
      const seniorCheck = row.querySelector('.passenger-senior-check');

      const berthVal = berthSelect ? berthSelect.value : 'NO_PREFERENCE';
      const mealVal = mealSelect ? mealSelect.value : 'NO_FOOD';
      const seniorVal = Boolean(seniorCheck && seniorCheck.checked && ageVal >= 60);

      passengers.push({
        name: nameVal,
        age: ageVal,
        gender: genderVal,
        berthPreference: berthVal,
        mealPreference: mealVal,
        seniorConcession: seniorVal
      });
    });

    if (hasPassengerError) {
      showFlash('Please correct the highlighted passenger details. Name is required and age must be between 1 and 120.', 'warning');
      return;
    }

    // Guard duplicate submissions
    confirmBookingBtn.disabled = true;
    bookingBtnSpinner.style.display = 'inline-block';
    bookingBtnText.textContent = 'Reserving Seats...';

    const bookingPayload = {
      userId: parseInt(userIdVal, 10) || 1,
      trainId: selectedTrain.trainId,
      classId: selectedClass.classId,
      journeyDate: activeSearchDate,
      passengers: passengers
    };

    try {
      const res = await authFetch('/api/v1/bookings', token, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
        body: JSON.stringify(bookingPayload)
      });

      if (!res.ok) {
        let errDesc = 'Booking failed.';
        try {
          const errBody = await res.json();
          if (errBody && errBody.message) {
            errDesc = errBody.message;
          }
        } catch (_) {}
        showFlash(`Booking rejected (${res.status}): ${errDesc}`, 'error');
        return;
      }

      const confirmedBooking = await res.json();

      // RRS-F-008: Redirect to Payment Gateway
      if (confirmedBooking.paymentUrl) {
        showFlash(`Seats reserved! Redirecting to secure payment gateway (₹${confirmedBooking.totalFare})...`, 'info');
        setTimeout(() => {
          window.location.href = confirmedBooking.paymentUrl;
        }, 800);
        return;
      }

      // Never render the full e-ticket from an unverified booking response.
      showFlash('Booking created, but payment could not be initiated. The e-ticket becomes available only after verified payment.', 'warning');

    } catch (err) {
      showFlash(`Error connecting to booking service: ${err.message}`, 'error');
    } finally {
      confirmBookingBtn.disabled = false;
      bookingBtnSpinner.style.display = 'none';
      bookingBtnText.textContent = 'Confirm & Book Ticket';
    }
  });

  // --- Display Booking Confirmation (Step 4) ---
  function displayBookingConfirmation(booking) {
    bookingSection.style.display = 'none';
    resultsSection.style.display = 'none';

    confirmedPnrDisplay.textContent = booking.pnr || 'CONFIRMED';
    confTrainDetails.textContent = `${booking.trainName} (#${booking.trainNo})`;
    confRouteDetails.textContent = `${booking.sourceStation} ➔ ${booking.destinationStation}`;
    confJourneyDate.textContent = booking.journeyDate;
    confTimings.textContent = `Dep: ${formatTime(booking.departureTime)} | Arr: ${formatTime(booking.arrivalTime)}`;
    confClassDetails.textContent = `${booking.className} (₹${booking.farePerSeat} / seat)`;
    confTotalFare.textContent = `Total: ₹${booking.totalFare}`;

    // Payment status comes from the server-side Payment record, never from a
    // frontend query parameter such as ?payment=success.
    const paymentStatus = String(booking.paymentStatus || 'NOT VERIFIED').toUpperCase();
    const isPaymentVerified = ['SUCCESS', 'CONFIRMED', 'PAID'].includes(paymentStatus);
    const isBookingConfirmed = booking.status === 'CONFIRMED' && isPaymentVerified;

    confPaymentStatus.textContent = paymentStatus.replace(/_/g, ' ');
    confPaymentStatus.style.color = isPaymentVerified ? '#166534' : '#b45309';

    if (isBookingConfirmed) {
      confirmationHeading.textContent = 'Ticket Successfully Booked & Confirmed';
      confirmationSubtitle.textContent = 'Your reservation and payment are confirmed. Electronic reservation slip (ERS) generated below.';
    } else if (paymentStatus === 'FAILED') {
      confirmationHeading.textContent = 'Booking Payment Failed';
      confirmationSubtitle.textContent = 'The payment was not successful. This slip is not valid for travel.';
    } else {
      confirmationHeading.textContent = 'Ticket Booked - Payment Not Verified';
      confirmationSubtitle.textContent = 'Your seats are reserved, but payment is still pending verification. This slip is provisional.';
    }

    const downloadPdfBtn = document.getElementById('downloadPdfTicketBtn');
    if (downloadPdfBtn && booking && booking.pnr) {
      downloadPdfBtn.setAttribute('data-pnr', booking.pnr);
    }

    // Passengers & assigned seat numbers
    while (confirmedPassengersBody.firstChild) {
      confirmedPassengersBody.removeChild(confirmedPassengersBody.firstChild);
    }

    if (booking.passengers && booking.passengers.length > 0) {
      booking.passengers.forEach(p => {
        const row = document.createElement('tr');

        const tdSeat = document.createElement('td');
        const seatBadge = document.createElement('span');
        seatBadge.style.fontWeight = '700';
        seatBadge.style.background = '#dbeafe';
        seatBadge.style.color = '#1e40af';
        seatBadge.style.padding = '0.2rem 0.5rem';
        seatBadge.style.borderRadius = '4px';
        seatBadge.textContent = p.seatNo || 'Allocated';
        tdSeat.appendChild(seatBadge);
        row.appendChild(tdSeat);

        const tdName = document.createElement('td');
        tdName.textContent = p.name;
        tdName.style.fontWeight = '600';
        row.appendChild(tdName);

        const tdAge = document.createElement('td');
        tdAge.textContent = String(p.age);
        row.appendChild(tdAge);

        const tdGender = document.createElement('td');
        tdGender.textContent = p.gender;
        row.appendChild(tdGender);

        const tdPref = document.createElement('td');
        const prefParts = [];
        if (p.berthPreference && p.berthPreference !== 'NO_PREFERENCE') {
          prefParts.push(p.berthPreference.replace('_', ' '));
        }
        if (p.mealPreference && p.mealPreference !== 'NO_FOOD') {
          prefParts.push(p.mealPreference.replace('_', ' '));
        }
        if (p.seniorConcession) {
          prefParts.push('Senior 40%');
        }
        tdPref.textContent = prefParts.length > 0 ? prefParts.join(' • ') : 'Standard';
        tdPref.style.fontSize = '0.825rem';
        row.appendChild(tdPref);

        const tdStatus = document.createElement('td');
        const stBadge = document.createElement('span');
        stBadge.className = 'seats-badge available';
        stBadge.textContent = booking.status || 'CONFIRMED';
        tdStatus.appendChild(stBadge);
        row.appendChild(tdStatus);

        confirmedPassengersBody.appendChild(row);
      });
    }

    confirmationSection.style.display = 'block';
    showFlash(`Congratulations! Ticket booked successfully with PNR: ${booking.pnr}.`, 'success');
    confirmationSection.scrollIntoView({ behavior: 'smooth' });
  }

  // Copy PNR
  copyPnrBtn.addEventListener('click', () => {
    const pnrText = confirmedPnrDisplay.textContent;
    if (navigator.clipboard) {
      navigator.clipboard.writeText(pnrText).then(() => {
        showFlash(`PNR ${pnrText} copied to clipboard!`, 'info');
      });
    } else {
      showFlash(`PNR: ${pnrText}`, 'info');
    }
  });

  // Book Another
  bookAnotherBtn.addEventListener('click', () => {
    confirmationSection.style.display = 'none';
    resultsSection.style.display = 'none';
    bookingSection.style.display = 'none';
    trainSearchForm.reset();
    dateInput.value = `${tYyyy}-${tMm}-${tDd}`;
    window.scrollTo({ top: 0, behavior: 'smooth' });
  });

  // --- PNR Status Modal Actions ---
  openPnrModalBtn.addEventListener('click', () => {
    pnrModal.classList.add('active');
    pnrInput.focus();
  });

  closePnrModalBtn.addEventListener('click', () => {
    pnrModal.classList.remove('active');
    pnrResultContainer.style.display = 'none';
  });

  pnrModal.addEventListener('click', (e) => {
    if (e.target === pnrModal) {
      pnrModal.classList.remove('active');
      pnrResultContainer.style.display = 'none';
    }
  });

  const adminCommandCenterLink = document.getElementById('adminCommandCenterLink');

  function updateAuthNavUI() {
    const token = getStoredToken();
    if (token && !isTokenExpired(token)) {
      const payload = decodeJwtPayload(token);
      const email = payload && payload.sub ? payload.sub : 'Passenger';
      const userId = payload && payload.userId ? payload.userId : null;
      if (userIdInput && userId) {
        userIdInput.value = userId;
      }
      if (openLoginModalBtn) {
        openLoginModalBtn.innerHTML = `
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
            <circle cx="12" cy="7" r="4"/>
          </svg>
          Sign Out (${escapeHtml(email)})
        `;
        openLoginModalBtn.setAttribute('title', `Signed in as ${email}. Click to sign out.`);
      }
      if (adminCommandCenterLink) {
        adminCommandCenterLink.style.display = (payload && payload.role === 'ROLE_ADMIN') ? 'inline-flex' : 'none';
      }
      const emailDisplay = document.getElementById('authenticatedUserEmail');
      if (emailDisplay) {
        emailDisplay.textContent = email;
      }
    } else {
      if (adminCommandCenterLink) {
        adminCommandCenterLink.style.display = 'none';
      }
      if (openLoginModalBtn) {
        openLoginModalBtn.innerHTML = `
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
            <circle cx="12" cy="7" r="4"/>
          </svg>
          Sign In
        `;
        openLoginModalBtn.setAttribute('title', 'Sign In or Register');
      }
      const emailDisplay = document.getElementById('authenticatedUserEmail');
      if (emailDisplay) {
        emailDisplay.textContent = 'Guest (Sign in required to book)';
      }
    }
  }

  // --- Existing User Login Modal Actions ---
  openLoginModalBtn.addEventListener('click', () => {
    const token = getStoredToken();
    if (token && !isTokenExpired(token)) {
      clearStoredToken();
      updateAuthNavUI();
      showFlash('Signed out successfully.', 'info');
    } else {
      openLoginModal('');
    }
  });

  const switchBookingAccountBtn = document.getElementById('switchBookingAccountBtn');
  if (switchBookingAccountBtn) {
    switchBookingAccountBtn.addEventListener('click', () => {
      clearStoredToken();
      updateAuthNavUI();
      openLoginModal('Please sign in with the account you want to use for this reservation.');
    });
  }

  closeLoginModalBtn.addEventListener('click', closeLoginModal);
  loginModal.addEventListener('click', (e) => {
    if (e.target === loginModal) {
      closeLoginModal();
    }
  });

  loginForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const email = loginEmail.value.trim();
    const password = loginPassword.value;

    if (!email || !password) {
      loginStatus.textContent = 'Enter your email and password.';
      return;
    }

    loginSubmitBtn.disabled = true;
    loginSpinner.style.display = 'inline-block';
    loginSubmitText.textContent = 'Signing in...';
    loginStatus.textContent = '';

    try {
      const result = await signIn(email, password);
      if (result.ok) {
        loginPassword.value = '';
        closeLoginModal();
        updateAuthNavUI();

        // Check if there is a pending booking state to auto-resume! [RRS-SR-006]
        const pendingBookingRaw = sessionStorage.getItem('pendingBookingState');
        if (pendingBookingRaw) {
          try {
            const pendingBooking = JSON.parse(pendingBookingRaw);
            sessionStorage.removeItem('pendingBookingState');
            if (pendingBooking && pendingBooking.train && pendingBooking.cls) {
              openBookingSection(pendingBooking.train, pendingBooking.cls);
              showFlash('Signed in successfully! Resuming your booking.', 'success');
              return;
            }
          } catch (_) {
            sessionStorage.removeItem('pendingBookingState');
          }
        }

        showFlash('Signed in successfully. E-ticket access is enabled for the signed-in account.', 'success');

        const params = new URLSearchParams(window.location.search);
        const pendingPnr = params.get('pnr');
        if (pendingPnr) {
          await loadFullTicketForPnr(pendingPnr);
        }
      } else if (result.status === 401) {
        loginStatus.textContent = 'Invalid email or password.';
      } else if (result.status === 400) {
        loginStatus.textContent = 'Please check your email and password.';
      } else {
        loginStatus.textContent = 'Unable to sign in right now.';
      }
    } catch (err) {
      console.error('Login error:', err);
      loginStatus.textContent = 'Network error while signing in.';
    } finally {
      loginSubmitBtn.disabled = false;
      loginSpinner.style.display = 'none';
      loginSubmitText.textContent = 'Sign In';
    }
  });

  pnrSearchForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const pnr = pnrInput.value.trim().toUpperCase();

    if (!pnr || pnr.length !== 10) {
      pnrInput.classList.add('is-invalid');
      pnrInputError.style.display = 'block';
      return;
    }
    pnrInput.classList.remove('is-invalid');
    pnrInputError.style.display = 'none';

    pnrSearchSubmitBtn.disabled = true;
    pnrBtnSpinner.style.display = 'inline-block';

    while (pnrResultContainer.firstChild) {
      pnrResultContainer.removeChild(pnrResultContainer.firstChild);
    }

    try {
      const res = await fetch(`/api/v1/bookings/${encodeURIComponent(pnr)}`);
      pnrResultContainer.style.display = 'block';

      if (!res.ok) {
        const notFoundCard = document.createElement('div');
        notFoundCard.className = 'flash-banner error';
        notFoundCard.textContent = `No booking found for PNR "${pnr}". Please check the number and try again.`;
        pnrResultContainer.appendChild(notFoundCard);
        return;
      }

      const data = await res.json();

      const box = document.createElement('div');
      box.style.border = '1px solid var(--border)';
      box.style.borderRadius = 'var(--radius)';
      box.style.padding = '1rem';
      box.style.backgroundColor = '#f8fafc';

      const pnrTitle = document.createElement('div');
      pnrTitle.style.fontWeight = '700';
      pnrTitle.style.fontSize = '1.1rem';
      pnrTitle.textContent = `PNR: ${data.pnr} [${data.status || 'CONFIRMED'}]`;
      box.appendChild(pnrTitle);

      const trainLine = document.createElement('div');
      trainLine.style.fontSize = '0.9rem';
      trainLine.style.margin = '0.35rem 0';
      trainLine.textContent = `Train: ${data.trainName} (#${data.trainNo}) | Class: ${data.className}`;
      box.appendChild(trainLine);

      const routeLine = document.createElement('div');
      routeLine.style.fontSize = '0.9rem';
      routeLine.textContent = `Route: ${data.sourceStation} ➔ ${data.destinationStation} on ${data.journeyDate}`;
      box.appendChild(routeLine);

      const fareLine = document.createElement('div');
      fareLine.style.fontSize = '0.9rem';
      fareLine.style.marginTop = '0.35rem';
      fareLine.textContent = `Total Fare: ₹${data.totalFare ?? '-'}`;
      box.appendChild(fareLine);

      if (Array.isArray(data.passengers) && data.passengers.length > 0) {
        const passLine = document.createElement('div');
        passLine.style.fontSize = '0.9rem';
        passLine.style.marginTop = '0.35rem';
        passLine.innerHTML = '<strong>Passengers:</strong> ' + data.passengers
          .map(p => `${p.name || 'Passenger'} (Seat: ${p.seatNo || p.seatNumber || 'Confirmed'})`)
          .join(', ');
        box.appendChild(passLine);
      } else {
        const seatsLine = document.createElement('div');
        seatsLine.style.fontSize = '0.9rem';
        seatsLine.style.marginTop = '0.35rem';
        const seatNumbers = Array.isArray(data.seatNumbers) ? data.seatNumbers : [];
        seatsLine.textContent = seatNumbers.length > 0
          ? `Seats: ${seatNumbers.join(', ')}`
          : 'Seats: Not assigned';
        box.appendChild(seatsLine);
      }

      const note = document.createElement('div');
      note.style.fontSize = '0.8rem';
      note.style.color = 'var(--text-muted)';
      note.style.marginTop = '0.5rem';
      note.textContent = 'Sign in to view and download your full PDF e-ticket.';
      box.appendChild(note);

      pnrResultContainer.appendChild(box);

    } catch (err) {
      const errCard = document.createElement('div');
      errCard.className = 'flash-banner error';
      errCard.textContent = `Failed to query PNR: ${err.message}`;
      pnrResultContainer.appendChild(errCard);
    } finally {
      pnrSearchSubmitBtn.disabled = false;
      pnrBtnSpinner.style.display = 'none';
    }
  });

  // --- Utility: Format Time (HH:mm:ss -> HH:mm) ---
  function formatTime(timeStr) {
    if (!timeStr) return '--:--';
    const parts = timeStr.split(':');
    if (parts.length >= 2) {
      return `${parts[0]}:${parts[1]}`;
    }
    return timeStr;
  }

  async function loadFullTicketForPnr(pnr) {
    const token = getStoredToken();
    if (!token) {
      openLoginModal('Sign in to view your full e-ticket and download the PDF.');
      showFlash('Sign in to view your e-ticket. The full ticket is available only after server-side payment verification.', 'info');
      return;
    }

    try {
      const res = await authFetch(`/api/v1/bookings/${encodeURIComponent(pnr)}/ticket`, token);

      if (res.ok) {
        const booking = await res.json();
        displayBookingConfirmation(booking);
        return;
      }

      if (res.status === 401) {
        handleSessionTimeout();
        return;
      }

      if (res.status === 409) {
        const errorBody = await res.json().catch(() => ({}));
        showFlash(errorBody.message || 'Payment is still pending verification. The e-ticket and PDF are available only after verified payment.', 'warning');
        return;
      }

      if (res.status === 404) {
        showFlash(`No ticket found for PNR ${pnr} on the signed-in account.`, 'danger');
        return;
      }

      showFlash(`Unable to load e-ticket (HTTP ${res.status}).`, 'danger');
    } catch (err) {
      console.error('Error loading ticket:', err);
      showFlash('Network error while loading e-ticket.', 'danger');
    }
  }

  // --- Resume or end a stored session on page load (RRS-SR-005) ---
  (function initSessionTimeout() {
    if (new URLSearchParams(window.location.search).get('timeout') === '1') {
      handleSessionTimeout();
      return;
    }
    const existing = getStoredToken();
    if (existing) scheduleSessionTimeout(existing);
  })();

  // --- Handle Return from Payment Gateway (RRS-F-008, RRS-F-009, RRS-F-010) ---
  (async function checkPaymentReturn() {
    const urlParams = new URLSearchParams(window.location.search);
    const pnrParam = urlParams.get('pnr');
    const paymentStatus = urlParams.get('payment');
    const refParam = urlParams.get('ref');

    if (pnrParam && paymentStatus === 'success') {
      await loadFullTicketForPnr(pnrParam);
      if (refParam) {
        showFlash(`Payment authorized successfully (Ref: ${refParam}). Electronic ticket confirmed!`, 'success');
      }
    } else if (pnrParam && (paymentStatus === 'cancelled' || paymentStatus === 'failed')) {
      showFlash(`Payment for PNR ${pnrParam} was cancelled or declined. Held seats have been released.`, 'warning');
    }
  })();


  // --- Booking History Feature (RRS-F-014) ---
  let cachedBookings = [];
  let currentHistoryFilter = 'ALL';

  function openHistoryModal() {
    const token = getStoredToken();
    if (!token) {
      openLoginModal('Sign in to view your past and upcoming booking history.');
      showFlash('Sign in to view your booking history.', 'info');
      return;
    }
    if (historyModal) {
      historyModal.classList.add('active');
      loadBookingHistory();
    }
  }

  function closeHistoryModal() {
    if (historyModal) {
      historyModal.classList.remove('active');
    }
  }

  async function loadBookingHistory() {
    const token = getStoredToken();
    if (!token) {
      closeHistoryModal();
      openLoginModal(SESSION_TIMEOUT_NOTICE);
      return;
    }

    if (historyLoading) historyLoading.style.display = 'block';
    if (historyEmpty) historyEmpty.style.display = 'none';
    if (historyListContainer) {
      while (historyListContainer.firstChild) {
        historyListContainer.removeChild(historyListContainer.firstChild);
      }
    }

    try {
      const res = await authFetch('/api/v1/bookings', token);
      if (res.status === 401) {
        handleSessionTimeout();
        closeHistoryModal();
        return;
      }
      if (!res.ok) {
        const errCard = document.createElement('div');
        errCard.className = 'flash-banner error';
        errCard.textContent = `Failed to load booking history (HTTP ${res.status}).`;
        if (historyListContainer) historyListContainer.appendChild(errCard);
        return;
      }

      cachedBookings = await res.json();
      renderBookingHistory(cachedBookings, currentHistoryFilter);
    } catch (err) {
      console.error('History fetch error:', err);
      const errCard = document.createElement('div');
      errCard.className = 'flash-banner error';
      errCard.textContent = `Error fetching booking history: ${err.message}`;
      if (historyListContainer) historyListContainer.appendChild(errCard);
    } finally {
      if (historyLoading) historyLoading.style.display = 'none';
    }
  }

  function renderBookingHistory(bookings, filter) {
    if (!historyListContainer) return;
    while (historyListContainer.firstChild) {
      historyListContainer.removeChild(historyListContainer.firstChild);
    }

    const filtered = bookings.filter((b) => {
      if (filter === 'UPCOMING') return b.upcoming === true;
      if (filter === 'PAST') return b.upcoming === false;
      return true;
    });

    if (filtered.length === 0) {
      if (historyEmpty) historyEmpty.style.display = 'block';
      return;
    }
    if (historyEmpty) historyEmpty.style.display = 'none';

    filtered.forEach((b) => {
      const card = document.createElement('div');
      card.className = 'history-card';

      // Header row: PNR + Timeline tag + Status badge
      const header = document.createElement('div');
      header.className = 'history-card-header';

      const leftH = document.createElement('div');
      leftH.style.display = 'flex';
      leftH.style.alignItems = 'center';
      leftH.style.gap = '0.5rem';

      const pnrSpan = document.createElement('span');
      pnrSpan.style.fontFamily = 'monospace';
      pnrSpan.style.fontWeight = '700';
      pnrSpan.style.fontSize = '1.05rem';
      pnrSpan.textContent = b.pnr || 'N/A';
      leftH.appendChild(pnrSpan);

      const timelineTag = document.createElement('span');
      timelineTag.className = 'history-timeline-badge' + (b.upcoming ? ' upcoming' : '');
      timelineTag.textContent = b.upcoming ? 'Upcoming Journey' : 'Past Journey';
      leftH.appendChild(timelineTag);

      const rightH = document.createElement('div');
      const statusBadge = document.createElement('span');
      const statusKey = (b.status || '').toLowerCase();
      statusBadge.className = `history-status-badge status-${statusKey}`;
      statusBadge.textContent = b.status || 'UNKNOWN';
      rightH.appendChild(statusBadge);

      header.appendChild(leftH);
      header.appendChild(rightH);
      card.appendChild(header);

      // Body: Train, Route, Timings, Class, Fare
      const body = document.createElement('div');
      body.style.display = 'grid';
      body.style.gridTemplateColumns = 'repeat(auto-fit, minmax(200px, 1fr))';
      body.style.gap = '0.5rem';
      body.style.fontSize = '0.875rem';
      body.style.marginBottom = '0.75rem';

      const trainCol = document.createElement('div');
      trainCol.innerHTML = `<div><strong>Train:</strong> ${escapeHtml(b.trainNo || '')} &bull; ${escapeHtml(b.trainName || '')}</div>
                            <div><strong>Route:</strong> ${escapeHtml(b.sourceStation || '')} &rarr; ${escapeHtml(b.destinationStation || '')}</div>`;

      const journeyCol = document.createElement('div');
      journeyCol.innerHTML = `<div><strong>Journey Date:</strong> ${escapeHtml(b.journeyDate || '')}</div>
                              <div><strong>Time:</strong> Dept ${formatTime(b.departureTime)} | Arrv ${formatTime(b.arrivalTime)}</div>`;

      const fareCol = document.createElement('div');
      fareCol.innerHTML = `<div><strong>Class:</strong> ${escapeHtml(b.className || '')}</div>
                           <div><strong>Total Fare:</strong> &#8377;${escapeHtml(String(b.totalFare || '0.00'))}</div>`;

      body.appendChild(trainCol);
      body.appendChild(journeyCol);
      body.appendChild(fareCol);
      card.appendChild(body);

      // Passengers list
      if (b.passengers && b.passengers.length > 0) {
        const paxContainer = document.createElement('div');
        paxContainer.style.background = '#f8fafc';
        paxContainer.style.borderRadius = 'var(--radius)';
        paxContainer.style.padding = '0.5rem 0.75rem';
        paxContainer.style.marginBottom = '0.75rem';
        paxContainer.style.fontSize = '0.825rem';

        const paxTitle = document.createElement('div');
        paxTitle.style.fontWeight = '600';
        paxTitle.style.marginBottom = '0.25rem';
        paxTitle.style.color = 'var(--text-muted)';
        paxTitle.textContent = `Passengers (${b.passengers.length}):`;
        paxContainer.appendChild(paxTitle);

        const paxList = document.createElement('div');
        paxList.style.display = 'flex';
        paxList.style.flexWrap = 'wrap';
        paxList.style.gap = '0.5rem';

        b.passengers.forEach((p) => {
          const pItem = document.createElement('span');
          pItem.style.background = 'var(--color-paper-2)';
          pItem.style.border = '1px solid var(--border)';
          pItem.style.padding = '0.2rem 0.5rem';
          pItem.style.borderRadius = '4px';
          pItem.style.fontSize = '0.8rem';
          if (p.status === 'CANCELLED') {
            pItem.style.textDecoration = 'line-through';
            pItem.style.opacity = '0.6';
            pItem.textContent = `${p.name} (${p.age}, ${p.gender}) [CANCELLED]`;
          } else {
            pItem.textContent = `${p.name} (${p.age}, ${p.gender}) - Seat: ${p.seatNumber || p.seatNo || 'Assigned'}`;
          }
          paxList.appendChild(pItem);
        });
        paxContainer.appendChild(paxList);
        card.appendChild(paxContainer);
      }

      // Quick Actions
      const actionsRow = document.createElement('div');
      actionsRow.style.display = 'flex';
      actionsRow.style.gap = '0.5rem';
      actionsRow.style.flexWrap = 'wrap';
      actionsRow.style.justifyContent = 'flex-end';
      actionsRow.style.alignItems = 'center';

      if (b.status === 'CONFIRMED') {
        const viewTicketBtn = document.createElement('button');
        viewTicketBtn.type = 'button';
        viewTicketBtn.className = 'btn btn-sm btn-secondary';
        viewTicketBtn.textContent = 'View E-Ticket';
        viewTicketBtn.addEventListener('click', async () => {
          closeHistoryModal();
          await loadFullTicketForPnr(b.pnr);
        });
        actionsRow.appendChild(viewTicketBtn);

        const downloadTicketBtn = document.createElement('button');
        downloadTicketBtn.type = 'button';
        downloadTicketBtn.className = 'btn btn-sm btn-outline';
        downloadTicketBtn.textContent = 'Download PDF';
        downloadTicketBtn.addEventListener('click', async () => {
          const token = getStoredToken();
          if (!token) {
            openLoginModal('Sign in to download ticket PDF.');
            return;
          }
          try {
            const resp = await authFetch(`/api/v1/bookings/${encodeURIComponent(b.pnr)}/ticket.pdf`, token);
            if (resp.ok) {
              const blob = await resp.blob();
              const url = window.URL.createObjectURL(blob);
              const a = document.createElement('a');
              a.href = url;
              a.download = `ticket-${b.pnr}.pdf`;
              document.body.appendChild(a);
              a.click();
              a.remove();
              window.URL.revokeObjectURL(url);
            } else {
              showFlash('Unable to download PDF ticket.', 'danger');
            }
          } catch (e) {
            showFlash('Error downloading PDF: ' + e.message, 'danger');
          }
        });
        actionsRow.appendChild(downloadTicketBtn);

        if (b.cancellable) {
          const cancelBtn = document.createElement('button');
          cancelBtn.type = 'button';
          cancelBtn.className = 'btn btn-sm btn-danger';
          cancelBtn.textContent = 'Cancel / Manage Seats';
          cancelBtn.addEventListener('click', async () => {
            await handleBookingCancellation(b.pnr);
          });
          actionsRow.appendChild(cancelBtn);
        }
      } else if (b.status === 'PENDING' && b.paymentUrl) {
        const payLink = document.createElement('a');
        payLink.href = b.paymentUrl;
        payLink.className = 'btn btn-sm btn-primary';
        payLink.textContent = 'Complete Payment';
        actionsRow.appendChild(payLink);
      }

      if (actionsRow.children.length > 0) {
        card.appendChild(actionsRow);
      }

      historyListContainer.appendChild(card);
    });
  }

  // --- Partial Passenger Cancellation & Live Tiered Refund Preview (RRS-F-012-EXT) ---
  const cancellationModal = document.getElementById('cancellationModal');
  const closeCancellationModalBtn = document.getElementById('closeCancellationModalBtn');
  const closeCancellationModalBottomBtn = document.getElementById('closeCancellationModalBottomBtn');
  const cancellationLoading = document.getElementById('cancellationLoading');
  const cancellationDetails = document.getElementById('cancellationDetails');
  const cancelPnrDisplay = document.getElementById('cancelPnrDisplay');
  const cancelTrainDisplay = document.getElementById('cancelTrainDisplay');
  const cancelDepartureDisplay = document.getElementById('cancelDepartureDisplay');
  const cancelPassengerChecklist = document.getElementById('cancelPassengerChecklist');
  const cancelSelectAllBtn = document.getElementById('cancelSelectAllBtn');
  const cancelHoursBeforeDeparture = document.getElementById('cancelHoursBeforeDeparture');
  const cancelSelectedCount = document.getElementById('cancelSelectedCount');
  const cancelGrossFare = document.getElementById('cancelGrossFare');
  const cancelRefundTier = document.getElementById('cancelRefundTier');
  const cancelDeduction = document.getElementById('cancelDeduction');
  const cancelNetRefund = document.getElementById('cancelNetRefund');
  const confirmCancellationBtn = document.getElementById('confirmCancellationBtn');
  const cancelBtnSpinner = document.getElementById('cancelBtnSpinner');

  let activeCancellationBooking = null;

  function closeCancellationModal() {
    if (cancellationModal) {
      cancellationModal.classList.remove('active');
      cancellationModal.style.display = 'none';
    }
  }

  if (closeCancellationModalBtn) closeCancellationModalBtn.addEventListener('click', closeCancellationModal);
  if (closeCancellationModalBottomBtn) closeCancellationModalBottomBtn.addEventListener('click', closeCancellationModal);
  if (cancellationModal) {
    cancellationModal.addEventListener('click', (e) => {
      if (e.target === cancellationModal) {
        closeCancellationModal();
      }
    });
  }

  async function handleBookingCancellation(pnr) {
    const token = getStoredToken();
    if (!token) {
      openLoginModal('Sign in to cancel booking.');
      return;
    }

    if (!cancellationModal) return;

    cancellationModal.classList.add('active');
    cancellationModal.style.display = 'flex';
    cancellationLoading.style.display = 'block';
    cancellationDetails.style.display = 'none';
    confirmCancellationBtn.disabled = true;

    try {
      const res = await authFetch(`/api/v1/bookings/${encodeURIComponent(pnr)}`, token);
      if (!res.ok) {
        const errJson = await res.json().catch(() => ({}));
        showFlash(errJson.message || 'Unable to retrieve reservation details for cancellation.', 'danger');
        closeCancellationModal();
        return;
      }

      const booking = await res.json();
      activeCancellationBooking = booking;

      cancelPnrDisplay.textContent = booking.pnr;
      cancelTrainDisplay.textContent = `${booking.trainName || 'Train'} (#${booking.trainNo}) | Class: ${booking.className || 'SL'} (${booking.sourceStation} -> ${booking.destinationStation})`;
      cancelDepartureDisplay.textContent = `${booking.journeyDate} at ${booking.departureTime || 'Scheduled'}`;

      while (cancelPassengerChecklist.firstChild) {
        cancelPassengerChecklist.removeChild(cancelPassengerChecklist.firstChild);
      }

      const passengers = booking.passengers || [];
      const activePassengers = passengers.filter(p => !p.status || p.status !== 'CANCELLED');

      if (activePassengers.length === 0) {
        showFlash('All passengers on this booking are already cancelled.', 'warning');
        closeCancellationModal();
        return;
      }

      passengers.forEach((p) => {
        const item = document.createElement('div');
        item.style.display = 'flex';
        item.style.alignItems = 'center';
        item.style.justifyContent = 'space-between';
        item.style.padding = '0.625rem 0.875rem';
        item.style.border = '1px solid var(--color-rule)';
        item.style.borderRadius = '4px';

        const isCancelled = (p.status === 'CANCELLED');
        if (isCancelled) {
          item.style.opacity = '0.55';
          item.style.background = 'var(--color-paper-2)';
          item.style.borderStyle = 'dashed';

          const left = document.createElement('div');
          left.style.display = 'flex';
          left.style.alignItems = 'center';
          left.style.gap = '0.5rem';

          const nameSpan = document.createElement('span');
          nameSpan.style.textDecoration = 'line-through';
          nameSpan.style.fontWeight = '500';
          nameSpan.textContent = `${p.name} (${p.age} yrs, ${p.gender})`;
          left.appendChild(nameSpan);

          const badge = document.createElement('span');
          badge.className = 'badge';
          badge.style.background = 'var(--color-paper)';
          badge.style.border = '1px solid var(--color-rule)';
          badge.style.fontSize = '0.75rem';
          badge.style.color = 'var(--color-muted)';
          badge.textContent = `CANCELLED (Seat ${p.seatNumber || p.seatNo || '-'} Released)`;

          item.appendChild(left);
          item.appendChild(badge);
        } else {
          item.style.background = 'var(--color-paper)';

          const left = document.createElement('label');
          left.style.display = 'flex';
          left.style.alignItems = 'center';
          left.style.gap = '0.65rem';
          left.style.cursor = 'pointer';
          left.style.margin = '0';
          left.style.userSelect = 'none';

          const cb = document.createElement('input');
          cb.type = 'checkbox';
          cb.className = 'passenger-cancel-checkbox';
          cb.value = String(p.id);
          cb.checked = true;
          cb.style.width = '16px';
          cb.style.height = '16px';
          cb.style.cursor = 'pointer';
          cb.addEventListener('change', updateCancellationPreview);

          const nameSpan = document.createElement('span');
          nameSpan.style.fontWeight = '600';
          nameSpan.textContent = `${p.name} (${p.age} yrs, ${p.gender})`;

          left.appendChild(cb);
          left.appendChild(nameSpan);

          const badge = document.createElement('span');
          badge.className = 'badge';
          badge.style.background = 'var(--color-paper-2)';
          badge.style.border = '1px solid var(--color-rule)';
          badge.style.fontFamily = 'var(--font-mono)';
          badge.style.fontSize = '0.75rem';
          badge.textContent = `Seat: ${p.seatNumber || p.seatNo || 'Assigned'}`;

          item.appendChild(left);
          item.appendChild(badge);
        }

        cancelPassengerChecklist.appendChild(item);
      });

      cancellationLoading.style.display = 'none';
      cancellationDetails.style.display = 'block';

      await updateCancellationPreview();
    } catch (err) {
      showFlash('Failed to prepare cancellation: ' + err.message, 'danger');
      closeCancellationModal();
    }
  }

  async function updateCancellationPreview() {
    if (!activeCancellationBooking) return;
    const token = getStoredToken();
    if (!token) return;

    const checkboxes = cancelPassengerChecklist.querySelectorAll('.passenger-cancel-checkbox:checked');
    const selectedIds = Array.from(checkboxes).map(cb => parseInt(cb.value, 10)).filter(id => !isNaN(id));

    if (selectedIds.length === 0) {
      confirmCancellationBtn.disabled = true;
      cancelSelectedCount.textContent = '0';
      cancelGrossFare.textContent = '₹0.00';
      cancelRefundTier.textContent = '--%';
      cancelDeduction.textContent = '₹0.00';
      cancelNetRefund.textContent = '₹0.00';
      return;
    }

    try {
      const pnr = activeCancellationBooking.pnr;
      const previewRes = await authFetch(`/api/v1/bookings/${encodeURIComponent(pnr)}/cancel-passengers/preview`, token, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ passengerIds: selectedIds })
      });

      if (!previewRes.ok) {
        const errJson = await previewRes.json().catch(() => ({}));
        showFlash(errJson.message || 'Failed to calculate live refund preview.', 'danger');
        confirmCancellationBtn.disabled = true;
        return;
      }

      const quote = await previewRes.json();
      confirmCancellationBtn.disabled = false;
      cancelHoursBeforeDeparture.textContent = `${quote.hoursBeforeDeparture} hours before departure`;
      cancelSelectedCount.textContent = String(quote.passengerIds.length);
      cancelGrossFare.textContent = `₹${quote.grossFare.toFixed(2)}`;
      cancelRefundTier.textContent = `${quote.refundPercent}%`;
      cancelDeduction.textContent = `-₹${quote.deductionAmount.toFixed(2)}`;
      cancelNetRefund.textContent = `₹${quote.refundAmount.toFixed(2)}`;
    } catch (e) {
      showFlash('Preview calculation error: ' + e.message, 'danger');
    }
  }

  if (cancelSelectAllBtn) {
    cancelSelectAllBtn.addEventListener('click', async () => {
      const checkboxes = cancelPassengerChecklist.querySelectorAll('.passenger-cancel-checkbox');
      const allChecked = Array.from(checkboxes).every(cb => cb.checked);
      checkboxes.forEach(cb => { cb.checked = !allChecked; });
      cancelSelectAllBtn.textContent = allChecked ? 'Select All Active' : 'Deselect All';
      await updateCancellationPreview();
    });
  }

  if (confirmCancellationBtn) {
    confirmCancellationBtn.addEventListener('click', async () => {
      if (!activeCancellationBooking) return;
      const token = getStoredToken();
      if (!token) {
        openLoginModal('Sign in to confirm cancellation.');
        return;
      }

      const checkboxes = cancelPassengerChecklist.querySelectorAll('.passenger-cancel-checkbox:checked');
      const selectedIds = Array.from(checkboxes).map(cb => parseInt(cb.value, 10)).filter(id => !isNaN(id));

      if (selectedIds.length === 0) {
        showFlash('Please select at least one passenger to cancel.', 'warning');
        return;
      }

      confirmCancellationBtn.disabled = true;
      if (cancelBtnSpinner) cancelBtnSpinner.style.display = 'inline-block';

      try {
        const pnr = activeCancellationBooking.pnr;
        const cancelRes = await authFetch(`/api/v1/bookings/${encodeURIComponent(pnr)}/cancel-passengers`, token, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ passengerIds: selectedIds })
        });

        if (cancelRes.ok) {
          const res = await cancelRes.json();
          closeCancellationModal();

          const releasedSeatStr = (res.releasedSeats && res.releasedSeats.length > 0)
            ? ` Released seat(s): ${res.releasedSeats.join(', ')}.`
            : '';
          const statusStr = (res.remainingActivePassengers > 0)
            ? `${res.remainingActivePassengers} passenger(s) remain active on reservation.`
            : 'Entire reservation transitioned to CANCELLED.';

          showFlash(`Passenger cancellation processed successfully.${releasedSeatStr} Refund of ₹${res.refundAmount} initiated (Ref: ${res.refundRef}). ${statusStr}`, 'success');
          await loadBookingHistory();
        } else {
          const errJson = await cancelRes.json().catch(() => ({}));
          showFlash(errJson.message || 'Failed to process cancellation.', 'danger');
          confirmCancellationBtn.disabled = false;
        }
      } catch (err) {
        showFlash('Error executing cancellation: ' + err.message, 'danger');
        confirmCancellationBtn.disabled = false;
      } finally {
        if (cancelBtnSpinner) cancelBtnSpinner.style.display = 'none';
      }
    });
  }

  function escapeHtml(str) {
    if (str == null) return '';
    return String(str)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#039;');
  }

  // History Filter Buttons
  function setHistoryFilter(filter) {
    currentHistoryFilter = filter;
    [historyFilterAll, historyFilterUpcoming, historyFilterPast].forEach((btn) => {
      if (!btn) return;
      if (btn.getAttribute('data-filter') === filter) {
        btn.classList.add('active');
      } else {
        btn.classList.remove('active');
      }
    });
    renderBookingHistory(cachedBookings, currentHistoryFilter);
  }

  if (historyFilterAll) historyFilterAll.addEventListener('click', () => setHistoryFilter('ALL'));
  if (historyFilterUpcoming) historyFilterUpcoming.addEventListener('click', () => setHistoryFilter('UPCOMING'));
  if (historyFilterPast) historyFilterPast.addEventListener('click', () => setHistoryFilter('PAST'));

  if (openHistoryModalBtn) openHistoryModalBtn.addEventListener('click', openHistoryModal);
  if (closeHistoryModalBtn) closeHistoryModalBtn.addEventListener('click', closeHistoryModal);
  if (historyModal) {
    historyModal.addEventListener('click', (e) => {
      if (e.target === historyModal) {
        closeHistoryModal();
      }
    });
  }

  // --- Route & Halts Modal Actions (RRS-F-004-UI) ---
  function closeRouteModal() {
    if (routeModal) {
      routeModal.classList.remove('active');
      routeModal.style.display = 'none';
    }
  }

  if (closeRouteModalBtn) closeRouteModalBtn.addEventListener('click', closeRouteModal);
  if (closeRouteModalBottomBtn) closeRouteModalBottomBtn.addEventListener('click', closeRouteModal);
  if (routeModal) {
    routeModal.addEventListener('click', (e) => {
      if (e.target === routeModal) {
        closeRouteModal();
      }
    });
  }

  async function openRouteModal(train) {
    if (!routeModal) return;

    routeModalTrainName.textContent = `${train.name || 'Express Train'} (#${train.trainNo || 'N/A'})`;
    routeModalSubtitle.textContent = `${train.sourceStation} ➔ ${train.destinationStation}`;

    while (routeModalTableContainer.firstChild) {
      routeModalTableContainer.removeChild(routeModalTableContainer.firstChild);
    }

    routeModal.classList.add('active');
    routeModal.style.display = 'flex';

    let stops = train.stops;
    if (!stops || stops.length === 0) {
      routeModalLoading.style.display = 'block';
      try {
        const res = await fetch(`/api/v1/trains/${train.trainId}/route`);
        if (res.ok) {
          stops = await res.json();
        } else {
          stops = [];
        }
      } catch (_) {
        stops = [];
      } finally {
        routeModalLoading.style.display = 'none';
      }
    }

    renderRouteTable(stops, train);
  }

  function renderRouteTable(stops, train) {
    while (routeModalTableContainer.firstChild) {
      routeModalTableContainer.removeChild(routeModalTableContainer.firstChild);
    }

    if (!stops || stops.length === 0) {
      const emptyNotice = document.createElement('p');
      emptyNotice.style.padding = '1.5rem';
      emptyNotice.style.color = 'var(--color-muted)';
      emptyNotice.style.textAlign = 'center';
      emptyNotice.textContent = 'Timetable halt details are currently not available for this service.';
      routeModalTableContainer.appendChild(emptyNotice);
      return;
    }

    const table = document.createElement('table');
    table.className = 'timeline-table';

    const thead = document.createElement('thead');
    thead.innerHTML = `
      <tr>
        <th style="width: 48px;">#</th>
        <th>Station</th>
        <th>Arrival</th>
        <th>Departure</th>
        <th>Halt</th>
        <th>Platform</th>
        <th>Distance</th>
      </tr>
    `;
    table.appendChild(thead);

    const tbody = document.createElement('tbody');
    stops.forEach((s, idx) => {
      const isOrigin = idx === 0;
      const isDest = idx === stops.length - 1;

      const tr = document.createElement('tr');
      if (isOrigin) tr.className = 'origin-row';
      if (isDest) tr.className = 'dest-row';

      const badgeHtml = isOrigin
        ? '<span class="stop-badge origin">Origin</span>'
        : isDest
        ? '<span class="stop-badge dest">Destination</span>'
        : '<span class="stop-badge">Halt</span>';

      const arrText = isOrigin ? '--' : formatTime(s.arrivalTime);
      const depText = isDest ? '--' : formatTime(s.departureTime);
      const haltText = (isOrigin || isDest || !s.haltMinutes) ? '--' : `${s.haltMinutes}m`;
      const pfText = s.platformNo ? `PF ${s.platformNo}` : '--';
      const distText = `${s.distanceKm || 0} km`;

      tr.innerHTML = `
        <td style="color: var(--color-muted);">${s.stopOrder || idx + 1}</td>
        <td>
          <div style="display: flex; align-items: center; gap: 0.5rem;">
            <span class="station-code-pill">${escapeHtml(s.stationCode || '')}</span>
            <span style="font-weight: 600;">${escapeHtml(s.stationName || '')}</span>
            ${badgeHtml}
          </div>
        </td>
        <td>${arrText}</td>
        <td>${depText}</td>
        <td>${haltText}</td>
        <td>${pfText}</td>
        <td>${distText}</td>
      `;

      tbody.appendChild(tr);
    });

    table.appendChild(tbody);
    routeModalTableContainer.appendChild(table);
  }

  // Global Escape Key Listener for Modals & Dropdowns (WCAG 2.1)
  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') {
      if (routeModal && routeModal.classList.contains('active')) closeRouteModal();
      if (loginModal && loginModal.classList.contains('active')) closeLoginModal();
      if (pnrModal && pnrModal.classList.contains('active')) pnrModal.classList.remove('active');
      if (historyModal && historyModal.classList.contains('active')) closeHistoryModal();
      if (srcSuggestions) srcSuggestions.style.display = 'none';
      if (dstSuggestions) dstSuggestions.style.display = 'none';
    }
  });

  // --- Secure PDF Download Handler (RRS-F-010, RRS-SR-004) ---
  const downloadPdfBtn = document.getElementById('downloadPdfTicketBtn');
  if (downloadPdfBtn) {
    downloadPdfBtn.addEventListener('click', async (e) => {
      e.preventDefault();
      const pnr = downloadPdfBtn.getAttribute('data-pnr') || (confirmedPnrDisplay ? confirmedPnrDisplay.textContent.trim() : '');
      if (!pnr || pnr === 'XXXXXXXXXX' || pnr === 'CONFIRMED') {
        showFlash('No active booking PNR available for PDF download.', 'warning');
        return;
      }

      // The server enforces both verified payment and ticket ownership.
      const token = getStoredToken();

      if (!token) {
        showFlash(
          `Sign in to download the ticket PDF for PNR ${pnr}. PDFs are available only after server-side payment verification.`,
          'info'
        );
        openLoginModal('Sign in to download your ticket PDF.');
        return;
      }

      try {
        const response = await authFetch(`/api/v1/bookings/${encodeURIComponent(pnr)}/ticket.pdf`, token);
        if (response.ok) {
          const blob = await response.blob();
          const downloadUrl = window.URL.createObjectURL(blob);
          const a = document.createElement('a');
          a.href = downloadUrl;
          a.download = `ticket-${pnr}.pdf`;
          document.body.appendChild(a);
          a.click();
          a.remove();
          window.URL.revokeObjectURL(downloadUrl);
        } else if (response.status === 401) {
          handleSessionTimeout();
        } else if (response.status === 409) {
          const errorBody = await response.json().catch(() => ({}));
          showFlash(errorBody.message || 'Payment has not been verified. PDF download is unavailable until payment is confirmed.', 'warning');
        } else if (response.status === 403) {
          showFlash('You are not authorized to download this ticket PDF.', 'danger');
        } else if (response.status === 404) {
          showFlash('Ticket not found for the signed-in account.', 'danger');
        } else {
          showFlash(`Unable to download ticket PDF (HTTP ${response.status}).`, 'danger');
        }
      } catch (err) {
        console.error('PDF download error:', err);
        showFlash('Network error while requesting ticket PDF.', 'danger');
      }
    });
  }

  // Initialize Authentication Navigation UI on page load [RRS-SR-006]
  updateAuthNavUI();

})();
