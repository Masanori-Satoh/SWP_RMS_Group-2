/*
 * Trang "Hồ sơ đã ứng tuyển": hỏi xác nhận trước khi rút đơn.
 * - Mỗi dòng là một form POST thường ([data-withdraw-form]); không có JS thì form gửi thẳng, server vẫn kiểm tra đủ.
 * - Bấm "Rút đơn" → mở hộp thoại với tên vị trí; "Rút hồ sơ" mới gửi form, "Giữ hồ sơ" đóng hộp thoại.
 * - Khóa nút xác nhận khi đang gửi để tránh gửi hai lần.
 */
(function () {
  'use strict';

  var dialog = document.getElementById('myapp-withdraw-dialog');
  if (!dialog || typeof dialog.showModal !== 'function') {
    return;
  }

  var titleSlot = dialog.querySelector('[data-withdraw-title]');
  var cancelButton = dialog.querySelector('[data-withdraw-cancel]');
  var confirmButton = dialog.querySelector('[data-withdraw-confirm]');
  var pendingForm = null;
  var trigger = null;

  document.querySelectorAll('form[data-withdraw-form]').forEach(function (form) {
    form.addEventListener('submit', function (event) {
      event.preventDefault();
      pendingForm = form;
      trigger = form.querySelector('button[type="submit"]');
      titleSlot.textContent = form.dataset.postingTitle || '';
      confirmButton.disabled = false;
      dialog.showModal();
      cancelButton.focus();
    });
  });

  cancelButton.addEventListener('click', function () {
    dialog.close();
  });

  confirmButton.addEventListener('click', function () {
    if (!pendingForm || confirmButton.disabled) {
      return;
    }
    confirmButton.disabled = true;
    // form.submit() không phát lại sự kiện submit nên không mở lại hộp thoại.
    pendingForm.submit();
  });

  dialog.addEventListener('close', function () {
    if (trigger && !confirmButton.disabled) {
      trigger.focus();
    }
    pendingForm = null;
  });

  // Quay lại trang bằng nút Back: trình duyệt có thể giữ trạng thái đã khóa.
  window.addEventListener('pageshow', function () {
    confirmButton.disabled = false;
  });
})();
