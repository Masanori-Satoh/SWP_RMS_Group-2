/* Trạng thái ghim sidebar nội bộ. Nạp ĐỒNG BỘ (không defer) trong <head> của layout/internal.html.
   Việc duy nhất: đặt class `sidebar-pinned` lên <html> trước lần vẽ đầu, để --sidebar-w đúng ngay
   từ khung hình đầu tiên. Nếu để interface.js (defer) làm thì nội dung nhảy 68px -> 274px sau khi vẽ
   và hiệu ứng margin-left của .main-content biến thành layout shift.
   Nút ghim, aria-pressed, lưu lựa chọn: vẫn ở interface.js. */
(function () {
  'use strict';
  try {
    if (localStorage.getItem('rms_sidebar_pinned') === 'true') {
      document.documentElement.classList.add('sidebar-pinned');
    }
  } catch (err) {
    /* Chặn lưu trữ (chế độ riêng tư...): giữ sidebar thu gọn mặc định. */
  }
})();
