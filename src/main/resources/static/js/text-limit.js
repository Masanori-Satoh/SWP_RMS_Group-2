/* =============================================================================
   TEXT-LIMIT.JS — Báo lỗi đỏ "Nội dung quá N ký tự" cho ô nhập chữ có maxlength

   - maxlength="N" làm trình duyệt KHÓA ô: đủ N ký tự thì không nhận thêm (dán vào thì bị cắt bớt).
   - File này báo cho người dùng biết: khi cố gõ / dán vượt N ký tự thì hiện dòng chữ đỏ
     ngay dưới ô và viền ô đỏ. Xóa bớt chữ thì dòng lỗi tự mất.
   - Server vẫn luôn kiểm tra lại độ dài (Validator), file này chỉ giúp người dùng thấy lỗi sớm.
   ============================================================================= */
(() => {
    'use strict';

    const FIELDS = 'input[maxlength], textarea[maxlength]';

    /** Hiện dòng chữ đỏ dưới ô (hoặc dưới bộ đếm "0/2000" nếu ô có bộ đếm). */
    function showError(field) {
        field.classList.add('is-too-long');
        if (field.textLimitError) {
            return;
        }
        const slot = document.createElement('small');
        slot.className = 'form-error';
        slot.setAttribute('role', 'alert');
        slot.textContent = `Nội dung quá ${field.maxLength} ký tự`;
        const counter = field.id ? document.getElementById(field.id + '-count') : null;
        (counter || field).after(slot);
        field.textLimitError = slot;
    }

    function clearError(field) {
        field.classList.remove('is-too-long');
        field.textLimitError?.remove();
        field.textLimitError = null;
    }

    /** Thêm addedLength ký tự vào ô (thay cho phần đang bôi đen) thì có vượt maxlength không? */
    function wouldExceed(field, addedLength) {
        const selected = (field.selectionEnd ?? 0) - (field.selectionStart ?? 0);
        return field.value.length - selected + addedLength > field.maxLength;
    }

    // Gõ phím (kể cả Enter xuống dòng trong textarea)
    document.addEventListener('beforeinput', event => {
        const field = event.target;
        if (!field.matches?.(FIELDS) || !event.inputType.startsWith('insert') || event.inputType === 'insertFromPaste') {
            return;
        }
        const addedLength = event.data ? event.data.length : 1;
        if (wouldExceed(field, addedLength)) {
            showError(field);
        }
    });

    // Dán chữ: trình duyệt tự cắt bớt phần thừa, ở đây chỉ báo cho người dùng biết
    document.addEventListener('paste', event => {
        const field = event.target;
        if (!field.matches?.(FIELDS)) {
            return;
        }
        const pasted = event.clipboardData?.getData('text') || '';
        if (wouldExceed(field, pasted.length)) {
            showError(field);
        }
    });

    // Xóa bớt chữ, còn dưới giới hạn thì bỏ báo lỗi
    document.addEventListener('input', event => {
        const field = event.target;
        if (field.matches?.(FIELDS) && field.value.length < field.maxLength) {
            clearError(field);
        }
    });
})();
