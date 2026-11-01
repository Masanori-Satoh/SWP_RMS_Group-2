package com.group2.rms.jobposting.service;

import com.group2.rms.admin.entity.AuditLog;
import com.group2.rms.jobposting.dto.InternalJobPostingDetailResponse;
import com.group2.rms.jobposting.dto.InternalJobPostingDetailResponse.ActivityHistoryItem;
import com.group2.rms.jobposting.dto.InternalJobPostingDetailResponse.ApplicationStats;
import com.group2.rms.jobposting.dto.InternalJobPostingDetailResponse.PostingRound;
import com.group2.rms.jobposting.dto.InternalJobPostingResponse;
import com.group2.rms.jobposting.dto.JobPostingRequest;
import com.group2.rms.jobposting.entity.JobPosting;
import com.group2.rms.jobposting.validator.JobPostingValidator;
import com.group2.rms.requisition.entity.JobRequisition;
import com.group2.rms.requisition.service.RequisitionLabels;
import com.group2.rms.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/*
 * MAPPER: entity tin → DTO cho danh sách, chi tiết, form; không truy vấn DB.
 */
@Component
@RequiredArgsConstructor
public class JobPostingMapper {

    /** Số người hiển thị mặc định khi tạo tin, ví dụ "2 người". */
    private static final String PEOPLE_SUFFIX = " người";

    private final JobPostingAccess access;
    private final JobPostingValidator validator;

    // ----- Màn danh sách -----

    /** Một dòng của bảng; nhãn và cờ thao tác tính sẵn để template chỉ đọc. */
    public InternalJobPostingResponse toListRow(JobPosting posting, User viewer, long totalApplications,
            long newApplications, LocalDateTime now) {
        String viewStatus = posting.getPostingStatus();
        LocalDate today = now.toLocalDate();

        Long daysUntilDeadline = null;
        if (posting.getApplicationDeadline() != null) {
            daysUntilDeadline = JobPostingLabels.daysFromToday(posting.getApplicationDeadline(), today);
        }
        boolean hasApplications = totalApplications > 0;

        return InternalJobPostingResponse.builder()
                .jobPostingId(posting.getJobPostingId())
                .postingTitle(posting.getPostingTitle())
                .departmentName(departmentName(posting.getRequisition()))
                .employmentType(employmentType(posting.getRequisition()))
                .applicationDeadline(posting.getApplicationDeadline())
                .postingDate(posting.getPostingDate())
                .createdByName(fullName(posting.getCreatedBy()))
                .postingNo(posting.getPostingNo())
                .viewStatus(viewStatus)
                .statusLabel(JobPostingLabels.statusLabel(viewStatus))
                .statusTone(JobPostingLabels.statusTone(viewStatus))
                .applicationCount(totalApplications)
                .newApplicationCount(newApplications)
                .daysUntilDeadline(daysUntilDeadline)
                .deadlineState(JobPostingLabels.deadlineState(viewStatus, posting.getApplicationDeadline(), now))
                // Cờ thao tác (luật nằm ở JobPostingAccess, template chỉ đọc cờ)
                .publishable(access.canPublish(viewer, viewStatus))
                .reopenable(access.canReopen(viewer, viewStatus))
                .editable(access.canEdit(viewer, viewStatus))
                .pausable(access.canPause(viewer, viewStatus))
                .closable(access.canClose(viewer, viewStatus))
                .deletable(access.canDelete(viewer, viewStatus, hasApplications))
                .applicationsViewable(hasApplications)
                .build();
    }

    // ----- Trang chi tiết -----

