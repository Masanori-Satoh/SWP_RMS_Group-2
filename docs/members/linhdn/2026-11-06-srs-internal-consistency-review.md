# Rà soát tính nhất quán nội bộ SRS — phần LinhDN

**Ngày:** 06/11/2026  
**Nguồn duy nhất để kết luận yêu cầu:** `D:/dowload/RMS_SRS (1).docx`.  
**Phạm vi người dùng xác nhận:** Authentication, Account/Department, Talent Pool, Admin và Core/Audit Log. Bao gồm Profile, Notifications và phần phân quyền chung liên quan.  
**Giới hạn:** Chỉ đối chiếu các phần trong chính SRS: entity, business rule, UC, permission matrix, diagram, screen inventory, ảnh màn hình, bảng trường/thao tác và system message. Chưa đối chiếu mã nguồn, database hoặc hành vi ứng dụng. Không sửa file Word gốc. Các đề xuất dưới đây chưa phải quyết định nghiệp vụ đã được duyệt.

## Kết quả

Có **12 nhóm mâu thuẫn/lỗi tài liệu xác định được**, cùng **14 điểm cần bổ sung hoặc làm rõ**. Không quy các điểm thiếu mô tả thành lỗi triển khai.

Ưu tiên xử lý: bảng message; thời hạn OTP; quyền Admin; menu Offer của HM; phân nhánh CV cũ/CV mới; giới hạn SĐT. Sau đó đồng bộ ảnh minh họa và các tham chiếu.

Các diagram cần thiết đã được trích trực tiếp từ DOCX và đọc ở ảnh gốc. Hiện không cần người dùng chụp lại. Trong báo cáo, tên `image*.png` là tên ảnh nhúng trong DOCX, giúp định vị khi cần; không phải tên file mã nguồn. Dùng số mục và tên màn hình để tìm trong Word vì số trang có thể đổi khi Word cập nhật mục lục.

## A. Mâu thuẫn và lỗi tài liệu đã xác định

### SRS-01 — Mã message không duy nhất và có dòng không có mã

**Vị trí:** III.2 System Messages, dòng số thứ tự 2, 4, 5, 6, 17.

- `MSG05` xuất hiện ở dòng 2 và 5, với context/content khác nhau.
- `MSG17` xuất hiện ở dòng 6 cho đăng nhập thất bại và dòng 17 cho đăng tin tuyển dụng.
- Dòng 4 để trống Message code.

**Ảnh hưởng:** Không thể ánh xạ duy nhất message vào yêu cầu hoặc test case. Dẫn `MSG17` không xác định được đang nói về Login hay Job Posting.

**Đề xuất:** Tách mỗi tình huống thành một message có ID duy nhất, rà lại mọi tham chiếu sau khi chốt mã. Chưa tự gán lại các mã bị thiếu vì tài liệu không cho biết mã nào được chủ ý dành cho chúng.

### SRS-02 — Nội dung nhiều message bị ghép nhầm giữa các dòng

**Vị trí:** III.2 System Messages, dòng 2, 4, 5, 6. Đã kiểm tra trực tiếp từng ô XML để loại trừ lỗi do trích xuất.

| Dòng | Context hiện tại | Content hiện tại và lỗi |
| --- | --- | --- |
| 2 | Required field is empty | `Password and confirm-password values do not match.Mật khẩu xác nhận không khớp.` — sai tình huống, dính tiếng Anh và tiếng Việt |
| 4 | Input exceeds maximum length | `A deactivated or blocked account attempts to sign in.` — là mô tả tình huống khóa tài khoản, không phải thông báo quá độ dài |
| 5 | Password confirmation mismatch | Ghép mô tả lỗi server/database với `Không thể hoàn tất yêu cầu. Vui lòng kiểm tra thông tin và thử lại.` — không mô tả đúng lỗi xác nhận mật khẩu |
| 6 | Invalid login credentials | `Thông tin đăng nhập không đúng. Vui lòng thử lại.Đã đăng tin tuyển dụng.` — ghép thông báo thất bại với thành công của nghiệp vụ khác |

