# Commit 01 — Tích hợp main và giữ luồng Talent Pool

Ngày: 14/10/2026. Tách từ merge commit `d6e91bb84a4db4716f9f142271bcd82f8f8ec4f0`.

## Mục đích

Giữ việc tích hợp commit main `d1f1144` vào nhánh `feature/talent-pool-first-sprint`, đồng thời giữ code Talent Pool đã có ở `92fb06e`. Commit này có hai parent; các commit gốc của đồng đội vẫn tồn tại với đúng hash và tác giả.

## Các file resolve cần đi cùng nhau

- `src/main/java/com/group2/rms/candidate/controller/ApplicationPipelineController.java`: giữ stage items, stage links và bộ lọc mới; bổ sung talent notes và thông tin đóng đợt.
- `src/main/java/com/group2/rms/candidate/dto/ApplicationPipelineResponse.java`: giữ owner label, interview link và cờ tiềm năng trong projection/withProgress.
- `src/main/java/com/group2/rms/candidate/repository/ApplicationRepository.java`: giữ query ID/dòng, latest AI score và interviewId của main cùng query Talent Pool.
- `src/main/java/com/group2/rms/candidate/service/ApplicationPipelineService.java`: ghép paging, stage counts, stepper và facts mới với audit đóng đợt và Candidate tiềm năng.
- `src/main/java/com/group2/rms/candidate/service/ApplicationStatusLabels.java`: giữ nhãn HM_Suggested; bỏ currentStep đã được main thay thế.
- `src/main/resources/templates/candidate/application-detail.html`: giữ UI mới của main; bổ sung phần tiềm năng, notes, xác nhận/từ chối và resend.
- `src/test/java/com/group2/rms/candidate/ApplicationPipelineWebTests.java`: giữ test của hai phía và đồng bộ fixture DTO.

Các file Offer, sidebar, stage bar, CSS và tài liệu tự merge từ main được giữ nguyên bản ở commit đã tích hợp. Không tách chúng thành commit mang tên mình, vì chúng đã có commit riêng của đồng đội trong parent main.

## Vì sao đây là một commit tích hợp

DTO, Repository, Service, Controller, template và fixture phụ thuộc cùng một contract mới. Việc nhập main và resolve các contract này cần nằm trong cùng merge commit. Các sửa độc lập phía sau được tách riêng: stepper đóng đợt, mock web slice, JPQL và mapping InterviewId.

## Kiểm tra

1. Xem diff so với từng parent để kiểm tra cả code main lẫn Talent Pool.
2. HR mở /applications: stage bar, bộ lọc, phân trang, link kết quả và badge tiềm năng cùng tồn tại.
3. HR/HM mở hồ sơ: chỉ một form duyệt; notes và thao tác Talent Pool vẫn có.
4. Không tuyên bố mọi commit trung gian đều chạy xanh: các lỗi được sửa trong commit 02–06 vẫn tồn tại trước những commit đó.
