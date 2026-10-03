# Mộc Careers — critique và quyết định refinement

Target: `src/main/resources/templates/careers/index.html`, cùng shared header, detail và apply. Đây là baseline trước batch sửa cuối, không phải chứng nhận production.

## Tổng hợp độc lập

Assessment A hoàn tất trước khi root đọc findings B. A đánh giá UX/visual/source/bốn viewport; B chạy detector một lần và browser injection trên ba trang, không đọc A/Gemini. Báo cáo đầy đủ ở [A](2026-10-01-career-assessment-a.md) và [B](2026-10-01-career-assessment-b.md).

**27/40 — Acceptable (67.5%)**, tất cả 10 heuristic áp dụng: status3, match3, control3, consistency3, prevention3, recognition3, efficiency2, aesthetics3, recovery2, help2. Cognitive load: moderate, 2/8 checklist failures (flat groups of six facts/policies/profile details; desktop navigation over four choices). Không tự chấm lại điểm sau polish khi chưa có assessment mới.

Specificity: moderately specific, coherent editorial direction. Warm off-white/forest, Lora/Source Sans 3, project-notes visual, real salary/location/deadline và policy rows tạo một career site cho một công ty. Giữ tất cả; không thêm trang marketplace, metrics, ảnh stock hoặc SaaS decoration.

Emotional journey: đến trang bình tĩnh, lọc/đọc role có thông tin thiết thực; valley ở Apply Now rồi mới thấy submission chưa có. Jordan cần biết hạn chế trước login; Casey cần department hoạt động dễ tìm trên mobile; Sam cần nhận ra fields read-only và trạng thái thiếu dữ liệu.

## Priorities và disposition

| Priority | Finding đã xác nhận | Quyết định trong batch sửa |
|---|---|---|
| P1 | Apply Now hứa submission trước khi thấy backend chưa có | Giữ label theo brief, thêm availability cạnh CTA trước login; process có notice ngắn. Không fake submit. |
| P2 | Mobile strip che Sales & Marketing sau department0 | Real departments có openings đứng trước0, giữ một hàng scroll theo brief; cue chỉ khi thực sự overflow. Không re-sort DOM sau mỗi filter. |
| P2 | Read-only controls giống editable, optional blanks không rõ | Fieldset/legend read-only phía trên; style trung tính, optional placeholder Not provided. Không thêm profile-edit route chưa có. |
| P2 | Browser title literal template expression | Evaluate Thymeleaf fragment args cho detail/apply/unavailable; assertion title trong test hiện có. |
| P3 | Hyphen từ DB kèm native bullet | Dùng paragraph lines giữ nguyên wording/markers từ DB, bỏ bullet do browser thêm. |

Minor dispositions: footer theo auth state; Talent Pool CTA gọi đúng Create a Candidate Account (đích `/register`, chưa có enrollment/talent-pool backend). Giữ role facts sáu trường vì đều có nguồn thật; không bỏ metadata để đạt checklist. Filter history restoration, profile correction, ảnh/product thật và contact chính thức là khả năng/nội dung chưa có, không tự tạo.

B: CLI2 flat-type warnings là false positives theo computed stylesheet (desktop46/34/20/16; mobile32/28/20/16; apply40/20/16). Accepted CLI issues0. Browser detect.js loaded/executed3/3, runtime issues0; headless không chứng minh user-visible overlay. Literal title là bug ngoài detector, được cả A/B xác nhận. Các giới hạn/driver failures và nguồn HTML thực so với fixture xem report B/test doc.

## Những điều còn thiếu

- CV storage/upload, submission transaction, duplicate-application policy và profile correction chưa có backend.
- Company policy copy là fiction được duyệt trước; thương hiệu/contact/legal/product media chính thức cần chủ sản phẩm cung cấp.
- Real HTTP startup đang FAIL do loopback SocketException; MockMvc và static browser preview không thay cho browser E2E.

Questions skipped: 0 quyết định nghiệp vụ mới cần hỏi để hoàn tất batch refinement này; các khoảng trống backend/nội dung đã ghi rõ, controls tương ứng không được bật.