**Đề xuất:** Tách đúng bốn tình huống required/max length/password mismatch/invalid login; mỗi Content chỉ chứa chuỗi thực sự hiển thị. Lỗi server/database đã có `MSG11`, không ghép vào password mismatch.

### SRS-03 — OTP đăng ký có hai thời hạn: 15 phút và 1 phút

**Vị trí đối chiếu:** I.5.1.b Screen List, dòng 3 Register OTP Verification và ảnh II.1.2 `image52.png` ↔ II.1.1 Register Account, nút Gửi mã xác minh.

- Screen List: OTP `valid for 15 minutes`.
- Ảnh màn hình Verify email cũng ghi `Mã có hiệu lực trong 15 phút kể từ khi gửi.`
- Register Account: OTP `valid for 1 minute`.
- II.1.2 quy định cooldown gửi lại 60 giây; đây là thời gian chờ gửi lại, không thay thế việc chốt thời hạn OTP.

**Ảnh hưởng:** Cùng một OTP có expected result khác nhau tại phút thứ 2.

**Đề xuất:** Chốt một giá trị và cập nhật đồng thời Screen List, đặc tả màn hình, helper/countdown và test requirement. Với reset password, Screen List dòng 5 và ảnh II.1.5 `image58.png` cùng ghi 15 phút; bảng trường/thao tác II.1.4–1.5 chưa nhắc lại thời hạn. Không có bằng chứng từ SRS rằng reset OTP cũng chỉ có 1 phút.

### SRS-04 — SĐT cùng tài khoản có giới hạn 15 và 20 ký tự

**Vị trí đối chiếu:** II.1.7 View Profile, ảnh `image10.png` ↔ II.3.7 Create Internal Account và II.3.8 Edit Internal Account.

- Ảnh Profile ghi helper SĐT: `Tối đa 15 ký tự.`
- Bảng tạo/sửa tài khoản nội bộ ghi `Max length: 20 characters`.
- I.3.2 Entity Candidate xác định SĐT nằm ở User liên kết; Profile và Admin cùng thao tác dữ liệu tài khoản này.

**Ảnh hưởng:** Admin có thể nhập dữ liệu dài hơn giới hạn màn hình Profile công bố.

**Đề xuất:** Chốt quy tắc SĐT chung, ghi rõ chuẩn hóa/định dạng/độ dài và áp dụng thống nhất. Không tự thay bằng quy tắc 10 số vì lượt này chưa so với code hoặc yêu cầu ngoài SRS.

### SRS-05 — Permission Matrix cấm Admin nhưng phần giải thích lại cho phép

**Vị trí:** I.4.4 Permission Matrix; I.5.1.b Screen List dòng 13 và 44.

- Cột Admin ghi `No` cho View requisitions, Edit/delete requisitions, View applications and CVs, View offer list/details.
- Chú thích 3 lại ghi `Admin sees all requisitions`.
- Chú thích 4 ghi `Admin: any requisition` trong phạm vi chỉnh sửa/xóa.
- Chú thích 6 ghi Director và Admin có thể xem application nhưng không review.
- Đoạn dẫn nói Admin được mở màn hình tuyển dụng để hỗ trợ; Screen List dòng 13 cho Admin mở form tạo requisition; dòng 44 cho Admin mở Internal Offer Management.

**Ảnh hưởng:** Người đọc không xác định được Admin cần nhận dữ liệu hay bị từ chối truy cập. Riêng quyền sửa/xóa không thể suy ra từ quyền hỗ trợ xem.

**Đề xuất:** Lập lại từng quyền Admin theo hành động: view/create/edit/delete/approve/review/finalize/close; đồng bộ bảng, chú thích và Screen List. Không dùng một câu “hỗ trợ” để thay cho quyền cụ thể.

### SRS-06 — Dashboard mô tả HM có chức năng Offer Proposal trong khi quyền cấm

