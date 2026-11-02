package com.group2.rms.jobposting.service;

import com.group2.rms.candidate.domain.entity.Application;
import com.group2.rms.jobposting.entity.JobPosting;
import com.group2.rms.core.util.ListFilters;
import com.group2.rms.user.entity.User;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/*
 * SPECIFICATIONS: điều kiện WHERE của màn danh sách tin; service ghép bằng .and(...).
 */
public final class JobPostingSpecifications {

    /** Id không tồn tại, dùng khi HM chưa được gán phòng ban. */
    private static final Integer NO_DEPARTMENT_ID = -1;

    private JobPostingSpecifications() {
        // Chỉ chứa hàm static
    }

    /** Tiêu đề tin HOẶC tên phòng ban có chứa từ khóa (không phân biệt hoa thường). */
    public static Specification<JobPosting> titleOrDepartmentContains(String keyword) {
        String pattern = ListFilters.containsPattern(keyword);
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("postingTitle")), pattern, '\\'),
                cb.like(cb.lower(root.join("requisition", JoinType.LEFT).join("department", JoinType.LEFT)
                        .get("departmentName")), pattern, '\\'));
    }

    /** Trạng thái tin nằm trong danh sách đã chọn. */
    public static Specification<JobPosting> viewStatusIn(List<String> viewStatuses) {
        return (root, query, cb) -> {
            List<Predicate> anyOf = new ArrayList<>();
            for (String viewStatus : viewStatuses) {
                anyOf.add(cb.equal(root.get("postingStatus"), viewStatus));
            }
            return cb.or(anyOf.toArray(new Predicate[0]));
        };
    }

    /** Bỏ tin nháp (HM và Director không xem tin nháp). */
    public static Specification<JobPosting> notDraft() {
        return (root, query, cb) -> cb.notEqual(root.get("postingStatus"), JobPostingAccess.STATUS_DRAFT);
    }

    /** Tin thuộc phòng ban của HM: phòng ban HM đang làm, hoặc phòng ban HM là trưởng phòng. */
    public static Specification<JobPosting> inDepartmentOf(User hiringManager) {
        // HM chưa có phòng ban: không khớp phòng nào (id -1), chỉ còn điều kiện "là trưởng phòng"
        Integer departmentId = NO_DEPARTMENT_ID;
        if (hiringManager.getDepartment() != null) {
            departmentId = hiringManager.getDepartment().getDepartmentId();
        }
        Integer ownDepartmentId = departmentId;
        return (root, query, cb) -> {
            var department = root.join("requisition").join("department");
            var manager = department.join("manager", JoinType.LEFT);
            return cb.or(cb.equal(department.get("departmentId"), ownDepartmentId),
                    cb.equal(manager.get("userId"), hiringManager.getUserId()));
        };
    }

    public static Specification<JobPosting> departmentIs(Integer departmentId) {
        return (root, query, cb) -> cb.equal(
                root.join("requisition", JoinType.LEFT).join("department", JoinType.LEFT).get("departmentId"), departmentId);
    }

    public static Specification<JobPosting> creatorIs(Integer userId) {
        return (root, query, cb) -> cb.equal(root.get("createdBy").get("userId"), userId);
    }

    /** Nhóm hạn nộp: past = đã qua hạn; open = chưa qua hạn hoặc không có hạn. */
    public static Specification<JobPosting> deadlineIn(String bucket, LocalDateTime now) {
        return (root, query, cb) -> {
            if (JobPostingLabels.DEADLINE_PAST.equals(bucket)) {
                return cb.lessThan(root.get("applicationDeadline"), now);
            }
            return cb.or(cb.isNull(root.get("applicationDeadline")),
                    cb.greaterThanOrEqualTo(root.get("applicationDeadline"), now));
        };
    }

    /** Ngày đăng từ (tính cả ngày này). */
    public static Specification<JobPosting> postedOnOrAfter(LocalDate from) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("postingDate"), from.atStartOfDay());
    }

    /** Ngày đăng đến (tính cả ngày này: nhỏ hơn 0 giờ của ngày hôm sau). */
    public static Specification<JobPosting> postedOnOrBefore(LocalDate to) {
        return (root, query, cb) -> cb.lessThan(root.get("postingDate"), to.plusDays(1).atStartOfDay());
    }

    /** Sắp xếp "Nhiều hồ sơ nhất"; câu COUNT của Spring không được có ORDER BY nên bỏ qua khi đếm. */
    public static Specification<JobPosting> orderByApplicationCount() {
        return (root, query, cb) -> {
            Class<?> resultType = query.getResultType();
            if (resultType != Long.class && resultType != long.class) {
                Subquery<Long> applicationCount = query.subquery(Long.class);
                Root<Application> application = applicationCount.from(Application.class);
                applicationCount.select(cb.count(application)).where(cb.equal(application.get("jobPosting"), root));
                query.orderBy(cb.desc(applicationCount), cb.desc(root.get("jobPostingId")));
            }
            return cb.conjunction();
        };
    }
}
