# 13/11/2026 — Ước tính LOC LinhDN

- Đọc cả ba sheet và ba ảnh ví dụ trong `Tính Loc.xlsx`. Sheet `LOC Estimation guide` cho bảy mức, từ 60 đến 240 LOC.
- Đối chiếu Git của nhánh `feature/update-ui-authen` tại `bafdf65d`, báo cáo của LinhDN, các template Thymeleaf, controller và service tham gia từng luồng.
- Kiểm tra 26 dòng ước tính và tổng từng nhóm. Authentication 450, Quản trị 1.230, Talent Pool 990, Hồ sơ cá nhân 120. Tổng 2.790 LOC quy đổi.
- Áp trần Level 7, gộp các biến thể và thành phần tái sử dụng có cùng nghiệp vụ. LOC quy đổi là mức ước tính cho phạm vi có đóng góp, chưa có hệ số phân bổ công việc chung.
- Phân tích tĩnh, không chạy app. Nội dung Excel và mã nguồn được đọc trực tiếp.
- [Bảng tính chi tiết và căn cứ](../../members/linhdn/2026-11-13-uoc-tinh-loc.md)
