# TỔNG KẾT NỘI DUNG HOÀN THIỆN PHÂN HỆ QUẢN LÝ ĐỀ XUẤT OFFER (PHASE 2 - DIRECTOR APPROVAL)

- **Dự án:** Hệ Thống Quản Lý Tuyển Dụng — SWP_RMS_Group-2 (Mộc RMS)
- **Thành viên phụ trách:** Phạm Thị Huyền (`HuyenPT`)
- **Phân hệ (Module):** Quản lý Đề xuất Lương & Tuyển dụng (`Offer Management`) — Phase 2: Luồng Phê duyệt của Giám đốc (Director Review & Approval).
- **Nhánh phát triển (Branch):** `feat/huyenpt/inte2/director-offer-approval`

---

## I. MỤC TIÊU VÀ BỐI CẢNH TRIỂN KHAI

Tiếp nối Phase 1 (Cải thiện UI/UX và Xuất Excel), Phase 2 tập trung vào việc hoàn thiện **luồng phê duyệt Offer chính thức từ Giám đốc (Director)** và **chuẩn hóa trải nghiệm giao diện người dùng (UI/UX)**:
- Khép kín toàn bộ vòng đời của một Đề xuất Offer: Tạo bản thảo (Draft) -> Trình duyệt (`Pending_Director`) -> Giám đốc phê duyệt (`Director_Approved`) hoặc Từ chối (`Director_Rejected`) -> Phát hành Offer Letter (`Sent_Candidate`) -> Kết quả phản hồi từ Ứng viên (`Accepted` / `Declined`).
- Theo quyết định nghiệp vụ hiện tại:
  - Quyết định phê duyệt/từ chối của Director là quyết định chính thức được lưu vết vào hệ thống. Các điều chỉnh nhỏ được thảo luận ngoài hệ thống trước khi ra quyết định cuối cùng.
  - Không phát sinh bảng cơ sở dữ liệu dư thừa, sử dụng bảng `OfferApproval` để ghi lại nhật ký phê duyệt.
- Tuân thủ nghiêm ngặt quy chuẩn kiến trúc [`docs/architecture/ARCHITECTURE_GUIDE.md`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/docs/architecture/ARCHITECTURE_GUIDE.md):
  - Controller giữ vai trò mỏng (Thin Controller), toàn bộ nghiệp vụ kiểm tra trạng thái và phân quyền được bao gói tại Service Layer.
  - Đồng nhất trải nghiệm tương tác thông qua **Modal Pop-up căn giữa màn hình** thay vì chuyển trang phụ hay ngăn kéo trượt, đảm bảo giao diện gọn gàng, liền mạch theo hệ thống phong cách Mộc RMS.

---

## II. CHI TIẾT CÁC TÍNH NĂNG VÀ THAY ĐỔI ĐÃ TRIỂN KHAI

### 1. Backend (Tầng Dữ liệu, Nghiệp vụ & Điều hướng)

- **DTO Mới:** 
  - Tạo `DirectorDecisionRequest.java` trong package `dto` nhằm tiếp nhận dữ liệu nhận xét/lý do (`comments`) từ Giám đốc khi phê duyệt hoặc từ chối. Tích hợp validation `@Size(max = 2000)`.
  
- **Tầng Repository (`OfferProposalRepository`):**
  - Bổ sung các phương thức truy vấn và thống kê KPI dành riêng cho Director:
    - Đếm số lượng theo trạng thái: Chờ duyệt (`countByOfferStatus(Pending_Director)`), Đã duyệt (`statApproved`), Bị từ chối (`statRejected`).
    - Tìm kiếm và phân trang theo các tab trạng thái: `PENDING`, `APPROVED`, `REJECTED`, `ALL`/`HISTORY`.

