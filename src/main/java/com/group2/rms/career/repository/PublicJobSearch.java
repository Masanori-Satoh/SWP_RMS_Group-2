package com.group2.rms.career.repository;

import com.group2.rms.core.util.ListFilters;
import com.group2.rms.jobposting.entity.JobPosting;
import com.group2.rms.jobposting.service.JobPostingLabels;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Xây dựng JPA Specification tìm kiếm và xếp hạng tin tuyển dụng công khai theo từ khóa và tiêu chí lọc.
 */
public final class PublicJobSearch {

    static final int MAX_TERMS = 5;
    static final String ACCENT_INSENSITIVE = ListFilters.ACCENT_INSENSITIVE;
    static final char ESCAPE = '\\';

    static final int WEIGHT_TITLE = 5;
    static final int WEIGHT_DEPARTMENT = 3;
    static final int WEIGHT_LOCATION = 2;
    static final int WEIGHT_TEXT = 1;

    private PublicJobSearch() {
    }

    /**
     * Tách chuỗi từ khóa thành danh sách các từ đơn không trùng lặp, tối đa theo giới hạn cấu hình.
     */
    public static List<String> terms(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        Set<String> seen = new LinkedHashSet<>();
        List<String> terms = new ArrayList<>();
        for (String word : keyword.trim().split("\\s+")) {
            if (terms.size() < MAX_TERMS && seen.add(word.toLowerCase(Locale.ROOT))) {
                terms.add(word);
            }
        }
        return terms;
    }

    /**
     * Tạo biểu thức LIKE khớp chứa từ khóa đã thoát ký tự đặc biệt.
     */
    static String containsPattern(String term) {
        return "%" + ListFilters.escapeLike(term) + "%";
    }

    /**
     * Tạo Specification lọc tin tuyển dụng công khai theo từ khóa, phòng ban và hình thức làm việc.
     */
    public static Specification<JobPosting> openJobs(List<String> terms, Integer departmentId, String employmentType) {
        return openJobs(terms, departmentId, employmentType, null, null);
    }