    /** Trang chi tiết; hasOpenPosting quyết định nút "Đăng lại", stats rỗng nếu tin chưa đăng. */
    public InternalJobPostingDetailResponse toDetail(JobPosting posting, User viewer, LocalDateTime now,
            boolean hasApplications, boolean hasOpenPosting, ApplicationStats stats, List<PostingRound> rounds,
            List<AuditLog> activityLogs) {
        JobRequisition requisition = posting.getRequisition();
        String viewStatus = posting.getPostingStatus();
        boolean published = JobPostingAccess.STATUS_PUBLISHED.equals(viewStatus);
        boolean closed = JobPostingAccess.STATUS_CLOSED.equals(viewStatus);
        boolean viewerIsHiringManager = access.isHiringManager(viewer);

        Long daysUntilDeadline = null;
        if (posting.getApplicationDeadline() != null) {
            daysUntilDeadline = JobPostingLabels.daysFromToday(posting.getApplicationDeadline(), now.toLocalDate());
        }
        boolean deadlineNotPassed = daysUntilDeadline != null && daysUntilDeadline >= 0;
        boolean deadlinePassed = daysUntilDeadline != null && daysUntilDeadline < 0;
        boolean deadlineSoon = deadlineNotPassed && daysUntilDeadline <= JobPostingLabels.DEADLINE_SOON_DAYS;

        String salaryDisplay = posting.getSalaryDisplay();
        if (salaryDisplay == null) {
            salaryDisplay = JobPostingLabels.SALARY_NEGOTIABLE;
        }

        InternalJobPostingDetailResponse.InternalJobPostingDetailResponseBuilder detail =
                InternalJobPostingDetailResponse.builder()
                        .jobPostingId(posting.getJobPostingId())
                        .postingTitle(posting.getPostingTitle())
                        .departmentName(departmentName(requisition))
                        .employmentType(employmentType(requisition))
                        .workLocation(posting.getWorkLocation())
                        .salaryDisplay(salaryDisplay)
                        .numberOfPositionsDisplay(posting.getNumberOfPositionsDisplay())
                        .postingNo(posting.getPostingNo())
                        .closedAt(posting.getClosedAt())
                        .jobDescription(posting.getJobDescription())
                        .jobRequirements(posting.getJobRequirements())
                        .benefits(posting.getBenefits())
                        .postingStatus(posting.getPostingStatus())
                        .viewStatus(viewStatus)
                        .postingStatusLabel(JobPostingLabels.statusLabel(viewStatus))
                        .statusTone(JobPostingLabels.statusTone(viewStatus))
                        .postingDate(posting.getPostingDate())
                        .applicationDeadline(posting.getApplicationDeadline())
                        .daysUntilDeadline(daysUntilDeadline)
                        .createdAt(posting.getCreatedAt())
                        .updatedAt(posting.getUpdatedAt())
                        .createdByName(fullName(posting.getCreatedBy()))
                        .activityHistory(toActivityHistory(posting, activityLogs))
                        // Dải thông báo theo trạng thái
                        .draftBanner(JobPostingAccess.STATUS_DRAFT.equals(viewStatus))
                        .pausedBanner(JobPostingAccess.STATUS_PAUSED.equals(viewStatus))
                        .closedBanner(closed)
                        .deadlineSoonBanner(published && deadlineSoon)
                        .deadlinePassedBanner(published && deadlinePassed)
                        .deadlineCountdown(!closed && deadlineNotPassed)
                        .deadlineOverdue(!closed && deadlinePassed)
                        .live(JobPostingAccess.hasBeenPublished(viewStatus))
                        .canManage(access.canManage(viewer))
                        // Cờ thao tác (luật nằm ở JobPostingAccess)
                        .publishable(access.canPublish(viewer, viewStatus))
                        .editable(access.canEdit(viewer, viewStatus))
                        .pausable(access.canPause(viewer, viewStatus))
                        .reopenable(access.canReopen(viewer, viewStatus))
                        .closable(access.canClose(viewer, viewStatus))
                        .deletable(access.canDelete(viewer, viewStatus, hasApplications))
                        .repostable(access.canRepost(viewer, viewStatus, requisition, hasOpenPosting))
                        .publicViewable(posting.isVisibleToCandidates(now))
                        // Khối "Ứng viên tiềm năng"
                        .talentPoolVisible(access.canSeeTalentPool(viewer))
                        .suggestionsLinkLabel(JobPostingLabels.suggestionsLinkLabel(viewerIsHiringManager))
                        .recommendationsLinkLabel(JobPostingLabels.recommendationsLinkLabel(viewerIsHiringManager))
                        // Số liệu hồ sơ
                        .totalApplications(stats.total())
                        .applicationsLast7Days(stats.last7Days())
                        .pendingApplications(stats.pending())
                        .passedCvRoundApplications(stats.passedCvRound())
                        .interviewingApplications(stats.interviewing())
                        .hiredApplications(stats.hired())
                        .recentApplications(stats.recent())
                        .rounds(rounds);

        if (requisition != null) {
            detail.requisitionId(requisition.getRequisitionId())
                    .requisitionCode(requisition.getRequisitionCode())
                    .requisitionTitle(requisition.getTitle())
                    .numberOfPositions(requisition.getNumberOfPositions())
                    .workModelLabel(RequisitionLabels.workModel(requisition.getWorkModel()))
                    .hiringManagerName(fullName(requisition.getHiringManager()));
        } else {
            detail.requisitionCode(JobPostingLabels.MISSING)
                    .requisitionTitle(JobPostingLabels.MISSING)
                    .workModelLabel(JobPostingLabels.MISSING)
                    .hiringManagerName(JobPostingLabels.MISSING);
        }
        return detail.build();
    }

