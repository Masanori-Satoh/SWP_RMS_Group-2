/**
 * offer-common.js — Phân hệ Offer Management (SWP391 — Mộc RMS Group 2)
 * Trách nhiệm dùng chung giữa các trang Offer nội bộ (HR List, Detail, Director List):
 * - Đọc CSRF Token từ thẻ meta (getCsrfInfo)
 * - Khử ký tự HTML độc hại chống XSS (escapeOfferHtml)
 * - Hiển thị Flash Toast thông báo phản hồi (showFlashToast)
 * - Tự động hiển thị toast của Offer từ sessionStorage sau khi reload trang
 * - Điều khiển Modal xác nhận phát hành Thư mời nhận việc (confirmSendOffer, executeSendOfferAction)
 */

// =========================================================================
// 1. CSRF TOKEN HELPER
// =========================================================================

/** Gắn sự kiện theo data-* trong vùng Offer; không thực thi chuỗi JavaScript từ HTML. */
function bindOfferEvents(handlers) {
    for (const type of ['click', 'change', 'input']) {
        document.addEventListener(type, event => {
            const element = event.target.closest?.(`[data-offer-${type}]`);
            const handler = element && handlers[element.dataset['offer' + type[0].toUpperCase() + type.slice(1)]];
            if (handler && !element.disabled && element.closest('.ofw-page')) handler(element, event);
        });
    }
}

bindOfferEvents({
    'close-dialog': el => el.closest('dialog').close(),
    'open-send': el => confirmSendOffer(el.dataset.offerId),
    'open-delete': el => confirmDeleteOffer(el.dataset.offerId),
    send: () => executeSendOfferAction(),
    delete: el => executeDeleteOffer(el),
    'filter-status': el => { el.form.elements.status.value = el.dataset.offerStatus; el.form.requestSubmit(); },
    'clear-search': el => { el.form.elements.search.value = ''; },
    filter: el => el.form.requestSubmit()
});

/**
 * Lấy CSRF token và header name từ thẻ meta tag
 */
function getCsrfInfo() {
    const token = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
    const header = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content') || 'X-CSRF-TOKEN';
    return { token, header };
}

// =========================================================================
// 2. ESCAPE HTML HELPER
// =========================================================================

/**
 * Gửi thao tác JSON của Offer: tự gắn CSRF, đọc ApiResponse và ném lỗi nếu HTTP hoặc success không hợp lệ.
 * Ví dụ: offerRequest(url, { body: { comments } }); lỗi được hàm gọi hiển thị bằng toast.
 * Không dùng cho tải Excel vì phản hồi Excel là Blob, không phải JSON.
 */
async function offerRequest(url, { method = 'POST', body } = {}) {
    const csrf = getCsrfInfo();
    const headers = { Accept: 'application/json' };
    if (csrf.token) headers[csrf.header] = csrf.token;
    if (body !== undefined) headers['Content-Type'] = 'application/json';
    let response;
    try {
        response = await fetch(url, { method, headers,
            ...(body !== undefined ? { body: JSON.stringify(body) } : {}) });
    } catch {
        throw new Error('Không thể kết nối đến máy chủ. Vui lòng kiểm tra lại kết nối mạng.');
    }
    let data = null;
    if (response.headers.get('content-type')?.includes('application/json')) {
        try { data = await response.json(); } catch { /* Invalid responses are handled below. */ }
    }
    if (!response.ok || !data?.success) {
        const fallback = response.status === 403
            ? 'Bạn không có quyền thực hiện thao tác này hoặc phiên làm việc đã hết hạn.'
            : 'Thao tác không thành công. Vui lòng thử lại. (' + response.status + ')';
        throw new Error(data?.message || fallback);
    }
    return data;
}

function escapeOfferHtml(str) {
    if (str == null) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}

// =========================================================================
// 3. FLASH TOAST NOTIFICATION HELPERS
// =========================================================================

const OFFER_PENDING_TOAST_KEY = 'offer.pendingFlashToast';

/**
 * Lưu thông báo trong tab trước khi reload/redirect; trang đến đọc rồi xóa để thông báo chỉ hiện một lần.
 * SessionStorage chỉ phục vụ UI, không phải bằng chứng thao tác đã được lưu ở server.
 */
function queueOfferToast(message, type = 'success') {
    try {
        sessionStorage.setItem(OFFER_PENDING_TOAST_KEY, JSON.stringify({ message, type }));
    } catch (e) { }
}

const TOAST_ICONS = {
    circle: '<svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true" class="flash-toast-icon"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>',
    warning: '<svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true" class="flash-toast-icon"><path d="m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3Z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>',
    success: '<svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true" class="flash-toast-icon"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>'
};

/**
 * Chuẩn hóa Flash Toast hiển thị thông báo theo hệ thống (flash-toast.css / UI Catalog)
 * Thay thế hoàn toàn alert() và các popup SweetAlert chặn toàn màn hình.
 * @param {string} message
 * @param {'success'|'error'|'warning'|'info'} type
 */
