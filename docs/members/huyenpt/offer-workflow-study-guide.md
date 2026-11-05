# Học luồng Offer Management — HuyenPT

Tài liệu mô tả hành vi đang có trong mã nguồn sau commit `78a99cbb`. Các mục “đề xuất” chưa được triển khai trong lần thêm comment này. Dùng tài liệu cùng comment tiếng Việt tại controller, service, repository và JavaScript; không học chỉ từ tên nút trên giao diện.

## 1. Nắm ba vai trò trước

| Vai trò | Được làm trong Offer | Không được làm |
|---|---|---|
| HR | Xem, lọc, tạo/sửa/xóa nháp, trình duyệt, phát hành thư mời, xuất Excel | Duyệt/từ chối thay Giám đốc |
| Director | Xem đề xuất không phải Draft, duyệt/từ chối, xem lịch sử; endpoint xuất Excel cũng cho phép Director | Tạo, sửa, xóa nháp, phát hành |
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
| Ứng viên nhận | Accepted | Yêu cầu chuyển Offered → Hired |
| Ứng viên từ chối | Declined | Yêu cầu chuyển Offered → Withdrawn |

`OfferApproval.status` là **quyết định trong lịch sử**, chỉ dùng Approved/Rejected; không phải toàn bộ vòng đời Offer. Một Offer Accepted vẫn có thể có log Director Approved từ trước.

Hai chuỗi Approved/Rejected cũ còn được hỗ trợ ở một số chỗ để đọc dữ liệu tương thích. Không tự đổi tên trạng thái, cột DB hoặc biến template khi thống nhất cách gọi UI.

`ApplicationStatusService.onOfferAnswered` chỉ đổi hồ sơ nếu hồ sơ còn Offered; hiện tại không ném lỗi khi số dòng cập nhật bằng 0. Vì vậy bảng trên mô tả chuyển trạng thái mong muốn, không phải bảo đảm rằng mọi thay đổi đồng thời của hồ sơ đều đã được chặn.

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

Lý do từ chối của Director bắt buộc ở `OfferController.rejectOfferJson`; DTO giới hạn tối đa 2.000 ký tự. Hiện service dùng chung chưa tự kiểm tra lý do trống nếu bị gọi trực tiếp bằng Java. Không học nhầm rằng service đã bao hết điều kiện này.

Thẻ “đã duyệt/từ chối” của Director đếm log của chính người đó; bộ đếm trạng thái của danh sách đếm vòng đời Offer. Nhóm “đã duyệt” trên danh sách Director gồm cả Sent_Candidate/Accepted/Declined; bộ lọc Approved của HR/Excel không có nghĩa nhóm rộng như vậy.

## 8. HR phát hành và gửi email

`sendOfferToCandidate` chỉ nhận Approved/Director_Approved và hồ sơ chưa kết thúc. Khóa ghi Offer ngăn hai yêu cầu HR cùng phát hành trên cùng bản ghi.

Hạn trả lời = thời điểm phát hành + responsePeriodDays. Ngày của hạn này phải trước ngày bắt đầu dự kiến, nên thư mời đã duyệt lâu vẫn có thể cần được xử lý nghiệp vụ nếu đến lúc gửi không còn đủ thời gian.

Sau khi đặt Sent_Candidate, service chuyển hồ sơ sang Offered bằng điều kiện “trạng thái trong DB vẫn bằng trạng thái vừa đọc”. Nếu thất bại thì ném lỗi để rollback toàn bộ việc phát hành. Không thay đoạn này bằng setApplicationStatus rồi save, vì có thể ghi đè hồ sơ vừa bị rút/đóng.

Email chạy trong `afterCommit`: không gửi trước khi thư mời được lưu thành công. `OfferMailSender` trả SENT, TEST_RECIPIENT hoặc NOT_SENT. TEST_RECIPIENT không phải gửi thành công tới ứng viên thực. Thiếu SMTP/gửi lỗi không biến Offer về trạng thái đã duyệt; UI báo đã phát hành nhưng email chưa gửi tới ứng viên.

Thông báo trong ứng dụng của các đường HR tạo/trình/phát hành hiện không phải tất cả đều đã được chuyển sang afterCommit như quyết định Director.

## 9. Ứng viên xem và trả lời

Query chỉ lấy thư mời thuộc UserId đang đăng nhập, thuộc Sent_Candidate/Accepted/Declined. ID của người khác không được xem; đổi ID trên URL không cấp thêm quyền. Xem chi tiết còn đánh dấu thông báo đã đọc.

`answer` nhận cả hai trường hợp: accepted=true để nhận; false để từ chối. Lý do từ chối của ứng viên tùy chọn, được trim và giới hạn 1.000 ký tự. Khác với lý do bắt buộc của Director.

