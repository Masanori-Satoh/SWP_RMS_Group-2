document.addEventListener('DOMContentLoaded', () => {
    const button = document.getElementById('account-menu-button');
    const sidebar = document.getElementById('account-sidebar');
    button.addEventListener('click', () => {
        const open = sidebar.classList.toggle('open');
        button.setAttribute('aria-expanded', String(open));
    });
});
