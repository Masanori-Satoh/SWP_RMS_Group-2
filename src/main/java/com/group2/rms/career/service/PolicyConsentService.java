package com.group2.rms.career.service;

import com.group2.rms.candidate.entity.Candidate;
import com.group2.rms.candidate.repository.CandidateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Service quản lý trạng thái đồng ý Điều khoản sử dụng và Chính sách bảo mật của ứng viên.
 */
@Service
@RequiredArgsConstructor
public class PolicyConsentService {

    private final CandidateRepository candidateRepository;
    private final Clock clock;

    /**
     * Kiểm tra ứng viên có cần xác nhận đồng ý chính sách hay không.
     */
    @Transactional(readOnly = true)
    public boolean needsConsent(String username) {
        Optional<Candidate> candidate = candidateRepository.findByAccount_UsernameIgnoreCase(username);
        return candidate.isPresent() && candidate.get().getPolicyAgreedAt() == null;
    }

    /**
     * Ghi nhận thời điểm ứng viên đồng ý chính sách vào hệ thống.
     */
    @Transactional
    public void recordConsent(String username) {
        Optional<Candidate> candidate = candidateRepository.findByAccount_UsernameIgnoreCase(username);
        if (candidate.isEmpty() || candidate.get().getPolicyAgreedAt() != null) {
            return;
        }
        candidate.get().setPolicyAgreedAt(LocalDateTime.now(clock));
        candidateRepository.save(candidate.get());
    }
}
