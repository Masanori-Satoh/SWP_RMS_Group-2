package com.group2.rms.jobposting.service;

import com.group2.rms.candidate.domain.repository.ApplicationRepository;
import com.group2.rms.candidate.domain.rule.ApplicationStatusLabels;
import com.group2.rms.candidate.talentpool.service.TalentPoolService;
import com.group2.rms.core.dto.FilterOption;
import com.group2.rms.core.exception.ResourceNotFoundException;
import com.group2.rms.core.util.FieldChanges;
import com.group2.rms.jobposting.dto.InternalJobPostingDetailResponse;
import com.group2.rms.jobposting.dto.InternalJobPostingDetailResponse.ApplicationStats;
import com.group2.rms.jobposting.dto.InternalJobPostingDetailResponse.PostingRound;
import com.group2.rms.jobposting.dto.InternalJobPostingDetailResponse.RecentApplication;
import com.group2.rms.jobposting.dto.JobPostingRequest;
import com.group2.rms.jobposting.entity.JobPosting;
import com.group2.rms.jobposting.exception.JobPostingValidationException;
import com.group2.rms.jobposting.repository.JobPostingRepository;
import com.group2.rms.jobposting.validator.JobPostingValidator;
import com.group2.rms.notification.service.NotificationService;
import com.group2.rms.requisition.entity.JobRequisition;
import com.group2.rms.requisition.repository.JobRequisitionRepository;
import com.group2.rms.requisition.service.RequisitionAccess;
import com.group2.rms.requisition.service.RequisitionLabels;
import com.group2.rms.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/*
 * SERVICE: nghiệp vụ một tin tuyển dụng (tạo, sửa, chuyển trạng thái, xóa, xem chi tiết).
 */
@Service
@RequiredArgsConstructor
public class JobPostingServiceImpl implements JobPostingService {

    // Trạng thái hồ sơ (cột Application.ApplicationStatus) dùng cho số liệu trang chi tiết
    private static final String APPLICATION_APPLIED = "Applied";
    private static final String APPLICATION_AI_SCREENED = "AI_Screened";
    private static final String APPLICATION_INTERVIEWING = "Interviewing";
    private static final String APPLICATION_HIRED = "Hired";

    /** Khóa cấu hình ngưỡng điểm AI "đạt" trong bảng SystemConfig, và giá trị dùng khi chưa cấu hình. */
    private static final String AI_THRESHOLD_CONFIG_KEY = "AI_SCORE_THRESHOLD";
    private static final BigDecimal DEFAULT_AI_THRESHOLD = new BigDecimal("70");
    /** Số hồ sơ gần đây hiện ở tab "Ứng viên" trang chi tiết tin. */
    private static final int RECENT_APPLICATIONS = 7;
    /** Ô "+N trong 7 ngày" ở trang chi tiết. */
    private static final int RECENT_WINDOW_DAYS = 7;

    private final JobPostingRepository postings;
    private final JobRequisitionRepository requisitions;
    private final ApplicationRepository applications;
    private final JobPostingAccess access;
    private final JobPostingValidator validator;
    private final JobPostingMapper mapper;
    private final JobPostingAuditLogger auditLogger;
    private final NotificationService notificationService;
    private final TalentPoolService talentPoolService;
    private final Clock clock;

    // ----- Danh sách yêu cầu chờ tạo tin -----

    @Override
    @Transactional(readOnly = true)
    public List<FilterOption> requisitionChoices(User currentUser) {
        access.requireManage(currentUser);
        List<JobRequisition> awaiting = requisitionsAwaitingPosting();
        Map<Integer, Integer> postedTimes = maxPostingNoOf(awaiting);

        List<FilterOption> choices = new ArrayList<>();
        for (JobRequisition requisition : awaiting) {
            String label = requisition.getTitle() + " · " + requisition.getRequisitionCode();
            int posted = postedTimes.getOrDefault(requisition.getRequisitionId(), 0);
            if (posted > 0) {
                label = label + " (đã đăng " + posted + " lần)";
            }
            choices.add(new FilterOption(String.valueOf(requisition.getRequisitionId()), label));
        }
        return choices;
    }

