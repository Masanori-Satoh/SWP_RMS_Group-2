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
});
