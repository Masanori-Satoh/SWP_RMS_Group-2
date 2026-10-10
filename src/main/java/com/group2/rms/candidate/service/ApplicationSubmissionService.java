package com.group2.rms.candidate.service;

import com.group2.rms.candidate.entity.Application;
import com.group2.rms.candidate.entity.Candidate;
import com.group2.rms.candidate.exception.ApplicationSubmissionException;
import com.group2.rms.candidate.repository.ApplicationRepository;
import com.group2.rms.candidate.repository.CandidateRepository;
import com.group2.rms.candidate.validator.CvFileValidator;
import com.group2.rms.core.exception.ResourceNotFoundException;
import com.group2.rms.requisition.entity.JobPosting;
import com.group2.rms.requisition.repository.JobPostingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

/**
 * Ứng viên nộp đơn cho một tin tuyển dụng (5.1.36).
 * Mọi kiểm tra chạy trước khi lưu file; file đã lưu sẽ bị xóa nếu transaction không commit.
 */
@Service
@RequiredArgsConstructor
public class ApplicationSubmissionService {

    static final String INITIAL_STATUS = "Applied";

    private final CandidateRepository candidates;
    private final ApplicationRepository applications;
    private final JobPostingRepository jobPostings;
    private final CvFileValidator cvFileValidator;
    private final CvStorage cvStorage;
    private final ApplicationEventPublisher events;

    @Transactional
    public Integer apply(String username, Integer jobPostingId, MultipartFile cvFile) {
        Candidate candidate = candidates.findByAccount_UsernameIgnoreCase(username)
                .orElseThrow(() -> new ApplicationSubmissionException(null,
                        "Tài khoản chưa có hồ sơ ứng viên. Vui lòng liên hệ quản trị viên."));

        JobPosting job = jobPostings.findById(jobPostingId)
                .filter(j -> "Published".equals(j.getPostingStatus()))
                .orElseThrow(() -> new ResourceNotFoundException("Tin tuyển dụng này không còn khả dụng."));

        if (job.getApplicationDeadline() != null && job.getApplicationDeadline().isBefore(LocalDateTime.now())) {
            throw new ApplicationSubmissionException(null, "Tin tuyển dụng đã hết hạn nhận hồ sơ.");
        }
        if (applications.existsByCandidate_CandidateIdAndJobPosting_JobPostingId(
                candidate.getCandidateId(), job.getJobPostingId())) {
            throw new ApplicationSubmissionException(null, "Bạn đã ứng tuyển vị trí này.");
        }
        cvFileValidator.validate(cvFile);

        StoredFile stored = cvStorage.store(cvFile, candidate.getCandidateId());
        deleteFileUnlessCommitted(stored);

        Application saved = applications.save(Application.builder()
                .candidate(candidate)
                .jobPosting(job)
                .appliedCvUrl(stored.url())
                .applicationStatus(INITIAL_STATUS)
                .build());
        events.publishEvent(new ApplicationSubmittedEvent(saved.getApplicationId()));
        return saved.getApplicationId();
    }

    /** Chưa đăng nhập hoặc không phải ứng viên thì coi như chưa nộp. */
    @Transactional(readOnly = true)
    public boolean hasApplied(String username, Integer jobPostingId) {
        if (username == null) {
            return false;
        }
        return candidates.findByAccount_UsernameIgnoreCase(username)
                .map(c -> applications.existsByCandidate_CandidateIdAndJobPosting_JobPostingId(
                        c.getCandidateId(), jobPostingId))
                .orElse(false);
    }

    /** File nằm ngoài DB nên rollback không xóa được: tự xóa khi transaction kết thúc mà không commit. */
    private void deleteFileUnlessCommitted(StoredFile stored) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    cvStorage.delete(stored.key());
                }
            }
        });
    }
}