    // ----- Mở form -----

    @Override
    @Transactional(readOnly = true)
    public JobPostingRequest prepareCreateForm(Integer requisitionId, User currentUser) {
        access.requireManage(currentUser);
        // Rule: tin chỉ được tạo từ một yêu cầu cụ thể đã duyệt, và mỗi yêu cầu chỉ có 1 tin đang mở
        if (requisitionId == null) {
            throw new JobPostingValidationException("Mã yêu cầu tuyển dụng không hợp lệ.");
        }
        JobRequisition requisition = findRequisition(requisitionId);
        validator.requireApproved(requisition);
        requireNoOtherOpenPosting(requisitionId, null);
        return mapper.toCreateForm(requisition, nextPostingNo(requisitionId));
    }

    @Override
    @Transactional(readOnly = true)
    public JobPostingRequest prepareRepostForm(Integer closedPostingId, User currentUser) {
        access.requireManage(currentUser);
        JobPosting closed = findById(closedPostingId);
        // Rule (D10): "Đăng lại" = tạo tin MỚI cùng yêu cầu (PostingNo + 1), nội dung lấy từ tin vừa đóng
        if (!JobPostingAccess.STATUS_CLOSED.equals(closed.getPostingStatus())) {
            throw new JobPostingValidationException("Chỉ tin đã đóng mới đăng lại được.");
        }
        JobPostingRequest form = prepareCreateForm(closed.getRequisition().getRequisitionId(), currentUser);
        mapper.copyPostingContent(form, closed);
        return form;
    }

    @Override
    @Transactional(readOnly = true)
    public JobPostingRequest prepareEditForm(Integer jobPostingId, User currentUser) {
        access.requireManage(currentUser);
        JobPosting posting = findById(jobPostingId);
        validator.requireEditable(posting.getPostingStatus());
        validator.requireApproved(posting.getRequisition());
        return mapper.toEditForm(posting);
    }

    @Override
    @Transactional(readOnly = true)
    public JobPostingRequest restoreSourceInfo(JobPostingRequest edited, User currentUser) {
        access.requireManage(currentUser);
        if (edited.getJobPostingId() != null) {
            JobPosting posting = findById(edited.getJobPostingId());
            mapper.fillSourceInfo(edited, posting.getRequisition());
            mapper.fillFormState(edited, posting);
            return edited;
        }
        if (edited.getRequisitionId() == null) {
            throw new ResourceNotFoundException("Không tìm thấy yêu cầu tuyển dụng nguồn.");
        }
        JobRequisition requisition = findRequisition(edited.getRequisitionId());
        mapper.fillSourceInfo(edited, requisition);
        mapper.fillNewFormState(edited, nextPostingNo(requisition.getRequisitionId()));
        return edited;
    }

    // ----- Tạo / sửa -----

    @Override
    @Transactional
    public Integer createJobPosting(JobPostingRequest request, User currentUser) {
        return create(request, currentUser, false);
    }

    @Override
    @Transactional
    public Integer createAndPublishJobPosting(JobPostingRequest request, User currentUser) {
        return create(request, currentUser, true);
    }

