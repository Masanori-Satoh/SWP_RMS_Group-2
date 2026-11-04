/**
 * offer-common.js — Phân hệ Offer Management (SWP391 — Mộc RMS Group 2)
 * Trách nhiệm dùng chung giữa các trang Offer nội bộ (HR List, Form, Detail, Director List):
 * - Đọc CSRF Token từ thẻ meta (getCsrfInfo)
 * - Khử ký tự HTML độc hại chống XSS (escapeOfferHtml)
 * - Hiển thị Flash Toast thông báo phản hồi (showFlashToast, showToast)
 * - Tự động hiển thị pendingFlashToast từ sessionStorage sau khi reload trang
 * - Điều khiển Modal xác nhận phát hành Thư mời nhận việc (confirmSendOffer, executeSendOfferAction)
 */

// =========================================================================
// 1. CSRF TOKEN HELPER
// =========================================================================

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

    function dismissToast() {
        if (toast.classList.contains('is-leaving')) return;
        toast.classList.add('is-leaving');
        const remove = () => {
            toast.remove();
            if (container && !container.querySelector('[data-flash-toast]')) {
                container.remove();
            }
        };
        toast.addEventListener('transitionend', remove, { once: true });
        setTimeout(remove, 350);
    }

    toast.querySelector('.flash-toast-progress')?.addEventListener('animationend', dismissToast);
    toast.querySelector('[data-flash-close]')?.addEventListener('click', dismissToast);
}

function checkOfferPendingFlashToast() {
    try {
        const pending = sessionStorage.getItem('pendingFlashToast');
        if (pending) {
            sessionStorage.removeItem('pendingFlashToast');
            const data = JSON.parse(pending);
            if (data && data.message) {
                showFlashToast(data.message, data.type || 'success');
            }
        }
    } catch (e) {
        sessionStorage.removeItem('pendingFlashToast');
    }
}

if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', checkOfferPendingFlashToast);
} else {
    checkOfferPendingFlashToast();
}

function showToast(message, type = 'info') {
    showFlashToast(message, type === 'danger' ? 'error' : type);
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
    } else if (confirm('Bạn có chắc chắn muốn phát hành Thư mời nhận việc tới ứng viên?')) {
        executeSendOfferAction();
    }
}

function executeSendOfferAction() {
    const offerId = pendingSendOfferId;
    if (!offerId) return;

    const dialog = document.getElementById('sendOfferConfirmDialog');
    if (dialog) dialog.close();

    const btnModal = document.getElementById('btnConfirmSendOfferModal');
    if (btnModal) {
        btnModal.disabled = true;
        btnModal.innerHTML = '<span class="ofw-spinner-sm" aria-hidden="true"></span> Đang gửi...';
    }

    showFlashToast('Đang phát hành và gửi email Thư mời nhận việc tới ứng viên...', 'info');

    const csrf = getCsrfInfo();
    const headers = { 'Accept': 'application/json' };
    if (csrf.token) {
        headers[csrf.header] = csrf.token;
    }

    fetch(`/offers/${offerId}/send`, {
        method: 'POST',
        headers: headers
    })
        .then(async res => {
            const isJson = res.headers.get('content-type')?.includes('application/json');
            const data = isJson ? await res.json() : null;
            if (!res.ok) {
                throw new Error(data?.message || `Lỗi phát hành thư mời nhận việc (${res.status})`);
            }
            return data;
        })
        .then(res => {
            if (res && res.success) {
                try {
                    sessionStorage.setItem('pendingFlashToast', JSON.stringify({
                        message: res.message || 'Phát hành Thư mời làm việc thành công!',
                        type: 'success'
                    }));
                } catch (e) { }
                window.location.reload();
            } else {
                showFlashToast(res?.message || 'Lỗi phát hành thư mời nhận việc.', 'error');
            }
        })
        .catch(err => {
            showFlashToast(err.message || 'Không thể phát hành thư mời nhận việc.', 'error');
        })
        .finally(() => {
            if (btnModal) {
                btnModal.disabled = false;
                btnModal.innerHTML = '<svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true" class="ofw-btn-icon-inline"><line x1="22" y1="2" x2="11" y2="13"/><polygon points="22 2 15 22 11 13 2 9 22 2"/></svg> Xác nhận phát hành';
            }
        });
}

// =========================================================================
// 5. WINDOW GLOBAL EXPORTS
// =========================================================================
window.getCsrfInfo = getCsrfInfo;
window.escapeOfferHtml = escapeOfferHtml;
window.showFlashToast = showFlashToast;
window.showToast = showToast;
window.confirmSendOffer = confirmSendOffer;
window.executeSendOfferAction = executeSendOfferAction;
