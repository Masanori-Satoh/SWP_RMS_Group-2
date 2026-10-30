/* Talent Pool presentation only: expose the full selected filter label on hover, because long job titles are
   ellipsised inside the compact filter pills. Filtering itself stays a native GET form. */
(() => {
    'use strict';
    const page = document.querySelector('.tpw-page');
    if (!page) return;
    page.querySelectorAll('.tpw-toolbar select').forEach(select => {
        const sync = () => { select.title = select.selectedOptions?.[0]?.textContent.trim() || ''; };
        select.addEventListener('change', sync);
        sync();
    });
})();