**Vị trí:** I.5.1.b Screen List dòng 7 Dashboard ↔ I.4.4 và Screen List dòng 44–45.

- Dashboard liệt kê HM có `offer-proposal functions`.
- Permission Matrix ghi HM không được xem/tạo/duyệt Offer Proposal.
- Screen List dòng 44 nói HM không mở Internal Offer Management; dòng 45 chỉ cho HR tạo Offer.

**Đề xuất:** Nếu giữ chính sách quyền hiện mô tả ở bảng, sửa menu HM thành theo dõi trạng thái tuyển dụng/thông báo trạng thái Offer. Đây là đối chiếu tài liệu phân quyền chung liên quan, không phải review riêng module Offer của thành viên khác.

### SRS-07 — UC dùng CV mới đi thẳng HM Passed, diagram lại quay về Applied

**Vị trí:** I.4.2 UC 45 Confirm Candidate Participation; I.5.1.b dòng 33; I.2.6 diagram `image51.png`; I.5.1.a flow 2.6 `image90.png`; II.4.11.

- UC 45 và Screen List dòng 33: khi ứng viên đồng ý, HR nộp CV `new or existing`, trạng thái thành HM Passed/HM_Passed và được lên lịch.
- Business flow 2.6 tách hai nhánh: dùng CV cũ → HR xác nhận → HM_Passed; đổi CV → Candidate tự nộp → Applied → normal screening.
- Screen flow 2.6 và ảnh II.4.11 cũng hướng dẫn ứng viên muốn đổi CV phải tự nộp tại trang tuyển dụng.

**Ảnh hưởng:** Một nhánh được bỏ qua sàng lọc, nhánh kia bắt buộc sàng lọc; đồng thời khác actor nộp CV.

**Đề xuất:** Viết lại UC 45 và dòng 33 thành hai nhánh rõ ràng, ưu tiên giữ cấu trúc đang thể hiện nhất quán ở diagram và màn hình nếu nhóm xác nhận đó là ý định nghiệp vụ. Đồng bộ `CV status` trong UC thành `ApplicationStatus` để tránh nhầm CV là entity có lifecycle riêng.

### SRS-08 — Xác nhận chọn ứng viên xong về hai màn hình khác nhau

**Vị trí:** I.5.1.a flow 2.6 `image90.png` ↔ II.4.13 Confirm Recommend for HM.

- Diagram ghi `Save / Cancel: stay on Talent Pool List` cho popup Add / Recommend Candidates Confirmation.
- II.4.13, nút Gửi X gợi ý cho HM: gửi thành công tự chuyển tới `/talent-pool/jobs/{id}/recommendations`.

**Đề xuất:** Tách nhánh Cancel và Save success. Nếu giữ điều hướng ở II.4.13, Cancel quay về selection còn gửi thành công về danh sách gợi ý. Nhánh HM thêm trực tiếp cũng cần ghi đích riêng nếu khác.

### SRS-09 — Hai mũi tên extend của UC Admin ngược với mô tả UC

**Vị trí:** I.4.3.8 UCs for System Admin, `image2.png` ↔ I.4.2 UC 10/12 và 14/17.

- Diagram: mũi tên nét đứt giữa View Internal Account List và Update Internal Account trỏ về Update Internal Account.
- Diagram: mũi tên giữa View Department List và Toggle Department Status trỏ về Toggle Department Status.
- Bảng UC mô tả Update Account và Toggle Department là các extension của màn danh sách.

**Đề xuất:** Với quan hệ extend đang được bảng mô tả, đặt đầu mũi tên về UC cơ sở: View Internal Account List / View Department List. Kiểm tra lại hai đường này trên bản diagram biên tập, không chỉ đổi nhãn.

### SRS-10 — Quyền sửa/xóa note dẫn tới chú thích không tồn tại

**Vị trí:** I.4.4 Permission Matrix.

- Hàng Edit/delete internal note ghi `Restricted14` cho HR/HM.
- Phần Footnotes không có số 14; quy tắc chỉ sửa/xóa note của chính mình lại bị đánh số 13, trùng với chú thích Interview panel.

