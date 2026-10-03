# 📚 Quán triệt Cấu trúc Tài liệu & Database (SWP_RMS_Group-2)

Tài liệu này sinh ra để thiết lập **kỷ luật thép** về cách tổ chức thư mục trong dự án. Tất cả thành viên vui lòng đọc kỹ để tránh làm "rác" kho chứa chung.

---

## 1. 🗄️ Phân biệt ranh giới Database (Root vs Docs)
Rất nhiều bạn đang nhầm lẫn chức năng của 2 thư mục này. Xin quán triệt như sau:

*   **Thư mục `database/` (Ở ngoài cùng, thư mục gốc):** 
    *   **Chức năng:** Là nơi chứa CODE CHẠY THẬT.
    *   **Được phép chứa:** Chỉ chứa các file script như `.sql` (tạo bảng, insert dữ liệu). 
    *   **Cấm:** Không vứt tài liệu chữ, ảnh hay file Word vào đây.

*   **Thư mục `docs/database/`:** 
    *   **Chức năng:** Là nơi chứa TÀI LIỆU (cho con người đọc).
    *   **Được phép chứa:** Các file Markdown (`.md`), file PDF mô tả sơ đồ ERD, thiết kế Model, hoặc bài viết Hướng dẫn cách tạo data mẫu (Seeding Guide).

---

## 2. 📂 Cấu trúc thư mục Docs
*(Lưu ý: Tuyệt đối KHÔNG tự ý "đẻ" thêm các thư mục nghe cho sang trọng (như architecture, flows, design...) nếu nó không thực sự là tài sản chung của team. Những thứ do AI sinh ra hoặc tài liệu cá nhân bắt buộc phải đưa vào thư mục `members/`)*

Hiện tại, không gian chung chỉ công nhận 3 thư mục sau:

*   **`database/`**: Xem lại điều số 1 ở trên.
*   **`management/`**: Trung tâm quản lý dự án. Nơi lưu trữ duy nhất lịch sử thay đổi code và tiến độ của team (bao gồm file bảng `WORK_LOG.md` và thư mục chi tiết `work_logs/`).
*   **`members/`**: 🚷 Không gian nháp cá nhân.
    *   *Ví dụ: `members/linhdn/` là nơi chuyên chứa bằng chứng test (hàng đống file ảnh/json), các script chạy tự động, prototype giao diện, và các tài liệu flows/design ảo do AI sinh ra của riêng bạn linhdn. Khu vực này hoàn toàn KHÔNG ĐẠI DIỆN cho cấu trúc chuẩn của dự án.*

---
> **🚨 TỐI HẬU THƯ:** 
> 1. Không dùng không gian chung để lưu trữ ảnh chụp màn hình test. Git sinh ra để lưu code.
> 2. Mọi tài liệu nháp, hoặc tài liệu do AI "chế" ra mà sai lệch với dự án thực tế, làm ơn ném hết vào thư mục `members/[tên-bạn]/`. Đừng lừa người khác đọc!
