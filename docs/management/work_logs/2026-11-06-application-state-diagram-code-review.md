# Nhật ký — đối chiếu Application state diagram với code

- **Ngày / người thực hiện:** 06/11/2026 — LinhDN.
- **Yêu cầu:** Kiểm tra ảnh state machine Application người dùng gửi có phù hợp code hiện tại không.
- **Phạm vi:** Application submission/review/withdrawal, Talent Pool draft/consent/recommendation/closure, Interview schedule/final result và Offer sent/response.
- **Database / mã nguồn:** Chỉ đọc code, không query database, không sửa hoặc chạy ứng dụng.
- **Kết quả:** Mười trạng thái và luồng chính khớp. Cần thêm HM_Suggested → Rejected, guard đóng tin và consent/Offer, cùng note các thao tác không đổi trạng thái. Ghi riêng fallback khi chốt interview và thiếu guard ApplicationStatus nguồn trong service gửi Offer.
- **Tài liệu:** [Báo cáo đối chiếu và vị trí code](../../members/linhdn/2026-11-06-application-state-diagram-code-review.md), cập nhật README thành viên và WORK_LOG chung. Giữ báo cáo SRS và các file người dùng đang có.
- **Kiểm chứng:** Review tĩnh các service/repository sở hữu transition; đọc DTO và danh sách status; tham khảo test source liên quan nhưng không chạy test. Kiểm tra định dạng bằng git diff --check.
- **Giới hạn / tồn đọng:** Chưa phải kiểm chứng runtime; chưa sửa diagram hoặc code. Các ngoại lệ kỹ thuật được mô tả riêng, không gộp thành lỗi chắc chắn đã tái hiện.
- **Git:** Không commit/push.
