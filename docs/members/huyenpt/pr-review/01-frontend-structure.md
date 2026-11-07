# PR 01/08 — Tách frontend Offer theo vai trò

Base: `main` (`1e3d7134`). Branch: `codex/offer-01-frontend-structure`.

## Thay đổi

- Thay JS/CSS Offer nguyên khối bằng các tài nguyên common, HR và Director; mỗi trang nạp đúng nhóm tài nguyên.
- Bỏ modal Director dư trên danh sách HR, phục hồi flash toast và loại việc nạp trùng stylesheet tại sáu template Offer.
- Production chỉ gồm 16 file HTML/CSS/JS thuộc Offer, +4.467/-6.488 dòng, giảm ròng 2.021 dòng. Không đổi backend, database, shared layout hoặc module khác.
- Phần frontend được trích từ mốc `a8954570` của lịch sử Offer; không cherry-pick các commit merge hoặc thay đổi thành viên khác. Báo cáo lịch sử đầy đủ dành cho PR 08.

## Kiểm tra

- 202 Maven test đạt: OfferIntegrationTests 34, CandidateOfferWebTests 19, CandidateOfferServiceTests 35, OfferServiceTests 114; không lỗi hoặc bỏ qua.
- `node --check` đạt cho cả ba JS mới; sáu template không tham chiếu tài nguyên bị xóa và các đường dẫn CSS/JS đều tồn tại.
- `git diff --cached --check` đạt. Nhánh dựng trực tiếp trên main; kiểm tra merge với origin/main được thực hiện trước khi push.
- Chưa chạy lại browser A/B trong lần tách PR này. Các bản sửa nghiệp vụ, thông báo và export sau mốc trên được giữ ở nhánh gốc để đưa vào PR sau; PR này chưa phải toàn bộ bản hoàn thiện Offer.

## Thứ tự cả bộ PR

1. Frontend: tách tài nguyên và loại CSS trùng (PR hiện tại).
2. Phân quyền và validation nghiệp vụ Offer.
3. Đồng bộ UI, popup, ngôn ngữ và form.
4. Lưu thời gian thử việc: migration 020 và backend liên quan.
5. Hợp đồng export, tối ưu truy vấn/API và workbook.
6. Giao dịch, xử lý đồng thời và notification nhận/từ chối với link đúng người nhận.
7. Vòng đời Expired, migration 021, job tắt mặc định và tích hợp Application.
8. Tài liệu audit, học nghiệp vụ, comment tiếng Việt và bằng chứng kiểm thử.

PR sau dựa trên PR trước nếu chưa merge; khi đã merge thì cập nhật main và dựng trên main mới. Chỉ PR 01 được chuẩn bị/push trong lượt này; đợi người dùng xác nhận tạo PR trước khi tiếp tục.
