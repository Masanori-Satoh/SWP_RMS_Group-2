/* Application CV preview and shared talent-pool bookmarks. No workflow mutations. */
(() => {
    'use strict';
    const page = document.querySelector('.app-page');
    if (!page) return;
    const byId = (id) => document.getElementById(id);
    const preview = byId('app-preview');
    const bookmark = byId('app-bookmark');
    let previewRequest;
    let previewObjectUrl;
    let bookmarkRequest;
    let bookmarkSource;
    let saving = false;

    page.querySelectorAll('[data-app-close]').forEach((button) => {
        button.addEventListener('click', () => {
            if (button.closest('dialog') === bookmark && saving) return;
            button.closest('dialog').close();
        });
    });

    const previewExpandBtn = byId('app-preview-expand');
    const updatePreviewExpandState = (isWide) => {
        if (!previewExpandBtn) return;
        const label = previewExpandBtn.querySelector('.app-preview-expand-text');
        if (label) label.textContent = isWide ? 'Thu nhỏ' : 'Mở rộng';
        previewExpandBtn.title = isWide ? 'Thu nhỏ về khổ đọc chuẩn' : 'Mở rộng toàn màn hình';
    };
    previewExpandBtn?.addEventListener('click', () => {
        const isWide = preview.classList.toggle('app-dialog--preview-wide');
        updatePreviewExpandState(isWide);
    });

    const releasePreview = () => {
        previewRequest?.abort();
        byId('app-preview-frame').removeAttribute('src');
        if (previewObjectUrl) URL.revokeObjectURL(previewObjectUrl);
        previewObjectUrl = null;
        preview?.classList.remove('app-dialog--preview-wide');
        updatePreviewExpandState(false);
    };
    preview?.addEventListener('close', releasePreview);

    page.querySelectorAll('[data-app-preview]').forEach((button) => {
        button.addEventListener('click', async () => {
            releasePreview();
            const controller = new AbortController();
            previewRequest = controller;
            const timer = setTimeout(() => controller.abort(), 30000);
            const frame = byId('app-preview-frame');
            const status = byId('app-preview-status');
            frame.hidden = true;
            frame.title = `CV của ${button.dataset.name}`;
            byId('app-preview-title').textContent = `CV · ${button.dataset.name}`;
            byId('app-preview-job').textContent = button.dataset.job || '';
            byId('app-preview-original').href = button.dataset.cvUrl;
            byId('app-preview-detail').href = button.dataset.detailUrl;
            status.textContent = 'Đang tải CV…';
            preview.showModal();
            try {
                const response = await fetch(button.dataset.cvUrl, { signal: controller.signal, cache: 'no-store' });
                if (!response.ok) throw new Error('unavailable');
                if (!response.headers.get('content-type')?.toLowerCase().includes('application/pdf')) {
                    throw new Error('unsupported');
                }
                const blob = await response.blob();
                if (controller.signal.aborted || !preview.open) return;
                previewObjectUrl = URL.createObjectURL(blob);
                // PDF viewers may honor these hints; the full-size frame works independently.
                frame.src = `${previewObjectUrl}#view=FitH&navpanes=0`;
                frame.hidden = false;
                status.textContent = '';
            } catch (error) {
                if (previewRequest !== controller || !preview.open) return;
                status.textContent = error.message === 'unsupported'
                    ? 'Tài nguyên này không hỗ trợ xem nhanh PDF. Hãy mở CV trong tab mới hoặc xem hồ sơ chi tiết.'
                    : 'Không tải được CV. Tệp có thể không còn tồn tại, phiên đăng nhập đã hết hoặc kết nối bị gián đoạn. Hãy thử mở CV trong tab mới.';
            } finally {
                clearTimeout(timer);
            }
        });
    });

    if (!bookmark) return;
    const form = byId('app-bookmark-form');
    const submit = byId('app-bookmark-submit');
    const noteInput = byId('app-bookmark-content');
    const charCount = byId('app-bookmark-char-count');
    const status = byId('app-bookmark-status');
    const errorMessage = byId('app-bookmark-error');
    const retry = byId('app-bookmark-retry');
    const removeButton = byId('app-bookmark-remove');
    const removeConfirm = byId('app-bookmark-remove-confirm');
    const removeSubmit = byId('app-bookmark-remove-submit');

    function getInitials(name) {
        if (!name) return 'UV';
        const parts = name.trim().split(/\s+/).filter(Boolean);
        if (parts.length === 1) return parts[0].substring(0, 2).toUpperCase();
        return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
    }

    function toggleRemoveConfirmation(show) {
        removeConfirm.hidden = !show;
        submit.hidden = show;
        removeButton.hidden = show || bookmarkSource?.dataset.saved !== 'true';
    }

    function renderNotes(list, notes) {
        list.replaceChildren();
        notes.forEach((note) => {
            const item = document.createElement('li');
            item.className = 'app-note';
            const meta = document.createElement('div');
            meta.className = 'app-note__meta';
            const authorWrap = document.createElement('div');
            authorWrap.className = 'app-note__author-wrap';
            const authorIcon = document.createElement('span');
            authorIcon.className = 'app-note__author-icon';
            authorIcon.setAttribute('aria-hidden', 'true');
            authorIcon.textContent = (note.author || 'U').trim().charAt(0).toUpperCase();
            const author = document.createElement('strong');
            author.className = 'app-note__author-name';
            author.textContent = note.author;
            authorWrap.append(authorIcon, author);
            const time = document.createElement('time');
            if (note.createdAt) {
                time.dateTime = note.createdAt;
                const date = new Date(note.createdAt);
                time.textContent = Number.isNaN(date.getTime()) ? '' : date.toLocaleString('vi-VN', {
                    day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit'
                });
            }
            meta.append(authorWrap, time);
            const body = document.createElement('p');
            body.className = 'app-note__body';
            body.textContent = note.content;
            item.append(meta, body);
            list.append(item);
        });
    }

    function reflectSaved(data) {
        page.querySelectorAll('[data-app-bookmark]').forEach((button) => {
            if (button.dataset.candidateKey !== bookmarkSource.dataset.candidateKey) return;
            button.dataset.saved = String(data.saved);
            button.classList.toggle('app-bookmark-button--saved', data.saved);
            button.title = data.saved ? 'Đã lưu vào kho tiềm năng' : 'Lưu vào kho ứng viên tiềm năng';
            button.setAttribute('aria-label', `${data.saved ? 'Xem ghi chú chung của' : 'Lưu ứng viên'} ${button.dataset.name}`);
            const label = button.querySelector('[data-app-bookmark-label]');
            if (label) label.textContent = data.saved ? 'Thêm ghi chú' : 'Lưu ứng viên';
            const scope = button.closest('[data-app-row]') || page;
            scope.querySelectorAll('[data-app-saved-badge], [data-app-pool-link]').forEach((item) => {
                item.hidden = !data.saved;
            });
        });
        const detailNotes = byId('app-detail-notes');
        if (detailNotes) {
            renderNotes(detailNotes, data.notes);
            byId('app-detail-notes-empty').hidden = data.notes.length > 0;
        }
        const bannerBadge = byId('app-bookmark-badge');
        if (bannerBadge) {
            bannerBadge.textContent = data.saved ? '✓ Đã lưu tiềm năng' : 'Chưa lưu tiềm năng';
            bannerBadge.className = `app-candidate-banner__badge ${data.saved ? 'app-candidate-banner__badge--saved' : 'app-candidate-banner__badge--unsaved'}`;
        }
    }

    async function readJson(response) {
        const json = response.headers.get('content-type')?.includes('application/json')
            ? await response.json() : null;
        if (!response.ok || !json) {
            throw new Error(json?.message || (response.status === 403
                ? 'Bạn không có quyền truy cập hoặc phiên đăng nhập đã hết. Hãy tải lại trang.'
                : 'Không thể tải dữ liệu. Hãy kiểm tra kết nối và thử lại.'));
        }
        return json;
    }

    async function loadBookmark() {
        bookmarkRequest?.abort();
        const controller = new AbortController();
        bookmarkRequest = controller;
        const timer = setTimeout(() => controller.abort(), 20000);
        submit.disabled = true;
        removeButton.disabled = true;
        toggleRemoveConfirmation(false);
        retry.hidden = true;
        status.textContent = 'Đang tải ghi chú…';
        errorMessage.textContent = '';
        byId('app-bookmark-notes').replaceChildren();
        try {
            const data = await readJson(await fetch(bookmarkSource.dataset.bookmarkUrl, {
                signal: controller.signal, cache: 'no-store', headers: { Accept: 'application/json' }
            }));
            if (controller.signal.aborted || !bookmark.open) return;
            reflectSaved(data);
            renderNotes(byId('app-bookmark-notes'), data.notes);
            byId('app-bookmark-title').textContent = data.saved ? 'Ghi chú ứng viên' : 'Lưu ứng viên tiềm năng';
            const countBadge = byId('app-bookmark-count');
            if (countBadge) {
                countBadge.textContent = String(data.notes.length);
                countBadge.hidden = !data.notes.length;
            }
            status.textContent = data.notes.length ? '' : 'Chưa có ghi chú nào cho ứng viên này.';
            noteInput.required = data.saved;
            noteInput.placeholder = data.saved ? 'Nhập nhận xét…' : 'Nhập nhận xét (không bắt buộc)…';
            submit.textContent = data.saved ? 'Thêm ghi chú' : 'Lưu ứng viên';
            submit.disabled = false;
            removeButton.disabled = false;
            toggleRemoveConfirmation(false);
        } catch (error) {
            if (bookmarkRequest !== controller || !bookmark.open) return;
            status.textContent = '';
            errorMessage.textContent = controller.signal.aborted ? 'Tải ghi chú quá lâu. Hãy thử lại.' : error.message;
            retry.hidden = false;
        } finally {
            clearTimeout(timer);
        }
    }

    page.querySelectorAll('[data-app-bookmark]').forEach((button) => {
        button.addEventListener('click', () => {
            bookmarkSource = button;
            form.action = button.dataset.bookmarkUrl;
            noteInput.value = '';
            noteInput.setCustomValidity('');
            if (charCount) charCount.textContent = '0/1000';
            const name = button.dataset.name || '';
            byId('app-bookmark-name').textContent = name;
            const avatar = byId('app-bookmark-avatar');
            if (avatar) avatar.textContent = getInitials(name);
            const jobEl = byId('app-bookmark-job');
            if (jobEl) {
                jobEl.textContent = button.dataset.job ? `Vị trí ứng tuyển: ${button.dataset.job}` : '';
                jobEl.hidden = !button.dataset.job;
            }
            const badge = byId('app-bookmark-badge');
            if (badge) {
                const isSaved = button.dataset.saved === 'true';
                badge.textContent = isSaved ? '✓ Đã lưu tiềm năng' : 'Chưa lưu tiềm năng';
                badge.className = `app-candidate-banner__badge ${isSaved ? 'app-candidate-banner__badge--saved' : 'app-candidate-banner__badge--unsaved'}`;
            }
            const countBadge = byId('app-bookmark-count');
            if (countBadge) countBadge.hidden = true;
            bookmark.showModal();
            loadBookmark();
        });
    });
    retry.addEventListener('click', loadBookmark);
    bookmark.addEventListener('cancel', (event) => { if (saving) event.preventDefault(); });
    bookmark.addEventListener('close', () => bookmarkRequest?.abort());
    noteInput.addEventListener('input', () => {
        noteInput.setCustomValidity('');
        if (charCount) charCount.textContent = `${noteInput.value.length}/1000`;
    });
    removeButton.addEventListener('click', () => {
        if (saving || removeButton.disabled) return;
        toggleRemoveConfirmation(true);
        removeSubmit.focus();
    });
    byId('app-bookmark-remove-cancel').addEventListener('click', () => {
        if (saving) return;
        toggleRemoveConfirmation(false);
        removeButton.focus();
    });
    removeSubmit.addEventListener('click', () => mutateBookmark(true));

    async function mutateBookmark(removing = false) {
        if (saving || submit.disabled) return;
        if (!removing && noteInput.required && !noteInput.value.trim()) {
            noteInput.setCustomValidity('Vui lòng nhập ghi chú.');
            noteInput.reportValidity();
            return;
        }
        saving = true;
        retry.hidden = true;
        noteInput.readOnly = true;
        submit.disabled = true;
        const label = submit.textContent;
        submit.textContent = 'Đang lưu…';
        removeSubmit.textContent = 'Đang bỏ lưu…';
        errorMessage.textContent = '';
        bookmark.querySelectorAll('[data-app-close], #app-bookmark-remove, #app-bookmark-remove-submit, #app-bookmark-remove-cancel').forEach((button) => { button.disabled = true; });
        try {
            const data = await readJson(await fetch(removing ? `${form.action}/remove` : form.action, {
                method: 'POST', body: new FormData(form), headers: { Accept: 'application/json' },
                signal: AbortSignal.timeout(30000)
            }));
            reflectSaved(data);
            byId('app-feedback').textContent = `${bookmarkSource.dataset.name}: ${removing ? 'đã bỏ lưu tiềm năng.' : noteInput.value.trim() ? 'đã lưu ghi chú chung.' : 'đã lưu vào kho tiềm năng.'}`;
            bookmark.close();
        } catch (error) {
            errorMessage.textContent = `${error.message} Nếu kết nối bị ngắt khi lưu, hãy tải lại ghi chú để kiểm tra trước khi gửi lại.`;
            retry.hidden = false;
        } finally {
            saving = false;
            noteInput.readOnly = false;
            submit.disabled = false;
            submit.textContent = label;
            removeSubmit.textContent = 'Xác nhận bỏ lưu';
            bookmark.querySelectorAll('[data-app-close], #app-bookmark-remove, #app-bookmark-remove-submit, #app-bookmark-remove-cancel').forEach((button) => { button.disabled = false; });
        }
    }

    form.addEventListener('submit', (event) => {
        event.preventDefault();
        if (!removeConfirm.hidden) return;
        mutateBookmark();
    });
})();
