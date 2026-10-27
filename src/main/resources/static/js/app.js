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
  const confirmedPnrDisplay = document.getElementById('confirmedPnrDisplay');
  const copyPnrBtn = document.getElementById('copyPnrBtn');
  const confTrainDetails = document.getElementById('confTrainDetails');
  const confRouteDetails = document.getElementById('confRouteDetails');
  const confJourneyDate = document.getElementById('confJourneyDate');
  const confTimings = document.getElementById('confTimings');
  const confClassDetails = document.getElementById('confClassDetails');
  const confTotalFare = document.getElementById('confTotalFare');
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
  dstInput.addEventListener('input', validateStationInputs);
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

  // --- Quick Guest / Passenger Account Creation ---
  quickRegisterBtn.addEventListener('click', async () => {
    quickRegisterBtn.disabled = true;
    quickRegisterBtn.textContent = 'Generating account...';

    const randomSuffix = Math.floor(1000 + Math.random() * 9000);
    const regPayload = {
      name: `Guest Passenger ${randomSuffix}`,
      email: `guest${randomSuffix}@railwaydemo.com`,
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
        showFlash(`Demo passenger account created! User ID #${data.userId} auto-populated.`, 'success');
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

    // 1. Validate User ID
    const userIdVal = userIdInput.value.trim();
    if (!userIdVal || isNaN(parseInt(userIdVal, 10)) || parseInt(userIdVal, 10) <= 0) {
      userIdInput.classList.add('is-invalid');
      userIdError.style.display = 'block';
      showFlash('Please enter a valid numeric User ID, or click "Auto-Create Demo Passenger Account".', 'warning');
      userIdInput.focus();
      return;
    }
    userIdInput.classList.remove('is-invalid');
    userIdError.style.display = 'none';

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
      userId: parseInt(userIdVal, 10),
      trainId: selectedTrain.trainId,
      classId: selectedClass.classId,
      journeyDate: activeSearchDate,
      passengers: passengers
    };

    try {
      const res = await fetch('/api/v1/bookings', {
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
      displayBookingConfirmation(confirmedBooking);

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

      if (data.passengers && data.passengers.length > 0) {
        const passHeader = document.createElement('div');
        passHeader.style.fontWeight = '600';
        passHeader.style.marginTop = '0.75rem';
        passHeader.style.fontSize = '0.85rem';
        passHeader.textContent = 'PASSENGERS:';
        box.appendChild(passHeader);

        data.passengers.forEach(p => {
          const pDiv = document.createElement('div');
          pDiv.style.fontSize = '0.85rem';
          pDiv.style.padding = '0.2rem 0';
          pDiv.textContent = `• ${p.name} (Age: ${p.age}, ${p.gender}) — Seat: ${p.seatNo || 'Allocated'}`;
          box.appendChild(pDiv);
        });
      }

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

})();
