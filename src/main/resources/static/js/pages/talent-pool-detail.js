(() => {
    'use strict';
    document.querySelectorAll('[data-note-item]').forEach(item => {
        const editor = item.querySelector('[data-note-edit-form]');
        const text = item.querySelector('[data-note-text]');
        const toggle = item.querySelector('[data-note-edit]');
        const menu = item.querySelector('.tpw-note-menu');
        toggle?.addEventListener('click', () => {
            menu.open = false;
            editor.hidden = false;
            text.hidden = true;
            editor.querySelector('textarea').focus();
        });
        item.querySelector('[data-note-cancel]')?.addEventListener('click', () => {
            editor.hidden = true;
            text.hidden = false;
            editor.querySelector('textarea').value = text.textContent;
            menu.querySelector('summary').focus();
        });
        menu?.addEventListener('keydown', event => {
            if (event.key === 'Escape') { menu.open = false; menu.querySelector('summary').focus(); }
        });
    });
    document.addEventListener('click', event => {
        document.querySelectorAll('.tpw-note-menu[open]').forEach(menu => {
            if (!menu.contains(event.target)) menu.open = false;
        });
    });
    document.querySelectorAll('[data-note-edit-form], [data-note-delete]').forEach(form => {
        let saving = false;
        form.addEventListener('submit', event => {
            if (saving) { event.preventDefault(); return; }
            if (form.hasAttribute('data-note-delete') && !window.confirm('Xóa ghi chú này? Bạn không thể khôi phục sau khi xóa.')) {
                event.preventDefault(); return;
            }
            saving = true;
            form.setAttribute('aria-busy', 'true');
            form.querySelectorAll('button').forEach(button => { button.disabled = true; });
        });
        window.addEventListener('pageshow', () => {
            saving = false;
            form.removeAttribute('aria-busy');
            form.querySelectorAll('button').forEach(button => { button.disabled = false; });
        });
    });
    const input = document.querySelector('[data-pool-note] textarea');
    const counter = document.querySelector('[data-note-counter]');
    if (input && counter) {
        const update = () => { counter.textContent = `${input.value.length}/${input.maxLength}`; };
        input.addEventListener('input', update);
        update();
    }
})();