    /**
     * Xây dựng câu truy vấn JPA Specification động để lọc tin tuyển dụng và chấm điểm tương đồng từ khóa.
     */
    public static Specification<JobPosting> openJobs(
            List<String> terms,
            Integer departmentId,
            String employmentType,
            String workModel,
            String salaryRange) {
        return (root, query, cb) -> {
            Join<JobPosting, Object> requisition = root.join("requisition", JoinType.LEFT);
            Join<Object, Object> department = requisition.join("department", JoinType.LEFT);
            Expression<LocalDateTime> now = cb.localDateTime();

            List<Predicate> where = new ArrayList<>();
            where.add(cb.equal(root.get("postingStatus"), JobPosting.STATUS_PUBLISHED));

            where.add(cb.or(
                    cb.isNull(root.get("postingDate")),
                    cb.lessThanOrEqualTo(root.<LocalDateTime>get("postingDate"), now)));

            where.add(cb.or(
                    cb.isNull(root.get("applicationDeadline")),
                    cb.greaterThanOrEqualTo(root.<LocalDateTime>get("applicationDeadline"), now)));

            if (departmentId != null) {
                where.add(cb.equal(department.get("departmentId"), departmentId));
            }

            if (employmentType != null && !employmentType.isBlank()) {
                where.add(cb.equal(
                        cb.coalesce(root.<String>get("employmentType"), requisition.<String>get("employmentType")),
                        employmentType.trim()));
            }

            if (workModel != null && !workModel.isBlank()) {
                where.add(cb.equal(
                        cb.coalesce(root.<String>get("workModel"), requisition.<String>get("workModel")),
                        workModel.trim()));
            }

            if (salaryRange != null && !salaryRange.isBlank()) {
                String range = salaryRange.trim();
                Expression<BigDecimal> minSalary = requisition.get("minSalary");
                Expression<BigDecimal> maxSalary = requisition.get("maxSalary");
                Expression<BigDecimal> effMin = cb.coalesce(minSalary, maxSalary);
                Expression<BigDecimal> effMax = cb.coalesce(maxSalary, minSalary);

                Predicate isNegoDisplay = cb.or(
                        cb.equal(root.get("salaryDisplay"), JobPostingLabels.SALARY_NEGOTIABLE),
                        cb.equal(root.get("salaryDisplay"), JobPostingLabels.SALARY_NEGOTIABLE_OLD_SPELLING));
                Predicate isBothNull = cb.and(
                        cb.isNull(minSalary),
                        cb.isNull(maxSalary));

                if ("negotiable".equalsIgnoreCase(range)) {
                    where.add(cb.or(isNegoDisplay, isBothNull));
                } else {
                    where.add(cb.or(
                            cb.isNull(root.get("salaryDisplay")),
                            cb.and(
                                    cb.notEqual(root.get("salaryDisplay"), JobPostingLabels.SALARY_NEGOTIABLE),
                                    cb.notEqual(
                                            root.get("salaryDisplay"),
                                            JobPostingLabels.SALARY_NEGOTIABLE_OLD_SPELLING))));

                    BigDecimal val15 = new BigDecimal("15000000");
                    BigDecimal val25 = new BigDecimal("25000000");
                    BigDecimal val40 = new BigDecimal("40000000");

                    switch (range) {
                        case "under-15" -> where.add(cb.and(
                                cb.isNotNull(effMin),
                                cb.lessThan(effMin, val15)));
                        case "15-25" -> where.add(cb.and(
                                cb.isNotNull(effMin),
                                cb.greaterThanOrEqualTo(effMax, val15),
                                cb.lessThanOrEqualTo(effMin, val25)));
                        case "25-40" -> where.add(cb.and(
                                cb.isNotNull(effMin),
                                cb.greaterThanOrEqualTo(effMax, val25),
                                cb.lessThanOrEqualTo(effMin, val40)));
                        case "above-40" -> where.add(cb.and(
                                cb.isNotNull(effMax),
                                cb.greaterThan(effMax, val40)));
                        default -> {
                        }
                    }
                }
            }

            if (!terms.isEmpty()) {
                List<Field> fields = List.of(
                        new Field(root.<String>get("postingTitle"), WEIGHT_TITLE),
                        new Field(department.<String>get("departmentName"), WEIGHT_DEPARTMENT),
                        new Field(root.<String>get("workLocation"), WEIGHT_LOCATION),
                        new Field(root.<String>get("jobRequirements"), WEIGHT_TEXT),
                        new Field(root.<String>get("jobDescription"), WEIGHT_TEXT));
                HibernateCriteriaBuilder hcb = (HibernateCriteriaBuilder) cb;
                List<Predicate> anyTerm = new ArrayList<>();
                Expression<Integer> score = cb.literal(0);
                for (String term : terms) {
                    String pattern = containsPattern(term);
                    for (Field field : fields) {
                        Predicate hit = cb.like(hcb.collate(field.value(), ACCENT_INSENSITIVE), pattern, ESCAPE);
                        anyTerm.add(hit);
                        score = cb.sum(score, cb.<Integer>selectCase().when(hit, field.weight()).otherwise(0));
                    }
                }
                where.add(cb.or(anyTerm.toArray(Predicate[]::new)));
                if (query != null && !Long.class.equals(query.getResultType())) {
                    query.orderBy(
                            cb.desc(score),
                            cb.desc(root.get("postingDate")),
                            cb.desc(root.get("jobPostingId")));
                }
            }
            return cb.and(where.toArray(Predicate[]::new));
        };
    }

    /**
     * Cấu hình trường tìm kiếm và trọng số điểm tương ứng.
     */
    private record Field(Expression<String> value, int weight) {
    }
}
