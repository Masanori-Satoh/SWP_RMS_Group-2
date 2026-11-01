(() => {
    'use strict';
    const headers = () => {
        const result = {Accept: 'application/json'};
        const csrf = document.querySelector('meta[name="_csrf"]')?.content;
        const name = document.querySelector('meta[name="_csrf_header"]')?.content;
        if (csrf) result[name || 'X-CSRF-TOKEN'] = csrf;
        return result;
    };
    const read = async response => {
        if (!response.headers.get('content-type')?.includes('application/json'))
            throw new Error('Phiên làm việc có thể đã hết hạn. Hãy tải lại trang trước khi thử lại.');
        const json = await response.json();
        if (!response.ok) throw new Error(json.message || 'Không thể cập nhật kho tiềm năng. Hãy thử lại.');
        const data = json.data || json;
        if (typeof data.saved !== 'boolean') throw new Error('Chưa xác nhận được trạng thái. Hãy tải lại trang để kiểm tra.');
        return data;
    };
    document.querySelectorAll('[data-potential-toggle]').forEach(button => {
        const label = button.querySelector('[data-potential-toggle-label]');
        const feedback = button.closest('.app-potential-control').querySelector('[data-potential-toggle-feedback]');
        let saving = false;
        const render = saved => {
            button.dataset.saved = String(saved);
            button.setAttribute('aria-pressed', String(saved));
            const text = saved ? 'Bỏ lưu tiềm năng' : 'Lưu tiềm năng';
            label.textContent = text;
            button.setAttribute('aria-label', `${text}: ${button.dataset.name}`);
            button.setAttribute('title', saved ? 'Bỏ ứng viên khỏi kho tiềm năng' : 'Lưu ứng viên vào kho tiềm năng');
        };
        const showError = error => {
            feedback.textContent = error.name === 'TimeoutError'
                ? 'Chưa nhận được kết quả. Hãy tải lại trang để kiểm tra trước khi thử tiếp.' : error.message;
            feedback.hidden = false;
        };
        if (button.dataset.saved !== 'true' && button.dataset.saved !== 'false') {
            button.disabled = true;
            fetch(button.dataset.bookmarkUrl, {headers: headers(), signal: AbortSignal.timeout(30000)})
                .then(read).then(data => {render(data.saved); button.disabled = false;})
                .catch(error => {label.textContent = 'Chưa tải được trạng thái'; showError(error);});
        }
        button.addEventListener('click', async () => {
            if (saving || button.disabled) return;
            const saved = button.dataset.saved === 'true';
            saving = true;
            button.disabled = true;
            button.setAttribute('aria-busy', 'true');
            label.textContent = saved ? 'Đang bỏ lưu…' : 'Đang lưu…';
            feedback.hidden = true;
            try {
                const data = await read(await fetch(button.dataset.bookmarkUrl + (saved ? '/remove' : ''), {
                    method: 'POST', headers: headers(), body: new URLSearchParams({content: ''}),
                    signal: AbortSignal.timeout(30000)
                }));
                if (data.saved === saved) throw new Error('Chưa xác nhận được trạng thái mới. Hãy tải lại trang để kiểm tra.');
                const destination = new URL(window.location.href);
                if (button.dataset.sourceId) destination.searchParams.set('sourceApplicationId', button.dataset.sourceId);
                window.location.replace(destination.href);
            } catch (error) {
                showError(error);
                render(saved);
                saving = false;
                button.disabled = false;
                button.removeAttribute('aria-busy');
            }
        });
    });
})();
