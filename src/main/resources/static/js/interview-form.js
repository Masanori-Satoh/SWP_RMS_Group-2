/* =============================================================================
   INTERVIEW-FORM.JS — Tạo / sửa lịch phỏng vấn (interview/form.html)
   1. Tìm hồ sơ ứng tuyển trong dropdown #applicationId (Enter chọn kết quả đầu tiên).
   2. Lọc người phỏng vấn trong #interviewerIds theo từ khóa và vai trò (.if-role-pill).
   3. Hiện số người đã chọn và chip bỏ chọn nhanh.
   Ẩn/hiện bằng thuộc tính hidden (không dùng style nội tuyến). Trước đây là <script> nội tuyến trong form.html.
   ============================================================================= */
(function () {
  'use strict';

  function filterOptions(select, query, extraMatch) {
    let count = 0;
    Array.from(select.options).forEach(function (option) {
      if (option.value === '') {
        option.hidden = query !== '';
        return;
      }
      const searchData = option.getAttribute('data-search') || option.text.toLowerCase();
      const match = (query === '' || searchData.includes(query)) && (!extraMatch || extraMatch(option));
      option.hidden = !match;
      if (match) {
        count++;
      }
    });
    return count;
  }

  // 1. Hồ sơ ứng tuyển
  const appSelect = document.getElementById('applicationId');
  const appSearch = document.getElementById('applicationSearchBox');
  const appClear = document.getElementById('btnClearAppSearch');
  const appSummary = document.getElementById('appSearchSummary');

  if (appSelect && appSearch) {
    const filterApplications = function () {
      const query = appSearch.value.trim().toLowerCase();
      const count = filterOptions(appSelect, query);
      if (appSummary) {
        appSummary.hidden = query === '';
        appSummary.textContent = query === ''
          ? ''
          : 'Tìm thấy ' + count + ' hồ sơ phù hợp với “' + query + '”.';
      }
    };

    appSearch.addEventListener('input', filterApplications);
    appSearch.addEventListener('keydown', function (event) {
      if (event.key !== 'Enter') {
        return;
      }
      event.preventDefault();
      const first = Array.from(appSelect.options).find(function (option) {
        return option.value !== '' && !option.hidden;
      });
      if (first) {
        appSelect.value = first.value;
      }
    });
    if (appClear) {
      appClear.addEventListener('click', function () {
        appSearch.value = '';
        filterApplications();
        appSearch.focus();
      });
    }
  }

  // 2. Hội đồng phỏng vấn
  const panelSelect = document.getElementById('interviewerIds');
  if (!panelSelect) {
    return;
  }
  const panelSearch = document.getElementById('interviewerSearchBox');
  const panelClear = document.getElementById('btnClearSearch');
  const rolePills = Array.from(document.querySelectorAll('.if-role-pill'));
  const countDisplay = document.getElementById('selectedCount');
  const chips = document.getElementById('selectedChipsContainer');
  let roleFilter = 'ALL';

  const filterInterviewers = function () {
    const query = panelSearch ? panelSearch.value.trim().toLowerCase() : '';
    filterOptions(panelSelect, query, function (option) {
      return roleFilter === 'ALL'
        || (option.getAttribute('data-role') || '').toLowerCase() === roleFilter.toLowerCase();
    });
  };

  const renderSelected = function () {
    if (!countDisplay || !chips) {
      return;
    }
    chips.replaceChildren();
    const selected = Array.from(panelSelect.options).filter(function (option) {
      return option.selected;
    });
    selected.forEach(function (option) {
      const name = option.text.split('—')[0].trim();
      const chip = document.createElement('span');
      chip.className = 'if-chip';
      chip.textContent = name;

      const remove = document.createElement('button');
      remove.type = 'button';
      remove.className = 'if-chip__remove';
      remove.textContent = '×';
      remove.setAttribute('aria-label', 'Bỏ chọn ' + name);
      remove.addEventListener('click', function () {
        option.selected = false;
        renderSelected();
      });
      chip.appendChild(remove);
      chips.appendChild(chip);
    });
    countDisplay.textContent = String(selected.length);
  };

  if (panelSearch) {
    panelSearch.addEventListener('input', filterInterviewers);
  }
  if (panelClear) {
    panelClear.addEventListener('click', function () {
      if (panelSearch) {
        panelSearch.value = '';
      }
      filterInterviewers();
    });
  }
  rolePills.forEach(function (pill) {
    pill.addEventListener('click', function () {
      rolePills.forEach(function (other) {
        const active = other === pill;
        other.classList.toggle('is-active', active);
        other.setAttribute('aria-pressed', String(active));
      });
      roleFilter = pill.getAttribute('data-role');
      filterInterviewers();
    });
  });
  panelSelect.addEventListener('change', renderSelected);

  renderSelected();
})();