**Đề xuất:** Đổi chú thích note thành 14 và rà lại mọi tham chiếu. Quy tắc “chỉ tác giả được sửa/xóa” đã có trong UC 89/90 và menu owned notes; vấn đề ở đây là tham chiếu sai, không phải tài liệu chưa có quy tắc.

### SRS-11 — Ảnh minh họa gắn tên ứng viên với CV của người khác

**Vị trí:** Các ảnh thuộc Talent Pool trong II.4 và II.5.

| Mục / ảnh | Tên hồ sơ hoặc tiêu đề | Tên đọc được trong CV |
| --- | --- | --- |
| II.4.8, `image19.png` | Vũ Hoàng Cường | Lê Quốc Bảo |
| II.4.11, `image82.png` | Lê Duy Nam | Trần Thu Hà |
| II.5.14, `image68.png` | Huỳnh Khánh Quyên | Nguyễn Minh Khoa |
| II.5.17, `image102.png` | Trịnh Tuấn Thắng | Nguyễn Minh Khoa |

**Ảnh hưởng:** Ảnh không thể làm ví dụ đáng tin cậy cho chọn đúng CV nguồn và xem đúng hồ sơ. Đây là lỗi dữ liệu minh họa trong SRS; chưa có bằng chứng ứng dụng lấy nhầm CV.

**Đề xuất:** Thay CV mẫu hoặc chụp lại ảnh sao cho tên, email và CV cùng người. Với mỗi dòng, ghi rõ tin nguồn/CV nguồn, tránh dùng tên khác rồi giải thích là CV lịch sử.

### SRS-12 — Forgot Password được mô tả vừa là popup vừa là màn hình riêng

**Vị trí:** I.5.1.b dòng 4 ↔ I.5.1.a Authentication screen flow và II.1.3–1.4.

- Screen List gọi Forgot Password là `recovery pop-up`.
- Diagram dùng hình chữ nhật Screen, có legend phân biệt với popup; Login điều hướng tới `/forgot-password`; ảnh II.1.4 là trang recovery riêng.

**Đề xuất:** Nếu giữ flow và ảnh, sửa dòng 4 thành recovery screen/page. Đây là lỗi nhất quán mô tả màn hình, ưu tiên thấp hơn lỗi nghiệp vụ.

## B. Điểm thiếu hoặc cần quyết định — chưa kết luận là mâu thuẫn chắc chắn

### GAP-01 — Chính sách lỗi đăng nhập chung và lỗi tài khoản không khả dụng

**Nguồn:** III.1 GBR-03; III.2 MSG07; II.1.3.

GBR-03 yêu cầu thông báo chung khi login fail, còn MSG07 có thông báo riêng cho inactive/blocked. Message riêng này không chỉ ra email hay mật khẩu sai, nhưng vẫn cần làm rõ có được phép phân biệt account state không, và chỉ hiển thị sau khi kiểm tra mật khẩu hay luôn hiển thị. Chốt rõ ngoại lệ của GBR-03 hoặc dùng một message chung; chưa khẳng định chỉ riêng MSG07 là bằng chứng có lỗ hổng hoặc vi phạm quy tắc.

### GAP-02 — Blocked xuất hiện trong lifecycle nhưng chưa có cách biểu diễn/quản lý

**Nguồn:** I.3.2 Entity User; II.3.2–3.3, 3.6, 3.8–3.9; MSG07.

User có Active/Inactive/Blocked, trong khi danh sách, bộ lọc, form và nhãn chi tiết chỉ mô tả Active/Inactive. Chưa rõ Blocked là trạng thái hiện hành, dự phòng hay được ánh xạ vào Inactive. Không tự suy ra cần xóa Blocked; cần chốt và cập nhật toàn bộ tài liệu.

### GAP-03 — Profile chưa đặc tả đủ các trường và thao tác mà UC/ảnh đã có

