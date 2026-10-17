# Ghi chú nghiên cứu cấu trúc kiểm thử

Tra cứu ngày 17/10/2026. Đây là ghi chú áp dụng cho RMS, không sao chép source/cấu hình từ các repo. Các URL nhánh main có thể thay đổi sau thời điểm đọc.

## Spring Petclinic

Nguồn: [repository chính thức](https://github.com/spring-projects/spring-petclinic), [OwnerControllerTests](https://github.com/spring-projects/spring-petclinic/blob/main/src/test/java/org/springframework/samples/petclinic/owner/OwnerControllerTests.java), [ClinicServiceTests](https://github.com/spring-projects/spring-petclinic/blob/main/src/test/java/org/springframework/samples/petclinic/service/ClinicServiceTests.java), [pom](https://github.com/spring-projects/spring-petclinic/blob/main/pom.xml).

Controller tests dùng WebMvcTest, MockMvc và mock repository; persistence tests dùng DataJpaTest, có transaction và dữ liệu kiểm thử. Áp dụng cho RMS: phân biệt test web/binding với test query/database; xác minh quan hệ và dữ liệu đọc lại thay vì chỉ HTTP status. Pom main tại lần đọc là Boot 4.1.0, khác RMS 3.5.16. Không chép import `org.springframework.boot.data.jpa.test...` hoặc dependency Boot 4 vào dự án này. Sử dụng package/API Boot 3.5 đã có trong test RMS khi triển khai sau.

## Spring Guide Testing the Web Layer

Nguồn: [repo gs-testing-web](https://github.com/spring-guides/gs-testing-web), [guide chính thức](https://spring.io/guides/gs/testing-web/), [complete pom](https://github.com/spring-guides/gs-testing-web/blob/main/complete/pom.xml).

Guide phân biệt context test, full context và web slice. WebMvcTest chỉ khởi tạo phần web cần thiết; dependency mock làm assertion về routing/rendering nhanh nhưng không chứng minh service/database. Áp dụng cho RMS: giữ MockMvc cho CSRF, role, binding và Thymeleaf; bổ sung full-context SQL Server test cho những điểm ghi nhiều bảng. Guide hiện có ví dụ RestTestClient và complete pom Boot 4.0.8, nên không chép nguyên cấu hình/API vào RMS 3.5.16. `contextLoads` chỉ là smoke context.

## Testcontainers Java

Nguồn: [repo chính thức](https://github.com/testcontainers/testcontainers-java), [MSSQLServer module](https://java.testcontainers.org/modules/databases/mssqlserver/).

Thư viện cung cấp container dùng cho test; module MSSQL yêu cầu chấp nhận EULA của image. Có thể dùng SQL Server tách biệt, tái lập fixture, kiểm native SQL/lock sát database thật. RMS chưa có dependency này. Với đồ án local, dùng database SQL Server dành riêng cho test trước là đủ; container chỉ là lựa chọn sau khi có Docker, tài nguyên máy và phiên bản thư viện tương thích. Không thêm dependency, image, license acceptance hoặc chạy Docker trong lượt này.

## ISTQB CTFL v4.0.1

Nguồn: [syllabus chính thức v4.0.1](https://istqb.org/wp-content/uploads/2024/11/ISTQB_CTFL_Syllabus_v4.0.1.pdf), chương 4, đặc biệt 4.2.1–4.2.4, trang in 39–42; phân tầng ở 2.2 và rủi ro ở 5.2.

EP chọn đại diện từ các phân vùng hợp lệ và không hợp lệ. BVA kiểm giá trị biên cùng lân cận. Decision Table mô tả kết quả theo tổ hợp điều kiện khả thi. State Transition kiểm trạng thái, sự kiện và guard, gồm thao tác bị cấm. Trong RMS, mỗi tình huống âm tách một điều kiện lỗi; không đưa nhiều lỗi vào cùng input rồi coi đã kiểm hết rule. Đặt trace từ test basis đến scenario, input và expected, ưu tiên rủi ro nghiệp vụ.

## Chuyển nghiên cứu thành thiết kế RMS

| Học từ nguồn | Áp dụng cụ thể |
|---|---|
| Slice và persistence riêng | AuthBotanicalWebTests không thay cho OTP → User/Candidate DB; OfferIntegrationTest đang là slice, không nhận nhầm là DB test. |
| DB đúng công nghệ | SQL Server cho native queries, Unicode, conditional update, posting/requisition lock. |
| EP/BVA | CV header/extension/kích thước, OTP/expiry/resend/attempts, note length, batch count. |
| Decision Table | HM Pass không bằng Candidate consent; quyền approve phụ thuộc role + owner + status. |
| State Transition | Proposal HM_Suggested → HM_Passed chỉ sau consent hoặc CV mới đúng đề xuất đã được duyệt; withdrawal/closure có audit phân loại Rejected. |

Các Expected Result chi tiết được suy ra từ **source RMS hiện tại và tài liệu thay đổi đã đối chiếu**, không phải yêu cầu của Petclinic hay ví dụ Spring Guide. SRS cũ chưa được dùng làm acceptance baseline do chủ dự án xác nhận chưa cập nhật.
