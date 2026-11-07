# Nhật ký — rà soát tính nhất quán nội bộ SRS

- **Ngày:** 06/11/2026.
- **Người thực hiện:** LinhDN.
- **Yêu cầu:** Rà SRS đầu tiên, chỉ các module người dùng phụ trách; kiểm tra chức năng, màn hình, message và diagram; chưa đối chiếu code. Người dùng xác nhận Authentication, Account/Department, Talent Pool, Admin và Core/Audit Log.
- **Nguồn:** `D:/dowload/RMS_SRS (1).docx`.
- **Database:** Không đọc hoặc thay đổi.
- **Mã nguồn:** Không đọc để so sánh, không chỉnh sửa, không chạy ứng dụng/test.
- **Tài liệu:** Tạo [báo cáo review](../../members/linhdn/2026-11-06-srs-internal-consistency-review.md), thêm liên kết vào README thành viên và WORK_LOG chung. Giữ nguyên DOCX gốc.
- **Cách kiểm chứng:** Trích XML paragraph/table, đối chiếu các mục trong chính SRS; kiểm tra ô message gốc; quan sát ảnh diagram/màn hình trích từ DOCX. Các diagram cần thiết đọc được, chưa cần ảnh chụp bổ sung.
- **Kết quả:** 12 nhóm lỗi/mâu thuẫn xác định và 14 điểm thiếu/cần chốt. Nhóm chính: message trùng/sai nội dung, OTP 15 phút/1 phút, giới hạn SĐT 15/20 ký tự, quyền Admin, menu HM, nhánh CV mới, điều hướng selection, diagram UC, footnote và CV mẫu sai người.
- **Giới hạn:** Đây là review tài liệu, không phải xác nhận lỗi runtime hoặc đánh giá mức tuân thủ code. Đề xuất nghiệp vụ chưa được chốt thành thay đổi SRS.
- **Tồn đọng:** Sửa bản SRS sau khi người dùng chọn hướng cho các rule còn mở; so sánh với code là công việc riêng khi được yêu cầu.
- **Git:** Không commit/push.
