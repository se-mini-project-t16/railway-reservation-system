/**
 * Railway Reservation System — Booking UI & UX Logic
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
  const swapStationsBtn = document.getElementById('swapStationsBtn');
  const searchSubmitBtn = document.getElementById('searchSubmitBtn');
  const searchBtnSpinner = document.getElementById('searchBtnSpinner');
  const searchBtnText = document.getElementById('searchBtnText');

  // Results Section
  const resultsSection = document.getElementById('resultsSection');
  const resultsCount = document.getElementById('resultsCount');
  const resultsRouteSummary = document.getElementById('resultsRouteSummary');
  const trainsListContainer = document.getElementById('trainsListContainer');

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

  // --- Pre-fill default journey date (tomorrow) & min date (today) ---
  const today = new Date();
  const yyyy = today.getFullYear();
  const mm = String(today.getMonth() + 1).padStart(2, '0');
  const dd = String(today.getDate()).padStart(2, '0');
  const minDateStr = `${yyyy}-${mm}-${dd}`;
  dateInput.min = minDateStr;

  const tomorrow = new Date();
  tomorrow.setDate(today.getDate() + 1);
  const tYyyy = tomorrow.getFullYear();
  const tMm = String(tomorrow.getMonth() + 1).padStart(2, '0');
  const tDd = String(tomorrow.getDate()).padStart(2, '0');
  dateInput.value = `${tYyyy}-${tMm}-${tDd}`;

  // --- Station Swap Action ---
  swapStationsBtn.addEventListener('click', () => {
    const temp = srcInput.value;
    srcInput.value = dstInput.value;
    dstInput.value = temp;
    validateStationInputs();
  });

  // --- Quick Route Chips ---
  document.querySelectorAll('.chip').forEach(chip => {
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

    // Hide downstream sections
    resultsSection.style.display = 'none';
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

      const title = document.createElement('h3');
      title.textContent = 'No Trains Found for Route';
      emptyState.appendChild(title);

      const p = document.createElement('p');
      p.textContent = `There are no scheduled trains operating directly between ${src} and ${dst} on ${date}. Please verify the station spellings or try another journey date.`;
      emptyState.appendChild(p);

      trainsListContainer.appendChild(emptyState);
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

  function updateTotalFare() {
    if (!selectedClass) return;
    const count = passengerRowsContainer.querySelectorAll('tr').length;
    const unitFare = parseFloat(selectedClass.fare) || 0;
    const total = (count * unitFare).toFixed(2);
    totalFareDisplay.textContent = `₹${total}`;
    passengerCountSummary.textContent = `${count} Passenger${count > 1 ? 's' : ''} (₹${unitFare} each)`;
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

      passengers.push({
        name: nameVal,
        age: ageVal,
        gender: genderVal
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
      const emailDisplay = document.getElementById('authenticatedUserEmail');
      if (emailDisplay) {
        emailDisplay.textContent = email;
      }
    } else {
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
          pItem.style.background = '#ffffff';
          pItem.style.border = '1px solid var(--border)';
          pItem.style.padding = '0.2rem 0.5rem';
          pItem.style.borderRadius = '4px';
          pItem.textContent = `${p.name} (${p.age}, ${p.gender}) — Seat: ${p.seatNo || 'Unassigned'}`;
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
          cancelBtn.textContent = 'Cancel Booking';
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

  async function handleBookingCancellation(pnr) {
    const token = getStoredToken();
    if (!token) {
      openLoginModal('Sign in to cancel booking.');
      return;
    }

    try {
      // 1. Fetch refund preview
      const previewRes = await authFetch(`/api/v1/bookings/${encodeURIComponent(pnr)}/refund-preview`, token);
      if (!previewRes.ok) {
        const errJson = await previewRes.json().catch(() => ({}));
        showFlash(errJson.message || 'Cannot preview refund for this booking.', 'danger');
        return;
      }
      const quote = await previewRes.json();
      const confirmMsg = `Are you sure you want to cancel booking ${pnr}?\n\nHours before departure: ${quote.hoursBeforeDeparture}\nRefund Percentage: ${quote.refundPercent}%\nRefund Amount: ₹${quote.refundAmount}\n\nSeats will be released immediately.`;
      if (!window.confirm(confirmMsg)) {
        return;
      }

      // 2. Perform cancellation
      const cancelRes = await authFetch(`/api/v1/bookings/${encodeURIComponent(pnr)}/cancel`, token, { method: 'POST' });
      if (cancelRes.ok) {
        const cancelData = await cancelRes.json();
        showFlash(`Booking ${pnr} cancelled successfully. Refund of ₹${cancelData.refundAmount} initiated (Ref: ${cancelData.refundRef}).`, 'success');
        await loadBookingHistory();
      } else {
        const errJson = await cancelRes.json().catch(() => ({}));
        showFlash(errJson.message || 'Failed to cancel booking.', 'danger');
      }
    } catch (err) {
      showFlash('Error during cancellation: ' + err.message, 'danger');
    }
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
