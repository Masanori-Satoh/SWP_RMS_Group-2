/*
 * Trang "Đơn của tôi" (danh sách + chi tiết đơn).
 * 1. Xem lại CV: link [data-cv-open] mở CV trong popup #myapp-cv-dialog (iframe chỉ nạp khi mở, gỡ khi đóng để
 *    lần sau không hiện CV cũ). Không có JS hoặc trình duyệt không có <dialog> thì link mở CV ở tab mới như thường.
 * 2. Rút đơn (chỉ ở trang chi tiết): form POST thường ([data-withdraw-form]); không có JS thì form gửi thẳng, server
 *    vẫn kiểm tra đủ. Bấm "Rút đơn" → hộp thoại với tên vị trí; "Rút hồ sơ" mới gửi form, "Giữ hồ sơ" đóng hộp thoại.
 *    Khóa nút xác nhận khi đang gửi để tránh gửi hai lần.
 */
(function () {
  'use strict';

  // ---- 1. Popup xem lại CV ----
  var cvDialog = document.getElementById('myapp-cv-dialog');
  if (cvDialog && typeof cvDialog.showModal === 'function') {
    var cvFrame = cvDialog.querySelector('[data-cv-frame]');
    var cvTitle = cvDialog.querySelector('[data-cv-title]');
    var cvClose = cvDialog.querySelector('[data-cv-close]');
    var cvTrigger = null;

    document.querySelectorAll('a[data-cv-open]').forEach(function (link) {
      link.addEventListener('click', function (event) {
        event.preventDefault();
        cvTrigger = link;
        cvTitle.textContent = link.dataset.cvTitle || 'CV đã nộp';
        cvFrame.title = cvTitle.textContent;
        cvFrame.src = link.getAttribute('href') + '#view=FitH&navpanes=0';
        cvDialog.showModal();
        cvClose.focus();
      });
    });

    cvClose.addEventListener('click', function () {
      cvDialog.close();
    });

    // Bấm ra ngoài khung (vùng nền mờ) cũng đóng.
    cvDialog.addEventListener('click', function (event) {
      if (event.target === cvDialog) {
        cvDialog.close();
      }
    });

    cvDialog.addEventListener('close', function () {
      cvFrame.removeAttribute('src');
      if (cvTrigger) {
        cvTrigger.focus();
      }
    });
  }

  // ---- 2. Xác nhận rút đơn ----
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
