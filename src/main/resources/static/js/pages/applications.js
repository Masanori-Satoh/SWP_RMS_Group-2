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

    const releasePreview = () => {
        previewRequest?.abort();
        byId('app-preview-frame').removeAttribute('src');
        if (previewObjectUrl) URL.revokeObjectURL(previewObjectUrl);
        previewObjectUrl = null;
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
            byId('app-preview-job').textContent = button.dataset.job;
            byId('app-preview-original').href = button.dataset.cvUrl;
            byId('app-preview-detail').href = button.dataset.detailUrl;
            status.textContent = 'Đang tải CV…';
            preview.showModal();
            try {
                const response = await fetch(button.dataset.cvUrl, {signal: controller.signal, cache: 'no-store'});
                if (!response.ok) throw new Error('unavailable');
                if (!response.headers.get('content-type')?.toLowerCase().includes('application/pdf')) {
                    throw new Error('unsupported');
                }
                const blob = await response.blob();
                if (controller.signal.aborted || !preview.open) return;
                previewObjectUrl = URL.createObjectURL(blob);
                frame.src = previewObjectUrl;
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
    const status = byId('app-bookmark-status');
    const errorMessage = byId('app-bookmark-error');
    const retry = byId('app-bookmark-retry');

    function renderNotes(list, notes) {
        list.replaceChildren();
        notes.forEach((note) => {
            const item = document.createElement('li');
            item.className = 'app-note';
            const meta = document.createElement('div');
            meta.className = 'app-note__meta';
            const author = document.createElement('strong');
            author.textContent = note.author;
            const time = document.createElement('time');
            if (note.createdAt) {
                time.dateTime = note.createdAt;
                const date = new Date(note.createdAt);
                time.textContent = Number.isNaN(date.getTime()) ? '' : date.toLocaleString('vi-VN', {
                    day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit'
                });
            }
            const body = document.createElement('p');
            body.className = 'app-note__body';
            body.textContent = note.content;
            meta.append(author, time);
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
        retry.hidden = true;
        status.textContent = 'Đang tải trạng thái và ghi chú chung…';
        errorMessage.textContent = '';
        byId('app-bookmark-notes').replaceChildren();
        try {
            const data = await readJson(await fetch(bookmarkSource.dataset.bookmarkUrl, {
                signal: controller.signal, cache: 'no-store', headers: {Accept: 'application/json'}
            }));
            if (controller.signal.aborted || !bookmark.open) return;
            reflectSaved(data);
            renderNotes(byId('app-bookmark-notes'), data.notes);
            byId('app-bookmark-title').textContent = data.saved ? 'Ghi chú ứng viên tiềm năng' : 'Lưu ứng viên tiềm năng';
            status.textContent = data.saved ? (data.notes.length ? 'Đã lưu trong kho tiềm năng chung.' : 'Đã lưu trong kho tiềm năng chung. Chưa có ghi chú.') : 'Ứng viên chưa được lưu vào kho tiềm năng.';
            noteInput.required = data.saved;
            byId('app-note-optional').textContent = data.saved ? '(bổ sung)' : '(không bắt buộc)';
            submit.textContent = data.saved ? 'Thêm ghi chú' : 'Lưu ứng viên';
            submit.disabled = false;
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
            byId('app-bookmark-name').textContent = button.dataset.name;
            bookmark.showModal();
            loadBookmark();
        });
    });
    retry.addEventListener('click', loadBookmark);
    bookmark.addEventListener('cancel', (event) => { if (saving) event.preventDefault(); });
    bookmark.addEventListener('close', () => bookmarkRequest?.abort());
    noteInput.addEventListener('input', () => noteInput.setCustomValidity(''));

    form.addEventListener('submit', async (event) => {
        event.preventDefault();
        if (saving || submit.disabled) return;
        if (noteInput.required && !noteInput.value.trim()) {
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
        errorMessage.textContent = '';
        bookmark.querySelectorAll('[data-app-close]').forEach((button) => { button.disabled = true; });
        try {
            const data = await readJson(await fetch(form.action, {
                method: 'POST', body: new FormData(form), headers: {Accept: 'application/json'},
                signal: AbortSignal.timeout(30000)
            }));
            reflectSaved(data);
            byId('app-feedback').textContent = `${bookmarkSource.dataset.name}: ${noteInput.value.trim() ? 'đã lưu ghi chú chung.' : 'đã lưu vào kho tiềm năng.'}`;
            bookmark.close();
        } catch (error) {
            errorMessage.textContent = `${error.message} Nếu kết nối bị ngắt khi lưu, hãy tải lại ghi chú để kiểm tra trước khi gửi lại.`;
            retry.hidden = false;
        } finally {
            saving = false;
            noteInput.readOnly = false;
            submit.disabled = false;
            submit.textContent = label;
            bookmark.querySelectorAll('[data-app-close]').forEach((button) => { button.disabled = false; });
        }
    });
})();
