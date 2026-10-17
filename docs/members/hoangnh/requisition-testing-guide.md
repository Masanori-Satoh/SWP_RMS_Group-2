# Hướng dẫn viết test: module Yêu cầu tuyển dụng (Requisition)

> Ghi chú cá nhân của HoangNH. Đi từ test nhỏ nhất (một hàm) đến test lớn nhất (cả hệ thống qua HTTP).
> Mỗi phần có: test để làm gì, file mẫu đang có trong dự án, khung code để chép, và lỗi hay gặp.

---

## 0. Bốn tầng test và khi nào dùng tầng nào

| Tầng | Kiểm tra cái gì | Cần Spring? Cần DB? | Tốc độ | File trong dự án |
|---|---|---|---|---|
| **Unit: Validator** | Một quy tắc nhập liệu (số lượng 1–30, trọng số 100%...) | Không / Không | Rất nhanh | `requisition/validator/RequisitionValidatorTests.java` |
| **Unit: Service** | Nghiệp vụ một hàm service (tạo, xóa, duyệt...), phụ thuộc được giả lập bằng Mockito | Không / Không | Nhanh | `requisition/service/JobRequisitionServiceTest.java`, `RequisitionListFilterTests.java`, `RequisitionAccessTest.java` |
| **Controller (MVC)** | URL → controller → view / redirect / flash, bảo mật (403, CSRF); service được giả lập | Một phần (web) / Không | Trung bình | `requisition/controller/JobRequisitionControllerTests.java` |
| **Integration** | Service + Repository + **SQL Server thật**, rollback sau mỗi test | Đầy đủ / **Có** | Chậm | `requisition/integration/RequisitionIntegrationTest.java` |
| **System (Postman)** | Cả ứng dụng đang chạy, qua HTTP như trình duyệt (đăng nhập, CSRF, session) | App đang chạy / **Có** | Chậm nhất | `docs/members/hoangnh/requisition.postman_collection.json` |

**Quy tắc chọn tầng:** quy tắc nhập liệu → test validator; "ai được làm gì", "ghi bảng nào" → test service;
"bấm nút thì về trang nào, hiện thông báo gì" → test controller; "câu SQL / ràng buộc DB có đúng không" → integration;
"demo cả luồng cho GV" → Postman.

### Lệnh chạy

```bash
# Toàn bộ test của module requisition (khoảng 1 phút, gồm cả integration nên cần SQL Server đang chạy)
./mvnw -o clean test -Dtest='com.group2.rms.requisition.**'

# Một class
./mvnw -o test -Dtest=RequisitionValidatorTests

# Một hàm test
./mvnw -o test -Dtest='JobRequisitionServiceTest#svc10_delete_blockedWhenLaterRoundExists'
```

Kết quả nằm ở `target/surefire-reports/*.txt`. Luôn dùng `clean` khi thấy lỗi lạ kiểu "Unresolved compilation problems":
VS Code cũng biên dịch vào `target/` và có thể để lại class cũ.

### Đặt tên và cấu trúc một test (AAA)

```java
@Test
@DisplayName("SVC-10: Phiếu đã có đợt sau -> Không xóa, báo lỗi rõ ràng")   // câu tiếng Việt: điều kiện -> kết quả
void svc10_delete_blockedWhenLaterRoundExists() {                          // mã_hàm_điềuKiện
    // Arrange: chuẩn bị dữ liệu và giả lập
    // Act:     gọi đúng 1 hàm cần test
    // Assert:  kiểm tra kết quả + kiểm tra những gì KHÔNG được xảy ra
}
```

---

## 1. Unit test Validator

**Mục đích:** mỗi quy tắc trong `validation.md` có ít nhất 1 test **hợp lệ** và 1 test **sai**, nhất là ở **biên** (30 được, 31 không).

**Hàm hỗ trợ đã có** trong `RequisitionValidatorTests`:

| Hàm | Trả về |
|---|---|
| `valid()` | Một yêu cầu **gửi duyệt** hợp lệ đầy đủ |
| `minimalDraft()` | Một **bản nháp** chỉ có 4 trường bắt buộc tối thiểu |
| `criterion(name, weight)` | Một dòng tiêu chí đầy đủ |
| `expectPass(d)` | Gọi validate, fail test nếu có lỗi |
| `expectFail(d)` | Gọi validate, trả về exception để đọc `getErrors()` |

