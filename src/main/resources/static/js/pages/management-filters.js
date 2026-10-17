/* Only the three management GET toolbars. Native details/form/links also work without JS. */
(() => {
    'use strict';
    const form = document.querySelector('.mft-toolbar');
    const disclosure = form?.querySelector('.mft-disclosure');
    if (!disclosure) return;
    const toggle = disclosure.querySelector('summary');
    // Closing never resets draft values; the existing native GET form owns submission.
    document.addEventListener('click', event => {
        if (!disclosure.contains(event.target)) disclosure.open = false;
    });
    disclosure.addEventListener('keydown', event => {
        if (event.key !== 'Escape' || !disclosure.open) return;
        event.preventDefault();
        disclosure.open = false;
        toggle.focus();
    });
    form.querySelectorAll('select').forEach(control => {
        const updateTitle = () => { control.title = control.selectedOptions[0]?.textContent || ''; };
        control.addEventListener('change', updateTitle);
        updateTitle();
    });
})();
