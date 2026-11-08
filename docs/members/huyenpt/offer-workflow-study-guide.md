# Học luồng Offer Management — HuyenPT

Tài liệu đối chiếu với main sau PR #163 (07/11/2026), gồm sửa notification candidate và link theo người nhận. Mục đề xuất cần đọc cùng ghi chú đã/chưa triển khai. Dùng tài liệu cùng comment tiếng Việt tại controller, service, repository và JavaScript; không học chỉ từ tên nút trên giao diện. Báo cáo cũ trong [mục lục](README.md) là bằng chứng lịch sử, không thay thế code hiện hành.

## 1. Nắm ba vai trò trước

| Vai trò | Được làm trong Offer | Không được làm |
|---|---|---|
| HR | Xem, lọc, tạo/sửa/xóa nháp, trình duyệt, phát hành thư mời, xuất Excel | Duyệt/từ chối thay Giám đốc |
| Director | Danh sách loại Draft; xem chi tiết, duyệt/từ chối, lịch sử; endpoint xuất Excel cũng cho phép Director | Tạo, sửa, xóa nháp, phát hành |
| Candidate | Xem và nhận/từ chối thư mời của chính mình sau khi phát hành | Xem đề xuất nội bộ hoặc thư mời của người khác |

`SecurityConfig` chặn theo vai trò/URL. Service kiểm tra trạng thái và nghiệp vụ. Query phía ứng viên còn ràng buộc chủ sở hữu bằng UserId. Ẩn nút ở HTML chỉ là hỗ trợ giao diện, không phải biện pháp phân quyền duy nhất. Không mở nghiệp vụ Offer cho System Admin.

## 2. Phân biệt ba loại trạng thái

| Thao tác | OfferProposal.offerStatus | Application.applicationStatus |
|---|---|---|
| HR lưu nháp | Draft | Giữ nguyên |
| HR trình duyệt | Pending_Director | Giữ nguyên |
| Director duyệt | Director_Approved | Giữ nguyên |
| Director từ chối đề xuất | Director_Rejected | Giữ nguyên, không tự chuyển hồ sơ thành Rejected |
| HR phát hành | Sent_Candidate | Cập nhật có điều kiện sang Offered |
| Job chốt quá hạn (khi được bật) | Expired | Giữ nguyên; không tự kết thúc hồ sơ |
| Ứng viên nhận | Accepted | Yêu cầu chuyển Offered → Hired |
| Ứng viên từ chối | Declined | Yêu cầu chuyển Offered → Withdrawn |

`OfferApproval.status` là **quyết định trong lịch sử**, chỉ dùng Approved/Rejected; không phải toàn bộ vòng đời Offer. Một Offer Accepted vẫn có thể có log Director Approved từ trước.

Hai chuỗi Approved/Rejected cũ còn được hỗ trợ ở một số chỗ để đọc dữ liệu tương thích. Không tự đổi tên trạng thái, cột DB hoặc biến template khi thống nhất cách gọi UI.

Offer hiện dùng query transition sẵn có của ApplicationRepository và bắt buộc cập nhật đúng 1 dòng từ Offered. Nếu không đạt, giao dịch rollback cả phản hồi Offer. Không sửa mã ApplicationStatusService của người phụ trách hồ sơ.

## 3. Bản đồ gọi hàm cần nhớ

| Tính năng | Điểm vào | Hàm xử lý chính | Kết quả |
|---|---|---|---|
| Danh sách nội bộ | GET /offers → OfferController.listOffers | getAllOffersForHr hoặc getDirectorOffers | offers/list hoặc offers/director-list |
| Mở form tạo | GET /offers/create → createOfferForm | getPassedCandidatesForOffer | offers/form |
| Lưu/trình đề xuất mới | POST /offers/create → createOffer | createOfferByHr | Draft/Pending_Director |
| Chi tiết nội bộ | GET /offers/{id} → viewOfferDetail | getOfferDetailForHr | offers/detail |
| Sửa nháp | GET /offers/{id}/edit; POST /offers/{id}/edit | getUpdateOfferRequestById; updateOfferByHr | Draft hoặc Pending_Director |
| Xóa nháp | DELETE /offers/{id}; hoặc POST /offers/{id}/delete | deleteDraftOfferByHr | isDeleted=true |
| Duyệt/từ chối | POST /offers/{id}/approve hoặc /reject | approveOfferByDirector/rejectOfferByDirector → decideOffer | Trạng thái mới và OfferApproval |
| Phát hành | POST /offers/{id}/send | sendOfferToCandidate | Sent_Candidate, Offered, hạn trả lời |
| Xuất Excel | POST /offers/export | exportOffersToExcel | File .xlsx, không đổi trạng thái |
| Danh sách/chi tiết ứng viên | GET /portal/offers; GET /portal/offers/{id} | CandidateOfferServiceImpl.list/getDetail | DTO của đúng chủ sở hữu |
| Ứng viên trả lời | POST /portal/offers/{id}/accept hoặc /decline | accept/decline → answer | Accepted/Declined và xử lý hồ sơ |

