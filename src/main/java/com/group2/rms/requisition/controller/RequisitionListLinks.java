package com.group2.rms.requisition.controller;

import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

/**
 * Tạo link "bỏ một bộ lọc" cho các chip ở màn danh sách yêu cầu tuyển dụng.
 * Cách làm giống core/web/PageLinks: giữ nguyên mọi tham số đang có trên URL, chỉ bỏ những tham số được chỉ định.
 *
 * Ví dụ URL hiện tại: /requisitions?q=java&departmentId=3&page=2
 *   without(List.of("departmentId"))  →  /requisitions?q=java
 * (luôn bỏ "page" để quay về trang đầu, vì bộ lọc đổi thì số trang cũ không còn đúng)
 */
public class RequisitionListLinks {

    private final String path;
    private final String query;

    public RequisitionListLinks(String path, String query) {
        this.path = path;
        this.query = query;
    }

    public String without(List<String> params) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(path).query(query);
        builder.replaceQueryParam("page");
        for (String param : params) {
            builder.replaceQueryParam(param);
        }
        // Bỏ chip "Trạng thái" thì vẫn gửi status rỗng (?status=).
        // Nếu bỏ hẳn tham số, service sẽ hiểu là "lần vào đầu" và Director lại bị tự lọc "Chờ duyệt".
        if (params.contains("status")) {
            builder.queryParam("status", "");
        }
        return builder.build().toUriString();
    }
}
