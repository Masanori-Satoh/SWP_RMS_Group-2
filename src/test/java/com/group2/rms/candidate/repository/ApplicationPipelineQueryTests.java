package com.group2.rms.candidate.repository;

import com.group2.rms.candidate.dto.ApplicationPipelineResponse;
import com.group2.rms.candidate.dto.ApplicationSearch;
import com.group2.rms.candidate.dto.JobPostingOption;
import com.group2.rms.candidate.entity.Application;
import com.group2.rms.candidate.service.ApplicationScope;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Các query của danh sách hồ sơ chạy trên DB local (chỉ đọc). Không khẳng định số lượng cụ thể vì dữ liệu
 * thay đổi khi thử tay; chỉ khẳng định tính chất: mỗi đơn một dòng, đúng thứ tự, đúng phạm vi.
 */
@SpringBootTest
@Transactional(readOnly = true)
class ApplicationPipelineQueryTests {

    private static final PageRequest ALL_ROWS = PageRequest.of(0, 5000);

    @Autowired ApplicationRepository applications;
    @Autowired ApplicationReviewRepository reviews;
    @Autowired UserRepository users;
    @Autowired EntityManager entityManager;

    @Test
    void recruiterScopeListsEveryApplicationOnceWithBestScoreFirst() {
        Page<ApplicationPipelineResponse> page = search(ApplicationScope.ALL, null, null, ApplicationSearch.SORT_SCORE);

        List<ApplicationPipelineResponse> rows = page.getContent();
        assertEquals(applications.count(), page.getTotalElements());
        assertEquals(rows.size(), new HashSet<>(rows.stream().map(ApplicationPipelineResponse::applicationId).toList())
                .size(), "mỗi đơn đúng một dòng dù có nhiều lần AI chấm");
        int firstUnscored = rows.stream().map(ApplicationPipelineResponse::aiMatchScore).toList().indexOf(null);
        if (firstUnscored >= 0) {
            assertTrue(rows.subList(firstUnscored, rows.size()).stream().allMatch(r -> r.aiMatchScore() == null),
                    "đơn chưa có điểm xếp cuối");
        }
        List<BigDecimal> scores = rows.stream().map(ApplicationPipelineResponse::aiMatchScore)
                .filter(Objects::nonNull).toList();
        for (int i = 1; i < scores.size(); i++) {
            assertTrue(scores.get(i - 1).compareTo(scores.get(i)) >= 0, "điểm giảm dần tại vị trí " + i);
        }
    }

    @Test
    void newestSortOrdersBySubmissionDate() {
        List<ApplicationPipelineResponse> rows = search(ApplicationScope.ALL, null, null, ApplicationSearch.SORT_NEWEST)
                .getContent();

        for (int i = 1; i < rows.size(); i++) {
            assertFalse(rows.get(i - 1).submissionDate().isBefore(rows.get(i).submissionDate()));
        }
    }

    @Test
    void hiringManagerSeesOnlyForwardedApplicationsOfManagedDepartment() {
        User manager = users.findByUsernameIgnoreCase("hm_tuan").orElseThrow();
        Integer ownDepartmentId = manager.getDepartment() == null ? null : manager.getDepartment().getDepartmentId();
        ApplicationScope scope = ApplicationScope.hiringManager(manager.getUserId(), ownDepartmentId);

        List<ApplicationPipelineResponse> rows = search(scope, null, null, ApplicationSearch.SORT_SCORE).getContent();

        assertFalse(rows.isEmpty());
        for (ApplicationPipelineResponse row : rows) {
            Application application = applications.findDetailById(row.applicationId()).orElseThrow();
            var department = application.getJobPosting().getRequisition().getDepartment();
            boolean managesIt = department.getManager() != null
                    && Objects.equals(department.getManager().getUserId(), manager.getUserId());
            assertTrue(managesIt || Objects.equals(department.getDepartmentId(), ownDepartmentId),
                    "đúng phòng ban: " + row.applicationId());
            assertTrue(reviews.existsByApplication_ApplicationIdAndReviewerRoleAndDecision(
                    row.applicationId(), "HR", "Pass"), "đã được HR chuyển: " + row.applicationId());
        }
        assertTrue(rows.size() < applications.count());
    }