Tạo/sửa dùng form, không dùng JSON để ghi nghiệp vụ. JSON POST tạo/sửa cũ được trả 415 với thông báo dùng form. Phát hành và quyết định nhanh dùng AJAX; xóa nháp trên chi tiết vẫn có đường form riêng. GET chi tiết nội bộ còn có biến thể JSON theo Accept header.

## 4. HR tạo đề xuất — học theo thứ tự kiểm tra

1. DTO kiểm tra dữ liệu đầu vào: bắt buộc, định dạng, độ dài, giới hạn trường. Lỗi hiển thị lại trên form.
2. `validateOfferBusinessRules` kiểm tra lại điều kiện ở service: lương, ngày bắt đầu, thời gian phản hồi/thử việc, chức danh và địa điểm.
3. Tìm Application; hồ sơ Hired/Rejected/Withdrawn/Cancelled bị chặn.
4. Tìm Offer của Application bằng query có khóa. Nếu đã có Offer chưa xóa mềm thì không tạo thêm, bất kể Draft hay trạng thái khác.
5. Phải có ít nhất một kết quả phỏng vấn Passed. Code đang dùng `anyMatch`, không phải “chỉ lấy kết quả phỏng vấn mới nhất”.
6. Xác định HR từ tài khoản đăng nhập; không dùng proposedById do trình duyệt gửi làm người đề xuất.
7. Có bản ghi đã xóa mềm thì khôi phục/cập nhật chính bản ghi đó; nếu chưa có thì tạo bản ghi mới.
8. isDraft=true lưu Draft; false/null chuyển Pending_Director. Chỉ trường hợp trình duyệt mới thông báo cho Director.

Đơn vị ràng buộc là **ApplicationId**, không phải CandidateId. Cùng một người ứng tuyển hai tin là hai hồ sơ khác nhau. Khóa query bản ghi hiện hữu và ràng buộc UNIQUE trên ApplicationId có vai trò khác nhau; không nói rằng khóa bản ghi hiện hữu đã xử lý mọi trường hợp tạo đồng thời khi chưa có bản ghi.

Ví dụ: có Draft chưa xóa thì phải vào sửa; đã xóa mềm Draft thì có thể tạo lại theo điều kiện trên; đã Accepted thì không tạo thêm Offer cho chính hồ sơ đó.

## 5. Luật đãi ngộ và thời hạn

| Dữ liệu | Code hiện tại kiểm tra ở đâu |
|---|---|
| Lương chính thức/thử việc dương | DTO và service |
| 85% lương chính thức ≤ lương thử việc ≤ lương chính thức | DTO và service dùng BigDecimal |
| Thời gian thử việc 1–365 ngày | DTO và service; DB có CHECK tương ứng |
| Thời gian phản hồi 1–60 ngày | DTO; service còn kiểm tra dương, không kiểm tra lại toàn bộ mức tối đa |
| Phúc lợi | Không bắt buộc; DTO giới hạn độ dài |
| Ngày bắt đầu | Phải sau ngày hiện tại cộng số ngày phản hồi |

Ví dụ: hôm nay là ngày 10, thời gian phản hồi 5 ngày thì ngày bắt đầu sớm nhất là ngày 16. Giới hạn này khi tạo/sửa chưa phải hạn trả lời đã phát hành.

