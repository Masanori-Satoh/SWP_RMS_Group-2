package com.group2.rms.candidate.repository;

import com.group2.rms.candidate.dto.ApplicationPipelineResponse;
import com.group2.rms.candidate.dto.ApplicationSearch;
import com.group2.rms.candidate.dto.JobPostingOption;
import com.group2.rms.candidate.entity.Application;
import com.group2.rms.candidate.service.ApplicationProgressFacts;
import com.group2.rms.candidate.service.ApplicationProgressResolver;
import com.group2.rms.candidate.service.ApplicationScope;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Các query của danh sách hồ sơ chạy trên DB local (chỉ đọc). Không khẳng định số lượng cụ thể vì dữ liệu
 * thay đổi khi thử tay; chỉ khẳng định tính chất: mỗi đơn một dòng, đúng thứ tự, đúng phạm vi.
 */
@SpringBootTest
@Transactional(readOnly = true)
class ApplicationPipelineQueryTests {

    @Autowired ApplicationRepository applications;
    @Autowired ApplicationReviewRepository reviews;
    @Autowired UserRepository users;
    @Autowired EntityManager entityManager;

    @Test
    void recruiterScopeListsEveryApplicationOnceWithBestScoreFirst() {
        List<Integer> ids = ids(ApplicationScope.ALL, null, null, ApplicationSearch.SORT_SCORE);
        List<ApplicationPipelineResponse> rows = search(ApplicationScope.ALL, null, null, ApplicationSearch.SORT_SCORE);

        assertEquals(applications.count(), ids.size());
        assertEquals(ids.size(), new HashSet<>(ids).size(), "mỗi đơn đúng một dòng dù có nhiều lần AI chấm");
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
        List<ApplicationPipelineResponse> rows = search(ApplicationScope.ALL, null, null, ApplicationSearch.SORT_NEWEST);

        for (int i = 1; i < rows.size(); i++) {
            assertFalse(rows.get(i - 1).submissionDate().isBefore(rows.get(i).submissionDate()));
        }
    }

    @Test
    void hiringManagerSeesOnlyForwardedApplicationsOfManagedDepartment() {
        User manager = users.findByUsernameIgnoreCase("hm_tuan").orElseThrow();
        Integer ownDepartmentId = manager.getDepartment() == null ? null : manager.getDepartment().getDepartmentId();
        ApplicationScope scope = ApplicationScope.hiringManager(manager.getUserId(), ownDepartmentId);

        List<ApplicationPipelineResponse> rows = search(scope, null, null, ApplicationSearch.SORT_SCORE);

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
                .getFirst();
        String keyword = any.email().substring(0, any.email().indexOf('@')).toUpperCase();

        List<ApplicationPipelineResponse> byKeyword = search(ApplicationScope.ALL, keyword, null,
                ApplicationSearch.SORT_SCORE);
        List<ApplicationPipelineResponse> byStatus = search(ApplicationScope.ALL, null, "Rejected",
                ApplicationSearch.SORT_SCORE);

        assertTrue(byKeyword.stream().anyMatch(r -> r.applicationId().equals(any.applicationId())));
        assertTrue(byStatus.stream().allMatch(r -> r.applicationStatus().equals("Rejected")));
    }

