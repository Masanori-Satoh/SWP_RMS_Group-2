/*
 * Trang "Thư mời nhận việc": hỏi xác nhận trước khi nhận hoặc từ chối offer.
 * - Mỗi nút là một form POST thường ([data-offer-form="accept|decline"]); không có JS thì form gửi thẳng, server vẫn kiểm tra đủ.
 * - Bấm nút → mở hộp thoại với tên vị trí và nội dung theo hành động; "Xem lại" đóng hộp thoại.
 * - Khóa nút xác nhận khi đang gửi để tránh gửi hai lần.
 */
(function () {
  'use strict';

  var dialog = document.getElementById('cof-dialog');
  if (!dialog || typeof dialog.showModal !== 'function') {
    return;
  }

  var titleSlots = dialog.querySelectorAll('[data-offer-title]');
  var copies = dialog.querySelectorAll('[data-offer-copy]');
  var cancelButton = dialog.querySelector('[data-offer-cancel]');
  var confirmButton = dialog.querySelector('[data-offer-confirm]');
  var pendingForm = null;
  var trigger = null;

  function showCopy(action) {
    copies.forEach(function (node) {
      node.hidden = node.dataset.offerCopy !== action;
    });
    // Từ chối là thao tác không hoàn tác: nút xác nhận dùng kiểu nguy hiểm.
    confirmButton.classList.toggle('btn--danger', action === 'decline');
    confirmButton.classList.toggle('btn--primary', action !== 'decline');
  }

  document.querySelectorAll('form[data-offer-form]').forEach(function (form) {
    form.addEventListener('submit', function (event) {
      event.preventDefault();
      pendingForm = form;
      trigger = form.querySelector('button[type="submit"]');
      showCopy(form.dataset.offerForm);
      titleSlots.forEach(function (slot) {
        slot.textContent = form.dataset.postingTitle || '';
      });
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