`probationDays` là **số ngày thử việc** được lưu; `responsePeriodDays` là **số ngày được phản hồi**; `responseDeadline` là **mốc ngày giờ hạn trả lời**, chỉ được tính khi phát hành. Không nhầm ba trường này.

Các con số trong bảng là giới hạn phần mềm đang áp dụng, không dùng bảng để suy ra quy tắc cho mọi loại hợp đồng ngoài phạm vi hệ thống.

## 6. Sửa/xóa nháp

`updateOfferByHr` và `deleteDraftOfferByHr` đọc bằng `findActiveOfferForUpdate`. Sau khi lấy khóa mới kiểm tra trạng thái Draft. Việc lúc mở form vẫn là Draft không bảo đảm lúc bấm lưu vẫn còn Draft.

- Sửa rồi lưu nháp: Draft → Draft.
- Sửa rồi trình: Draft → Pending_Director.
- Xóa: giữ bản ghi, đặt isDeleted=true; không xóa vật lý.
- Director_Rejected không phải trạng thái được sửa trong đường hiện tại. Các comment cũ nói “Draft hoặc Rejected” đã được sửa cho đúng code.

## 7. Director quyết định — đoạn phức tạp cần hiểu

`approveOfferByDirector` và `rejectOfferByDirector` là hai cửa vào cùng `decideOffer`.

1. Đọc Offer chưa xóa, chặn hồ sơ đã kết thúc, kiểm tra Pending_Director.
2. Xác định người ra quyết định.
3. `decideIfPending` thực hiện UPDATE với điều kiện trạng thái vẫn là Pending_Director.
4. 0 dòng: có người đã xử lý/đổi trạng thái, trả lỗi xung đột; không ghi lịch sử hoặc thông báo tiếp.
5. 1 dòng: đọc lại dữ liệu vì bulk UPDATE đã làm sạch persistence context; ghi OfferApproval.
6. Sau commit mới gửi thông báo quyết định trong giao dịch mới REQUIRES_NEW.

Vì sao vừa đọc kiểm tra vừa UPDATE có điều kiện? Hai request có thể cùng đọc Pending_Director; điều kiện trong UPDATE mới quyết định request nào được ghi. Đây là kiểm tra-và-cập nhật nguyên tử, thường gọi CAS.

Lý do từ chối của Director bắt buộc ở `OfferController.rejectOfferJson`; DTO giới hạn tối đa 2.000 ký tự. Service hiện cũng bắt buộc lý do khác trống và giới hạn 2.000 ký tự để bảo vệ lời gọi Java trực tiếp.

Thẻ “đã duyệt/từ chối” của Director đếm log của chính người đó; bộ đếm trạng thái của danh sách đếm vòng đời Offer. Nhóm “đã duyệt” trên danh sách Director gồm cả Sent_Candidate/Accepted/Declined; bộ lọc Approved của HR/Excel không có nghĩa nhóm rộng như vậy.

## 8. HR phát hành và gửi email

`sendOfferToCandidate` chỉ nhận Approved/Director_Approved và hồ sơ chưa kết thúc. Khóa ghi Offer ngăn hai yêu cầu HR cùng phát hành trên cùng bản ghi.

Hạn trả lời = thời điểm phát hành + responsePeriodDays. Ngày của hạn này phải trước ngày bắt đầu dự kiến, nên thư mời đã duyệt lâu vẫn có thể cần được xử lý nghiệp vụ nếu đến lúc gửi không còn đủ thời gian.

Sau khi đặt Sent_Candidate, service chuyển hồ sơ sang Offered bằng điều kiện “trạng thái trong DB vẫn bằng trạng thái vừa đọc”. Nếu thất bại thì ném lỗi để rollback toàn bộ việc phát hành. Không thay đoạn này bằng setApplicationStatus rồi save, vì có thể ghi đè hồ sơ vừa bị rút/đóng.

Email chạy trong `afterCommit`: không gửi trước khi thư mời được lưu thành công. `OfferMailSender` trả SENT, TEST_RECIPIENT hoặc NOT_SENT. TEST_RECIPIENT không phải gửi thành công tới ứng viên thực. Thiếu SMTP/gửi lỗi không biến Offer về trạng thái đã duyệt; UI báo đã phát hành nhưng email chưa gửi tới ứng viên.

