package com.group2.rms.candidate.service;

import com.group2.rms.candidate.entity.Application;
import com.group2.rms.candidate.entity.Candidate;
import com.group2.rms.candidate.exception.ApplicationSubmissionException;
import com.group2.rms.candidate.repository.ApplicationRepository;
import com.group2.rms.candidate.repository.CandidateRepository;
import com.group2.rms.candidate.storage.CvStorage;
import com.group2.rms.candidate.storage.StoredFile;
import com.group2.rms.candidate.validator.CvFileValidator;
import com.group2.rms.core.exception.ResourceNotFoundException;
import com.group2.rms.jobposting.entity.JobPosting;
import com.group2.rms.jobposting.repository.JobPostingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

/**
 * Service tiếp nhận hồ sơ ứng tuyển của ứng viên cho tin tuyển dụng.
 */
@Service
@RequiredArgsConstructor
public class ApplicationSubmissionService {

    public static final String INITIAL_STATUS = "Applied";
    public static final String ALREADY_APPLIED_MESSAGE = "Bạn đã ứng tuyển tin tuyển dụng này.";

    private final CandidateRepository candidateRepository;
    private final ApplicationRepository applicationRepository;
    private final JobPostingRepository jobPostingRepository;
    private final CvFileValidator cvFileValidator;
    private final CvStorage cvStorage;

    /**
     * Xử lý nộp hồ sơ ứng tuyển và lưu trữ file CV.
     */
    @Transactional
    public Integer apply(String username, Integer jobPostingId, MultipartFile cvFile) {
        Candidate candidate = candidateRepository.findByAccount_UsernameIgnoreCase(username)
                .orElseThrow(() -> new ApplicationSubmissionException(null,
                        "Tài khoản chưa có hồ sơ ứng viên. Vui lòng liên hệ quản trị viên."));

        JobPosting job = jobPostingRepository.findForUpdate(jobPostingId)
                .filter(j -> j.isVisibleToCandidates(LocalDateTime.now()))
                .orElseThrow(() -> new ResourceNotFoundException("Tin tuyển dụng này không còn khả dụng."));

        if (job.getApplicationDeadline() != null && job.getApplicationDeadline().isBefore(LocalDateTime.now())) {
            throw new ApplicationSubmissionException(null, "Tin tuyển dụng đã hết hạn nhận hồ sơ.");
        }
        if (applicationRepository.existsActiveByCandidateAndJobPosting(
                candidate.getCandidateId(), job.getJobPostingId())) {
            throw new ApplicationSubmissionException(null, ALREADY_APPLIED_MESSAGE);
        }
        cvFileValidator.validate(cvFile);

        StoredFile stored = cvStorage.store(cvFile, candidate.getCandidateId());
        deleteFileUnlessCommitted(stored);

        Application draft = applicationRepository
                .findFirstByCandidate_CandidateIdAndJobPosting_JobPostingIdAndApplicationStatus(
                        candidate.getCandidateId(), jobPostingId, "HM_Suggested")
                .orElse(null);
        Application saved;
        if (draft != null) {
            if (applicationRepository.submitSuggestionWithNewCv(
                    draft.getApplicationId(), stored.url(), INITIAL_STATUS) != 1) {
                throw new ApplicationSubmissionException(null, "Đề xuất vừa được xử lý. Vui lòng tải lại trang.");
            }
            applicationRepository.recordSuggestionSubmitted(
                    candidate.getAccount().getUserId(), draft.getApplicationId());
            saved = draft;
        } else {
            saved = applicationRepository.save(Application.builder()
                    .candidate(candidate)
                    .jobPosting(job)
                    .appliedCvUrl(stored.url())
                    .applicationStatus(INITIAL_STATUS)
                    .build());
        }
        return saved.getApplicationId();
    }

    /**
     * Kiểm tra ứng viên đã nộp hồ sơ cho tin tuyển dụng hay chưa.
     */
    @Transactional(readOnly = true)
    public boolean hasApplied(String username, Integer jobPostingId) {
        if (username == null) {
            return false;
        }
        return candidateRepository.findByAccount_UsernameIgnoreCase(username)
                .map(c -> applicationRepository.existsActiveByCandidateAndJobPosting(
                        c.getCandidateId(), jobPostingId))
                .orElse(false);
    }

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
