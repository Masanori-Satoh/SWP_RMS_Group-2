# Hướng dẫn code chatbot tuyển dụng — giai đoạn 1

> **Trạng thái triển khai (2026-10-22):** Tài liệu bên dưới là hướng dẫn thiết kế ban đầu. Widget đang dùng `fragments/rms-chat.html`, `static/js/rms-chat.js` và `static/css/rms-chat.css`, gọi trực tiếp `POST /api/chatbot/message` qua `fetch`; không gọi webhook n8n. Hợp đồng request thực tế là `message` hoặc `jobPostingId`, response thực tế ở `ChatResponse.java`. Hội thoại chỉ giữ trong trang đang mở, xóa khi tải lại trang; không lưu vào DB hay `sessionStorage`.

Đọc cùng [chatbot_plan.md](chatbot_plan.md). Tài liệu này là thứ tự triển khai từng hàm cho chatbot theo luật. Phạm vi: chỉ đọc dữ liệu, không thêm bảng, không dùng LLM/API key, không lưu lịch sử trên server.

Nguồn đối chiếu: `ARCHITECTURE_GUIDE.md`, `UI/README.md`, `UI_RULES.md`, `UI_CATALOG.md`, `UI_CSS_GUIDE.md`, `CLAUDE.md` của HoangNH và mã hiện tại ở `career`, `candidate`, `jobposting`, `core`. File ngoài repo `C:/Users/Admin/Downloads/UXUI/Rule thiết kế UI UX.md` là đặc tả thiết kế do Hoang soạn: dùng cho trải nghiệm chat (mobile-first, trạng thái, bàn phím, câu chữ), còn class/token và quyền sửa file lấy theo tài liệu UI của team và code đang chạy. Nếu các kích thước/màu trong đặc tả khác token hiện tại, không hard-code màu mới.

## 1. Những điểm cần chốt trước khi triển khai

1. Wireframe widget theo `CLAUDE.md` cần được duyệt. `layout/public.html` và `SecurityConfig` là file chung; đề xuất thay đổi nhỏ để Lead duyệt/gắn. Không mở `/api/chatbot/**` hàng loạt: giai đoạn 1 chỉ cần `POST /api/chatbot/message` công khai.
2. Landing `/` dùng `candidate/landing.html` độc lập; `/jobs` và `/jobs/{id}` dùng `layout/public.html`; `/portal/applications` dùng `layout/internal.html`. Muốn widget hiện trên cả ba nhóm trang phải gắn fragment vào đúng các nơi này. Không tự sửa layout chung khi chưa thống nhất với Lead.
3. `CareerPortalService.getPublishedJobs(keyword, departmentId, employmentType, pageable)` chưa lọc địa điểm. `JobPostingRepository.findPublishedJobs` chỉ tìm từ khóa trong tiêu đề. Không lọc địa điểm sau khi phân trang vì sẽ bỏ sót kết quả; thêm truy vấn đọc riêng hoặc mở rộng bằng overload có kiểm thử, giữ nguyên chữ ký đang được các trang khác dùng.
4. `JobPosting.isVisibleToCandidates(now)` chỉ kiểm tra `Published` và ngày đăng; truy vấn công khai còn kiểm tra hạn nộp. Mọi danh sách job chatbot phải áp dụng đủ ba điều kiện như truy vấn công khai. `getPublishedJobDetail` hiện dùng điều kiện hiển thị và có thể trả tin hết hạn với cờ không còn nhận đơn; thống nhất rõ hành vi chi tiết với trang `/jobs/{id}`.
5. `MyApplicationService.list(null)` lấy đơn theo người đăng nhập; Guest không được gọi hàm này. `CurrentUserService.requireUser()` có thể ném `AccessDeniedException` cho Guest; kiểm tra đăng nhập và vai trò Candidate trước, rồi mới gọi.
6. `SystemConfig` có bảng/entity nhưng chưa có repository hay 4 khóa chatbot. Seed mới không tự cập nhật database hiện hữu. Chuẩn bị dữ liệu cấu hình thực tế, nhất là email/số điện thoại HR; không dùng địa chỉ ví dụ. Theo quy tắc dự án, trao đổi trước khi cập nhật dữ liệu DB. Không sửa schema giai đoạn 1.

