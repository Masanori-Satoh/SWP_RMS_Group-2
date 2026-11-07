# Talent Pool Candidate theo SRS

Nguồn: `D:/dowload/RMS_SRS (3).docx`. Đây là class diagram phân tích/thiết kế theo yêu cầu; chưa xác nhận tên lớp, phương thức hay DTO bằng source code.

## Phạm vi và căn cứ

| Nội dung | Căn cứ trong SRS | Thành phần trên diagram |
|---|---|---|
| Đánh dấu/bỏ đánh dấu tiềm năng, độc lập trạng thái hồ sơ | BF 2.5; BR-TAL-01; Mark Potential Candidate Dialog | Candidate.isPotential; markPotential/unmarkPotential |
| Xem/tìm kiếm/lọc theo người đánh dấu và vị trí nguồn | View Potential Candidate List; màn hình 4.7 | TalentPoolSearch; viewList |
| Xem thông tin, các CV cũ và lịch sử hồ sơ | Candidate; Application; màn hình 4.8–4.9 | Candidate, User, Application.appliedCvUrl; viewDetail/viewPreviousCV |
| Xem/thêm/sửa/xóa ghi chú của mình | TalentPoolNote; các use case ghi chú; màn hình 4.9 | TalentPoolNote, TalentPoolNoteInput; viewNotes/addNote/editOwnNote/deleteOwnNote |
| Chọn CV nguồn, kiểm tra lựa chọn rồi mới xác nhận | Add / Recommend Candidates Confirmation; BF 2.6 | TalentPoolSelection; reviewSelection, suggestForJob, recommendToHM |
| HM đề xuất trực tiếp; HR giới thiệu cần HM chấp nhận | BF 2.6; BR-TAL-01; ApplicationReview; màn hình 5.13–5.16 | Application, ApplicationReview; viewSuggestions/viewRecommendations, acceptRecommendation/declineRecommendation |
| HR rút giới thiệu, giữ CV/ghi chú/lịch sử | Recommended Candidates; MSG77 | withdrawRecommendation; ApplicationReview; AuditLog |
| HR liên hệ và ghi nhận đồng ý/từ chối | BF 2.6; BR-TAL-01; MSG78–81 | ParticipationConfirmation; confirmParticipation |
| Truy vết sự kiện và người đánh dấu | AuditLog; bộ lọc người đánh dấu; timeline đề xuất | AuditLog và liên kết actor tới User |
| Người phụ trách tin tuyển dụng | ERD 3.1; JobRequisition | JobPosting → JobRequisition → User |

## Quy ước thiết kế

- `TalentPoolController`, `TalentPoolService`, bốn DTO và các chữ ký phương thức là đề xuất thiết kế từ chức năng trong SRS. Chúng không phải danh mục class Java đã kiểm chứng. Kiểu dữ liệu là lựa chọn thiết kế.
- Các entity và thuộc tính nghiệp vụ lấy từ ERD 3.1 và mô tả entity 3.2. Chỉ giữ phần liên quan Talent Pool.
- HR và Hiring Manager là vai trò của User; không tự tạo lớp con HR/HM. Candidate được liên kết một User; User có tối đa một Candidate.
- Candidate tiềm năng là `Candidate.isPotential`; không tạo bảng/lớp con PotentialCandidate. CV lưu trên Application; không giả định một entity CV riêng.
- Giới thiệu được biểu diễn bằng Application tạm `HM_Suggested` và ApplicationReview. Không tự tạo entity Recommendation độc lập vì ERD không có entity đó.
- Số hồ sơ chính thức trên mỗi cặp candidate/job posting bị giới hạn bởi BR-CAN-01; đây là ràng buộc nghiệp vụ, không chuyển các liên kết Candidate–Application và JobPosting–Application thành one-to-one.
- Association nét liền, dependency nét đứt, bội số hai đầu cho entity. Không thêm interface/realization/repository do SRS không quy định các hợp đồng triển khai này. Không thêm composition vì SRS yêu cầu giữ hồ sơ/lịch sử, không mô tả quyền sở hữu vòng đời kiểu đó.
- Tham số được giữ trong mô hình VPP nhưng ẩn trên hình cho dễ đọc. Các thao tác của boundary chỉ chuyển tiếp các chức năng cùng tên tới control.
- Ghi chú nội bộ, nội dung 1–1000 ký tự; chỉ tác giả được sửa/xóa. Không chia sẻ ghi chú cho candidate.
- HR liên hệ ngoài hệ thống qua điện thoại/email; mailto trong màn hình không chứng minh có chức năng tự gửi email. Vì thế diagram không thêm EmailSender.
- Không đưa đóng tin tuyển dụng, gửi lại thông báo, sàng lọc AI hay lập lịch phỏng vấn vào thao tác Talent Pool. Chỉ biểu diễn liên kết tin và kết quả chuyển sang luồng tiếp theo.

## Điểm mâu thuẫn cần chốt trong SRS

BR-TAL-01, mô tả Application và BF 2.6 quy định dùng CV cũ → `HM_Passed`; nộp CV mới → `Applied` và quay lại screening. Mô tả danh mục màn hình Potential Candidate Contact List/Decline Participation Dialog lại nói việc đồng ý (kể cả CV mới tại một đoạn) → `HM_Passed`.

Mô hình thiết kế này lấy BR-TAL-01 và BF 2.6 làm giả định: dùng CV cũ → `HM_Passed`, CV mới → `Applied`, từ chối → `Rejected`. HM chấp nhận giới thiệu chỉ đưa vào danh sách chờ HR liên hệ, không trực tiếp tạo trạng thái sẵn sàng phỏng vấn. Giả định cũng được ghi trong documentation của diagram VPP.

## Kiểm chứng

Đối chiếu văn bản SRS, BF 2.5/2.6 và ERD nhúng. Kiểm tra geometry, quan hệ UML và mở lại project bằng Visual Paradigm là kiểm tra artifact; không phải bằng chứng chạy HTTP/DB hoặc kiểm chứng code.
