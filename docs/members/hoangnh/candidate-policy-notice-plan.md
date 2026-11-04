# Kế hoạch: Ứng viên xác nhận đồng ý Điều khoản & Chính sách bảo mật

**Trạng thái:** Đã code (bước 1–7) · cần báo nhóm: migration 015, `SecurityConfig`, `public-footer.html`, sidebar admin · **Branch:** `feat/hoang-user_consent_management`
**Cập nhật 04/11/2026:** đổi từ "popup chỉ thông báo" sang **popup ghi nhận đồng ý** theo ảnh tham chiếu JobsGo (2 checkbox + nút "Tôi đồng ý"). Lưu bằng **1 cột** trên bảng `Candidate`, không thêm bảng.
**Mockup:** xem link ở mục 9.

## 1. Quyết định đã chốt

| # | Quyết định |
| --- | --- |
| D1 | Hai tài liệu của Mộc: **Điều khoản sử dụng** và **Chính sách bảo mật**, mỗi tài liệu là một file PDF. |
| D2 | Popup có **2 checkbox bắt buộc** ("Tôi đã đọc và đồng ý với …") và nút **"Tôi đồng ý"**. Nút chỉ bật khi tick đủ 2 ô. |
| D3 | Chỉ hiện cho **tài khoản Candidate đã đăng nhập chưa đồng ý**. Khách chưa đăng nhập và vai trò nội bộ không thấy popup. |
| D4 | Popup **không đóng được** khi chưa đồng ý: không có nút ×, Esc không đóng, bấm nền không đóng. Nền mờ phủ cả topbar, nên chân popup có link nhỏ **"Không phải bạn? Đăng xuất"** (POST `/logout` + CSRF) để ứng viên không bị kẹt. |
| D5 | Đồng ý được lưu vào **cột `Candidate.PolicyAgreedAt`** (thời điểm bấm "Tôi đồng ý"). Một lần bấm = đồng ý cả 2 tài liệu. |
| D6 | **Chỉ chặn ở giao diện:** chưa bấm "Tôi đồng ý" thì popup che trang, không xem tiếp được. Không thêm kiểm tra phía server cho nộp đơn hay thao tác khác. |
| D7 | Bấm tên tài liệu mở PDF ở **tab mới** (`/terms`, `/privacy-policy`), xem công khai không cần đăng nhập. Tab hiện tại giữ nguyên trạng thái checkbox. |
| D8 | **Đồng ý một lần là xong.** Mộc thay PDF thì ứng viên không phải đồng ý lại (không lưu phiên bản). |
| D9 | Ảnh JobsGo chỉ là tham chiếu bố cục. Dùng màu, chữ, logo Mộc; không chép chữ/thương hiệu JobsGo. |
| D11 | **System Admin thay 2 file PDF** qua màn `/admin/policy-documents` (làm luôn trong feature này). Thay là ghi đè, không lưu lịch sử bản cũ, ứng viên không phải đồng ý lại. Không thêm bảng. |
| D10 | **Footer giữ nguyên giao diện cũ.** Hai link "Chính sách bảo mật", "Điều khoản sử dụng" vốn đã trỏ `/privacy-policy`, `/terms` → mở **đúng file PDF** như link trong popup. Không sửa markup footer cho phần này. |

## 2. Hiện trạng mã nguồn (đã kiểm tra 04/11)

| Phần | Hiện trạng | Tác động |
| --- | --- | --- |
| Footer `fragments/layout/public-footer.html` | Được gọi ở `candidate/landing.html`, `layout/public.html`, `layout/internal.html` (chỉ khi người dùng là Candidate). Đã có link `/terms`, `/privacy-policy` nhưng **chưa có controller** → hiện 302 về /login. | Một chỗ gắn popup phủ được mọi trang ứng viên. |
| Bảng `Candidate` | Mọi tài khoản Candidate đều có 1 dòng: đăng ký và Admin tạo tài khoản đều gọi `AccountManagementService.newCandidateProfile`. | Thêm 1 cột là đủ, không cần bảng mới. |
| `SecurityConfig` | `GET /`, `/jobs/**` permitAll; `/jobs/*/apply` cần `ROLE_CANDIDATE`; còn lại `authenticated`. | Thêm permitAll cho `GET /terms`, `/privacy-policy`; `POST /policy-consents` cần `ROLE_CANDIDATE`. |
| `TopbarUserAdvice` (core/web, Lead-only) | Cấp `topbarUser` cho mọi view. | **Không sửa.** Tạo advice riêng trong module `career`. |
| `components.css` mục H | Đã có `dialog.confirm-dialog`, `.dialog-header/-body/-actions`. | Tái dùng, chỉ thêm CSS riêng `pages/policy-consent.css`. |
| `candidate/job-detail.html` | Hộp thoại ứng tuyển có `data-open-on-load` (sau đăng nhập quay về `#apply`). | Nếu popup đồng ý đang mở thì hộp ứng tuyển **chờ** tới khi đồng ý xong. |
| Đăng ký `RegisterAccountRequest` | Không có checkbox điều khoản. | Giữ nguyên; tài khoản mới gặp popup ngay lần đầu đăng nhập. |