`requireAnswerable` yêu cầu còn Sent_Candidate, không qua hạn trả lời và không qua ngày bắt đầu. Sau đó `answerIfSent` chỉ cập nhật khi trạng thái vẫn Sent_Candidate. Hai lần bấm nhận/từ chối gần nhau không được ghi đè câu trả lời trước.

Ví dụ: nhận thành công rồi bấm từ chối phải lỗi; thời điểm lớn hơn hạn phải lỗi; bằng đúng hạn chưa bị chặn bởi isAfter; ngày bắt đầu bằng hôm nay chưa bị chặn bởi điều kiện “ngày bắt đầu trước hôm nay”. Điều kiện thời gian hiện được đọc trước UPDATE, chưa nằm trong chính UPDATE.

Hết hạn không tự ghi Expired vào DB và không tự đóng Application. `isResponseExpiredAt` tính nhãn/quyền bấm; `initCountdowns` chỉ cập nhật chữ đếm ngược. Đổi đồng hồ hoặc sửa DOM không bỏ được kiểm tra server.

## 10. Excel và JavaScript

`executeOfferExport` gửi đúng cấu trúc scope, offerIds, filters={keyword,status,sort}, columns. SELECTED là các checkbox đang có trên trang; FILTERED là kết quả bộ lọc; ALL là mọi Offer chưa xóa. Đây là ba phạm vi khác nhau.

`exportOffersToExcel` kiểm tra danh sách cột cho phép, lấy dữ liệu, ghép OfferExportRow rồi tạo hai sheet. Nếu chọn cột Director, lịch sử được đọc theo lô 500 ID; thứ tự thời gian/ID giảm dần và putIfAbsent giữ log mới nhất mỗi Offer. EntityGraph tải các quan hệ một-một/nhiều-một cần dùng, tránh một truy vấn riêng cho mỗi dòng. Không fetch collection trong query phân trang.

`offerRequest` chỉ nhận JSON và tự gắn CSRF, kiểm tra HTTP lẫn success. Excel trả Blob nên dùng nhánh tải riêng. `queueOfferToast` lưu thông báo qua lần reload; thông tin trong sessionStorage không phải dữ liệu nghiệp vụ đã được lưu vào DB.

`offer-form` chỉ gợi ý/kiểm tra tức thời; DTO và service quyết định được lưu hay không. `offer-director` mở modal chưa có nghĩa đã ghi quyết định; nút xác nhận mới gửi request. `candidate-offers` quản lý dialog, lý do nhanh, khóa nút, đếm ngược và gọi trình duyệt in/lưu PDF; nút PDF hiện dùng window.print, không tạo PDF tại backend.

## 11. Cách đọc đoạn khó và đề xuất tên dễ hiểu hơn

Giữ tên code bằng tiếng Anh theo quy ước dự án, giải thích ý nghĩa bằng tiếng Việt. Những tên dưới đây là đề xuất, chưa đổi để tránh ảnh hưởng interface, test và onclick của template.

| Tên hiện tại | Cách diễn đạt dễ nhớ | Đề xuất nếu refactor riêng |
|---|---|---|
| decideOffer | Ghi quyết định của Giám đốc | recordDirectorDecision |
| answer | Ứng viên trả lời thư mời | respondToOffer |
| requireAnswerable | Kiểm tra còn được phản hồi | validateCandidateResponse |
| resolveProposedBy | Helper tìm người thực hiện, còn fallback cũ | Dùng CurrentUserService.requireUser; không chỉ đổi tên rồi giữ fallback |
| mapToResponse / toResponse | Biến entity thành dữ liệu cho UI | Giữ tên hiện tại, đọc danh sách trường DTO |
| loadLatestApprovals | Lấy quyết định mới nhất theo từng Offer | Giữ tên; nhớ “sắp giảm dần → giữ phần tử đầu của từng ID” |

Với stream lọc ứng viên, đọc theo bốn bước: lấy Application → bỏ hồ sơ không hợp lệ/đã kết thúc → lấy ID duy nhất → đọc Offer hiện hữu theo lô. Có thể viết lại bằng vòng for và biến `interviewResult`, `application`, `eligibleApplicationIds` trong cùng file nếu nhóm thấy dễ đọc hơn; phải giữ thứ tự ưu tiên kết quả và việc loại trùng.

Với điều kiện hết hạn, có thể đặt hai biến `responseDeadlinePassed` và `startDatePassed`, rồi trả về `isSent && (responseDeadlinePassed || startDatePassed)`. Đây là đề xuất cách trình bày; không được đổi isAfter thành bằng-hoặc-lớn-hơn hoặc bỏ ngoặc nhóm OR.

## 12. Tối ưu còn hợp lý — đề xuất riêng, không trộn vào comment

