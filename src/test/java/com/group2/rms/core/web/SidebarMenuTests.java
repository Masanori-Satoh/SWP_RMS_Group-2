package com.group2.rms.core.web;

import com.group2.rms.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Menu "Hồ sơ ứng tuyển" trong cả 2 nguồn sidebar (mới: {@code sidebar-shell}, cũ: {@code fragments/sidebar}):
 * đúng vai trò mới thấy, các mục có sẵn không mất, không đổi thứ tự.
 * Render bằng controller thăm dò chỉ có trong test ({@code templates/test/sidebar-probe.html}).
 */
@WebMvcTest(SidebarMenuTests.ProbeController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(SidebarMenuTests.ProbeController.class)
class SidebarMenuTests {

    private static final String MENU = "href=\"/applications\"";

    @Autowired MockMvc mvc;
    @MockitoBean UserRepository users;

    @ParameterizedTest
    @ValueSource(strings = {"HR", "Hiring Manager", "Director"})
    void recruiterRolesSeeApplicationsBetweenJobPostingsAndInterviews(String role) throws Exception {
        for (String sidebar : List.of(shell(role, "dashboard"), legacy(role, "dashboard"))) {
            assertTrue(sidebar.contains(MENU), role);
            assertTrue(sidebar.contains("href=\"/requisitions\""), role + ": mục cũ vẫn còn");
            assertTrue(sidebar.contains("href=\"/interviews\""), role + ": mục cũ vẫn còn");
            int applications = sidebar.indexOf(MENU);
            assertTrue(applications < sidebar.indexOf("href=\"/interviews\""), role + ": đứng trước Lịch phỏng vấn");
            if (sidebar.contains("href=\"/internal/job-postings\"")) {
                assertTrue(sidebar.indexOf("href=\"/internal/job-postings\"") < applications, role + ": sau Tin tuyển dụng");
            }
        }
    }

    @Test
    void adminSeesApplicationsInBothSidebars() throws Exception {
        String shell = shell("System Admin", "dashboard");
        assertTrue(shell.contains(MENU));
        assertTrue(shell.contains("href=\"/admin/accounts\""), "mục quản trị vẫn còn");
        assertTrue(legacy("System Admin", "dashboard").contains(MENU));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Candidate", "Interviewer"})
    void candidateAndInterviewerNeverSeeApplications(String role) throws Exception {
        assertFalse(shell(role, "dashboard").contains(MENU), role);
        assertFalse(legacy(role, "dashboard").contains(MENU), role);
    }

    /** /internal/job-postings/** chỉ cho HR và Admin (SecurityConfig): vai trò khác không được thấy link dẫn tới 403. */
    @Test
    void jobPostingsEntryOnlyForRolesAllowedToOpenIt() throws Exception {
        String jobPostings = "href=\"/internal/job-postings\"";
        assertTrue(shell("HR", "dashboard").contains(jobPostings));
        assertTrue(shell("System Admin", "dashboard").contains(jobPostings));
        for (String role : List.of("Hiring Manager", "Director", "Interviewer", "Candidate")) {
            assertFalse(shell(role, "dashboard").contains(jobPostings), role);
            assertFalse(legacy(role, "dashboard").contains(jobPostings), role);
        }
    }

    @Test
    void applicationsEntryIsHighlightedOnItsOwnPages() throws Exception {
        String shell = shell("HR", "applications");
        int link = shell.indexOf(MENU);
        String tag = shell.substring(shell.lastIndexOf("<a", link), shell.indexOf('>', link));
        assertTrue(tag.contains("active"), tag);
        assertTrue(tag.contains("aria-current=\"page\""), tag);
        assertFalse(shell("HR", "dashboard").substring(shell("HR", "dashboard").indexOf(MENU) - 120,
                shell("HR", "dashboard").indexOf(MENU)).contains("aria-current"));
    }

    /** Cùng một vai trò phải thấy cùng một bộ mục, dù trang dùng layout mới hay sidebar cũ. */
    @ParameterizedTest
    @ValueSource(strings = {"HR", "Hiring Manager", "Director", "System Admin", "Interviewer", "Candidate"})
    void bothSidebarSourcesShowTheSameEntriesForEachRole(String role) throws Exception {
        assertEquals(links(legacy(role, "dashboard")), links(shell(role, "dashboard")), role);
    }

    /** href của mọi mục menu, theo thứ tự xuất hiện. */
    private static List<String> links(String html) {
        Pattern href = Pattern.compile("href=\"([^\"]*)\"");
        return Pattern.compile("<a\\b[^>]*>").matcher(html).results()
                .map(MatchResult::group)
                .filter(tag -> tag.contains("sidebar-nav-link"))
                .map(tag -> {
                    Matcher m = href.matcher(tag);
                    return m.find() ? m.group(1) : tag;
                })
                .toList();
    }

    private String shell(String role, String activeMenu) throws Exception {
        return section(render(role, activeMenu), "shell");
    }

    private String legacy(String role, String activeMenu) throws Exception {
        return section(render(role, activeMenu), "legacy");
    }

    private String render(String role, String activeMenu) throws Exception {
        return mvc.perform(get("/test/sidebar-probe").param("role", role).param("menu", activeMenu))
                .andReturn().getResponse().getContentAsString();
    }

    private static String section(String html, String id) {
        int start = html.indexOf("<div id=\"" + id + "\">");
        int end = id.equals("shell") ? html.indexOf("<div id=\"legacy\">") : html.length();
        return html.substring(start, end);
    }

    @Controller
    static class ProbeController {
        @GetMapping("/test/sidebar-probe")
        String probe(@RequestParam String role, @RequestParam String menu, Model model) {
            model.addAttribute("probeRole", role);
            model.addAttribute("probeMenu", menu);
            return "test/sidebar-probe";
        }
    }
}
