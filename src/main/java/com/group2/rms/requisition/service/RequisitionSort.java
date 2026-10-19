package com.group2.rms.requisition.service;

import org.springframework.data.domain.Sort;

/**
 * Danh sách các cách sắp xếp được phép ở màn danh sách yêu cầu (whitelist).
 * Giá trị lạ trên URL luôn quay về mặc định NEWEST, không bao giờ dùng thẳng tên cột từ client.
 *
 * Mỗi cách sắp xếp có thêm requisitionId làm tiêu chí phụ,
 * để hai dòng trùng giá trị vẫn có thứ tự ổn định giữa các trang.
 */
public enum RequisitionSort {

    NEWEST("newest", "Mới nhất trước",
            Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("requisitionId"))),
    OLDEST("oldest", "Cũ nhất trước",
            Sort.by(Sort.Order.asc("createdAt"), Sort.Order.asc("requisitionId"))),
    TITLE("title", "Tên vị trí A–Z",
            Sort.by(Sort.Order.asc("title"), Sort.Order.desc("requisitionId")));

    private final String param;
    private final String label;
    private final Sort sort;

    RequisitionSort(String param, String label, Sort sort) {
        this.param = param;
        this.label = label;
        this.sort = sort;
    }

    /** Giá trị xuất hiện trên URL, ví dụ ?sort=oldest */
    public String param() {
        return param;
    }

    /** Chữ hiển thị trong ô "Sắp xếp theo" */
    public String label() {
        return label;
    }

    public Sort toSort() {
        return sort;
    }

    /** Đổi giá trị trên URL thành enum; giá trị lạ hoặc trống thì dùng NEWEST. */
    public static RequisitionSort fromParam(String value) {
        for (RequisitionSort option : values()) {
            if (option.param.equalsIgnoreCase(value)) {
                return option;
            }
        }
        return NEWEST;
    }
}