| Ưu tiên | Bằng chứng hiện tại | Đề xuất và kiểm thử cần có |
|---|---|---|
| Cao | initOfferForm gọi handleSelectPassedCandidate khi select có giá trị; hàm này gán lương gợi ý kể cả ô đã có dữ liệu | Tách thao tác “khởi tạo lại form” khỏi “người dùng đổi ứng viên”; test form lỗi giữ nguyên mức lương đã nhập |
| Cao | autoCalculateProbationSalary dùng Math.round; input cho phép step=any | Chốt cách xử lý phần lẻ; nếu chọn số VNĐ nguyên không dưới 85%, cân nhắc Math.ceil. Ví dụ 10.000.004 × 85% = 8.500.003,4 nhưng Math.round gợi ý 8.500.003, thấp hơn mức kiểm tra server |
| Cao | resolveProposedBy vẫn có fallback lấy user đầu tiên; reject lý do trống chỉ chặn ở controller | Dùng CurrentUserService và đưa kiểm tra nghiệp vụ bắt buộc xuống service; test tài khoản thiếu/không hoạt động, lời gọi Java trực tiếp |
| Vừa | listOffers HR vẫn lấy passedCandidates nhưng hiện chỉ form.html dùng model này trong các template Offer | Xác minh cả fragment/test/consumer rồi bỏ truy vấn/model không cần ở danh sách; giữ nguyên dữ liệu ở form. Không tự đổi biến binding của form |
| Vừa | validateProbationSalaryRule chỉ còn định nghĩa; luật lương đang nằm trong validateOfferBusinessRules | Xác minh caller rồi bỏ helper chết; không tạo thêm validator class chỉ để dời vài dòng |
| Vừa | Mở lại form tạo khi lỗi dựng model lặp ở nhiều nhánh catch | Tách helper private dựng model form trong controller hiện có, giữ DTO/errors/isLockedCandidate/fixedCandidate |
| Cần chốt nghiệp vụ | Tạo đề xuất chấp nhận bất kỳ kết quả Passed trong lịch sử; trang chi tiết lại hiển thị kết quả phỏng vấn mới nhất | Chốt “có Passed” hay “kết quả mới nhất Passed” trước khi sửa, không coi hai cách tương đương |
| Cần kiểm thử đồng thời | Hạn trả lời kiểm tra trước UPDATE; ApplicationStatusService bỏ qua transition 0 dòng | Thiết kế điều kiện cập nhật nhất quán cho thời gian/hồ sơ; phối hợp chủ module hồ sơ nếu cần sửa phần dùng chung |

Đây là thứ tự ưu tiên để bàn tiếp, không phải danh sách lỗi đã được sửa trong commit tài liệu này. Không đổi database, quyền Director, trạng thái hoặc loại khóa chỉ để giảm dòng code. Comment làm tăng số dòng văn bản nhưng không tăng số file production hay thay đổi thuật toán.

## 13. Tự trả lời khi bảo vệ bài

1. **Tại sao duyệt rồi ứng viên chưa thấy?** Chưa phát hành; Approved/Director_Approved không nằm trong ba trạng thái nhìn thấy của ứng viên.
2. **Tại sao đã hiện Draft vẫn lưu lỗi?** Trạng thái có thể đổi giữa lúc mở form và lúc lưu; service kiểm tra lại sau khi lấy khóa.
3. **Tại sao không tạo Offer thứ hai?** Ràng buộc theo ApplicationId và quy tắc không tạo khi còn Offer chưa xóa.
4. **Tại sao có kiểm tra ở JS, DTO và service?** Phản hồi tức thời, kiểm tra request, bảo vệ nghiệp vụ là ba trách nhiệm khác nhau.
5. **Tại sao email lỗi mà Offer vẫn Sent_Candidate?** Phát hành trên cổng đã commit; email là tác vụ sau commit với kết quả riêng.
6. **Tại sao Director Rejected không làm Application Rejected?** Một cái từ chối đề xuất đãi ngộ, một cái kết thúc hồ sơ tuyển dụng.
7. **Tại sao ứng viên từ chối thành Withdrawn?** Theo ApplicationStatusService, đây là ứng viên tự dừng, khác công ty loại hồ sơ.
8. **Tại sao không thay UPDATE có điều kiện bằng save?** Có nguy cơ ghi đè một thay đổi đồng thời vừa được lưu.
9. **Tại sao không gộp mọi filter Approved?** Director có nhóm lịch sử đã duyệt rộng hơn HR/Excel.
10. **Hết hạn có tự đóng đơn không?** Hiện chưa; nhãn quá hạn và điều kiện trả lời không phải job cập nhật trạng thái.

Thứ tự học: bảng vai trò/trạng thái → tạo và nháp → quyết định Director → phát hành/email → ứng viên trả lời → Excel → các cơ chế khóa/CAS/afterCommit. Mỗi tính năng tập nói được bốn ý: ai gọi, điều kiện trước, dữ liệu đổi, kết quả/lỗi trả về.
