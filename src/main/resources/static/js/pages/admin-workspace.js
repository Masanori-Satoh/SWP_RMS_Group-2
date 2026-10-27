/* Admin workspace presentation. No form, navigation or business handlers. */
(() => {
    'use strict';
    const page = document.querySelector('.adw-page');
    const frame = page?.closest('.page-wrapper');
    if (!page || !frame || frame.classList.contains('page-wrapper--candidate')) return;

    const chrome = [
        [frame, 'adw-frame'],
        [page.closest('.main-content'), 'adw-main'],
        [document.getElementById('workspace-navigation'), 'adw-sidebar'],
        [document.querySelector('.sidebar-top-bar'), 'adw-sidebar-head'],
        [document.querySelector('.sidebar-menu-list'), 'adw-sidebar-menu'],
        [document.querySelector('.workspace-header'), 'adw-topbar'],
        [document.querySelector('.workspace-header-actions'), 'adw-actions'],
        [document.querySelector('.sidebar-footer'), 'adw-footer']
    ];
    chrome.forEach(([element, className]) => element?.classList.add(className));

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
