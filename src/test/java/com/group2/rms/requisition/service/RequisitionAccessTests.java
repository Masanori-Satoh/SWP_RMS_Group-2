package com.group2.rms.requisition.service;

import com.group2.rms.requisition.entity.JobRequisition;
import com.group2.rms.user.entity.Department;
import com.group2.rms.user.entity.Role;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.*;

/*
 * UNIT TEST: RequisitionAccess (luật "ai được làm gì" với một yêu cầu tuyển dụng).
 * Các hàm can...() là hàm thuần: chỉ đọc role, người tạo, trạng thái → không cần DB.
 * Cờ quyền trên màn danh sách / chi tiết (editable, deletable, copyable...) đều lấy từ đây.
 *
 * Quy ước (docs/members/hoangnh/CLAUDE.md §23): hàm_shouldKếtQuả_whenĐiềuKiện; bảng nhiều trường hợp dùng @ParameterizedTest.
 */
@ExtendWith(MockitoExtension.class)
class RequisitionAccessTests {

    @Mock
    private UserRepository users;

    @InjectMocks
    private RequisitionAccess access;

    private User hr;
    private User manager;
    private User otherManager;
    private User director;
    private User admin;
    private Department itDepartment;

    @BeforeEach
    void setUp() {
        itDepartment = Department.builder().departmentId(1).departmentName("IT").build();
        hr = userWithRole(1, RequisitionAccess.ROLE_HR);
        manager = userWithRole(2, RequisitionAccess.ROLE_HIRING_MANAGER);
        manager.setDepartment(itDepartment);
        otherManager = userWithRole(99, RequisitionAccess.ROLE_HIRING_MANAGER);
        director = userWithRole(3, RequisitionAccess.ROLE_DIRECTOR);
        admin = userWithRole(4, RequisitionAccess.ROLE_SYSTEM_ADMIN);
    }

    private User userWithRole(int id, String roleName) {
        return User.builder().userId(id).username("user" + id).fullName("User " + id)
                .role(Role.builder().roleName(roleName).build()).build();
    }

    /** Yêu cầu do {@code manager} tạo, ở trạng thái cho trước. */
    private JobRequisition ownedByManager(String status) {
        return JobRequisition.builder().requisitionId(1).hiringManager(manager).approvalStatus(status).build();
    }

    private User userByName(String name) {
        return switch (name) {
            case "hr" -> hr;
            case "manager" -> manager;
            case "otherManager" -> otherManager;
            case "director" -> director;
            default -> admin;
        };
    }

    // =========================================================================
    // requireView: bảng 5 người xem × 4 trạng thái
    // =========================================================================

    @Nested
    @DisplayName("requireView")
    class RequireView {

        @ParameterizedTest(name = "{0} xem yêu cầu {1} -> được xem = {2}")
        @CsvSource({
                // HR: chỉ yêu cầu đã duyệt
                "hr, Draft, false",
                "hr, Pending_Director, false",
                "hr, Approved, true",
                "hr, Rejected, false",
                // Người tạo: mọi trạng thái
                "manager, Draft, true",
                "manager, Pending_Director, true",
                "manager, Approved, true",
                "manager, Rejected, true",
                // HM khác phòng ban, không phải người tạo: không xem được
                "otherManager, Draft, false",
                "otherManager, Approved, false",
                // Director: mọi trạng thái trừ bản nháp
                "director, Draft, false",
                "director, Pending_Director, true",
                "director, Approved, true",
                "director, Rejected, true",
                // Admin: không tham gia nghiệp vụ
                "admin, Draft, false",
                "admin, Approved, false"
        })
        @DisplayName("Mỗi role chỉ xem được đúng các trạng thái được phép")
        void requireView_shouldFollowRoleAndStatusTable_whenViewerOpensDetail(String viewer, String status, boolean allowed) {
            // Arrange
            User user = userByName(viewer);
            JobRequisition requisition = ownedByManager(status);

            // Act + Assert
            if (allowed) {
                assertDoesNotThrow(() -> access.requireView(user, requisition));
            } else {
                assertThrows(AccessDeniedException.class, () -> access.requireView(user, requisition));
            }
        }