## 3. Database (sửa `db.sql` + migration `015_candidate_policy_agreed_at.sql`)

```sql
-- NULL = chưa đồng ý → hiện popup; có giá trị = thời điểm bấm "Tôi đồng ý"
IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.Candidate') AND name = N'PolicyAgreedAt')
    ALTER TABLE dbo.Candidate ADD PolicyAgreedAt DATETIME2 NULL;
```

- Mọi ứng viên hiện có sẽ là `NULL` → lần đăng nhập kế tiếp đều thấy popup (đúng ý).
- Seed: để `NULL`, hoặc đặt sẵn giá trị cho tài khoản demo nếu không muốn popup khi demo luồng khác.
- `candidate` là module nền → **báo nhóm** chạy migration (DB dùng chung) và rà chỗ nào dựng `Candidate` bằng builder (cột mới nullable nên không vỡ).
- Không lưu: lịch sử nhiều lần đồng ý, IP, đồng ý riêng từng tài liệu (đã cân nhắc, không cần cho đồ án).

## 4. Backend

| Class | Vai trò |
| --- | --- |
| `candidate/entity/Candidate` | Thêm trường `policyAgreedAt` (`LocalDateTime`, cột `PolicyAgreedAt`). |
| `career/service/PolicyConsentService` | `needsConsent(username)`: ứng viên chưa có `policyAgreedAt`. `recordConsent(username)`: transaction, đặt `LocalDateTime.now(clock)` **chỉ khi đang `NULL`** (bấm 2 lần / 2 tab không ghi đè lần đầu). Dùng `CandidateRepository` (gọi chéo module bằng 1 truy vấn đơn giản, CLAUDE.md §3.1). |
| `career/service/PolicyDocuments` | Hằng số đường dẫn 2 PDF và tên hiển thị tiếng Việt. |
| `career/controller/PolicyDocumentController` | `GET /terms`, `GET /privacy-policy` → `ResponseEntity<Resource>` PDF, `Content-Disposition: inline`. Tên file cố định trong code, không nhận đường dẫn từ request. |
| `career/controller/PolicyConsentController` | `POST /policy-consents` (CSRF) nhận `agreeTerms`, `agreePrivacy`, `returnUrl`. Thiếu ô nào → redirect về `returnUrl` + flash lỗi, không ghi gì. Đủ → `recordConsent`, redirect `returnUrl`. |
| `career/controller/PolicyConsentAdvice` (`@ControllerAdvice`) | `@ModelAttribute("policyConsentRequired")`: `false` ngay nếu không phải `ROLE_CANDIDATE` (đọc authority, không truy vấn DB); ngược lại hỏi `needsConsent`. |

