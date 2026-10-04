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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** Admin-initiated probes. Statistics use only real results from this process. */
@Service
public class ApiMonitoringService {

    private static final int HISTORY_LIMIT = 100;
    private static final String INTERNAL_HEALTH_PATH = "/admin/api-monitoring/internal/health";

    private final HttpProbeTransport transport;
    private final Deque<Probe> internalHistory = new ArrayDeque<>();

    public ApiMonitoringService(HttpProbeTransport transport) {
        this.transport = transport;
    }

    public ProbeOutcomeResponse probeInternal(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            throw new MonitoringSessionRequiredException();
        }
        String contextPath = request.getContextPath();
        if (contextPath == null || "/".equals(contextPath)) {
            contextPath = "";
        }
        URI target = URI.create("http://127.0.0.1:" + request.getLocalPort()
                + contextPath + INTERNAL_HEALTH_PATH);

        long started = System.nanoTime();
        Integer httpStatus = null;
        boolean success = false;
        String detail;
        try {
            httpStatus = transport.get(target, session.getId());
            success = httpStatus >= 200 && httpStatus < 300;
            detail = "HTTP " + httpStatus;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            detail = "The request was interrupted";
        } catch (IOException exception) {
            detail = "No HTTP response was received";
        }

        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
        Probe probe = new Probe(success, httpStatus, elapsedMs, LocalDateTime.now(), detail);
        synchronized (internalHistory) {
            internalHistory.addLast(probe);
            while (internalHistory.size() > HISTORY_LIMIT) {
                internalHistory.removeFirst();
            }
        }
        return new ProbeOutcomeResponse(success, detail, elapsedMs);
    }

    public List<MonitorRowResponse> rows() {
        List<Probe> samples;
        synchronized (internalHistory) {
            samples = new ArrayList<>(internalHistory);
        }
        MonitorRowResponse internal;
        if (samples.isEmpty()) {
            internal = new MonitorRowResponse("internal", "Internal", "Application and SQL Server",
                    "GET " + INTERNAL_HEALTH_PATH, "NOT_CHECKED", "Not Checked",
                    null, null, null, null, null, 0, true);
        } else {
            Probe latest = samples.getLast();
            int errors = (int) samples.stream().filter(sample -> !sample.success()).count();
            long averageMs = Math.round(samples.stream().mapToLong(Probe::elapsedMs).average().orElse(0));
            int errorRate = (int) Math.round(100.0 * errors / samples.size());
            internal = new MonitorRowResponse("internal", "Internal", "Application and SQL Server",
                    "GET " + INTERNAL_HEALTH_PATH,
                    latest.success() ? "OPERATIONAL" : "FAILED",
                    latest.success() ? "Operational" : "Error",
                    averageMs, errorRate, errors, latest.httpStatus(), latest.checkedAt(),
                    samples.size(), true);
        }
        return List.of(internal,
                new MonitorRowResponse("ai", "External Integration", "AI CV Screening", "No endpoint configured",
                        "UNCONFIGURED", "Not Configured", null, null, null, null, null, 0, false),
                new MonitorRowResponse("email", "External Integration", "Email Service", "No endpoint or SMTP host configured",
                        "UNCONFIGURED", "Not Configured", null, null, null, null, null, 0, false));
    }

    private record Probe(boolean success, Integer httpStatus, long elapsedMs,
                         LocalDateTime checkedAt, String detail) { }

   

    
}
