document.addEventListener('DOMContentLoaded', () => {
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
