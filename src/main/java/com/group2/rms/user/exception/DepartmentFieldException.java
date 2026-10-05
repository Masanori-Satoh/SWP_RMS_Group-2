package com.group2.rms.user.exception;

import com.group2.rms.core.exception.BaseBusinessException;
import com.group2.rms.user.dto.DepartmentRequest;

/** The global handler retains the submitted form when a recoverable business check fails. */
public class DepartmentFieldException extends BaseBusinessException {
    private final String field;
    private final Integer departmentId;
    private final DepartmentRequest form;

    public DepartmentFieldException(String field, String message, Integer departmentId, DepartmentRequest form) {
        super(message, "DEPARTMENT_FIELD_INVALID");
        this.field = field;
        this.departmentId = departmentId;
        this.form = form;
    }
    public String getField() { return field; }
    public Integer getDepartmentId() { return departmentId; }
    public DepartmentRequest getForm() { return form; }
}
