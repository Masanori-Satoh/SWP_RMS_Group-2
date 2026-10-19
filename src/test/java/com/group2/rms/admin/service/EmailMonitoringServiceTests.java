package com.group2.rms.admin.service;

import com.group2.rms.admin.exception.MonitoringSessionRequiredException;
import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmailMonitoringServiceTests {
    @SuppressWarnings("unchecked")
    private final ObjectProvider<JavaMailSender> senders = mock(ObjectProvider.class);
    private JavaMailSenderImpl sender;
    private EmailMonitoringService monitoring;

    @BeforeEach
    void configuredMail() {
        sender = spy(new JavaMailSenderImpl());
        sender.setHost("smtp.example.invalid");
        sender.setPort(587);
        sender.setUsername("test-user");
        sender.setPassword("test-password");
        sender.getJavaMailProperties().setProperty("mail.smtp.auth", "true");
        sender.getJavaMailProperties().setProperty("mail.smtp.starttls.required", "true");
        sender.getJavaMailProperties().setProperty("mail.smtp.connectiontimeout", "12345");
        when(senders.getIfAvailable()).thenReturn(sender);
        monitoring = spy(new EmailMonitoringService(senders, "monitor@example.invalid"));
    }

    @Test
    void configuredServiceStaysUncheckedWithoutAnyNetworkOrMessageOperation() throws Exception {
        var row = monitoring.row();
        assertEquals("Not check", row.status());
        assertNull(row.responseTimeMs());
        assertNull(row.lastCheckedAt());
        monitoring.row();
        verify(sender, never()).testConnection();
        verify(sender, never()).createMimeMessage();
    }

    @Test
    void explicitProbeUsesAnIsolatedBoundedConnectionAndNeverSendsAMessage() throws Exception {
        var probe = mock(JavaMailSenderImpl.class);
        doReturn(probe).when(monitoring).connectionProbe(sender);
        var outcome = monitoring.probe();
        assertTrue(outcome.success());
        assertTrue(outcome.elapsedMs() >= 0);
        assertEquals("OPERATIONAL", monitoring.row().status());
        assertNotNull(monitoring.row().lastCheckedAt());
        assertNotNull(monitoring.row().responseTimeMs());
        verify(probe).testConnection();
        // No send/createMimeMessage calls, including on the real delivery sender.
        verifyNoMoreInteractions(probe);
        verify(sender, never()).testConnection();
        verify(sender, never()).createMimeMessage();
    }

    @Test
    void probeCopiesTlsAndCredentialsWithoutChangingDeliveryTimeouts() {
        var probe = monitoring.connectionProbe(sender);
        var properties = probe.getJavaMailProperties();
        assertEquals("5000", properties.getProperty("mail.smtp.connectiontimeout"));
        assertEquals("5000", properties.getProperty("mail.smtp.timeout"));
        assertEquals("5000", properties.getProperty("mail.smtp.writetimeout"));
        assertEquals("true", properties.getProperty("mail.smtp.auth"));
        assertEquals("true", properties.getProperty("mail.smtp.starttls.required"));
        assertEquals("false", properties.getProperty("mail.debug"));
        assertEquals("12345", sender.getJavaMailProperties().getProperty("mail.smtp.connectiontimeout"));
        assertEquals(sender.getHost(), probe.getHost());
        assertEquals(587, probe.getPort());
        assertEquals("smtp", probe.getProtocol());
        assertEquals(sender.getUsername(), probe.getUsername());
        assertEquals(sender.getPassword(), probe.getPassword());
    }

    @ParameterizedTest
    @MethodSource("connectionFailures")
    void recordsFailureWithoutExposingProviderDetails(Exception failure) throws Exception {
        var probe = mock(JavaMailSenderImpl.class);
        doThrow(failure).when(probe).testConnection();
        doReturn(probe).when(monitoring).connectionProbe(sender);
        var outcome = monitoring.probe();
        assertFalse(outcome.success());
        assertFalse(outcome.detail().contains("private-provider-detail"));
        assertEquals("FAILED", monitoring.row().status());
        assertNotNull(monitoring.row().responseTimeMs());
        assertNotNull(monitoring.row().lastCheckedAt());
    }

    static Stream<Exception> connectionFailures() {
        return Stream.of(new AuthenticationFailedException("private-provider-detail"),
            new MessagingException("private-provider-detail"),
            new IllegalStateException("private-provider-detail"));
    }

    @Test
    void manualRetryCanRecoverFromFailedToHealthy() throws Exception {
        var probe = mock(JavaMailSenderImpl.class);
        doThrow(new MessagingException()).doNothing().when(probe).testConnection();
        doReturn(probe).when(monitoring).connectionProbe(sender);
        assertFalse(monitoring.probe().success());
        assertEquals("FAILED", monitoring.row().status());
        assertTrue(monitoring.probe().success());
        assertEquals("OPERATIONAL", monitoring.row().status());
        verify(probe, times(2)).testConnection();
    }

    @Test
    void missingConfigurationDoesNotBecomeAFailedConnectionAttempt() {
        when(senders.getIfAvailable()).thenReturn(null);
        assertEquals("Unconfigured", monitoring.row().status());
        assertFalse(monitoring.probe().success());
        assertEquals("Unconfigured", monitoring.row().status());
        assertNull(monitoring.row().lastCheckedAt());
        verify(monitoring, never()).connectionProbe(any());
    }

    @Test
    void missingAuthCredentialIsUnconfigured() {
        sender.setPassword("");
        assertEquals("Unconfigured", monitoring.row().status());
    }

    @Test
    void invalidFromAddressOrMissingHostIsUnconfigured() {
        assertEquals("Unconfigured", new EmailMonitoringService(senders, "invalid-from").row().status());
        sender.setHost("");
        assertEquals("Unconfigured", monitoring.row().status());
    }

    @Test
    void readRowsDoNotProbeAndAiRemainsUnconfigured() {
        var transport = mock(HttpProbeTransport.class);
        var service = new ApiMonitoringService(transport, monitoring);
        assertEquals("Not check", service.rows().get(2).status());
        assertEquals("Unconfigured", service.rows().get(1).status());
        verifyNoInteractions(transport);
    }

    @Test
    void missingSessionCannotStartEmailProbe() {
        var email = mock(EmailMonitoringService.class);
        var service = new ApiMonitoringService(mock(HttpProbeTransport.class), email);
        assertThrows(MonitoringSessionRequiredException.class,
            () -> service.probeEmail(mock(HttpServletRequest.class)));
        verifyNoInteractions(email);
    }
}
