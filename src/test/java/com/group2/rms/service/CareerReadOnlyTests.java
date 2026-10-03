package com.group2.rms.service;

import com.group2.rms.career.dto.DepartmentFilterResponse;
import com.group2.rms.career.dto.PublicJobListResponse;
import com.group2.rms.career.service.CareerPortalService;
import com.group2.rms.core.exception.BaseBusinessException;
import com.group2.rms.core.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** SELECT-only checks against the configured schema. No seed, save, or schema changes. */
@SpringBootTest
@AutoConfigureMockMvc
class CareerReadOnlyTests {
    private static final Pageable ALL = PageRequest.of(0, 1000, Sort.by(Sort.Direction.DESC, "postingDate"));

    @Autowired CareerPortalService careers;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;

    @Test
    void actualDatabaseJobsRenderThroughControllerAndThymeleaf() throws Exception {
        String html = mvc.perform(get("/jobs").param("size", "1000")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(html).contains("Mộc Careers");
        snapshot("index.html", html);
        for (PublicJobListResponse job : careers.getPublishedJobs(null, null, null, ALL)) {
            assertThat(html).contains("/jobs/" + job.id());
            String detail = mvc.perform(get("/jobs/" + job.id())).andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
            assertThat(detail).contains(job.postingTitle());
            snapshot("jobs/" + job.id() + "/index.html", detail);
        }
    }

    @Test
    void publishedJobsMatchSqlWithoutModifyingDatabase() {
        List<Integer> expected = jdbc.queryForList("SELECT JobPostingId FROM JobPosting WHERE PostingStatus = 'Published' "
                        + "AND ApplicationDeadline >= ? ORDER BY JobPostingId",
                Integer.class, LocalDateTime.now());
        var jobs = careers.getPublishedJobs(null, null, null, ALL);
        assertThat(jobs.getContent().stream().map(PublicJobListResponse::id).sorted().toList())
                .containsExactlyElementsOf(expected);
        for (var job : jobs) {
            var detail = careers.getPublishedJobDetail(job.id(), null);
            assertThat(detail.id()).isEqualTo(job.id());
            assertThat(detail.postingTitle()).isEqualTo(job.postingTitle());
            assertThat(detail.isAcceptingApplications()).isTrue();
            assertThatCode(() -> careers.validateJobForApplication(job.id())).doesNotThrowAnyException();
        }
    }

    @Test
    void unpublishedOrMissingJobsAreNotFound() {
        assertThatThrownBy(() -> careers.getPublishedJobDetail(-1, null)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> careers.validateJobForApplication(-1)).isInstanceOf(ResourceNotFoundException.class);
        List<Integer> hidden = jdbc.queryForList(
                "SELECT JobPostingId FROM JobPosting WHERE PostingStatus <> 'Published'", Integer.class);
        for (Integer id : hidden) {
            assertThatThrownBy(() -> careers.getPublishedJobDetail(id, null)).isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Test
    void departmentsAreCompleteAndDepartmentsWithOpenJobsComeFirst() {
        List<DepartmentFilterResponse> departments = careers.getSmartSortedDepartments();
        assertThat(departments).hasSize(jdbc.queryForObject("SELECT COUNT(*) FROM Department", Integer.class));
        List<Integer> active = jdbc.queryForList("SELECT DISTINCT r.DepartmentId FROM JobPosting j "
                + "JOIN JobRequisition r ON r.RequisitionId = j.RequisitionId WHERE j.PostingStatus = 'Published' "
                + "AND (j.ApplicationDeadline IS NULL OR j.ApplicationDeadline >= ?)", Integer.class, LocalDateTime.now());
        boolean seenInactive = false;
        for (DepartmentFilterResponse dept : departments) {
            boolean isActive = active.contains(dept.departmentId());
            assertThat(isActive && seenInactive).as("active department after inactive: " + dept.departmentName()).isFalse();
            if (!isActive) seenInactive = true;
        }
    }

    @Test
    void viewerProfileAndCandidateRoleCheckUseUserTable() {
        var candidates = jdbc.queryForList("SELECT u.Username FROM [User] u JOIN Role r ON r.RoleId = u.RoleId "
                + "WHERE r.RoleName = 'Candidate'", String.class);
        for (String username : candidates) {
            var viewer = careers.getViewerProfile(username).orElseThrow();
            assertThat(viewer.email()).isEqualTo(
                    jdbc.queryForObject("SELECT Email FROM [User] WHERE Username = ?", String.class, username));
            assertThatCode(() -> careers.validateCandidateApplicationProfile(username)).doesNotThrowAnyException();
        }
        var internal = jdbc.queryForList("SELECT u.Username FROM [User] u JOIN Role r ON r.RoleId = u.RoleId "
                + "WHERE r.RoleName <> 'Candidate'", String.class);
        for (String username : internal) {
            assertThatThrownBy(() -> careers.validateCandidateApplicationProfile(username))
                    .isInstanceOf(BaseBusinessException.class);
        }
        assertThat(careers.getViewerProfile("__nonexistent_career_test__")).isEmpty();
        assertThat(careers.getViewerProfile(null)).isEmpty();
        assertThatThrownBy(() -> careers.validateCandidateApplicationProfile(null)).isInstanceOf(BaseBusinessException.class);
    }

    private void snapshot(String filename, String html) throws Exception {
        if (!Boolean.getBoolean("career.preview")) return;
        Path target = Path.of("target/career-db-preview").resolve(filename);
        Files.createDirectories(target.getParent());
        Files.writeString(target, html, StandardCharsets.UTF_8);
    }
}