Thông báo trong ứng dụng của các đường HR tạo/trình/phát hành và quyết định Director dùng `notifyAfterCommit`: nghiệp vụ commit trước, thông báo chạy trong giao dịch REQUIRES_NEW và tải lại Offer. Lỗi notification được ghi log, không rollback thư mời đã lưu. Đây là best-effort, chưa có outbox/retry bền vững.

## 9. Ứng viên xem và trả lời

Query chỉ lấy thư mời thuộc UserId đang đăng nhập, thuộc Sent_Candidate/Accepted/Declined/Expired, chưa xóa mềm. Expired chỉ được xem, không được phản hồi. ID của người khác không được xem; đổi ID trên URL không cấp thêm quyền. Xem chi tiết còn đánh dấu thông báo đã đọc.

`answer` nhận cả hai trường hợp: accepted=true để nhận; false để từ chối. Lý do từ chối của ứng viên tùy chọn, được trim và giới hạn 1.000 ký tự. Khác với lý do bắt buộc của Director.

`requireAnswerable` yêu cầu còn Sent_Candidate, không qua hạn trả lời và không qua ngày bắt đầu. Sau đó `answerIfEligible` kiểm tra lại trạng thái, hạn, chủ sở hữu, xóa mềm và Application còn Offered ngay trong UPDATE. Hai lần bấm nhận/từ chối gần nhau không được ghi đè câu trả lời trước.

Sau khi Offer và Application commit, `notifyAfterAnswerCommit` mới đánh dấu thông báo đã đọc và báo HR/HM. Hai thao tác có giao dịch REQUIRES_NEW riêng; bước báo phản hồi tải lại Offer để không đọc quan hệ LAZY từ entity đã detached. Nếu không, HM khác HR có thể gây LazyInitializationException rồi rollback toàn bộ phản hồi. Lỗi notification nay có log và không làm mất quyết định. HR nhận link `/offers/{offerId}`, HM nhận link `/applications/{applicationId}` theo quyền hiện hữu.

Ví dụ: nhận thành công rồi bấm từ chối phải lỗi; thời điểm lớn hơn hạn phải lỗi; bằng đúng hạn chưa bị chặn bởi isAfter; ngày bắt đầu bằng hôm nay chưa bị chặn bởi điều kiện “ngày bắt đầu trước hôm nay”. UPDATE hiện kiểm tra lại hạn, ngày bắt đầu, chủ sở hữu, xóa mềm và hồ sơ còn Offered. Mốc xét hạn là thời gian server truyền ngay trước UPDATE, không phải thời điểm commit hay đồng hồ trình duyệt.

Khi được bật, OfferExpirationJob chạy mỗi 60 giây và cập nhật Sent_Candidate đã quá hạn thành Expired; không đổi Application. UI vẫn tính quá hạn tức thời trong lúc chờ job. `initCountdowns` chỉ cập nhật chữ; UPDATE bảo vệ thao tác ghi. Application trên main đã có ánh xạ/query/timeline Expired; job vẫn tắt mặc định chờ xác nhận triển khai chung, không tự bật chỉ vì code đã merge.

## 10. Excel và JavaScript

`executeOfferExport` gửi đúng cấu trúc scope, offerIds, filters={keyword,status,sort}, columns. SELECTED là các checkbox đang có trên trang; FILTERED là kết quả bộ lọc; ALL là mọi Offer chưa xóa. Đây là ba phạm vi khác nhau.

`exportOffersToExcel` kiểm tra danh sách cột cho phép, lấy dữ liệu, ghép OfferExportRow rồi tạo hai sheet. Nếu chọn cột Director, lịch sử được đọc theo lô 500 ID; thứ tự thời gian/ID giảm dần và putIfAbsent giữ log mới nhất mỗi Offer. EntityGraph tải các quan hệ một-một/nhiều-một cần dùng, tránh một truy vấn riêng cho mỗi dòng. Không fetch collection trong query phân trang.

Workbook dùng SXSSFWorkbook với cửa sổ 100 hàng mỗi sheet; Summary dùng chung hàm dịch trạng thái và có đếm Expired. Điều này giảm bộ nhớ cho các hàng workbook, nhưng entity/DTO đầu vào và byte[] kết quả vẫn ở RAM.

