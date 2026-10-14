/* Internal Accounts only: enhance existing controls without changing their URLs or submission. */
(() => {
    'use strict';
    const page = document.querySelector('.iac-page');
    if (!page) return;

    // Native selects retain the complete database label even when space is limited.
    page.querySelectorAll('.iac-filters select').forEach(select => {
        const updateTitle = () => { select.title = select.selectedOptions[0]?.textContent || ''; };
        updateTitle();
        select.addEventListener('change', updateTitle);
    });

    // Keep the shared paginator and its filter-preserving anchors intact.
    // Without this script, the server-rendered ‹ / › controls still work.
    page.querySelectorAll('.iac-pagination .page-link').forEach(control => {
        const glyph = control.textContent.trim();
        if (glyph !== '‹' && glyph !== '›') return;
        const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
        svg.setAttribute('viewBox', '0 0 24 24');
        svg.setAttribute('aria-hidden', 'true');
        svg.setAttribute('focusable', 'false');
        const path = document.createElementNS(svg.namespaceURI, 'path');
        path.setAttribute('d', glyph === '‹' ? 'm15 6-6 6 6 6' : 'm9 6 6 6-6 6');
        path.setAttribute('fill', 'none');
        path.setAttribute('stroke', 'currentColor');
        path.setAttribute('stroke-width', '1.8');
        path.setAttribute('stroke-linecap', 'round');
        path.setAttribute('stroke-linejoin', 'round');
        svg.append(path);
        control.replaceChildren(svg);
        control.classList.add('iac-page-arrow');
        if (control.classList.contains('disabled')) control.classList.add('iac-page-arrow--disabled');
    });
})();
