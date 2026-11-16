package com.group2.rms.jobposting.service;

import org.springframework.data.domain.Sort;

/*
 * SORT: whitelist cách sắp xếp màn danh sách tin; giá trị lạ trên URL quay về NEWEST.
 */
public enum JobPostingSort {
    NEWEST("newest", "Mới đăng nhất",
            Sort.by(Sort.Order.desc("postingDate"), Sort.Order.desc("createdAt"), Sort.Order.desc("jobPostingId"))),
    DEADLINE("deadline", "Hạn ứng tuyển gần nhất",
            Sort.by(Sort.Order.asc("applicationDeadline"), Sort.Order.desc("jobPostingId"))),
    MOST_APPLICATIONS("apps", "Nhiều hồ sơ nhất", Sort.unsorted()),
    TITLE("title", "Tiêu đề A–Z",
            Sort.by(Sort.Order.asc("postingTitle"), Sort.Order.desc("jobPostingId")));

    private final String param;
    private final String label;
    private final Sort sort;

    JobPostingSort(String param, String label, Sort sort) {
        this.param = param;
        this.label = label;
        this.sort = sort;
    }

    // Lấy giá trị sắp xếp trên URL.
    public String param() {
        return param;
    }

    // Lấy nhãn của cách sắp xếp.
    public String label() {
        return label;
    }

    // Chuyển lựa chọn thành thứ tự truy vấn.
    public Sort toSort() {
        return sort;
    }

    // Đọc cách sắp xếp từ URL.
    public static JobPostingSort fromParam(String value) {
        for (JobPostingSort option : values()) {
            if (option.param.equals(value)) {
                return option;
            }
        }
        return NEWEST;
    }
}
