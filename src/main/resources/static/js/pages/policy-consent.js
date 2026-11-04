/*
 * Popup đồng ý Điều khoản & Chính sách bảo mật (fragments/ui/policy-consent-dialog.html).
 * - Tự mở khi tải trang; Esc / đóng kiểu khác đều bị chặn tới khi bấm "Tôi đồng ý".
 * - Nút "Tôi đồng ý" chỉ bật khi tick đủ 2 ô.
 * - Gửi kèm trang đang xem (cả #apply) để server đưa ứng viên về đúng chỗ.
 */
(function () {
  const dialog = document.querySelector('[data-policy-consent]');
  if (!dialog || typeof dialog.showModal !== 'function') {
    return;
  }

  const checks = dialog.querySelectorAll('[data-policy-consent-check]');
  const submit = dialog.querySelector('[data-policy-consent-submit]');
  const hint = dialog.querySelector('[data-policy-consent-hint]');
  const returnInput = dialog.querySelector('[data-policy-consent-return]');
  let leaving = false;

  function allChecked() {
    for (const box of checks) {
      if (!box.checked) {
        return false;
      }
    }
    return true;
  }

  function refreshButton() {
    const ready = allChecked();
    submit.disabled = !ready;
    hint.textContent = ready ? 'Bạn có thể bấm "Tôi đồng ý".' : 'Hãy tick cả 2 ô để tiếp tục';
  }

  for (const box of checks) {
    box.addEventListener('change', refreshButton);
  }

  returnInput.value = window.location.pathname + window.location.search + window.location.hash;

  // Gửi form đồng ý / đăng xuất thì cho phép rời trang, không mở lại popup
  for (const form of dialog.querySelectorAll('form')) {
    form.addEventListener('submit', function () {
      leaving = true;
    });
  }

  dialog.addEventListener('cancel', function (event) {
    event.preventDefault();
  });

  // Trình duyệt vẫn có thể đóng sau nhiều lần Esc → mở lại ngay
  dialog.addEventListener('close', function () {
    if (!leaving) {
      dialog.showModal();
    }
  });

  refreshButton();
  dialog.showModal();
})();
