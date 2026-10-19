/* =============================================================================
   JOB-POSTING-ACTIONS.JS — Thao tác ghi trên tin tuyển dụng (danh sách + chi tiết)
   Một hộp xác nhận dùng chung cho mọi thao tác ghi trong menu ⋯
   và ở đầu trang chi tiết (Đăng tin, Đăng ngay, Hủy lịch, Mở lại, Tạm dừng, Đóng tin, Xóa bản nháp).
   Nút trong menu mang data-*: url (POST tới đâu), heading, title (tên tin), message, confirm (chữ trên nút),
   data-danger nếu là thao tác nguy hiểm (nút xác nhận màu đỏ).
   Server vẫn kiểm tra lại quyền và trạng thái; JS chỉ hỏi lại người dùng trước khi gửi.
   ============================================================================= */
(() => {
    'use strict';

    const dialog = document.getElementById('jp-confirm');
    const form = document.getElementById('jp-confirm-form');
    if (!dialog || !form || typeof dialog.showModal !== 'function') {
        return;
    }
    const submitButton = document.getElementById('jp-confirm-submit');
    let lastTrigger = null;

    document.querySelectorAll('[data-jp-action]').forEach(button => button.addEventListener('click', () => {
        lastTrigger = button.closest('details.more-menu')?.querySelector('summary') || button;
        // Đóng menu ⋯ trước khi mở hộp thoại
        button.closest('details.more-menu')?.removeAttribute('open');

        form.action = button.dataset.url;
        document.getElementById('jp-confirm-heading').textContent = button.dataset.heading;
        document.getElementById('jp-confirm-title').textContent = button.dataset.title;
        document.getElementById('jp-confirm-message').textContent = button.dataset.message;
        submitButton.textContent = button.dataset.confirm;
        const danger = button.hasAttribute('data-danger');
        submitButton.classList.toggle('btn--danger', danger);
        submitButton.classList.toggle('btn--primary', !danger);

        dialog.showModal();
        dialog.querySelector('[data-jp-cancel]').focus();
    }));

    dialog.querySelector('[data-jp-cancel]').addEventListener('click', () => dialog.close());
    // Đóng hộp thoại thì trả focus về nút ⋯ đã mở nó (người dùng bàn phím không bị lạc)
    dialog.addEventListener('close', () => lastTrigger?.focus());
    // Chặn bấm "Xác nhận" 2 lần
    form.addEventListener('submit', () => {
        submitButton.disabled = true;
    });
})();
