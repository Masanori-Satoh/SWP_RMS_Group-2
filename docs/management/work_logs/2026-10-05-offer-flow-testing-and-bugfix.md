# Nhật ký Công việc: Hoàn thiện Ràng buộc Lương, Single Active Offer (GBR-07) & Kiểm thử Luồng Offer

- **Ngày thực hiện:** 05/10/2026
- **Người thực hiện:** Phạm Thị Huyền (HuyenPT)
- **Nhánh làm việc:** `fix/huyenpt/offer-flow-bugfix`
- **Module phụ trách:** `offer` (Quản lý Đề xuất Tuyển dụng)
- **Tài liệu chi tiết kiểm thử:** [offer-inter1-flow-testing.md](../../members/huyenpt/offer-inter1-flow-testing.md)

---

## 1. Yêu cầu & Mục tiêu thực hiện
1. **Ràng buộc lương:** Bổ sung ràng buộc nghiệp vụ lương thử việc $\le$ mức lương chính thức (bên cạnh ràng buộc đã có $\ge 85\%$).
2. **Quy tắc Single Active Offer (`GBR-07`):**
   - Phân loại rõ trạng thái Offer: Nhóm A (được phép tạo đè/cập nhật) và Nhóm B (đang xử lý/bị khóa).
   - Tự động lọc danh sách ứng viên đỗ phỏng vấn: loại bỏ ứng viên có Offer Nhóm B, giữ lại Nhóm A và ứng viên chưa có Offer.
   - Khi tạo đè lên Nhóm A, thực hiện cập nhật in-place trên bản ghi cũ để tuân thủ ràng buộc CSDL `UQ_OfferProposal_Application UNIQUE`.
   - Chặn tuyệt đối và ném ngoại lệ chuẩn `OFFER_LOCKED_STATE` nếu cố ý tạo/sửa Offer Nhóm B.
3. **Cải tiến UI Stat Cards:** Căn giữa tiêu đề từng ô thống kê ở dòng trên, đưa số lượng và icon xuống dòng dưới dàn đều 2 bên.
4. **Kiểm thử tự động:** Viết và hoàn thiện bộ Unit Test (20 test cases) và Integration Test (7 test cases).

---

## 2. Thay đổi Cơ sở dữ liệu (Database)
- Không thêm cột mới hay thay đổi cấu trúc bảng.
- Tương thích 100% với constraint hiện tại `UQ_OfferProposal_Application` (mỗi Application chỉ có 1 OfferProposal).

---

## 3. Thay đổi Mã nguồn & Giao diện
- `CreateOfferRequest.java`, `UpdateOfferRequest.java`: Thêm custom cross-field validation `@AssertTrue` cho `probationSalary <= proposedSalary`.
- `OfferServiceImpl.java`: Cài đặt logic lọc Nhóm A/Nhóm B, xử lý ghi đè in-place và ném lỗi nghiệp vụ `BaseBusinessException`.
- `list.html`, `offers.css`: Redesign layout stat cards.
- `OfferServiceTests.java`: 20 Unit test cases bao phủ toàn bộ các rule và trường hợp biên.
- `docs/members/huyenpt/offer-flow-testing.md`: Báo cáo kiểm thử và kịch bản test tay.

---

## 4. Kết quả Kiểm thử (Testing)
- **Unit Test (`OfferServiceTests.java`):** **56/56 PASS (100%)** (32 test methods tham số hóa toàn diện).
- **Integration Test (`OfferIntegrationTest.java`):** **12/12 PASS (100%)**.
- **Tổng số test Offer:** **68/68 tests PASS (100%)**.
- **Build Maven (`mvnw test`):** **BUILD SUCCESS**.