- **Tầng Service (`OfferService` & `OfferServiceImpl`):**
  - **Phê duyệt đề xuất (`approveOfferByDirector`):**
    - Kiểm tra trạng thái nghiêm ngặt: Chỉ cho phép thao tác khi Offer ở trạng thái `Pending_Director`.
    - Tạo bản ghi mới trong bảng `OfferApproval` với trạng thái `Approved`, ghi nhận Giám đốc phê duyệt, thời điểm và nhận xét.
    - Cập nhật trạng thái của `OfferProposal` sang `Director_Approved`.
  - **Từ chối đề xuất (`rejectOfferByDirector`):**
    - Bắt buộc nhập lý do từ chối.
    - Ghi nhận vào bảng `OfferApproval` với trạng thái `Rejected`.
    - Cập nhật trạng thái `OfferProposal` sang `Director_Rejected`.
  - **Lấy chi tiết đề xuất đầy đủ (`getOfferDetail`):**
    - Trả về DTO `OfferDetailResponse` bao gồm thông tin ứng viên, kết quả đánh giá phỏng vấn của Hội đồng, chi tiết gói đãi ngộ, và danh sách lịch sử phê duyệt `approvalHistory`.
    - Đảm bảo an toàn null-safety cho tất cả các trường dữ liệu quan trọng.

- **Tầng Controller (`OfferController`):**
  - **Rẽ nhánh hiển thị danh sách (`GET /offers`):**
    - Tự động kiểm tra quyền hạn của người dùng đăng nhập.
    - Nếu người dùng có quyền `ROLE_DIRECTOR`, controller tự động điều hướng sang giao diện chuyên biệt `offers/director-list.html`, truyền kèm các số liệu KPI (`statPending`, `statApproved`, `statRejected`), danh sách phân trang và các bộ lọc tab.
  - **Content Negotiation cho Pop-up Modal (`GET /offers/{id}`):**
    - Khi nhận được request với header `Accept: application/json`, controller trả về dữ liệu JSON chi tiết qua `ApiResponse<OfferDetailResponse>`, phục vụ việc render động nội dung Modal mà không cần tải lại toàn bộ trang.
  - **Endpoint xử lý quyết định:**
    - `POST /offers/{id}/approve`: Nhận quyết định phê duyệt và trả về `ApiResponse<OfferResponse>`.
    - `POST /offers/{id}/reject`: Nhận quyết định từ chối và lý do, trả về `ApiResponse<OfferResponse>`.
    - Phân quyền URL trong Spring Security chỉ cho phép `ROLE_DIRECTOR` (và `System Admin`) thực thi.

---

### 2. Frontend (Giao diện Người dùng & Tương tác UX)

- **Màn hình Danh sách chuyên biệt cho Director (`offers/director-list.html`):**
  - **Thẻ thống kê KPI (Stat Cards):** Thiết kế 3 thẻ thống kê nổi bật: *Chờ duyệt* (Icon đồng hồ cát màu hổ phách), *Đã duyệt* (Icon tích xanh), và *Tổng đề xuất*.
  - **Thanh công cụ lọc:** Hỗ trợ chuyển tab nhanh giữa *Chờ duyệt* và *Lịch sử đã duyệt/từ chối*, kết hợp ô tìm kiếm theo tên ứng viên hoặc vị trí.
  - **Bảng danh sách chuẩn Mộc RMS:** Hiển thị rõ ràng mã đề xuất, họ tên ứng viên, vị trí tuyển dụng, mức lương đề xuất, mức lương thử việc (kèm badge % tự động), trạng thái có badge màu nhận diện, thời gian tạo và thông tin HR phụ trách.

