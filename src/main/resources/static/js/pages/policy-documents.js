/*
 * Admin · Tài liệu chính sách: nút "Tải bản mới" mở hộp thoại của đúng tài liệu, "Hủy" đóng lại.
 * Server báo lỗi file (data-open-on-load="true") → mở lại hộp đó để Admin thấy lỗi cạnh ô chọn file.
 */
(function () {
  for (const button of document.querySelectorAll('[data-pdoc-open]')) {
    button.addEventListener('click', function () {
      const dialog = document.getElementById('pdoc-dialog-' + button.dataset.pdocOpen);
      if (dialog) {
        dialog.showModal();
      }
    });
  }

  for (const button of document.querySelectorAll('[data-pdoc-close]')) {
    button.addEventListener('click', function () {
      button.closest('dialog').close();
    });
  }

  const failed = document.querySelector('.pdoc-dialog[data-open-on-load="true"]');
  if (failed) {
    failed.showModal();
  }
})();
