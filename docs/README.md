# 📚 Trung Tâm Tài Liệu Dự Án (RMS Docs Hub)

Chào mừng bạn đến với trung tâm tài liệu của dự án **Recruitment Management System (RMS)**. Tài liệu này hướng dẫn cách tổ chức, tra cứu và đóng góp tài liệu để đảm bảo không gian làm việc của team luôn đồng bộ, sạch sẽ và chuyên nghiệp.

---

## 1. 🗄️ Phân Định Ranh Giới Database
Dự án áp dụng quy tắc phân tách nghiêm ngặt giữa **Mã nguồn thực thi** và **Tài liệu tham khảo**:

* **`database/` (Tại thư mục gốc của dự án):**
  * **Mục đích:** Lưu trữ mã nguồn khởi tạo cơ sở dữ liệu (`.sql`).
  * **Quy định:** Chỉ chứa các script thực thi như `schema/db.sql` (Source of Truth) và `seeds/seed_data.sql`. Tuyệt đối không lưu trữ tài liệu giải nghĩa (Markdown, PDF, Image) tại đây.
* **`docs/database/`:**
  * **Mục đích:** Lưu trữ tài liệu mô tả thiết kế dữ liệu cho con người và AI đọc hiểu.
  * **Quy định:** Chứa tài liệu [`model.md`](database/model.md) (sơ đồ ERD, mô tả chi tiết bảng/cột) và [`data_seeding_guide.md`](database/data_seeding_guide.md) (hướng dẫn tạo dữ liệu mẫu).

---

## 2. 📂 Cấu Trúc Thư Mục Tài Liệu (`docs/`)

Không gian tài liệu của dự án được quy hoạch thành các phân khu chức năng dưới đây. Vui lòng không tự ý tạo thêm thư mục mới ở cấp gốc của `docs/`:

| Thư mục | Mục đích | Các tệp chính |
| :--- | :--- | :--- |
| **[`architecture/`](architecture/)** | Quy chuẩn kiến trúc hệ thống (backend + tổng quan giao diện) và tiêu chuẩn lập trình | [`ARCHITECTURE_GUIDE.md`](architecture/ARCHITECTURE_GUIDE.md)<br>[`schema-migration-impact.md`](architecture/schema-migration-impact.md)<br>[`UI/`](architecture/UI/README.md): bộ tài liệu giao diện (luật, catalog, tạo trang mới, migrate, CSS, dọn file cũ); bắt đầu từ [`UI/USER_GUIDE.md`](architecture/UI/USER_GUIDE.md) |
| **[`database/`](database/)** | Sơ đồ thiết kế CSDL và hướng dẫn dữ liệu mẫu | [`model.md`](database/model.md)<br>[`data_seeding_guide.md`](database/data_seeding_guide.md) |
| **[`management/`](management/)** | Lưu vết tiến độ, nhật ký thay đổi và kiểm thử | [`WORK_LOG.md`](management/WORK_LOG.md)<br>[`work_logs/`](management/work_logs/) (Chi tiết theo ngày) |
| **[`prototype-reference/`](prototype-reference/)** | Bản nháp HTML/CSS tĩnh ban đầu, chỉ còn giá trị tham khảo lịch sử. **Giao diện chuẩn hiện tại là chính ứng dụng**, theo `ARCHITECTURE_GUIDE.md` mục 6 và [`architecture/UI/`](architecture/UI/README.md) | Các file mockup mẫu ban đầu |
| **[`members/`](members/)** | Không gian lưu trữ tài liệu cá nhân của từng thành viên | [`duc/`](members/duc/), [`dunglt/`](members/dunglt/), [`hoangnh/`](members/hoangnh/), [`huyenpt/`](members/huyenpt/), [`linhdn/`](members/linhdn/) |

---

## 💡 Nguyên Tắc Làm Việc & Đóng Góp Tài Liệu

### 1. Phân định giữa Tài liệu Chính thức và Tài liệu Cá nhân:
* Mọi tài liệu nằm ngoài thư mục `members/` đều được coi là **Tài liệu chính thức**. Thành viên khi cập nhật cần đảm bảo nội dung phản ánh chính xác 100% với mã nguồn hiện tại của dự án.
* Thư mục `members/{member_name}/` là không gian riêng để lưu nháp, work log cá nhân, ghi chú nghiên cứu hoặc bằng chứng kiểm thử riêng biệt.

### 2. Quy trình ghi Log công việc để chống Xung Đột Git (`WORK_LOG.md`):
File `docs/management/WORK_LOG.md` là bảng mục lục chung nên rất dễ xảy ra xung đột khi nhiều người cùng push code cuối ngày. Quy trình bắt buộc:
1. **Viết file chi tiết trước:** Tạo file báo cáo trong thư mục `docs/management/work_logs/` theo định dạng `YYYY-MM-DD-ten-cong-viec.md` (hoặc trong thư mục `docs/members/{name}/`).
2. **Pull nhánh mới nhất:** Trước khi sửa `WORK_LOG.md`, luôn pull code mới nhất từ nhánh chung.
3. **Thêm dòng tham chiếu:** Thêm 1 dòng tóm tắt vào bảng trong `WORK_LOG.md` trỏ liên kết tới file chi tiết vừa tạo.
4. **Xử lý conflict (nếu có):** Tuyệt đối **không** chọn "Accept Current / Accept Yours" làm mất log của đồng đội. Phải chọn "Accept Both" và giữ lại đầy đủ dòng của cả hai bên.

### 3. Tối ưu dung lượng Repository:
* Tuyệt đối không commit các file nặng (video ghi hình, hàng trăm ảnh screenshot uncompressed, file binary tạm) lên repository. Nếu cần đính kèm ảnh kiểm thử, hãy nén dung lượng và đặt trong thư mục cá nhân `members/`.