    @Test
    void keywordAndStatusFiltersNarrowTheList() {
        ApplicationPipelineResponse any = search(ApplicationScope.ALL, null, null, ApplicationSearch.SORT_SCORE)
                .getContent().getFirst();
        String keyword = any.email().substring(0, any.email().indexOf('@')).toUpperCase();

        List<ApplicationPipelineResponse> byKeyword = search(ApplicationScope.ALL, keyword, null,
                ApplicationSearch.SORT_SCORE).getContent();
        List<ApplicationPipelineResponse> byStatus = search(ApplicationScope.ALL, null, "Rejected",
                ApplicationSearch.SORT_SCORE).getContent();

        assertTrue(byKeyword.stream().anyMatch(r -> r.applicationId().equals(any.applicationId())));
        assertTrue(byStatus.stream().allMatch(r -> r.applicationStatus().equals("Rejected")));
    }

    @Test
    void postingOptionsAreDistinctAndSorted() {
        List<JobPostingOption> options = applications.findPostingOptions(true, null, null, false);

        assertFalse(options.isEmpty());
        assertEquals(options.size(), new HashSet<>(options).size());
    }

    /** Seed không khớp trạng thái (đơn {@code Offered} chưa chắc có offer), nên tìm đơn đã tuyển có offer thật. */
    @Test
    void timelineQueriesReadInterviewAndOfferTables() {
        Integer withOffer = search(ApplicationScope.ALL, null, "Hired", ApplicationSearch.SORT_SCORE).getContent()
                .stream().map(ApplicationPipelineResponse::applicationId)
                .filter(id -> !applications.findOfferEvents(id).isEmpty())
                .findFirst().orElseThrow(() -> new AssertionError("DB local không có đơn Hired nào có offer"));

        ApplicationRepository.TimelineEventRow offer = applications.findOfferEvents(withOffer).getFirst();
        List<ApplicationRepository.TimelineEventRow> interviews = applications.findInterviewEvents(withOffer);

        assertNotNull(offer.getAt());
        assertNotNull(offer.getStatus());
        assertFalse(interviews.isEmpty());
        interviews.forEach(event -> {
            assertNotNull(event.getAt());
            assertNotNull(event.getStatus());
        });
    }

    /** Ghi thật rồi rollback khi test kết thúc (transaction của test không commit): DB không đổi. */
    @Test
    @Transactional
    void transitionOnlyMovesApplicationsStillInExpectedStatus() {
        Integer id = search(ApplicationScope.ALL, null, "AI_Screened", ApplicationSearch.SORT_SCORE)
                .getContent().getFirst().applicationId();

        assertEquals(0, applications.transition(id, List.of("HR_Passed"), "HM_Passed"), "sai trạng thái nguồn: không đổi");
        assertEquals(1, applications.transition(id, List.of("Applied", "AI_Screened"), "HR_Passed"));
        assertEquals(0, applications.transition(id, List.of("Applied", "AI_Screened"), "HR_Passed"),
                "lần thứ hai (người duyệt sau): không đè");
        entityManager.clear();
        Application moved = applications.findById(id).orElseThrow();
        assertEquals("HR_Passed", moved.getApplicationStatus());
        assertNotNull(moved.getUpdatedAt());
    }

    private Page<ApplicationPipelineResponse> search(ApplicationScope scope, String keyword, String status, String sort) {
        return applications.searchPipeline(scope.allDepartments(), scope.managerUserId(), scope.departmentId(),
                scope.forwardedOnly(), null, status, keyword, sort, ALL_ROWS);
    }
}
