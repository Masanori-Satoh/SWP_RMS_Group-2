> Ghi chú 11/11/2026: đường dẫn và package test đã được cập nhật theo tầng kiểm thử; kết quả Task/PR lịch sử giữ nguyên.

> Ghi chú 11/11/2026: đường dẫn JavaScript đã được cập nhật theo vị trí hiện hành; kết quả thực hiện lịch sử không thay đổi.

# Tổng kết thay đổi chưa PR — Offer Management

Ngày lập: **11/11/2026** — Người phụ trách: **HuyenPT**.

## 1. Phạm vi đối chiếu và trạng thái GitHub

| Nội dung | Giá trị tại thời điểm lập báo cáo |
| --- | --- |
| Repository | `Masanori-Satoh/SWP_RMS_Group-2` |
| Nhánh | `fix/huyenpt/project-test-errors` |
| Worktree thực hiện | `scratch/project-test-errors-worktree` |
| Base `origin/main` đã fetch lại | `054c73d2c9183119393aad986883d3d52ce67b71` |
| Snapshot chức năng còn giữ sau hoàn tác seed | `44fcc15326890e9d437a02363c67c89dcfdae589` |
| HEAD nhánh trên remote | `35c16a87739c260aceb3059a64510c78af446fc1` |
| Diff chức năng tìm kiếm trước khi chuyển CSS | **10 file, +286/-44 dòng**, chưa tính chính báo cáo này; phần CSS bổ sung ở mục 2.5 |
| PR của nhánh | PR #171 đã merge; chưa tìm thấy PR mới cho phần bổ sung dưới đây |