**Khung code (chép rồi đổi):**

```java
@Test void val003_positionsAtMaximumIsAccepted() {
    var d = minimalDraft(); d.setNumberOfPositions(RequisitionValidator.POSITIONS_MAX);
    assertDoesNotThrow(() -> validator.validate(d));
}

@Test void val003_positionsAboveMaximumIsRejected() {
    var d = minimalDraft(); d.setNumberOfPositions(RequisitionValidator.POSITIONS_MAX + 1);
    assertEquals("Số lượng cần tuyển phải từ 1 đến 30 người.",
            expectFail(d).getErrors().get("numberOfPositions"));
}
```

**Mẹo:**
- Dùng hằng số (`POSITIONS_MAX + 1`) thay vì gõ `31`: đổi giới hạn thì test vẫn đúng ý.
- Kiểm tra **đúng key lỗi** (`"numberOfPositions"`, `"screeningCriteria[0].weight"`): key này quyết định lỗi hiện ở ô nào trên form.
- Nhiều giá trị cùng kết quả → `@ParameterizedTest`:

```java
@ParameterizedTest
@ValueSource(ints = {0, -1, 31, 1000})
void val003_positionsOutOfRangeRejected(int positions) {
    var d = minimalDraft(); d.setNumberOfPositions(positions);
    assertTrue(expectFail(d).getErrors().containsKey("numberOfPositions"));
}
```

**Checklist quy tắc nên có test:** xem `validation.md` mục 7 (mỗi dòng checklist ≈ 1–2 test).

---

## 2. Unit test Service (Mockito)

**Mục đích:** kiểm tra nghiệp vụ của `RequisitionServiceImpl` **không cần DB**. Repository, quyền, thông báo đều là "đồ giả" (mock) để:
- chỉ định chúng trả gì (`when(...).thenReturn(...)`);
- kiểm tra service có gọi chúng đúng không (`verify(...)`).

**Khung lớp test** (đã có trong `JobRequisitionServiceTest`):

```java
@ExtendWith(MockitoExtension.class)
class JobRequisitionServiceTest {
    @Mock private JobRequisitionRepository requisitions;   // mỗi phụ thuộc của service là 1 @Mock
    @Mock private JobPostingRepository postings;
    @Mock private AuditLogRepository audit;
    @Mock private RequisitionAccess access;
    // ...
    @InjectMocks private RequisitionServiceImpl service;   // service thật, nhận các mock ở trên

    @BeforeEach
    void setUp() {
        // lenient(): stub dùng chung, test nào không dùng cũng không bị Mockito báo "unnecessary stubbing"
        lenient().when(access.actor()).thenReturn(manager);
    }
}
```

> Thiếu `@Mock` cho một phụ thuộc → service nhận `null` → `NullPointerException` khi chạy. Ví dụ test xóa cần `@Mock JobPostingRepository postings`.

**Ví dụ đầy đủ: chặn xóa phiếu gốc đã có đợt sau**

```java
@Test
@DisplayName("SVC-10: Phiếu đã có đợt tuyển sau sao chép từ nó -> Không xóa, báo lỗi rõ ràng")
void svc10_delete_blockedWhenLaterRoundExists() {
    // Arrange
    rejectedRequisition();                                                     // findForUpdate(60) trả phiếu Rejected
    when(postings.existsByRequisition_RequisitionId(60)).thenReturn(false);    // chưa có tin
    when(requisitions.existsByOriginalRequisition_RequisitionId(60)).thenReturn(true); // đã có đợt 2

    // Act
    var ex = assertThrows(RequisitionValidationException.class, () -> service.deleteRequisition(60));

    // Assert: đúng câu lỗi, và KHÔNG xóa, KHÔNG ghi audit
    assertEquals("Yêu cầu này đã có đợt tuyển sau được sao chép từ nó và không thể xóa.", ex.getErrors().get("action"));
    verify(requisitions, never()).delete(any(JobRequisition.class));
    verifyNoInteractions(audit);
}
```