**Nguồn:** I.4.2 UC94; II.1.7–1.8; III.2 MSG54–55.

UC94 cho sửa contact, avatar, background; ảnh View Profile có SĐT và helper ảnh JPEG/PNG/WebP tối đa 2 MB. Bảng Edit Profile chỉ ghi Full Name, Email, Role, Department; thiếu SĐT, thao tác upload ảnh, điều kiện nút Save và trường riêng Candidate. Ảnh II.1.7 cũng đang chứa input và nút lưu dù tiêu đề là View Profile. Cần thống nhất màn Profile dùng chung hay tách view/edit, định nghĩa field theo role, required/format/giới hạn và message ảnh lỗi. Dòng Screen List 8 chỉ nhắc internal users, trong khi matrix cho cả Candidate: bổ sung Candidate để khỏi bỏ sót phạm vi, không coi câu này tự nó là cấm Candidate.

### GAP-04 — Change Password và Logout chưa có đặc tả thao tác chi tiết tương ứng

**Nguồn:** I.4.2 UC4/6; I.5.1.b dòng 9/11; Authentication flow; II.1; MSG48/56.

Có UC và screen inventory nhưng thiếu bảng chi tiết riêng cho current/new/confirm password, sai mật khẩu hiện tại, dùng lại mật khẩu, session sau đổi/reset, và hành vi xác nhận/hủy logout. Cần ghi rõ session hiện tại/phiên khác có hết hiệu lực không, logout thành công đi đâu và thông báo hiển thị ở đâu. Không tự lấy hành vi từ code để điền.

### GAP-05 — OTP thiếu hợp đồng hết hạn, gửi lại và số lần thử

**Nguồn:** II.1.1–1.6; Screen List dòng 3/5; MSG41–44.

Đăng ký có TTL 1 phút tại một chỗ và cooldown 60 giây; cần phân biệt OTP hết hạn với pending registration/reset session hết hạn. Nếu OTP hết hạn thì có còn gửi lại được không? Sau 5 lần sai, “session expires/invalidated” nhưng MSG42 lại hướng dẫn yêu cầu mã mới: cần chỉ rõ bắt đầu lại bước 1 hay resend tại bước 2. Bổ sung OTP cũ có bị vô hiệu sau resend, gửi email lỗi có giữ OTP cũ/cooldown không, và verify thành công dùng lại mã bị xử lý thế nào. Hai đồng hồ cùng 60 giây tự nó chưa chứng minh chức năng resend không thể hoạt động.

### GAP-06 — Chưa chốt đồng bộ role Hiring Manager với Department.Manager

**Nguồn:** II.3.7–3.8 và II.3.11; I.3.2 User/Department.

Tạo/sửa account có thể chọn HM + department, còn sửa Department lại tự đổi người được chọn thành trưởng phòng và đổi trưởng phòng cũ thành Interviewer. Thiếu quy tắc đồng bộ khi tạo HM thứ hai, chuyển HM sang phòng khác, đổi role, vô hiệu hóa manager, hoặc chọn một HR/Director/Admin làm manager. Cần định nghĩa số manager Active tối đa, tác động role/session/quyền và cập nhật ManagerId. Đây là thiếu business rule, chưa có hai câu khẳng định đối lập về số manager.

### GAP-07 — Uniqueness username/email chưa thống nhất mức mô tả

**Nguồn:** II.1.1 ↔ II.3.7–3.8 ↔ I.3.2 User.

Register cấm username trùng cả username lẫn email người khác và ngược lại. Admin chỉ nói username/email unique riêng; chưa nói cùng namespace, case sensitivity, trim, hoặc email hiện tại của chính account được phép giữ. Với login nhận username/email, nên ghi một rule chung và dẫn từ mọi màn hình. Không kết luận Admin cho phép trùng chéo chỉ vì bảng đang viết thiếu.

### GAP-08 — MSG109 chưa mô tả ngoại lệ giữ phòng ban đã vô hiệu hóa

