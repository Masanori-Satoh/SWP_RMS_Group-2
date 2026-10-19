# Đặc tả Validation: Form Tạo / Cập nhật Yêu cầu Tuyển dụng (Job Requisition)

- **Phân hệ:** Job Requisition (SRS 5.1.15)
- **Phụ trách:** Nguyễn Huy Hoàng (hoangnh)
- **Bảng liên quan:** `JobRequisition`, `ScreeningCriteria`
- **Nơi cài đặt:** `RequisitionValidator` (server, quyết định cuối cùng) và `static/js/pages/requisition-form.js` (báo lỗi sớm trên trình duyệt, quy tắc viết giống server).
  Các giới hạn số đều là hằng số trong `RequisitionValidator`; controller truyền xuống form (`min` / `max`), JS đọc lại từ form, nên đổi một chỗ là cả 3 nơi đổi theo.

> Tài liệu này mô tả **đúng code hiện tại**. Câu thông báo lỗi được chép nguyên văn từ code.

---

## 1. Nguyên tắc chung

### 1.1 Lưu nháp (`action = draft`) và Gửi duyệt (`action = submit`)

| Loại kiểm tra | Lưu nháp | Gửi duyệt |
| :--- | :---: | :---: |
| Tên vị trí, Số lượng, Hình thức, Mô hình (cột `NOT NULL` trong DB) | Bắt buộc | Bắt buộc |
| Các trường bắt buộc còn lại (đánh dấu `*` trên form) | Bỏ qua | Bắt buộc |
| Định dạng: độ dài, kiểu số, giới hạn số, danh sách giá trị hợp lệ | Áp dụng | Áp dụng |
| Lương tối đa ≥ tối thiểu | Áp dụng | Áp dụng |
| Ngày bắt đầu ≥ hôm nay | Bỏ qua | Áp dụng |
| Ngày bắt đầu ≤ hôm nay + 1 năm | Áp dụng | Áp dụng |
| Dòng tiêu chí trống hoàn toàn | Bỏ qua (không lưu) | Bỏ qua (không lưu) |
| Dòng tiêu chí đã nhập một phần | Phải điền đủ 4 ô | Phải điền đủ 4 ô |
| Có ít nhất 1 tiêu chí, tổng trọng số = 100% | Bỏ qua | Áp dụng |

> Lưu nháp vẫn kiểm tra định dạng và các cột `NOT NULL`, nếu không dữ liệu nháp sẽ gây lỗi ở DB
> và người dùng chỉ thấy câu "Không thể lưu dữ liệu" (NHH §22: không dựa vào lỗi DB để báo lỗi người dùng).

### 1.2 Phía server

- Mọi quy tắc trong tài liệu này đều có trong `RequisitionValidator`. JS chỉ để người dùng thấy lỗi sớm.
- Form **không gửi được** các trường sau (không có trong `@InitBinder`, server tự gán):
  `hiringManagerId`, `recruitmentRound`, `approvalStatus`, `requisitionCode`.
  `departmentId` có trong `@InitBinder` nhưng chỉ dùng cho Giám đốc; vai trò khác bị service ghi đè.
- Mọi chuỗi đều được **trim**; chuỗi chỉ có khoảng trắng coi như để trống (`cleanString`).
- Ô nhiều dòng (Lý do / Mô tả / Yêu cầu): đổi `\r\n` thành `\n` trước khi đếm độ dài (`cleanMultiline`).
  Trình duyệt đếm mỗi lần xuống dòng là 1 ký tự nhưng gửi lên 2 ký tự.
- "Hôm nay" ở server là `LocalDate.now()` (giờ máy chủ); ở JS là ngày theo giờ máy người dùng (không dùng `toISOString()` vì hàm đó trả giờ UTC).
- Giá trị không đổi được sang số / ngày (ví dụ gửi `abc` bằng Postman) → câu lỗi trong `messages.properties`
  (`typeMismatch...`: "Vui lòng nhập số nguyên hợp lệ.", "Vui lòng nhập giá trị số hợp lệ.", "Ngày bắt đầu dự kiến không hợp lệ.").

### 1.3 Trải nghiệm khi có lỗi

