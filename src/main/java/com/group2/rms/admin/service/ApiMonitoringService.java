package com.group2.rms.admin.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;
import com.group2.rms.admin.dto.MonitorRowResponse;
import com.group2.rms.admin.dto.ProbeOutcomeResponse;
import com.group2.rms.admin.exception.MonitoringSessionRequiredException;

import java.io.IOException;
import java.net.URI;
import java.time.LocalDateTime;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class ApiMonitoringService {

    private static final String INTERNAL_HEALTH_PATH = "/admin/api-monitoring/internal/health";
    // send request
    private final HttpProbeTransport transport;

    private final AtomicReference<MonitorRowResponse> lastInternalProbe = new AtomicReference<>();

    public ApiMonitoringService(HttpProbeTransport transport) {
        this.transport = transport;
    }

    public ProbeOutcomeResponse probeInternal(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        // check if session not valid
        if (session == null) {
            throw new MonitoringSessionRequiredException();
        }
        String contextPath = request.getContextPath();
        // check context path not
        if (contextPath == null || "/".equals(contextPath)) {
            contextPath = "";
        }
        URI target = URI.create("http://127.0.0.1:" + request.getLocalPort()
                + contextPath + INTERNAL_HEALTH_PATH);
        // time check by nano
        long started = System.nanoTime();

        boolean success = false;
        String detail;
        try {
            int httpStatus = transport.get(target, session.getId());
            success = httpStatus >= 200 && httpStatus < 300;
            detail = "HTTP " + httpStatus;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            detail = "The request was interrupted";
        } catch (IOException exception) {
            detail = "No HTTP response was received";
        }

        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);

        lastInternalProbe.set(new MonitorRowResponse(
                "internal",
                "Application and SQL Server",
                "Internal",
                success ? "OPERATIONAL" : "FAILED",
                elapsedMs,
                LocalDateTime.now()));
        return new ProbeOutcomeResponse(success, detail, elapsedMs);
    }

    @SuppressWarnings("null")
    public List<MonitorRowResponse> rows() {
        MonitorRowResponse internal = lastInternalProbe.get();

        // check if first time check
        if (internal == null) {
            internal = new MonitorRowResponse(
                    "internal",
                    "Application and SQL Server",
                    "Internal",
                    "Not check",
                    null,
                    null);
        }

        return List.of(internal,
                new MonitorRowResponse(
                        "ai",
                        "AI CV Screening",
                        "External Intergration",
                        "Unconfigured",
                        null,
                        null),
                new MonitorRowResponse(
                        "email",
                        "Email Service",
                        "External Integration",
                        "Unconfigured",
                        null,
                        null));
    }


}
