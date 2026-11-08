# Tài liệu và hướng dẫn luồng Offer

**Nhánh:** `docs/huyenpt/offer-workflow-review`.
**Base:** main sau PR #163, commit `1324a1d6`.

Đây là PR cuối của đợt tách thay đổi Offer. Các PR code #160, #161, #163 đã merge; PR hiện tại chỉ bổ sung Markdown trong thư mục tài liệu HuyenPT, không đổi production, test, database, migration, quyền hoặc cấu hình job.

## Nội dung

- Đưa 46 tài liệu còn lại từ nhánh Offer gốc vào Git: 38 báo cáo Task 01–25C và tám tài liệu audit/học nghiệp vụ/bàn giao/kiểm chứng.
- Giữ nguyên ba báo cáo PR đã có trên main, không xóa theo diff của nhánh cũ.
- Thêm README để phân biệt hướng dẫn hiện hành và bằng chứng lịch sử. Báo cáo cũ được gắn ghi chú theo thời điểm; PoC layout chung không được coi là thay đổi production đã áp dụng.
- Cập nhật study guide theo code đã merge: answerIfEligible, notification HR/candidate sau commit và giao dịch riêng, tránh LAZY detached, link HR/HM, workbook streaming, helper renderCreateForm và trạng thái hỗ trợ Expired của Application.
- Bảo toàn khối code/diff và dấu +/-; chuẩn hóa 222 dòng whitespace, dùng br cho 188 hard-break Markdown cần giữ hiển thị. Giữ số liệu test lịch sử như bằng chứng tại từng mốc, không tuyên bố đã chạy lại.

## Kiểm tra

- Manifest chỉ thêm Markdown của HuyenPT; code, test, schema và tài liệu chung không khác main.
- Quét pattern private key, token và gán password/secret không phát hiện nội dung cần che; không đưa local config, log, ảnh hoặc scratch/target vào PR.
- 48 tài liệu qua kiểm tra fence/whitespace; 19 liên kết local hiện hành tồn tại. Đã đối chiếu 45 tài liệu lịch sử với bản gốc: nội dung giữ nguyên ngoài banner và định dạng whitespace/hard-break. Tham chiếu local trong archive được giải thích là bằng chứng không portable.
- Git diff check và merge với main được kiểm tra trước push.
- Không chạy Maven/Node vì PR chỉ sửa tài liệu. Các kết quả 510 Maven/20 Node trong báo cáo workflow là của PR code trước, không phải test mới trong PR này.

Job hết hạn vẫn tắt mặc định. Migration và việc bật job phải được kiểm tra trên môi trường đích; PR tài liệu này không thực hiện thao tác triển khai.

## Danh sách file