## 2. Hợp đồng API nhỏ nhất

`POST /api/chatbot/message` nhận JSON, trả `ApiResponse<ChatResponse>` theo `ARCHITECTURE_GUIDE.md`.

```java
record ChatRequest(
    @Size(max = 500) String message,
    ChatbotIntent intent,
    Integer jobPostingId,
    String location
) {}

record ChatJobResponse(Integer id, String title, String department,
                       String location, String url) {}
record ChatQuickReplyResponse(String label, ChatbotIntent intent) {}
record ChatContactResponse(String email, String phone) {}
record ChatResponse(String reply, List<ChatJobResponse> jobs,
                    List<ChatQuickReplyResponse> quickReplies,
                    String actionUrl) {}
```

`message` hoặc `intent` phải có ít nhất một giá trị; kiểm tra chéo này ở validator, vì `@Size` cho phép `null`. `jobPostingId` phục vụ nút xem chi tiết; không cố đoán ID từ văn bản tự do. `location` là bộ lọc tùy chọn từ widget; thêm `@Size` theo giới hạn chốt cho trường này. Chỉ chấp nhận enum intent hợp lệ; JSON sai hoặc message > 500 ký tự trả 400. Không nhận `candidateId`, `userId`, trạng thái hay URL tùy ý từ trình duyệt. `actionUrl` do server tạo từ các đường dẫn cố định (`/login`, `/jobs/{id}`, `/portal/applications`).

**Quy tắc lỗi JSON hiện tại:** `GlobalExceptionHandler` trả `ApiResponse(false, ...)` cho lỗi `@Valid` và JSON sai trên `/api/`. Ngược lại, `BaseBusinessException` hiện render trang `error/400`, kể cả URL API. Vì vậy kiểm tra chéo bằng Bean Validation ở cấp record (ví dụ method `@AssertTrue isValidCombination()`); nếu service vẫn cần ném `ChatbotValidationException` để tự bảo vệ, controller chỉ bắt đúng exception này và trả `ResponseEntity.badRequest().body(new ApiResponse<>(false, ...))`. Không sửa `GlobalExceptionHandler` chỉ để phục vụ chatbot và không để frontend đoán lỗi từ HTML.

## 3. Thứ tự code từng hàm

### Bước A — DTO và phân loại intent

1. `ChatbotIntent` (`chatbot/service`): các giá trị `FAQ`, `SEARCH_JOB`, `JOB_DETAIL`, `CHECK_STATUS`, `RECOMMEND`, `GUIDANCE`, `CONTACT_HR`, `UNKNOWN` như kế hoạch. Nhánh hỗ trợ lỗi nộp đơn có thể đi qua `GUIDANCE` và FAQ.
2. `ChatbotIntentResolver.resolve(String message, ChatbotIntent selectedIntent) -> ChatbotIntent`: nếu nút gợi ý gửi `selectedIntent` hợp lệ thì ưu tiên nó; nếu không, chuẩn hóa văn bản (trim, chữ thường, bỏ dấu tiếng Việt) rồi xét từ khóa theo thứ tự rõ ràng: trạng thái hồ sơ, gợi ý việc, chi tiết việc, tìm việc, hướng dẫn, liên hệ, FAQ, cuối cùng `UNKNOWN`. Tránh quy tắc đơn lẻ `"hồ sơ" -> CHECK_STATUS` vì "cách nộp hồ sơ" phải ra `GUIDANCE`. Hàm thuần, không gọi DB. Test câu có dấu/không dấu, câu rỗng, ý định trùng từ khóa và intent do nút gửi.
3. `ChatbotRequestValidator.normalizeAndValidate(ChatRequest request) -> ChatRequest` (`chatbot/validator`): trim `message`/`location`, kiểm tra ít nhất message hoặc intent, `JOB_DETAIL` phải có `jobPostingId > 0`, và các giới hạn sau chuẩn hóa. DTO dùng Bean Validation cho từng trường; validator kiểm tra lại khi service được gọi ngoài HTTP. Không xử lý nghiệp vụ ở controller. Nếu validator ném `ChatbotValidationException`, controller phải chuyển đúng exception này thành JSON 400 như ghi ở mục 2.