- Giữ lại toàn bộ dữ liệu đã nhập, kể cả các dòng tiêu chí.
- Lỗi hiện ngay dưới từng ô và trong khung tổng hợp ở đầu form; cuộn tới lỗi đầu tiên.
- Rời ô (blur): kiểm tra theo luật **gửi duyệt** để người dùng biết sớm ô nào còn thiếu. Lưu nháp vẫn được, vì nút Lưu bản nháp kiểm tra lại theo luật nháp.
- Bấm "Gửi Giám đốc duyệt" → hiện hộp xác nhận; chỉ khi bấm "Gửi duyệt" mới gửi form.
- Sau khi gửi, các nút bị khóa để không gửi 2 lần (mở lại nếu người dùng bấm Back quay lại trang).

---

## 2. Section A: Vị trí cần tuyển

| Field | Bắt buộc | Quy tắc | Thông báo lỗi |
| :--- | :---: | :--- | :--- |
| Tên vị trí | ✔ (cả nháp) | Trim, 1–200 ký tự (`maxlength="200"`) | "Vui lòng nhập tên vị trí." / "Nội dung quá 200 ký tự" |
| Phòng ban | — | Chỉ đọc. Tạo mới: phòng ban của tài khoản đang đăng nhập, phải đang `Active`. Sửa: giữ phòng ban đã lưu của phiếu | "Tài khoản của bạn chưa được gán phòng ban đang hoạt động. Vui lòng liên hệ quản trị viên." |
| Phòng ban (Giám đốc) | Nháp + Gửi | Giám đốc tự chọn trong danh sách phòng ban đang `Active` (tạo và sửa bản nháp của mình) | "Vui lòng chọn phòng ban đang hoạt động." |
| Đợt tuyển dụng | — | Chỉ đọc. Tạo mới = 1; sao chép = đợt lớn nhất trong chuỗi + 1 (mục 8.2) | — |
| Số lượng cần tuyển | ✔ (cả nháp) | Số nguyên 1–30 (`POSITIONS_MIN` / `POSITIONS_MAX`; công ty khoảng 200 nhân sự). Nút −/+ dừng ở 1 và 30 | "Vui lòng nhập số lượng cần tuyển." / "Số lượng cần tuyển phải từ 1 đến 30 người." |
| Loại tuyển dụng | — | `New` / `Replacement`; "Chưa xác định" lưu `NULL`. Không bắt buộc kể cả khi gửi duyệt | "Vui lòng chọn một tùy chọn hợp lệ." |

Ngoài ra (service): không được có **2 bản nháp cùng tên vị trí trong cùng phòng ban**:
"Bạn đang có một bản nháp cho vị trí này trong phòng ban. Vui lòng hoàn thiện bản nháp hiện có."

---

## 3. Section B: Điều kiện làm việc

| Field | Bắt buộc | Quy tắc | Thông báo lỗi |
| :--- | :---: | :--- | :--- |
| Hình thức làm việc | ✔ (cả nháp) | `Full-time` / `Part-time` / `Internship` / `Contract`. Mặc định Toàn thời gian | "Vui lòng chọn một tùy chọn hợp lệ." |
| Mô hình làm việc | ✔ (cả nháp) | `On-site` / `Remote` / `Hybrid`. Mặc định Tại văn phòng | "Vui lòng chọn một tùy chọn hợp lệ." |
| Yêu cầu giới tính | — | `Any` / `Male` / `Female`. Để trống → `Any` (giống DEFAULT của cột) | "Vui lòng chọn một tùy chọn hợp lệ." |
| Địa điểm làm việc | ✔ | Trim, tối đa 255 ký tự | "Vui lòng nhập địa điểm làm việc." / "Nội dung quá 255 ký tự" |
| Ngày bắt đầu dự kiến | ✔ | Gửi duyệt: ≥ hôm nay. Luôn luôn: ≤ hôm nay + 1 năm (`START_DATE_MAX_YEARS`; chặn cả năm gõ nhầm như `20266`). Ô ngày có `min` / `max` | "Ngày bắt đầu dự kiến phải từ hôm nay trở đi." / "Ngày bắt đầu dự kiến không được quá 1 năm kể từ hôm nay." |
| Thời gian thử việc | — | Để trống = không thử việc, hoặc số nguyên 1–180 ngày (Điều 25 BLLĐ 2019). Nút −/+ (ô trống bấm nút thì bắt đầu từ 60). Lưu số nguyên vào cột `ProbationDays INT` | "Thời gian thử việc phải từ 1 đến 180 ngày." |
| Lương tối thiểu / tối đa | — | Xem 3.1 | — |

### 3.1 Quy tắc lương

