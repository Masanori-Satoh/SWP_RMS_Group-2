package com.group2.rms.candidate.service;

import com.group2.rms.candidate.dto.CandidateProfileResponse;
import com.group2.rms.candidate.dto.UpdateCandidateProfileRequest;
import com.group2.rms.candidate.entity.Candidate;
import com.group2.rms.candidate.exception.CandidateProfileException;
import com.group2.rms.candidate.repository.CandidateRepository;
import com.group2.rms.core.exception.ResourceNotFoundException;
import com.group2.rms.user.dto.UpdateProfileRequest;
import com.group2.rms.user.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Service quản lý thông tin hồ sơ của ứng viên.
 */
@Service
@RequiredArgsConstructor
public class CandidateProfileService {

    private final CandidateRepository candidateRepository;
    private final UserProfileService userProfileService;

    @Transactional(readOnly = true)
    public CandidateProfileResponse get(String username) {
        Candidate candidate = findByUsername(username);
        return new CandidateProfileResponse(userProfileService.getProfile(username),
                candidate.getDateOfBirth(), candidate.getGender(), candidate.getAddress(),
                candidate.getLinkedInUrl(), candidate.getPortfolioUrl());
    }

    @Transactional
    public void update(String username, UpdateProfileRequest account, UpdateCandidateProfileRequest details) {
        Candidate candidate = findByUsername(username);
        String gender = blankToNull(details.gender());
        if (gender != null && !UpdateCandidateProfileRequest.genderOptions(candidate.getGender()).contains(gender)) {
            throw new CandidateProfileException("gender", "Giới tính không hợp lệ.");
        }
        if (details.dateOfBirth() != null) {
            if (details.dateOfBirth().isAfter(LocalDate.now().minusYears(18))) {
                throw new CandidateProfileException("dateOfBirth", "Ứng viên phải từ đủ 18 tuổi.");
            }
            if (details.dateOfBirth().isBefore(LocalDate.now().minusYears(100))) {
                throw new CandidateProfileException("dateOfBirth", "Ngày sinh không hợp lệ.");
            }
        }
        userProfileService.updateCandidateProfile(username, account, details.email());
        candidate.setDateOfBirth(details.dateOfBirth());
        candidate.setGender(gender);
        candidate.setAddress(blankToNull(details.address()));
        candidate.setLinkedInUrl(blankToNull(details.linkedInUrl()));
        candidate.setPortfolioUrl(blankToNull(details.portfolioUrl()));
        candidateRepository.save(candidate);
    }

    private Candidate findByUsername(String username) {
        return candidateRepository.findByAccount_UsernameIgnoreCase(username)
                .orElseThrow(() -> new ResourceNotFoundException("Tài khoản chưa có hồ sơ ứng viên."));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
