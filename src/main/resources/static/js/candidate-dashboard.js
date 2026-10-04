(() => {
  'use strict';
  const normalize = value => value.normalize('NFD').replace(/[\u0300-\u036f]/g, '')
    .replace(/[đĐ]/g, 'd').toLowerCase().trim();

  document.querySelectorAll('[data-candidate-filter]').forEach(section => {
    const controls = section.querySelector('[data-filter-controls]');
    if (!controls) return;
    const keyword = controls.querySelector('[data-filter-keyword]');
    const status = controls.querySelector('[data-filter-status]');
    const clear = controls.querySelector('[data-filter-clear]');
    const rows = [...section.querySelectorAll('[data-filter-row]')];
    const count = section.querySelector('[data-result-count]');
    const empty = section.querySelector('[data-filter-empty]');
    const table = section.querySelector('.table-wrapper');
    const unit = section.dataset.candidateFilter === 'offers' ? 'thư mời' : 'hồ sơ';
    const active = () => keyword.value !== '' || status.value !== '';
    const update = () => {
      const term = normalize(keyword.value);
      let visible = 0;
      rows.forEach(row => {
        row.hidden = !(normalize(row.dataset.title).includes(term)
          && (!status.value || row.dataset.status === status.value));
        if (!row.hidden) visible += 1;
      });
      count.textContent = `${visible} / ${rows.length} ${unit} hiển thị`;
      empty.hidden = visible > 0;
      if (table) table.hidden = visible === 0;
      // Keep keyboard focus on Clear until the user leaves it; never jump to another section.
      clear.hidden = !active() && document.activeElement !== clear;
    };
    keyword.addEventListener('input', update);
    status.addEventListener('change', update);
    clear.addEventListener('click', () => { keyword.value = ''; status.value = ''; update(); });
    clear.addEventListener('blur', () => { clear.hidden = !active(); });
    controls.hidden = false;
    update();
  });

  const form = document.querySelector('form[data-confirm-logout]');
  const dialog = document.getElementById('candidate-logout-dialog');
  if (form && dialog && typeof dialog.showModal === 'function') {
    let confirmed = false;
    form.addEventListener('submit', event => {
      if (confirmed) return;
      event.preventDefault();
      dialog.showModal();
    });
    dialog.querySelector('[data-logout-cancel]').addEventListener('click', () => dialog.close());
    dialog.querySelector('[data-logout-confirm]').addEventListener('click', event => {
      confirmed = true;
      event.currentTarget.disabled = true;
      form.requestSubmit();
    });
    // Native dialog restores focus and handles Escape; the existing POST form keeps its CSRF token.
  }
})();
