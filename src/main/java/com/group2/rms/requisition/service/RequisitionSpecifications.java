package com.group2.rms.requisition.service;

import com.group2.rms.core.util.ListFilters;
import com.group2.rms.jobposting.entity.JobPosting;
import com.group2.rms.requisition.entity.JobRequisition;

import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;

/**
 * Các điều kiện WHERE dùng cho màn danh sách yêu cầu tuyển dụng.
 */
public final class RequisitionSpecifications {

    private RequisitionSpecifications() {
        // Chỉ chứa hàm static, không tạo đối tượng
    }

    /** Tên vị trí HOẶC tên phòng ban có chứa từ khóa (không phân biệt hoa thường). */
    public static Specification<JobRequisition> titleOrDepartmentContains(String keyword) {
        String pattern = ListFilters.containsPattern(keyword);
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("title")), pattern, '\\'),
                cb.like(cb.lower(root.join("department", JoinType.LEFT).get("departmentName")), pattern, '\\'));
    }

    /** Trạng thái duyệt nằm trong danh sách đã chọn. */
    public static Specification<JobRequisition> statusIn(List<String> statuses) {
        return (root, query, cb) -> root.get("approvalStatus").in(statuses);
    }

    /** Thuộc phòng ban đã chọn. */
    public static Specification<JobRequisition> departmentIs(Integer departmentId) {
        return (root, query, cb) -> cb.equal(root.get("department").get("departmentId"), departmentId);
    }

    /** Do Hiring Manager đã chọn tạo. */
    public static Specification<JobRequisition> creatorIs(Integer userId) {
        return (root, query, cb) -> cb.equal(root.get("hiringManager").get("userId"), userId);
    }

    /** Ngày tạo từ đầu ngày "from" trở đi. */
    public static Specification<JobRequisition> createdOnOrAfter(LocalDate from) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from.atStartOfDay());
    }

    /** Ngày tạo đến hết ngày "to" (tức là trước 00:00 của ngày hôm sau). */
    public static Specification<JobRequisition> createdOnOrBefore(LocalDate to) {
        return (root, query, cb) -> cb.lessThan(root.get("createdAt"), to.plusDays(1).atStartOfDay());
    }

    /** Yêu cầu chưa có tin tuyển dụng nào (NOT EXISTS trong bảng JobPosting). */
    public static Specification<JobRequisition> hasNoPosting() {
        return (root, query, cb) -> {
            Subquery<Integer> posting = query.subquery(Integer.class);
            Root<JobPosting> p = posting.from(JobPosting.class);
            posting.select(p.get("jobPostingId")).where(cb.equal(p.get("requisition"), root));
            return cb.not(cb.exists(posting));
        };
    }

    /**
     * Tin tuyển dụng MỚI NHẤT của yêu cầu (JobPostingId lớn nhất) đang ở trạng thái đã chọn.
     * Một yêu cầu có thể đăng nhiều lần (PostingNo 1, 2...), nên chỉ xét lần mới nhất.
     */
    public static Specification<JobRequisition> latestPostingStatusIs(String postingStatus) {
        return (root, query, cb) -> {
            // Bước 1: tìm id tin mới nhất của yêu cầu đang xét
            Subquery<Integer> latestId = query.subquery(Integer.class);
            Root<JobPosting> latest = latestId.from(JobPosting.class);
            latestId.select(cb.max(latest.get("jobPostingId")))
                    .where(cb.equal(latest.get("requisition"), root));

            // Bước 2: tin có id đó phải đang ở trạng thái được chọn
            Subquery<Integer> match = query.subquery(Integer.class);
            Root<JobPosting> p = match.from(JobPosting.class);
            match.select(p.get("jobPostingId"))
                    .where(cb.equal(p.get("jobPostingId"), latestId),
                            cb.equal(p.get("postingStatus"), postingStatus));
            return cb.exists(match);
        };
    }
}