| # | Quy tắc | Thông báo lỗi |
| :--- | :--- | :--- |
| 1 | Được để trống cả hai ("Thỏa thuận") hoặc chỉ nhập một ô ("Từ X triệu" / "Đến Y triệu") | — |
| 2 | Mỗi ô > 0, tối đa 2 chữ số thập phân | "Mức lương phải lớn hơn 0 với tối đa 2 chữ số thập phân." |
| 3 | Mỗi ô ≤ 1.000.000.000 (`SALARY_MAX`) | "Mức lương không được vượt quá 1.000.000.000 VND/tháng." |
| 4 | Lương tối đa ≥ lương tối thiểu | "Mức lương tối đa phải lớn hơn hoặc bằng mức lương tối thiểu." |

**Định dạng khi nhập:**

- JS tự thêm dấu chấm hàng nghìn khi gõ (`20000000` → `20.000.000`), giữ đúng vị trí con trỏ (chèn số giữa chừng không làm lệch số); ký tự không phải số bị bỏ.
- Server (`@InitBinder`, editor cho `minSalary` / `maxSalary`) đọc `20.000.000` thành 20000000 (số viết liền như `20000000` từ Postman vẫn đọc được), và hiển thị lại dạng `20.000.000` khi form bị render lại do lỗi.

---

## 4. Section C: Nội dung tuyển dụng

| Field | Bắt buộc | Giới hạn | Bộ đếm | Thông báo lỗi |
| :--- | :---: | :---: | :---: | :--- |
| Lý do tuyển dụng | ✔ | 2000 ký tự (cột `NVARCHAR(2000)`) | `/2000` | "Vui lòng nhập trường này." / "Nội dung quá 2000 ký tự" |
| Mô tả công việc | ✔ | 2000 ký tự | `/2000` | "Vui lòng nhập trường này." / "Nội dung quá 2000 ký tự" |
| Yêu cầu ứng viên | ✔ | 2000 ký tự | `/2000` | "Vui lòng nhập trường này." / "Nội dung quá 2000 ký tự" |

- Trim; nội dung chỉ có khoảng trắng coi như để trống. `maxlength="2000"` trên textarea.
- Mô tả / Yêu cầu là `NVARCHAR(MAX) NOT NULL`: bản nháp để trống thì lưu chuỗi rỗng.
- Trang chi tiết hiển thị bằng `th:utext` nhưng **an toàn**: service escape HTML trước (`HtmlUtils.htmlEscape`) rồi mới đổi `\n` thành `<br/>`.

---

## 5. Section D: Tiêu chí sàng lọc hồ sơ

### 5.1 Từng dòng tiêu chí

| Field | Bắt buộc | Quy tắc | Thông báo lỗi |
| :--- | :---: | :--- | :--- |
| Tiêu chí | ✔ | Trim, tối đa 150 ký tự. Không trùng tên trong cùng yêu cầu, so sánh **không phân biệt hoa thường** (khớp collation CI của `UQ_ScreeningCriteria_Requisition_Name`) | "Vui lòng nhập trường này." / "Tên các tiêu chí không được trùng nhau." |
| Loại | ✔ | `Education` / `Experience` / `Skill` / `Knockout` | "Vui lòng chọn một tùy chọn hợp lệ." |
| Giá trị yêu cầu | ✔ | Trim, tối đa 255 ký tự | "Vui lòng nhập trường này." |
| Trọng số (%) | ✔ | 0.01–100, tối đa 2 chữ số thập phân (`DECIMAL(5,2)`), **cả khi lưu nháp** | "Vui lòng nhập trọng số." / "Trọng số phải từ 0.01 đến 100 với tối đa 2 chữ số thập phân." |
| Bắt buộc | — | Có / không | — |

Dòng "trống hoàn toàn" = không có tên, loại, giá trị và không bật Bắt buộc (không xét trọng số). Dòng trống bị bỏ qua, không lưu.

### 5.2 Toàn bộ danh sách

| Quy tắc | Áp dụng | Thông báo lỗi |
| :--- | :---: | :--- |
| Tối đa 50 dòng (`MAX_CRITERIA_LIMIT`, cũng là `autoGrowCollectionLimit` của binder) | Luôn luôn | "Chỉ được thêm tối đa 50 tiêu chí." |
| Có ít nhất 1 tiêu chí và tổng trọng số đúng 100 | Gửi duyệt | "Vui lòng thêm đầy đủ tiêu chí với tổng trọng số đúng bằng 100%." |

