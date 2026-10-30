# HR Recommendations — bảng gợi ý và trạng thái rỗng

- **Ngày:** 30/10/2026 (Asia/Saigon).
- **Người thực hiện:** LinhDN, theo yêu cầu người dùng.
- **Yêu cầu cuối cùng:** làm bảng HR Recommend theo ảnh tham khảo; không có tìm kiếm/bộ lọc; chuyển liên kết “Xem ứng viên được HM đề xuất” lên đầu trang; thống kê file và số dòng thêm/xóa.
- **Baseline:** chụp từng file ngay trước tác vụ này. Báo cáo không cộng thay đổi đã có từ những lượt Talent Pool trước hoặc file lịch sử đang sửa sẵn.

## Kết quả người dùng nhìn thấy

- Header “Danh sách gợi ý” có icon, tổng ứng viên và số đang chờ phản hồi. Bỏ hoàn toàn toolbar tìm kiếm/bộ lọc; tham số keyword cũ không còn làm ẩn bớt gợi ý trên trang này.
- Bảng có bảy cột: **STT · Ứng viên · Liên hệ · HR gợi ý · Trạng thái · Ngày gửi · Thao tác**. Avatar và tên, email/điện thoại, người gửi và thời gian đều lấy từ dữ liệu hiện hữu.
- Pending dùng màu cảnh báo; HM chấp nhận dùng màu thành công; HM từ chối/đã kết thúc dùng màu trung tính. Dòng giải thích giữ đúng nghiệp vụ: HM đồng ý chưa thay sự đồng ý của ứng viên, HR vẫn cần liên hệ.
- HR thấy Xem hồ sơ và Xóa gợi ý. Xóa chỉ hoạt động khi pending; sau phản hồi nút disabled có giải thích. Hiring Manager giữ Chấp nhận/Từ chối theo canDecide, không có thao tác HR.
- Khi chưa có gợi ý, không dựng bảng/header cột trống: hiển thị khối rỗng gọn với icon, tiêu đề và hướng dẫn. Không thêm dữ liệu giả để lấp bảng.
- Đưa “Xem ứng viên được HM đề xuất” lên nhóm thao tác đầu trang cùng Gợi ý ứng viên/Quay lại tin. Giữ route và quyền hiện hữu.
- Bảng giữ cuộn ngang trong wrapper ở màn hình hẹp, không làm tràn toàn trang. Không thêm framework, font hoặc sửa shared shell.

## Phạm vi dữ liệu và nghiệp vụ

- TalentPoolRecommendationResponse có thêm email, phoneNumber, stateKey và hàm trình bày; giữ constructor cũ để tương thích các caller/fixture đang có.
- Service nối liên hệ từ Candidate.account và stateKey từ review HM gốc; không suy trạng thái gợi ý từ trạng thái tuyển dụng mới về sau.
- Controller bỏ lọc keyword, vẫn giữ phân trang 20 dòng, tổng đếm đúng toàn bộ gợi ý của tin và xử lý URL cũ an toàn.
- Không đổi schema/entity/migration/seed. Không sửa luồng gửi, HM phản hồi, thu hồi gợi ý, CSRF hoặc phân quyền backend. Không stage/commit/push.

## Skill và hướng thiết kế

- DESIGN hiện hữu và UI_RULES/UI_CSS_GUIDE/UI_CATALOG giữ vai trò nền; tái dùng adw-, badge, button và token Mộc.
- UI UX Pro Max: tra table handling/submit feedback, áp dụng wrapper cuộn và trạng thái thao tác rõ ràng.
- Impeccable: refinement trong Operate mode, craft floor; giữ danh tính sản phẩm và kiểm tra ảnh có dữ liệu/không dữ liệu.
- Không cần Taste draw hoặc đổi visual world vì mockup đã chỉ rõ bố cục; không thay palette/type chung.

## Thống kê thay đổi

Đếm bằng Git diff giữa snapshot đầu tác vụ và file cuối tác vụ; không phải toàn bộ diff của branch. Ký hiệu **+ / −** lần lượt là dòng thêm / xóa. File tài liệu mới được tính toàn bộ số dòng thêm.

