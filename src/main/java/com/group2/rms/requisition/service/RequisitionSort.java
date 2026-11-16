package com.group2.rms.requisition.service;

import org.springframework.data.domain.Sort;


public enum RequisitionSort {

    NEWEST("newest", "Mới nhất trước",
            Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("requisitionId"))),
    OLDEST("oldest", "Cũ nhất trước",
            Sort.by(Sort.Order.asc("createdAt"), Sort.Order.asc("requisitionId")));

    private final String param;
    private final String label;
    private final Sort sort;

    RequisitionSort(String param, String label, Sort sort) {
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
    public static RequisitionSort fromParam(String value) {
        for (RequisitionSort option : values()) {
            if (option.param.equalsIgnoreCase(value)) {
                return option;
            }
        }
        return NEWEST;
    }
}
