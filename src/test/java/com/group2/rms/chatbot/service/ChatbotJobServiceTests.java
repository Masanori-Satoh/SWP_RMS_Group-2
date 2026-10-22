package com.group2.rms.chatbot.service;

import com.group2.rms.career.dto.PublicJobDetailResponse;
import com.group2.rms.career.service.CareerPortalService;
import com.group2.rms.chatbot.dto.ChatResponse.JobCard;
import com.group2.rms.core.exception.ResourceNotFoundException;
import com.group2.rms.jobposting.entity.JobPosting;
import com.group2.rms.jobposting.repository.JobPostingRepository;
import com.group2.rms.jobposting.service.JobPostingAccess;
import com.group2.rms.requisition.entity.JobRequisition;
import com.group2.rms.user.entity.Department;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/*
 * UNIT TEST (Mockito): ChatbotJobService (tìm tin, chi tiết tin, gợi ý tin).
 * JobPostingRepository và CareerPortalService là mock; tin đang mở dựng bằng tay (đã kèm phòng ban).
 * Câu JPQL findOpenWithDepartment / findByJobPostingIdIn chạy thật ở ChatbotIntegrationTests.
 * Quy ước (docs/members/hoangnh/CLAUDE.md §23): hàm_shouldKếtQuả_whenĐiềuKiện, Clock cố định.
 */