        @Test
        @DisplayName("HR dù là người tạo (dữ liệu sai) nhưng yêu cầu chưa duyệt -> Vẫn bị chặn")
        void requireView_shouldDenyHr_whenHrOwnsNonApprovedRequisition() {
            // Arrange
            JobRequisition requisition = JobRequisition.builder().requisitionId(1).hiringManager(hr)
                    .approvalStatus(RequisitionAccess.STATUS_DRAFT).build();

            // Act + Assert
            assertThrows(AccessDeniedException.class, () -> access.requireView(hr, requisition));
        }

        @Test
        @DisplayName("HM cùng phòng ban (không phải người tạo) -> Xem được yêu cầu của phòng mình")
        void requireView_shouldAllowManager_whenRequisitionBelongsToSameDepartment() {
            // Arrange
            User colleague = userWithRole(50, RequisitionAccess.ROLE_HIRING_MANAGER);
            JobRequisition requisition = JobRequisition.builder().requisitionId(1).hiringManager(colleague)
                    .department(itDepartment).approvalStatus(RequisitionAccess.STATUS_DRAFT).build();

            // Act + Assert
            assertDoesNotThrow(() -> access.requireView(manager, requisition));
        }

        @Test
        @DisplayName("Yêu cầu null (không tồn tại) -> AccessDenied")
        void requireView_shouldThrowAccessDenied_whenRequisitionIsNull() {
            assertThrows(AccessDeniedException.class, () -> access.requireView(admin, null));
        }
    }

    // =========================================================================
    // Cờ quyền dùng cho nút / menu ⋯
    // =========================================================================

    @Nested
    @DisplayName("Cờ quyền")
    class PermissionFlags {

        @ParameterizedTest(name = "trạng thái {0} -> sửa được = {1}")
        @CsvSource({"Draft, true", "Rejected, true", "Pending_Director, false", "Approved, false"})
        @DisplayName("Người tạo chỉ sửa được khi yêu cầu là Draft hoặc Rejected")
        void canEdit_shouldDependOnStatus_whenViewerIsOwner(String status, boolean expected) {
            assertEquals(expected, access.canEdit(manager, ownedByManager(status)));
        }

        @Test
        @DisplayName("HM khác sửa bản nháp không phải của mình -> Không được")
        void canEdit_shouldReturnFalse_whenViewerIsNotOwner() {
            assertFalse(access.canEdit(otherManager, ownedByManager(RequisitionAccess.STATUS_DRAFT)));
        }

        @ParameterizedTest(name = "có tin = {0}, có đợt sau = {1} -> xóa được = {2}")
        @CsvSource({"false, false, true", "true, false, false", "false, true, false", "true, true, false"})
        @DisplayName("Xóa được khi sửa được, chưa có tin tuyển dụng và chưa có đợt sau")
        void canDelete_shouldRequireNoPostingAndNoLaterRound_whenOwnerDeletesDraft(boolean hasPosting,
                boolean hasLaterRound, boolean expected) {
            assertEquals(expected, access.canDelete(manager, ownedByManager(RequisitionAccess.STATUS_DRAFT),
                    hasPosting, hasLaterRound));
        }

        @Test
        @DisplayName("Yêu cầu đã duyệt (không sửa được) -> Không xóa được dù chưa có tin")
        void canDelete_shouldReturnFalse_whenRequisitionIsApproved() {
            assertFalse(access.canDelete(manager, ownedByManager(RequisitionAccess.STATUS_APPROVED), false, false));
        }

        @ParameterizedTest(name = "trạng thái {0} -> rút lại được = {1}")
        @CsvSource({"Pending_Director, true", "Draft, false", "Approved, false", "Rejected, false"})
        @DisplayName("Người tạo chỉ rút lại được yêu cầu đang chờ duyệt")
        void canWithdraw_shouldOnlyAllowPending_whenViewerIsOwner(String status, boolean expected) {
            assertEquals(expected, access.canWithdraw(manager, ownedByManager(status)));
        }

        @Test
        @DisplayName("Người khác (kể cả Giám đốc) rút lại yêu cầu đang chờ -> Không được")
        void canWithdraw_shouldReturnFalse_whenViewerIsNotOwner() {
            assertFalse(access.canWithdraw(director, ownedByManager(RequisitionAccess.STATUS_PENDING_DIRECTOR)));
        }