`offerRequest` chỉ nhận JSON và tự gắn CSRF, kiểm tra HTTP lẫn success. Excel trả Blob nên dùng nhánh tải riêng. `queueOfferToast` lưu thông báo qua lần reload; thông tin trong sessionStorage không phải dữ liệu nghiệp vụ đã được lưu vào DB.

`offer-form` chỉ gợi ý/kiểm tra tức thời; DTO và service quyết định được lưu hay không. `offer-director` mở modal chưa có nghĩa đã ghi quyết định; nút xác nhận mới gửi request. `candidate-offers` quản lý dialog, lý do nhanh, khóa nút, đếm ngược và gọi trình duyệt in/lưu PDF; nút PDF hiện dùng window.print, không tạo PDF tại backend.

## 11. Cách đọc đoạn khó và đề xuất tên dễ hiểu hơn

Giữ tên code bằng tiếng Anh theo quy ước dự án, giải thích ý nghĩa bằng tiếng Việt. Những tên dưới đây là đề xuất, chưa đổi để tránh ảnh hưởng interface, test và onclick của template.

| Tên hiện tại | Cách diễn đạt dễ nhớ | Đề xuất nếu refactor riêng |
|---|---|---|
| decideOffer | Ghi quyết định của Giám đốc | recordDirectorDecision |
| answer | Ứng viên trả lời thư mời | respondToOffer |
| requireAnswerable | Kiểm tra còn được phản hồi | validateCandidateResponse |
| resolveProposedBy | Helper cũ đã được loại bỏ | Đã dùng CurrentUserService.requireUser và bỏ helper fallback |
| mapToResponse / toResponse | Biến entity thành dữ liệu cho UI | Giữ tên hiện tại, đọc danh sách trường DTO |
| loadLatestApprovals | Lấy quyết định mới nhất theo từng Offer | Giữ tên; nhớ “sắp giảm dần → giữ phần tử đầu của từng ID” |

Với stream lọc ứng viên, đọc theo bốn bước: lấy Application → bỏ hồ sơ không hợp lệ/đã kết thúc → lấy ID duy nhất → đọc Offer hiện hữu theo lô. Có thể viết lại bằng vòng for và biến `interviewResult`, `application`, `eligibleApplicationIds` trong cùng file nếu nhóm thấy dễ đọc hơn; phải giữ thứ tự ưu tiên kết quả và việc loại trùng.

Với điều kiện hết hạn, có thể đặt hai biến `responseDeadlinePassed` và `startDatePassed`, nhưng phải giữ nhánh trạng thái Expired: `isExpiredStatus || (isSent && (responseDeadlinePassed || startDatePassed))`. Đây là đề xuất cách trình bày; không đổi isAfter thành bằng-hoặc-lớn-hơn hoặc bỏ ngoặc nhóm OR.

## 12. Những điểm đã tối ưu và phần còn mở

| Trạng thái | Nội dung | Hành vi hiện tại / bước tiếp |
|---|---|---|
| Đã xử lý | Form bị gán lại lương khi dựng lại | Khởi tạo chỉ dựng preview; gợi ý chỉ điền ô trống khi chọn ứng viên, không làm mất phần lẻ lương gợi ý |
| Đã xử lý | Làm tròn gợi ý thử việc | Làm tròn lên tới 2 chữ số thập phân theo DECIMAL(18,2); BigDecimal ở server vẫn là kiểm tra quyết định |
| Đã xử lý | Tài khoản và lý do Director từ chối | CurrentUserService bắt buộc tài khoản Active; service tự chặn lý do null/trống và quá 2.000 ký tự |
| Đã xử lý | Query dropdown dư ở danh sách HR | Không đọc ứng viên đủ điều kiện ở trang list; giữ model tương thích, form vẫn lấy đủ dữ liệu |
| Đã xử lý | Helper validation không có caller | Đã bỏ validateProbationSalaryRule; validateOfferBusinessRules giữ luật lương đang dùng |
| Đã xử lý | Phản hồi nguyên tử và hồ sơ xung đột | UPDATE kiểm tra chủ sở hữu, trạng thái, xóa mềm, hạn/ngày bắt đầu và hồ sơ Offered; transition hồ sơ 0 dòng thì rollback Offer |
| Code đã merge, chưa bật job | Offer Expired | Offer và Application đã có hỗ trợ; vẫn cần xác nhận triển khai và kiểm tra migration trên môi trường đích |
| Đã xử lý | Dựng model form lặp ở nhiều catch | Đã có renderCreateForm, giữ nguyên DTO/errors/isLockedCandidate/fixedCandidate |
| Đã xử lý | Candidate nhận/từ chối bị rollback do notification | Sau commit, giao dịch riêng, tải lại entity; đã kiểm thử HR khác HM và lỗi từng bước thông báo |
| Cần chốt nghiệp vụ | Có bất kỳ Passed hay kết quả mới nhất Passed | Tạo đề xuất hiện dùng anyMatch Passed; không tự thay bằng kết quả mới nhất vì hai luật không tương đương |