    /** Một lần đăng của yêu cầu, hiện ở mục "Các lần đăng". */
    public PostingRound toPostingRound(JobPosting round, Integer currentId, long applications, long hired) {
        String viewStatus = round.getPostingStatus();
        return new PostingRound(round.getJobPostingId(), round.getPostingNo(), round.getPostingDate(),
                round.getApplicationDeadline(), JobPostingLabels.statusLabel(viewStatus),
                JobPostingLabels.statusTone(viewStatus), applications, hired,
                round.getJobPostingId().equals(currentId));
    }

    /** Lịch sử theo thời gian; tin cũ chưa có audit thì dựng tạm từ ngày tạo / sửa / đăng. */
    public List<ActivityHistoryItem> toActivityHistory(JobPosting posting, List<AuditLog> logs) {
        List<ActivityHistoryItem> history = new ArrayList<>();
        if (logs != null && !logs.isEmpty()) {
            List<AuditLog> chronological = new ArrayList<>(logs);
            chronological.sort(Comparator.comparing(AuditLog::getTimestamp));
            int index = 1;
            for (AuditLog log : chronological) {
                history.add(new ActivityHistoryItem(index, JobPostingLabels.activityLabel(log),
                        fullNameOrSystem(log.getUser()), log.getTimestamp()));
                index++;
            }
            return history;
        }

        String author = fullNameOrSystem(posting.getCreatedBy());
        if (posting.getCreatedAt() != null) {
            history.add(new ActivityHistoryItem(history.size() + 1, "Tạo mới", author, posting.getCreatedAt()));
        }
        if (posting.getUpdatedAt() != null && !posting.getUpdatedAt().equals(posting.getCreatedAt())) {
            history.add(new ActivityHistoryItem(history.size() + 1, "Cập nhật", author, posting.getUpdatedAt()));
        }
        if (JobPostingAccess.STATUS_PUBLISHED.equals(posting.getPostingStatus()) && posting.getPostingDate() != null) {
            history.add(new ActivityHistoryItem(history.size() + 1, "Đăng tin", author, posting.getPostingDate()));
        }
        return history;
    }

    // ----- Form tạo / sửa -----

    /** Form tạo tin từ yêu cầu đã duyệt, điền sẵn nội dung; postingNo = lần đăng lớn nhất + 1. */
    public JobPostingRequest toCreateForm(JobRequisition requisition, int postingNo) {
        JobPostingRequest form = new JobPostingRequest();
        fillSourceInfo(form, requisition);
        form.setPostingNo(postingNo);
        form.setDraftOrNew(true);

        form.setPostingTitle(requisition.getTitle());
        form.setJobDescription(form.getSourceJobDescription());
        form.setJobRequirements(form.getSourceRequirements());
        form.setWorkLocation(form.getSourceWorkLocation());
        form.setBenefits("");
        boolean hasSalary = requisition.getMinSalary() != null || requisition.getMaxSalary() != null;
        if (hasSalary) {
            form.setSalaryMode(JobPostingRequest.SALARY_RANGE);
        } else {
            form.setSalaryMode(JobPostingRequest.SALARY_NEGOTIABLE);
        }
        form.setShowPositions(true);
        String positions = "";
        if (requisition.getNumberOfPositions() != null) {
            positions = requisition.getNumberOfPositions() + PEOPLE_SUFFIX;
        }
        form.setNumberOfPositionsDisplay(positions);
        form.setApplicationDeadline(validator.defaultDeadline());
        form.setAction(JobPostingValidator.ACTION_DRAFT);
        return form;
    }

    /** Form sửa một tin có sẵn: nội dung tin và trạng thái form. */
    public JobPostingRequest toEditForm(JobPosting posting) {
        JobPostingRequest form = toCreateForm(posting.getRequisition(), posting.getPostingNo());
        form.setJobPostingId(posting.getJobPostingId());
        fillFormState(form, posting);
        copyPostingContent(form, posting);
        LocalDate deadline = null;
        if (posting.getApplicationDeadline() != null) {
            deadline = posting.getApplicationDeadline().toLocalDate();
        }
        form.setApplicationDeadline(deadline);

        return form;
    }

    /** Trạng thái form theo trạng thái hiện tại của tin. */
    public void fillFormState(JobPostingRequest form, JobPosting posting) {
        String viewStatus = posting.getPostingStatus();
        form.setPostingNo(posting.getPostingNo());
        form.setDraftOrNew(JobPostingAccess.STATUS_DRAFT.equals(viewStatus));
        form.setPaused(JobPostingAccess.STATUS_PAUSED.equals(viewStatus));
        form.setLive(JobPostingAccess.isLive(viewStatus));
    }

