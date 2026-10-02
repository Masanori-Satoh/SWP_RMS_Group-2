document.addEventListener('DOMContentLoaded', () => {
    const dialog = document.getElementById('deactivate-dialog');
    const form = document.getElementById('deactivate-form');
    const name = document.getElementById('deactivate-account-name');
    const username = document.getElementById('deactivate-account-username');
    const cancel = document.getElementById('deactivate-cancel');
    let trigger;

    document.querySelectorAll('[data-deactivate-account]').forEach((button) => {
        button.addEventListener('click', () => {
            trigger = button;
            name.textContent = button.dataset.accountName;
            username.textContent = button.dataset.accountUsername;
            form.action = button.dataset.deactivateUrl;
            dialog.showModal();
            cancel.focus();
        });
    });

    cancel.addEventListener('click', () => dialog.close());
    dialog.addEventListener('close', () => trigger?.focus());

    const filters = document.querySelector('.account-filter-details');
    if (!filters) return;
    const narrow = matchMedia('(max-width:600px)');
    const syncFilters = () => {
        const shouldOpen = !narrow.matches || filters.dataset.active === 'true';
        if (!shouldOpen && filters.contains(document.activeElement)) filters.querySelector('summary').focus();
        filters.open = shouldOpen;
    };
    narrow.addEventListener('change', syncFilters);
    syncFilters();
});
