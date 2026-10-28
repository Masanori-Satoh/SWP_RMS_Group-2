/* Talent Pool presentation only: keep long selected-CV labels readable.
   The existing talent-pool.js remains responsible for selection and submission. */
(() => {
    'use strict';
    const page = document.querySelector('.tpw-page');
    if (!page) return;
    page.querySelectorAll('[data-pool-cv]').forEach(select => {
        const picker = select.closest('.tpw-source-picker');
        if (!picker) return;
        const caption = picker.querySelector('[data-tpw-source-caption]');
        const date = picker.querySelector('[data-tpw-source-date]');
        const summary = picker.querySelector('summary');
        const sync = () => {
            const option = select.selectedOptions?.[0];
            if (!option) return;
            const title = option.dataset.jobTitle || option.textContent.trim();
            const submitted = option.dataset.submittedDate || 'Chưa có ngày nộp';
            if (caption) caption.textContent = title;
            if (date) date.textContent = submitted;
            if (summary) summary.title = `${title} · ${submitted}`;
        };
        select.addEventListener('change', sync);
        sync();
    });
})();
