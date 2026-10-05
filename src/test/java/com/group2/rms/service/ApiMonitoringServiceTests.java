package com.group2.rms.service;

import com.group2.rms.admin.dto.MonitorRowResponse;
import com.group2.rms.admin.service.ApiMonitoringService;
import com.group2.rms.admin.service.HttpProbeTransport;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApiMonitoringServiceTests {

    @Mock
    private HttpProbeTransport transport;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpSession session;

    private ApiMonitoringService monitoring;
    private URI internalUrl;

    @BeforeEach
    void setUp() {
        monitoring = new ApiMonitoringService(transport);
        internalUrl = URI.create("http://127.0.0.1:8082/rms/admin/api-monitoring/internal/health");
    }

    @Test
    void realResponsesDetermineStatusAndRollingStatistics() throws Exception {
        when(request.getSession(false)).thenReturn(session);
        when(session.getId()).thenReturn("session-for-test");
        when(request.getContextPath()).thenReturn("/rms");
        when(request.getLocalPort()).thenReturn(8082);

        var initial = monitoring.rows();
        assertEquals("NOT_CHECKED", initial.getFirst().status());
        assertNull(initial.getFirst().averageResponseMs());
        assertEquals("UNCONFIGURED", initial.get(1).status());
        assertEquals("UNCONFIGURED", initial.get(2).status());

        when(transport.get(internalUrl, "session-for-test")).thenReturn(200, 503);
        assertTrue(monitoring.probeInternal(request).success());
        assertFalse(monitoring.probeInternal(request).success());
        verify(transport, org.mockito.Mockito.times(2)).get(internalUrl, "session-for-test");

        var internal = monitoring.rows().getFirst();
        assertEquals("FAILED", internal.status());
        assertEquals(503, internal.lastHttpStatus());
        assertEquals(2, internal.sampleCount());
        assertEquals(50, internal.errorRatePercent());
        assertEquals(1, internal.recentErrors());
        assertTrue(internal.averageResponseMs() >= 0);
        assertTrue(monitoring.rows().stream().skip(1).noneMatch(MonitorRowResponse::canProbe));
    }

    @Test
    void connectionFailureIsRecordedWithoutExposingExceptionMessage() throws Exception {
        when(request.getSession(false)).thenReturn(session);
        when(session.getId()).thenReturn("session-for-test");
        when(request.getContextPath()).thenReturn("");
        when(request.getLocalPort()).thenReturn(8082);
        when(transport.get(URI.create("http://127.0.0.1:8082/admin/api-monitoring/internal/health"),
                "session-for-test")).thenThrow(new IOException("private network details"));

        var outcome = monitoring.probeInternal(request);
        assertFalse(outcome.success());
        assertEquals("No HTTP response was received", outcome.detail());
        assertNull(monitoring.rows().getFirst().lastHttpStatus());
        assertEquals(100, monitoring.rows().getFirst().errorRatePercent());
    }

    @Test
    void unauthenticatedRequestCannotStartProbe() {
        when(request.getSession(false)).thenReturn(null);
        assertThrows(IllegalStateException.class, () -> monitoring.probeInternal(request));
        verifyNoInteractions(transport);
    }
}
