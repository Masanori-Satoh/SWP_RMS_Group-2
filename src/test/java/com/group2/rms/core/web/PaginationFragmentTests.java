package com.group2.rms.core.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.StringTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class PaginationFragmentTests {
    private SpringTemplateEngine engine;

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

    @ParameterizedTest
    @ValueSource(strings = {
            "fragments/ui/pagination :: paged(page=${page})"
    })
    void outOfRangePageOnlyRendersLinksToExistingPages(String fragment) {
        for (int pageNumber : List.of(9999, Integer.MAX_VALUE)) {
            var page = new PageImpl<>(List.of(), PageRequest.of(pageNumber, 20), 54);
            String html = render(fragment, page);

            assertEquals(5, countItems(html));
            assertTrue(html.contains("Trang yêu cầu không tồn tại."));
            assertFalse(html.contains("aria-current=\"page\""));
            assertFalse(html.contains("aria-label=\"Trang sau\""));
            var links = Pattern.compile("(?:\\?|&amp;)page=(\\d+)\"").matcher(html).results()
                    .map(match -> Integer.parseInt(match.group(1))).toList();
            assertEquals(List.of(2, 0, 1, 2), links);
        }
    }

    @Test
    void validPageKeepsFirstLastAndFilteredNavigation() {
        var page = new PageImpl<>(List.of(), PageRequest.of(7, 20), 400);
        String html = render("fragments/ui/pagination :: paged(page=${page})", page);

        assertEquals(11, countItems(html));
        assertTrue(html.contains("aria-current=\"page\">8</span>"));
        assertTrue(html.contains("keyword=Java&amp;markedBy=5&amp;page=0"));
        assertTrue(html.contains("keyword=Java&amp;markedBy=5&amp;page=19"));
    }

    @Test
    void emptyPageHidesNavigation() {
        String html = render("fragments/ui/pagination :: paged(page=${page})",
                Page.empty(PageRequest.of(9999, 20)));

        assertEquals(0, countItems(html));
        assertFalse(html.contains("pagination-wrapper"));
    }

    private String render(String fragment, Page<?> page) {
        var request = new MockHttpServletRequest("GET", "/talent-pool");
        var web = JakartaServletWebApplication.buildApplication(request.getServletContext());
        var exchange = web.buildExchange(request, new MockHttpServletResponse());
        var links = new PageLinks("/talent-pool", "keyword=Java&markedBy=5&page=" + page.getNumber());
        return engine.process("<nav th:replace=\"~{" + fragment + "}\"></nav>",
                new WebContext(exchange, null, Map.of("page", page, "pageLinks", links)));
    }

    private long countItems(String html) {
        return Pattern.compile("class=\"page-item\"").matcher(html).results().count();
    }
}
