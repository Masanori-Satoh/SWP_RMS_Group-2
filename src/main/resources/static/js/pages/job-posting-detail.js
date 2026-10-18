/* =============================================================================
   JOB-POSTING-DETAIL.JS — Chi tiết tin tuyển dụng (job-postings/detail.html)
   Tab "Nội dung tin" / "Ứng viên": bấm tab hoặc dùng phím ← → để chuyển.
   Bản đồ thu gọn: chỉ tải Google Maps khi người dùng bấm "Xem bản đồ".
   Thao tác ghi (đăng, tạm dừng, đóng...) nằm ở pages/job-posting-actions.js.
   ============================================================================= */
(() => {
    'use strict';

    // Bản đồ: iframe để trống src; lần đầu bung khung mới gán src (lấy từ data-src) để tải bản đồ
    const mapToggle = document.querySelector('.jpd-map-toggle');
    mapToggle?.addEventListener('toggle', () => {
        const frame = mapToggle.querySelector('iframe');
        if (mapToggle.open && !frame.src) {
            frame.src = frame.dataset.src;
        }
    });

    const container = document.querySelector('[data-tabs]');
    if (!container) {
        return;
    }
    const tabs = [...container.querySelectorAll('[role="tab"]')];

    function select(tab) {
        tabs.forEach(other => {
            const active = other === tab;
            other.setAttribute('aria-selected', String(active));
            other.tabIndex = active ? 0 : -1;
            container.querySelector(`[data-panel="${other.dataset.tab}"]`).hidden = !active;
        });
    }

    tabs.forEach((tab, index) => {
        tab.addEventListener('click', () => select(tab));
        tab.addEventListener('keydown', event => {
            if (event.key !== 'ArrowRight' && event.key !== 'ArrowLeft') return;
            const next = tabs[(index + (event.key === 'ArrowRight' ? 1 : tabs.length - 1)) % tabs.length];
            select(next);
            next.focus();
        });
    });

    // Mở thẳng tab Ứng viên khi URL có #ung-vien (ví dụ link từ thông báo)
    if (location.hash === '#ung-vien') {
        select(tabs.find(tab => tab.dataset.tab === 'candidates'));
    }
})();
