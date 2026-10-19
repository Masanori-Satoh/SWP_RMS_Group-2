package com.group2.rms.jobposting.service;

import org.springframework.data.domain.Sort;

/**
 * Các cách sắp xếp màn danh sách tin tuyển dụng (whitelist: giá trị lạ trên URL quay về NEWEST).
 */
public enum JobPostingSort {
    NEWEST("newest", "Mới đăng nhất",
            Sort.by(Sort.Order.desc("postingDate"), Sort.Order.desc("createdAt"), Sort.Order.desc("jobPostingId"))),
    DEADLINE("deadline", "Hạn nộp gần nhất",
            Sort.by(Sort.Order.asc("applicationDeadline"), Sort.Order.desc("jobPostingId"))),
    // Không sắp được bằng cột của JobPosting: toSort() trả unsorted, service dùng JobPostingSpecifications#orderByApplicationCount
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

    public String param() {
        return param;
    }

    public String label() {
        return label;
    }

    public Sort toSort() {
        return sort;
    }

    /** Đổi giá trị trên URL (?sort=...) sang enum; giá trị lạ hoặc trống thì dùng NEWEST. */
    public static JobPostingSort fromParam(String value) {
        for (JobPostingSort option : values()) {
            if (option.param.equals(value)) {
                return option;
            }
        }
        return NEWEST;
    }
}
