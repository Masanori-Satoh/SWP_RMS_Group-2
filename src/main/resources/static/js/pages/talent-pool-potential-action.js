/* Small adapter for the existing atomic bookmark/shared-note API. No interview or application mutations. */
(() => {
    'use strict';
    document.querySelectorAll('[data-tp-potential]').forEach(root => {
        const get = selector => root.querySelector(selector);
        const open = get('[data-tp-open]'), openLabel = get('[data-tp-open-label]');
        const saved = get('[data-tp-saved]'), notesButton = get('[data-tp-notes]');
        const retry = get('[data-tp-retry]'), panel = get('[data-tp-panel]');
        const feedback = get('[data-tp-feedback]'), errorBox = get('[data-tp-error]');
        const form = get('[data-tp-form]'), content = get('[data-tp-content]');
        const submit = get('[data-tp-submit]'), close = get('[data-tp-close]');
        let state = null, busy = false, needsReload = false;

        function controls() {
            open.disabled = notesButton.disabled = busy || !state || needsReload;
            retry.disabled = close.disabled = busy;
            submit.disabled = busy || !state || needsReload || (state.saved && !content.value.trim());
            content.readOnly = busy;
            root.setAttribute('aria-busy', String(busy));
        }
        function render(data) {
            state = data;
            open.hidden = data.saved;
            saved.hidden = notesButton.hidden = !data.saved;
            openLabel.textContent = 'Lưu ứng viên tiềm năng';
            content.required = data.saved;
            get('[data-tp-optional]').hidden = data.saved;
            submit.textContent = data.saved ? 'Thêm ghi chú' : 'Lưu ứng viên';
            const list = get('[data-tp-note-list]');
            list.replaceChildren();
            data.notes.forEach(note => {
                const item = document.createElement('li'); item.className = 'tp-potential__note';
                const meta = document.createElement('div'); meta.className = 'tp-potential__note-meta';
                const author = document.createElement('strong'); author.textContent = note.author;
                const time = document.createElement('time');
                if (note.createdAt) {
                    const date = new Date(note.createdAt);
                    if (!Number.isNaN(date.getTime())) { time.dateTime = note.createdAt; time.textContent = date.toLocaleString('vi-VN', {day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit'}); }
                }
                const text = document.createElement('p'); text.textContent = note.content;
                meta.append(author, time); item.append(meta, text); list.append(item);
            });
            get('[data-tp-empty]').hidden = data.notes.length > 0;
        }
        async function read(response) {
            const body = response.headers.get('content-type')?.includes('application/json') ? await response.json() : null;
            if (!response.ok) throw new Error(body?.message || (response.status === 403
                ? 'Bạn không có quyền lưu hồ sơ này hoặc phiên đăng nhập đã hết.' : 'Không tải được dữ liệu. Hãy thử lại.'));
            if (!body || !Number.isInteger(body.candidateId) || typeof body.saved !== 'boolean' || !Array.isArray(body.notes))
                throw new Error('Chưa xác minh được trạng thái từ máy chủ. Hãy tải lại trạng thái.');
            return body;
        }
        async function load() {
            if (busy) return;
            busy = true; retry.hidden = true; feedback.textContent = 'Đang tải trạng thái và ghi chú…'; errorBox.textContent = '';
            controls();
            try {
                const data = await read(await fetch(form.action, {cache: 'no-store', headers: {Accept: 'application/json'}, signal: AbortSignal.timeout(20000)}));
                needsReload = false; render(data); feedback.textContent = '';
            } catch (error) {
                needsReload = true; errorBox.textContent = error.message; feedback.textContent = ''; retry.hidden = false;
                if (!state) openLabel.textContent = 'Chưa tải được trạng thái';
            } finally { busy = false; controls(); }
        }
        function expand() {
            if (busy || !state || needsReload) return;
            panel.hidden = false;
            open.setAttribute('aria-expanded', 'true'); notesButton.setAttribute('aria-expanded', 'true');
            load();
        }
        open.addEventListener('click', expand); notesButton.addEventListener('click', expand);
        retry.addEventListener('click', load);
        close.addEventListener('click', () => {
            if (busy) return;
            panel.hidden = true;
            open.setAttribute('aria-expanded', 'false'); notesButton.setAttribute('aria-expanded', 'false');
            (state?.saved ? notesButton : open).focus();
        });
        content.addEventListener('input', () => { content.setCustomValidity(''); controls(); });
        form.addEventListener('submit', async event => {
            event.preventDefault();
            if (busy || !state || needsReload) return;
            if (state.saved && !content.value.trim()) { content.setCustomValidity('Vui lòng nhập ghi chú.'); content.reportValidity(); return; }
            if (!form.reportValidity()) return;
            busy = true; retry.hidden = true; errorBox.textContent = ''; feedback.textContent = 'Đang lưu…'; controls();
            submit.textContent = 'Đang lưu…';
            try {
                const token = document.querySelector('meta[name="_csrf"]')?.content;
                const header = document.querySelector('meta[name="_csrf_header"]')?.content || 'X-CSRF-TOKEN';
                if (!token) throw new Error('Thiếu thông tin phiên bảo mật. Hãy tải lại trang.');
                const data = await read(await fetch(form.action, {method: 'POST', body: new FormData(form),
                    headers: {Accept: 'application/json', [header]: token}, signal: AbortSignal.timeout(30000)}));
                if (!data.saved) throw new Error('Máy chủ chưa xác nhận lưu ứng viên.');
                const addedNote = Boolean(content.value.trim());
                content.value = ''; needsReload = false; render(data);
                feedback.textContent = addedNote ? 'Đã lưu ghi chú chung.' : 'Đã lưu ứng viên vào kho tiềm năng.';
                content.focus();
            } catch (error) {
                needsReload = true; retry.hidden = false; feedback.textContent = '';
                errorBox.textContent = `${error.message} Hãy tải lại trạng thái trước khi gửi lại để tránh ghi chú trùng.`;
                submit.textContent = state?.saved ? 'Thêm ghi chú' : 'Lưu ứng viên';
            } finally { busy = false; controls(); }
        });
        load();
    });
})();
