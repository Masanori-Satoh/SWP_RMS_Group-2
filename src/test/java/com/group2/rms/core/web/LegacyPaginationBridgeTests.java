package com.group2.rms.core.web;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Trang cũ (interfaceHead) không nạp components.css nên interface.css giữ bản sao style phân trang.
 * Test giữ 2 bản khớp nhau: sửa mục L của components.css mà quên bản sao thì test này báo.
 */
class LegacyPaginationBridgeTests {

    private static final String COMPONENTS = css("static/css/components.css");
    private static final String INTERFACE = css("static/css/interface.css");

    @ParameterizedTest
    @ValueSource(strings = {
            ".pagination-wrapper", ".pagination-info", ".pagination-controls", ".pagination-list", ".page-item",
            ".page-link", ".page-link:hover:not(.disabled):not(.active)", ".page-link.active", ".page-link.disabled"})
    void legacyStylesheetMatchesCatalogPagination(String selector) {
        Set<String> catalog = declarations(COMPONENTS, selector);
        assertFalse(catalog.isEmpty(), "components.css không còn " + selector);
        assertEquals(catalog, declarations(INTERFACE, selector), selector);
    }

    /** Khai báo của khối CSS có {@code selector} nằm trong danh sách selector của nó. */
    private static Set<String> declarations(String css, String selector) {
        Matcher block = Pattern.compile("([^{}]+)\\{([^{}]*)}").matcher(css.replaceAll("(?s)/\\*.*?\\*/", "")
                .replace("\r", ""));
        while (block.find()) {
            boolean matches = Arrays.stream(block.group(1).split(","))
                    .map(String::strip).anyMatch(selector::equals);
            if (matches) {
                return Arrays.stream(block.group(2).split(";")).map(String::strip).filter(s -> !s.isEmpty())
                        .map(s -> s.replaceAll("\\s+", " ")).collect(Collectors.toCollection(TreeSet::new));
            }
        }
        return Set.of();
    }

    private static String css(String path) {
        try (InputStream in = LegacyPaginationBridgeTests.class.getClassLoader().getResourceAsStream(path)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException | NullPointerException e) {
            throw new IllegalStateException("Không đọc được " + path, e);
        }
    }
}
