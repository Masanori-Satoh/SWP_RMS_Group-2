# Class diagram — Ứng viên tiềm năng (bản rút gọn)

Ngày đối chiếu source: 26/10/2026. Project: `SWP_RMS_Group-2`.

## 1. Phạm vi

Kho ứng viên tiềm năng, tìm kiếm, ghi chú, HM đề xuất / HR gợi ý, HM quyết định và HR xác nhận hoặc kết thúc đề xuất. Chỉ hiện attribute/method chính của luồng; DTO trực tiếp phục vụ dữ liệu được giữ lại.

Đóng tin tuyển dụng, gửi lại thông báo và `RecruitmentEmailService` nằm ngoài phạm vi này. Chúng có lời gọi trong cùng Controller nhưng không phải các thao tác quản lý ứng viên tiềm năng đang mô tả. `JobPosting` vẫn được giữ vì mỗi hồ sơ/đề xuất phải gắn với một tin tuyển dụng.

## 2. Các class và source

Hình có **18 kiểu**: 2 Controller/Service, 4 repository interface, 4 entity và 8 DTO record.

| Class / record / interface | Source đã đối chiếu |
|---|---|
| `TalentPoolController` | `src/main/java/com/group2/rms/candidate/talentpool/controller/TalentPoolController.java:30` |
| `TalentPoolService` | `src/main/java/com/group2/rms/candidate/talentpool/service/TalentPoolService.java:39` |
| `TalentPoolSearch` | `src/main/java/com/group2/rms/candidate/talentpool/dto/TalentPoolSearch.java:4` |
| `TalentPoolSuggestRequest` | `src/main/java/com/group2/rms/candidate/talentpool/dto/TalentPoolSuggestRequest.java:7` |
| `TalentPoolConfirmRequest` | `src/main/java/com/group2/rms/candidate/talentpool/dto/TalentPoolConfirmRequest.java:6` |
| `TalentPoolNoteRequest` | `src/main/java/com/group2/rms/candidate/talentpool/dto/TalentPoolNoteRequest.java:5` |
| `TalentPoolRecommendationDecisionRequest` | `src/main/java/com/group2/rms/candidate/talentpool/dto/TalentPoolRecommendationDecisionRequest.java:6` |
| `TalentPoolSuggestResponse` | `src/main/java/com/group2/rms/candidate/talentpool/dto/TalentPoolSuggestResponse.java:3` |
| `TalentPoolCandidateResponse` | `src/main/java/com/group2/rms/candidate/talentpool/dto/TalentPoolCandidateResponse.java:6` |
| `TalentPoolCvResponse` | `src/main/java/com/group2/rms/candidate/talentpool/dto/TalentPoolCvResponse.java:6` |
| `TalentPoolCandidateRepository` | `src/main/java/com/group2/rms/candidate/talentpool/repository/TalentPoolCandidateRepository.java:15` |
| `TalentPoolApplicationRepository` | `src/main/java/com/group2/rms/candidate/talentpool/repository/TalentPoolApplicationRepository.java:20` |
| `ApplicationRepository` | `src/main/java/com/group2/rms/candidate/domain/repository/ApplicationRepository.java:16` |
| `TalentPoolNoteRepository` | `src/main/java/com/group2/rms/candidate/talentpool/repository/TalentPoolNoteRepository.java:7` |
| `Candidate` | `src/main/java/com/group2/rms/candidate/domain/entity/Candidate.java:22` |
| `Application` | `src/main/java/com/group2/rms/candidate/domain/entity/Application.java:23` |
| `JobPosting` | `src/main/java/com/group2/rms/jobposting/entity/JobPosting.java:35` |
| `TalentPoolNote` | `src/main/java/com/group2/rms/candidate/talentpool/entity/TalentPoolNote.java:16` |

## 3. Quan hệ UML

- Đường liền, mũi tên mở: association từ field inject (`TalentPoolController.pool`, các repository trong Service) hoặc tham chiếu entity/DTO.
- Nét đứt, mũi tên mở: dependency khi dùng request, tạo response hoặc truy vấn/cập nhật entity.
- `Application → Candidate`, `Application → JobPosting`, `TalentPoolNote → Candidate`: mỗi đối tượng phía nguồn bắt buộc tham chiếu 1 đối tượng đích; phía ngược lại là `0..*`, căn cứ JPA và FK NOT NULL trong `database/schema/db.sql`.
- `TalentPoolCandidateResponse.cvs` chứa `0..*` `TalentPoolCvResponse`.
- Không vẽ aggregation/composition cho dependency injection; không tạo kế thừa giả giữa các repository.

## 4. Điểm nghiệp vụ cần giữ đúng

- `Candidate.isPotential` là cờ của ứng viên; đánh dấu tiềm năng không tự chuyển trạng thái Application.
- `suggest()` và `recommend()` tạo đề xuất `HM_Suggested` từ CV cũ. Nhánh HR cần HM phản hồi trước khi HR xác nhận.
- `confirm()` kiểm tra consent, tin còn mở, điều kiện đề xuất và CV; gọi `promoteSuggestionToHmPassed()` để chuyển hồ sơ sang `HM_Passed`.
- Tên, email và số điện thoại trên danh sách thuộc DTO `TalentPoolCandidateResponse`, lấy từ `Candidate.account`; không thêm các field này vào entity Candidate.

## 5. Kết quả và QA

- `talent-pool.drawio`: XML native, 114 cell, 18 kiểu riêng biệt, 20 connector độc lập; tên/ngăn/đường nối đều là thành phần chỉnh sửa được.
- `talent-pool.png`: **4200 × 2389 px**, xuất trực tiếp bằng draw.io Desktop 32.5.5 từ DRAWIO cuối cùng.
- PASS: đọc/lưu XML qua draw.io không đổi nội dung cell sau khi chuẩn hóa giá trị tọa độ mặc định.
- PASS: model nhúng trong PNG khớp các cell của DRAWIO cuối cùng.
- PASS: đối chiếu 18 operation và 38 field với source, gồm visibility, tên, kiểu, tham số và return type.
- PASS: kiểm tra bản render native bằng Chrome — không tràn chữ, không class chồng nhau, không nhãn đè class; kiểm tra hình học không connector chạy xuyên class.
- PASS: SHA-256 của 713 file trong `src/`, `database/` và `pom.xml` không đổi.

## 6. Mức sử dụng và giới hạn

Phù hợp đưa vào SDS ở mức tổng quan chức năng. Đây là phép chiếu rút gọn, không phải toàn bộ dependency graph của module: các DTO/method khác, BaseEntity, repository CRUD dùng chung, audit, review history, phân quyền, exception và CV storage được lược khỏi hình. `User` vẫn là kiểu thật của `TalentPoolNote.author` nhưng không mở thêm entity User trong view này.

Đã kiểm tra cấu trúc chỉnh sửa và native load/export/round-trip; chưa thực hiện kéo-thả/chỉnh text qua GUI. Đây là phân tích source và QA tài liệu, không phải kiểm thử HTTP/SQL Server/email runtime.