**Lấy object service đã lưu để kiểm tra: `ArgumentCaptor`**

```java
var captor = ArgumentCaptor.forClass(JobRequisition.class);
verify(requisitions).saveAndFlush(captor.capture());
JobRequisition saved = captor.getValue();
assertEquals("Draft", saved.getApprovalStatus());
assertSame(itDepartment, saved.getDepartment());
```

**Danh sách hàm service nên có test** (đánh dấu ✅ là đã có):

| Hàm | Trường hợp |
|---|---|
| `createRequisition` | ✅ nháp đủ trường; ✅ nháp tối thiểu sinh mã `REQ-0101`; ✅ lọc dòng tiêu chí trống; ✅ chặn `criteriaId` cũ; ✅ không có phòng ban; ✅ phòng ban ngừng hoạt động; ✅ bỏ qua `departmentId` từ form; ☐ trùng tên bản nháp cùng phòng ban; ☐ gửi duyệt có `SubmittedAt` |
| `updateRequisition` | ✅ giữ phòng ban cũ; ☐ không ai khác ngoài chủ / Admin sửa được; ☐ chỉ ghi audit các trường thay đổi |
| `deleteRequisition` | ✅ chặn khi có đợt sau; ✅ xóa thành công; ☐ chặn khi đã có tin tuyển dụng |
| `decide` | ✅ trả lại → Rejected + thông báo cho HM; ☐ trả lại thiếu lý do; ☐ lý do > 1000 ký tự; ☐ duyệt → Approved, không thông báo |
| `withdraw` | ☐ chỉ chủ phiếu, chỉ khi Pending_Director |
| `getListPage` | ✅ 9 test bộ lọc theo role trong `RequisitionListFilterTests`; ✅ HR chỉ Approved |

---

## 3. Controller test (`@WebMvcTest` + MockMvc)

**Mục đích:** giả lập request HTTP vào controller **không khởi động cả app**, service là mock. Kiểm tra:
status, tên view, redirect, flash message, model attribute, và bảo mật (role, CSRF).

```java
@WebMvcTest(controllers = RequisitionController.class)
@Import({SecurityConfig.class, DatabaseUserDetailsService.class})   // dùng cấu hình bảo mật THẬT
class JobRequisitionControllerTests {
    @Autowired private MockMvc mvc;
    @MockitoBean private RequisitionService service;
    @MockitoBean private RequisitionAccess access;
    @MockitoBean private DepartmentRepository departments;
    @MockitoBean private UserRepository userRepository;   // AccountSessionGuardFilter cần đọc user
}
```

**Ví dụ: lưu nháp thành công**

```java
@Test
@WithMockUser(username = "hm", roles = "HIRING_MANAGER")       // giả lập đã đăng nhập với role HM
void create_saveDraft_success() throws Exception {
    when(service.createRequisition(any(RequisitionRequest.class))).thenReturn(10);

    mvc.perform(post("/requisitions/create")
                    .with(csrf())                               // thiếu dòng này → 403
                    .param("action", "draft")
                    .param("title", "New Dev"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/requisitions/10"))
            .andExpect(flash().attribute("successMessage", "Đã lưu bản nháp thành công."));
}
```

**Các kiểu test nên có:**

| Muốn kiểm tra | Cách viết |
|---|---|
| Role không được vào | `@WithMockUser(roles = "CANDIDATE")` → `status().isForbidden()` |
| Thiếu CSRF | bỏ `.with(csrf())` → `status().isForbidden()` |
| Lỗi validate trả lại form | `doThrow(new RequisitionValidationException(Map.of("title", "..."))).when(service).createRequisition(any())` → `status().isOk()`, `view().name("requisitions/form")`, `model().attributeHasFieldErrors("requisitionDto", "title")` |
| Form không bind được trường cấm | gửi `.param("approvalStatus", "Approved")`, dùng `ArgumentCaptor` kiểm tra DTO nhận được không có giá trị đó |

**Lỗi hay gặp:** `roles = "HIRING_MANAGER"` phải khớp tên role trong `RoleAuthorities` / `SecurityConfig`;
`userRepository.findByUsernameIgnoreCase("hm")` phải được stub, nếu không session guard coi tài khoản không tồn tại và chuyển về `/login`.