**Nguồn:** II.3.8, trường Department ↔ III.2 MSG109.

Edit Account cho giữ phòng ban hiện tại dù Inactive; MSG109 có context “an account uses ... a deactivated department”. Cần giới hạn lỗi cho tạo mới hoặc chuyển sang phòng Inactive khác, nếu muốn giữ ngoại lệ đã mô tả. Không coi việc giữ phòng hiện tại là lỗi chỉ dựa trên wording tổng quát của message.

### GAP-09 — Message Talent Pool chưa phủ đủ actor và thao tác

**Nguồn:** UC56, 87–90, 97–98; Permission Matrix; II.4.9/4.11; MSG74–81.

MSG74/75 chỉ ghi HR dù HM cũng được mark/thêm note. Chưa có mapping cụ thể cho HM thêm trực tiếp, bỏ lưu tiềm năng, sửa/xóa note thành công, note trống/quá 1000 ký tự, sửa/xóa note người khác, selection không hợp lệ. Có thể tái dùng MSG08–12/103 nếu quy định mapping rõ; không bắt buộc mỗi thao tác phải có message ID mới. Chuẩn hóa tên role tiếng Việt trên màn hình/message sau khi chốt thuật ngữ.

### GAP-10 — Điều kiện chọn/gợi ý ứng viên và CV nguồn chưa kiểm thử được

**Nguồn:** Screen List dòng 28–29; II.4.12–4.13; II.5.13/5.15.

Các cụm `eligible`, “CV nguồn hợp lệ”, `canSuggest`, `canRecommend` chưa định nghĩa đầy đủ. Cần nêu ảnh hưởng của trạng thái account, IsPotential, CV thiếu/hỏng, application chính thức đã có, suggestion đang chờ/đã từ chối/đã rút, trạng thái và hạn tin đích, manager phụ trách; quy tắc khi chỉ một phần selection hợp lệ; kiểm tra lại tại thời điểm xác nhận. Đây là danh sách cần chốt, không phải khẳng định hiện tại phải cấm tất cả trường hợp trên.

### GAP-11 — Nút quyết định HM được liệt kê ở phần HR nhưng thiếu điều kiện hiển thị

**Nguồn:** II.4.14; II.5.15–5.16; Permission Matrix.

II.4.14 liệt kê Chấp nhận/Từ chối trong chương HR, dù quyết định thuộc HM; II.5.15 mới ghi rõ pending + assigned HM. Cần viết điều kiện tương tự ở mọi bản mô tả dùng chung: HR chỉ xem trạng thái/rút pending theo quyền; HM phụ trách mới quyết định khi pending. Không kết luận HR thật sự có quyền quyết định chỉ vì nút được liệt kê trong chương này.

### GAP-12 — Phân biệt Rejected của suggestion nội bộ với application chính thức

**Nguồn:** BR-TAL-01; Entity Application; Screen List dòng 34; II.4.11; MSG77–81.

Tài liệu đã nói declined suggestion thành Rejected và không hiện cho Candidate. Tuy nhiên Entity Application diễn giải Rejected chung, còn vòng đời pending/accepted/declined/withdrawn recommendation chưa có bảng định nghĩa riêng. Cần ghi cách phân loại internal placeholder, điều kiện chuyển thành formal application, có cho tự ứng tuyển sau khi từ chối gợi ý không, và quy tắc không tạo hai hồ sơ chính thức. Không đề xuất thêm bảng/enum/schema trong lượt review này.

### GAP-13 — Audit Log thiếu đặc tả dữ liệu hiển thị và xử lý bộ lọc

**Nguồn:** I.3.2 AuditLog; UC85; II.3.13 và ảnh `image87.png`.

Bảng II.3.13 chỉ mô tả filter/button, trong khi ảnh có Thời gian, Người thực hiện, Hành động, Đối tượng, ID, Nội dung cũ/mới. Cần bổ sung cột, cách biểu diễn actor hệ thống, entity đã xóa, giá trị cũ/mới, thứ tự mặc định, timezone, khoảng ngày bao gồm hai đầu và from > to. Cần định nghĩa sự kiện Auth/Account/Department/Talent Pool phải ghi, dữ liệu nhạy cảm phải che/không ghi và phạm vi đọc chỉ Admin. Tính immutable đã có; không báo thiếu tính immutable.

