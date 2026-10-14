package com.group2.rms.core.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.StringTemplateResolver;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Fragment dùng chung {@code fragments/ui/stages :: stageBar(items, mode)} ở mode {@code stepper}, không cần khởi động
 * Spring. Mode {@code tabs} (có link) được kiểm ở {@code ApplicationPipelineWebTests}.
 */
class StageBarFragmentTests {

    private static final String CALLER = "<div><nav th:replace=\"~{fragments/ui/stages :: stageBar(items=${items}, mode=${mode})}\"></nav></div>";

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
        engine = new SpringTemplateEngine();
        engine.addTemplateResolver(files);
        engine.addTemplateResolver(inline);
    }

    @Test
    void stepperMarksDoneAndCurrentStepAndShowsDetailOnlyWhereGiven() {
        String html = render("stepper", List.of(
                new StageItem("APPLIED", "Ứng tuyển", null, null, "done", null, false),
                new StageItem("INTERVIEW", "Phỏng vấn", null, null, "current",
                        "Chờ xếp lịch phỏng vấn · Đang chờ: HR", false),
                new StageItem("OFFER", "Offer", null, null, "upcoming", null, false)));

        assertTrue(html.contains("stage-bar--stepper"));
        assertTrue(html.contains("aria-label=\"Tiến độ hồ sơ\""));
        assertEquals(3, count(html, "class=\"stage-bar__item\""));
        assertTrue(html.contains("data-state=\"done\""));
        assertTrue(html.contains("data-state=\"upcoming\""));
        assertEquals(1, count(html, "aria-current=\"step\""), "chỉ bước hiện tại có aria-current");
        assertEquals(1, count(html, "class=\"stage-bar__detail\""));
        assertTrue(html.contains(">Chờ xếp lịch phỏng vấn · Đang chờ: HR<"));
        assertFalse(html.contains("<a "), "bước không bấm được");
    }

    @Test
    void stepperKeepsClosedKeyForStyling() {
        String html = render("stepper", List.of(
                new StageItem("APPLIED", "Ứng tuyển", null, null, "done", null, false),
                new StageItem("CLOSED", "Đã dừng", null, null, "current", "Không đạt phỏng vấn", false)));

        assertTrue(html.contains("data-key=\"CLOSED\""));
        assertTrue(html.contains(">Không đạt phỏng vấn<"));
    }

    @Test
    void tabsKeepTheirOwnLabelAndDoNotRenderStepperMarkup() {
        String html = render("tabs", List.of(
                new StageItem("ALL", "Tất cả", 4, null, "current", null, true)));

        assertTrue(html.contains("aria-label=\"Lọc theo giai đoạn\""));
        assertTrue(html.contains("stage-bar--tabs"));
        assertFalse(html.contains("stage-bar__step"));
        assertTrue(html.contains(">4<"));
    }

    private String render(String mode, List<StageItem> items) {
        return engine.process(CALLER, new Context(null, Map.of("items", items, "mode", mode)));
    }

    private static int count(String text, String part) {
        return text.split(java.util.regex.Pattern.quote(part), -1).length - 1;
    }
}
