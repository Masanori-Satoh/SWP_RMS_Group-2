package com.group2.rms.career.service;

import com.group2.rms.candidate.domain.entity.Candidate;
import com.group2.rms.candidate.domain.repository.CandidateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/*
 * UNIT TEST (Mockito): PolicyConsentService — popup đồng ý Điều khoản + Chính sách bảo mật.
 * CandidateRepository giả lập; Clock cố định để kiểm tra đúng thời điểm lưu.
 */
@ExtendWith(MockitoExtension.class)
class PolicyConsentServiceTests {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-04T03:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));
    private static final LocalDateTime NOW = LocalDateTime.now(CLOCK);
    private static final String USERNAME = "an.nguyen";

    @Mock
    private CandidateRepository candidates;

    private PolicyConsentService service;

    @BeforeEach
    void setUp() {
        service = new PolicyConsentService(candidates, CLOCK);
    }

    @Test
    @DisplayName("Ứng viên chưa đồng ý -> Cần hiện popup")
    void needsConsent_shouldBeTrue_whenCandidateHasNotAgreed() {
        // Arrange
        when(candidates.findByAccount_UsernameIgnoreCase(USERNAME)).thenReturn(Optional.of(new Candidate()));

        // Act + Assert
        assertTrue(service.needsConsent(USERNAME));
    }

    @Test
    @DisplayName("Ứng viên đã đồng ý -> Không hiện popup")
    void needsConsent_shouldBeFalse_whenCandidateAlreadyAgreed() {
        // Arrange
        Candidate candidate = new Candidate();
        candidate.setPolicyAgreedAt(NOW.minusDays(3));
        when(candidates.findByAccount_UsernameIgnoreCase(USERNAME)).thenReturn(Optional.of(candidate));

        // Act + Assert
        assertFalse(service.needsConsent(USERNAME));
    }

    @Test
    @DisplayName("Tài khoản không có hồ sơ ứng viên -> Không hiện popup")
    void needsConsent_shouldBeFalse_whenNoCandidateProfile() {
        // Arrange
        when(candidates.findByAccount_UsernameIgnoreCase(USERNAME)).thenReturn(Optional.empty());

        // Act + Assert
        assertFalse(service.needsConsent(USERNAME));
    }

    @Test
    @DisplayName("Bấm Tôi đồng ý lần đầu -> Lưu đúng thời điểm theo Clock")
    void recordConsent_shouldSaveNow_whenCandidateHasNotAgreed() {
        // Arrange
        Candidate candidate = new Candidate();
        when(candidates.findByAccount_UsernameIgnoreCase(USERNAME)).thenReturn(Optional.of(candidate));

        // Act
        service.recordConsent(USERNAME);

        // Assert
        assertEquals(NOW, candidate.getPolicyAgreedAt());
        verify(candidates).save(candidate);
    }

    @Test
    @DisplayName("Bấm Tôi đồng ý lần hai (2 tab) -> Giữ thời điểm lần đầu, không lưu lại")
    void recordConsent_shouldKeepFirstTime_whenAlreadyAgreed() {
        // Arrange
        LocalDateTime firstTime = NOW.minusDays(1);
        Candidate candidate = new Candidate();
        candidate.setPolicyAgreedAt(firstTime);
        when(candidates.findByAccount_UsernameIgnoreCase(USERNAME)).thenReturn(Optional.of(candidate));

        // Act
        service.recordConsent(USERNAME);

        // Assert
        assertEquals(firstTime, candidate.getPolicyAgreedAt());
        verify(candidates, never()).save(any());
    }

    @Test
    @DisplayName("Tài khoản không có hồ sơ ứng viên bấm đồng ý -> Không lưu gì")
    void recordConsent_shouldDoNothing_whenNoCandidateProfile() {
        // Arrange
        when(candidates.findByAccount_UsernameIgnoreCase(USERNAME)).thenReturn(Optional.empty());

        // Act
        service.recordConsent(USERNAME);

        // Assert
        verify(candidates, never()).save(any());
    }
}
