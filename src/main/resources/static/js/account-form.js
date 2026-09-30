document.addEventListener('DOMContentLoaded', () => {
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
