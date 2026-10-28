package com.group2.rms.career.repository;

import com.group2.rms.jobposting.entity.JobPosting;
import com.group2.rms.jobposting.service.JobPostingAccess;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Tìm tin tuyển dụng công khai ({@code /jobs}) theo từ khóa, chạy bằng {@code JobPostingRepository.findAll(spec, page)}.
 * <p>
 * Cách khớp (dunglt chốt 2026-10-20): tách từ khóa thành từng từ, tin nào khớp <b>ít nhất một từ</b> ở một trong 5 trường
 * (tên tin, phòng ban, địa điểm, mô tả, yêu cầu) là lấy. Tiếng Việt tách theo âm tiết ("kế toán" = "ke" + "toan") nên
 * "một từ là lấy" kéo theo nhiều tin; vì vậy kết quả <b>xếp theo điểm khớp</b>: mỗi từ khớp cộng điểm theo trường
 * (tên 5, phòng ban 3, địa điểm 2, yêu cầu/mô tả 1), tin khớp nhiều từ ở tên đứng đầu, cùng điểm thì mới đăng trước.
 * So sánh không phân biệt hoa thường và dấu ({@code Latin1_General_100_CI_AI}: "ke toan" khớp "Kế toán", "dien" khớp
 * "điện"); ký tự đại diện của LIKE trong từ khóa ({@code % _ [}) được thoát nên "%" không khớp mọi tin.
 * Xếp hạng nằm trong SQL nên phân trang vẫn đúng (trang 1 là các tin khớp nhất).
 */
public final class PublicJobSearch {

    /** Tối đa số từ lấy từ ô tìm kiếm (chặn chuỗi dài làm câu SQL phình). */
    static final int MAX_TERMS = 5;
    static final String ACCENT_INSENSITIVE = "Latin1_General_100_CI_AI";
    static final char ESCAPE = '\\';

    static final int WEIGHT_TITLE = 5;
    static final int WEIGHT_DEPARTMENT = 3;
    static final int WEIGHT_LOCATION = 2;
    static final int WEIGHT_TEXT = 1;

    private PublicJobSearch() {
    }

    /** Các từ của ô tìm kiếm: tách theo khoảng trắng, bỏ trùng (không phân biệt hoa thường), tối đa {@link #MAX_TERMS}. */
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

    /** Mẫu LIKE "chứa từ này", đã thoát {@code \ % _ [} bằng {@link #ESCAPE}. */
    static String containsPattern(String term) {
        StringBuilder pattern = new StringBuilder("%");
        for (char c : term.toCharArray()) {
            if (c == ESCAPE || c == '%' || c == '_' || c == '[') {
                pattern.append(ESCAPE);
            }
            pattern.append(c);
        }
        return pattern.append('%').toString();
    }

    /**
     * Tin đang mở cho ứng viên (đã đăng, tới ngày đăng, chưa quá hạn nộp), lọc phòng ban / hình thức nếu có, và khớp từ
     * khóa nếu có. Có từ khóa thì tự đặt thứ tự theo điểm khớp: khi đó gọi với {@code Pageable} <b>không sort</b>, vì sort
     * của {@code Pageable} sẽ đè thứ tự này.
     */
    public static Specification<JobPosting> openJobs(List<String> terms, Integer departmentId, String employmentType) {
        return (root, query, cb) -> {
            Join<JobPosting, Object> requisition = root.join("requisition", JoinType.LEFT);
            Join<Object, Object> department = requisition.join("department", JoinType.LEFT);
            Expression<LocalDateTime> now = cb.localDateTime();

            List<Predicate> where = new ArrayList<>();
            where.add(cb.equal(root.get("postingStatus"), JobPostingAccess.STATUS_PUBLISHED));
            // Giữ tin cũ có PostingDate ở tương lai ngoài trang công khai cho tới khi dữ liệu được xử lý.
            where.add(cb.or(cb.isNull(root.get("postingDate")),
                    cb.lessThanOrEqualTo(root.<LocalDateTime>get("postingDate"), now)));
            where.add(cb.or(cb.isNull(root.get("applicationDeadline")),
                    cb.greaterThanOrEqualTo(root.<LocalDateTime>get("applicationDeadline"), now)));
            if (departmentId != null) {
                where.add(cb.equal(department.get("departmentId"), departmentId));
            }
            if (employmentType != null) {
                where.add(cb.equal(requisition.get("employmentType"), employmentType));
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
                // Câu đếm (count) của phân trang không được có ORDER BY.
                if (query != null && !Long.class.equals(query.getResultType())) {
                    query.orderBy(cb.desc(score), cb.desc(root.get("postingDate")), cb.desc(root.get("jobPostingId")));
                }
            }
            return cb.and(where.toArray(Predicate[]::new));
        };
    }

    /** Một trường được dò và điểm cộng khi một từ khớp trường đó. */
    private record Field(Expression<String> value, int weight) {
    }
}
