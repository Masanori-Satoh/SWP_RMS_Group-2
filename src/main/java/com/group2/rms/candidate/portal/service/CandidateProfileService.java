package com.group2.rms.candidate.portal.service;

import com.group2.rms.candidate.portal.dto.CandidateDetailsRequest;
import com.group2.rms.candidate.portal.dto.CandidateProfileResponse;
import com.group2.rms.candidate.entity.Candidate;
import com.group2.rms.candidate.portal.exception.CandidateProfileException;
import com.group2.rms.candidate.repository.CandidateRepository;
import com.group2.rms.core.exception.ResourceNotFoundException;
import com.group2.rms.user.dto.UpdateProfileRequest;
import com.group2.rms.user.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ứng viên tự xem, sửa hồ sơ của mình ({@code /portal/profile}). Phần chung (họ tên, SĐT) đi qua
 * {@link UserProfileService} để chỉ có một nguồn cho mọi vai trò; ở đây chỉ ghi 5 trường của bảng {@code Candidate}.
 * Sửa tự do kể cả khi đang có đơn; không ghi log cũ → mới, chỉ {@code UpdatedAt}.
 */
@Service
@RequiredArgsConstructor
public class CandidateProfileService {

    private final CandidateRepository candidates;
    private final UserProfileService userProfiles;

    @Transactional(readOnly = true)
    public CandidateProfileResponse get(String username) {
        Candidate candidate = findByUsername(username);
        return new CandidateProfileResponse(userProfiles.getProfile(username),
                candidate.getDateOfBirth(), candidate.getGender(), candidate.getAddress(),
                candidate.getLinkedInUrl(), candidate.getPortfolioUrl());
    }

    @Transactional
    public void update(String username, UpdateProfileRequest account, CandidateDetailsRequest details) {
        Candidate candidate = findByUsername(username);
        String gender = blankToNull(details.gender());
        if (gender != null && !CandidateDetailsRequest.genderOptions(candidate.getGender()).contains(gender)) {
            throw new CandidateProfileException("gender", "Giới tính không hợp lệ.");
        }
        userProfiles.updateProfile(username, account);
        candidate.setDateOfBirth(details.dateOfBirth());
        candidate.setGender(gender);
        candidate.setAddress(blankToNull(details.address()));
        candidate.setLinkedInUrl(blankToNull(details.linkedInUrl()));
        candidate.setPortfolioUrl(blankToNull(details.portfolioUrl()));
        candidates.save(candidate);
    }

    private Candidate findByUsername(String username) {
        return candidates.findByAccount_UsernameIgnoreCase(username)
                .orElseThrow(() -> new ResourceNotFoundException("Tài khoản chưa có hồ sơ ứng viên."));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
