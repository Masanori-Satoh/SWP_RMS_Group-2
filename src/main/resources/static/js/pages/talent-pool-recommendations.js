(() => {
    'use strict';
    const dialog = document.getElementById('tp-withdraw-dialog');
    if (dialog) {
        let source;
        const confirm = dialog.querySelector('[data-pool-withdraw-confirm]');
        document.querySelectorAll('[data-pool-withdraw-dialog]').forEach(button => {
            button.addEventListener('click', () => {
                source = button;
                dialog.querySelector('#tp-withdraw-description').textContent = `Thu hồi gợi ý ${button.dataset.name} trước khi HM phản hồi. CV và ghi chú của ứng viên vẫn được giữ nguyên.`;
                confirm.disabled = false;
                dialog.showModal();
            });
        });
        dialog.querySelector('[data-pool-withdraw-cancel]').addEventListener('click', () => dialog.close());
        dialog.addEventListener('close', () => { source?.focus(); source = null; });
        confirm.addEventListener('click', () => {
            if (!source || confirm.disabled) return;
            confirm.disabled = true;
            source.closest('form').requestSubmit();
            dialog.close();
        });
    }
    const forms = document.querySelectorAll('[data-pool-decision], [data-pool-note], [data-pool-withdraw]');
    forms.forEach(form => {
        const buttons = [...form.querySelectorAll('button[type="submit"]')];
        // Buttons with an icon keep their text in [data-label] so the icon survives relabelling.
        const label = button => button.querySelector?.('[data-label]') || button;
        const labels = buttons.map(button => label(button).textContent);
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
            if (event.submitter) label(event.submitter).textContent = 'Đang lưu…';
        });
        window.addEventListener('pageshow', () => {
            submitting = false; form.removeAttribute('aria-busy');
            form.querySelector('[data-pool-decision-value]')?.remove();
            buttons.forEach((button, index) => { button.disabled = false; label(button).textContent = labels[index]; });
        });
    });
})();
