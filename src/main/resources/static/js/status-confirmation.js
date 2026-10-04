document.addEventListener('DOMContentLoaded', () => {
    const dialog = document.getElementById('status-dialog');
    if (!dialog) return;
    const title = document.getElementById('status-title');
    const description = document.getElementById('status-description');
    const cancel = document.getElementById('status-cancel');
    const confirm = document.getElementById('status-confirm');
    const confirmed = new WeakSet();
    let pendingForm;
    let trigger;

    document.querySelectorAll('form[data-status-confirm]').forEach((form) => {
        form.addEventListener('submit', (event) => {
            if (confirmed.has(form)) { confirmed.delete(form); return; }
            event.preventDefault();
            pendingForm = form;
            trigger = event.submitter || form.querySelector('button[type="submit"]');
            title.textContent = form.dataset.confirmTitle;
            description.textContent = form.dataset.confirmMessage;
            confirm.textContent = form.dataset.confirmLabel;
            confirm.className = form.dataset.confirmDanger === 'true' ? 'btn btn-danger' : 'btn btn-primary';
            confirm.disabled = false;
            dialog.showModal();
            cancel.focus();
        });
    });
    cancel.addEventListener('click', () => dialog.close());
    dialog.addEventListener('close', () => { trigger?.focus(); pendingForm = null; });
    confirm.addEventListener('click', () => {
        if (!pendingForm || confirm.disabled) return;
        confirm.disabled = true;
        const form = pendingForm;
        confirmed.add(form);
        form.requestSubmit(trigger);
        dialog.close();
    });
});