- **Tái thiết kế Modal Pop-up Chi tiết Ứng viên Căn Giữa (`#directorDetailModal`):**
  - **Loại bỏ Drawer trượt bên phải:** Chuyển đổi toàn bộ cơ chế hiển thị chi tiết từ drawer trượt thành **Modal pop-up hiển thị chính giữa màn hình**, sử dụng các class chuẩn `.modal-overlay`, `.modal-box.modal-xl` đồng nhất với giao diện HR (`detailOfferModal`).
  - **Bố cục 4 phân vùng thông tin mạch lạc, trực quan:**
    1. **1. Thông tin ứng viên:** Họ và tên, email liên hệ, số điện thoại, vị trí ứng tuyển, phòng ban, liên kết xem file CV an toàn (Safe URL check, tự động ẩn nếu không có), địa chỉ liên hệ.
    2. **2. Kết quả phỏng vấn của Hội đồng:** Quyết định của hội đồng, mức lương khuyến nghị từ Hiring Manager, tên HM phụ trách, nhận xét đánh giá chi tiết.
    3. **3. Thông tin đãi ngộ & Điều khoản Offer:** Bố cục 3 cột tối ưu gồm chức danh đề xuất, lương chính thức (Gross/tháng), lương thử việc kèm badge tỷ lệ % (`85%`), thời gian thử việc, ngày bắt đầu dự kiến, địa điểm làm việc, gói phúc lợi & đãi ngộ đặc thù, cùng ghi chú từ chuyên viên HR.
    4. **4. Thao tác phê duyệt / Lịch sử duyệt (Hiển thị có điều kiện theo trạng thái):**
       - **Khi ở trạng thái `Pending_Director`:** Hiển thị khung nhập ý kiến phản hồi / lý do từ chối (có bộ đếm ký tự 0/500) và 2 nút hành động ở modal footer: **Từ chối** (`btn-outline-danger`) và **Phê duyệt** (`btn-success`).
       - **Khi ở trạng thái khác:** Tự động ẩn khu vực hành động, thay vào đó hiển thị phân vùng **Lịch sử phê duyệt của Director** dưới dạng Timeline với mốc thời gian, tên người duyệt và nội dung nhận xét.
  - **Trải nghiệm tương tác tiện ích cao cấp:**
    - Nền mờ kính (Backdrop blur) tạo chiều sâu thị giác.
    - Đóng mở linh hoạt bằng nút ✕ góc trên, nút "Đóng" ở footer, nhấp chuột vào vùng nền bên ngoài (Backdrop click), hoặc nhấn phím `Escape`.

- **Cập nhật JavaScript (`offers.js`):**
  - Bổ sung các hàm điều khiển modal: `openDirectorDetailModal(id)`, `closeDirectorDetailModal()`, `handleDirectorModalBackdropClick(event)`, cùng event listener lắng nghe phím `Escape`.
  - Tự động gọi API `GET /offers/{id}` với header `Accept: application/json`, điền dữ liệu động, định dạng tiền tệ VNĐ và tính toán tỷ lệ % thử việc chính xác.
  - Xử lý hành động phê duyệt (`submitDirectorApprove`) và từ chối (`submitDirectorReject`) với hộp thoại xác nhận SweetAlert2 và tự động đính kèm CSRF token.
  - Cung cấp alias tương thích ngược `window.openDirectorDrawer` và `window.closeDirectorDrawer` để không làm gián đoạn mã nguồn cũ.

- **Chuyển đổi Luồng Xem Chi tiết sang Màn hình Chuyên biệt (Dedicated Screen - URL `/offers/{id}`):**
  - Tiếp tục hoàn thiện trải nghiệm theo chuẩn của màn hình HR, luồng xem chi tiết của Director được chuyển đổi hoàn toàn từ Pop-up Modal sang **Màn hình chuyên biệt (Dedicated Screen)**:
    1. Thay thế toàn bộ liên kết click trên bảng `offers/director-list.html` sang thẻ điều hướng `GET /offers/{id}`.
    2. Đồng bộ giao diện `offers/detail.html`: Bổ sung hiển thị liên kết xem hồ sơ CV (`appliedCvUrl`), địa chỉ liên hệ của ứng viên (`candidateAddress`), và thông tin người lập đề xuất (`proposedByName`).
    3. Tích hợp trực tiếp **Card Quyết định của Giám đốc (Director Decision Panel)** trên trang chi tiết khi ở trạng thái `Pending_Director`: Có khung nhập lý do/ý kiến phản hồi kèm bộ đếm 0/500 ký tự và 2 nút hành động trực tiếp "Phê duyệt" / "Từ chối".
    4. Bổ sung các hàm `submitDirectorDecisionFromScreen`, `scrollToDirectorDecision`, `updateDirectorDetailCommentCounter` trong `offers.js` gọi RESTful API và cập nhật trạng thái liền mạch.

