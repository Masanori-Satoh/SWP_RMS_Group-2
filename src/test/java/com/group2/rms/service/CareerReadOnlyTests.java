package com.group2.rms.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** SELECT-only checks against the configured schema. No seed, save, or schema changes. */
@SpringBootTest
@AutoConfigureMockMvc
class CareerReadOnlyTests {
    @Autowired CareerService careers;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;

    @Test
    void actualDatabaseJobsRenderThroughControllerAndThymeleaf() throws Exception {
        var response = mvc.perform(get("/")).andExpect(status().isOk()).andReturn().getResponse();
        String html = response.getContentAsString(StandardCharsets.UTF_8);
        assertThat(html).contains("Open Positions", "lang=\"en\"").doesNotContain("const jobs");
        snapshot("index.html", html);
        for (var job : careers.openJobs()) {
            assertThat(html).contains("/jobs/" + job.jobPostingId());
            String detail = mvc.perform(get("/jobs/" + job.jobPostingId())).andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
            assertThat(detail).contains("/jobs/" + job.jobPostingId() + "/apply", "Overview &amp; Responsibilities");
            snapshot("jobs/" + job.jobPostingId() + "/index.html", detail);
        }
    }

    private void snapshot(String filename, String html) throws Exception {
        if (!Boolean.getBoolean("career.preview")) return;
        Path target = Path.of("target/career-db-preview").resolve(filename);
        Files.createDirectories(target.getParent());
        Files.writeString(target, html, StandardCharsets.UTF_8);
    }

    @Test
    void publishedJobsAndRelationshipsMatchSqlWithoutModifyingDatabase() {
        List<Integer> expected = jdbc.queryForList("SELECT JobPostingId FROM JobPosting WHERE PostingStatus = 'Published' "
                + "AND (ApplicationDeadline IS NULL OR ApplicationDeadline >= ?) ORDER BY PostingDate DESC, JobPostingId DESC",
                Integer.class, LocalDateTime.now());
        var jobs = careers.openJobs();
        assertThat(jobs.stream().map(CareerService.PublicJob::jobPostingId).toList()).isEqualTo(expected);
        for (var job : jobs) {
            assertThat(careers.openJob(job.jobPostingId())).contains(job);
            assertThat(job.departmentId()).isNotNull();
            assertThat(job.departmentName()).isNotBlank();
            assertThat(job.employmentType()).isNotBlank();
        }
        assertThat(careers.openJob(-1)).isEmpty();
        assertThat(careers.departments()).hasSize(jdbc.queryForObject("SELECT COUNT(*) FROM Department", Integer.class));
    }

    @Test
    void profileReadUsesCandidateUserForeignKey() {
        var usernames = jdbc.queryForList("SELECT u.Username FROM [User] u JOIN Candidate c ON c.UserId = u.UserId "
                + "JOIN Role r ON r.RoleId = u.RoleId WHERE r.RoleName = 'Candidate'", String.class);
        for (String username : usernames) {
            var details = careers.candidateDetails(username).orElseThrow();
            assertThat(details.email()).isEqualTo(jdbc.queryForObject("SELECT Email FROM [User] WHERE Username = ?", String.class, username));
        }
        assertThat(careers.candidateDetails("__nonexistent_career_test__")).isEmpty();
    }
}
