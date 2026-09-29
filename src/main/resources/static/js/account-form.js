document.addEventListener('DOMContentLoaded', () => {
    const menuButton = document.getElementById('account-menu-button');
    const sidebar = document.getElementById('account-sidebar');
    menuButton.addEventListener('click', () => {
        const open = sidebar.classList.toggle('open');
        menuButton.setAttribute('aria-expanded', String(open));
    });

    const role = document.getElementById('role');
    const department = document.getElementById('department');
    const syncRequiredFields = () => {
        const selected = role.options[role.selectedIndex];
        const candidate = selected && selected.dataset.roleName === 'Candidate';
        department.required = Boolean(selected && selected.value && !candidate);
    };
    role.addEventListener('change', syncRequiredFields);
    syncRequiredFields();
});
