/* API Monitoring: enhance the existing native POST form; never start a probe on page load. */
(() => {
    'use strict';
    document.querySelectorAll('.apm-page .apm-probe-form').forEach(form => {
        const button = form.querySelector('button[type="submit"]');
        if (!button) return;
        const label = button.textContent.trim();
        let pending = false;

        form.addEventListener('submit', event => {
            if (pending) { event.preventDefault(); return; }
            pending = true;
            form.setAttribute('aria-busy', 'true');
            button.disabled = true;
            button.textContent = 'Đang kiểm tra…';
        });

        // Restore the native control when navigating back from a response or browser error page.
        window.addEventListener('pageshow', () => {
            pending = false;
            form.removeAttribute('aria-busy');
            button.disabled = false;
            button.textContent = label;
        });
    });
})();
