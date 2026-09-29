document.addEventListener('DOMContentLoaded', () => {
    const dialog = document.getElementById('deactivate-dialog');
    const form = document.getElementById('deactivate-form');
    const name = document.getElementById('deactivate-account-name');

    document.querySelectorAll('[data-deactivate-account]').forEach((button) => {
        button.addEventListener('click', () => {
            name.textContent = button.dataset.accountName;
            form.action = button.dataset.deactivateUrl;
            dialog.showModal();
        });
    });

    document.getElementById('deactivate-cancel').addEventListener('click', () => dialog.close());

    const menuButton = document.getElementById('account-menu-button');
    const sidebar = document.getElementById('account-sidebar');
    menuButton.addEventListener('click', () => {
        const open = sidebar.classList.toggle('open');
        menuButton.setAttribute('aria-expanded', String(open));
    });
});