---

## 4. Integration test (`@SpringBootTest` + SQL Server thật)

**Mục đích:** kiểm tra những thứ mock không bắt được: câu `@Query`, Specification, mapping entity ↔ cột,
ràng buộc DB (NOT NULL, CHECK, UNIQUE), cascade xóa.

```java
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureMockMvc
@Transactional          // mỗi test chạy trong 1 transaction và ROLLBACK khi xong → DB không bị thay đổi
class RequisitionIntegrationTest { ... }
```

> ⚠️ Test chạy trên **database thật** trong `application.properties` (`RitirementManagement2`). Nhờ `@Transactional` dữ liệu được rollback,
> nhưng **identity (RequisitionId) vẫn tăng**. Không dùng `@Commit` và không gọi code tự mở transaction mới (`REQUIRES_NEW`) trong test.

**Ví dụ: kiểm tra câu query mới `existsByOriginalRequisition_RequisitionId`**

```java
@Test
@WithMockUser(username = "it_hm", roles = "HIRING_MANAGER")
void it11_existsByOriginal_detectsLaterRound() {
    JobRequisition round1 = requisitions.saveAndFlush(/* phiếu đợt 1, Rejected, phòng ban + HM thật */);
    JobRequisition round2 = /* phiếu đợt 2 trỏ originalRequisition = round1, recruitmentRound = 2 */;
    requisitions.saveAndFlush(round2);

    assertTrue(requisitions.existsByOriginalRequisition_RequisitionId(round1.getRequisitionId()));
    assertFalse(requisitions.existsByOriginalRequisition_RequisitionId(round2.getRequisitionId()));
}
```

**Mẹo:**
- Sau khi lưu, gọi `entityManager.flush(); entityManager.clear();` rồi đọc lại để chắc chắn dữ liệu đi xuống DB, không phải lấy từ cache của Hibernate.
- Dữ liệu dùng trong test (user `it_hm`, `it_dir`) được tạo trong `setUp()` nếu chưa có, không phụ thuộc dữ liệu demo.

---

## 5. System test qua Postman

### 5.1 Ứng dụng này không phải REST API: Postman phải làm như trình duyệt

Các URL `/requisitions/**` là **form HTML** (Thymeleaf), dùng **đăng nhập bằng session** và **CSRF token**:

```
1. GET  /login                 → trang có <input name="_csrf" value="TOKEN_A">, cookie JSESSIONID
2. POST /login                 → username, password, _csrf=TOKEN_A  → 302 về /dashboard (sai mật khẩu: /login?error)
3. GET  /requisitions          → trang có <meta name="_csrf" content="TOKEN_B">  (token đổi sau khi đăng nhập)
4. POST /requisitions/create   → các trường form + _csrf=TOKEN_B   → 302 về /requisitions/{id}
```

- **Cookie `JSESSIONID`:** Postman tự lưu và gửi lại (cookie jar), không cần làm gì.
- **CSRF:** collection có script chạy sau **mọi** request, tìm token trong HTML và lưu vào biến `{{csrf}}`. Mọi POST gửi `_csrf={{csrf}}`.
- **Không tự đi theo redirect:** mỗi request đặt `followRedirects: false` để kiểm tra được mã `302` và header `Location`.
- **Thông báo flash** (thành công / lỗi) chỉ hiện ở **lần GET ngay sau redirect**, nên sau mỗi POST có một request GET trang chi tiết để kiểm tra câu thông báo.
- Đổi người dùng: chỉ cần `GET /login` rồi `POST /login` với tài khoản khác (Spring tạo session mới).

### 5.2 Chạy collection

1. Khởi động app: `./mvnw -o clean spring-boot:run` (cổng 8088, dữ liệu seed có `hm_tuan`, `director`, `hr_lan`, mật khẩu `12345678`).
2. Postman → **Import** → chọn `docs/members/hoangnh/requisition.postman_collection.json`.
3. Chuột phải collection → **Run collection** → giữ đúng thứ tự → **Run**.
4. Hoặc chạy bằng dòng lệnh (không cần mở Postman):

