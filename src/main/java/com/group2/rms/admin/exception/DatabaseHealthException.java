package com.group2.rms.admin.exception;
import com.group2.rms.core.exception.BaseBusinessException;
public class DatabaseHealthException extends BaseBusinessException {

    public DatabaseHealthException() {
        super("Database health check failed.", "DB_HEALTH_DOWN");
    }

}
