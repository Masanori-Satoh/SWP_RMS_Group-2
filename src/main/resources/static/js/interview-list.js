/* ============================================================
   Mộc Careers — Interview List Page Logic
   Module: interview/list
   ============================================================ */

document.addEventListener('DOMContentLoaded', function () {
  const cards = Array.from(document.querySelectorAll('.job-grid .job-card'));
  if (cards.length === 0) return;

  // 1. Gather all dates, jobs, interviewers
  const meetingDateCounts = {};
  const jobTitlesSet = new Set();
  const interviewersSet = new Set();

  cards.forEach(card => {
    const d = card.dataset.date;
    if (d) {
      meetingDateCounts[d] = (meetingDateCounts[d] || 0) + 1;
    }
    if (card.dataset.job) {
      jobTitlesSet.add(card.dataset.job.trim());
    }
    card.querySelectorAll('.panel-member').forEach(m => {
      const name = m.dataset.memberName;
      if (name) interviewersSet.add(name.trim());
    });
  });

  // Populate Job filter
  const jobSelect = document.getElementById('filter-job');
  Array.from(jobTitlesSet).sort().forEach(job => {
    const opt = document.createElement('option');
    opt.value = job;
    opt.textContent = job;
    jobSelect.appendChild(opt);
  });

  // Populate Interviewer filter
  const interviewerSelect = document.getElementById('filter-interviewer');
  Array.from(interviewersSet).sort().forEach(name => {
    const opt = document.createElement('option');
    opt.value = name;
    opt.textContent = name;
    interviewerSelect.appendChild(opt);
  });

  // Date helper: YYYY-MM-DD
  function getLocalDateStr(d) {
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${y}-${m}-${day}`;
  }

  const todayStr = getLocalDateStr(new Date());

  // State: Default is today
  let selectedDate = todayStr;
  let activePreset = 'today'; // 'today', 'this-week', 'this-month', 'all', 'custom'
  let currentCalMonth = new Date().getMonth();
  let currentCalYear = new Date().getFullYear();

  // Elements
  const calTitle = document.getElementById('cal-month-year');
  const calDaysGrid = document.getElementById('cal-days');
  const btnCalPrev = document.getElementById('cal-prev');
  const btnCalNext = document.getElementById('cal-next');

  const btnFilterToday = document.getElementById('btn-filter-today');
  const btnFilterThisWeek = document.getElementById('btn-filter-this-week');
  const btnFilterThisMonth = document.getElementById('btn-filter-this-month');
  const btnFilterAll = document.getElementById('btn-filter-all');

  const searchInput = document.getElementById('filter-search');
  const levelSelect = document.getElementById('filter-level');
  const statusSelect = document.getElementById('filter-status');

  const countText = document.getElementById('filter-count-text');
  const activePillsContainer = document.getElementById('active-pills');
  const btnClearAll = document.getElementById('btn-clear-all');

  const emptyState = document.getElementById('filter-empty-state');
  const emptyStateTitle = document.getElementById('empty-state-title');
  const emptyStateDesc = document.getElementById('empty-state-desc');
  const btnEmptyAction = document.getElementById('btn-empty-action');

  // Job Level deduction
  function getJobLevel(title) {
    const t = (title || '').toLowerCase();
    if (t.includes('senior') || t.includes('sr.') || t.includes('trưởng')) return 'Senior';
    if (t.includes('lead') || t.includes('quản lý') || t.includes('manager')) return 'Lead';
    if (t.includes('junior') || t.includes('jr.') || t.includes('fresher')) return 'Junior';
    if (t.includes('intern') || t.includes('thực tập')) return 'Intern';
    if (t.includes('middle') || t.includes('mid')) return 'Middle';
    return 'Other';
  }

  // Render Mini Calendar
  function renderCalendar() {
    const monthNames = [
      'Tháng 1', 'Tháng 2', 'Tháng 3', 'Tháng 4', 'Tháng 5', 'Tháng 6',
      'Tháng 7', 'Tháng 8', 'Tháng 9', 'Tháng 10', 'Tháng 11', 'Tháng 12'
    ];
    calTitle.textContent = `${monthNames[currentCalMonth]}, ${currentCalYear}`;
    calDaysGrid.innerHTML = '';

    const firstDay = new Date(currentCalYear, currentCalMonth, 1).getDay();
    const startOffset = (firstDay + 6) % 7;
    const daysInMonth = new Date(currentCalYear, currentCalMonth + 1, 0).getDate();

    for (let i = 0; i < startOffset; i++) {
      const emptyCell = document.createElement('div');
      emptyCell.className = 'cal-day empty';
      calDaysGrid.appendChild(emptyCell);
    }

    for (let day = 1; day <= daysInMonth; day++) {
      const mStr = String(currentCalMonth + 1).padStart(2, '0');
      const dStr = String(day).padStart(2, '0');
      const dateKey = `${currentCalYear}-${mStr}-${dStr}`;

      const isToday = dateKey === todayStr;
      const isSelected = selectedDate === dateKey;
      const hasMeeting = Boolean(meetingDateCounts[dateKey]);

      const dayBtn = document.createElement('button');
      dayBtn.type = 'button';
      dayBtn.className = 'cal-day' +
        (isToday ? ' today' : '') +
        (isSelected ? ' active' : '') +
        (hasMeeting ? ' has-meeting' : '');
      dayBtn.dataset.date = dateKey;
      dayBtn.title = `${dateKey}: ${hasMeeting ? meetingDateCounts[dateKey] + ' cuộc họp' : 'Không có lịch'}`;

      dayBtn.innerHTML = `<span>${day}</span>${hasMeeting ? '<span class="cal-dot"></span>' : ''}`;

      dayBtn.addEventListener('click', function () {
        selectedDate = this.dataset.date;
        activePreset = 'custom';
        updatePresetButtons();
        renderCalendar();
        applyFilters();
      });

      calDaysGrid.appendChild(dayBtn);
    }
  }

  function updatePresetButtons() {
    btnFilterToday.classList.toggle('active', activePreset === 'today');
    btnFilterThisWeek.classList.toggle('active', activePreset === 'this-week');
    btnFilterThisMonth.classList.toggle('active', activePreset === 'this-month');
    btnFilterAll.classList.toggle('active', activePreset === 'all');
  }

  function getWeekRange(date) {
    const d = new Date(date);
    const day = d.getDay();
    const diffToMonday = (day + 6) % 7;
    const monday = new Date(d);
    monday.setDate(d.getDate() - diffToMonday);
    monday.setHours(0, 0, 0, 0);
    const sunday = new Date(monday);
    sunday.setDate(monday.getDate() + 6);
    sunday.setHours(23, 59, 59, 999);
    return { start: getLocalDateStr(monday), end: getLocalDateStr(sunday) };
  }

  function applyFilters() {
    const searchVal = searchInput.value.trim().toLowerCase();
    const jobVal = jobSelect.value;
    const levelVal = levelSelect.value;
    const statusVal = statusSelect.value;
    const interviewerVal = interviewerSelect.value;

    const currentWeek = getWeekRange(new Date());
    const currentMonthPrefix = todayStr.substring(0, 7);

    let visibleCount = 0;

    cards.forEach(card => {
      const cardDate = card.dataset.date || '';
      const cardJob = card.dataset.job || '';
      const cardCandidate = (card.dataset.candidate || '').toLowerCase();
      const cardEmail = (card.dataset.candidateEmail || '').toLowerCase();
      const cardAppId = (card.dataset.appId || '').toLowerCase();
      const cardStatus = card.dataset.status || '';
      const cardLevel = getJobLevel(cardJob);

      // Date Check
      let dateMatch = true;
      if (activePreset === 'today' || activePreset === 'custom') {
        dateMatch = cardDate === selectedDate;
      } else if (activePreset === 'this-week') {
        dateMatch = cardDate >= currentWeek.start && cardDate <= currentWeek.end;
      } else if (activePreset === 'this-month') {
        dateMatch = cardDate.startsWith(currentMonthPrefix);
      } else if (activePreset === 'all') {
        dateMatch = true;
      }

      // Search Check
      let searchMatch = true;
      if (searchVal) {
        searchMatch = cardCandidate.includes(searchVal) ||
                      cardEmail.includes(searchVal) ||
                      cardJob.toLowerCase().includes(searchVal) ||
                      cardAppId.includes(searchVal);
      }

      // Job Check
      let jobMatch = true;
      if (jobVal) {
        jobMatch = cardJob === jobVal;
      }

      // Level Check
      let levelMatch = true;
      if (levelVal) {
        levelMatch = cardLevel === levelVal;
      }

      // Status Check
      let statusMatch = true;
      if (statusVal) {
        statusMatch = cardStatus === statusVal;
      }

      // Interviewer Check
      let interviewerMatch = true;
      if (interviewerVal) {
        const members = Array.from(card.querySelectorAll('.panel-member')).map(m => m.dataset.memberName);
        interviewerMatch = members.includes(interviewerVal);
      }

      if (dateMatch && searchMatch && jobMatch && levelMatch && statusMatch && interviewerMatch) {
        card.style.display = '';
        visibleCount++;
      } else {
        card.style.display = 'none';
      }
    });

    // Summary Text
    countText.textContent = `Hiển thị ${visibleCount} / ${cards.length} buổi phỏng vấn`;

    // Render Active Filter Pills
    renderActivePills();

    // Empty state handling
    if (visibleCount === 0) {
      emptyState.style.display = 'block';
      if (activePreset === 'today') {
        const parts = todayStr.split('-');
        emptyStateTitle.textContent = `Hôm nay (${parts[2]}/${parts[1]}/${parts[0]}) không có lịch phỏng vấn nào`;
        emptyStateDesc.textContent = 'Không có buổi phỏng vấn nào được lên lịch cho hôm nay. Bạn có thể chọn ngày có dấu chấm trên cuốn lịch hoặc bấm xem toàn bộ.';
        btnEmptyAction.textContent = 'Xem tất cả các cuộc họp';
        btnEmptyAction.onclick = () => {
          activePreset = 'all';
          selectedDate = null;
          updatePresetButtons();
          renderCalendar();
          applyFilters();
        };
      } else {
        emptyStateTitle.textContent = 'Không tìm thấy lịch phỏng vấn phù hợp';
        emptyStateDesc.textContent = 'Không có kết quả nào thỏa mãn bộ lọc hiện tại. Thử xóa bớt bộ lọc hoặc chọn ngày khác.';
        btnEmptyAction.textContent = 'Xóa tất cả bộ lọc';
        btnEmptyAction.onclick = clearAllFilters;
      }
    } else {
      emptyState.style.display = 'none';
    }
  }

  function renderActivePills() {
    activePillsContainer.innerHTML = '';
    let hasFilters = false;

    if (activePreset === 'today') {
      createPill('Hôm nay', () => {
        activePreset = 'all';
        selectedDate = null;
        updatePresetButtons();
        renderCalendar();
        applyFilters();
      });
      hasFilters = true;
    } else if (activePreset === 'custom' && selectedDate) {
      const parts = selectedDate.split('-');
      createPill(`Ngày ${parts[2]}/${parts[1]}/${parts[0]}`, () => {
        activePreset = 'all';
        selectedDate = null;
        updatePresetButtons();
        renderCalendar();
        applyFilters();
      });
      hasFilters = true;
    } else if (activePreset === 'this-week') {
      createPill('Tuần này', () => {
        activePreset = 'all';
        selectedDate = null;
        updatePresetButtons();
        applyFilters();
      });
      hasFilters = true;
    } else if (activePreset === 'this-month') {
      createPill('Tháng này', () => {
        activePreset = 'all';
        selectedDate = null;
        updatePresetButtons();
        applyFilters();
      });
      hasFilters = true;
    }

    if (searchInput.value.trim()) {
      createPill(`"${searchInput.value.trim()}"`, () => {
        searchInput.value = '';
        applyFilters();
      });
      hasFilters = true;
    }

    if (jobSelect.value) {
      createPill(`Vị trí: ${jobSelect.value}`, () => {
        jobSelect.value = '';
        applyFilters();
      });
      hasFilters = true;
    }

    if (levelSelect.value) {
      createPill(`Cấp bậc: ${levelSelect.value}`, () => {
        levelSelect.value = '';
        applyFilters();
      });
      hasFilters = true;
    }

    if (statusSelect.value) {
      const text = statusSelect.options[statusSelect.selectedIndex].text;
      createPill(`Trạng thái: ${text}`, () => {
        statusSelect.value = '';
        applyFilters();
      });
      hasFilters = true;
    }

    if (interviewerSelect.value) {
      createPill(`Người phỏng vấn: ${interviewerSelect.value}`, () => {
        interviewerSelect.value = '';
        applyFilters();
      });
      hasFilters = true;
    }

    btnClearAll.style.display = hasFilters ? 'inline-block' : 'none';
  }

  function createPill(text, onRemove) {
    const pill = document.createElement('span');
    pill.className = 'active-pill';
    pill.innerHTML = `<span>${text}</span><span class="pill-remove" title="Xóa lọc">✕</span>`;
    pill.querySelector('.pill-remove').addEventListener('click', onRemove);
    activePillsContainer.appendChild(pill);
  }

  function clearAllFilters() {
    activePreset = 'all';
    selectedDate = null;
    searchInput.value = '';
    jobSelect.value = '';
    levelSelect.value = '';
    statusSelect.value = '';
    interviewerSelect.value = '';
    updatePresetButtons();
    renderCalendar();
    applyFilters();
  }

  // Event Listeners
  btnFilterToday.addEventListener('click', () => {
    selectedDate = todayStr;
    activePreset = 'today';
    currentCalMonth = new Date().getMonth();
    currentCalYear = new Date().getFullYear();
    updatePresetButtons();
    renderCalendar();
    applyFilters();
  });

  btnFilterThisWeek.addEventListener('click', () => {
    selectedDate = null;
    activePreset = 'this-week';
    updatePresetButtons();
    renderCalendar();
    applyFilters();
  });

  btnFilterThisMonth.addEventListener('click', () => {
    selectedDate = null;
    activePreset = 'this-month';
    currentCalMonth = new Date().getMonth();
    currentCalYear = new Date().getFullYear();
    updatePresetButtons();
    renderCalendar();
    applyFilters();
  });

  btnFilterAll.addEventListener('click', () => {
    selectedDate = null;
    activePreset = 'all';
    updatePresetButtons();
    renderCalendar();
    applyFilters();
  });

  btnCalPrev.addEventListener('click', () => {
    currentCalMonth--;
    if (currentCalMonth < 0) {
      currentCalMonth = 11;
      currentCalYear--;
    }
    renderCalendar();
  });

  btnCalNext.addEventListener('click', () => {
    currentCalMonth++;
    if (currentCalMonth > 11) {
      currentCalMonth = 0;
      currentCalYear++;
    }
    renderCalendar();
  });

  searchInput.addEventListener('input', applyFilters);
  jobSelect.addEventListener('change', applyFilters);
  levelSelect.addEventListener('change', applyFilters);
  statusSelect.addEventListener('change', applyFilters);
  interviewerSelect.addEventListener('change', applyFilters);
  btnClearAll.addEventListener('click', clearAllFilters);

  // Click Job Title on card -> auto filter by job
  document.querySelectorAll('.job-department').forEach(dept => {
    dept.addEventListener('click', function (e) {
      e.stopPropagation();
      const jobName = this.dataset.jobName;
      if (jobName) {
        jobSelect.value = jobName;
        activePreset = 'all';
        selectedDate = null;
        updatePresetButtons();
        renderCalendar();
        applyFilters();
      }
    });
  });

  // Click Interviewer on card -> auto filter by interviewer
  document.querySelectorAll('.panel-member').forEach(member => {
    member.addEventListener('click', function (e) {
      e.stopPropagation();
      const name = this.dataset.memberName;
      if (name) {
        interviewerSelect.value = name;
        activePreset = 'all';
        selectedDate = null;
        updatePresetButtons();
        renderCalendar();
        applyFilters();
      }
    });
  });

  // Initial execution
  renderCalendar();
  applyFilters();

  // ── Filter Collapse Toggle (Bottom-Right Button) ─────────────────────
  (function () {
    const toggleBtn   = document.getElementById('btn-toggle-filter');
    const filterBody  = document.getElementById('filter-body');
    const toggleIcon  = document.getElementById('toggle-filter-icon');
    const toggleLabel = document.getElementById('toggle-filter-label');
    const dashboard   = document.getElementById('filter-dashboard');
    if (!toggleBtn || !filterBody) return;

    let collapsed = false;
    function setCollapsed(isCollapsed) {
      collapsed = isCollapsed;
      filterBody.classList.toggle('collapsed', collapsed);
      if (dashboard) dashboard.classList.toggle('is-collapsed', collapsed);
      toggleBtn.setAttribute('aria-expanded', String(!collapsed));
      if (collapsed) {
        toggleLabel.textContent = 'Mở rộng bộ lọc & lịch';
        toggleIcon.setAttribute('points', '6 9 12 15 18 9');
        toggleBtn.title = 'Mở rộng bộ lọc & lịch';
      } else {
        toggleLabel.textContent = 'Thu gọn bộ lọc';
        toggleIcon.setAttribute('points', '18 15 12 9 6 15');
        toggleBtn.title = 'Thu gọn bộ lọc';
      }
    }

    toggleBtn.addEventListener('click', function () {
      setCollapsed(!collapsed);
    });
  })();

  // ── User Menu Dropdown ────────────────────────────────────────────────
  (function () {
    const trigger  = document.getElementById('user-menu-trigger');
    const dropdown = document.getElementById('user-dropdown');
    if (!trigger || !dropdown) return;

    trigger.addEventListener('click', function (e) {
      e.stopPropagation();
      const isOpen = dropdown.classList.toggle('open');
      trigger.setAttribute('aria-expanded', String(isOpen));
    });

    document.addEventListener('click', function () {
      dropdown.classList.remove('open');
      trigger.setAttribute('aria-expanded', 'false');
    });

    dropdown.addEventListener('click', function (e) { e.stopPropagation(); });
  })();

});