function showFlashToast(message, type = 'success') {
    let container = document.querySelector('.flash-toasts');
    if (!container) {
        container = document.createElement('div');
        container.className = 'flash-toasts';
        document.body.appendChild(container);
    }

    const isError = type === 'error' || type === 'danger';
    const isWarning = type === 'warning';
    const isInfo = type === 'info';
    const toast = document.createElement('div');
    toast.className = `flash-toast ${isError ? 'flash-toast--error' : (isWarning ? 'flash-toast--warning' : (isInfo ? 'flash-toast--info' : ''))}`;
    toast.setAttribute('role', isError ? 'alert' : 'status');
    toast.setAttribute('data-flash-toast', '');

    const iconSvg = (isError || isInfo) ? TOAST_ICONS.circle : (isWarning ? TOAST_ICONS.warning : TOAST_ICONS.success);

    toast.innerHTML = `
        <span class="flash-toast-progress" aria-hidden="true"></span>
        ${iconSvg}
        <p class="flash-toast-text">${escapeOfferHtml(message)}</p>
        <button type="button" class="flash-toast-close" aria-label="Đóng thông báo" data-flash-close>&times;</button>
    `;

    container.appendChild(toast);
    window.initFlashToasts(container);
}

function checkOfferPendingFlashToast() {
    const warning = document.getElementById('main')?.dataset.offerWarning;
    if (warning) showFlashToast(warning, 'warning');
    try {
        const pending = sessionStorage.getItem(OFFER_PENDING_TOAST_KEY);
        if (pending) {
            sessionStorage.removeItem(OFFER_PENDING_TOAST_KEY);
            const data = JSON.parse(pending);
            if (data && data.message) {
                showFlashToast(data.message, data.type || 'success');
            }
        }
    } catch (e) {
        sessionStorage.removeItem(OFFER_PENDING_TOAST_KEY);
    }
}

if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', checkOfferPendingFlashToast);
} else {
    checkOfferPendingFlashToast();
}

// =========================================================================
// 4. THAO TÁC PHÁT HÀNH OFFER (Send Offer Confirmation Dialog & Action)
// =========================================================================

let pendingSendOfferId = null;

function confirmSendOffer(offerId) {
    if (!offerId) return;
    pendingSendOfferId = offerId;

    const dialog = document.getElementById('sendOfferConfirmDialog');
    if (dialog && typeof dialog.showModal === 'function') {
        dialog.showModal();
    } else if (confirm('Bạn có chắc chắn muốn gửi thư mời nhận việc tới ứng viên?')) {
        executeSendOfferAction();
    }
}

/**
 * Sau xác nhận: khóa nút → gọi POST /offers/{id}/send → lưu toast → tải lại dữ liệu từ server.
 * Offer đã phát hành nhưng email chưa gửi tới ứng viên thì hiện warning, không báo toàn bộ nghiệp vụ thất bại.
 */
function executeSendOfferAction() {
    const offerId = pendingSendOfferId;
    if (!offerId) return;

    const dialog = document.getElementById('sendOfferConfirmDialog');
    if (dialog) dialog.close();

    const btnModal = document.getElementById('btnConfirmSendOfferModal');
    const originalButtonHtml = btnModal?.innerHTML;
    if (btnModal) {
        btnModal.disabled = true;
        btnModal.innerHTML = '<span class="ofw-spinner-sm" aria-hidden="true"></span> Đang gửi...';
    }

    showFlashToast('Đang phát hành thư mời nhận việc...', 'info');

    offerRequest(`/offers/${offerId}/send`)
        .then(res => {
            queueOfferToast(res.message || 'Đã phát hành thư mời nhận việc.',
                res.data?.emailStatus === 'SENT' ? 'success' : 'warning');
            window.location.reload();
        })
        .catch(err => {
            showFlashToast(err.message || 'Không thể phát hành thư mời nhận việc.', 'error');
        })
        .finally(() => {
            if (btnModal) {
                btnModal.disabled = false;
                btnModal.innerHTML = originalButtonHtml;
            }
        });
}

/**
 * Xác nhận và gọi API xóa bản nháp đề xuất (Chỉ áp dụng cho trạng thái Draft)
 */
function confirmDeleteOffer(offerId) {
    const dialog = document.getElementById('deleteOfferDialog');
    if (!offerId || !dialog || dialog.open) return;
    dialog.dataset.offerId = offerId;
    dialog.showModal();
}

/**
 * Xóa nháp bằng DELETE sau khi người dùng xác nhận; server mới quyết định bản ghi có được xóa hay không.
 * Khóa nút trong lúc chờ, mở lại khi hoàn tất; chỉ reload khi ApiResponse báo thành công.
 */
function executeDeleteOffer(button) {
    const dialog = button.closest('dialog');
    const offerId = dialog.dataset.offerId;
    if (!offerId || button.disabled) return;
    button.disabled = true;

    offerRequest(`/offers/${offerId}`, { method: 'DELETE' })
        .then(res => {
            queueOfferToast(res.message || 'Đã xóa bản nháp thư mời thành công.');
            if (dialog.dataset.successUrl) window.location.href = dialog.dataset.successUrl;
            else window.location.reload();
        })
        .catch(err => {
            if (typeof showFlashToast === 'function') {
                showFlashToast(err.message, 'error');
            }
        })
        .finally(() => {
            button.disabled = false;
        });
}