**Ghi chú cài đặt:**

- Server cộng tổng bằng `BigDecimal`. JS đổi sang đơn vị 0.01 (`Math.round(x * 100)`) trước khi cộng để tránh sai số số thực.
- Thanh tổng chỉ cộng các dòng đã nhập: vàng = còn thiếu, xanh = đủ 100%, đỏ = thừa.
- Nút **Chia đều trọng số**: dòng cuối nhận phần dư, ví dụ 3 dòng → 33.33 / 33.33 / 33.34.
- Khi sửa: `synchronizeCriteria` cập nhật dòng cũ theo `criteriaId`, xóa dòng bị bỏ, thêm dòng mới; `criteriaId` không thuộc yêu cầu này bị từ chối.

---

## 6. Chống vỡ giao diện khi nhập nhiều dữ liệu

- `maxlength` trên mọi input / textarea khớp giới hạn trong tài liệu này.
- `overflow-wrap:anywhere` cho vùng hiển thị (chuỗi dài không có khoảng trắng).
- Input trong grid / flex: `min-width:0`; textarea `resize:vertical`.
- Bảng tiêu chí: `table-layout:fixed`, bọc trong vùng cuộn ngang khi màn hình hẹp.
- Danh sách yêu cầu không còn cột Người tạo để bảng không phải cuộn ngang trên laptop.

---

## 7. Checklist kiểm thử nhanh

- [ ] Tên vị trí rỗng / chỉ khoảng trắng / 201 ký tự (cả khi lưu nháp)
- [ ] Số lượng: `0`, `-1`, `1.5`, `30`, `31`, để trống
- [ ] Bỏ trống địa điểm rồi rời ô → hiện lỗi ngay; vẫn lưu nháp được
- [ ] Ngày bắt đầu: hôm qua (nháp được, gửi duyệt bị chặn), hôm nay, hôm nay + 1 năm, hôm nay + 1 năm + 1 ngày, năm `20266`
- [ ] Thử việc: trống, `0`, `181`, `60`
- [ ] Lương: chỉ nhập 1 ô; `0`; tối đa < tối thiểu; `1,000,000,001`; gõ `20tr` (JS bỏ chữ, còn `20`)
- [ ] Nhập `20000000` → hiển thị `20.000.000`; đặt con trỏ giữa số rồi gõ thêm → không lệch số, trang chi tiết hiện "20 – ... triệu VND/tháng"
- [ ] Lý do tuyển dụng 2001 ký tự; văn bản nhiều dòng sát 2000 ký tự → lưu được
- [ ] Tiêu chí trùng tên khác hoa thường (`Java` / `java `)
- [ ] Dòng tiêu chí chỉ nhập tên khi lưu nháp → báo thiếu loại / giá trị / trọng số
- [ ] Trọng số `0`, `100.001`, `101`; tổng 99.99 và 100.01
- [ ] Chia đều với 3 và 7 tiêu chí → tổng đúng 100
- [ ] Sửa `departmentId` / `recruitmentRound` / option select bằng DevTools → server bỏ qua hoặc từ chối
- [ ] Bấm "Gửi duyệt" 2 lần thật nhanh → chỉ tạo 1 yêu cầu
- [ ] Có lỗi khi gửi duyệt → dữ liệu và các dòng tiêu chí được giữ nguyên
- [ ] Dán chuỗi 500 ký tự không có khoảng trắng → form, trang danh sách và trang chi tiết không vỡ layout
- [ ] Tài khoản chưa có phòng ban tạo yêu cầu → báo lỗi dễ hiểu
- [ ] Giám đốc tạo yêu cầu: chưa chọn phòng ban → báo lỗi; bấm "Phê duyệt trực tiếp" → phiếu `Approved`, HR tạo tin được ngay
- [ ] Xóa phiếu đợt 1 đã được sao chép sang đợt 2 → báo lỗi, nút Xóa bị ẩn

---

## 8. Ràng buộc dữ liệu: kiểm tra ở tầng ứng dụng (không đổi DB)

> **Quyết định:** không viết migration, không sửa `db.sql`. Các ràng buộc được bản review DB đề xuất đều được
> kiểm tra trong code (validator / service) để người dùng luôn thấy lỗi dễ hiểu, thay vì lỗi từ DB.

### 8.1 Các ràng buộc và nơi kiểm tra

