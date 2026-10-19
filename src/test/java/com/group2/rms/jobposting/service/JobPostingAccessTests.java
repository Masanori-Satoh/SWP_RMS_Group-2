package com.group2.rms.jobposting.service;

import com.group2.rms.jobposting.entity.JobPosting;
import com.group2.rms.requisition.entity.JobRequisition;
import com.group2.rms.requisition.service.RequisitionAccess;
import com.group2.rms.user.entity.Department;
import com.group2.rms.user.entity.Role;
import com.group2.rms.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/*
 * UNIT TEST: JobPostingAccess (luật "ai được làm gì" với tin tuyển dụng).
 * Các hàm can...() là hàm thuần: chỉ đọc role, phòng ban, trạng thái → không cần DB, không cần mock.
 * RequisitionAccess thật với UserRepository = null (chỉ dùng role() và inManagedDepartment(), không gọi actor()).
 *
 * Quy ước (docs/members/hoangnh/CLAUDE.md §23): hàm_shouldKếtQuả_whenĐiềuKiện; bảng role × trạng thái dùng @CsvSource.
 */
class JobPostingAccessTests {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 18, 10, 0);

    private final JobPostingAccess access = new JobPostingAccess(new RequisitionAccess(null));

    private Department itDepartment;
    private User hr;
    private User admin;
    private User manager;
    private User otherManager;
    private User director;
    private User interviewer;

    @BeforeEach
    void setUp() {
        itDepartment = Department.builder().departmentId(1).departmentName("IT").build();
        hr = userWithRole(1, RequisitionAccess.ROLE_HR);
        admin = userWithRole(2, RequisitionAccess.ROLE_SYSTEM_ADMIN);
        manager = userWithRole(3, RequisitionAccess.ROLE_HIRING_MANAGER);
        manager.setDepartment(itDepartment);
        otherManager = userWithRole(4, RequisitionAccess.ROLE_HIRING_MANAGER);
        otherManager.setDepartment(Department.builder().departmentId(2).departmentName("Sales").build());
        director = userWithRole(5, RequisitionAccess.ROLE_DIRECTOR);
        interviewer = userWithRole(6, "Interviewer");
    }

    // ----- Hàm hỗ trợ -----

    private static User userWithRole(int id, String roleName) {
        return User.builder().userId(id).username("user" + id).fullName("User " + id)
                .role(Role.builder().roleName(roleName).build()).build();
    }

    private User userByName(String name) {
        return switch (name) {
            case "hr" -> hr;
            case "admin" -> admin;
            case "manager" -> manager;
            case "otherManager" -> otherManager;
            case "director" -> director;
            default -> interviewer;
        };
    }

    /** Tin thuộc phòng IT ở trạng thái DB cho trước. */
    private JobPosting postingInIt(String postingStatus) {
        JobRequisition requisition = JobRequisition.builder().requisitionId(10).department(itDepartment)
                .approvalStatus(RequisitionAccess.STATUS_APPROVED).build();
        return JobPosting.builder().jobPostingId(100).requisition(requisition).postingStatus(postingStatus).build();
    }

    // =========================================================================
    // viewStatus: "Hẹn đăng" không lưu DB
    // =========================================================================
    @Nested
    @DisplayName("viewStatus")
    class ViewStatus {

        @Test
        @DisplayName("Published + ngày đăng sau hiện tại 1 phút -> Scheduled (hẹn đăng)")
        void viewStatus_shouldBeScheduled_whenPublishedInFuture() {
            assertEquals(JobPostingAccess.STATUS_SCHEDULED,
                    JobPostingAccess.viewStatus(JobPostingAccess.STATUS_PUBLISHED, NOW.plusMinutes(1), NOW));
        }

        @Test
        @DisplayName("Published + ngày đăng đúng bằng hiện tại (biên) -> Published")
        void viewStatus_shouldBePublished_whenPostingDateIsNow() {
            assertEquals(JobPostingAccess.STATUS_PUBLISHED,
                    JobPostingAccess.viewStatus(JobPostingAccess.STATUS_PUBLISHED, NOW, NOW));
        }

        @Test
        @DisplayName("Draft có ngày đăng ở tương lai (dữ liệu lạ) -> vẫn là Draft")
        void viewStatus_shouldKeepDbStatus_whenNotPublished() {
            assertEquals(JobPostingAccess.STATUS_DRAFT,
                    JobPostingAccess.viewStatus(JobPostingAccess.STATUS_DRAFT, NOW.plusDays(1), NOW));
        }

        @ParameterizedTest(name = "{0} -> sửa được: {1}")
        @CsvSource({"Draft, true", "Published, true", "Paused, true", "Closed, false"})
        @DisplayName("Chỉ tin đã đóng là không sửa được")
        void isEditableStatus_shouldRejectOnlyClosed(String status, boolean expected) {
            assertEquals(expected, JobPostingAccess.isEditableStatus(status));
        }
    }

    // =========================================================================
    // Quyền theo vai trò
    // =========================================================================
    @Nested
    @DisplayName("Vai trò")
    class Roles {

        @ParameterizedTest(name = "{0}: quản lý={1}, xem={2}, đóng tin={3}, talent pool={4}")
        @CsvSource({
                "hr,          true,  true,  true,  true",
                "admin,       true,  true,  false, false",
                "manager,     false, true,  false, true",
                "director,    false, true,  false, false",
                "interviewer, false, false, false, false"})
        @DisplayName("Bảng quyền theo vai trò (D4: Director chỉ xem; D7: chỉ HR đóng tin)")
        void canX_shouldFollowRoleTable(String who, boolean manage, boolean read, boolean close, boolean talentPool) {
            User user = userByName(who);

            assertEquals(manage, access.canManage(user));
            assertEquals(read, access.canRead(user));
            assertEquals(close, access.canCloseAny(user));
            assertEquals(talentPool, access.canSeeTalentPool(user));
        }

        @Test
        @DisplayName("Không phải HR / Admin gọi requireManage -> AccessDeniedException")
        void requireManage_shouldThrow_whenViewerIsDirector() {
            assertThrows(AccessDeniedException.class, () -> access.requireManage(director));
            assertDoesNotThrow(() -> access.requireManage(admin));
        }

        @Test
        @DisplayName("System Admin đóng tin -> AccessDeniedException (chỉ HR)")
        void requireCloseAny_shouldThrow_whenViewerIsAdmin() {
            assertThrows(AccessDeniedException.class, () -> access.requireCloseAny(admin));
            assertDoesNotThrow(() -> access.requireCloseAny(hr));
        }
    }

    // =========================================================================
    // canView / requireView: bảng người xem × trạng thái tin
    // =========================================================================
    @Nested
    @DisplayName("canView")
    class View {

        @ParameterizedTest(name = "{0} xem tin {1} của phòng IT -> {2}")
        @CsvSource({
                "hr,           Draft,     true",
                "admin,        Draft,     true",
                "manager,      Draft,     false",
                "director,     Draft,     false",
                "manager,      Published, true",
                "director,     Paused,    true",
                "otherManager, Published, false",
                "interviewer,  Published, false"})
        @DisplayName("HM chỉ xem tin phòng mình; HM / Director không xem tin nháp (D5)")
        void canView_shouldFollowRoleStatusAndDepartmentTable(String who, String status, boolean expected) {
            assertEquals(expected, access.canView(userByName(who), postingInIt(status)));
        }

        @Test
        @DisplayName("Director mở tin nháp (gõ thẳng URL) -> AccessDeniedException")
        void requireView_shouldThrow_whenDirectorOpensDraft() {
            assertThrows(AccessDeniedException.class,
                    () -> access.requireView(director, postingInIt(JobPostingAccess.STATUS_DRAFT)));
        }
    }

    // =========================================================================
    // Cờ thao tác theo trạng thái hiển thị
    // =========================================================================
    @Nested
    @DisplayName("Cờ thao tác")
    class Actions {

        @ParameterizedTest(name = "HR, {0}: đăng={1} đăng ngay/hủy lịch={2} mở lại={3} sửa={4} tạm dừng={5} đóng={6}")
        @CsvSource({
                "Draft,     true,  false, false, true,  false, false",
                "Scheduled, false, true,  false, true,  false, false",
                "Published, false, false, false, true,  true,  true",
                "Paused,    false, false, true,  true,  false, true",
                "Closed,    false, false, false, false, false, false"})
        @DisplayName("HR: nút theo từng trạng thái (Closed không làm gì được nữa)")
        void canX_shouldFollowStatusTable_whenViewerIsHr(String viewStatus, boolean publish, boolean schedule,
                boolean reopen, boolean edit, boolean pause, boolean close) {
            assertEquals(publish, access.canPublish(hr, viewStatus));
            assertEquals(schedule, access.canChangeSchedule(hr, viewStatus));
            assertEquals(reopen, access.canReopen(hr, viewStatus));
            assertEquals(edit, access.canEdit(hr, viewStatus));
            assertEquals(pause, access.canPause(hr, viewStatus));
            assertEquals(close, access.canClose(hr, viewStatus));
        }

        @Test
        @DisplayName("Director: mọi cờ thao tác đều tắt, kể cả tin đang đăng")
        void canX_shouldBeFalse_whenViewerIsDirector() {
            String published = JobPostingAccess.STATUS_PUBLISHED;
            assertFalse(access.canEdit(director, published) || access.canPause(director, published)
                    || access.canClose(director, published) || access.canPublish(director, JobPostingAccess.STATUS_DRAFT));
        }

        @Test
        @DisplayName("Xóa: chỉ bản nháp chưa có hồ sơ")
        void canDelete_shouldRequireDraftWithoutApplications() {
            assertTrue(access.canDelete(hr, JobPostingAccess.STATUS_DRAFT, false));
            assertFalse(access.canDelete(hr, JobPostingAccess.STATUS_DRAFT, true));
            assertFalse(access.canDelete(hr, JobPostingAccess.STATUS_PUBLISHED, false));
        }

        @Test
        @DisplayName("Đăng lại: tin đã đóng, yêu cầu còn Approved, chưa có tin đang mở (D10)")
        void canRepost_shouldRequireApprovedRequisitionWithoutOpenPosting() {
            JobRequisition approved = JobRequisition.builder().approvalStatus(RequisitionAccess.STATUS_APPROVED).build();
            JobRequisition rejected = JobRequisition.builder().approvalStatus(RequisitionAccess.STATUS_REJECTED).build();
            String closed = JobPostingAccess.STATUS_CLOSED;

            assertTrue(access.canRepost(hr, closed, approved, false));
            assertFalse(access.canRepost(hr, closed, approved, true), "Đã có tin đang mở");
            assertFalse(access.canRepost(hr, closed, rejected, false), "Yêu cầu không còn Approved");
            assertFalse(access.canRepost(director, closed, approved, false), "Director chỉ xem");
        }
    }
}
