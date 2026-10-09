package com.group2.rms.core.web;

import org.springframework.web.util.UriComponentsBuilder;

/**
 * Tạo link phân trang giữ nguyên mọi tham số đang có trên URL (từ khóa, bộ lọc...), chỉ thay {@code page}.
 * Có sẵn ở mọi view qua model attribute {@code pageLinks} (xem {@link ViewHelpersAdvice}).
 * Dùng trong fragment {@code fragments/ui/pagination :: paged(page)}: {@code th:href="${pageLinks.to(2)}"}.
 */
public class PageLinks {

    private final String path;
    private final String query;

    public PageLinks(String path, String query) {
        this.path = path;
        this.query = query;
    }

    public String to(int page) {
        return UriComponentsBuilder.fromPath(path)
                .query(query)
                .replaceQueryParam("page", page)
                .build()
                .toUriString();
    }
}