| Ràng buộc | Nơi kiểm tra trong code | Trạng thái |
| :--- | :--- | :---: |
| Mỗi yêu cầu chỉ có 1 tin đang mở (Draft / Published / Paused) | `JobPostingServiceImpl#requireNoOtherOpenPosting` (dùng `JobPostingAccess.OPEN_STATUSES`) | ✅ Có sẵn |
| Lương > 0 | `RequisitionValidator#validateMoneyField` | ✅ Có sẵn |
| Phiếu nào cũng có người tạo (`CreatedBy`) | `RequisitionServiceImpl#createRequisition` luôn gán người đang đăng nhập | ✅ Có sẵn |
| Trọng số mỗi tiêu chí 0.01–100, tối đa 2 chữ số thập phân | `RequisitionValidator#validate`, áp dụng **cả khi lưu nháp** (trước đây nháp cho tới 999.99) | ✅ Đã sửa |
| Tin Closed có `ClosedAt`; tin đã đăng có `PostingDate` | `TalentPoolService#closeJob` gán `ClosedAt`; chỉ đóng được tin Published / Paused; `publishJobPosting` gán `PostingDate` | ✅ Có sẵn |
| Phiếu đã gửi duyệt có `SubmittedAt` | `createRequisition` / `updateRequisition` gán khi `action = submit` | ✅ Có sẵn |
| Mã phiếu không trùng | Sinh từ `RequisitionId` (khóa chính) nên không bao giờ trùng | ✅ Có sẵn |
| Lương ≤ 1.000.000.000 | `RequisitionValidator#validateMoneyField` (`SALARY_MAX`) | ✅ Đã thêm |
| Lương "nhập cả hai hoặc để trống cả hai" | Không áp dụng: được nhập một ô, hiển thị "Từ X" / "Đến Y" | — |
| Ngày bắt đầu ≤ hôm nay + 1 năm | Validator, luôn kiểm tra (chặn năm vượt kiểu `DATE`) | ✅ Đã thêm |
| Phòng ban của phiếu luôn có, đang hoạt động | `createRequisition` kiểm tra phòng ban của người tạo; `updateRequisition` giữ phòng ban cũ | ✅ Đã thêm |
| Ngày bắt đầu ≥ hôm nay | Validator, chỉ khi gửi duyệt (không đưa vào CHECK vì phiếu cũ sẽ không sửa được) | ✅ Có sẵn |

### 8.2 Đợt tuyển và xóa phiếu

- `OriginalRequisitionId` luôn trỏ về **phiếu đợt 1**. Sao chép từ đợt 2 thì lấy phiếu gốc của đợt 2 (`rootOf`).
- Đợt mới = đợt lớn nhất trong chuỗi + 1 (`findMaxRoundInChain`, tính cả mọi trạng thái nên số đợt không bao giờ bị dùng lại).
  Phiếu gốc bị khóa dòng (`findForUpdate`) để hai người sao chép cùng lúc không lấy trùng số đợt.
- Xóa phiếu (`deleteRequisition`): chỉ khi được sửa (Draft / Rejected, người tạo hoặc Admin), và bị chặn khi:
  - đã có tin tuyển dụng: "Yêu cầu này đã được liên kết với tin đăng tuyển và không thể xóa.";
  - **đã có đợt tuyển sau sao chép từ nó**: "Yêu cầu này đã có đợt tuyển sau được sao chép từ nó và không thể xóa." (✅ mới thêm, trước đây gặp lỗi khóa ngoại).
  Nút Xóa trên trang chi tiết cũng ẩn trong 2 trường hợp này (cờ `deletable`).
- Thứ tự xóa: ghi `AuditLog` → xóa `RequisitionApproval` → xóa phiếu (`ScreeningCriteria` xóa theo cascade).

### 8.3 Ghi chú khác

- Default của cột `ScreeningCriteria.Weight` là `1.00`. Form tạo mới hiện `100` ở dòng tiêu chí đầu tiên vì controller (`createForm`) gán sẵn; các dòng thêm sau để trống.
- Tin đang Paused được đăng lại: `publishJobPosting` ghi đè `PostingDate` bằng thời điểm đăng lại, nhưng trước đó kiểm tra hạn nộp phải sau thời điểm hiện tại, nên không vi phạm `CK_JobPosting_Deadline`.
- `PostingNo` = lần đăng lớn nhất của yêu cầu + 1, yêu cầu được khóa dòng khi tạo tin nên không bị trùng.
