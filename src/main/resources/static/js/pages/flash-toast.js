/* =============================================================================
   FLASH-TOAST.JS — Tự tắt thông báo sau thao tác (fragment fragments/ui/flash-toast :: flashToast)

   Thời gian 3 giây nằm ở CSS (animation của thanh ngang, css/pages/flash-toast.css).
   JS chỉ chờ thanh chạy hết (animationend) rồi cho khung mờ dần và gỡ khỏi trang,
   nên rê chuột vào (CSS tạm dừng thanh) cũng là tạm dừng đếm ngược.
   ============================================================================= */
(() => {
    'use strict';

    /** Mờ dần rồi gỡ khung; gỡ luôn vùng chứa khi không còn khung nào. */
    function dismiss(toast) {
        if (toast.classList.contains('is-leaving')) return;
        toast.classList.add('is-leaving');
        // Dự phòng: trình duyệt không chạy transition (tắt hiệu ứng) thì vẫn gỡ sau 400ms
        const remove = () => {
            const container = toast.parentElement;
            toast.remove();
            if (container && !container.querySelector('[data-flash-toast]')) {
                container.remove();
            }
        };
        toast.addEventListener('transitionend', remove, { once: true });
        setTimeout(remove, 350);
    }

    function initToasts(root = document) {
        root.querySelectorAll('[data-flash-toast]').forEach(toast => {
            if (toast._toastBound) return;
            toast._toastBound = true;
            toast.querySelector('.flash-toast-progress')
                ?.addEventListener('animationend', () => dismiss(toast));
            toast.querySelector('[data-flash-close]')
                ?.addEventListener('click', () => dismiss(toast));
        });
    }

    initToasts();

    // Hỗ trợ hiển thị pendingFlashToast từ sessionStorage (sau khi reload / chuyển trang)
    try {
        const pending = sessionStorage.getItem('pendingFlashToast');
        if (pending) {
            sessionStorage.removeItem('pendingFlashToast');
            const data = JSON.parse(pending);
            if (data && data.message && typeof window.showFlashToast === 'function') {
                window.showFlashToast(data.message, data.type || 'success');
            }
        }
    } catch (e) {
        sessionStorage.removeItem('pendingFlashToast');
    }

    window.initFlashToasts = initToasts;
    window.dismissFlashToast = dismiss;
})();
