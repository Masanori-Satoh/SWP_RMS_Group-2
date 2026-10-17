(() => {
    'use strict';
    const forms = document.querySelectorAll('[data-pool-decision], [data-pool-note]');
    forms.forEach(form => {
        const buttons = [...form.querySelectorAll('button[type="submit"]')];
        const labels = buttons.map(button => button.textContent);
        let submitting = false;
        form.addEventListener('submit', event => {
            if (submitting) { event.preventDefault(); return; }
            if (form.hasAttribute('data-pool-decision')) {
                if (!['Pass', 'Fail'].includes(event.submitter?.value)) { event.preventDefault(); return; }
                const input = document.createElement('input');
                input.type = 'hidden'; input.name = 'decision'; input.value = event.submitter.value;
                input.dataset.poolDecisionValue = 'true'; form.append(input);
            }
            submitting = true;
            form.setAttribute('aria-busy', 'true');
            buttons.forEach(button => { button.disabled = true; });
            if (event.submitter) event.submitter.textContent = 'Đang lưu…';
        });
        window.addEventListener('pageshow', () => {
            submitting = false; form.removeAttribute('aria-busy');
            form.querySelector('[data-pool-decision-value]')?.remove();
            buttons.forEach((button, index) => { button.disabled = false; button.textContent = labels[index]; });
        });
    });
})();
