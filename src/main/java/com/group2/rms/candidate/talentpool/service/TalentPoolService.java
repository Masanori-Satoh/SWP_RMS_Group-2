package com.group2.rms.candidate.talentpool.service;

import com.group2.rms.candidate.domain.entity.Application;
import com.group2.rms.candidate.domain.entity.ApplicationReview;
import com.group2.rms.candidate.domain.entity.Candidate;
import com.group2.rms.candidate.infra.cv.ApplicationCv;
import com.group2.rms.candidate.infra.cv.CvStorage;
import com.group2.rms.candidate.infra.cv.LocalCvStorage;
import com.group2.rms.candidate.domain.repository.ApplicationRepository;
import com.group2.rms.candidate.domain.repository.ApplicationReviewRepository;
import com.group2.rms.candidate.domain.repository.CandidateRepository;
import com.group2.rms.candidate.talentpool.repository.TalentPoolApplicationRepository;
import com.group2.rms.candidate.talentpool.repository.TalentPoolCandidateRepository;
import com.group2.rms.candidate.talentpool.entity.TalentPoolNote;
import com.group2.rms.candidate.talentpool.exception.TalentPoolException;
import com.group2.rms.candidate.domain.rule.ApplicationAccess;
import com.group2.rms.candidate.talentpool.dto.*;
import com.group2.rms.candidate.talentpool.repository.TalentPoolAuditRepository;
import com.group2.rms.candidate.talentpool.repository.TalentPoolNoteRepository;
import com.group2.rms.core.exception.ResourceNotFoundException;
import com.group2.rms.core.security.CurrentUserService;
import com.group2.rms.jobposting.entity.JobPosting;
import com.group2.rms.jobposting.repository.JobPostingRepository;
import com.group2.rms.requisition.service.RequisitionAccess;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TalentPoolService {
    private final CandidateRepository candidates;
    private final ApplicationRepository applications;
    private final TalentPoolApplicationRepository poolApplications;
    private final TalentPoolCandidateRepository poolCandidates;
    private final TalentPoolNoteRepository notes;
    private final TalentPoolAuditRepository audit;
    private final JobPostingRepository jobs;
    private final CurrentUserService currentUser;
    private final RequisitionAccess jobAccess;
    private final ApplicationAccess applicationAccess;
    private final UserRepository users;
    private final CvStorage storage;
    private final ApplicationReviewRepository reviews;

    public String viewerRole() {
        return actor().getRole().getRoleName();
    }

    public Integer selectionScope() {
        return actor().getUserId();
    }

    public User actor() {
        // get current user
        User user = currentUser.requireUser();
        // check if user is not HR or Hiring Manager
        if (!CurrentUserService.hasRole(user, "HR") && !CurrentUserService.hasRole(user, "Hiring Manager"))
            throw new AccessDeniedException("Chỉ HR và Hiring Manager được truy cập kho tiềm năng.");
        return user;
    }

    private User hr() {
        User user = actor();
        if (!CurrentUserService.hasRole(user, "HR"))
            throw new AccessDeniedException("Chỉ HR được xác nhận ứng tuyển.");
        return user;
    }

    public Page<TalentPoolCandidateResponse> search(TalentPoolSearch filter, Pageable page) {
        // get current user
        actor();
        // get keyword
        String keyword = filter.keyword() == null || filter.keyword().isBlank() ? null : filter.keyword().strip();
        // check if keyword is exceed max
        if (keyword != null && keyword.length() > 100)
            keyword = keyword.substring(0, 100);
        // get match potential candidate
        return poolCandidates.searchPotential(keyword, filter.markedBy(), filter.sourceJobId(),
                PageRequest.of(Math.max(0, page.getPageNumber()), Math.min(50, page.getPageSize())))
                .map(this::candidateResponse);
    }

    public Map<Integer, String> authors() {
        // get current user
        actor();
        Map<Integer, String> result = new LinkedHashMap<>();
        // get all user who is HR or Hiring Manager
        users.findAllWithRoleAndDepartment().stream()
                .filter(u -> CurrentUserService.hasRole(u, "HR") || CurrentUserService.hasRole(u, "Hiring Manager"))
                .forEach(u -> result.put(u.getUserId(), u.getFullName()));
        return result;
    }

    public Map<Integer, String> sourceJobs() {
        // get current user
        actor();

        Map<Integer, String> result = new LinkedHashMap<>();
        // get source jobs
        jobs.findAll().forEach(j -> result.put(j.getJobPostingId(), j.getPostingTitle()));
        return result;
    }

    public TalentPoolDetailResponse detail(Integer id) {
        actor();
        Candidate candidate = candidate(id);
        if (!candidate.isPotential())
            throw new ResourceNotFoundException("Ứng viên chưa nằm trong kho tiềm năng.");
        return new TalentPoolDetailResponse(candidateResponse(candidate), true,
                notesFor(id), poolApplications.findPreviousCvByCandidateId(id).stream()
                        .map(a -> new TalentPoolCvResponse(a.getApplicationId(), a.getJobPosting().getPostingTitle(),
                                a.getSubmissionDate()))
                        .toList());
    }

    public List<TalentPoolNoteResponse> notesFor(Integer id) {
        actor();
        return notes.findByCandidate_CandidateIdOrderByCreatedAtDescNoteIdDesc(id).stream()
                .map(n -> new TalentPoolNoteResponse(n.getNoteId(), n.getAuthor().getFullName(), n.getNoteContent(),
                        n.getCreatedAt()))
                .toList();
    }

    public TalentPoolDetailResponse recommendationDetail(Integer candidateId, Integer recommendationId) {
        recommendation(recommendationId, candidateId);
        Candidate c = candidate(candidateId);
        return new TalentPoolDetailResponse(candidateResponse(c), c.isPotential(), notesFor(candidateId),
                poolApplications.findPreviousCvByCandidateId(candidateId).stream()
                        .map(a -> new TalentPoolCvResponse(a.getApplicationId(), a.getJobPosting().getPostingTitle(),
                                a.getSubmissionDate()))
                        .toList());
    }

    public String pendingRecommendationUrl(Integer id) {
        if (applications.countPendingHrRecommendation(id) == 0)
            return null;
        User user = actor();
        Application a = application(id);
        requireTarget(user, a.getJobPosting());
        return "/talent-pool/" + a.getCandidate().getCandidateId() + "?recommendationId=" + id;
    }

    @Transactional
    public void markPotential(Integer applicationId) {
        User user = actor();
        Application application = application(applicationId);
        applicationAccess.requireView(user, application);
        Candidate candidate = application.getCandidate();
        if (!candidate.isPotential()) {
            candidate.setPotential(true);
            audit.record(user.getUserId(), "UPDATE", "Candidate", candidate.getCandidateId(), "false",
                    "TalentPoolMarked");
        }
    }

    public TalentPoolBookmarkResponse bookmark(Integer applicationId) {
        User user = actor();
        Application application = application(applicationId);
        applicationAccess.requireView(user, application);
        Candidate candidate = application.getCandidate();
        return new TalentPoolBookmarkResponse(candidate.getCandidateId(), candidate.isPotential(),
                notesFor(candidate.getCandidateId()));
    }

    /**
     * Remove only the shared bookmark; applications, CVs and note history remain
     * intact.
     */
    @Transactional
    public TalentPoolBookmarkResponse removeBookmark(Integer applicationId) {
        User user = actor();
        Application application = application(applicationId);
        applicationAccess.requireView(user, application);
        Candidate candidate = application.getCandidate();
        if (candidate.isPotential()) {
            candidate.setPotential(false);
            audit.record(user.getUserId(), "UPDATE", "Candidate", candidate.getCandidateId(), "true",
                    "TalentPoolUnmarked");
        }
        return new TalentPoolBookmarkResponse(candidate.getCandidateId(), false, notesFor(candidate.getCandidateId()));
    }

    /**
     * One transaction for the shared flag and optional note; no recruitment
     * transition.
     */
    @Transactional
    public TalentPoolBookmarkResponse saveBookmark(Integer applicationId, TalentPoolBookmarkRequest request) {
        String content = request.content() == null ? "" : request.content().strip();
        if (content.length() > 1000)
            throw new TalentPoolException("Ghi chú tối đa 1000 ký tự.");
        markPotential(applicationId);
        Candidate candidate = application(applicationId).getCandidate();
        if (!content.isEmpty())
            addNote(candidate.getCandidateId(), new TalentPoolNoteRequest(content));
        return bookmark(applicationId);
    }

    @Transactional
    public void addNote(Integer candidateId, TalentPoolNoteRequest request) {
        User user = actor();
        Candidate candidate = candidate(candidateId);
        if (!candidate.isPotential())
            throw new TalentPoolException("Hãy đánh dấu ứng viên tiềm năng trước khi thêm ghi chú.");
        notes.save(TalentPoolNote.builder().candidate(candidate).author(user).noteContent(request.content().strip())
                .build());
    }

    public String targetJobTitle(Integer id) {
        // get current user
        User user = actor();
        // get job posting
        JobPosting job = job(id, false);
        // check has permission to view
        requireTarget(user, job);
        // title of job posting
        return job.getPostingTitle();
    }

    public List<TalentPoolSuggestionResponse> suggestions(Integer jobId) {
        // get current user
        User user = actor();
        // get current job
        JobPosting job = job(jobId, false);
        // check permission in department
        requireTarget(user, job);
        // get list candidate recommend by HM
        return poolApplications.findSuggestionsByJobPostingId(jobId).stream().map(a -> {
            User c = a.getCandidate().getAccount();
            return new TalentPoolSuggestionResponse(a.getApplicationId(), c.getFullName(), c.getEmail(),
                    c.getPhoneNumber(),
                    audit.suggestedBy(a.getApplicationId()), a.getCreatedAt());
        }).toList();
    }

    /**
     * Read-only preflight; suggest() revalidates while holding the posting lock.
     */
    public TalentPoolSelectionReviewResponse reviewSelection(Integer jobId, TalentPoolSuggestRequest request) {
        User user = actor();
        JobPosting target = job(jobId, false);
        requireTarget(user, target);
        requireOpen(target);
        Set<Integer> selected = new HashSet<>();
        List<TalentPoolSelectionReviewResponse.Item> result = new ArrayList<>();
        for (Integer sourceId : request.sourceApplicationIds()) {
            Integer candidateId = null;
            String name = "Hồ sơ #" + sourceId;
            String title = "";
            LocalDateTime submittedAt = null;
            String error = null;
            try {
                Application source = application(sourceId);
                Candidate candidate = source.getCandidate();
                candidateId = candidate.getCandidateId();
                name = candidate.getAccount().getFullName();
                title = source.getJobPosting().getPostingTitle();
                submittedAt = source.getSubmissionDate();
                if (!candidate.isPotential())
                    throw new TalentPoolException("Ứng viên không còn trong kho tiềm năng.");
                if (!selected.add(candidateId))
                    throw new TalentPoolException("Ứng viên được chọn nhiều lần.");
                if (source.getJobPosting().getJobPostingId().equals(jobId))
                    throw new TalentPoolException("Chọn CV từ một tin tuyển dụng khác.");
                if (poolApplications.findPreviousCvByCandidateId(candidateId).stream()
                        .noneMatch(a -> a.getApplicationId().equals(sourceId)))
                    throw new TalentPoolException("CV này không còn đủ điều kiện làm hồ sơ nguồn.");
                if (applications.existsActiveByCandidateAndJobPosting(candidateId, jobId)
                        || applications.findFirstByCandidate_CandidateIdAndJobPosting_JobPostingIdAndApplicationStatus(
                                candidateId, jobId, "HM_Suggested").isPresent())
                    throw new TalentPoolException("Ứng viên đã có hồ sơ hoặc đề xuất cho tin này.");
                requireCv(source.getAppliedCvUrl());
            } catch (TalentPoolException | ResourceNotFoundException e) {
                error = e.getMessage();
            }
            result.add(
                    new TalentPoolSelectionReviewResponse.Item(candidateId, sourceId, name, title, submittedAt, error));
        }
        return new TalentPoolSelectionReviewResponse(result, true);
    }

    @Transactional
    public void suggest(Integer jobId, TalentPoolSuggestRequest request) {
        // get current user
        User user = actor();
        // check if role is not HM
        if (!CurrentUserService.hasRole(user, "Hiring Manager"))
            throw new AccessDeniedException("Chỉ Hiring Manager tạo đề xuất.");
        createDrafts(jobId, request, user, false);
    }

    @Transactional
    public void recommend(Integer jobId, TalentPoolSuggestRequest request) {
        createDrafts(jobId, request, hr(), true);
    }

    private void createDrafts(Integer jobId, TalentPoolSuggestRequest request, User user, boolean fromHr) {
        JobPosting target = job(jobId, true);
        // check permission
        requireTarget(user, target);
        // check job posting is avaiable
        requireOpen(target);
        // check if Hr is recommend and not have HM manage
        if (fromHr && receivingHiringManager(target) == null)
            throw new TalentPoolException("Tin chưa có Hiring Manager phụ trách hợp lệ để nhận gợi ý.");
        Set<Integer> selected = new HashSet<>();
        List<Application> sources = new ArrayList<>();
        // loop full candidate by application
        for (Integer sourceId : request.sourceApplicationIds()) {
            Application source = application(sourceId);
            Candidate candidate = source.getCandidate();
            // check if candidate is not potential
            if (!candidate.isPotential())
                throw new TalentPoolException("Ứng viên phải được đánh dấu tiềm năng.");
            // check if candidate is add before
            if (!selected.add(candidate.getCandidateId()))
                throw new TalentPoolException("Chỉ chọn một CV cho mỗi ứng viên.");
            // check if application is from another job posting
            if (source.getJobPosting().getJobPostingId().equals(jobId))
                throw new TalentPoolException("CV nguồn phải thuộc một đợt tuyển khác.");
            // check if application is from official application
            if (!poolApplications.findPreviousCvByCandidateId(candidate.getCandidateId()).stream()
                    .anyMatch(a -> a.getApplicationId().equals(sourceId)))
                throw new TalentPoolException("CV nguồn phải thuộc đơn ứng tuyển chính thức.");
            // check if cv is exist
            requireCv(source.getAppliedCvUrl());
            // check if candidate is already applied or suggested
            if (applications.existsActiveByCandidateAndJobPosting(candidate.getCandidateId(), jobId)
                    || applications.findFirstByCandidate_CandidateIdAndJobPosting_JobPostingIdAndApplicationStatus(
                            candidate.getCandidateId(), jobId, "HM_Suggested").isPresent())
                throw new TalentPoolException("Ứng viên đã có đơn hoặc đề xuất cho vị trí này.");
            sources.add(source);
        }

        // loop all cv is valid
        for (Application source : sources) {
            Application saved = applications.saveAndFlush(Application.builder().candidate(source.getCandidate())
                    .jobPosting(target).appliedCvUrl(source.getAppliedCvUrl()).applicationStatus("HM_Suggested")
                    .build());
            // if hr create draft
            if (fromHr) {
                reviews.saveAndFlush(ApplicationReview.builder().application(saved).reviewer(user)
                        .reviewerRole("HR").decision("Hold").build());
            }
            audit.record(user.getUserId(), "CREATE", "Application", saved.getApplicationId(),
                    source.getApplicationId().toString(),
                    fromHr ? "TalentPoolHrRecommended" : "TalentPoolSuggested");
        }
    }

    public String receivingManager(Integer jobId) {
        // get current user
        User user = actor();
        // get job
        JobPosting target = job(jobId, false);
        // check permisson
        requireTarget(user, target);
        // get hiring manager of job posting
        User manager = receivingHiringManager(target);
        return manager == null ? null : manager.getFullName();
    }

    private User receivingHiringManager(JobPosting target) {
        var requisition = target == null ? null : target.getRequisition();
        if (requisition == null)
            return null;
        User assigned = requisition.getHiringManager();
        if (assigned != null && CurrentUserService.hasRole(assigned, "Hiring Manager"))
            return assigned;
        // A manager change must not rewrite the original requisition owner.
        User replacement = requisition.getDepartment() == null ? null : requisition.getDepartment().getManager();
        return replacement != null && CurrentUserService.hasRole(replacement, "Hiring Manager")
                && "Active".equals(replacement.getAccountStatus()) ? replacement : null;
    }

    public boolean canSuggest(Integer jobId) {
        // get current user
        User user = actor();
        // get current job
        JobPosting target = job(jobId, false);
        // check permission
        requireTarget(user, target);
        // check if user is Hiring Manager and job is Published and job is not expired
        return CurrentUserService.hasRole(user, "Hiring Manager") && "Published".equals(target.getPostingStatus())
                && (target.getApplicationDeadline() == null
                        || !target.getApplicationDeadline().isBefore(LocalDateTime.now()));
    }

    public boolean canRecommend(Integer jobId) {
        User user = actor();
        JobPosting target = job(jobId, false);
        requireTarget(user, target);
        return CurrentUserService.hasRole(user, "HR") && "Published".equals(target.getPostingStatus())
                && (target.getApplicationDeadline() == null
                        || !target.getApplicationDeadline().isBefore(LocalDateTime.now()))
                && receivingHiringManager(target) != null;
    }

    public long pendingRecommendationCount(Integer jobId) {
        requireTarget(actor(), job(jobId, false));
        return poolApplications.countPendingHrRecommendationsForJob(jobId);
    }

    public List<TalentPoolRecommendationResponse> recommendations(Integer jobId) {
        User user = actor();
        JobPosting target = job(jobId, false);
        requireTarget(user, target);
        return poolApplications.findHrRecommendations(jobId).stream().map(a -> recommendationResponse(a, user))
                .toList();
    }

    public TalentPoolRecommendationResponse recommendation(Integer id, Integer candidateId) {
        // get current user
        User user = actor();
        Application a = application(id);
        requireTarget(user, a.getJobPosting());
        if (!a.getCandidate().getCandidateId().equals(candidateId)
                || !audit.has("Application", id, "TalentPoolHrRecommended")
                || audit.has("Application", id, "TalentPoolHrWithdrawn"))
            throw new ResourceNotFoundException("Không tìm thấy gợi ý cho ứng viên này.");
        return recommendationResponse(a, user);
    }

    private TalentPoolRecommendationResponse recommendationResponse(Application a, User user) {
        var history = reviews.findWithReviewerByApplicationId(a.getApplicationId());
        var origin = history.stream().filter(r -> "HR".equals(r.getReviewerRole()) && "Hold".equals(r.getDecision()))
                .min(Comparator.comparing(ApplicationReview::getReviewId))
                .orElseThrow(() -> new TalentPoolException("Gợi ý thiếu thông tin người gửi. Cần kiểm tra dữ liệu."));
        var decision = history.stream().filter(r -> "HiringManager".equals(r.getReviewerRole())
                && Set.of("Pass", "Fail").contains(r.getDecision()))
                .min(Comparator.comparing(ApplicationReview::getReviewId));
        boolean pending = "HM_Suggested".equals(a.getApplicationStatus()) && decision.isEmpty();
        String label = pending ? "Chờ phản hồi"
                : decision.map(r -> "Fail".equals(r.getDecision())
                        ? "HM đã từ chối"
                        : "HM đã chấp nhận").orElse("Đã kết thúc chờ phản hồi");
        boolean open = "Published".equals(a.getJobPosting().getPostingStatus())
                && (a.getJobPosting().getApplicationDeadline() == null
                        || !a.getJobPosting().getApplicationDeadline().isBefore(LocalDateTime.now()));
        return new TalentPoolRecommendationResponse(a.getApplicationId(), a.getCandidate().getCandidateId(),
                a.getJobPosting().getJobPostingId(), a.getJobPosting().getPostingTitle(),
                a.getCandidate().getAccount().getFullName(),
                origin.getReviewer().getFullName(), origin.getReviewedAt(), label, pending,
                pending && open && CurrentUserService.hasRole(user, "Hiring Manager"));
    }

    public TalentPoolRecommendationResponse recommendationForDecision(Integer id) {
        User user = actor();
        if (!CurrentUserService.hasRole(user, "Hiring Manager"))
            throw new AccessDeniedException("Chỉ HM được phản hồi gợi ý.");
        Application a = application(id);
        return recommendation(id, a.getCandidate().getCandidateId());
    }

    /**
     * Both list and candidate detail post to this one serialized decision
     * transaction.
     */
    @Transactional
    public Integer decideRecommendation(Integer id, boolean accept) {
        User user = actor();
        if (!CurrentUserService.hasRole(user, "Hiring Manager"))
            throw new AccessDeniedException("Chỉ HM được phản hồi gợi ý.");
        Application a = application(id);
        JobPosting target = job(a.getJobPosting().getJobPostingId(), true);
        requireTarget(user, target);
        requireOpen(target);
        if (applications.countPendingHrRecommendation(id) != 1)
            throw new TalentPoolException("Gợi ý đã được xử lý. Vui lòng tải lại trang.");
        if (accept) {
            if (applications.existsActiveByCandidateAndJobPosting(a.getCandidate().getCandidateId(),
                    target.getJobPostingId()))
                throw new TalentPoolException("Ứng viên đã có hồ sơ chính thức cho tin này.");
            requireCv(a.getAppliedCvUrl());
        } else if (applications.transition(id, Set.of("HM_Suggested"), "Rejected") != 1) {
            throw new TalentPoolException("Gợi ý đã được xử lý.");
        }
        reviews.saveAndFlush(ApplicationReview.builder().application(a).reviewer(user).reviewerRole("HiringManager")
                .decision(accept ? "Pass" : "Fail").build());
        audit.record(user.getUserId(), "UPDATE", "Application", id, "HM_Suggested",
                accept ? "TalentPoolSuggested" : "TalentPoolCancelled");
        return target.getJobPostingId();
    }

    /**
     * Withdraw only an unanswered HR draft, serialized with HM decisions and
     * activation.
     */
    @Transactional
    public void withdrawRecommendation(Integer jobId, Integer id) {
        User user = hr();
        JobPosting target = job(jobId, true);
        Application draft = application(id);
        if (!target.getJobPostingId().equals(draft.getJobPosting().getJobPostingId())
                || !audit.has("Application", id, "TalentPoolHrRecommended"))
            throw new ResourceNotFoundException("Không tìm thấy gợi ý gửi cho HM trong tin này.");
        if (applications.countPendingHrRecommendation(id) != 1
                || applications.transition(id, Set.of("HM_Suggested"), "Rejected") != 1)
            throw new TalentPoolException("Gợi ý đã được xử lý, không thể xóa. Vui lòng tải lại trang.");
        // Existing cancellation metadata keeps this Rejected draft out of every formal
        // application query.
        audit.record(user.getUserId(), "UPDATE", "Application", id, "HM_Suggested", "TalentPoolCancelled");
        audit.record(user.getUserId(), "UPDATE", "Application", id, "HM_Suggested", "TalentPoolHrWithdrawn");
    }

    @Transactional
    public Integer confirm(Integer id, TalentPoolConfirmRequest request) {
        User user = hr();
        if (!request.consent())
            throw new TalentPoolException("Cần xác nhận ứng viên đồng ý dùng CV cũ.");
        Application application = application(id);
        requireOpen(job(application.getJobPosting().getJobPostingId(), true));
        requireHmDecision(id);
        if (!"HM_Suggested".equals(application.getApplicationStatus()))
            throw new TalentPoolException("Đề xuất đã được xử lý. Vui lòng tải lại trang.");
        if (applications.existsActiveByCandidateAndJobPosting(application.getCandidate().getCandidateId(),
                application.getJobPosting().getJobPostingId()))
            throw new TalentPoolException("Ứng viên đã có hồ sơ chính thức cho tin này.");
        requireCv(application.getAppliedCvUrl());
        if (applications.promoteSuggestionToHmPassed(id) != 1)
            throw new TalentPoolException("Đề xuất đã được xử lý. Vui lòng tải lại trang.");
        audit.record(user.getUserId(), "UPDATE", "Application", id, "HM_Suggested", "TalentPoolActivated");
        return id;
    }

    @Transactional
    public Integer decline(Integer id) {
        User user = hr();
        Application application = application(id);
        job(application.getJobPosting().getJobPostingId(), true);
        requireHmDecision(id);
        if (applications.transition(id, Set.of("HM_Suggested"), "Rejected") != 1)
            throw new TalentPoolException("Đề xuất đã được xử lý.");
        audit.record(user.getUserId(), "UPDATE", "Application", id, "HM_Suggested", "TalentPoolCancelled");
        return application.getJobPosting().getJobPostingId();
    }

    @Transactional
    public void closeJob(Integer id) {
        User user = hr();
        JobPosting posting = job(id, true);
        if ("Closed".equals(posting.getPostingStatus()))
            return;
        if (!"Published".equals(posting.getPostingStatus()) && !"Paused".equals(posting.getPostingStatus()))
            throw new TalentPoolException("Chỉ tin đã đăng hoặc đang tạm dừng mới được đóng đợt tuyển.");
        if (audit.hasOpenOffers(id))
            throw new TalentPoolException("Còn Offer chưa kết thúc. Hãy xử lý Offer trước khi đóng đợt tuyển.");
        posting.setPostingStatus("Closed");
        posting.setClosedAt(java.time.LocalDateTime.now());
        audit.record(user.getUserId(), "UPDATE", "JobPosting", id, null, "Closed");
        for (Application suggestion : poolApplications.findAllDraftsByJobPostingId(id)) {
            if (applications.transition(suggestion.getApplicationId(), Set.of("HM_Suggested"), "Rejected") == 1)
                audit.record(user.getUserId(), "UPDATE", "Application", suggestion.getApplicationId(), "HM_Suggested",
                        "TalentPoolCancelled");
        }
        for (Application app : poolApplications.findPendingApplicationsForClosedJob(id)) {
            if (audit.hasAcceptedOffer(app.getApplicationId()))
                continue;
            String old = app.getApplicationStatus();
            if (applications.transition(app.getApplicationId(), Set.of(old), "Rejected") == 1) {
                if (audit.cancelInterviews(app.getApplicationId()) > 0)
                    audit.record(user.getUserId(), "UPDATE", "Application", app.getApplicationId(), null,
                            "InterviewCancelledOnClose");
                audit.record(user.getUserId(), "UPDATE", "Application", app.getApplicationId(), old, "PostingClosed");
            }
        }
    }

    public ApplicationCv cv(Integer candidateId, Integer appId) {
        return cv(candidateId, appId, null);
    }

    public ApplicationCv cv(Integer candidateId, Integer appId, Integer recommendationId) {
        actor();
        if (recommendationId != null)
            recommendation(recommendationId, candidateId);
        Candidate c = candidate(candidateId);
        Application app = application(appId);
        if ((!c.isPotential() && recommendationId == null) || !app.getCandidate().getCandidateId().equals(candidateId))
            throw new AccessDeniedException("CV không thuộc ứng viên tiềm năng này.");
        requireCv(app.getAppliedCvUrl());
        if (app.getAppliedCvUrl().startsWith(LocalCvStorage.URL_PREFIX))
            return ApplicationCv
                    .file(storage.load(app.getAppliedCvUrl().substring(LocalCvStorage.URL_PREFIX.length())));
        return ApplicationCv.redirect(app.getAppliedCvUrl());
    }

    private void requireCv(String url) {
        // check if cv is empty
        if (url == null || url.isBlank())
            throw new TalentPoolException("Hồ sơ nguồn không có CV.");
        // check if cv is uploaded to the system
        if (url.startsWith(LocalCvStorage.URL_PREFIX)) {
            storage.load(url.substring(LocalCvStorage.URL_PREFIX.length()));
        } else if (url.startsWith("/") && !url.startsWith("//")) {
            // check if cv is exist in the system
            if (!new ClassPathResource("static" + url).exists())
                throw new TalentPoolException("File CV cũ không còn tồn tại. Ứng viên cần nộp CV mới.");
            // chek if cv is not store
        } else if (!url.startsWith("https://") && !url.startsWith("http://")) {
            throw new TalentPoolException("Đường dẫn CV không hợp lệ.");
        }
    }

    private void requireTarget(User user, JobPosting job) {
        // check if role is HM and job is not in department
        if (CurrentUserService.hasRole(user, "Hiring Manager")
                && !jobAccess.inManagedDepartment(user, job.getRequisition()))
            throw new AccessDeniedException("Job không thuộc phạm vi phòng ban của bạn.");
    }

    private void requireHmDecision(Integer id) {
        if (applications.countPendingHrRecommendation(id) > 0)
            throw new TalentPoolException("Gợi ý đang chờ HM phản hồi, chưa thể xác nhận hoặc kết thúc liên hệ.");
    }

    private void requireOpen(JobPosting job) {
        // cehck if status job is not publish or job is close
        if (!"Published".equals(job.getPostingStatus()) || (job.getApplicationDeadline() != null
                && job.getApplicationDeadline().isBefore(LocalDateTime.now())))
            throw new TalentPoolException("Tin tuyển dụng không còn nhận hồ sơ.");
    }

    private Candidate candidate(Integer id) {
        return candidates.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ứng viên."));
    }

    private Application application(Integer id) {
        // get application by id
        return applications.findDetailById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ."));
    }

    private JobPosting job(Integer id, boolean lock) {
        // get job by ID
        return (lock ? jobs.findForUpdate(id) : jobs.findById(id))
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tin tuyển dụng."));
    }

    private TalentPoolCandidateResponse candidateResponse(Candidate c) {
        User u = c.getAccount();
        // get cv of candidate
        var cvs = poolApplications.findPreviousCvByCandidateId(c.getCandidateId()).stream()
                .map(a -> new TalentPoolCvResponse(a.getApplicationId(), a.getJobPosting().getPostingTitle(),
                        a.getSubmissionDate()))
                .toList();
        return new TalentPoolCandidateResponse(c.getCandidateId(), u.getFullName(), u.getEmail(), u.getPhoneNumber(),
                cvs);
    }
}
