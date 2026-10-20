package com.group2.rms.career.repository;

import com.group2.rms.career.dto.PublicJobListResponse;
import com.group2.rms.career.service.CareerPortalService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Tìm kiếm {@code /jobs} chạy câu SQL thật trên DB local (chỉ SELECT, không ghi gì). Dựa trên tin mẫu của seed; thiếu tin
 * mẫu thì test bị bỏ qua (assume), không fail.
 */
@SpringBootTest
class PublicJobSearchQueryTests {

    /** Giống controller: 6 tin một trang, mới đăng trước. */
    private static final Pageable FIRST_PAGE = PageRequest.of(0, 6, Sort.by(Sort.Direction.DESC, "postingDate"));
    private static final Pageable ALL = PageRequest.of(0, 1000, Sort.by(Sort.Direction.DESC, "postingDate"));
    private static final String OPEN = """
            SELECT COUNT(*) FROM JobPosting WHERE PostingStatus = 'Published'
              AND (PostingDate IS NULL OR PostingDate <= SYSDATETIME())
              AND (ApplicationDeadline IS NULL OR ApplicationDeadline >= SYSDATETIME())""";

    @Autowired CareerPortalService careers;
    @Autowired JdbcTemplate jdbc;

    @Test
    void noKeywordListsEveryOpenJobNewestFirst() {
        Page<PublicJobListResponse> page = careers.getPublishedJobs(null, null, null, ALL);

        assertEquals(jdbc.queryForObject(OPEN, Long.class), page.getTotalElements());
        List<PublicJobListResponse> jobs = page.getContent();
        for (int i = 1; i < jobs.size(); i++) {
            if (jobs.get(i - 1).postingDate() != null && jobs.get(i).postingDate() != null) {
                assertFalse(jobs.get(i - 1).postingDate().isBefore(jobs.get(i).postingDate()), "mới đăng trước");
            }
        }
    }

    @Test
    void accentlessVietnameseFindsTheAccountingJobFirst() {
        assumeOpen("Kế toán tổng hợp");

        assertEquals("Kế toán tổng hợp", titles("ke toan", FIRST_PAGE).getFirst());
        assertEquals("Kế toán tổng hợp", titles("Kế toán", FIRST_PAGE).getFirst());
    }

    @Test
    void anyWordMatchesAndTitleMatchesComeFirst() {
        assumeOpen("Junior Java Developer (Spring Boot)");
        assumeOpen("Senior Java Backend Engineer (Spring Boot / Microservices)");

        List<String> titles = titles("java spring", FIRST_PAGE);
        assertTrue(titles.size() >= 2);
        assertTrue(titles.subList(0, 2).stream().allMatch(t -> t.contains("Java")), "hai tin Java đứng đầu: " + titles);
        assertFalse(titles.contains("Kế toán tổng hợp"));
    }

    @Test
    void wordOnlyInDescriptionOrRequirementsIsFound() {
        assumeTrue(jdbc.queryForObject(OPEN + " AND (JobDescription LIKE N'%Docker%' OR JobRequirements LIKE N'%Docker%')",
                Long.class) > 0, "seed không có tin nào nhắc Docker");

        assertFalse(titles("docker", ALL).isEmpty());
    }

    @Test
    void locationAndDepartmentAreSearchedToo() {
        assumeOpen("Mobile Developer (Flutter) – Hợp đồng 12 tháng");
        assumeOpen("Kế toán tổng hợp");

        assertTrue(titles("da nang", ALL).contains("Mobile Developer (Flutter) – Hợp đồng 12 tháng"));
        assertTrue(titles("finance", ALL).contains("Kế toán tổng hợp"));
    }

    @Test
    void wildcardKeywordDoesNotMatchEverything() {
        long open = jdbc.queryForObject(OPEN, Long.class);
        assumeTrue(open > 0);

        assertTrue(careers.getPublishedJobs("%", null, null, ALL).getTotalElements() < open);
        assertTrue(careers.getPublishedJobs("_", null, null, ALL).getTotalElements() < open);
    }

    @Test
    void keywordCombinesWithDepartmentFilterAndPagingCountsMatches() {
        Integer sales = jdbc.query("SELECT DepartmentId FROM Department WHERE DepartmentName = N'Sales & Marketing'",
                rs -> rs.next() ? rs.getInt(1) : null);
        assumeTrue(sales != null, "seed không có phòng Sales & Marketing");

        Page<PublicJobListResponse> page = careers.getPublishedJobs("java", sales, null, FIRST_PAGE);
        assertTrue(page.getContent().stream().noneMatch(j -> j.postingTitle().contains("Java")));
        assertEquals(page.getContent().size(), Math.min(6, page.getTotalElements()), "đếm và trang khớp nhau");
    }

    private List<String> titles(String keyword, Pageable pageable) {
        return careers.getPublishedJobs(keyword, null, null, pageable).getContent().stream()
                .map(PublicJobListResponse::postingTitle).toList();
    }

    private void assumeOpen(String title) {
        assumeTrue(jdbc.queryForObject(OPEN + " AND PostingTitle = ?", Long.class, title) > 0,
                "seed không có tin đang mở: " + title);
    }
}