Đã kiểm tra GitHub: [PR #171 — Fix/huyenpt/project test errors](https://github.com/Masanori-Satoh/SWP_RMS_Group-2/pull/171) đã merge vào main ngày 09/11/2026. PR đó chứa thay đổi giới hạn export chỉ cho HR (`7dcfa794`), **không phải nội dung mới cần PR lại**. Hiện `OfferController.java` không có diff so với main.

Commit `7dcfa794` vẫn xuất hiện trong lịch sử riêng của nhánh do cách merge lịch sử; chỉ nhìn danh sách commit chưa đủ để kết luận thay đổi chưa vào main. Báo cáo dùng diff thực tế và trạng thái PR để tách phạm vi. Các chức năng còn giữ tương ứng **4 commit công việc** ở mục 3; commit tài liệu và hoàn tác không phải tính năng mới.

Các commit tìm kiếm đến `35c16a87` đã có trên remote; `44fcc153` còn local tại thời điểm kiểm tra remote. Phần seed ở `7fb29794` đã được hoàn tác theo yêu cầu, không còn thuộc diff hiện tại. Chưa push, tạo PR hoặc merge trong lần cập nhật báo cáo này.

## 2. Nội dung thay đổi

### 2.1. Tìm kiếm dùng chung HR, Director và Excel

- Tách quy tắc keyword vào `OfferProposalRepository.matchingKeyword`, dùng lại trong bộ lọc HR/Excel và danh sách Director, giảm logic lặp ở service.
- Chuẩn hóa khoảng trắng; giới hạn keyword 120 ký tự; escape `%`, `_`, `[` theo tiện ích `ListFilters` hiện có.
- Tìm trên tên ứng viên, email, vị trí đề xuất và phòng ban theo OR. Dùng collation `Latin1_General_100_CI_AI` tại biểu thức query để hỗ trợ không phân biệt hoa/thường và dấu; không thay đổi collation/schema database.
- `#12` tìm chính xác OfferId 12; số thường `12` vẫn là keyword văn bản. Mã vượt giới hạn integer trả rỗng.
- Giữ nguyên phân quyền, phạm vi xem của Director, trạng thái, xóa mềm và sắp xếp.
- Hai template chỉ cập nhật placeholder và thêm `maxlength="120"`; không thêm thanh lọc, CSS hoặc bố cục mới.

### 2.2. Xuất Excel theo bộ lọc đã áp dụng

- FILTERED đọc `search`, `status`, `timeSort` từ URL hiện tại, không lấy nội dung toolbar đang gõ dở.
- Ví dụ URL đã áp dụng keyword Java nhưng người dùng mới gõ QA chưa tìm: Excel vẫn xuất theo Java, khớp danh sách đang xem.
- FILTERED không bị giới hạn bởi trang hiện tại; SELECTED/ALL giữ nguyên. Quyền export chỉ HR theo phần đã merge từ PR #171.

### 2.3. Tìm kiếm lương bằng định dạng đang hiển thị

- Nhận `36.000.000 VNĐ`, `36.000.000 VND`, `36.000.000`; cũng nhận số có đơn vị như `36000000 VNĐ`.
- So sánh BigDecimal chính xác với lương chính thức **hoặc** lương thử việc; không chuyển cột lương thành chuỗi để LIKE.
- Hỗ trợ khác biệt hoa/thường và khoảng trắng khi copy. Số trần `36000000` vẫn tìm như keyword; định dạng tiền sai hoặc câu trộn chữ dùng nhánh tìm văn bản.
- Tiền vượt 16 chữ số nguyên của decimal(18,2) trả rỗng. Không bổ sung tìm khoảng lương hoặc cú pháp điều kiện.
- Áp dụng cùng logic cho danh sách HR, Director và Excel FILTERED, không sửa UI.

### 2.4. Phần seed đã thu hồi

Theo yêu cầu ngày 11/11/2026, toàn bộ thay đổi ở `7fb29794` đã được hoàn tác: generator, SQL và hướng dẫn seed trở lại đúng nội dung trước commit; xóa tài liệu fixture demo. Không còn phần bổ sung ứng viên demo trong phạm vi PR.

Kiểm tra thực tế phát hiện dữ liệu demo đã được nạp vào database. Sau khi người dùng duyệt riêng, đã sao lưu và xóa đúng 16 tài khoản/Candidate/Application/Offer cùng dữ liệu phụ thuộc trong transaction trên `RitirementManagement2`. Còn 0 tài khoản demo; so sánh snapshot SHA-256 trên 19 bảng xác nhận mọi bản ghi ngoài phạm vi không đổi. Không reset database, sửa schema hoặc reseed. Backup và kết quả nằm local trong scratch, không đưa vào Git.

### 2.5. Đưa CSS Offer trực tiếp vào pages — 11/11/2026

- Di chuyển bằng Git 3 file `offer-common.css`, `offer-hr.css`, `offer-director.css` vào `src/main/resources/static/css/pages/`; gỡ thư mục con rỗng. SHA-256 trước/sau khớp 3/3, không sửa bất kỳ rule CSS nào.
- Đổi đúng 8 href trong 4 template `offers/list.html`, `form.html`, `detail.html`, `director-list.html`. Giữ nguyên thứ tự và cách ghép head hiện tại, không thêm styles fragment gây nạp lặp.
- Cập nhật đường dẫn trong 10 tài liệu HuyenPT có tham chiếu cũ, ghi chú đường dẫn đã chuyển; giữ các kết quả Task/PR lịch sử. Không sửa tài liệu thành viên khác.
- Bổ sung 3 tình huống HTTP: tài nguyên mới trả 200, đúng MIME và byte; URL cũ trả 404. Bốn template render 7 stylesheet duy nhất, common trước CSS vai trò, không còn đường dẫn thư mục con trong HTML.
- Maven sau thay đổi: **310/310 test Offer đạt** (OfferIntegrationTests tăng từ 65 lên 68); frontend **23/23 đạt**. Không đổi backend nghiệp vụ, JavaScript, database, exception hoặc shared layout.
- Browser xác nhận 4 trang và popup HR/Director trên HTML Thymeleaf với service mock, dùng CSS/JS thật; CSS mới được browser đọc rule thành công. Đây là fixture, không phải E2E đăng nhập/database. Bằng chứng ở `scratch/offer-search-upgrade-20261110/css-move-browser/`.
- Đã phát hiện 3 CSS build cũ trong `target/classes`; chuyển riêng các bản có hash khớp sang `scratch/offer-search-upgrade-20261110/css-move-stale-build-backup/`, không xóa nội dung hoặc clean toàn bộ target. Các artifact khác được giữ nguyên.
- Quét toàn bộ file Git quản lý bằng git grep và kiểm tra đường dẫn trong source/test/tài liệu: không còn tham chiếu thư mục CSS con cũ. Log/cache/snapshot lịch sử ngoài Git được giữ làm bằng chứng, không thuộc nội dung runtime hoặc commit.
- Manifest lần chuyển CSS: 3 rename CSS, 4 template, 1 file test, 10 tài liệu tham chiếu và báo cáo này. Snapshot sau lần chuyển CSS gồm 26 file; lần chuyển JavaScript bổ sung ở mục 2.6. Bảng mục 4 chỉ là snapshot phần tìm kiếm trước các lần chuyển tài nguyên.

### 2.6. Đưa JavaScript Offer trực tiếp vào pages — 11/11/2026

- Thư mục nguồn thực tế có 4 file: `offer-common.js`, `offer-director.js`, `offer-form.js`, `offer-hr.js`. Chuyển cả 4 bằng git mv vào `src/main/resources/static/js/pages/`, bỏ thư mục con rỗng; giữ nguyên byte nội dung, hash khớp 4/4.
- Cập nhật 7 script src trong 4 template, giữ nguyên defer, thứ tự common trước script vai trò và điều kiện chỉ Director nạp script quyết định. Sửa đường dẫn đọc file trong test Java/Node.
- Cập nhật 13 tài liệu của HuyenPT (gồm báo cáo này) có đường dẫn JavaScript cũ. Kết quả thực hiện Task/PR lịch sử được giữ nguyên, chỉ cập nhật đường dẫn và ghi chú.
- Bổ sung 4 kiểm tra HTTP: đường dẫn mới 200 đúng MIME/byte, URL cũ 404. Bốn template render đúng script, không lặp, giữ thứ tự; hồi quy **314/314 Java Offer và 23/23 JavaScript đạt**.
- Browser fixture xác nhận script mới trên 4 trang; popup HR/Director mở được, form gợi ý 85% cho 36.000.000 ra 30.600.000. Không submit tạo/sửa/gửi/duyệt dữ liệu. Bằng chứng trong `scratch/offer-search-upgrade-20261110/js-move-browser/`; không phải E2E đăng nhập/database.
- Giữ 4 bản JS build cũ trong `scratch/offer-search-upgrade-20261110/js-move-stale-build-backup/` để không còn tài nguyên cũ trong classpath; không clean toàn bộ target hoặc xóa bản lưu.
- Quét toàn bộ file Git quản lý: không còn tham chiếu thư mục JS con cũ, đồng thời không tái xuất hiện tham chiếu CSS con cũ. Log/snapshot lịch sử ngoài Git giữ nguyên.
- Phạm vi lần chuyển JS: 4 rename JS, 4 template, 2 file test và 13 tài liệu; không đổi nội dung JS, CSS, backend, database hoặc module khác. Snapshot sau lần chuyển JS là 34 file; tổ chức lại test bổ sung ở mục 2.7.

### 2.7. Đặt test theo tầng kiểm thử — 11/11/2026

- Chuyển `CandidateOfferWebTests.java` và `OfferIntegrationTests.java` vào `src/test/java/com/group2/rms/offer/controller/`; chuyển `OfferExportServiceTests.java` vào `src/test/java/com/group2/rms/offer/service/`.
- Giữ nguyên tên lớp, method, annotation, fixture, mock và assertion; mỗi file chỉ đổi dòng package. Hai lớp WebMvcTest thuộc controller, lớp Mockito xuất Excel thuộc service; không tạo thêm integration folder chỉ vì tên lớp.
- Cả 8 file test Offer có package khớp đường dẫn; không còn test Java nằm trực tiếp ở root package Offer. Không đổi cấu hình Surefire hoặc production.
- Cập nhật đường dẫn/package tham chiếu trong 19 tài liệu HuyenPT, ghi chú việc chuyển; giữ kết quả lịch sử. Tài liệu của thành viên khác không thay đổi.
- Ba nhóm class build ở package cũ được chuyển riêng sang `scratch/offer-search-upgrade-20261110/test-package-stale-build-backup/` để tránh JUnit chạy cả lớp cũ và mới; không xóa các artifact khác.
- Chạy lại 5 lớp Offer bằng tên package đầy đủ mới: **314/314 đạt**, không failure/error/skipped. Surefire xác nhận CandidateOfferWebTests 19 test, OfferIntegrationTests 72 test, OfferExportServiceTests 15 test ở package mới; hai lớp service còn lại 46 và 162 test. Log: `scratch/offer-search-upgrade-20261110/test-package-move.log`; không chạy test database opt-in hoặc kết nối/ghi database. Không chạy lại Node vì không đổi JavaScript/test frontend.
- Manifest lần này: 3 rename test và 19 tài liệu. Sau bổ sung, diff tổng branch so với main là **48 file**; phần lớn file tăng thêm là cập nhật đường dẫn trong tài liệu lịch sử.

### 2.8. Rà soát cuối — Phase 1: phạm vi đọc của Director

- Dùng một specification `visibleToDirector` cho danh sách, đọc theo ID (HTML/JSON) và bộ đếm trạng thái: loại xóa mềm/Draft; cho xem mọi Pending_Director hoặc thư mời có lịch sử quyết định của Director hiện tại.
- Đọc nội bộ kiểm tra tài khoản và vai trò: HR giữ phạm vi hiện có, Director áp dụng query giới hạn bản ghi, các vai trò khác bị từ chối. Bản ghi ngoài phạm vi trả ResourceNotFoundException/404, không tiết lộ sự tồn tại qua URL trực tiếp.
- Dùng EXISTS thay vì join lịch sử để không nhân đôi hàng và không làm sai phân trang/count. Không đổi endpoint, UI, lifecycle, quyền ghi hoặc schema; Candidate portal tiếp tục dùng truy vấn riêng theo chủ sở hữu.
- Sửa test template trước đây cho phép Director xem Draft: dùng Pending hợp lệ; thêm test 404 HTML/JSON với hai tài khoản Director Active và test service với hai Director ID khác nhau. Kiểm tra Draft, lịch sử của người khác, lịch sử của mình, các trạng thái sau gửi, vai trò không thuộc Offer và phạm vi các bộ đếm.
- Bộ kiểm thử mở rộng: **337/337 đạt** thuộc 5 lớp Offer, không failure/error/skipped; tăng 23 test so với baseline 314. Không chạy các test database opt-in hoặc gửi email.
- Probe JPA chỉ đọc SQL Server: so khớp danh sách/count/chi tiết với SQL tham chiếu cho Director có lịch sử (29 dòng) và một danh tính không có lịch sử (4 Pending); Draft không truy cập được. Bộ đếm trạng thái của service khớp các bản ghi được phép xem trong cả hai trường hợp. Đây là kiểm tra trên dữ liệu local, không phải hai tài khoản Director thực được tạo mới. Không chạy ứng dụng, job hoặc DDL/DML.
- Log nằm tại `scratch/offer-search-upgrade-20261110/phase1-director-access-verified.log` và `phase1-director-sql-verified.log`; probe/helper giữ ngoài Git. Không kết luận toàn bộ test dự án đều đạt.
- Phạm vi commit Phase 1: repository/service Offer, hai file test Offer và báo cáo này. Phase 2–6 chưa thuộc lần chỉnh sửa này: phân trang/lương, giới hạn nhận xét, xử lý exception, endpoint trùng và chuẩn hóa frontend.

### 2.9. Rà soát cuối — Phase 2: phân trang và độ chính xác lương

- Chuẩn hóa `size` về 1–50 trước khi tạo PageRequest ở cả danh sách HR và Director; mặc định vẫn là 10, page âm vẫn về 0. Mức trần 50 phù hợp giới hạn các màn danh sách Requisition/Job Posting hiện có, không sửa code dùng chung.
- Bổ sung `@Digits(integer = 16, fraction = 2)` cho lương chính thức và thử việc trong cả CreateOfferRequest/UpdateOfferRequest, khớp cột decimal(18,2) hiện tại. Giữ kiểm tra số dương và tỷ lệ 85%–100%; từ chối độ chính xác vượt giới hạn thay vì để database làm tròn hoặc báo tràn số.
- Validation sai trả lại form cùng dữ liệu đã nhập và lỗi tiếng Việt; không gọi service tạo/sửa. Không thay bố cục, HTML/CSS/JS, schema hoặc dữ liệu database; không sửa Application hay module khác.
- Bổ sung 20 trường hợp MockMvc: 8 giá trị size từ Integer.MIN_VALUE đến Integer.MAX_VALUE cho cả hai vai trò, 8 trường hợp lương vượt độ chính xác qua form tạo/sửa, 4 trường hợp lương có hai chữ số thập phân hoặc đạt giới hạn tối đa được truyền nguyên vẹn tới service.
- Kiểm thử đạt **357/357** thuộc 5 lớp Offer, không failure/error/skipped (94 Web/MVC, 183 service Offer, 46 service Candidate, 19 web Candidate, 15 export). Log: `scratch/offer-search-upgrade-20261110/phase2-pagination-salary-verified.log`. Đây là MockMvc/Bean Validation và hồi quy service, không phải E2E đăng nhập/database; không lưu lương biên vào database thực. Lần Maven sandbox đầu dừng khi truy cập dependency jboss-logging trước test; lần chạy lại có quyền truy cập cache đạt BUILD SUCCESS. JaCoCo vẫn có cảnh báo dữ liệu cũ Notification, không dùng coverage làm bằng chứng. Không cần chạy lại Node vì không đổi frontend. Phase 3–6 chưa thực hiện trong lần này.

### 2.10. Rà soát cuối — Phase 3: thống nhất nhận xét Giám đốc

- Popup danh sách và ô nhận xét trang chi tiết cùng dùng maxlength 2000, hiển thị bộ đếm /2000; giữ cấu trúc và thiết kế hiện có. DTO dùng từ “Giám đốc” giống service; giới hạn service/DTO đã là 2000, không đổi nghiệp vụ.
- Thêm 10 trường hợp MockMvc: duyệt/từ chối với 500, 501, 2000 ký tự được chuyển nguyên vẹn tới service; 2001 bị Bean Validation chặn trước service; hai trang render maxlength và bộ đếm thống nhất. Các test service hiện có tiếp tục chặn 2001 nếu gọi trực tiếp.
- Hồi quy đạt **367/367 Java Offer và 23/23 Node**, log `phase3-director-comments.log` / `phase3-frontend.log` trong scratch/offer-search-upgrade-20261110. Không ghi database, không thay module khác; không kết luận E2E trình duyệt hoặc toàn bộ test dự án. Giữ cảnh báo coverage Notification cũ như các phase trước.

### 2.11. Rà soát cuối — Phase 4: xử lý exception đúng tầng

- OfferController chỉ bắt OfferValidationException; CandidateOfferController chỉ bắt OfferResponseException. Bỏ catch Exception/BaseBusinessException ở controller để ResourceNotFoundException/AccessDeniedException/lỗi hệ thống tới GlobalExceptionHandler, trả đúng 404/403/500 thay vì chuyển thành form HTTP 200, redirect hoặc 500 sai loại.
- Lỗi nhập liệu/nghiệp vụ vẫn giữ form và nội dung đã nhập; JSON nghiệp vụ giữ ApiResponse 400 và 409 khi quyết định cũ. Không sửa core handler; với lỗi không thể tiếp tục, AJAX nhận trang lỗi theo cơ chế chung và helper hiện tại xử lý mã HTTP, không cố đọc HTML thành JSON.
- Giữ catch bảo vệ tác vụ email/thông báo sau commit trong service; lỗi tác vụ phụ không đổi kết quả nghiệp vụ đã lưu. Không thay quyền, trạng thái, endpoint, dữ liệu hoặc module khác.
- Kiểm thử đạt **379/379 Java Offer, 25/25 Node**. Ma trận lỗi mô phỏng kiểm tra 9 đường nội bộ và 3 đường Candidate cho từng mã 403/404/500, không có flash thành công và không lộ thông tin kỹ thuật; kiểm tra riêng JSON nghiệp vụ 400/409 và giữ form tạo/sửa. Các GET HTML bị cấm vẫn theo cơ chế điều hướng chung, ma trận GET dùng Accept JSON để xác minh mã lỗi gốc.
- Log `phase4-exception-handling.log` / `phase4-frontend.log` trong scratch/offer-search-upgrade-20261110. Test dùng service mock hoặc unit test, không phải E2E database; không ghi DB. Giữ cảnh báo JaCoCo Notification cũ.

### 2.12. Rà soát cuối — Phase 5: một endpoint cho mỗi thao tác xóa/phát hành

- Bỏ POST /offers/{id}/delete và handler form phát hành trùng POST /offers/{id}/send. Giữ DELETE /offers/{id} và một handler phát hành trả JSON; form tạo/sửa giữ nguyên. Không sửa SecurityConfig dùng chung; rule bảo vệ URL cũ còn lại không tạo endpoint thực thi mới.
- Chuyển logic xác nhận/xóa từ offer-hr.js sang offer-common.js để cả danh sách và chi tiết dùng cùng API, CSRF, toast, khóa nút. Fragment xóa dùng button thống nhất, không còn form xóa riêng. Xóa ở chi tiết thành công về /offers để không reload bản ghi đã xóa; danh sách reload giữ bộ lọc, thất bại ở lại trang và mở khóa nút.
- Đạt **379/379 Java Offer và 27/27 Node**, log `phase5-single-endpoints.log` / `phase5-frontend.log` trong scratch/offer-search-upgrade-20261110. Test URL xóa form cũ trả 404, không gọi service; gửi không có Accept vẫn trả JSON từ đúng một handler; quyền và ma trận lỗi tiếp tục đạt. Hai test Node thêm kiểm tra xóa tại chi tiết và chặn bấm lặp.
- Không thay trạng thái nghiệp vụ, database hoặc module khác; kiểm chứng bằng MockMvc/service mock và Node, chưa là E2E trình duyệt/database. Các tài liệu/test manual dùng POST xóa cũ cần chuyển sang DELETE; lịch sử báo cáo trước đây giữ nguyên.

### 2.13. Rà soát cuối — Phase 6: sự kiện frontend, cảnh báo chưa lưu và CSS

- Chuyển 54 thuộc tính sự kiện inline trong 5 template/fragment Offer sang data-offer-click/change/input và listener trong 4 script hiện có. Điều phối chung chỉ xử lý phần tử trong .ofw-page, dùng danh sách hàm rõ ràng, không eval chuỗi HTML; giữ thứ tự common trước HR/Director. Form tự gắn listener riêng, không nạp thêm common hoặc tạo file mới.
- Bỏ các phép gán window.<hàm> dư thừa sau khi template không còn gọi inline; giữ các hàm hiện có và thứ tự nạp. Comment đường dẫn CSS/route cũ được sửa. JavaScript tăng ròng 23 dòng để có điều phối và bảo vệ dữ liệu, không tuyên bố toàn bộ JS ngắn hơn; số file không tăng.
- Form chỉnh sửa lấy snapshot sau khởi tạo, loại CSRF khỏi so sánh; beforeunload chỉ cảnh báo thay đổi chưa lưu. Dữ liệu không đổi/đã hoàn tác không cảnh báo, submit hợp lệ không bị chặn; pageshow khôi phục cảnh báo khi quay lại bằng lịch sử. Dùng hộp thoại native của trình duyệt, không thêm UI/popup riêng. Form tạo mới giữ hành vi hiện tại.
- Giới hạn nhóm selector .btn-icon và .card-header-flex vào :where(.ofw-page), giữ độ ưu tiên CSS và giá trị cũ, không tác động topbar/layout chung. Không đổi thiết kế, nghiệp vụ, backend hoặc database.
- Đạt **379/379 Java Offer và 35/35 Node**. Assertion sự kiện inline chỉ kiểm tra nội dung Offer; các nút đăng xuất của topbar chung giữ nguyên. Test export nạp common trước HR và mô phỏng meta CSRF đúng DOM mới; hai lỗi fixture ban đầu được sửa ở test, không sửa code dùng chung.
- Edge headless trên HTML MockMvc/Thymeleaf và CSS/JS thật đạt 6 nhóm: CSS A/B ba trang giữ kích thước/màu nút icon và card header; popup export/chọn cột/xóa/chọn hàng HR; Director từ chối bắt buộc lý do, bộ đếm/payload 2000 và mở khóa khi lỗi; xác nhận/hủy tại chi tiết; gợi ý 85%; rời form sửa cảnh báo và hoàn tác cho phép rời trang. Không có pageerror từ script Offer.
- Log: `phase6-final-verification.log`, `phase6-frontend.log`, `phase6-browser-verified.log`; HTML, ảnh và results.json tại scratch/offer-search-upgrade-20261110/phase6-browser. Browser chỉ dùng fixture, request quyết định được intercept; không đăng nhập E2E hoặc ghi DB. beforeunload phụ thuộc hỗ trợ/chính sách của trình duyệt; không bảo đảm phục hồi dữ liệu khi ứng dụng/trình duyệt bị tắt đột ngột. JaCoCo vẫn có warning Notification cũ, không dùng coverage làm bằng chứng.

### 2.14. Sửa P2 — Phase 1: trạng thái hết hạn hiệu lực khi đọc

- Thêm matchingStatus(status, now), dùng chung với matchingFilters cho HR/Excel, bộ lọc và bộ đếm Director. Expired gồm trạng thái đã lưu hoặc Sent_Candidate đã quá ResponseDeadline/ngày bắt đầu; Sent_Candidate chỉ còn các bản còn hạn. Guard NULL rõ ràng, bằng đúng hạn/ngày bắt đầu hôm nay vẫn hợp lệ.
- Bỏ fast-path HR dùng query trạng thái lưu riêng; giữ sắp xếp mặc định ID tăng dần và scope Director hiện có. List dùng cùng một now cho query và responseExpired, không cập nhật OfferStatus trong database khi đọc.
- Export dùng bean Clock chung, một now cho query/nhãn/tổng hợp; cả ALL/SELECTED/FILTERED hiển thị trạng thái hiệu lực, không sửa entity. Ngày tên file cũng lấy Clock để test ổn định. Không đổi Application, quyền Candidate, email hoặc job.
- Đạt **392/392 Java Offer** (thêm 13 trường hợp workbook/summary trước/đúng/sau hạn, NULL, trạng thái kết thúc, các scope). Cập nhật mock query/Clock và chữ ký helper trong một test opt-in để toàn bộ test source biên dịch; không bật opt-in ghi database.
- JPA/SQL Server chỉ đọc với CTE VALUES: **88 đối chiếu, 22 tình huống**, so khớp helper query với isResponseExpiredAt trước/sau mô phỏng job; không tạo bảng hoặc UPDATE. Probe dữ liệu local với Clock vượt một deadline: HR/Director/counts/Excel khớp, Sent còn 1, Expired 6 trong snapshot kiểm tra. Scope Director và chặn Draft tiếp tục đạt.
- Log: p2-phase1-complete.log, p2-phase1-sql-pass.log, p2-phase1-live-readonly.log tại scratch/offer-search-upgrade-20261110. Helper CTE/probe ngoài Git; không khởi động job, không đổi đồng hồ máy, schema hoặc dữ liệu. Chưa kiểm thử E2E thao tác ghi; JaCoCo vẫn cảnh báo dữ liệu cũ Notification.

### 2.15. Sửa P2 — Phase 2: chặn offset phân trang vượt giới hạn JPA

- Sau khi chuẩn hóa size 1–50/page âm, Controller nhân page × size bằng long. Offset vượt Integer.MAX_VALUE được đưa về page 0, báo bằng flash-toast error hiện có qua model errorMessage; không tạo truy vấn offset vượt giới hạn. Không sửa layout/component hoặc thêm UI mới.
- Giữ keyword, trạng thái, sort và size đã chuẩn hóa. Các page/offset còn hợp lệ không bị đổi, kể cả giá trị sát giới hạn. Thêm 8 trường hợp tham số, mỗi trường hợp kiểm cả HR/Director: âm, 0, bình thường, sát/vượt ngưỡng và Integer.MAX_VALUE với các size khác nhau.
- Đạt **400/400 Java Offer**, log p2-phase2-pagination.log tại scratch/offer-search-upgrade-20261110; kiểm tra thông báo thực trong HTML, model bộ lọc và Pageable tới service đều đúng. Không ghi database, không đổi lifecycle/quyền hoặc module khác; không tuyên bố E2E thao tác ghi.

## 3. Các commit công việc mới

| Commit | Nội dung | Diff riêng commit |
| --- | --- | --- |
| `444979a3` | Dùng chung keyword; Excel lấy bộ lọc trên URL | 6 file, +76/-25 |
| `3fffedad` | Tìm mã, email, phòng ban và hỗ trợ không dấu | 5 file, +64/-30 |
| `35c16a87` | Kiểm tra toolbar/phân trang, báo cáo bằng chứng | 2 file, +100/-0 |
| `44fcc153` | Tìm chính xác lương theo định dạng tiền | 4 file, +62/-5 |

Các phase có sửa trùng file; không cộng số file hoặc số dòng từng commit thành diff cuối branch. Các merge commit chỉ cập nhật main, không được tính là tính năng Offer mới.

## 4. Danh sách file trong diff thực tế

Đường dẫn bên dưới tương đối từ root repository, thống kê phần còn giữ theo `git diff --numstat origin/main...44fcc153`.

| File | + | - | Nội dung |
| --- | ---: | ---: | --- |
| `src/main/java/com/group2/rms/offer/repository/OfferProposalRepository.java` | 36 | 9 | Quy tắc keyword và lương dùng chung |
| `src/main/java/com/group2/rms/offer/service/OfferServiceImpl.java` | 2 | 22 | Director dùng lại specification, bỏ imports/logic thừa |
| `src/main/resources/static/js/pages/offer-hr.js` | 5 | 4 | FILTERED export lấy bộ lọc URL |
| `src/main/resources/templates/offers/list.html` | 1 | 1 | Gợi ý ô tìm và maxlength cho HR |
| `src/main/resources/templates/offers/director-list.html` | 1 | 1 | Gợi ý ô tìm và maxlength cho Director |
| `src/test/java/com/group2/rms/offer/service/OfferServiceTests.java` | 96 | 0 | Keyword, wildcard, #ID, lương và giới hạn |
| `src/test/java/com/group2/rms/offer/controller/OfferIntegrationTests.java` | 41 | 3 | Payload frontend, HTML theo vai trò, bộ lọc khi phân trang |
| `src/test/java/com/group2/rms/offer/service/OfferExportServiceTests.java` | 7 | 4 | Workbook FILTERED với keyword lương |
| `src/test/js/offer-frontend.test.cjs` | 24 | 0 | Scope export và input chưa áp dụng |
| `docs/members/huyenpt/offer-search-upgrade-20261110.md` | 73 | 0 | Các phase tìm kiếm, bằng chứng và giới hạn kiểm chứng |
| **Tổng trước báo cáo này** | **286** | **44** | **10 file** |

Snapshot trước lần chuyển CSS gồm 5 file production của ứng dụng Offer, 4 file test, 1 tài liệu; cộng báo cáo này là 11 file. Các lần chuyển CSS/JS và tổ chức lại test nằm ở mục 2.5–2.7; manifest hiện tại là 48 file. Không có diff seed hoặc hướng dẫn seed so với main.

## 5. Kết quả kiểm chứng đã thực hiện

| Kiểm tra | Kết quả và giới hạn |
| --- | --- |
| Maven sau bổ sung tìm lương | **307/307 đạt**: OfferServiceTests 162, OfferIntegrationTests 65, OfferExportServiceTests 15, CandidateOfferServiceTests 46, CandidateOfferWebTests 19. Là 5 lớp Offer được chọn, không phải toàn bộ dự án. |
| JavaScript frontend | **23/23 đạt**, bao gồm các scope export và nội dung chưa áp dụng khác URL. |
| SQL chỉ đọc của đợt tìm kiếm | 9 tình huống SELECT đạt: tên/phòng ban không dấu, email, vị trí, wildcard literal, số thường, #ID, không có kết quả, chuỗi giống SQL injection. Không kiểm chứng nhánh tìm lương qua SQL thực trong đợt này. |
| Browser HR/Director | HTML từ MockMvc/Thymeleaf với service mock, CSS/JS thật; toolbar giữ bố cục, Director không có export modal; payload thực lấy bộ lọc URL dù ô nhập đang khác. Chưa là E2E trên ứng dụng thật có đăng nhập. |
| Hoàn tác seed | Ba file seed/hướng dẫn khớp snapshot trước `7fb29794`; tài liệu demo được xóa. |
| Dọn database đã được duyệt | Đã backup và xóa 16 tài khoản demo cùng dữ liệu phụ thuộc; giữ nguyên mọi bản ghi ngoài phạm vi trên 19 bảng; không đổi identity counter hoặc schema. |
| Git | `git diff --check origin/main...HEAD` đạt trước khi thêm báo cáo này; báo cáo tiếp tục được kiểm tra whitespace trước commit. |

Log local nằm trong `scratch/offer-search-upgrade-20261110`: `salary-search-final.log`, `salary-search-node.log`, `sql-read-only-results.json`, `browser-export-payload.json`, thư mục `browser/`; backup dọn demo là `removed-offer-demo-backup-20261111-020529.xml`, kết quả `removed-offer-demo-result.json`. Giữ nguyên log lịch sử trong scratch/target, không đưa vào commit. Không chạy lại Maven cho lần hoàn tác seed/tài liệu vì không đổi mã ứng dụng.

## 6. Rủi ro, giới hạn và bước trước PR

- Chưa xác minh end-to-end nhánh tìm lương bằng ứng dụng thật kết nối database; cần restart đúng worktree, thử các định dạng tiền và đối chiếu Excel FILTERED.
- Query nhiều trường có thêm công việc. Đo SQL tương đương trên 8 dòng local: median trước 1,764 ms, sau 2,198 ms; không phải SQL Hibernate đo thực tế và không đủ để kết luận hiệu năng dữ liệu lớn. Không tuyên bố tìm kiếm mới nhanh hơn.
- JaCoCo có warning execution data cũ của lớp Notification; test đạt nhưng báo cáo này không dùng coverage làm bằng chứng.
- Phần seed bổ sung đã thu hồi khỏi file và database; không chạy schema hoặc seed lại. Giữ backup ngoài Git cho khả năng khôi phục dữ liệu nếu cần.
- Chưa kiểm tra merge thử với main trong lần lập báo cáo; không suy ra không conflict chỉ từ `diff --check`. Main được fetch lại để chốt base, chưa merge thêm.
- Giữ nguyên thư mục project gốc đang có conflict tài liệu; mọi công việc ở worktree riêng nêu trên. Không reset, clean hoặc đổi credentials. Thay đổi database chỉ gồm lần dọn nhóm demo đã được duyệt riêng.
- Khi chuẩn bị PR: kiểm tra main/remote lại, push các commit local theo yêu cầu riêng, kiểm tra mergeability và CI; không đưa scratch, target, cấu hình local hoặc mật khẩu vào PR.

## 7. Nội dung PR đề xuất

**Title:** `feat(offer): unify keyword and salary search and align filtered exports`

**Mô tả ngắn:** Nâng cấp ô tìm kiếm Offer hiện có để dùng chung quy tắc cho HR, Director và Excel, hỗ trợ tên/email/vị trí/phòng ban không dấu, #ID và số tiền định dạng VNĐ/VND. Sửa export FILTERED để khớp bộ lọc đã áp dụng; giữ bố cục và phân quyền hiện tại. Đã đạt 307 test Java Offer và 23 test frontend. Không có thay đổi seed hoặc schema trong diff PR.

**Bổ sung mô tả sau chuyển CSS:** Đặt 3 stylesheet Offer trực tiếp trong css/pages, cập nhật template và tài liệu liên quan; giữ nguyên nội dung CSS, thứ tự nạp và giao diện. Kết quả kiểm thử mới nhất là 310 test Java Offer và 23 test frontend đạt.

**Bổ sung mô tả sau chuyển JS:** Đặt 4 script Offer trực tiếp trong js/pages, cập nhật tất cả tham chiếu template/test/tài liệu; giữ nguyên nội dung JS, defer, thứ tự nạp và điều kiện theo vai trò. Kết quả mới nhất là 314 test Java Offer và 23 test frontend đạt.

Phạm vi PR không bao gồm thay đổi đã merge ở PR #171, không đổi schema, nghiệp vụ Application hoặc cấu trúc layout chung.
