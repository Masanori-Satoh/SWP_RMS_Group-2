package com.group2.rms.admin.exception;

import com.group2.rms.core.exception.BaseBusinessException;

public class MonitoringSessionRequiredException extends BaseBusinessException {
    public MonitoringSessionRequiredException() {
        super("Authenticated session is required.", "MONITORING_SESSION_REQUIRED");
    }
}
