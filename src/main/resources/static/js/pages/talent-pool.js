document.querySelectorAll('[data-pool-cv]').forEach(select => {
    select.addEventListener('change', () => {
        const checkbox = select.closest('tr').querySelector('input[name="sourceApplicationIds"]');
        if (checkbox) checkbox.value = select.value;
    });
});
document.querySelectorAll('[data-dialog-open^="talent-"]').forEach(button => {
    button.addEventListener('click', () => {
        const dialog = document.getElementById(button.dataset.dialogOpen);
        if (dialog) dialog.showModal();
    });
});
document.querySelectorAll('dialog[id^="talent-"] [data-dialog-close]').forEach(button => {
    button.addEventListener('click', () => button.closest('dialog').close());
});

