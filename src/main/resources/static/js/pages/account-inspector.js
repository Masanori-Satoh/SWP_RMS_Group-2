/* List inspectors for Admin accounts and departments; reuse native links and confirmation forms. */
(() => {
    'use strict';
    const page = document.querySelector('.aim-page');
    if (!page) return;
    const layout = page.querySelector('.aim-layout'), master = page.querySelector('.aim-master');
    const inspector = page.querySelector('.aim-inspector'), drawer = page.querySelector('.aim-drawer');
    const table = master?.querySelector('table'), scroll = master?.querySelector('.iac-scroll');
    if (!layout || !inspector || !drawer || !table || !scroll || !window.ResizeObserver || !drawer.showModal) return;
    const rows = [...table.querySelectorAll('.aim-row')];
    const department = page.dataset.inspectorKind === 'department';
    const detailTitle = department ? 'Chi tiết phòng ban' : 'Chi tiết tài khoản';
    const overviewTitle = department ? 'Tổng quan phòng ban' : 'Tổng quan tài khoản';
    const overview = page.dataset.accountOverview === 'true' || page.dataset.inspectorOverview === 'true'
        ? inspector.querySelector('.aim-summary') : null;
    const overviewButton = page.querySelector('.aim-open-overview');
    const detail = inspector.querySelector('.aim-detail'), actions = inspector.querySelector('.aim-detail-actions');
    const title = inspector.querySelector('.aim-panel-title'), close = inspector.querySelector('.aim-close');
    const reset = inspector.querySelector('.aim-reset-selection');
    const edit = inspector.querySelector('.aim-inspector-edit'), stateAction = inspector.querySelector('.aim-state-action');
    let selected = null, docked = false, panelClosed = !overview, lastWidth = -1, scheduled = false, returnFocus = null;

    const expanded = () => {
        rows.forEach(row => row.querySelector('.aim-select').setAttribute('aria-expanded', String(row === selected && (docked || drawer.open))));
        if (overviewButton) {
            overviewButton.setAttribute('aria-expanded', String(!panelClosed && (docked || drawer.open)));
            overviewButton.textContent = selected ? detailTitle : overviewTitle;
        }
    };
    const resetSelection = () => {
        selected = null;
        rows.forEach(row => { row.removeAttribute('data-selected'); row.querySelector('.aim-select').setAttribute('aria-pressed', 'false'); });
        detail.hidden = true; detail.replaceChildren(); actions.hidden = true;
        stateAction.hidden = true; stateAction.removeAttribute('form'); edit.hidden = true; edit.removeAttribute('href');
        if (overview) { overview.hidden = false; title.textContent = overviewTitle; }
        expanded();
    };
    const showDrawer = trigger => {
        if (docked) return;
        returnFocus = trigger;
        if (!drawer.open) drawer.showModal();
        close.focus(); expanded();
    };
    const setDocked = value => {
        const destination = value ? layout : drawer;
        inspector.hidden = panelClosed;
        if (docked === value && inspector.parentElement === destination) return;
        const wasDocked = docked, wasOpen = drawer.open;
        docked = value;
        // Avoid focus jumping into the page during a resize from modal to docked.
        if (drawer.open) drawer.close();
        layout.classList.toggle('aim-docked', value);
        destination.append(inspector);
        close.hidden = false;
        if (overviewButton) overviewButton.hidden = value;
        if (!value && wasDocked && selected && !panelClosed) showDrawer(selected.querySelector('.aim-select'));
        if (value && wasOpen) { title.tabIndex = -1; title.focus({preventScroll: true}); }
        expanded();
    };
    const select = (row, trigger) => {
        const template = page.querySelector(department
            ? '#department-inspector-template-' + row.dataset.inspectorId
            : '#account-inspector-template-' + row.dataset.accountId);
        if (!template) return;
        if (panelClosed) { panelClosed = false; lastWidth = -1; updateLayout(); }
        selected = row;
        rows.forEach(item => { item.toggleAttribute('data-selected', item === row); if (item === row) item.dataset.selected = 'true'; item.querySelector('.aim-select').setAttribute('aria-pressed', String(item === row)); });
        detail.replaceChildren(template.content.cloneNode(true)); detail.hidden = false; actions.hidden = false;
        if (overview) overview.hidden = true;
        title.textContent = detailTitle;
        const originalEdit = row.querySelector('.iac-edit'), form = row.querySelector('form[data-status-confirm]');
        edit.hidden = !originalEdit;
        if (originalEdit) edit.href = originalEdit.href; else edit.removeAttribute('href');
        stateAction.hidden = !form;
        if (form) {
            stateAction.setAttribute('form', form.id); stateAction.textContent = form.dataset.confirmLabel;
            stateAction.setAttribute('aria-label', form.dataset.confirmTitle);
            stateAction.classList.toggle('btn--danger', form.dataset.confirmDanger === 'true');
            stateAction.classList.toggle('btn--secondary', form.dataset.confirmDanger !== 'true');
        } else stateAction.removeAttribute('form');
        page.querySelector('.aim-announcement').textContent = (department ? 'Đã chọn phòng ban ' : 'Đã chọn tài khoản ') + row.querySelector('.aim-select').textContent.trim();
        if (!docked) showDrawer(trigger); else expanded();
    };
    const updateLayout = () => {
        scheduled = false;
        const width = layout.getBoundingClientRect().width;
        if (Math.abs(width - lastWidth) < 1) return;
        lastWidth = width;
        const minimum = parseFloat(getComputedStyle(table).minWidth) || 0;
        const gap = parseFloat(getComputedStyle(page).getPropertyValue('--s6')) || 24;
        setDocked(!panelClosed && (width - gap) * .75 >= minimum);
        // Long real values may need more space than the CSS minimum: keep the whole table instead.
        if (docked && scroll.scrollWidth > scroll.clientWidth + 1) setDocked(false);
    };
    table.addEventListener('click', event => {
        const row = event.target.closest('.aim-row');
        if (!row) return;
        const button = event.target.closest('.aim-select');
        if (button) {
            select(row, button);
            if (docked && event.detail === 0) { title.tabIndex = -1; title.focus({preventScroll: true}); }
        }
        else if (!event.target.closest('a,button,input,select,form') && !window.getSelection()?.toString()) select(row, row.querySelector('.aim-select'));
    });
    const closePanel = () => {
        const origin = selected?.querySelector('.aim-select');
        panelClosed = true; lastWidth = -1;
        if (drawer.open) drawer.close();
        updateLayout(); expanded();
        if (origin) origin.focus(); else overviewButton?.focus();
    };
    overviewButton?.addEventListener('click', () => {
        panelClosed = false; lastWidth = -1; updateLayout();
        if (!docked) showDrawer(overviewButton);
        else { title.tabIndex = -1; title.focus({preventScroll: true}); }
    });
    reset.addEventListener('click', () => {
        const origin = selected?.querySelector('.aim-select');
        resetSelection();
        if (overview) { if (docked) origin?.focus(); else close.focus(); }
        else { closePanel(); origin?.focus(); }
    });
    close.addEventListener('click', closePanel);
    drawer.addEventListener('close', () => {
        if (!docked) { panelClosed = true; lastWidth = -1; updateLayout(); }
        expanded(); if (!docked && returnFocus?.isConnected) returnFocus.focus();
    });
    drawer.addEventListener('keydown', event => {
        if (event.key !== 'Tab') return;
        const controls = [...drawer.querySelectorAll('button:not([disabled]),a[href],[tabindex="0"]')]
            .filter(control => control.getClientRects().length > 0);
        const first = controls[0], last = controls.at(-1);
        if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last?.focus(); }
        else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first?.focus(); }
    });
    drawer.addEventListener('click', event => {
        if (event.target !== drawer) return;
        const rect = drawer.getBoundingClientRect();
        if (event.clientX < rect.left || event.clientX > rect.right || event.clientY < rect.top || event.clientY > rect.bottom) drawer.close();
    });
    const advanced = page.querySelector('.aim-advanced-filters');
    if (advanced) {
        page.querySelectorAll('.aim-compact-filters select').forEach(control => {
            const updateTitle = () => { control.title = control.selectedOptions[0]?.textContent || ''; };
            control.addEventListener('change', updateTitle); updateTitle();
        });
        document.addEventListener('click', event => { if (!advanced.contains(event.target)) advanced.open = false; });
        advanced.addEventListener('keydown', event => {
            if (event.key === 'Escape' && advanced.open) { event.preventDefault(); advanced.open = false; advanced.querySelector('summary').focus(); }
        });
    }
    const observer = new ResizeObserver(() => { if (!scheduled) { scheduled = true; requestAnimationFrame(updateLayout); } });
    observer.observe(layout);
    resetSelection();
    rows.forEach(row => { row.querySelector('.aim-select').hidden = false; row.querySelector('.aim-name-fallback').hidden = true; });
    page.classList.add('aim-ready'); updateLayout();
})();
