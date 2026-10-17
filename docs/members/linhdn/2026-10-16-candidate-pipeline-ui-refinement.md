# 2026-10-16 — Tinh chỉnh Giao diện Quản lý Hồ sơ Ứng tuyển & Tuân thủ Kiến trúc Tuyệt đối

1. **Người thực hiện:** Nhóm phát triển Candidate Feature / AI Assistant — nhánh `feature/potential-candidate`.
2. **Yêu cầu & Quyết định:**
   - **Tối ưu Pop-up Ghi chú Ứng viên tiềm năng (`#app-bookmark`):**
     - Nâng cấp Banner nhận diện ứng viên nổi bật hơn: bổ sung avatar initials, họ tên, vị trí ứng tuyển, badge trạng thái tiềm năng (`.badge.badge--success` / `.badge.badge--neutral`).
     - Cân đối bố cục 2 cột (Lịch sử ghi chú bên trái và Thêm ghi chú bên phải): đồng bộ header cột (`.app-bookmark-col-header`), căn chỉnh thẳng hàng, bổ sung bộ đếm ký tự thời gian thực (`0/1000`).
     - Sửa lệch form nhập ghi chú, textarea dùng chuẩn `.form-textarea`, các nút bấm chuẩn hóa DNA hình chữ nhật bo góc 8px (`var(--radius-md)`).
   - **Tối ưu Hộp thoại Xem nhanh CV (`#app-preview`):**
     - Khắc phục tình trạng xem CV chiều ngang bị giãn rộng khó nhìn: giới hạn độ rộng đọc tiêu chuẩn A4 `width: min(980px, calc(100vw - var(--s8)))` với nền tương phản `.app-preview__wrapper`.
     - Bổ sung nút chuyển đổi Mở rộng / Thu nhỏ (`#app-preview-expand` -> `.app-dialog--preview-wide`) cho phép phóng to toàn màn hình khi cần.
   - **Thu nhỏ Icon Thao tác Bảng (`.app-icon-button`):**
     - Thu gọn các nút thao tác từ kích thước lớn `44px x 44px` xuống kích thước chuẩn `32px x 32px` (`var(--s8)`), bo góc 8px (`var(--radius-md)`).
     - Thu nhỏ icon SVG từ `20px` xuống `16px x 16px` sắc nét, tinh tế.
     - Trạng thái đã lưu tiềm năng (`.app-bookmark-button--saved`) hiển thị nền xanh nhạt `var(--brand-soft)`, icon và viền `var(--brand)`.
   - **Cân đối Thanh Tiêu đề & Bố cục Cột Bảng (`.app-table`):**
     - Khắc phục tình trạng tiêu đề và dữ liệu bị dồn lệch trái phải ("trái phải lung tung"): phân bổ tỷ lệ và độ rộng rõ ràng qua các class semantic BEM (`.app-col-num`, `.app-col-candidate`, `.app-col-job`, `.app-col-date`, `.app-col-score`, `.app-col-status`, `.app-col-actions`).
     - Căn giữa các cột số liệu/thao tác (STT 56px, Ngày nộp 115px, Điểm AI 95px, Thao tác 140px); căn trái các cột nội dung (Ứng viên 26%, Vị trí 22%, Trạng thái 20%).
     - Căn giữa cụm nút thao tác (`.app-row-actions { justify-content: center; }`), loại bỏ khoảng trống thừa ở cuối bảng.
   - **Tra cứu Lịch sử Git:**
     - Xác định tác giả xây dựng tính năng "Hành trình hồ sơ" (`detail.timeline` và component `fragments/ui/timeline`): **Dung Luu Tien** (`luutiendung2k6@gmail.com` - Lead Architect) tại commit `68e980a` (10/10/2026).
   - **Tuân thủ Tuyệt đối Quy chuẩn Kiến trúc (`docs/architecture`):**
     - Tuân thủ 100% tài liệu `ARCHITECTURE_GUIDE.md`, `UI_RULES.md`, `UI_CSS_GUIDE.md`, `UI_CATALOG.md`.
     - Không chạm vào các file vùng đỏ 🔴 (`layout/base.html`, `tokens.css`, `topbar.css`, `workspace.css`...).
     - Không hardcode màu (`#hex`, `rgba()`), sử dụng 100% Design Tokens từ `tokens.css`.
     - Giữ nguyên 100% biến backend, form action, ID, và CSRF token.
3. **Database (Cơ sở dữ liệu):**
   - Không thay đổi schema, migration hay seed dữ liệu.
   - Tái sử dụng bảng dữ liệu `Candidate.IsPotential` và hệ thống ghi chú `TalentPoolNote` hiện hành.
4. **Mã nguồn & Giao diện:**
   - **`src/main/resources/templates/candidate/applications.html`:** Bổ sung class `.app-col-*` cho các thẻ `<th>` và `<td>`; điều chỉnh kích thước icon SVG thành `16x16`.
   - **`src/main/resources/templates/candidate/application-dialogs.html`:** Chuẩn hóa cấu trúc HTML modal xem nhanh CV và ghi chú ứng viên, giữ nguyên văn bản tiếng Việt trên 1 dòng để tránh vỡ test assertions.
   - **`src/main/resources/static/css/pages/applications.css`:**
     - Thiết lập tỷ lệ các cột bảng `.app-table` và căn giữa cột thao tác, STT, ngày nộp, điểm AI.
     - Thiết lập kích thước `.app-icon-button` chuẩn 32px (`var(--s8)`) và SVG 16px.
     - Cân đối `.app-bookmark-layout`, tạo banner ứng viên `.app-candidate-banner`, styling bộ đếm ký tự và responsive layout.
     - Cấu hình kích thước đọc A4 cho `#app-preview` và class phóng to `.app-dialog--preview-wide`.
   - **`src/main/resources/static/js/pages/applications.js`:**
     - Bổ sung logic toggle mở rộng/thu nhỏ cho modal preview CV.
     - Cập nhật avatar chữ cái đầu và metadata banner ứng viên khi mở modal bookmark.
5. **Kiểm tra (Testing):**
   - **Kiểm thử tự động:** Chạy `mvn test "-Dtest=ApplicationPipelineWebTests,TalentPoolBookmarkTests"` đạt **39/39 PASS (100%)**, 0 failure, 0 error.
   - **Kiểm thử kiểm tra hồi quy:** Xác nhận các quyền phân quyền HR, Hiring Manager, Director, CSRF token và API bookmark đều hoạt động chính xác.
6. **Vấn đề tồn đọng:** Không có. Giao diện đã đạt độ hoàn thiện cao, tuân thủ nghiêm ngặt quy tắc kiến trúc và sẵn sàng hoạt động ổn định.
