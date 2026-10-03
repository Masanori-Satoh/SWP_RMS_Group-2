# 📚 SWP_RMS_Group-2 Documentation

Chào mừng đến với trung tâm tài liệu của dự án. Tài liệu này hướng dẫn cách tổ chức và quản lý các thư mục tài liệu để đảm bảo không gian làm việc của team luôn đồng bộ, sạch sẽ và chuyên nghiệp.

---

## 1. 🗄️ Phân định ranh giới Database
Dự án áp dụng quy tắc phân tách nghiêm ngặt giữa **Mã nguồn thực thi** và **Tài liệu tham khảo**:

*   **`database/` (Thư mục gốc của dự án):**
    *   **Mục đích:** Lưu trữ mã nguồn khởi tạo cơ sở dữ liệu (`.sql`).
    *   **Quy định:** Chỉ chứa các script thực thi. Tuyệt đối không lưu trữ tài liệu giải nghĩa (Markdown, PDF, Image) tại đây.

*   **`docs/database/`:**
    *   **Mục đích:** Lưu trữ tài liệu mô tả thiết kế dữ liệu cho con người đọc.
    *   **Quy định:** Chứa sơ đồ ERD, tài liệu mô tả Model, và hướng dẫn tạo dữ liệu mẫu (Data Seeding Guide).

---

## 2. 📂 Cấu trúc thư mục Docs
Không gian tài liệu chung của dự án được quy hoạch thành các phân khu chức năng dưới đây. Vui lòng không tự ý tạo thêm thư mục mới ở cấp gốc của `docs/` nếu chưa có sự thống nhất chung của team.

*   **`architecture/`**: Sổ tay kiến trúc hệ thống và quy chuẩn lập trình (Coding Standards).
*   **`database/`**: Sơ đồ thiết kế và hướng dẫn dữ liệu (như đã phân định ở trên).
*   **`management/`**: Trung tâm lưu vết tiến độ dự án. Nơi lưu trữ duy nhất lịch sử thay đổi code của team (bao gồm file bảng `WORK_LOG.md` và thư mục chi tiết `work_logs/`). 
*   **`prototype-reference/`**: Thư viện chứa các bản nháp HTML/CSS tĩnh (static prototypes) đóng vai trò làm giao diện tham chiếu chuẩn cho Frontend và AI.
*   **`members/`**: Không gian lưu trữ tài liệu cá nhân của các thành viên.
    *   *Mục đích:* Nơi lưu trữ tài liệu nháp, kịch bản kiểm thử riêng biệt (Test Plans), bằng chứng kiểm thử (Screenshots, JSON logs), hoặc các tài liệu do AI tạo ra đang trong quá trình thử nghiệm.
    *   *Ví dụ:* `members/hoangnh/`, `members/linhdn/`...
    *   *Quy định:* Tài liệu chưa hoàn thiện, sai lệch so với thực tế dự án, hoặc mang tính chất cá nhân bắt buộc phải lưu tại đây để không làm nhiễu tài liệu chính thức.

---

## 💡 Nguyên tắc chung
1. **Tối ưu dung lượng Repo:** Hạn chế tối đa việc commit hàng loạt ảnh chụp màn hình kiểm thử lên repository. Nếu cần thiết lưu vết, hãy đặt chúng vào không gian cá nhân (`members/`) hoặc cấu hình `.gitignore` nếu số lượng quá lớn.
2. **Tính chính xác:** Mọi tài liệu nằm ngoài thư mục `members/` đều được coi là **Tài liệu chính thức**. Thành viên cần đảm bảo nội dung tài liệu luôn phản ánh đúng thực tế của mã nguồn hiện tại.
