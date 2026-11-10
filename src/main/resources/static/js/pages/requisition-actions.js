/* =============================================================================
   REQUISITION-ACTIONS.JS — Thao tác trên danh sách và chi tiết yêu cầu tuyển dụng.
   Menu bảng do pages/row-menu.js quản lý; form tạo/sửa do pages/requisition-form.js quản lý.
   ============================================================================= */
document.addEventListener('DOMContentLoaded', () => {

    // Báo ngay khi người dùng thử nhập vượt quá 120 ký tự ở ô tìm kiếm.
    const requisitionSearch = document.getElementById('requisition-search');
    const requisitionSearchError = document.getElementById('requisition-search-error');
    if (requisitionSearch && requisitionSearchError) {
        requisitionSearch.addEventListener('beforeinput', event => {
            const insertsText = event.inputType?.startsWith('insert') && event.data;
            const selectedLength = requisitionSearch.value.length - (requisitionSearch.selectionEnd - requisitionSearch.selectionStart);
            if (insertsText && selectedLength + event.data.length > 120) {
                requisitionSearchError.textContent = 'Từ khóa tìm kiếm không được vượt quá 120 ký tự.';
                requisitionSearchError.style.display = '';
            }
        });
        requisitionSearch.addEventListener('input', () => {
            if (requisitionSearch.value.length <= 120) {
                requisitionSearchError.style.display = 'none';
            }
        });
        requisitionSearch.addEventListener('paste', event => {
            const pastedText = event.clipboardData?.getData('text') || '';
            const selectedLength = requisitionSearch.value.length - (requisitionSearch.selectionEnd - requisitionSearch.selectionStart);
            if (selectedLength + pastedText.length > 120) {
                requisitionSearchError.textContent = 'Từ khóa tìm kiếm không được vượt quá 120 ký tự.';
                requisitionSearchError.style.display = '';
            }
        });
    }

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

    const decisionButtons = decision ? decision.querySelectorAll('button[name="decision"]') : [];
    const commentCount = document.getElementById('decision-comment-count');
    const commentError = document.getElementById('decision-comment-error');
    const commentLimit = Number(feedback?.dataset.limit) || 1000;

    // Đếm như server: xuống dòng tính 1 ký tự, bỏ khoảng trắng hai đầu
    function commentLength() {
        return feedback.value.replace(/\r\n/g, '\n').trim().length;
    }

    // Cập nhật bộ đếm; quá giới hạn thì báo lỗi đỏ và khóa cả 2 nút Trả lại / Phê duyệt
    function checkCommentLength() {
        if (!feedback) return true;
        const length = commentLength();
        const tooLong = length > commentLimit;
        if (commentCount) commentCount.textContent = `${length}/${commentLimit}`;
        if (commentError) commentError.textContent = tooLong ? `Nội dung quá ${commentLimit} ký tự` : '';
        feedback.classList.toggle('is-too-long', tooLong);
        feedback.setAttribute('aria-invalid', String(tooLong));
        decisionButtons.forEach(button => { button.disabled = tooLong; });
        return !tooLong;
    }
    checkCommentLength();

    // Xóa thông báo lỗi khi người dùng chọn nút hành động hoặc bắt đầu gõ lý do
    decisionButtons.forEach(button => button.addEventListener('click', () => feedback?.setCustomValidity('')));
    feedback?.addEventListener('input', () => {
        feedback.setCustomValidity('');
        checkCommentLength();
    });

    // Bắt buộc Giám đốc phải nhập lý do khi bấm Từ chối (Reject)
    decision?.addEventListener('submit', event => {
        // Chặn gửi khi ý kiến quá dài (phòng trường hợp nút bị mở khóa bằng cách khác, vd Enter)
        if (!checkCommentLength()) {
            event.preventDefault();
            feedback.focus();
            return;
        }
        if (event.submitter?.value === 'reject' && feedback && !feedback.value.trim()) {
            event.preventDefault();
            feedback.setCustomValidity('Vui lòng nêu rõ lý do từ chối để người tạo yêu cầu chỉnh sửa.');
            feedback.reportValidity();
        }
    });

    /* -------------------------------------------------------------------------
     * 3. CHỐNG GỬI FORM LẶP LẠI (DOUBLE-SUBMIT PREVENTION)
     * ------------------------------------------------------------------------- */
    document.querySelectorAll('#delete-form,#decision-form').forEach(target => target.addEventListener('submit', event => {
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

