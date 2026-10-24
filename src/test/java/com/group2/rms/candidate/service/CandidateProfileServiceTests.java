package com.group2.rms.candidate.service;

import com.group2.rms.candidate.dto.CandidateDetailsRequest;
import com.group2.rms.candidate.entity.Candidate;
import com.group2.rms.candidate.exception.CandidateProfileException;
import com.group2.rms.candidate.repository.CandidateRepository;
import com.group2.rms.core.exception.ResourceNotFoundException;
import com.group2.rms.user.dto.UpdateProfileRequest;
import com.group2.rms.user.dto.UserProfileResponse;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.service.UserProfileService;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CandidateProfileServiceTests {

    private final CandidateRepository candidates = mock(CandidateRepository.class);
    private final UserProfileService userProfiles = mock(UserProfileService.class);
    private final CandidateProfileService service = new CandidateProfileService(candidates, userProfiles);
    private final Candidate candidate = Candidate.builder().candidateId(9).account(User.builder().userId(54).build())
            .dateOfBirth(LocalDate.of(1998, 1, 1)).gender("Nam").address("Cũ").linkedInUrl("https://old.test")
            .portfolioUrl("https://old-portfolio.test").candidateSource("TopCV").isPotential(true).build();

    @Test
    void getCombinesAccountPartAndCandidateFields() {
        when(candidates.findByAccount_UsernameIgnoreCase("an.vo")).thenReturn(Optional.of(candidate));
        UserProfileResponse account = new UserProfileResponse(54, "an.vo", "a@x.test", "Võ Thành An", null, null, "Candidate", "Ứng viên", "Chưa thuộc phòng ban");
        when(userProfiles.getProfile("an.vo")).thenReturn(account);

        var result = service.get("an.vo");

        assertSame(account, result.account());
        assertEquals(LocalDate.of(1998, 1, 1), result.dateOfBirth());
        assertEquals("Nam", result.gender());
        assertEquals("https://old-portfolio.test", result.portfolioUrl());
    }

    @Test
    void updateWritesUserPartThroughSharedServiceAndTrimsCandidateFields() {
        when(candidates.findByAccount_UsernameIgnoreCase("an.vo")).thenReturn(Optional.of(candidate));
        UpdateProfileRequest account = new UpdateProfileRequest("Võ Thành An", "0901234567");

        service.update("an.vo", account, new CandidateDetailsRequest(
                LocalDate.of(1999, 4, 12), "Nữ", "  Số 1 Lê Lợi  ", " https://linkedin.com/in/an ", "   "));

        verify(userProfiles).updateProfile("an.vo", account);
        assertEquals(LocalDate.of(1999, 4, 12), candidate.getDateOfBirth());
        assertEquals("Nữ", candidate.getGender());
        assertEquals("Số 1 Lê Lợi", candidate.getAddress());
        assertEquals("https://linkedin.com/in/an", candidate.getLinkedInUrl());
        assertNull(candidate.getPortfolioUrl());
        verify(candidates).save(candidate);
    }

    @Test
    void updateNeverTouchesInternalFields() {
        when(candidates.findByAccount_UsernameIgnoreCase("an.vo")).thenReturn(Optional.of(candidate));

        service.update("an.vo", new UpdateProfileRequest("Võ Thành An", null),
                new CandidateDetailsRequest(null, "", "", "", ""));

        assertEquals("TopCV", candidate.getCandidateSource());
        assertTrue(candidate.isPotential());
        assertNull(candidate.getDateOfBirth());
        assertNull(candidate.getGender());
    }

    @Test
    void legacyGenderOutsideListIsKeptButOtherUnknownValuesAreRejected() {
        candidate.setGender("Male");
        when(candidates.findByAccount_UsernameIgnoreCase("an.vo")).thenReturn(Optional.of(candidate));
        UpdateProfileRequest account = new UpdateProfileRequest("Võ Thành An", null);

        service.update("an.vo", account, new CandidateDetailsRequest(null, "Male", null, null, null));
        assertEquals("Male", candidate.getGender());

        var error = assertThrows(CandidateProfileException.class, () -> service.update("an.vo", account,
                new CandidateDetailsRequest(null, "X", null, null, null)));
        assertEquals("gender", error.getField());
        assertEquals("Male", candidate.getGender());
        verify(userProfiles, times(1)).updateProfile("an.vo", account);
    }

    @Test
    void accountWithoutCandidateRowIsNotFound() {
        when(candidates.findByAccount_UsernameIgnoreCase("hr_lan")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.get("hr_lan"));
        assertThrows(ResourceNotFoundException.class, () -> service.update("hr_lan",
                new UpdateProfileRequest("Lan", null), new CandidateDetailsRequest(null, null, null, null, null)));
        verifyNoInteractions(userProfiles);
    }
}