    /** Trạng thái form khi tạo tin mới (hoặc hiện lại form tạo bị lỗi). */
    public void fillNewFormState(JobPostingRequest form, int postingNo) {
        form.setPostingNo(postingNo);
        form.setDraftOrNew(true);
        form.setPaused(false);
        form.setLive(false);
    }

    /** Thông tin yêu cầu nguồn (chỉ hiển thị) và các giá trị gốc cho nút "Khôi phục bản gốc". Luôn nạp lại từ DB. */
    public void fillSourceInfo(JobPostingRequest form, JobRequisition requisition) {
        form.setRequisitionId(requisition.getRequisitionId());
        form.setRequisitionCode(requisition.getRequisitionCode());
        form.setRequisitionTitle(requisition.getTitle());
        form.setDepartmentName(departmentName(requisition));
        form.setRecruitmentRound(recruitmentRound(requisition));
        form.setEmploymentType(RequisitionLabels.employmentType(requisition.getEmploymentType()));
        form.setWorkModel(RequisitionLabels.workModel(requisition.getWorkModel()));
        form.setNumberOfPositions(requisition.getNumberOfPositions());
        form.setHiringManagerName(fullName(requisition.getHiringManager()));
        form.setReasonForHiring(requisition.getReasonForHiring());
        form.setProbationDays(requisition.getProbationDays());
        form.setGender(RequisitionLabels.gender(requisition.getGender()));
        form.setSalaryRangeLabel(RequisitionLabels.salary(requisition.getMinSalary(), requisition.getMaxSalary()));
        form.setSourceWorkLocation(Objects.toString(requisition.getWorkLocation(), ""));
        form.setSourceJobDescription(Objects.toString(requisition.getJobDescription(), ""));
        form.setSourceRequirements(requirementsFromRequisition(requisition));
    }

    /** Chép nội dung một tin có sẵn vào form (dùng cho "Chỉnh sửa" và "Đăng lại"). */
    public void copyPostingContent(JobPostingRequest form, JobPosting posting) {
        form.setPostingTitle(posting.getPostingTitle());
        form.setJobDescription(posting.getJobDescription());
        form.setJobRequirements(posting.getJobRequirements());
        form.setBenefits(posting.getBenefits());
        form.setWorkLocation(posting.getWorkLocation());

        // Nhận lại lựa chọn lương từ chuỗi đã lưu
        String salary = posting.getSalaryDisplay();
        if (salary != null && salary.equals(form.getSalaryRangeLabel())) {
            form.setSalaryMode(JobPostingRequest.SALARY_RANGE);
        } else if (salary == null || JobPostingLabels.SALARY_NEGOTIABLE.equalsIgnoreCase(salary)
                || JobPostingLabels.SALARY_NEGOTIABLE_OLD_SPELLING.equalsIgnoreCase(salary)) {
            form.setSalaryMode(JobPostingRequest.SALARY_NEGOTIABLE);
        } else {
            form.setSalaryMode(JobPostingRequest.SALARY_CUSTOM);
            form.setSalaryCustom(salary);
        }

        boolean showPositions = posting.getNumberOfPositionsDisplay() != null;
        form.setShowPositions(showPositions);
        if (showPositions) {
            form.setNumberOfPositionsDisplay(posting.getNumberOfPositionsDisplay());
        }
    }

    // ----- Các hàm nhỏ -----

    /** Yêu cầu ứng viên điền sẵn: chi tiết yêu cầu của phiếu. */
    private String requirementsFromRequisition(JobRequisition requisition) {
        // dunglt bổ sung: bỏ phần chép "Tiêu chí bắt buộc" (gỡ tiêu chí sàng lọc, 01/11)
        String details = requisition.getRequirementDetails();
        return details == null ? "" : details.trim();
    }

    private static String departmentName(JobRequisition requisition) {
        if (requisition == null || requisition.getDepartment() == null) {
            return JobPostingLabels.DEPARTMENT_MISSING;
        }
        return requisition.getDepartment().getDepartmentName();
    }

    private static String employmentType(JobRequisition requisition) {
        if (requisition == null || requisition.getEmploymentType() == null) {
            return JobPostingLabels.MISSING;
        }
        return RequisitionLabels.employmentType(requisition.getEmploymentType());
    }

    /** Yêu cầu cũ chưa có số đợt thì coi là đợt 1. */
    private static Integer recruitmentRound(JobRequisition requisition) {
        if (requisition.getRecruitmentRound() == null) {
            return 1;
        }
        return requisition.getRecruitmentRound();
    }

    private static String fullName(User user) {
        if (user == null) {
            return JobPostingLabels.MISSING;
        }
        return user.getFullName();
    }

    private static String fullNameOrSystem(User user) {
        if (user == null) {
            return JobPostingLabels.SYSTEM_ACTOR;
        }
        return user.getFullName();
    }
}
