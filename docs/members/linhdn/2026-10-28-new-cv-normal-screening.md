# Candidate tiềm năng nộp CV: trở lại luồng xét duyệt thông thường

Ngày: 28/10/2026. Thay thế quyết định đi thẳng phỏng vấn trong báo cáo ngày 17/10. Không sửa schema; không commit/push.

## Hành vi hiện tại

- Candidate tự nộp CV qua `POST /jobs/{id}/apply` luôn bắt đầu ở `Applied`, kể cả được HM thêm hoặc HM đã chấp nhận gợi ý từ HR.
- Nếu có `HM_Suggested` khớp Candidate–JobPosting, cập nhật CV trên cùng Application, ghi audit hiện có; không tạo Application thứ hai hoặc Candidate mới.
- Phát `ApplicationSubmittedEvent` cho mọi lần nộp thành công. AI listener hiện có chạy sau commit; sau đó tiếp tục duyệt HR/HM như hồ sơ thông thường. Chưa được lên lịch phỏng vấn khi còn `Applied`.
- Query nhận CV mới chỉ chấp nhận trạng thái đích `Applied`; yêu cầu chuyển `HM_Passed` qua query này không cập nhật bản ghi.
- Cờ `Candidate.IsPotential`, CV và Application nguồn giữ nguyên. Chặn nộp trùng, kiểm tra tin còn mở/hạn nộp, validation PDF, job lock, transaction và dọn file khi rollback giữ nguyên.

## Phạm vi giữ nguyên

- HR xác nhận Candidate đồng ý dùng CV cũ vẫn dùng thao tác riêng: `HM_Suggested` → `HM_Passed`.
- HM/HR gợi ý, phản hồi, từ chối; luồng duyệt hồ sơ chính thức; phân quyền, CSRF và module Interview không sửa.
- Không chuyển ngược các Application đã ở `HM_Passed` trước thay đổi này; không sửa dữ liệu đang có.

## File thay đổi

- `ApplicationSubmissionService.java`: bỏ nhánh tự nâng trạng thái và luôn phát sự kiện nộp đơn.
- `ApplicationRepository.java`: giới hạn query nhận CV mới vào `Applied`, giữ chữ ký phương thức và query HR xác nhận CV cũ.
- `ApplicationSubmissionServiceTests.java`: kiểm tra Candidate thường/tiềm năng và đề xuất khi nộp CV mới đều đi qua sàng lọc; giữ các ca chống trùng/validation/rollback.
- `TalentPoolFlowIntegrationTests.java`: bốn nguồn `HM_ADDED`, `HR_ACCEPTED`, `HR_PENDING`, `NO_PROPOSAL` đều kỳ vọng `Applied`, có sự kiện sàng lọc và chưa được lên lịch. Các test HR xác nhận CV cũ vẫn kỳ vọng `HM_Passed`.

## Kiểm chứng

- **152 test qua, 0 failure/error/skipped**: ApplicationSubmissionService, CvFileValidator, AiScreeningService/Listener, ApplicationReviewService, ApplicationStatusService, TalentPoolDecisionUnit, TalentPoolRecommendation và InterviewSchedulingService.
- Candidate thường và Candidate tiềm năng không có đề xuất đều tạo `Applied`; đề xuất HM khi Candidate nộp CV mới dùng lại Application, cập nhật `Applied` và phát sự kiện. Các ca duplicate/validation/rollback giữ nguyên.
- **Integration chọn 6 ca trên SQL Server: 1 qua, 4 failure, 1 error**. Bốn nguồn nộp CV (`HM_ADDED`, `HR_ACCEPTED`, `HR_PENDING`, `NO_PROPOSAL`) dừng ở `fixture()` vì không có mẫu active HM + published managed job + potential Candidate/CV phù hợp; chưa chạy tới bước nộp CV mới. Một ca HR xác nhận CV cũ dừng ở GET danh sách vì không tìm thấy template `talent-pool/suggestions`. Không sửa dữ liệu mẫu hoặc template để làm xanh các ca ngoài phạm vi.
- Ca `realHrRecommendationHmReviewAllCvsAndNotesThenConsentUseOneDraft` qua trên SQL Server, giữ việc HR xác nhận CV cũ sang `HM_Passed` và đủ điều kiện Interview.
- Các test tích hợp dùng `ddl-auto=validate` và transaction rollback. Không tạo lịch thật; AI listener sau commit không được thực thi trong transaction rollback. Việc phát sự kiện đã được kiểm tra ở unit test; chưa xác minh AI thực tế hoặc thao tác browser.
- Lượt build dùng output chung gặp thiếu file `.class` lúc biên dịch test. Lượt tích hợp sau dùng POM tạm `target/normal-screening-verification-pom.xml` và output `target/normal-screening-verification/`; không sửa POM chính. Không coi lỗi build ban đầu là kết quả chạy test.
- `git diff --check`: qua. Chỉ sửa 2 file production, 2 file test và tài liệu LinhDN; giữ các file auth/diagram người dùng đang sửa.

Lệnh unit:

```powershell
mvn.cmd -B '-Dtest=ApplicationSubmissionServiceTests,CvFileValidatorTests,AiScreeningServiceTests,AiScreeningListenerTests,ApplicationReviewServiceTests,ApplicationStatusServiceTests,TalentPoolDecisionUnitTests,TalentPoolRecommendationTests,InterviewSchedulingServiceTests' '-Dspring.jpa.hibernate.ddl-auto=validate' test
```

Lệnh tích hợp (với POM tạm tách output nêu trên):

```powershell
mvn.cmd -B -f target/normal-screening-verification-pom.xml '-Dtest=TalentPoolFlowIntegrationTests#candidateNewCvAlwaysFollowsNormalScreeningRegardlessOfProposal+realHmSubmissionHrConsentAndInterviewEligibilityPreserveOriginalCandidateAndCv+realHrRecommendationHmReviewAllCvsAndNotesThenConsentUseOneDraft' '-Dspring.jpa.hibernate.ddl-auto=validate' '-Dapp.storage.local-root=target/normal-screening-verification/uploads' test
```

Bằng chứng tích hợp: `target/normal-screening-verification/surefire-reports/TEST-com.group2.rms.candidate.talentpool.TalentPoolFlowIntegrationTests.xml`. Không khẳng định toàn bộ E2E đã qua.