    private Integer create(JobPostingRequest request, User actor, boolean publish) {
        access.requireManage(actor);
        if (request.getJobPostingId() != null || request.getRequisitionId() == null) {
            throw new JobPostingValidationException("Yêu cầu tạo tin không hợp lệ.");
        }
        // Khóa yêu cầu nguồn: số lần đăng kế tiếp không bị trùng (UQ_JobPosting_Requisition_PostingNo)
        JobRequisition requisition = requisitions.findForUpdate(request.getRequisitionId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu tuyển dụng nguồn."));
        validator.requireApproved(requisition);
        requireNoOtherOpenPosting(requisition.getRequisitionId(), null);
        validator.normalizeAndValidate(request, publish);

        LocalDateTime now = LocalDateTime.now(clock);
        JobPosting posting = new JobPosting();
        posting.setRequisition(requisition);
        posting.setPostingNo(nextPostingNo(requisition.getRequisitionId()));
        posting.setCreatedBy(actor);
        applyEditableFields(posting, request, requisition);
        if (publish) {
            posting.setPostingStatus(JobPostingAccess.STATUS_PUBLISHED);
            posting.setPostingDate(now);
        } else {
            // Tin nháp chưa có ngày đăng.
            posting.setPostingStatus(JobPostingAccess.STATUS_DRAFT);
        }

        JobPosting saved = postings.save(posting);
        auditLogger.log(saved, actor, JobPostingAuditLogger.ACTION_CREATE,
                JobPostingLabels.LOG_CREATE + ": " + saved.getPostingStatus());
        if (publish) {
            notifyPublishedOrReopened(saved, actor, false);
        }
        return saved.getJobPostingId();
    }

    @Override
    @Transactional
    public void updateJobPosting(Integer id, JobPostingRequest request, User currentUser) {
        update(id, request, currentUser, false);
    }

    @Override
    @Transactional
    public void updateAndPublishJobPosting(Integer id, JobPostingRequest request, User currentUser) {
        update(id, request, currentUser, true);
    }

    /** Sửa tin; đăng hoặc mở lại thì ngày đăng = bây giờ; không đổi gì thì không lưu, không audit. */
    private void update(Integer id, JobPostingRequest request, User actor, boolean publish) {
        access.requireManage(actor);
        JobPosting posting = findForUpdate(id);
        validator.requireEditable(posting.getPostingStatus());
        requireSameSource(id, request, posting);
        validator.requireApproved(posting.getRequisition());

        LocalDateTime now = LocalDateTime.now(clock);
        String viewStatus = posting.getPostingStatus();
        boolean isDraft = JobPostingAccess.STATUS_DRAFT.equals(viewStatus);
        boolean isPaused = JobPostingAccess.STATUS_PAUSED.equals(viewStatus);
        if (publish && isPaused) {
            requireNoOtherOpenPosting(posting.getRequisition().getRequisitionId(), id);
        }
        // Tin đã đăng hoặc đang được đăng phải có hạn nộp.
        validator.normalizeAndValidate(request, publish || !isDraft);

        Map<String, String> before = auditLogger.snapshot(posting);
        LocalDateTime oldPostingDate = posting.getPostingDate();
        applyEditableFields(posting, request, posting.getRequisition());
        if (isDraft && publish) {
            posting.setPostingStatus(JobPostingAccess.STATUS_PUBLISHED);
            posting.setPostingDate(now);
        } else if (isPaused && publish) {
            posting.setPostingStatus(JobPostingAccess.STATUS_PUBLISHED);
            posting.setPostingDate(now);
        }
        Map<String, String> after = auditLogger.snapshot(posting);
        List<String> changedKeys = FieldChanges.changedKeys(before, after);
        if (changedKeys.isEmpty()) {
            return;
        }

        boolean isNewPublication = posting.getPostingDate() != null && !posting.getPostingDate().equals(oldPostingDate);
        String description = JobPostingLabels.LOG_UPDATE;
        if (isNewPublication) {
            description = JobPostingLabels.LOG_PUBLISH;
        }
        postings.save(posting);
        auditLogger.logChanges(posting, actor, description, changedKeys, before, after);
        if (isNewPublication) {
            notifyPublishedOrReopened(posting, actor, isPaused);
        }
    }

    // ----- Chuyển trạng thái -----

    @Override
    @Transactional
    public void publishJobPosting(Integer id, User currentUser) {
        access.requireManage(currentUser);
        JobPosting posting = findForUpdate(id);
        String status = posting.getPostingStatus();
        if (!JobPostingAccess.STATUS_DRAFT.equals(status) && !JobPostingAccess.STATUS_PAUSED.equals(status)) {
            throw new JobPostingValidationException("Chỉ tin nháp hoặc đang tạm dừng mới được đăng.");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        // Rule: kiểm lại khi đăng — yêu cầu nguồn vẫn Approved, tin đủ nội dung và còn hạn nộp
        validator.requireApproved(posting.getRequisition());
        validator.requirePublishableContent(posting, now);
        requireNoOtherOpenPosting(posting.getRequisition().getRequisitionId(), id);

        boolean wasPaused = JobPostingAccess.STATUS_PAUSED.equals(status);
        posting.setPostingStatus(JobPostingAccess.STATUS_PUBLISHED);
        posting.setPostingDate(now);
        postings.save(posting);
        auditLogger.log(posting, currentUser, JobPostingAuditLogger.ACTION_UPDATE, JobPostingLabels.LOG_PUBLISH);
        notifyPublishedOrReopened(posting, currentUser, wasPaused);
    }

    @Override
    @Transactional
    public void pauseJobPosting(Integer id, User currentUser) {
        access.requireManage(currentUser);
        JobPosting posting = findForUpdate(id);
        if (!JobPostingAccess.STATUS_PUBLISHED.equals(posting.getPostingStatus())) {
            throw new JobPostingValidationException("Chỉ tin đang đăng mới được tạm dừng.");
        }
        posting.setPostingStatus(JobPostingAccess.STATUS_PAUSED);
        postings.save(posting);
        auditLogger.log(posting, currentUser, JobPostingAuditLogger.ACTION_UPDATE, JobPostingLabels.LOG_PAUSE);
        notificationService.notifyJobPostingPaused(posting, currentUser);
    }

    @Override
    @Transactional
    public void closeJobPosting(Integer id, User currentUser) {
        access.requireCloseAny(currentUser);
        talentPoolService.closeJob(id);
        postings.findById(id).ifPresent(posting -> notificationService.notifyJobPostingClosed(posting, currentUser));
    }

    @Override
    @Transactional
    public void deleteJobPosting(Integer id, User currentUser) {
        access.requireManage(currentUser);
        JobPosting posting = findForUpdate(id);
        // Rule: chỉ xóa bản nháp chưa có hồ sơ ứng tuyển
        if (!JobPostingAccess.STATUS_DRAFT.equals(posting.getPostingStatus())
                || applications.existsByJobPosting_JobPostingId(id)) {
            throw new JobPostingValidationException("Chỉ có thể xóa bản nháp chưa có hồ sơ ứng tuyển.");
        }
        // Ghi audit trước khi xóa để vẫn còn tiêu đề tin trong lịch sử
        auditLogger.log(posting, currentUser, JobPostingAuditLogger.ACTION_DELETE, JobPostingLabels.LOG_DELETE);
        postings.delete(posting);
    }

    // ----- Trang chi tiết -----

    @Override
    @Transactional(readOnly = true)
    public InternalJobPostingDetailResponse getInternalJobPostingDetail(Integer id, User currentUser) {
        access.requireAccessPostings(currentUser);
        JobPosting posting = findById(id);
        access.requireView(currentUser, posting);

        LocalDateTime now = LocalDateTime.now(clock);
        String viewStatus = posting.getPostingStatus();
        JobRequisition requisition = posting.getRequisition();

        // Số liệu hồ sơ chỉ có khi tin đã lên trang tuyển dụng
        BigDecimal threshold = aiScoreThreshold();
        ApplicationStats stats = ApplicationStats.empty(threshold);
        if (JobPostingAccess.hasBeenPublished(viewStatus)) {
            stats = applicationStats(id, threshold, now);
        }
        boolean hasApplications = stats.total() > 0;
        if (JobPostingAccess.STATUS_DRAFT.equals(viewStatus)) {
            hasApplications = applications.existsByJobPosting_JobPostingId(id);
        }
        boolean hasOpenPosting = false;
        if (JobPostingAccess.STATUS_CLOSED.equals(viewStatus) && requisition != null) {
            hasOpenPosting = postings.existsByRequisition_RequisitionIdAndPostingStatusIn(
                    requisition.getRequisitionId(), JobPostingAccess.OPEN_STATUSES);
        }
        List<PostingRound> rounds = List.of();
        if (requisition != null) {
            rounds = postingRounds(requisition.getRequisitionId(), id);
        }
        return mapper.toDetail(posting, currentUser, now, hasApplications, hasOpenPosting, stats, rounds,
                auditLogger.history(id));
    }

    /** Số liệu hồ sơ của tin: đếm theo trạng thái (1 câu) + vài câu đếm riêng. */
    private ApplicationStats applicationStats(Integer postingId, BigDecimal threshold, LocalDateTime now) {
        Map<String, Long> byStatus = new HashMap<>();
        long total = 0;
        for (Object[] row : postings.countApplicationsByStatus(postingId)) {
            long count = ((Number) row[1]).longValue();
            byStatus.put((String) row[0], count);
            total += count;
        }
        return ApplicationStats.builder()
                .total(total)
                .last7Days(postings.countApplicationsSince(postingId, now.minusDays(RECENT_WINDOW_DAYS)))
                .pending(byStatus.getOrDefault(APPLICATION_APPLIED, 0L)
                        + byStatus.getOrDefault(APPLICATION_AI_SCREENED, 0L))
                .aiPassed(postings.countApplicationsWithAiScoreAtLeast(postingId, threshold))
                .aiThreshold(threshold)
                .interviewing(byStatus.getOrDefault(APPLICATION_INTERVIEWING, 0L))
                .hired(byStatus.getOrDefault(APPLICATION_HIRED, 0L))
                .recent(recentApplications(postingId, threshold))
                .build();
    }

    /** Các hồ sơ mới nhất kèm điểm AI lần chấm gần nhất (null = chưa chấm). Điểm AI tra gộp 1 câu. */
    private List<RecentApplication> recentApplications(Integer postingId, BigDecimal threshold) {
        List<Object[]> rows = postings.findRecentApplications(postingId, PageRequest.of(0, RECENT_APPLICATIONS));
        if (rows.isEmpty()) {
            return List.of();
        }
        List<Integer> ids = new ArrayList<>();
        for (Object[] row : rows) {
            ids.add((Integer) row[0]);
        }
        Map<Integer, BigDecimal> scores = new HashMap<>();
        for (Object[] row : postings.findLatestAiScores(ids)) {
            scores.put((Integer) row[0], (BigDecimal) row[1]);
        }

        List<RecentApplication> result = new ArrayList<>();
        for (Object[] row : rows) {
            Integer applicationId = (Integer) row[0];
            String status = (String) row[2];
            BigDecimal score = scores.get(applicationId);
            boolean isAiPassed = score != null && score.compareTo(threshold) >= 0;
            result.add(new RecentApplication(applicationId, (String) row[1], score, isAiPassed,
                    ApplicationStatusLabels.label(status), ApplicationStatusLabels.badge(status),
                    (LocalDateTime) row[3]));
        }
        return result;
    }

    /** Các lần đăng của cùng yêu cầu, kèm số hồ sơ và số đã tuyển (2 câu đếm gộp). */
    private List<PostingRound> postingRounds(Integer requisitionId, Integer currentId) {
        List<JobPosting> all = postings.findByRequisition_RequisitionIdOrderByPostingNoAsc(requisitionId);
        if (all.isEmpty()) {
            return List.of();
        }
        List<Integer> ids = new ArrayList<>();
        for (JobPosting round : all) {
            ids.add(round.getJobPostingId());
        }
        Map<Integer, Long> applicationCounts = countByPosting(
                postings.countApplicationsByPosting(ids, JobPostingListService.NEW_APPLICATION_STATUS));
        Map<Integer, Long> hiredCounts = countByPosting(
                postings.countApplicationsInStatusByPosting(ids, APPLICATION_HIRED));

        List<PostingRound> rounds = new ArrayList<>();
        for (JobPosting round : all) {
            rounds.add(mapper.toPostingRound(round, currentId,
                    applicationCounts.getOrDefault(round.getJobPostingId(), 0L),
                    hiredCounts.getOrDefault(round.getJobPostingId(), 0L)));
        }
        return rounds;
    }

    /** Ngưỡng điểm AI "đạt" (SystemConfig.AI_SCORE_THRESHOLD); thiếu hoặc sai định dạng thì dùng 70. */
    private BigDecimal aiScoreThreshold() {
        String configured = postings.findConfigValue(AI_THRESHOLD_CONFIG_KEY).orElse(null);
        if (configured == null) {
            return DEFAULT_AI_THRESHOLD;
        }
        try {
            // Cấu hình lưu "70.00" thì hiển thị "70" (setScale(0) để 70 không thành 7E+1)
            BigDecimal trimmed = new BigDecimal(configured.trim()).stripTrailingZeros();
            if (trimmed.scale() < 0) {
                return trimmed.setScale(0);
            }
            return trimmed;
        } catch (NumberFormatException invalidConfig) {
            return DEFAULT_AI_THRESHOLD;
        }
    }

    // ----- Hàm kiểm tra / tra cứu dùng chung -----

    private JobPosting findForUpdate(Integer id) {
        if (id == null) {
            throw new JobPostingValidationException("Mã tin tuyển dụng không hợp lệ.");
        }
        return postings.findForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tin tuyển dụng."));
    }

    private JobPosting findById(Integer id) {
        if (id == null) {
            throw new JobPostingValidationException("Mã tin tuyển dụng không hợp lệ.");
        }
        return postings.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tin tuyển dụng với mã: " + id));
    }

    private JobRequisition findRequisition(Integer requisitionId) {
        return requisitions.findById(requisitionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy yêu cầu tuyển dụng với mã: " + requisitionId));
    }

    /** Rule: mỗi yêu cầu chỉ có 1 tin nháp / đang đăng / tạm dừng. currentId = tin đang sửa (bỏ qua chính nó). */
    private void requireNoOtherOpenPosting(Integer requisitionId, Integer currentId) {
        boolean exists;
        if (currentId == null) {
            exists = postings.existsByRequisition_RequisitionIdAndPostingStatusIn(
                    requisitionId, JobPostingAccess.OPEN_STATUSES);
        } else {
            exists = postings.existsByRequisition_RequisitionIdAndJobPostingIdNotAndPostingStatusIn(
                    requisitionId, currentId, JobPostingAccess.OPEN_STATUSES);
        }
        if (exists) {
            throw new JobPostingValidationException("Yêu cầu này đã có tin nháp hoặc tin đang hoạt động.");
        }
    }

    /** Rule: sửa tin không đổi được tin khác hay yêu cầu nguồn (id gửi từ form phải khớp với tin trong DB). */
    private void requireSameSource(Integer id, JobPostingRequest request, JobPosting posting) {
        boolean isSamePosting = id.equals(request.getJobPostingId());
        boolean isSameRequisition = posting.getRequisition().getRequisitionId().equals(request.getRequisitionId());
        if (!isSamePosting || !isSameRequisition) {
            throw new JobPostingValidationException("Không thể đổi tin hoặc yêu cầu tuyển dụng nguồn.");
        }
    }

    /** Yêu cầu đã duyệt nhưng chưa có tin nháp / đang đăng / tạm dừng: HR cần tạo tin cho chúng. */
    private List<JobRequisition> requisitionsAwaitingPosting() {
        Set<Integer> occupied = postings.findRequisitionIdsWithPostingStatusIn(JobPostingAccess.OPEN_STATUSES);
        List<JobRequisition> awaiting = new ArrayList<>();
        for (JobRequisition requisition
                : requisitions.findWithDepartmentByApprovalStatusOrderByCreatedAtDesc(RequisitionAccess.STATUS_APPROVED)) {
            if (!occupied.contains(requisition.getRequisitionId())) {
                awaiting.add(requisition);
            }
        }
        return awaiting;
    }

    /** Số lần đăng lớn nhất của các yêu cầu (1 câu cho cả danh sách, tránh N+1). */
    private Map<Integer, Integer> maxPostingNoOf(List<JobRequisition> requisitionList) {
        Map<Integer, Integer> result = new HashMap<>();
        if (requisitionList.isEmpty()) {
            return result;
        }
        List<Integer> ids = new ArrayList<>();
        for (JobRequisition requisition : requisitionList) {
            ids.add(requisition.getRequisitionId());
        }
        for (Object[] row : postings.findMaxPostingNoByRequisitionIds(ids)) {
            result.put((Integer) row[0], ((Number) row[1]).intValue());
        }
        return result;
    }

    /** Rule: tin tạm dừng đăng lại, hoặc lần đăng thứ 2 trở đi (tin trước đã đóng) là "mở lại"; còn lại là phát hành lần đầu. */
    private void notifyPublishedOrReopened(JobPosting posting, User actor, boolean wasPaused) {
        boolean repostAfterClose = posting.getPostingNo() != null && posting.getPostingNo() > 1;
        if (wasPaused || repostAfterClose) {
            notificationService.notifyJobPostingReopened(posting, actor);
        } else {
            notificationService.notifyJobPostingPublished(posting, actor);
        }
    }

    private int nextPostingNo(Integer requisitionId) {
        return postings.findMaxPostingNo(requisitionId) + 1;
    }

    private static Map<Integer, Long> countByPosting(List<Object[]> rows) {
        Map<Integer, Long> counts = new HashMap<>();
        for (Object[] row : rows) {
            counts.put((Integer) row[0], ((Number) row[1]).longValue());
        }
        return counts;
    }

    /** Ghi các trường HR được sửa (CLAUDE.md §22). Chuỗi đã được validator trim / làm sạch. */
    private void applyEditableFields(JobPosting posting, JobPostingRequest request, JobRequisition requisition) {
        posting.setPostingTitle(request.getPostingTitle());
        posting.setJobDescription(request.getJobDescription());
        posting.setJobRequirements(request.getJobRequirements());
        posting.setBenefits(request.getBenefits());
        posting.setWorkLocation(request.getWorkLocation());
        posting.setSalaryDisplay(salaryDisplayOf(request, requisition));
        // Rule: tắt "Hiển thị số lượng tuyển" thì lưu NULL (trang công khai không hiện số lượng)
        String positions = null;
        if (Boolean.TRUE.equals(request.getShowPositions())) {
            positions = request.getNumberOfPositionsDisplay();
        }
        posting.setNumberOfPositionsDisplay(positions);
        posting.setApplicationDeadline(validator.deadlineOf(request));
    }

    /** Chuỗi lương lưu vào SalaryDisplay: theo yêu cầu (lấy từ DB, không tin số gửi lên), thỏa thuận, hoặc tự nhập. */
    private String salaryDisplayOf(JobPostingRequest request, JobRequisition requisition) {
        return switch (request.getSalaryMode()) {
            case JobPostingRequest.SALARY_RANGE ->
                    RequisitionLabels.salary(requisition.getMinSalary(), requisition.getMaxSalary());
            case JobPostingRequest.SALARY_CUSTOM -> request.getSalaryCustom();
            default -> JobPostingLabels.SALARY_NEGOTIABLE;
        };
    }
}
