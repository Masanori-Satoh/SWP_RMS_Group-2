document.addEventListener('DOMContentLoaded', () => {
    const positionMenu = (button, menu) => {
        const rect = button.getBoundingClientRect();
        if (rect.bottom < 0 || rect.top > window.innerHeight || rect.right < 0 || rect.left > window.innerWidth) {
            menu.hidePopover();
            return;
        }
        menu.style.left = Math.max(8, Math.min(rect.right - menu.offsetWidth, window.innerWidth - menu.offsetWidth - 8)) + 'px';
        menu.style.top = (rect.bottom + menu.offsetHeight + 8 > window.innerHeight ? Math.max(8, rect.top - menu.offsetHeight - 6) : rect.bottom + 6) + 'px';
    };
    document.querySelectorAll('[data-action-menu]').forEach(button => {
        const menu = document.getElementById(button.getAttribute('popovertarget'));
        menu.addEventListener('toggle', event => {
            if (event.newState !== 'open') return;
            positionMenu(button, menu);
        });
    });
    const closeMenus = () => document.querySelectorAll('.action-popover:popover-open').forEach(menu => menu.hidePopover());
    // Scrolling a table to reach its action button must not immediately dismiss the menu.
    const repositionMenus = () => document.querySelectorAll('[data-action-menu]').forEach(button => {
        const menu = document.getElementById(button.getAttribute('popovertarget'));
        if (menu.matches(':popover-open')) positionMenu(button, menu);
    });
    window.addEventListener('resize', repositionMenus);
    document.addEventListener('scroll', repositionMenus, true);
    const dialog = document.getElementById('delete-dialog');
    let deleteTrigger;
    document.querySelectorAll('[data-delete-requisition]').forEach(button => button.addEventListener('click', () => {
        const menu = button.closest('.action-popover');
        deleteTrigger = menu ? document.querySelector(`[popovertarget="${menu.id}"]`) : button;
        closeMenus();
        document.getElementById('delete-name').textContent = button.dataset.title;
        document.getElementById('delete-form').action = button.dataset.deleteUrl;
        dialog.showModal(); dialog.querySelector('[data-close-dialog]').focus();
    }));
    dialog?.querySelector('[data-close-dialog]').addEventListener('click', () => dialog.close());
    dialog?.addEventListener('close', () => deleteTrigger?.focus());
    dialog?.addEventListener('click', event => { if (event.target === dialog) dialog.close(); });

    const form = document.getElementById('requisition-form');
    if (form) {
        form.querySelectorAll('.textarea-custom').forEach(input => {
            const counter = input.parentElement.querySelector('.char-counter');
            const update = () => { counter.textContent = input.value.length + '/' + input.maxLength; };
            input.addEventListener('input', update); update();
        });
        const rows = document.getElementById('criteria-rows');
        const error = document.getElementById('criteria-error');
        const syncRows = () => rows.querySelectorAll('tr').forEach((row, index) => {
            row.querySelector('.row-index').textContent = index + 1;
            row.querySelectorAll('[name]').forEach(input => {
                input.name = input.name.replace(/screeningCriteria\[(?:\d+|__INDEX__)\]/, 'screeningCriteria[' + index + ']');
                if (input.type !== 'hidden') input.setAttribute('aria-label', ({criteriaName:'Criterion name',criteriaType:'Criterion type',requiredValue:'Required value',weight:'Weight percent',isMandatory:'Mandatory'})[input.name.split('.').pop()] || 'Criterion');
            });
            const checkbox = row.querySelector('input[type="checkbox"]');
            row.querySelector('.switch-text').textContent = checkbox.checked ? 'Yes' : 'No';
        });
        document.getElementById('btn-add-criteria').addEventListener('click', () => {
            if (rows.children.length >= 50) { error.hidden = false; error.textContent = 'Use at most 50 criteria.'; return; }
            const row = document.getElementById('criteria-template').content.firstElementChild.cloneNode(true);
            rows.appendChild(row); syncRows(); row.querySelector('input:not([type="hidden"])').focus();
        });
        rows.addEventListener('click', event => {
            const button = event.target.closest('[data-remove-criterion]');
            if (button) { button.closest('tr').remove(); syncRows(); }
        });
        rows.addEventListener('change', syncRows); syncRows();
        form.addEventListener('submit', event => {
            error.hidden = true;
            if (event.submitter?.value === 'draft') return;
            const names = [...rows.querySelectorAll('[name$=".criteriaName"]')].map(input => input.value.trim().toLowerCase());
            const total = [...rows.querySelectorAll('[name$=".weight"]')].reduce((sum,input) => sum + Math.round((Number(input.value) || 0) * 100),0);
            let message = '';
            if (!names.length || names.some(name => !name)) message = 'Add at least one complete screening criterion.';
            else if (new Set(names).size !== names.length) message = 'Criterion names must be unique.';
            else if (total !== 10000) message = 'Total screening weight must equal 100%.';
            if (message) { event.preventDefault(); error.textContent = message; error.hidden = false; error.scrollIntoView({block:'center'}); }
        });
        document.getElementById('validation-summary')?.focus();
    }
    const decision = document.getElementById('decision-form');
    const feedback = document.getElementById('decision-comment');
    decision?.querySelectorAll('button[name="decision"]').forEach(button => button.addEventListener('click', () => feedback.setCustomValidity('')));
    feedback?.addEventListener('input', () => feedback.setCustomValidity(''));
    decision?.addEventListener('submit', event => {
        if (event.submitter?.value === 'reject' && !feedback.value.trim()) {
            event.preventDefault(); feedback.setCustomValidity('Explain what the Hiring Manager must change.'); feedback.reportValidity();
        }
    });
    // Preserve the submitter name/value while preventing duplicate requests.
    document.querySelectorAll('#requisition-form,#delete-form,#decision-form,.withdraw-form').forEach(target => target.addEventListener('submit', event => {
        if (event.defaultPrevented) return;
        if (target.dataset.submitting === 'true') { event.preventDefault(); return; }
        target.dataset.submitting = 'true'; target.setAttribute('aria-busy','true');
    }));
    window.addEventListener('pageshow', () => document.querySelectorAll('[data-submitting]').forEach(target => { delete target.dataset.submitting; target.removeAttribute('aria-busy'); }));
});
