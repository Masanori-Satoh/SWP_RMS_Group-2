/* =============================================================================
   INTERVIEW-LIST.JS — Lịch phỏng vấn (/interviews, interview/list.html)
   - Lọc phía client: lịch mini, khoảng thời gian, tìm kiếm, vị trí, cấp bậc, trạng thái, người phỏng vấn.
   - Bấm tên vị trí / người phỏng vấn trên thẻ để lọc nhanh.
   - Nút "Hủy lịch": gán action cho #il-cancel-form rồi mở hộp thoại xác nhận #il-cancel-dialog
     (class catalog confirm-dialog; nút "Hủy lịch" trong dialog submit form qua thuộc tính form=).
   Ẩn/hiện bằng thuộc tính hidden (không dùng style nội tuyến).
   ============================================================================= */
(function () {
  'use strict';

  // ---------------------------------------------------------------- Hủy lịch
  const cancelForm = document.getElementById('il-cancel-form');
  const cancelDialog = document.getElementById('il-cancel-dialog');
  if (cancelForm && cancelDialog) {
    document.querySelectorAll('[data-cancel-url]').forEach(function (button) {
      button.addEventListener('click', function () {
        cancelForm.action = button.getAttribute('data-cancel-url');
        cancelDialog.showModal();
      });
    });
    const dismiss = cancelDialog.querySelector('[data-cancel-dismiss]');
    if (dismiss) {
      dismiss.addEventListener('click', function () {
        cancelDialog.close();
      });
    }
  }

  // ---------------------------------------------------------------- Bộ lọc
  const cards = Array.from(document.querySelectorAll('.il-grid .il-card'));
  if (cards.length === 0) {
    return;
  }

  const calTitle = document.getElementById('cal-month-year');
  const calDaysGrid = document.getElementById('cal-days');
  const presets = {
    today: document.getElementById('btn-filter-today'),
    'this-week': document.getElementById('btn-filter-this-week'),
    'this-month': document.getElementById('btn-filter-this-month'),
    all: document.getElementById('btn-filter-all')
  };
  const searchInput = document.getElementById('filter-search');
  const jobSelect = document.getElementById('filter-job');
  const levelSelect = document.getElementById('filter-level');
  const statusSelect = document.getElementById('filter-status');
  const interviewerSelect = document.getElementById('filter-interviewer');
  const countText = document.getElementById('filter-count-text');
  const pillsContainer = document.getElementById('active-pills');
  const btnClearAll = document.getElementById('btn-clear-all');
  const emptyState = document.getElementById('filter-empty-state');
  const emptyTitle = document.getElementById('empty-state-title');
  const emptyDesc = document.getElementById('empty-state-desc');
  const btnEmptyAction = document.getElementById('btn-empty-action');

  // 1. Gom ngày có lịch, vị trí, người phỏng vấn để dựng lịch mini và dropdown
  const meetingDateCounts = {};
  const jobTitles = new Set();
  const interviewers = new Set();
  cards.forEach(function (card) {
    const date = card.dataset.date;
    if (date) {
      meetingDateCounts[date] = (meetingDateCounts[date] || 0) + 1;
    }
    if (card.dataset.job) {
      jobTitles.add(card.dataset.job.trim());
    }
    card.querySelectorAll('.il-member').forEach(function (member) {
      if (member.dataset.memberName) {
        interviewers.add(member.dataset.memberName.trim());
      }
    });
  });
  fillOptions(jobSelect, jobTitles);
  fillOptions(interviewerSelect, interviewers);

  function fillOptions(select, values) {
    if (!select) {
      return;
    }
    Array.from(values).sort().forEach(function (value) {
      const option = document.createElement('option');
      option.value = value;
      option.textContent = value;
      select.appendChild(option);
    });
  }

  function toDateKey(date) {
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return date.getFullYear() + '-' + month + '-' + day;
  }

  function formatDateKey(key) {
    const parts = key.split('-');
    return parts[2] + '/' + parts[1] + '/' + parts[0];
  }

  const todayKey = toDateKey(new Date());
  let selectedDate = todayKey;
  let activePreset = 'today'; // today | this-week | this-month | all | custom
  let calMonth = new Date().getMonth();
  let calYear = new Date().getFullYear();

  function jobLevel(title) {
    const t = (title || '').toLowerCase();
    if (t.includes('senior') || t.includes('sr.') || t.includes('trưởng')) return 'Senior';
    if (t.includes('lead') || t.includes('quản lý') || t.includes('manager')) return 'Lead';
    if (t.includes('junior') || t.includes('jr.') || t.includes('fresher')) return 'Junior';
    if (t.includes('intern') || t.includes('thực tập')) return 'Intern';
    if (t.includes('middle') || t.includes('mid')) return 'Middle';
    return 'Other';
  }

  function weekRange(date) {
    const monday = new Date(date);
    monday.setDate(date.getDate() - ((date.getDay() + 6) % 7));
    const sunday = new Date(monday);
    sunday.setDate(monday.getDate() + 6);
    return { start: toDateKey(monday), end: toDateKey(sunday) };
  }

  // 2. Lịch mini
  function renderCalendar() {
    calTitle.textContent = 'Tháng ' + (calMonth + 1) + ', ' + calYear;
    calDaysGrid.replaceChildren();

    const startOffset = (new Date(calYear, calMonth, 1).getDay() + 6) % 7;
    const daysInMonth = new Date(calYear, calMonth + 1, 0).getDate();

    for (let i = 0; i < startOffset; i++) {
      const empty = document.createElement('div');
      empty.className = 'il-day il-day--empty';
      calDaysGrid.appendChild(empty);
    }

    for (let day = 1; day <= daysInMonth; day++) {
      const key = toDateKey(new Date(calYear, calMonth, day));
      const count = meetingDateCounts[key] || 0;
      const button = document.createElement('button');
      button.type = 'button';
      button.className = 'il-day';
      button.classList.toggle('is-today', key === todayKey);
      button.classList.toggle('is-active', key === selectedDate);
      button.title = formatDateKey(key) + ': ' + (count ? count + ' buổi phỏng vấn' : 'không có lịch');

      const label = document.createElement('span');
      label.textContent = String(day);
      button.appendChild(label);
      if (count) {
        const dot = document.createElement('span');
        dot.className = 'il-calendar__dot';
        button.appendChild(dot);
      }

      button.addEventListener('click', function () {
        selectedDate = key;
        setPreset('custom');
      });
      calDaysGrid.appendChild(button);
    }
  }

  function setPreset(preset) {
    activePreset = preset;
    if (preset === 'today') {
      selectedDate = todayKey;
      calMonth = new Date().getMonth();
      calYear = new Date().getFullYear();
    } else if (preset !== 'custom') {
      selectedDate = null;
    }
    Object.entries(presets).forEach(function (entry) {
      entry[1].classList.toggle('is-active', entry[0] === activePreset);
    });
    renderCalendar();
    applyFilters();
  }

  // 3. Áp dụng bộ lọc
  function applyFilters() {
    const search = searchInput.value.trim().toLowerCase();
    const week = weekRange(new Date());
    const monthPrefix = todayKey.substring(0, 7);
    let visible = 0;

    cards.forEach(function (card) {
      const date = card.dataset.date || '';
      const job = card.dataset.job || '';
      let match;

      if (activePreset === 'today' || activePreset === 'custom') {
        match = date === selectedDate;
      } else if (activePreset === 'this-week') {
        match = date >= week.start && date <= week.end;
      } else if (activePreset === 'this-month') {
        match = date.startsWith(monthPrefix);
      } else {
        match = true;
      }

      if (match && search) {
        match = [card.dataset.candidate, card.dataset.candidateEmail, job, card.dataset.appId]
          .some(function (value) {
            return (value || '').toLowerCase().includes(search);
          });
      }
      if (match && jobSelect.value) {
        match = job === jobSelect.value;
      }
      if (match && levelSelect.value) {
        match = jobLevel(job) === levelSelect.value;
      }
      if (match && statusSelect.value) {
        match = card.dataset.status === statusSelect.value;
      }
      if (match && interviewerSelect && interviewerSelect.value) {
        match = Array.from(card.querySelectorAll('.il-member'))
          .some(function (member) {
            return member.dataset.memberName === interviewerSelect.value;
          });
      }

      card.hidden = !match;
      if (match) {
        visible++;
      }
    });

    countText.textContent = 'Hiển thị ' + visible + ' / ' + cards.length + ' buổi phỏng vấn';
    renderPills();
    renderEmptyState(visible);
  }

  function renderEmptyState(visible) {
    emptyState.hidden = visible > 0;
    if (visible > 0) {
      return;
    }
    if (activePreset === 'today') {
      emptyTitle.textContent = 'Hôm nay (' + formatDateKey(todayKey) + ') không có lịch phỏng vấn nào';
      emptyDesc.textContent = 'Chọn ngày có dấu chấm trên lịch hoặc xem toàn bộ.';
      btnEmptyAction.textContent = 'Xem tất cả';
      btnEmptyAction.onclick = function () {
        setPreset('all');
      };
    } else {
      emptyTitle.textContent = 'Không tìm thấy lịch phỏng vấn phù hợp';
      emptyDesc.textContent = 'Thử bỏ bớt bộ lọc hoặc chọn ngày khác.';
      btnEmptyAction.textContent = 'Xóa tất cả bộ lọc';
      btnEmptyAction.onclick = clearAllFilters;
    }
  }

  // 4. Chip bộ lọc đang áp dụng (chữ người dùng gõ chỉ gán qua textContent)
  function renderPills() {
    pillsContainer.replaceChildren();
    const resetDate = function () {
      setPreset('all');
    };

    if (activePreset === 'today') {
      addPill('Hôm nay', resetDate);
    } else if (activePreset === 'custom' && selectedDate) {
      addPill('Ngày ' + formatDateKey(selectedDate), resetDate);
    } else if (activePreset === 'this-week') {
      addPill('Tuần này', resetDate);
    } else if (activePreset === 'this-month') {
      addPill('Tháng này', resetDate);
    }
    if (searchInput.value.trim()) {
      addPill('“' + searchInput.value.trim() + '”', function () {
        searchInput.value = '';
        applyFilters();
      });
    }
    addSelectPill(jobSelect, 'Vị trí');
    addSelectPill(levelSelect, 'Cấp bậc');
    addSelectPill(statusSelect, 'Trạng thái');
    addSelectPill(interviewerSelect, 'Người phỏng vấn');

    btnClearAll.hidden = pillsContainer.childElementCount === 0;
  }

  function addSelectPill(select, label) {
    if (select && select.value) {
      addPill(label + ': ' + select.options[select.selectedIndex].text, function () {
        select.value = '';
        applyFilters();
      });
    }
  }

  function addPill(text, onRemove) {
    const pill = document.createElement('span');
    pill.className = 'il-pill';
    const label = document.createElement('span');
    label.textContent = text;
    const remove = document.createElement('button');
    remove.type = 'button';
    remove.className = 'il-pill__remove';
    remove.textContent = '✕';
    remove.setAttribute('aria-label', 'Bỏ lọc ' + text);
    remove.addEventListener('click', onRemove);
    pill.append(label, remove);
    pillsContainer.appendChild(pill);
  }

  function clearAllFilters() {
    searchInput.value = '';
    [jobSelect, levelSelect, statusSelect, interviewerSelect].forEach(function (select) {
      if (select) {
        select.value = '';
      }
    });
    setPreset('all');
  }

  // 5. Sự kiện
  presets.today.addEventListener('click', function () { setPreset('today'); });
  presets['this-week'].addEventListener('click', function () { setPreset('this-week'); });
  presets['this-month'].addEventListener('click', function () {
    calMonth = new Date().getMonth();
    calYear = new Date().getFullYear();
    setPreset('this-month');
  });
  presets.all.addEventListener('click', function () { setPreset('all'); });

  document.getElementById('cal-prev').addEventListener('click', function () {
    calMonth--;
    if (calMonth < 0) {
      calMonth = 11;
      calYear--;
    }
    renderCalendar();
  });
  document.getElementById('cal-next').addEventListener('click', function () {
    calMonth++;
    if (calMonth > 11) {
      calMonth = 0;
      calYear++;
    }
    renderCalendar();
  });

  searchInput.addEventListener('input', applyFilters);
  [jobSelect, levelSelect, statusSelect, interviewerSelect].forEach(function (select) {
    if (select) {
      select.addEventListener('change', applyFilters);
    }
  });
  btnClearAll.addEventListener('click', clearAllFilters);

  document.querySelectorAll('.il-card__job').forEach(function (button) {
    button.addEventListener('click', function () {
      if (button.dataset.jobName) {
        jobSelect.value = button.dataset.jobName;
        setPreset('all');
      }
    });
  });
  document.querySelectorAll('.il-member').forEach(function (button) {
    button.addEventListener('click', function () {
      if (interviewerSelect && button.dataset.memberName) {
        interviewerSelect.value = button.dataset.memberName;
        setPreset('all');
      }
    });
  });

  // 6. Thu gọn / mở rộng khung bộ lọc
  const toggle = document.getElementById('btn-toggle-filter');
  const filterBody = document.getElementById('filter-body');
  const filterBox = document.getElementById('filter-dashboard');
  if (toggle && filterBody) {
    toggle.addEventListener('click', function () {
      const collapse = !filterBody.hidden;
      filterBody.hidden = collapse;
      filterBox.classList.toggle('is-collapsed', collapse);
      toggle.setAttribute('aria-expanded', String(!collapse));
      toggle.textContent = collapse ? 'Mở rộng bộ lọc & lịch' : 'Thu gọn bộ lọc';
    });
  }

  renderCalendar();
  applyFilters();
})();
