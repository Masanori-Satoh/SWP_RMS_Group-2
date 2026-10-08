# TỔNG KẾT NỘI DUNG HOÀN THIỆN PHÂN HỆ QUẢN LÝ ĐỀ XUẤT OFFER (PHASE 2 - DIRECTOR APPROVAL)

- **Dự án:** Hệ Thống Quản Lý Tuyển Dụng — SWP_RMS_Group-2 (Mộc RMS)
- **Thành viên phụ trách:** Phạm Thị Huyền (`HuyenPT`)
- **Phân hệ (Module):** Quản lý Đề xuất Lương & Tuyển dụng (`Offer Management`) — Phase 2: Luồng Phê duyệt của Giám đốc (Director Review & Approval).

---

## I. MỤC TIÊU VÀ BỐI CẢNH TRIỂN KHAI

Tiếp nối Phase 1 (Cải thiện UI/UX và Xuất Excel), Phase 2 tập trung vào việc hoàn thiện **luồng phê duyệt Offer chính thức từ Giám đốc (Director)**.
Theo quyết định nghiệp vụ hiện tại:
- Không xây dựng vòng lặp `Reject -> HR sửa -> Resubmit`.
- Không thêm entity Negotiation.
- Quyết định phê duyệt/từ chối của Director là quyết định chính thức được lưu thẳng vào hệ thống. Các lỗi nhỏ hoặc điều chỉnh sẽ được HR và Director thảo luận ngoài hệ thống trước khi Director đưa ra quyết định cuối cùng trên phần mềm.
- Tuân thủ nghiêm ngặt `ARCHITECTURE_GUIDE.md`, thiết kế Controller mỏng, xử lý nghiệp vụ tại Service, và sử dụng UI Modal trực tiếp trên trang.

---

## II. CHI TIẾT CÁC TÍNH NĂNG ĐÃ TRIỂN KHAI

### 1. Backend (Tầng Dữ liệu & Xử lý Nghiệp vụ)

- **DTO Mới:** 
  - Tạo `DirectorDecisionRequest.java` trong package `dto` nhằm hứng dữ liệu ghi chú (comments) từ modal phê duyệt/từ chối của Giám đốc. Tích hợp validation `@Size(max = 2000)`.
  
- **Tầng Service (`OfferService` & `OfferServiceImpl`):**
  - Bổ sung 2 phương thức mới: `approveOfferByDirector(Integer id, String comments)` và `rejectOfferByDirector(Integer id, String comments)`.
  - Nghiệp vụ kiểm tra trạng thái chặt chẽ: Chỉ cho phép thao tác khi Offer đang ở trạng thái chờ duyệt (`Pending_Director`).
  - Ghi nhận lịch sử duyệt: Tạo mới và lưu bản ghi vào bảng `OfferApproval` với `status` tương ứng là `Approved` hoặc `Rejected`, cùng với nhận xét của Director.
  - Chuyển đổi trạng thái Offer (State Machine): Cập nhật trạng thái của `OfferProposal` sang `Director_Approved` hoặc `Director_Rejected`.

- **Tầng Controller (`OfferController`):**
  - Thêm 2 endpoint AJAX (JSON) mới:
    - `POST /offers/{id}/approve`
    - `POST /offers/{id}/reject`
  - Các endpoint này tiếp nhận dữ liệu từ UI, gọi Service xử lý và trả về chuẩn `ApiResponse<OfferResponse>`.
  - Phân quyền theo URL cấu hình trong `SecurityConfig` cho phép `ROLE_DIRECTOR` (và `System Admin`) truy cập.

### 2. Frontend (Giao diện Người dùng - UI/UX)

- **Cập nhật JavaScript (`offers.js`):**
  - Bổ sung logic xử lý cho **Modal Phê duyệt / Từ chối (Director Decision Modal)**.
  - Thêm các hàm `openDirectorDecisionModal()`, `closeDirectorDecisionModal()` và hàm gọi AJAX `submitDirectorDecision()` tích hợp trực tiếp token bảo mật CSRF.
  - Tích hợp thông báo Toast khi thao tác thành công và tự động tải lại trang mượt mà sau 1 giây.

- **Cập nhật Màn hình Danh sách (`list.html` - Screen 30):**
  - Tại cột "Thao tác" (Action Buttons): Thêm 2 nút bấm **Phê duyệt** (màu xanh lá) và **Từ chối** (màu đỏ) dành riêng cho người dùng có vai trò `Director` (hoặc `System Admin`) đối với các bản ghi đang ở trạng thái `Pending_Director`.
  - Thêm HTML cấu trúc Modal `#directorDecisionModal` vào cuối trang để hứng thao tác click.

- **Cập nhật Màn hình Chi tiết (`detail.html` - Screen 32):**
  - Tương tự như màn hình danh sách, bổ sung 2 nút **Phê duyệt** và **Từ chối** nổi bật tại khu vực Action Buttons trên cùng.
  - Đính kèm Modal `#directorDecisionModal` và các file script/toast thông báo để đảm bảo hoạt động độc lập ngay trong màn chi tiết.

---

## III. KẾT LUẬN

Phase 2 đã tích hợp thành công luồng phê duyệt từ cấp quản lý (Director), đánh dấu việc khép kín quy trình tạo, đệ trình, phê duyệt và phát hành Thư mời làm việc (Offer Letter) trong hệ thống Mộc RMS. Kiến trúc phần mềm tuân thủ nghiêm ngặt các quy tắc hiện hành, hạn chế tối đa việc tạo thêm bảng cơ sở dữ liệu dư thừa và duy trì trải nghiệm liền mạch qua các Pop-up Modal.