### GAP-14 — UI ghi chú có thời điểm cập nhật nhưng entity chỉ mô tả thời điểm tạo

**Nguồn:** I.3.2 TalentPoolNote; I.3.1 ERD `image13.png`; II.4.9.

II.4.9 hiển thị thời gian tạo/cập nhật. Entity và ERD TalentPoolNote chỉ liệt kê CreatedAt; không nêu nguồn last modified. Cần ghi rõ UI chỉ hiện CreatedAt hay lấy thời gian sửa từ dữ liệu/audit khác. Không tự kết luận phải thêm UpdatedAt vào database.

## C. Những điểm đã tương đối nhất quán — không báo nhầm thành lỗi

- Mark potential là cờ Candidate.IsPotential, không tự đổi trạng thái application hoặc interview: BR-TAL-01, flow 2.5 và Screen List dòng 35 phù hợp nhau.
- HM chấp nhận recommendation chưa phải Candidate đồng ý; HR vẫn phải liên hệ: flow 2.6, MSG78 và màn confirmation nêu cùng quy trình.
- Chỉ HR/HM đọc ghi chú nội bộ, tác giả mới sửa/xóa: UC, diagram và bảng màn hình có quy tắc; cần sửa số chú thích như SRS-10.
- Candidate account không tạo/sửa thủ công qua Admin: Screen List dòng 56 và II.3.3/3.9 phù hợp.
- Department bị vô hiệu hóa, dữ liệu liên quan giữ lại: Entity Department và MSG52 phù hợp. Tác động cụ thể tới manager/account vẫn cần GAP-06.
- Monitoring Email chỉ kiểm tra kết nối/xác thực SMTP, không chứng minh thư đến người nhận; II.3.5 đã nói rõ. Không ghi nhận đây là kiểm thử gửi mail thành công.
- AI Configuration trong Screen List dòng 62 đã ghi planned/unavailable; không coi việc không thấy popup cấu hình trong ảnh Monitoring là mâu thuẫn hiện hành.
- Tên Delete Account Confirmation ở Screen List dòng 55 đã được giải thích là toggle Active/Inactive, không xóa vật lý; nên đổi tên cho gọn nghĩa nhưng không báo là nghiệp vụ xóa tài khoản mâu thuẫn.

## D. Thứ tự sửa tài liệu đề xuất

1. Chốt OTP, quyền Admin/HM và hai nhánh CV cũ/mới trước khi viết expected result.
2. Sửa bảng message bị ghép ô/mã trùng; xác lập mapping message cho các thao tác thuộc phạm vi.
3. Đồng bộ Profile/SĐT, trạng thái tài khoản và quy tắc manager giữa Account/Department.
4. Sửa diagram extend, đích điều hướng sau confirmation và footnote note.
5. Thay ảnh CV sai người; bổ sung bảng trường/thao tác Profile, Change Password, Logout, Audit Log và validation còn thiếu.

## Kiểm chứng và bàn giao

- Đọc nội dung DOCX theo cấu trúc paragraph/table; kiểm tra trực tiếp ô gốc của bảng message bị lỗi.
- Quan sát ảnh gốc của context diagram, ERD, UC Guest/User/Admin/HR/HM, các screen flow liên quan và ảnh màn hình thuộc phạm vi.
- Đối chiếu mỗi finding với vị trí trong cùng SRS; không lấy báo cáo triển khai hoặc memory làm chuẩn yêu cầu.
- Không chạy ứng dụng, test, query database hoặc đọc source để đánh giá phù hợp SRS.
- Chỉ thêm báo cáo/work log và liên kết quản lý tài liệu; không sửa DOCX, code hay database; không commit/push.