        @Test
        @DisplayName("Giám đốc duyệt yêu cầu đang chờ của người khác -> Được; tự duyệt yêu cầu của mình -> Không")
        void canDecide_shouldAllowDirector_whenRequisitionIsPendingAndNotOwn() {
            // Arrange
            JobRequisition own = JobRequisition.builder().requisitionId(2).hiringManager(director)
                    .approvalStatus(RequisitionAccess.STATUS_PENDING_DIRECTOR).build();

            // Act + Assert
            assertTrue(access.canDecide(director, ownedByManager(RequisitionAccess.STATUS_PENDING_DIRECTOR)));
            assertFalse(access.canDecide(director, own));
            assertFalse(access.canDecide(director, ownedByManager(RequisitionAccess.STATUS_DRAFT)));
            assertFalse(access.canDecide(admin, ownedByManager(RequisitionAccess.STATUS_PENDING_DIRECTOR)));
        }

        @ParameterizedTest(name = "trạng thái {0} -> sao chép được = {1}")
        @CsvSource({"Approved, true", "Rejected, true", "Draft, false", "Pending_Director, false"})
        @DisplayName("Sao chép được khi có quyền tạo và yêu cầu đã xong vòng duyệt")
        void canCopy_shouldRequireFinishedStatus_whenViewerCanCreate(String status, boolean expected) {
            assertEquals(expected, access.canCopy(manager, ownedByManager(status)));
        }

        @Test
        @DisplayName("HR (không có quyền tạo yêu cầu) -> Không sao chép được dù yêu cầu đã duyệt")
        void canCopy_shouldReturnFalse_whenViewerCannotCreate() {
            assertFalse(access.canCopy(hr, ownedByManager(RequisitionAccess.STATUS_APPROVED)));
        }

        @Test
        @DisplayName("Tạo tin: HR + yêu cầu Đã duyệt + chưa có tin -> Được; thiếu một điều kiện -> Không")
        void canCreatePostingFrom_shouldRequireApprovedWithoutPosting_whenViewerIsHr() {
            assertTrue(access.canCreatePostingFrom(hr, ownedByManager(RequisitionAccess.STATUS_APPROVED), false));
            assertFalse(access.canCreatePostingFrom(hr, ownedByManager(RequisitionAccess.STATUS_APPROVED), true));
            assertFalse(access.canCreatePostingFrom(hr, ownedByManager(RequisitionAccess.STATUS_REJECTED), false));
            assertFalse(access.canCreatePostingFrom(manager, ownedByManager(RequisitionAccess.STATUS_APPROVED), false));
        }

        @ParameterizedTest(name = "{0} mở tin {1} -> có link = {2}")
        @CsvSource({
                "hr, Draft, true",
                "admin, Draft, false",
                "manager, Draft, false",
                "director, Draft, false",
                "manager, Published, true",
                "director, Closed, true"
        })
        @DisplayName("Link tin tuyển dụng: tin nháp chỉ HR thấy; Admin không thấy tin")
        void canOpenPosting_shouldHideDraftPosting_whenViewerIsNotHrOrAdmin(String viewer, String postingStatus,
                boolean expected) {
            assertEquals(expected, access.canOpenPosting(userByName(viewer), postingStatus));
        }

        @ParameterizedTest
        @NullSource
        @DisplayName("Yêu cầu chưa có tin (trạng thái null) -> Không có link")
        void canOpenPosting_shouldReturnFalse_whenRequisitionHasNoPosting(String postingStatus) {
            assertFalse(access.canOpenPosting(hr, postingStatus));
        }

        @ParameterizedTest
        @ValueSource(strings = {"Approved", "Rejected"})
        @DisplayName("Approved / Rejected là trạng thái đã xong vòng duyệt")
        void isFinishedStatus_shouldReturnTrue_whenStatusIsApprovedOrRejected(String status) {
            assertTrue(RequisitionAccess.isFinishedStatus(status));
        }

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = {"Draft", "Pending_Director", "", "approved"})
        @DisplayName("Draft / Pending / null / rỗng / sai hoa thường -> Chưa xong")
        void isFinishedStatus_shouldReturnFalse_whenStatusIsNotFinished(String status) {
            assertFalse(RequisitionAccess.isFinishedStatus(status));
        }
    }
}
