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

    document.querySelectorAll('[data-flash-toast]').forEach(toast => {
        toast.querySelector('.flash-toast-progress')
            ?.addEventListener('animationend', () => dismiss(toast));
        toast.querySelector('[data-flash-close]')
            ?.addEventListener('click', () => dismiss(toast));
    });
})();