| File | Nội dung |
|---|---|
| `docs/members/huyenpt/html-css-refactor/01-project-audit.md` | Báo cáo lịch sử 01-project-audit; giữ nội dung thực hiện và kết quả tại mốc đó. |
| `docs/members/huyenpt/html-css-refactor/02-js-classification.md` | Báo cáo lịch sử 02-js-classification; giữ nội dung thực hiện và kết quả tại mốc đó. |
| `docs/members/huyenpt/html-css-refactor/03-dependency-impact-analysis.md` | Báo cáo lịch sử 03-dependency-impact-analysis; giữ nội dung thực hiện và kết quả tại mốc đó. |
| `docs/members/huyenpt/html-css-refactor/04-regression-test-checklist.md` | Báo cáo lịch sử 04-regression-test-checklist; giữ nội dung thực hiện và kết quả tại mốc đó. |
| `docs/members/huyenpt/html-css-refactor/05-safe-migration-plan.md` | Báo cáo lịch sử 05-safe-migration-plan; giữ nội dung thực hiện và kết quả tại mốc đó. |
| `docs/members/huyenpt/html-css-refactor/06-pre-refactor-safety-validation.md` | Báo cáo lịch sử 06-pre-refactor-safety-validation; giữ nội dung thực hiện và kết quả tại mốc đó. |
| `docs/members/huyenpt/html-css-refactor/07-corrected-migration-priorities.md` | Báo cáo lịch sử 07-corrected-migration-priorities; giữ nội dung thực hiện và kết quả tại mốc đó. |
| `docs/members/huyenpt/html-css-refactor/08-task-02-native-validation-execution-report.md` | Báo cáo lịch sử 08-task-02-native-validation-execution-report; giữ nội dung thực hiện và kết quả tại mốc đó. |
| `docs/members/huyenpt/html-css-refactor/09-task-03-execution-report.md` | Báo cáo lịch sử 09-task-03-execution-report; giữ nội dung thực hiện và kết quả tại mốc đó. |
| `docs/members/huyenpt/html-css-refactor/10-task-04-real-js-reduction-report.md` | Báo cáo lịch sử 10-task-04-real-js-reduction-report; giữ nội dung thực hiện và kết quả tại mốc đó. |
| `docs/members/huyenpt/html-css-refactor/11-task-05-legacy-modal-removal-report.md` | Báo cáo lịch sử 11-task-05-legacy-modal-removal-report; giữ nội dung thực hiện và kết quả tại mốc đó. |
| `docs/members/huyenpt/html-css-refactor/12-task-06-director-modal-cleanup-report.md` | Báo cáo lịch sử 12-task-06-director-modal-cleanup-report; giữ nội dung thực hiện và kết quả tại mốc đó. |
| `docs/members/huyenpt/html-css-refactor/13-task-07-browser-regression-report.md` | Báo cáo lịch sử 13-task-07-browser-regression-report; giữ nội dung thực hiện và kết quả tại mốc đó. |
| `docs/members/huyenpt/html-css-refactor/14-task-08-browser-acceptance-report.md` | Báo cáo lịch sử 14-task-08-browser-acceptance-report; giữ nội dung thực hiện và kết quả tại mốc đó. |
| `docs/members/huyenpt/html-css-refactor/15-task-09-interactive-ui-and-native-html-report.md` | Báo cáo lịch sử 15-task-09-interactive-ui-and-native-html-report; giữ nội dung thực hiện và kết quả tại mốc đó. |
| `docs/members/huyenpt/html-css-refactor/16-task-10-final-refactor-audit.md` | Báo cáo lịch sử 16-task-10-final-refactor-audit; giữ nội dung thực hiện và kết quả tại mốc đó. |
| `docs/members/huyenpt/html-css-refactor/17-task-10-frontend-architecture-audit.md` | Báo cáo lịch sử 17-task-10-frontend-architecture-audit; giữ nội dung thực hiện và kết quả tại mốc đó. |
| `docs/members/huyenpt/html-css-refactor/18-task-10-js-css-target-structure.md` | Báo cáo lịch sử 18-task-10-js-css-target-structure; giữ nội dung thực hiện và kết quả tại mốc đó. |
| `docs/members/huyenpt/html-css-refactor/19-task-10-migration-roadmap.md` | Báo cáo lịch sử 19-task-10-migration-roadmap; giữ nội dung thực hiện và kết quả tại mốc đó. |
| `docs/members/huyenpt/html-css-refactor/20-task-10-js-reduction-opportunities.md` | Báo cáo lịch sử 20-task-10-js-reduction-opportunities; giữ nội dung thực hiện và kết quả tại mốc đó. |
| `docs/members/huyenpt/html-css-refactor/21-task-11-role-based-architecture-and-common-extraction.md` | Báo cáo lịch sử 21-task-11-role-based-architecture-and-common-extraction; giữ nội dung thực hiện và kết quả tại mốc đó. |
| `docs/members/huyenpt/html-css-refactor/22-task-12-hr-role-extraction-report.md` | Báo cáo lịch sử 22-task-12-hr-role-extraction-report; giữ nội dung thực hiện và kết quả tại mốc đó. |
| `docs/members/huyenpt/html-css-refactor/23-task-13-director-extraction-and-legacy-retirement-report.md` | Báo cáo lịch sử 23-task-13-director-extraction-and-legacy-retirement-report; giữ nội dung thực hiện và kết quả tại mốc đó. |
| `docs/members/huyenpt/html-css-refactor/24-task-14-css-common-extraction-report.md` | Báo cáo lịch sử 24-task-14-css-common-extraction-report; giữ nội dung thực hiện và kết quả tại mốc đó. |
| `docs/members/huyenpt/html-css-refactor/25-task-15-hr-css-extraction-and-visual-parity-report.md` | Báo cáo lịch sử 25-task-15-hr-css-extraction-and-visual-parity-report; giữ nội dung thực hiện và kết quả tại mốc đó. |
| `docs/members/huyenpt/html-css-refactor/26-task-16-director-css-and-legacy-retirement-report.md` | Báo cáo lịch sử 26-task-16-director-css-and-legacy-retirement-report; giữ nội dung thực hiện và kết quả tại mốc đó. |
| `docs/members/huyenpt/html-css-refactor/27-task-17-legacy-css-consolidation-report.md` | Báo cáo lịch sử 27-task-17-legacy-css-consolidation-report; giữ nội dung thực hiện và kết quả tại mốc đó. |
| `docs/members/huyenpt/html-css-refactor/28-task-18-internal-css-decoupling-and-final-legacy-audit.md` | Báo cáo lịch sử 28-task-18-internal-css-decoupling-and-final-legacy-audit; giữ nội dung thực hiện và kết quả tại mốc đó. |
| `docs/members/huyenpt/html-css-refactor/29-task-19-candidate-css-optimization-and-legacy-retirement-report.md` | Báo cáo lịch sử 29-task-19-candidate-css-optimization-and-legacy-retirement-report; giữ nội dung thực hiện và kết quả tại mốc đó. |
| `docs/members/huyenpt/html-css-refactor/30-task-20-frontend-acceptance-and-js-reduction-report.md` | Báo cáo lịch sử 30-task-20-frontend-acceptance-and-js-reduction-report; giữ nội dung thực hiện và kết quả tại mốc đó. |
| `docs/members/huyenpt/html-css-refactor/31-task-21-final-precommit-acceptance-audit.md` | Báo cáo lịch sử 31-task-21-final-precommit-acceptance-audit; giữ nội dung thực hiện và kết quả tại mốc đó. |
| `docs/members/huyenpt/html-css-refactor/32-task-22-final-commit-manifest-and-handoff.md` | Báo cáo lịch sử 32-task-22-final-commit-manifest-and-handoff; giữ nội dung thực hiện và kết quả tại mốc đó. |
| `docs/members/huyenpt/html-css-refactor/33-task-22a-commit-reproducibility-verification.md` | Báo cáo lịch sử 33-task-22a-commit-reproducibility-verification; giữ nội dung thực hiện và kết quả tại mốc đó. |
| `docs/members/huyenpt/html-css-refactor/34-task-23-shared-frontend-audit-and-optimization-roadmap.md` | Báo cáo lịch sử 34-task-23-shared-frontend-audit-and-optimization-roadmap; giữ nội dung thực hiện và kết quả tại mốc đó. |
| `docs/members/huyenpt/html-css-refactor/35-task-24-toast-reliability-and-safe-cleanup-report.md` | Báo cáo lịch sử 35-task-24-toast-reliability-and-safe-cleanup-report; giữ nội dung thực hiện và kết quả tại mốc đó. |
| `docs/members/huyenpt/html-css-refactor/36-task-24a-verification-and-controlled-cleanup-report.md` | Báo cáo lịch sử 36-task-24a-verification-and-controlled-cleanup-report; giữ nội dung thực hiện và kết quả tại mốc đó. |
| `docs/members/huyenpt/html-css-refactor/38-task-25a-shared-layout-css-deduplication-poc.md` | Báo cáo lịch sử 38-task-25a-shared-layout-css-deduplication-poc; giữ nội dung thực hiện và kết quả tại mốc đó. |
| `docs/members/huyenpt/html-css-refactor/39-task-25c-offer-only-css-deduplication.md` | Báo cáo lịch sử 39-task-25c-offer-only-css-deduplication; giữ nội dung thực hiện và kết quả tại mốc đó. |
| `docs/members/huyenpt/offer-completion-language-and-expiry-review.md` | Bằng chứng đồng bộ ngôn ngữ và kiểm chứng Offer–Application theo từng phase. |
| `docs/members/huyenpt/offer-expired-application-handoff.md` | Bàn giao bốn điểm Application, có ghi chú phần code đã vào main. |
| `docs/members/huyenpt/offer-management-defense-practice.md` | Bộ câu hỏi bảo vệ theo snapshot cũ, gắn nhãn lịch sử. |
| `docs/members/huyenpt/offer-management-flow-analysis.md` | Phân tích luồng theo snapshot cũ, không coi nhận định cũ là trạng thái hiện tại. |
| `docs/members/huyenpt/offer-management-knowledge-guide.md` | Giải thích kiến trúc và ví dụ học tập lịch sử. |
| `docs/members/huyenpt/offer-management-source-audit.md` | Audit source ban đầu và các phát hiện tại thời điểm thực hiện. |
| `docs/members/huyenpt/offer-risk-fixes-and-safe-cleanup.md` | Lưu các phase rủi ro, rollback candidate/notification và sửa link theo người nhận. |
| `docs/members/huyenpt/offer-workflow-study-guide.md` | Hướng dẫn hiện hành đã đối chiếu code sau PR #163. |
| `docs/members/huyenpt/README.md` | Mục lục, phân loại tài liệu và đường dẫn tới nguồn chuẩn. |
| `docs/members/huyenpt/pr-review/offer-workflow-review.md` | Mô tả phạm vi, kiểm tra và manifest PR cuối. |