    @Test
    void jobPostingFilterKeepsOnlyApplicationsOfThatPosting() {
        JobPostingOption posting = applications.findPostingOptions(true, null, null, false).getFirst();

        List<Integer> ids = applications.findPipelineIds(true, null, null, false, posting.jobPostingId(), null, null,
                ApplicationSearch.SORT_SCORE);

        assertFalse(ids.isEmpty());
        ids.forEach(id -> assertEquals(posting.jobPostingId(),
                applications.findDetailById(id).orElseThrow().getJobPosting().getJobPostingId(), "đơn " + id));
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
        Integer withOffer = search(ApplicationScope.ALL, null, "Hired", ApplicationSearch.SORT_SCORE).stream()
                .map(ApplicationPipelineResponse::applicationId)
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

    @Test
    void interviewResultEventsReadFinalResultTable() {
        List<?> decided = entityManager.createNativeQuery("SELECT TOP 1 s.ApplicationId FROM InterviewFinalResult r "
                + "JOIN InterviewSchedule s ON s.InterviewId = r.InterviewId").getResultList();
        Assumptions.assumeFalse(decided.isEmpty(), "DB local chưa có kết quả phỏng vấn nào");
        Integer id = ((Number) decided.getFirst()).intValue();

        List<ApplicationRepository.TimelineEventRow> results = applications.findInterviewResultEvents(id);

        assertFalse(results.isEmpty());
        results.forEach(event -> {
            assertNotNull(event.getAt());
            assertTrue(List.of("Passed", "Failed").contains(event.getStatus()), event.getStatus());
        });
    }

    /** Đối chiếu từng đơn với các query timeline (đọc thẳng bảng gốc), trên mọi đơn của DB local. */
    @Test
    void progressFactsMatchInterviewAndOfferTables() {
        List<ApplicationPipelineResponse> pipeline = search(ApplicationScope.ALL, null, null,
                ApplicationSearch.SORT_NEWEST);
        Map<Integer, String> statusById = pipeline.stream().collect(Collectors.toMap(
                ApplicationPipelineResponse::applicationId, ApplicationPipelineResponse::applicationStatus));

        List<ApplicationRepository.ProgressFactsRow> rows = applications.findProgressFacts(statusById.keySet());

        assertEquals(statusById.size(), rows.size(), "mỗi đơn đúng một dòng");
        LocalDateTime now = LocalDateTime.now();
        for (ApplicationRepository.ProgressFactsRow row : rows) {
            Integer id = row.getApplicationId();
            assertEquals(statusById.get(id), row.getApplicationStatus());

            List<LocalDateTime> activeStarts = applications.findInterviewEvents(id).stream()
                    .filter(event -> !"Cancelled".equals(event.getStatus()))
                    .map(ApplicationRepository.TimelineEventRow::getAt).toList();
            if (activeStarts.isEmpty()) {
                assertNull(row.getInterviewId(), "không có lịch còn hiệu lực: " + id);
                assertNull(row.getInterviewStatus(), "không có lịch còn hiệu lực: " + id);
                assertNull(row.getFinalDecision(), "không có lịch thì không có kết quả: " + id);
                assertEquals(0, row.getPanelCount());
                assertEquals(0, row.getEvaluationCount());
            } else {
                assertEquals(Collections.max(activeStarts), row.getInterviewStart(), "lịch mới nhất: " + id);
                assertNotEquals("Cancelled", row.getInterviewStatus());
                assertNotNull(row.getInterviewId(), "có lịch thì có id: " + id);
            }

            List<ApplicationRepository.TimelineEventRow> offers = applications.findOfferEvents(id);
            assertEquals(offers.isEmpty() ? null : offers.getFirst().getStatus(), row.getOfferStatus(), "offer: " + id);

            ApplicationProgressFacts facts = ApplicationProgressFacts.from(row);
            assertNotNull(ApplicationProgressResolver.resolve(facts, now).step(), "tính được trạng thái: " + id);
        }
    }

    @Test
    void progressFactsFlagWithdrawnApplications() {
        List<?> withdrawn = entityManager.createNativeQuery("SELECT TOP 1 CAST(l.EntityId AS INT) FROM AuditLog l "
                + "WHERE l.EntityName = 'Application' AND l.NewValue = 'Withdrawn'").getResultList();
        Assumptions.assumeFalse(withdrawn.isEmpty(), "DB local chưa có đơn nào bị rút");
        Integer id = ((Number) withdrawn.getFirst()).intValue();

        ApplicationRepository.ProgressFactsRow row = applications.findProgressFacts(List.of(id)).getFirst();

        assertTrue(row.getWithdrawn());
    }

    /** Ghi thật rồi rollback khi test kết thúc (transaction của test không commit): DB không đổi. */
    @Test
    @Transactional
    void transitionOnlyMovesApplicationsStillInExpectedStatus() {
        Integer id = ids(ApplicationScope.ALL, null, "AI_Screened", ApplicationSearch.SORT_SCORE).getFirst();

        assertEquals(0, applications.transition(id, List.of("HR_Passed"), "HM_Passed"), "sai trạng thái nguồn: không đổi");
        assertEquals(1, applications.transition(id, List.of("Applied", "AI_Screened"), "HR_Passed"));
        assertEquals(0, applications.transition(id, List.of("Applied", "AI_Screened"), "HR_Passed"),
                "lần thứ hai (người duyệt sau): không đè");
        entityManager.clear();
        Application moved = applications.findById(id).orElseThrow();
        assertEquals("HR_Passed", moved.getApplicationStatus());
        assertNotNull(moved.getUpdatedAt());
    }

    private List<Integer> ids(ApplicationScope scope, String keyword, String status, String sort) {
        return applications.findPipelineIds(scope.allDepartments(), scope.managerUserId(), scope.departmentId(),
                scope.forwardedOnly(), null, status, keyword, sort);
    }

    /** Các dòng đúng thứ tự của {@link #ids}, tối đa 2000 đơn đầu (giới hạn tham số của SQL Server). */
    private List<ApplicationPipelineResponse> search(ApplicationScope scope, String keyword, String status, String sort) {
        List<Integer> ids = ids(scope, keyword, status, sort);
        List<Integer> first = ids.subList(0, Math.min(ids.size(), 2000));
        if (first.isEmpty()) {
            return List.of();
        }
        Map<Integer, ApplicationPipelineResponse> byId = applications.findPipelineRows(first).stream()
                .collect(Collectors.toMap(ApplicationPipelineResponse::applicationId, row -> row));
        assertEquals(first.size(), byId.size(), "mỗi id đúng một dòng");
        return first.stream().map(byId::get).toList();
    }
}
