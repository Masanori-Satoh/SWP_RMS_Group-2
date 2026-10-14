# 2026-10-13 — Chuyển đổi Tạo mới & Xem chi tiết Offer sang Màn hình chuyên biệt (Module `offer`)

1. **Người thực hiện:** Phạm Thị Huyền (HuyenPT) — nhánh `feat/huyenpt/popup-to-screen`.
2. **Yêu cầu & Mục tiêu:**
   - Thay thế các thao tác dạng Pop-up Modal (Tạo mới Offer, Xem chi tiết Offer HR, Xem chi tiết & Duyệt Offer Director) bằng **Màn hình chuyên biệt (Dedicated Screens)** theo chuẩn SaaS và Layout Mộc RMS (`layout/internal.html`).
   - Cung cấp URL độc lập cho từng màn hình (`/offers/create`, `/offers/{id}/edit`, `/offers/{id}`) giúp lưu vết, điều hướng và chia sẻ thuận tiện.
   - Nâng cấp trải nghiệm duyệt của Giám đốc (Director) với Card ra quyết định trực tiếp trên trang thay cho modal lồng nhau.
3. **Database (Cơ sở dữ liệu):**
   - Không thay đổi schema hoặc migration cơ sở dữ liệu.
   - Tương thích 100% với cấu trúc bảng `OfferProposal` và `OfferApproval`.
4. **Mã nguồn & Giao diện:**
   - **Tạo mới & Sửa Offer (`offers/form.html`):** Form tạo mới (`GET /offers/create`) và chỉnh sửa (`GET /offers/{id}/edit`), hỗ trợ tự động điền ứng viên đỗ phỏng vấn, nút "Gợi ý 85%" (`BR-OFF-01`), real-time hint, và banner lý do từ chối từ Director.
   - **Chi tiết Offer HR & Director (`offers/detail.html`):** Màn hình chi tiết độc lập (`GET /offers/{id}`) kế thừa `layout/internal`, hiển thị hồ sơ ứng viên, kết quả phỏng vấn, liên kết xem CV (`appliedCvUrl`), địa chỉ, ghi chú người lập HR, lịch sử phê duyệt (timeline). Tích hợp Card quyết định của Giám đốc (`.director-decision-card`) khi ở trạng thái `Pending_Director`.
   - **Danh sách Director (`offers/director-list.html`):** Chuyển toàn bộ trigger click sang link `GET /offers/{id}`, loại bỏ modal `#directorDetailModal` cũ.
   - **Danh sách HR (`offers/list.html`):** Dọn dẹp modal tạo mới và chỉnh sửa, giữ lại modal Xuất Excel đa phạm vi.
   - **Controller & JavaScript:** Cập nhật `OfferController.java` (thêm Form POST endpoints cho approve/reject), cập nhật `offers.js` (các hàm xử lý quyết định trên screen với SweetAlert2), bổ sung styles trong `offers.css`.
5. **Kiểm tra (Testing):**
   - **Kiểm thử tự động:** Chạy `mvnw test "-Dtest=OfferIntegrationTest,OfferServiceTests"` đạt **76/76 PASS (100%)** (17 Integration tests, 59 Unit tests), 0 failure, 0 error.
   - **Kiểm thử thực tế (Manual/E2E):** Đã kiểm thử luồng HR tạo mới/chỉnh sửa/xem chi tiết và luồng Director mở chi tiết/xem CV/phê duyệt/từ chối trên trình duyệt hoạt động hoàn hảo.
6. **Vấn đề tồn đọng:** Không có. Nhánh đã sẵn sàng để tạo Pull Request hợp nhất vào `main`.
