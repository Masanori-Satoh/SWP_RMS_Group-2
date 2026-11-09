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


    // Returns the URL parameter for this sort option.
    public String param() {
        return param;
    }


    // Returns the display label for this sort option.
    public String label() {
        return label;
    }

    // Returns the database sort for this option.
    public Sort toSort() {
        return sort;
    }


    // Resolves a URL parameter to a sort option.
    public static RequisitionSort fromParam(String value) {
        for (RequisitionSort option : values()) {
            if (option.param.equalsIgnoreCase(value)) {
                return option;
            }
        }
        return NEWEST;
    }
}