---

---

### 5. Nâng cấp Bộ lọc Trạng thái & Hiển thị Trạng thái Vòng đời Thực tế (Director Offer Enhancements)

- **Thanh công cụ lọc trạng thái hiển thị số lượng theo thời gian thực (Live Counts Dropdown):**
  - Bổ sung dropdown `<select id="statusFilter">` trên thanh công cụ của Giám đốc tương tự bảng HR, tích hợp số lượng ứng viên theo từng trạng thái hiển thị trong ngoặc đơn:
    - `-- Tất cả trạng thái -- (x)`
    - `Chờ Giám đốc duyệt (x)`
    - `Giám đốc đã duyệt (x)`
    - `Đã gửi thư mời (x)`
    - `Ứng viên chấp nhận (x)`
    - `Ứng viên từ chối (x)`
    - `Giám đốc từ chối (x)`
  - Dữ liệu số lượng được cung cấp bởi `offerProposalRepository` qua phương thức `offerService.getDirectorStatusCounts()`.
  - Tự động đồng bộ với 2 Tab ("Chờ duyệt" và "Lịch sử quyết định") và hỗ trợ tìm kiếm từ khóa (tên ứng viên, vị trí, ID offer) kết hợp sắp xếp thời gian (`offers.js`: `applyDirectorFilters`).

- **Chuẩn hóa Hiển thị Trạng thái Vòng đời Thực tế (Real-time Offer Lifecycle Status):**
  - Khắc phục triệt để lỗi ghi đè trạng thái trong `getDirectorOfferHistory`: Loại bỏ việc gán cứng trạng thái quyết định quá khứ của Director (`approval.getStatus()`), bảo toàn trạng thái vòng đời thực tế của `OfferProposal` (`offer.getOfferStatus()`).
  - Cột **Trạng thái** trong bảng `offers/director-list.html` luôn hiển thị chính xác kết quả mới nhất: ví dụ đề xuất sau khi Director duyệt đã được HR gửi đi (`Đã gửi thư mời`) và ứng viên phản hồi (`Ứng viên chấp nhận` hoặc `Ứng viên từ chối`).

---

### 6. Kiểm thử & Đảm bảo Chất lượng (Testing & QA)

- **Unit & Integration Tests:**
  - `OfferServiceTests` (64/64 test cases passed): Bổ sung kiểm thử cho `getDirectorStatusCounts`, `getDirectorOffers`, và kiểm thử bảo toàn trạng thái vòng đời thực tế trong `getDirectorOfferHistory`.
  - `OfferControllerBugTests` (2/2 test cases passed): Xác thực controller trả về HTTP 200, đúng view `offers/director-list`, đầy đủ model attributes (`statusCounts`, `currentStatus`, `currentTab`).
- **Browser Subagent E2E Verification:**
  - Đã tự động kiểm thử toàn trình trên trình duyệt bằng tài khoản `director` tại `http://localhost:8080/offers`.
  - Xác nhận dropdown hiển thị đầy đủ 7 nhóm trạng thái kèm số lượng chính xác: `-- Tất cả trạng thái -- (17)`, `Chờ Giám đốc duyệt (3)`, `Giám đốc đã duyệt (1)`, `Đã gửi thư mời (2)`, `Ứng viên chấp nhận (9)`, `Ứng viên từ chối (1)`, `Giám đốc từ chối (1)`.
  - Bảng dữ liệu hiển thị đúng các huy hiệu trạng thái vòng đời thực tế.

---

## III. KẾT LUẬN

Phân hệ Quản lý Đề xuất Offer của Director đã hoàn thiện toàn diện: từ luồng phê duyệt, trang chi tiết chuyên biệt (Dedicated Screen), đến bộ lọc đa tiêu chí kèm số lượng thời gian thực và hiển thị trạng thái vòng đời thực tế của Offer. Toàn bộ mã nguồn tuân thủ nghiêm ngặt Hướng dẫn kiến trúc dự án RMS (`docs/architecture/ARCHITECTURE_GUIDE.md`) và đã sẵn sàng đưa vào vận hành.

