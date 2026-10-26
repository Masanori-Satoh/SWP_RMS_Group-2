package com.group2.rms.dashboard;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Read-only JPQL verification against the configured SQL Server schema. */
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@Transactional(readOnly = true)
class DashboardChartQueryTests {
    @Autowired DashboardMetricsRepository metrics;

    @Test
    void chartQueries_shouldUseExistingSchemaAndPreserveRoleScope() {
        LocalDateTime from = LocalDateTime.of(2026, 10, 1, 0, 0);
        LocalDateTime to = from.plusDays(30);

        assertTrue(metrics.hiringManagerRequisitionStatuses(-1, null).isEmpty());
        assertEquals(0, metrics.interviewsForDepartmentBetween(null, from, to));
        assertEquals(0, metrics.interviewsForDepartmentBetween(-1, from, to));
        assertTrue(metrics.directorDecisions(-1, from, to).isEmpty());
        assertTrue(metrics.assignedInterviewStatuses(-1).isEmpty());
        assertTrue(metrics.upcomingAssignedInterviewDates(-1, from, to).isEmpty());
        assertEquals(0, metrics.completedInterviewsEvaluatedBy(-1));
        assertTrue(metrics.formalApplicationStatuses().stream().allMatch(row -> row.count() >= 0));
        assertTrue(metrics.jobPostingViewStatuses(from).stream().allMatch(row -> row.count() >= 0));
        assertTrue(metrics.recentApplicationDates(from, to).stream()
                .allMatch(date -> !date.isBefore(from) && date.isBefore(to)));
    }
}