### Bước B — cấu hình và FAQ

4. `SystemConfigRepository extends JpaRepository<SystemConfig, String>` trong `admin/repository`: chỉ cần `findById(configKey)`; không tự tạo truy vấn thừa.
5. `ChatbotFaqService.isEnabled() -> boolean`: đọc `CHATBOT_ENABLED`, thiếu/sai giá trị thì coi là tắt. Nếu muốn demo bật bot, chuẩn bị giá trị này trong seed và DB đang chạy.
6. `ChatbotFaqService.answer(String normalizedQuestion) -> Optional<String>`: đọc `CHATBOT_FAQ`, parse JSON bằng `ObjectMapper` thành cấu trúc có `keywords/question/answer`; tìm keyword phù hợp, trả câu trả lời tĩnh. JSON lỗi phải có fallback an toàn và log đủ để sửa cấu hình, không trả stack trace ra API. Đặt giới hạn số mục/độ dài answer nếu nội dung do admin sửa.
7. `ChatbotFaqService.contact() -> ChatContactResponse`: đọc `HR_CONTACT_EMAIL` và `HR_CONTACT_PHONE`; trả các giá trị đã cấu hình, không bịa thông tin liên hệ. Thiếu cả hai thì trả hướng dẫn liên hệ qua trang tuyển dụng hoặc câu báo chưa cấu hình.

### Bước C — tìm và xem tin tuyển dụng

