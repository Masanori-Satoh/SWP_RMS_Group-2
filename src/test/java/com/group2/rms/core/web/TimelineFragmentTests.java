package com.group2.rms.core.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.StringTemplateResolver;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Fragment dùng chung {@code fragments/ui/timeline :: timeline(items)} render đúng, không cần khởi động Spring.
 */
class TimelineFragmentTests {

    private static final String CALLER = "<div><th:block th:replace=\"~{fragments/ui/timeline :: timeline(items=${items})}\"></th:block></div>";

    private TemplateEngine engine;

    @BeforeEach
    void setUp() {
        ClassLoaderTemplateResolver files = new ClassLoaderTemplateResolver();
        files.setPrefix("templates/");
        files.setSuffix(".html");
        files.setCheckExistence(true);
        files.setOrder(1);
        StringTemplateResolver inline = new StringTemplateResolver();
        inline.setOrder(2);
        engine = new SpringTemplateEngine();   // như app: biểu thức SpEL
        engine.addTemplateResolver(files);
        engine.addTemplateResolver(inline);
    }

    @Test
    void rendersEachItemWithToneTimeActorAndBody() {
        String html = render(List.of(
                new TimelineItem(LocalDateTime.of(2026, 10, 11, 9, 5), "HR: Đạt", "Lan HR", "Hợp vị trí", "success"),
                new TimelineItem(LocalDateTime.of(2026, 10, 10, 14, 59), "Nộp hồ sơ", "An Võ", null, null)));

        assertEquals(2, count(html, "class=\"timeline__item"));
        assertTrue(html.contains("timeline__item--success"));
        assertTrue(html.contains("timeline__item--neutral"), "tone null thì dùng neutral");
        assertTrue(html.contains("datetime=\"2026-10-11T09:05:00"));
        assertTrue(html.contains(">11/10/2026 09:05<"));
        assertTrue(html.contains(">Lan HR<"));
        assertEquals(1, count(html, "class=\"timeline__body\""), "chỉ mốc có nội dung mới có body");
        assertTrue(html.contains(">Hợp vị trí<"));
        assertFalse(html.contains("timeline__empty"));
    }

    @Test
    void escapesUserText() {
        String html = render(List.of(new TimelineItem(LocalDateTime.of(2026, 10, 10, 8, 0), "HR: Không đạt",
                "Lan HR", "<script>alert(1)</script>", "danger")));

        assertFalse(html.contains("<script>"));
        assertTrue(html.contains("&lt;script&gt;"));
    }

    @Test
    void emptyListShowsEmptyState() {
        String html = render(List.of());

        assertTrue(html.contains("Chưa có hoạt động"));
        assertFalse(html.contains("<ol"));
    }

    private String render(List<TimelineItem> items) {
        return engine.process(CALLER, new Context(null, Map.of("items", items)));
    }

    private static int count(String text, String part) {
        return text.split(java.util.regex.Pattern.quote(part), -1).length - 1;
    }
}
