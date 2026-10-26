/*
 * Chi tiết hồ sơ (/applications/{id}): hỏi xác nhận trước khi "Không đạt" (Q2.1).
 * - Form duyệt là form POST thường (#app-review-form); không có JS thì nút "Không đạt" gửi thẳng, server vẫn kiểm đủ.
 * - Bấm "Không đạt" → mở hộp thoại; "Hủy" đóng hộp, không gửi gì; "Xác nhận loại ứng viên" là nút submit của form
 *   qua thuộc tính form= nên gửi decision=Fail cùng nhận xét đã nhập.
 * - Chặn gửi hai lần bằng cờ: không khóa (disabled) nút trong lúc submit, vì nút bị khóa sẽ mất decision=Fail khỏi form.
 */
(function () {
  'use strict';

  var dialog = document.getElementById('app-reject-dialog');
  var form = document.getElementById('app-review-form');
  if (!dialog || !form || typeof dialog.showModal !== 'function') {
    return;
  }

  var failButton = form.querySelector('[data-review-fail]');
  var cancelButton = dialog.querySelector('[data-review-cancel]');
  var submitting = false;

  failButton.addEventListener('click', function (event) {
    event.preventDefault();
    dialog.showModal();
    cancelButton.focus();
  });

  cancelButton.addEventListener('click', function () {
    dialog.close();
  });

  form.addEventListener('submit', function (event) {
    if (submitting) {
      event.preventDefault();
      return;
    }
    submitting = true;
  });

  dialog.addEventListener('close', function () {
    if (!submitting) {
      failButton.focus();
    }
  });

  // Quay lại trang bằng nút Back: trình duyệt có thể giữ trạng thái "đang gửi".
  window.addEventListener('pageshow', function () {
    submitting = false;
  });
})();