Không đổi database, quyền Director, trạng thái hoặc loại khóa chỉ để giảm dòng code. Comment tăng số dòng văn bản nhưng không tăng thuật toán. Phần Application/TalentPool không được sửa trong 5 phase này theo quyết định của người dùng.

## 13. Tự trả lời khi bảo vệ bài

1. **Tại sao duyệt rồi ứng viên chưa thấy?** Chưa phát hành; Approved/Director_Approved không nằm trong bốn trạng thái nhìn thấy của ứng viên: Sent_Candidate, Accepted, Declined, Expired.
2. **Tại sao đã hiện Draft vẫn lưu lỗi?** Trạng thái có thể đổi giữa lúc mở form và lúc lưu; service kiểm tra lại sau khi lấy khóa.
3. **Tại sao không tạo Offer thứ hai?** Ràng buộc theo ApplicationId và quy tắc không tạo khi còn Offer chưa xóa.
4. **Tại sao có kiểm tra ở JS, DTO và service?** Phản hồi tức thời, kiểm tra request, bảo vệ nghiệp vụ là ba trách nhiệm khác nhau.
5. **Tại sao email lỗi mà Offer vẫn Sent_Candidate?** Phát hành trên cổng đã commit; email là tác vụ sau commit với kết quả riêng.
6. **Tại sao Director Rejected không làm Application Rejected?** Một cái từ chối đề xuất đãi ngộ, một cái kết thúc hồ sơ tuyển dụng.
7. **Tại sao ứng viên từ chối thành Withdrawn?** Offer giữ quy tắc ứng viên tự dừng của Application; không coi là công ty loại hồ sơ.
8. **Tại sao không thay UPDATE có điều kiện bằng save?** Có nguy cơ ghi đè một thay đổi đồng thời vừa được lưu.
9. **Tại sao không gộp mọi filter Approved?** Director có nhóm lịch sử đã duyệt rộng hơn HR/Excel.
10. **Hết hạn có tự đóng đơn không?** Không. Job (khi bật) chỉ đổi Offer sang Expired; Application giữ nguyên.

Thứ tự học: bảng vai trò/trạng thái → tạo và nháp → quyết định Director → phát hành/email → ứng viên trả lời → Excel → các cơ chế khóa/CAS/afterCommit. Mỗi tính năng tập nói được bốn ý: ai gọi, điều kiện trước, dữ liệu đổi, kết quả/lỗi trả về.

## 14. Triển khai Expired an toàn

- Chạy migration 021 trước khi bật job. Script chỉ mở rộng CHECK hiện hữu, không cập nhật dữ liệu Offer/Application.
- Job tắt mặc định; code bốn điểm Application đã có trên main. Chỉ bật `--app.offer.expiration.enabled=true` sau khi hai bên xác nhận migration và kiểm thử tích hợp trên môi trường đích.
- Không bật chỉ vì migration đã chạy hoặc test local đã đạt. Kiểm tra đúng phiên bản ApplicationProgressResolver và MyApplicationRepository/MyApplicationJourney được triển khai cùng Offer.
- Dù job tắt, API vẫn chặn phản hồi quá hạn; khi job bật, candidate vẫn xem được Expired nhưng không trả lời được.
- Hai request/job tranh chấp đều UPDATE có điều kiện trạng thái; kết quả đã Accepted/Declined không bị job ghi đè. Không tự gia hạn, phát hành lại hoặc đóng tin tuyển dụng.
