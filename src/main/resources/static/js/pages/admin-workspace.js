/* Admin workspace presentation. No form, navigation or business handlers. */
(() => {
    'use strict';
    const page = document.querySelector('.adw-page');
    const frame = page?.closest('.page-wrapper');
    if (!page || !frame || frame.classList.contains('page-wrapper--candidate')) return;

    // dunglt bổ sung: các class khung adw-frame, adw-main, adw-sidebar*, adw-topbar, adw-actions, adw-footer
    // nay viết sẵn trong HTML (layout/internal, sidebar-shell, sidebar-footer, workspace-header). Gắn bằng JS sau
    // khi vẽ làm khung đổi kích thước giữa chừng (layout shift lớn).

    // Cover the full document, including the sidebar gutter on long table pages.
    const scene = page.querySelector('.adw-scene');
    if (scene) frame.prepend(scene);

    // interface.js already creates and wires these controls. Add presentation
    // aliases only, so migrated forms keep the same show/hide and error focus.
    page.querySelectorAll('.password-field').forEach(field => {
        field.classList.add('adw-password-field');
        field.querySelector('input')?.classList.add('adw-password-input');
        field.querySelector('.password-toggle')?.classList.add('adw-password-toggle');
    });
    page.querySelector('[data-validation-summary]')?.classList.add('adw-validation-summary');
})();