```bash
npx newman run docs/members/hoangnh/requisition.postman_collection.json

# App chạy ở cổng khác
npx newman run docs/members/hoangnh/requisition.postman_collection.json --env-var baseUrl=http://localhost:8089
```

Kết quả lần chạy 17/10/2026: **30 request, 55 assertion, 0 failed** (dữ liệu tự dọn).

Biến của collection (tab **Variables**): `baseUrl` (mặc định `http://localhost:8088`), `password`; `csrf`, `reqId`, `startDate` do script tự điền.

### 5.3 Collection kiểm tra những gì (30 request, tự dọn dữ liệu)

| Thư mục | Người dùng | Các bước | Kỳ vọng |
|---|---|---|---|
| 0. Chưa đăng nhập | (không) | Mở danh sách | 302 → `/login` |
| 1. Hiring Manager | `hm_tuan` | Danh sách; tìm kiếm + lọc + sắp xếp; **[Âm]** tạo thiếu CSRF; **[Âm]** nháp thiếu tên; **[Âm]** trọng số 50%; **[Âm]** số lượng 31; **gửi duyệt hợp lệ** (lưu `reqId`); xem chi tiết; **[Âm]** tự duyệt; **[Âm]** sửa khi đang chờ duyệt | 403 / 200 kèm câu lỗi / 302 → chi tiết; chi tiết có mã `REQ-`, lương "20 – 30 triệu VND/tháng" |
| 2. Director | `director` | Danh sách mặc định có phiếu vừa gửi; **[Âm]** trả lại không lý do; **trả lại có lý do**; **[Âm]** xóa | Câu lỗi "Vui lòng nhập lý do..."; "Đã lưu quyết định thành công."; xóa bị 403 |
| 3. HR | `hr_lan` | **[Âm]** xem phiếu bị trả lại; **[Âm]** tạo yêu cầu | 403, 403 |
| 4. Dọn dữ liệu | `hm_tuan` | Xóa phiếu vừa tạo; xem lại | 302 → `/requisitions`; 404 |

Chạy xong DB trở về như cũ (chỉ còn dòng `AuditLog` của phiếu đã xóa và identity tăng thêm 1).
Nếu chạy dở giữa chừng (ví dụ dừng ở thư mục 2), vào app bằng `hm_tuan` xóa tay phiếu "Postman - Kiểm thử viên".

### 5.4 Tự thêm một test case Postman

1. Nhân bản (Duplicate) một request cùng loại.
2. Sửa tab **Body** (`x-www-form-urlencoded`), nhớ giữ dòng `_csrf = {{csrf}}`.
3. Tab **Settings**: tắt *Automatically follow redirects*.
4. Tab **Scripts → Post-response**:

```javascript
pm.test("HTTP 200", () => pm.response.to.have.status(200));
pm.test("Báo lỗi số lượng", () =>
    pm.expect(pm.response.text()).to.include("Số lượng cần tuyển phải từ 1 đến 30 người."));
```

### 5.5 Lỗi hay gặp

| Hiện tượng | Nguyên nhân |
|---|---|
| POST nào cũng 403 | Thiếu `_csrf` hoặc `{{csrf}}` rỗng: chưa có request GET nào sau khi đăng nhập |
| Bị 302 về `/login` giữa chừng | Mất session: đã xóa cookie, app vừa khởi động lại, hoặc đăng nhập sai |
| Đăng nhập 302 về `/login?error` | Sai mật khẩu, hoặc DB chưa có dữ liệu seed |
| Test "gửi duyệt hợp lệ" trả 200 thay vì 302 | Có lỗi validate; mở tab **Body** của response, tìm khung lỗi màu đỏ đầu form |
| Kết quả khác với code vừa sửa | App đang chạy là bản cũ: dừng app và chạy lại `clean spring-boot:run` |

---

## 6. Trước khi báo "xong"

- [ ] `./mvnw -o clean test -Dtest='com.group2.rms.requisition.**'` → 0 failures.
- [ ] Chạy collection Postman → mọi test xanh, dữ liệu đã được dọn.
- [ ] Quy tắc mới trong code đã có trong `validation.md` và có ít nhất 1 test biên.
- [ ] Ghi ngắn gọn vào `checklist.md`.
