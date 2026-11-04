package com.group2.rms.career.service;

import com.group2.rms.candidate.domain.entity.Candidate;
import com.group2.rms.candidate.domain.repository.CandidateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

/*
 * Ứng viên đồng ý Điều khoản sử dụng + Chính sách bảo mật (một lần, lưu Candidate.PolicyAgreedAt).
 * Popup đọc needsConsent; nút "Tôi đồng ý" gọi recordConsent. Không chặn nghiệp vụ nào khác.
 */
@Service
@RequiredArgsConstructor
public class PolicyConsentService {

    private final CandidateRepository candidates;
    private final Clock clock;

    /** true khi tài khoản có hồ sơ ứng viên và chưa đồng ý lần nào. */
    @Transactional(readOnly = true)
    public boolean needsConsent(String username) {
        Optional<Candidate> candidate = candidates.findByAccount_UsernameIgnoreCase(username);
        return candidate.isPresent() && candidate.get().getPolicyAgreedAt() == null;
    }

    /** Lưu lúc đồng ý; đã đồng ý rồi thì giữ nguyên thời điểm lần đầu (bấm 2 lần / mở 2 tab). */
    @Transactional
    public void recordConsent(String username) {
        Optional<Candidate> candidate = candidates.findByAccount_UsernameIgnoreCase(username);
        if (candidate.isEmpty() || candidate.get().getPolicyAgreedAt() != null) {
            return;
        }
        candidate.get().setPolicyAgreedAt(LocalDateTime.now(clock));
        candidates.save(candidate.get());
    }
}