@ExtendWith(MockitoExtension.class)
class ChatbotJobServiceTests {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-20T03:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));
    private static final LocalDateTime NOW = LocalDateTime.now(CLOCK);

    @Mock
    private JobPostingRepository postings;
    @Mock
    private CareerPortalService careerPortalService;

    private ChatbotJobService service;
    private final List<JobPosting> openJobs = new ArrayList<>();
    private Department engineering;
    private Department sales;

    @BeforeEach
    void setUp() {
        service = new ChatbotJobService(postings, careerPortalService, CLOCK);
        engineering = Department.builder().departmentId(1).departmentName("Phòng Kỹ thuật").build();
        sales = Department.builder().departmentId(2).departmentName("Phòng Kinh doanh").build();
        lenient().when(postings.findOpenWithDepartment(JobPostingAccess.STATUS_PUBLISHED, NOW)).thenReturn(openJobs);
    }

    // ----- Hàm hỗ trợ -----

    private static JobPosting posting(int id, String title, String location, Department department) {
        JobRequisition requisition = JobRequisition.builder().department(department).employmentType("Full-time").build();
        return JobPosting.builder().jobPostingId(id).postingTitle(title).workLocation(location)
                .requisition(requisition).salaryDisplay("15 - 20 triệu").applicationDeadline(NOW.plusDays(10)).build();
    }

    private static List<String> titles(List<JobCard> cards) {
        return cards.stream().map(JobCard::title).toList();
    }

    @Nested
    @DisplayName("search")
    class Search {

        @Test
        @DisplayName("\"Việc Java ở Hà Nội\" -> Chỉ tin có cả Java lẫn Hà Nội, không phân biệt dấu")
        void search_shouldReturnOnlyJobsMatchingAllTerms_whenMessageHasTitleAndLocation() {
            // Arrange
            openJobs.add(posting(1, "Lập trình viên Java", "Hà Nội", engineering));
            openJobs.add(posting(2, "Lập trình viên Java", "TP. Hồ Chí Minh", engineering));
            openJobs.add(posting(3, "Nhân viên kinh doanh", "Hà Nội", sales));

            // Act
            ChatbotJobService.SearchResult result = service.search("Có việc java nào ở ha noi không?");

            // Assert
            assertEquals(1, result.total());
            assertEquals(1, result.jobs().get(0).id());
            assertEquals("/jobs/1", result.jobs().get(0).detailHref());
        }

        @Test
        @DisplayName("Gõ tắt \"hcm\" -> Khớp tin ở TP. Hồ Chí Minh")
        void search_shouldMatchFullLocation_whenMessageUsesShortName() {
            // Arrange
            openJobs.add(posting(2, "Lập trình viên Java", "TP. Hồ Chí Minh", engineering));

            // Act + Assert
            assertEquals(1, service.search("java hcm").total());
        }

        @Test
        @DisplayName("Tìm theo tên phòng ban -> Khớp")
        void search_shouldMatchDepartmentName_whenMessageHasDepartment() {
            // Arrange
            openJobs.add(posting(3, "Nhân viên", "Hà Nội", sales));
            openJobs.add(posting(4, "Nhân viên", "Hà Nội", engineering));

            // Act
            ChatbotJobService.SearchResult result = service.search("tìm việc phòng kinh doanh");

            // Assert
            assertEquals(List.of("Nhân viên"), titles(result.jobs()));
            assertEquals(3, result.jobs().get(0).id());
        }

        @Test
        @DisplayName("Nhiều hơn MAX_JOB_CARDS tin khớp -> Tổng đúng, chỉ hiện MAX_JOB_CARDS thẻ")
        void search_shouldLimitCards_whenManyJobsMatch() {
            // Arrange
            for (int i = 1; i <= ChatbotJobService.MAX_JOB_CARDS + 2; i++) {
                openJobs.add(posting(i, "Java " + i, "Hà Nội", engineering));
            }

            // Act
            ChatbotJobService.SearchResult result = service.search("java");

            // Assert
            assertEquals(ChatbotJobService.MAX_JOB_CARDS + 2, result.total());
            assertEquals(ChatbotJobService.MAX_JOB_CARDS, result.jobs().size());
        }

        @Test
        @DisplayName("\"Tìm việc\" (không có từ có nghĩa) -> Mọi tin đang mở, không có từ để lọc")
        void search_shouldReturnAllOpenJobs_whenMessageHasNoTerms() {
            // Arrange
            openJobs.add(posting(1, "Java", "Hà Nội", engineering));
            openJobs.add(posting(2, "Kế toán", "Đà Nẵng", sales));

            // Act + Assert
            assertEquals(2, service.search("Tìm việc làm").total());
            assertFalse(service.hasSearchTerms("Tìm việc làm"));
            assertTrue(service.hasSearchTerms("Tìm việc Java"));
        }

        @Test
        @DisplayName("Không tin nào khớp -> total = 0, không có thẻ")
        void search_shouldReturnEmpty_whenNothingMatches() {
            // Arrange
            openJobs.add(posting(1, "Java", "Hà Nội", engineering));

            // Act
            ChatbotJobService.SearchResult result = service.search("golang");

            // Assert
            assertEquals(0, result.total());
            assertTrue(result.jobs().isEmpty());
        }
    }

    @Nested
    @DisplayName("detail")
    class Detail {

        private PublicJobDetailResponse detail(boolean accepting, boolean hasApplied, List<String> requirements) {
            return new PublicJobDetailResponse(5, "Java Dev", "Phòng Kỹ thuật", "Hà Nội", "Full-time", " ",
                    NOW.plusDays(3), NOW.minusDays(1), "", List.of(), "", requirements, "", List.of("Lương tháng 13"),
                    hasApplied, accepting);
        }

        @Test
        @DisplayName("Tin còn nhận, chưa nộp -> canApply, chỉ lấy MAX_DETAIL_LINES dòng yêu cầu, lương trống thành null")
        void detail_shouldTrimLinesAndAllowApply_whenJobIsOpenAndNotApplied() {
            // Arrange
            when(careerPortalService.getPublishedJobDetail(5, "candidate"))
                    .thenReturn(detail(true, false, List.of("A", "B", "C", "D")));

            // Act
            ChatbotJobService.DetailResult result = service.detail(5, "candidate").orElseThrow();

            // Assert
            assertTrue(result.canApply());
            assertEquals(List.of("A", "B", "C"), result.detail().requirements());
            assertNull(result.detail().salaryDisplay());
        }

        @Test
        @DisplayName("Đã nộp tin này -> canApply = false")
        void detail_shouldNotAllowApply_whenAlreadyApplied() {
            // Arrange
            when(careerPortalService.getPublishedJobDetail(5, "candidate")).thenReturn(detail(true, true, List.of()));

            // Act + Assert
            assertFalse(service.detail(5, "candidate").orElseThrow().canApply());
        }

        @Test
        @DisplayName("Tin không còn hiện cho ứng viên -> Rỗng, không ném lỗi ra ngoài")
        void detail_shouldReturnEmpty_whenJobIsNotVisible() {
            // Arrange
            when(careerPortalService.getPublishedJobDetail(eq(99), any()))
                    .thenThrow(new ResourceNotFoundException("Không tìm thấy"));

            // Act
            Optional<ChatbotJobService.DetailResult> result = service.detail(99, null);

            // Assert
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("recommend")
    class Recommend {

        @Test
        @DisplayName("Đã nộp tin Kỹ thuật ở Hà Nội -> Cùng phòng ban trước, rồi cùng địa điểm; bỏ tin đã nộp")
        void recommend_shouldRankSameDepartmentThenSameLocation_excludingAppliedJobs() {
            // Arrange
            JobPosting applied = posting(1, "Java cũ", "Hà Nội", engineering);
            openJobs.add(applied);
            openJobs.add(posting(2, "Kinh doanh Hà Nội", "Hà Nội", sales));
            openJobs.add(posting(3, "Kỹ thuật HCM", "TP. Hồ Chí Minh", engineering));
            openJobs.add(posting(4, "Kinh doanh Đà Nẵng", "Đà Nẵng", sales));
            when(postings.findByJobPostingIdIn(anyCollection())).thenReturn(List.of(applied));

            // Act
            ChatbotJobService.RecommendResult result = service.recommend(List.of(1));

            // Assert
            assertTrue(result.fromHistory());
            assertEquals(List.of("Kỹ thuật HCM", "Kinh doanh Hà Nội"), titles(result.jobs()));
            assertEquals(ChatbotLabels.REASON_SAME_DEPARTMENT, result.jobs().get(0).reason());
            assertEquals(ChatbotLabels.REASON_SAME_LOCATION, result.jobs().get(1).reason());
            verify(postings, times(1)).findByJobPostingIdIn(anyCollection());
        }

        @Test
        @DisplayName("Chưa nộp tin nào -> Tin mới nhất, không tra tin đã nộp")
        void recommend_shouldReturnLatestJobs_whenNoApplication() {
            // Arrange
            openJobs.add(posting(1, "Mới nhất", "Hà Nội", engineering));

            // Act
            ChatbotJobService.RecommendResult result = service.recommend(List.of());

            // Assert
            assertFalse(result.fromHistory());
            assertEquals(ChatbotLabels.REASON_LATEST, result.jobs().get(0).reason());
            verify(postings, never()).findByJobPostingIdIn(anyCollection());
        }

        @Test
        @DisplayName("Không tin nào cùng phòng ban / địa điểm -> Quay về tin mới nhất chưa nộp")
        void recommend_shouldFallBackToLatest_whenNothingIsSimilar() {
            // Arrange
            JobPosting appliedClosed = posting(9, "Tin đã đóng", "Cần Thơ", engineering);
            openJobs.add(posting(2, "Kinh doanh", "Đà Nẵng", sales));
            when(postings.findByJobPostingIdIn(anyCollection())).thenReturn(List.of(appliedClosed));

            // Act
            ChatbotJobService.RecommendResult result = service.recommend(List.of(9));

            // Assert
            assertFalse(result.fromHistory());
            assertEquals(List.of("Kinh doanh"), titles(result.jobs()));
        }
    }
}