<!-- FILE_STATS_START -->
| File | Thêm | Xóa | Nội dung |
| --- | ---: | ---: | --- |
| `src/main/java/com/group2/rms/candidate/talentpool/dto/TalentPoolRecommendationResponse.java` | +29 | −1 | Thêm liên hệ/stateKey và helper hiển thị, constructor tương thích. |
| `src/main/java/com/group2/rms/candidate/talentpool/service/TalentPoolService.java` | +3 | −1 | Map email/điện thoại/trạng thái từ dữ liệu và review hiện hữu. |
| `src/main/java/com/group2/rms/candidate/talentpool/controller/TalentPoolController.java` | +2 | −5 | Bỏ tìm kiếm keyword trên trang, giữ phân trang/tổng số. |
| `src/main/resources/templates/talent-pool/recommendations.html` | +40 | −23 | Bảng 7 cột, rỗng gọn, không search/filter; chuyển link HM lên đầu. |
| `src/main/resources/static/css/pages/talent-pool-workspace.css` | +30 | −0 | Alias HR riêng cho cột, liên hệ, badge và nút theo quyền. |
| `src/test/java/com/group2/rms/candidate/application/pipeline/controller/ApplicationPipelineWebTests.java` | +47 | −0 | Kiểm tra bảng/liên hệ/quyền/nút khóa/rỗng/không search và fixture. |
| `src/test/java/com/group2/rms/candidate/talentpool/service/TalentPoolRecommendationTests.java` | +26 | −0 | Kiểm tra liên hệ và stateKey/tone từ review gốc. |
| `docs/management/WORK_LOG.md` | +1 | −0 | Thêm mục và liên kết báo cáo. |
| `docs/management/work_logs/2026-10-30-hr-recommendations-ui.md` | +61 | −0 | Thống kê, quyết định, kiểm chứng và giới hạn. |
| **Tổng 9 file** | **+239** | **−30** | **5 production, 2 test, 2 tài liệu.** |
<!-- FILE_STATS_END -->

## Kiểm chứng

- **92/92 test Java qua**, 0 failure/error/skipped: ApplicationPipelineWebTests 66 và TalentPoolRecommendationTests 26. Maven offline với Byte Buddy agent nạp sẵn; không sửa POM. Log được giữ ngoài target ở `hr-recommendations-test.log` trong thư mục artifact để không mất khi clean build.
- **4/4 test JavaScript** recommendations qua: giữ decision đã chọn, CSRF, chặn submit lặp và khôi phục khi quay lại trang.
- **Chrome thật trên HTML fixture MVC:** HR có 4 hàng, 7 cột, 0 search/filter; 2 nút xóa hoạt động và 2 nút disabled; tone warning/warning/success/neutral đúng từng trạng thái. Liên hệ đủ email/điện thoại; liên kết danh sách HM trong header.
- Mở/Hủy dialog Xóa hoạt động; không gửi thao tác xóa trong lượt browser fixture. HM có 2 form quyết định, không có nút xóa HR. Empty state không dựng table/thead trống.
- Desktop 1440×1000 và mobile viewport 390px: scrollWidth toàn trang 390px, bảng cuộn trong wrapper (1254px/324px), không có JS exception. Đã xem ảnh có dữ liệu và rỗng.
- Ảnh/kết quả nằm ngoài source: `hr-recommendations-desktop.png`, `hr-recommendations-empty.png`, `hr-recommendations-hm.png`, `hr-recommendations-mobile.png`, `hr-recommendations-qa.json`.
- Dữ liệu trong ảnh là fixture, không seed vào database. Chưa kiểm chứng UI/đọc SQL Server/SMTP/PDF end-to-end với tài khoản thật trong tác vụ này. Không có schema hoặc mutation nghiệp vụ mới.

- `git diff --check` và đối chiếu resource source/target qua. Không cộng file lịch sử đã sửa sẵn vào thống kê của tác vụ này.
