document.querySelectorAll('[data-pool-cv]').forEach(select => {
    select.addEventListener('change', () => {
        if (select.closest('.tp-page')) return;
        const checkbox = select.closest('tr').querySelector('input[name="sourceApplicationIds"]');
        if (checkbox) checkbox.value = select.value;
    });
});

(() => {
    'use strict';
    const page = document.querySelector('.tp-page');
    if (!page) return;
    const byId = id => document.getElementById(id);
    const rows = [...page.querySelectorAll('[data-pool-candidate]')];
    const form = byId('tp-selection-form');
    const confirm = byId('tp-confirm');
    const notesDialog = byId('tp-notes');
    const feedback = byId('tp-feedback');
    let storage;
    try { storage = window.sessionStorage; } catch { storage = null; }
    const scope = page.dataset.poolScope;
    const key = scope && page.dataset.poolJob
        ? `rms:talent-selection:v1:${encodeURIComponent(scope)}:${page.dataset.poolJob}` : null;
    const state = form ? new globalThis.TalentPoolSelection(storage, key) : null;
    let reviewRequest;
    let notesRequest;
    let notesButton;

    page.querySelectorAll('[data-pool-close]').forEach(button => {
        button.addEventListener('click', () => button.closest('dialog').close());
    });

    function currentItem(row) {
        const select = row.querySelector('[data-pool-cv]');
        const option = select?.selectedOptions[0];
        if (!option) return null;
        return {candidateId: Number(row.dataset.poolCandidate), name: row.dataset.poolName,
            sourceId: Number(option.value), cvLabel: option.textContent.trim()};
    }

    function syncCv(row) {
        const select = row.querySelector('[data-pool-cv]');
        const option = select?.selectedOptions[0];
        if (!option) return;
        select.title = option.textContent.trim();
        const preview = row.querySelector('[data-app-preview]');
        preview.dataset.cvUrl = option.dataset.cvUrl;
        preview.dataset.job = option.dataset.jobTitle;
        const checkbox = row.querySelector('[data-pool-select]');
        if (checkbox) checkbox.value = option.value;
    }

    function updateSelection() {
        if (!state) return;
        rows.forEach(row => {
            const selected = Boolean(state.get(Number(row.dataset.poolCandidate)));
            const checkbox = row.querySelector('[data-pool-select]');
            if (checkbox) checkbox.checked = selected;
            row.classList.toggle('tp-row--selected', selected);
            row.querySelector('[data-pool-selected-label]').hidden = !selected;
        });
        const eligible = rows.map(row => row.querySelector('[data-pool-select]')).filter(input => input && !input.disabled);
        const checked = eligible.filter(input => input.checked).length;
        const all = byId('tp-select-page');
        if (all) {
            all.disabled = eligible.length === 0;
            all.checked = eligible.length > 0 && checked === eligible.length;
            all.indeterminate = checked > 0 && checked < eligible.length;
        }
        byId('tp-selection-bar').hidden = state.size === 0;
        byId('tp-selection-slot').hidden = state.size === 0;
        byId('tp-selection-count').textContent = `Đã chọn ${state.size} ứng viên`;
        byId('tp-review-selection').textContent = `Thêm ${state.size} ứng viên`;
        if (!state.persistenceAvailable && state.size > 0) {
            feedback.textContent = 'Trình duyệt không cho lưu lựa chọn qua trang. Hãy kiểm tra hoặc bỏ chọn trước khi chuyển trang/lọc.';
        }
    }

    rows.forEach(row => {
        const select = row.querySelector('[data-pool-cv]');
        const checkbox = row.querySelector('[data-pool-select]');
        if (state && checkbox) {
            checkbox.disabled = checkbox.dataset.eligible !== 'true';
            const saved = state.get(Number(row.dataset.poolCandidate));
            if (saved) {
                if (select && [...select.options].some(option => Number(option.value) === saved.sourceId)) {
                    select.value = String(saved.sourceId);
                    state.set(currentItem(row));
                } else {
                    state.remove(saved.candidateId);
                    feedback.textContent = `${row.dataset.poolName}: CV đã chọn không còn trong danh sách; vui lòng chọn lại.`;
                }
            }
            checkbox.addEventListener('change', () => {
                try {
                    if (checkbox.checked) state.set(currentItem(row));
                    else state.remove(Number(row.dataset.poolCandidate));
                } catch (error) { feedback.textContent = error.message; }
                updateSelection();
            });
        }
        syncCv(row);
        select?.addEventListener('change', () => {
            syncCv(row);
            if (state?.get(Number(row.dataset.poolCandidate))) state.set(currentItem(row));
            updateSelection();
        });
    });

    async function json(response) {
        const data = response.headers.get('content-type')?.includes('application/json') ? await response.json() : null;
        if (!response.ok || !data || data.success !== true) throw new Error(data?.message || (response.status === 403
            ? 'Bạn không có quyền truy cập hoặc phiên đăng nhập đã hết. Hãy tải lại trang.'
            : 'Không tải được dữ liệu. Hãy kiểm tra kết nối và thử lại.'));
        return data.data;
    }

    function ajaxHeaders(jsonBody = false) {
        const headers = {Accept: 'application/json'};
        const csrf = document.querySelector('meta[name="_csrf"]')?.content;
        if (csrf) headers['X-CSRF-TOKEN'] = csrf;
        if (jsonBody) headers['Content-Type'] = 'application/json';
        return headers;
    }

    async function loadNotes() {
        notesRequest?.abort();
        const controller = new AbortController();
        notesRequest = controller;
        const timer = setTimeout(() => controller.abort(), 20000);
        const status = byId('tp-notes-status');
        const list = byId('tp-notes-list');
        list.replaceChildren();
        status.textContent = 'Đang tải ghi chú…';
        byId('tp-notes-retry').hidden = true;
        try {
            const notes = await json(await fetch(notesButton.dataset.notesUrl, {
                signal: controller.signal, cache: 'no-store', headers: ajaxHeaders()
            }));
            if (controller.signal.aborted || !notesDialog.open) return;
            status.textContent = notes.length ? '' : 'Chưa có ghi chú chung.';
            notes.forEach(note => {
                const item = document.createElement('li');
                item.className = 'app-note';
                const meta = document.createElement('div');
                meta.className = 'app-note__meta';
                const author = document.createElement('strong');
                author.textContent = note.author;
                const time = document.createElement('time');
                if (note.createdAt) {
                    const date = new Date(note.createdAt);
                    time.dateTime = note.createdAt;
                    time.textContent = Number.isNaN(date.getTime()) ? '' : date.toLocaleString('vi-VN');
                }
                const body = document.createElement('p');
                body.className = 'app-note__body';
                body.textContent = note.content;
                meta.append(author, time);
                item.append(meta, body);
                list.append(item);
            });
        } catch (error) {
            if (notesRequest !== controller || !notesDialog.open) return;
            status.textContent = controller.signal.aborted ? 'Tải ghi chú quá lâu. Hãy thử lại.' : error.message;
            byId('tp-notes-retry').hidden = false;
        } finally { clearTimeout(timer); }
    }
    page.querySelectorAll('[data-pool-notes]').forEach(button => {
        button.addEventListener('click', () => {
            notesButton = button;
            const row = button.closest('[data-pool-candidate]');
            byId('tp-notes-name').textContent = row.dataset.poolName;
            byId('tp-notes-detail').href = row.querySelector('a[title="Xem hồ sơ chi tiết"]').href;
            notesDialog.showModal();
            loadNotes();
        });
    });
    byId('tp-notes-retry').addEventListener('click', loadNotes);
    notesDialog.addEventListener('close', () => notesRequest?.abort());
    if (!state) return;

    function svgIcon(path) {
        const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
        for (const [key, value] of Object.entries({viewBox: '0 0 24 24', width: '18', height: '18', fill: 'none', stroke: 'currentColor', 'stroke-width': '1.8', 'aria-hidden': 'true'})) svg.setAttribute(key, value);
        const line = document.createElementNS('http://www.w3.org/2000/svg', 'path');
        line.setAttribute('d', path);
        svg.append(line);
        return svg;
    }

    function reviewStatus(message, kind) {
        const status = byId('tp-review-status');
        status.className = kind ? `alert alert--${kind} tp-confirm-status` : 'tp-confirm-status';
        status.replaceChildren();
        if (kind === 'success') status.append(svgIcon('M22 11.1V12a10 10 0 1 1-5.9-9.1 M22 4 12 14l-3-3'));
        const text = document.createElement('span');
        text.textContent = message;
        status.append(text);
    }

    function detailUrl(candidateId) {
        const query = new URLSearchParams(window.location?.search || '');
        const context = new URLSearchParams();
        ['jobPostingId', 'keyword', 'markedBy', 'sourceJobId', 'page'].forEach(key => {
            if (query.has(key)) context.set(key, query.get(key));
        });
        const suffix = context.toString();
        return `${page.dataset.poolBase || '/talent-pool'}/${candidateId}${suffix ? '?' + suffix : ''}`;
    }

    function renderReview(results = null) {
        const list = byId('tp-confirm-list');
        list.replaceChildren();
        state.values().forEach(selected => {
            const result = results?.get(selected.sourceId);
            const item = document.createElement('li');
            item.className = 'tp-confirm-item';
            const content = document.createElement('div');
            const name = document.createElement('strong');
            name.textContent = result?.candidateName || selected.name || `Ứng viên #${selected.candidateId}`;
            const cv = document.createElement('p');
            cv.className = 'tp-confirm-source';
            cv.append(svgIcon('M14 3H5v18h14V8Z M14 3v5h5 M8 12h8 M8 16h6'));
            const cvName = document.createElement('span');
            cvName.textContent = result?.cvTitle || selected.cvLabel || `CV nguồn #${selected.sourceId}`;
            cv.append(cvName);
            content.append(name, cv);
            if (result?.submittedAt) {
                const date = new Date(result.submittedAt);
                if (!Number.isNaN(date.getTime())) {
                    const time = document.createElement('p');
                    time.className = 'tp-confirm-date';
                    time.textContent = `CV từ lần ứng tuyển: ${date.toLocaleDateString('vi-VN')}`;
                    content.append(time);
                }
            }
            if (results) {
                const outcome = document.createElement('p');
                const error = !result ? 'Chưa kiểm tra được hồ sơ này.' : result.candidateId !== selected.candidateId
                    ? 'CV nguồn không khớp ứng viên đã chọn. Hãy bỏ chọn và chọn lại.' : result.error;
                if (error) {
                    outcome.className = 'tp-review-error';
                    outcome.textContent = error;
                    content.append(outcome);
                }
            }
            const actions = document.createElement('div');
            actions.className = 'tp-confirm-actions';
            const preview = document.createElement('button');
            preview.type = 'button';
            preview.className = 'btn btn--secondary btn--sm app-icon-button';
            preview.setAttribute('data-app-preview', '');
            preview.disabled = !result || result.candidateId !== selected.candidateId;
            preview.title = 'Xem CV nguồn';
            preview.setAttribute('aria-label', `Xem CV nguồn của ${name.textContent}`);
            preview.setAttribute('aria-haspopup', 'dialog');
            preview.setAttribute('aria-controls', 'app-preview');
            preview.dataset.name = name.textContent;
            preview.dataset.job = cvName.textContent;
            preview.dataset.cvUrl = `${page.dataset.poolBase || '/talent-pool'}/${selected.candidateId}/cv/${selected.sourceId}`;
            preview.dataset.detailUrl = detailUrl(selected.candidateId);
            preview.append(svgIcon('M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12Z M15 12a3 3 0 1 1-6 0 3 3 0 0 1 6 0'));
            const remove = document.createElement('button');
            remove.type = 'button';
            remove.className = 'btn btn--secondary btn--sm app-icon-button';
            remove.title = 'Bỏ chọn';
            remove.append(svgIcon('m6 6 12 12 M6 18 18 6'));
            remove.setAttribute('aria-label', `Bỏ chọn ${name.textContent}`);
            remove.addEventListener('click', () => {
                state.remove(selected.candidateId);
                updateSelection();
                if (!state.size) confirm.close();
                else {
                    requestReview();
                    byId('tp-confirm-title').focus();
                }
            });
            actions.append(preview, remove);
            item.append(content, actions);
            list.append(item);
        });
        byId('tp-confirm-title').textContent = `Xác nhận thêm ${state.size} ứng viên`;
        byId('tp-confirm-submit').textContent = `Xác nhận thêm ${state.size} ứng viên`;
    }

    async function requestReview() {
        reviewRequest?.abort();
        const controller = new AbortController();
        reviewRequest = controller;
        const timer = setTimeout(() => controller.abort(), 20000);
        byId('tp-confirm-submit').disabled = true;
        byId('tp-recheck').disabled = true;
        reviewStatus('Đang kiểm tra CV và hồ sơ trùng…');
        renderReview();
        const body = JSON.stringify({sourceApplicationIds: state.values().map(item => item.sourceId)});
        try {
            const result = await json(await fetch(form.action, {
                method: 'POST', body, signal: controller.signal, headers: ajaxHeaders(true)
            }));
            if (controller.signal.aborted || !confirm.open) return;
            const results = new Map(result.items.map(item => [item.sourceApplicationId, item]));
            renderReview(results);
            const hasErrors = state.values().some(item => {
                const checked = results.get(item.sourceId);
                return !checked || checked.error || checked.candidateId !== item.candidateId;
            });
            const validCount = state.values().filter(item => {
                const checked = results.get(item.sourceId);
                return checked && !checked.error && checked.candidateId === item.candidateId;
            }).length;
            reviewStatus(hasErrors ? `${validCount}/${state.size} ứng viên có CV nguồn hợp lệ. Xem lý do dưới từng hồ sơ.`
                : `${state.size} ứng viên có CV nguồn hợp lệ`, hasErrors ? 'warning' : 'success');
            // Intentionally disabled: legacy /suggest creates Applications, contrary to this flow's contract.
            byId('tp-confirm-submit').disabled = true;
        } catch (error) {
            if (reviewRequest !== controller || !confirm.open) return;
            reviewStatus(controller.signal.aborted ? 'Kiểm tra quá lâu. Hãy thử lại.' : error.message, 'warning');
        } finally {
            clearTimeout(timer);
            if (reviewRequest === controller) byId('tp-recheck').disabled = false;
        }
    }
    byId('tp-review-selection').addEventListener('click', () => {
        if (!state.size) return;
        confirm.showModal();
        requestReview();
    });
    byId('tp-recheck').addEventListener('click', requestReview);
    confirm.addEventListener('close', () => reviewRequest?.abort());
    form.addEventListener('submit', event => {
        event.preventDefault();
        reviewStatus('Chức năng xác nhận thêm chưa sẵn sàng.', 'warning');
    });
    byId('tp-clear-selection').addEventListener('click', () => {
        state.clear();
        feedback.textContent = '';
        updateSelection();
    });
    byId('tp-select-page')?.addEventListener('change', event => {
        rows.forEach(row => {
            const checkbox = row.querySelector('[data-pool-select]');
            if (!checkbox || checkbox.disabled) return;
            try {
                if (event.target.checked) state.set(currentItem(row));
                else state.remove(Number(row.dataset.poolCandidate));
            } catch (error) { feedback.textContent = error.message; }
        });
        updateSelection();
    });
    const guardNavigation = event => {
        if (state.size && !state.persistenceAvailable) {
            event.preventDefault();
            feedback.textContent = 'Không thể giữ lựa chọn qua trang trên trình duyệt này. Hãy bỏ chọn tất cả trước khi chuyển trang hoặc lọc.';
            byId('tp-review-selection').focus();
        }
    };
    page.querySelectorAll('.pagination-wrapper a, .tp-filter a, [data-pool-detail], #tp-notes-detail, #app-preview-detail')
        .forEach(link => link.addEventListener('click', guardNavigation));
    page.querySelector('.tp-filter').addEventListener('submit', guardNavigation);
    updateSelection();
})();
document.querySelectorAll('[data-dialog-open^="talent-"]').forEach(button => {
    button.addEventListener('click', () => {
        const dialog = document.getElementById(button.dataset.dialogOpen);
        if (dialog) dialog.showModal();
    });
});
document.querySelectorAll('dialog[id^="talent-"] [data-dialog-close]').forEach(button => {
    button.addEventListener('click', () => button.closest('dialog').close());
});

