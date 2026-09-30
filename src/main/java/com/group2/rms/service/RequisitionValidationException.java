package com.group2.rms.service;
import java.util.Map;
public class RequisitionValidationException extends RuntimeException {
    private final Map<String,String> errors;
    public RequisitionValidationException(Map<String,String> errors) { super(String.join(" ", errors.values())); this.errors=errors; }
    public Map<String,String> getErrors() { return errors; }
}