8. Tạo một hàm đọc `findChatbotJobs(keyword, location, now, pageable)` ở tầng `career`/`jobposting` sau khi thống nhất với chủ module. Truy vấn phải lọc `Published`, ngày đăng <= `now`, hạn nộp >= `now` và `workLocation` nếu có; phân trang trước khi map sang `ChatJobResponse`. Tiêm `Clock` vào service gọi truy vấn để test biên ngày. Escape `%`, `_`, `[`, `\` trong điều kiện `LIKE` theo `ListFilters` hiện có; không cho người dùng thay đổi mẫu lọc bằng ký tự đặc biệt. Nếu map trường phòng ban từ entity, tải quan hệ `requisition.department` trong transaction, tránh N+1. Không sửa chữ ký `getPublishedJobs(...)` đang có.
9. `ChatbotService.searchJobs(ChatRequest request) -> ChatResponse`: gọi hàm tìm ở bước 8 với giới hạn cố định (ví dụ 5), map sang ID, tiêu đề, phòng ban, địa điểm, `/jobs/{id}`; không trả entity JPA hoặc mô tả HTML. Không có kết quả thì trả câu gợi ý đổi từ khóa/địa điểm.
10. `ChatbotService.jobDetail(Integer jobPostingId) -> ChatResponse`: dùng `CareerPortalService.getPublishedJobDetail(id, usernameOrNull)` và chỉ lấy trường công khai cần thiết; job không thấy/không còn hiển thị thì trả câu không tìm thấy, không lộ Draft/Closed. Nút “Xem chi tiết” dẫn `/jobs/{id}`; nếu hiển thị hạn nộp thì dùng cờ nhận đơn của DTO.

### Bước D — chức năng riêng của Candidate

11. `ChatbotService.candidateOrLogin(Authentication authentication)`: chỉ dùng `Authentication` để nhận biết Guest/anonymous; Guest trả `ChatResponse` có `/login`, không gọi `requireUser()`. Với người đã đăng nhập, lấy `User` từ `CurrentUserService.requireUser()` rồi kiểm tra `CurrentUserService.hasRole(user, "Candidate")`. Internal role không được đọc đơn Candidate, trả thông báo quyền phù hợp. Không tự truy vấn `UserRepository` trong controller hoặc tin `userId` từ request.
12. `ChatbotService.checkStatus(Authentication authentication) -> ChatResponse`: sau guard bước 11, gọi `MyApplicationService.list(null)`; chỉ map tối đa một số đơn mới nhất sang tiêu đề, `statusLabel`, `statusNote`, `lastActivityAt` và link `/portal/applications`; không đưa điểm AI, ghi chú nội bộ hay dữ liệu người khác. Không có đơn thì gợi ý `/jobs`.
13. Thêm truy vấn/projection lịch sử ứng tuyển của chính user ở `ApplicationRepository` nếu cần phòng ban/địa điểm của job đã nộp. Hàm ví dụ `findChatbotAppliedJobsByUserId(Integer userId)` phải có điều kiện `candidate.account.userId = :userId` và formal filter như `findOwnedByUserId`; projection gồm job ID, department ID, workLocation. Không lấy `userId` từ request.
14. `JobRecommendationService.recommendForCurrentCandidate() -> List<ChatJobResponse>`: lấy user ID từ `CurrentUserService`, lấy profile các job đã nộp, truy vấn job công khai cùng phòng ban **hoặc** cùng địa điểm, bỏ ID đã nộp, sort mới nhất, lấy tối đa 5. Nếu lịch sử rỗng, dùng `CareerPortalService.getLatestJobs(5)`. Tránh lấy một trang job đầu rồi lọc trong Java, vì có thể trả thiếu 5 job phù hợp. Giữ transaction read-only và map DTO trước khi ra controller.
15. `ChatbotService.recommend(Authentication authentication) -> ChatResponse`: chạy guard bước 11; gọi bước 14 và tạo câu trả lời/nút xem job. Nếu không có job phù hợp, giải thích và gợi ý tìm việc.

### Bước E — điều phối và HTTP

16. `ChatbotService.reply(ChatRequest request, Authentication authentication) -> ChatResponse`: `normalizeAndValidate` → kiểm tra cờ bật → `resolver.resolve` → `switch` gọi đúng hàm B/C/D. `GUIDANCE` trả các bước mở `/jobs`, xem tin, đăng nhập Candidate, nộp CV, xem `/portal/applications`; `CONTACT_HR` lấy cấu hình; `FAQ` lấy answer; `UNKNOWN` trả quick replies và Contact HR. Không có nhánh nào ghi đơn, gửi email hay đổi trạng thái.
17. `ChatbotApiController.message(@Valid @RequestBody ChatRequest request, Authentication authentication) -> ResponseEntity<ApiResponse<ChatResponse>>`: chỉ gọi service và bọc chuẩn API; chỉ bắt `ChatbotValidationException` nếu validator ở bước 3 ném nó, rồi trả JSON 400. Đăng ký **đúng** `POST /api/chatbot/message` là `permitAll` trong `SecurityConfig`, trước `anyRequest().authenticated()`; không tạo route history/server trong giai đoạn 1. `SecurityConfig` hiện bỏ kiểm tra CSRF cho `/api/**`, nhưng chuẩn AG vẫn yêu cầu AJAX gửi CSRF header.

### Bước F — widget và lịch sử trong tab

18. `chatbot-widget.html`: tra `UI_CATALOG.md` trước khi dựng nút, form, dialog và alert. Widget dùng class `chatbot-` có tiền tố riêng; CSS trong `static/css/pages/chatbot-widget.css` dùng token, không hex, không ghi đè `.btn`/`.card`, không inline script/style. Nút mở, hộp thoại có heading, vùng tin nhắn `aria-live="polite"`, input có label hiển thị, quick replies, nút đóng/xóa. Nút chỉ có icon cần `aria-label` và tooltip; Esc đóng chat rồi trả focus về nút mở; vùng bấm tối thiểu 44×44px trên mobile. Chuẩn bị trạng thái đang tải, trống, không có kết quả, lỗi có nút thử lại và thành công; CSS tôn trọng `prefers-reduced-motion`. Một fragment được dùng lại cho `/`, `/jobs`, `/jobs/{id}` và trang Candidate cần thiết; thay đổi layout chung do Lead duyệt/gắn. Landing độc lập cần nạp CSS/JS và CSRF meta cho AJAX nếu chưa có.
19. `initChatbotWidget()` trong `chatbot-widget.js`: bắt sự kiện mở/đóng, submit/Enter, click quick reply, click job link; vô hiệu hóa nút gửi khi đang chờ; báo lỗi mạng bằng tiếng Việt.
20. `sendChatMessage(payload)`: đọc `meta[name="_csrf"]` và `meta[name="_csrf_header"]`; gửi header CSRF cùng `Content-Type: application/json` theo AG, cả khi API hiện được miễn CSRF. `fetch('/api/chatbot/message', ...)` xử lý `ApiResponse.success`, JSON 400 và lỗi mạng/server; chỉ dựng text bằng `textContent`, không dùng `innerHTML` cho reply hoặc dữ liệu job.
21. `loadHistory()`, `appendHistory(entry)`, `clearHistory()`: dùng `sessionStorage`, tối đa số tin nhắn cố định, bắt lỗi storage bị chặn; xóa chỉ ở tab hiện tại. Không lưu thông tin trạng thái đơn nếu tài khoản có thể đổi trong cùng tab: đặt key theo user phiên hiện tại do server cung cấp hoặc xóa khi đổi đăng nhập/đăng xuất. Không quảng bá đây là lịch sử lưu DB.

## 4. Kiểm thử theo từng cụm

- Sau A: `ChatbotIntentResolverTest` cho từng intent, ưu tiên nút, câu tiếng Việt không dấu, xung đột “nộp hồ sơ”, unknown, >500 ký tự ở tầng DTO/API.
- Sau B/C: unit test cấu hình thiếu/JSON sai, job Published và còn hạn, job Draft/Closed/hẹn đăng/hết hạn, tìm theo địa điểm có phân trang đúng. Truy vấn JPQL mới cần integration test trên SQL Server vì dự án không dùng H2.
- Sau D: Guest hỏi trạng thái/gợi ý trả link login; Internal role không đọc đơn; Candidate A chỉ thấy đơn của A; đề xuất bỏ job đã nộp, giới hạn 5, lịch sử rỗng trả latest.
- Sau E/F: `@WebMvcTest` với SecurityConfig thật cho Guest POST 200, payload >500 ký tự 400, intent Candidate của Guest không 500; kiểm tra DOM `/`, `/jobs`, `/jobs/{id}` và nơi Candidate đã chọn trên `localhost:8088`, cả desktop/mobile. Test thủ công đủ 11 use case của `chatbot_plan.md`.

## 5. Cụm commit gợi ý

1. `feat(chatbot): add phase-one request models and intent resolver` — A + unit tests.
2. `feat(chatbot): add configuration FAQ and public job answers` — B/C + truy vấn/test.
3. `feat(chatbot): add candidate status and job recommendations` — D + test quyền/phạm vi.
4. `feat(chatbot): expose guest message API and widget` — E/F + web test + phần tích hợp layout được Lead duyệt.

Sau mỗi cụm, cập nhật `docs/members/hoangnh/checklist.md` theo `CLAUDE.md`; chỉ đánh dấu hoàn tất khi đã kiểm thử. Chạy test tập trung trước, rồi `./mvnw -o clean test` khi môi trường SQL Server local sẵn sàng.
