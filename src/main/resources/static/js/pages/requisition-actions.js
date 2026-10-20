/* =============================================================================
   REQUISITION-ACTIONS.JS — Thao tác trên danh sách và chi tiết yêu cầu tuyển dụng.
   Menu bảng do pages/row-menu.js quản lý; form tạo/sửa do pages/requisition-form.js quản lý.
   ============================================================================= */
document.addEventListener('DOMContentLoaded', () => {

    /* -------------------------------------------------------------------------
     * 1. HỘP THOẠI XÁC NHẬN XÓA (DELETE CONFIRMATION DIALOG)
     * ------------------------------------------------------------------------- */
    const dialog = document.getElementById('delete-dialog');
    let deleteTrigger;

    // Lắng nghe sự kiện click vào nút Xóa (trong menu bảng hoặc trên trang)
    document.querySelectorAll('[data-delete-requisition]').forEach(button => button.addEventListener('click', () => {
        const menu = button.closest('details.more-menu');
        deleteTrigger = menu?.querySelector('.more-menu__toggle') || button;
        menu?.removeAttribute('open');

        // Gán tên bản ghi và đường dẫn xóa vào hộp thoại
        const deleteNameEl = document.getElementById('delete-name');
        const deleteFormEl = document.getElementById('delete-form');
        if (deleteNameEl) deleteNameEl.textContent = button.dataset.title;
        if (deleteFormEl) deleteFormEl.action = button.dataset.deleteUrl;

        // Mở dialog dạng modal và focus vào nút Hủy
        if (dialog) {
            dialog.showModal();
            dialog.querySelector('[data-close-dialog]')?.focus();
        }
    }));

    // Đóng dialog khi nhấn nút Hủy hoặc click ra ngoài vùng backdrop
    dialog?.querySelector('[data-close-dialog]')?.addEventListener('click', () => dialog.close());
    dialog?.addEventListener('close', () => deleteTrigger?.focus());
    dialog?.addEventListener('click', event => { if (event.target === dialog) dialog.close(); });

    /* -------------------------------------------------------------------------
     * 2. FORM QUYẾT ĐỊNH CỦA GIÁM ĐỐC (DIRECTOR DECISION REVIEW)
     * ------------------------------------------------------------------------- */
    const decision = document.getElementById('decision-form');
    const feedback = document.getElementById('decision-comment');

    // Xóa thông báo lỗi khi người dùng chọn nút hành động hoặc bắt đầu gõ lý do
    decision?.querySelectorAll('button[name="decision"]').forEach(button => button.addEventListener('click', () => feedback?.setCustomValidity('')));
    feedback?.addEventListener('input', () => feedback.setCustomValidity(''));

    // Bắt buộc Giám đốc phải nhập lý do khi bấm Từ chối (Reject)
    decision?.addEventListener('submit', event => {
        if (event.submitter?.value === 'reject' && feedback && !feedback.value.trim()) {
            event.preventDefault();
            feedback.setCustomValidity('Vui lòng nêu rõ lý do từ chối để người tạo yêu cầu chỉnh sửa.');
            feedback.reportValidity();
        }
    });

    /* -------------------------------------------------------------------------
     * 3. CHỐNG GỬI FORM LẶP LẠI (DOUBLE-SUBMIT PREVENTION)
     * ------------------------------------------------------------------------- */
    document.querySelectorAll('#delete-form,#decision-form,.withdraw-form').forEach(target => target.addEventListener('submit', event => {
        if (event.defaultPrevented) return;
        if (target.dataset.submitting === 'true') {
            event.preventDefault();
            return;
        }
        target.dataset.submitting = 'true';
        target.setAttribute('aria-busy', 'true');
    }));

    // Khôi phục trạng thái nút bấm khi quay lại trang bằng nút Back của trình duyệt
    window.addEventListener('pageshow', () => document.querySelectorAll('[data-submitting]').forEach(target => {
        delete target.dataset.submitting;
        target.removeAttribute('aria-busy');
    }));
});

