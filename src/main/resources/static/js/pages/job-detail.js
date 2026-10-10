/*
 * Trang chi tiết tin tuyển dụng: hộp thoại ứng tuyển.
 * - Mở khi bấm nút [data-dialog-open], khi URL có #apply (quay lại sau đăng nhập)
 *   hoặc khi server render lại sau lỗi (data-open-on-load="true").
 * - Kiểm tra sớm đuôi .pdf và dung lượng; server vẫn là nơi kiểm tra chính (CvFileValidator).
 * - Khóa nút "Nộp hồ sơ" khi đang gửi để tránh gửi hai lần.
 */
(function () {
  'use strict';

  var MAX_CV_BYTES = 5 * 1024 * 1024;
  var dialog = document.getElementById('job-apply-dialog');
  if (!dialog || typeof dialog.showModal !== 'function') {
    return;
  }

  var form = dialog.querySelector('form');
  var fileInput = dialog.querySelector('#cvFile');
  var clientError = dialog.querySelector('[data-cv-client-error]');
  var submitButton = dialog.querySelector('[data-apply-submit]');
  var submitLabel = submitButton ? submitButton.textContent : '';

  function open() {
    if (!dialog.open) {
      dialog.showModal();
    }
  }

  function clearApplyHash() {
    if (window.location.hash === '#apply') {
      history.replaceState(null, '', window.location.pathname + window.location.search);
    }
  }

  function validateFile() {
    var file = fileInput && fileInput.files[0];
    if (!file) {
      return 'Vui lòng chọn file CV.';
    }
    if (!file.name.toLowerCase().endsWith('.pdf')) {
      return 'Chỉ nhận file PDF.';
    }
    if (file.size > MAX_CV_BYTES) {
      return 'File CV tối đa 5 MB.';
    }
    return '';
  }

  function showClientError(message) {
    clientError.textContent = message;
    clientError.hidden = !message;
    fileInput.setAttribute('aria-invalid', message ? 'true' : 'false');
  }

  function resetSubmitButton() {
    if (submitButton) {
      submitButton.disabled = false;
      submitButton.textContent = submitLabel;
    }
  }

  document.querySelectorAll('[data-dialog-open="job-apply-dialog"]').forEach(function (button) {
    button.addEventListener('click', open);
  });

  dialog.querySelectorAll('[data-dialog-close]').forEach(function (button) {
    button.addEventListener('click', function () {
      dialog.close();
    });
  });

  dialog.addEventListener('close', clearApplyHash);

  if (fileInput && clientError) {
    fileInput.addEventListener('change', function () {
      showClientError(validateFile());
    });

    form.addEventListener('submit', function (event) {
      var message = validateFile();
      if (message) {
        event.preventDefault();
        showClientError(message);
        fileInput.focus();
        return;
      }
      submitButton.disabled = true;
      submitButton.textContent = 'Đang gửi…';
    });
  }

  // Quay lại trang bằng nút Back: trình duyệt có thể giữ nút đang bị khóa.
  window.addEventListener('pageshow', resetSubmitButton);

  if (dialog.dataset.openOnLoad === 'true' || window.location.hash === '#apply') {
    open();
  }
})();
