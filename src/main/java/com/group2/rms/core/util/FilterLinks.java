package com.group2.rms.core.util;

import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

/**
 * Tạo link "bỏ một bộ lọc" cho các chip ở màn danh sách, giữ nguyên các tham số khác đang có trên URL.
 * Bỏ bộ lọc thì quay về trang đầu (xóa luôn tham số page).
 *
 * Controller đưa vào model: {@code model.addAttribute("filterLinks", new FilterLinks(uri, query))};
 * template dùng: {@code th:href="${filterLinks.without(chip.params)}"}.
 */
public class FilterLinks {

    private final String path;
    private final String query;

    public FilterLinks(String path, String query) {
        this.path = path;
        this.query = query;
    }

    public String without(List<String> params) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(path).query(query);
        builder.replaceQueryParam("page");
        for (String param : params) {
            builder.replaceQueryParam(param);
        }
        return builder.build().toUriString();
    }
}