**`returnUrl` an toàn:** chỉ chấp nhận chuỗi bắt đầu bằng `/` và không bắt đầu bằng `//` hoặc `/\`; sai thì về `/jobs`. (Chống open redirect.)

**PDF — đọc từ đâu:** ưu tiên file Admin đã tải ở `{app.storage.local-root}/policy/terms.pdf`, `privacy.pdf`; chưa có thì dùng bản mặc định trong `src/main/resources/policy/` (classpath, **không** nằm trong `static/` để luôn đi qua controller, header đúng). Máy mới clone vẫn mở được, không 404.

## 4b. Màn System Admin — Tài liệu chính sách (D11)

| Phần | Cách làm |
| --- | --- |
| URL | `GET /admin/policy-documents` (danh sách 2 dòng), `POST /admin/policy-documents/{type}` (`type` ∈ `terms`, `privacy`; giá trị khác → 404). `/admin/**` đã giới hạn `ROLE_SYSTEM_ADMIN`, không thêm rule. |
| Class | `admin/controller/PolicyDocumentAdminController`, `admin/service/PolicyDocumentStorage` (đọc/ghi file, dùng chung với `PolicyDocumentController` để cả hai đọc cùng một chỗ). |
| Mỗi dòng | Tên tài liệu, đường dẫn công khai, nhãn **Bản mặc định** / **Đã tải lên**, ngày cập nhật + người cập nhật, nút **Xem PDF** (tab mới) và **Tải bản mới** (mở hộp thoại). |
| Ngày + người cập nhật | Đọc dòng `AuditLog` mới nhất có `EntityName = 'PolicyDocument'`, `EntityId = 'terms' / 'privacy'`. Chưa có dòng nào → "Chưa tải bản nào". Không cần bảng mới. |
| Kiểm tra file | Bắt buộc chọn file; 5 byte đầu là `%PDF-` (không tin đuôi file / Content-Type); ≤ 10MB (giới hạn multipart có sẵn). Lỗi hiện cạnh ô chọn file, không đổi file đang dùng. |
| Ghi file an toàn | Ghi ra file tạm cùng thư mục → `Files.move(..., REPLACE_EXISTING, ATOMIC_MOVE)`. Tên file cố định trong code, không dùng tên người dùng gửi. |
| Audit | `AuditLog` `Action = UPDATE`, `NewValue` = tên file gốc + kích thước. Hiện trong màn "Lịch sử hoạt động" có sẵn. |
| Thông báo | Thành công → toast "Đã thay {tên tài liệu}. Ứng viên thấy bản mới ngay." (`fragments/ui/flash-toast` có sẵn). |
| Menu | Thêm mục **"Tài liệu chính sách"** vào `fragments/layout/sidebars/admin.html` (được phép thêm mục, copy đúng khuôn mục có sẵn). |
| Giao diện | `templates/admin/policy-documents.html`, dùng lại class các màn admin (`ads-*`, `adw-*`); CSS riêng `static/css/pages/policy-documents.css` nếu cần. |

**Hiệu năng:** advice chạy mỗi trang của Candidate = 1 truy vấn đọc cột. Chấp nhận ở quy mô đồ án.

## 5. Giao diện

### 5.1. Vị trí

- Fragment mới `templates/fragments/ui/policy-consent-dialog.html` (UI_RULES §2B: được thêm fragment mới).
- Gọi 1 dòng trong `public-footer.html`: `<th:block th:if="${policyConsentRequired}" th:replace="~{fragments/ui/policy-consent-dialog :: dialog}"/>`. Giao diện footer không đổi (D10).
  ⚠ `public-footer.html` là khung dùng chung → **báo Lead (Dũng LT) trước**, PR nhỏ riêng.
- CSS: `static/css/pages/policy-consent.css` (tiền tố `.policy-consent-*`). JS: `static/js/pages/policy-consent.js` (bật/tắt nút, chặn Esc, mở `showModal()` khi tải trang).

### 5.2. Bố cục (theo ảnh, đổi sang DNA Mộc)

```text
┌──────────────────────────────────────────────┐
│ [nền --brand]          🔔                    │
│      Thông báo & Xác nhận đồng ý             │
│      Vui lòng đọc kỹ trước khi tiếp tục      │
├──────────────────────────────────────────────┤
│ Chào mừng bạn đến với Mộc Tuyển dụng!         │
│ Để tiếp tục sử dụng dịch vụ, vui lòng xác     │
│ nhận bạn đã đọc và đồng ý với:                │
│ ┌──────────────────────────────────────────┐ │
│ │ ☐ Tôi đã đọc và đồng ý với Điều khoản  ↗ │ │
│ │   sử dụng.                               │ │
│ └──────────────────────────────────────────┘ │
│ ┌──────────────────────────────────────────┐ │
│ │ ☐ Tôi đã đọc và đồng ý với Chính sách  ↗ │ │
│ │   bảo mật.                               │ │
│ └──────────────────────────────────────────┘ │
│ 🛡 Mộc cam kết bảo vệ dữ liệu cá nhân của bạn │
│   và chỉ sử dụng trong phạm vi đã đồng ý.    │
│            [ ✓ Tôi đồng ý ] (tắt → bật)       │
│   Không phải bạn? Đăng xuất                   │
└──────────────────────────────────────────────┘
```

- Checkbox: cả hàng là `<label>` (bấm chỗ nào cũng tick); link tài liệu nằm trong nhãn nhưng bấm link **không** đổi checkbox.
- Nút "Tôi đồng ý" `disabled` + `aria-disabled` tới khi đủ 2 ô; gợi ý nhỏ "Hãy tick cả 2 ô để tiếp tục" bên dưới.
- Lỗi từ server (flash) hiện `.alert` trong popup.
- Mobile ≤ 480px: popup rộng 100% − 32px, thân cuộn được, nút full width.
- Truy cập: `aria-labelledby` tiêu đề, `aria-describedby` đoạn mô tả, focus vào checkbox đầu khi mở, focus bị giữ trong popup (do `<dialog>` modal lo), icon ↗ có `aria-label="mở ở tab mới"`.

## 6. Luồng chính

1. Candidate đăng nhập → bất kỳ trang nào có footer (landing, /jobs, /jobs/{id}, /portal/...) → advice báo `policyConsentRequired=true` → popup tự mở.
2. Bấm "Điều khoản sử dụng" → PDF mở tab mới. Quay lại tab cũ, checkbox vẫn còn.
3. Tick đủ 2 ô → "Tôi đồng ý" → `POST /policy-consents` → lưu `PolicyAgreedAt` → redirect đúng trang đang xem → không còn popup, kể cả lần đăng nhập sau.
4. Khách bấm "Ứng tuyển" → đăng nhập → về `/jobs/{id}#apply`: popup đồng ý mở trước; đồng ý xong mới mở hộp ứng tuyển.

## 7. Thứ tự thực hiện

| Bước | Việc | Ghi chú |
| --- | --- | --- |
| 0 | GV/nhóm duyệt mockup (cả 2 màn Admin); Lead đồng ý sửa 1 dòng `public-footer.html` + rule Security; người phụ trách sidebar admin đồng ý thêm mục menu. | Bắt buộc trước khi code. |
| 1 | `db.sql` + migration 015 + trường `policyAgreedAt` trên entity `Candidate`. | Báo nhóm chạy migration (DB dùng chung). |
| 2 | `GET /terms`, `/privacy-policy` + rule permitAll + 2 PDF tạm. | Test: khách mở được, đúng header. |
| 3 | `PolicyConsentService` + unit test. | |
| 4 | `POST /policy-consents` + advice + web test. | |
| 5 | Fragment + CSS + JS, gắn vào footer. | Kiểm tra landing, job board, job detail, portal. |
| 6 | Màn Admin (mục 4b): storage, controller, template, mục menu, audit + test. | `GET /terms` chuyển sang đọc qua `PolicyDocumentStorage`. |
| 7 | Chạy `mvnw clean test`, kiểm tra tay theo mục 8, ghi `checklist.md`, commit 1 lần. | |

## 8. Kiểm thử & nghiệm thu

| Tình huống | Kết quả mong đợi | Tầng test |
| --- | --- | --- |
| Candidate chưa đồng ý mở `/jobs` | Popup mở, nút tắt | Web test (model `policyConsentRequired=true`) + tay |
| Tick 1 ô | Nút vẫn tắt | Tay / JS |
| POST thiếu `agreePrivacy` (gửi tay) | Không lưu, flash lỗi | Web test, verify `never()` gọi `recordConsent` |
| POST đủ 2 ô | `PolicyAgreedAt` = giờ `Clock` | Unit test |
| POST lần 2 khi đã đồng ý | Không đổi thời điểm lần đầu | Unit test |
| `returnUrl=//evil.com` hoặc `https://…` | Redirect `/jobs` | Web test |
| Đã đồng ý | Không popup | Unit test |
| Khách, HR, Admin mở trang công khai | Không popup, advice không truy vấn DB | Web test |
| Khách mở `/terms` | 200, `application/pdf`, `inline` | Web test |
| Cột mới đọc/ghi đúng trên SQL Server | Entity khớp `db.sql` (`ddl-auto=validate`) | Integration test |
| Màn ≤ 480px, chỉ dùng bàn phím | Thao tác được hết, không thoát popup bằng Esc | Tay |
| Admin tải PDF hợp lệ | File được thay, `/terms` trả bản mới, có 1 dòng AuditLog | Service test + web test |
| Admin tải file `.docx` đổi đuôi thành `.pdf` | Báo lỗi "File không phải PDF", file cũ giữ nguyên, không audit | Service test, verify `never()` ghi |
| Admin gửi file rỗng / không chọn file | Báo lỗi bắt buộc, không đổi gì | Web test |
| `POST /admin/policy-documents/abc` | 404 | Web test |
| HR / Candidate gọi `/admin/policy-documents` | 403 | Web test |
| Chưa ai tải file | `/terms` trả bản mặc định trong classpath | Unit test |

## 9. Mockup & mở rộng sau

- **Mockup:** https://claude.ai/artifact/VQsXNPkYHWwYrFGwLSoeys — 5 màn: (1) lần đầu, desktop, bấm thử được; (2) mobile 390px; (3) footer giữ giao diện cũ, 2 link mở cùng PDF với popup; (4) Admin – Tài liệu chính sách; (5) Admin – tải bản mới, lỗi sai định dạng.
- **Nếu sau này muốn ứng viên đồng ý lại khi Admin thay file:** thêm `POLICY_EFFECTIVE_DATE` vào `SystemConfig`, popup hiện lại khi `PolicyAgreedAt` < ngày đó — vẫn không cần bảng mới.

## 10. Ngoài phạm vi / câu hỏi mở

- Không soạn nội dung pháp lý; Mộc cung cấp 2 PDF.
- Không thêm checkbox ở form đăng ký (popup đã phủ lần đăng nhập đầu). Nếu GV yêu cầu đồng ý ngay khi đăng ký → thêm 2 ô vào `register.html`, dùng lại `recordConsent`.
- **Rút lại đồng ý** (quyền theo Nghị định 13/2023) — chưa làm; cần nhóm quyết có đưa vào iter này không.
