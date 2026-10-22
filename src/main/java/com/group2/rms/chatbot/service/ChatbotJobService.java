package com.group2.rms.chatbot.service;

import com.group2.rms.career.dto.PublicJobDetailResponse;
import com.group2.rms.career.service.CareerPortalService;
import com.group2.rms.chatbot.dto.ChatResponse.JobCard;
import com.group2.rms.chatbot.dto.ChatResponse.JobDetail;
import com.group2.rms.core.exception.ResourceNotFoundException;
import com.group2.rms.jobposting.entity.JobPosting;
import com.group2.rms.jobposting.repository.JobPostingRepository;
import com.group2.rms.jobposting.service.JobPostingAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/*
 * Phần việc làm của chatbot: tìm tin theo từ khóa, chi tiết một tin, gợi ý tin theo lịch sử ứng tuyển.
 * Chỉ đọc tin đang nhận hồ sơ (Published, đã tới giờ đăng, chưa hết hạn). Không ghi gì vào DB.
 * Mỗi lần chỉ chạy một câu lấy tin đang mở (kèm phòng ban), lọc trong bộ nhớ vì số tin đang mở nhỏ.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatbotJobService {

    /** Số thẻ tin tối đa trong một câu trả lời (khung chat hẹp). */
    public static final int MAX_JOB_CARDS = 3;
    /** Số dòng yêu cầu / quyền lợi tối đa trong chi tiết tin. */
    public static final int MAX_DETAIL_LINES = 3;

    /** Từ hay gặp trong câu hỏi nhưng không dùng để lọc tin (đã bỏ dấu). */
    static final Set<String> STOP_WORDS = Set.of(
            "tim", "kiem", "viec", "lam", "cong", "job", "jobs", "vi", "tri", "tuyen", "dung", "dang", "nao", "khong",
            "ko", "k", "co", "cho", "toi", "minh", "em", "anh", "chi", "ban", "o", "tai", "khu", "vuc", "can", "muon",
            "nhung", "cac", "gi", "la", "va", "hay", "voi", "ve", "mot", "nhe", "a", "oi", "giup", "xem", "danh",
            "sach", "hien", "nay", "nganh", "mang", "thi", "duoc", "khac", "moi", "nhat", "con", "the");

    private final JobPostingRepository postings;
    private final CareerPortalService careerPortalService;
    private final Clock clock;

    /** Kết quả tìm: tổng số tin khớp và các thẻ hiện ra (tối đa MAX_JOB_CARDS). */
    public record SearchResult(int total, List<JobCard> jobs) {
    }

    /** Kết quả gợi ý: {@code fromHistory} = gợi ý dựa trên tin đã nộp (không phải chỉ tin mới nhất). */
    public record RecommendResult(boolean fromHistory, List<JobCard> jobs) {
    }

    /** Chi tiết tin cho khung chat; {@code canApply} = còn nhận hồ sơ và người xem chưa nộp tin này. */
    public record DetailResult(JobDetail detail, boolean canApply) {
    }

    /**
     * Tin đang mở có chứa MỌI từ có nghĩa trong câu hỏi (ở tên vị trí, địa điểm, phòng ban hoặc hình thức).
     * Câu hỏi không có từ có nghĩa (vd "tìm việc") → mọi tin đang mở, mới nhất trước.
     */
    public SearchResult search(String message) {
        List<String> terms = searchTerms(message);
        List<JobPosting> matched = new ArrayList<>();
        for (JobPosting job : openPostings()) {
            if (matchesAll(job, terms)) {
                matched.add(job);
            }
        }
        List<JobCard> cards = new ArrayList<>();
        for (JobPosting job : firstN(matched)) {
            cards.add(toCard(job, null));
        }
        return new SearchResult(matched.size(), cards);
    }

    /** Có từ có nghĩa để lọc không (để phân biệt "tìm việc" với "tìm việc Java"). */
    public boolean hasSearchTerms(String message) {
        return !searchTerms(message).isEmpty();
    }

    /** Chi tiết một tin còn hiện cho ứng viên; tin không tồn tại / chưa đăng / đã đóng → rỗng. */
    public Optional<DetailResult> detail(Integer jobPostingId, String username) {
        PublicJobDetailResponse job;
        try {
            job = careerPortalService.getPublishedJobDetail(jobPostingId, username);
        } catch (ResourceNotFoundException e) {
            return Optional.empty();
        }
        JobDetail detail = new JobDetail(job.id(), job.postingTitle(), job.departmentName(), job.workLocation(),
                blankToNull(job.salaryDisplay()), ChatbotLabels.deadline(job.applicationDeadline()),
                firstLines(job.jobRequirementsList()), firstLines(job.benefitsList()));
        return Optional.of(new DetailResult(detail, job.isAcceptingApplications() && !job.hasApplied()));
    }

    /**
     * Gợi ý tin đang mở mà ứng viên chưa nộp: cùng phòng ban với tin đã nộp trước, rồi cùng địa điểm.
     * Chưa nộp tin nào, hoặc không có tin nào giống → tin mới nhất.
     *
     * @param appliedPostingIds id các tin ứng viên đã nộp (lấy từ hồ sơ của chính ứng viên)
     */
    public RecommendResult recommend(Collection<Integer> appliedPostingIds) {
        Set<Integer> appliedIds = new HashSet<>(appliedPostingIds);
        List<JobPosting> notApplied = new ArrayList<>();
        for (JobPosting job : openPostings()) {
            if (!appliedIds.contains(job.getJobPostingId())) {
                notApplied.add(job);
            }
        }
        if (appliedIds.isEmpty()) {
            return new RecommendResult(false, latestCards(notApplied));
        }

        // Rule: phòng ban / địa điểm lấy từ cả tin đã đóng, vì ứng viên có thể đã nộp tin cũ
        Set<Integer> appliedDepartments = new HashSet<>();
        Set<String> appliedLocations = new HashSet<>();
        for (JobPosting applied : postings.findByJobPostingIdIn(appliedIds)) {
            Integer departmentId = departmentIdOf(applied);
            if (departmentId != null) {
                appliedDepartments.add(departmentId);
            }
            String location = ChatbotText.normalize(applied.getWorkLocation());
            if (!location.isEmpty()) {
                appliedLocations.add(location);
            }
        }

        List<JobCard> cards = new ArrayList<>();
        for (JobPosting job : notApplied) {
            if (cards.size() < MAX_JOB_CARDS && appliedDepartments.contains(departmentIdOf(job))) {
                cards.add(toCard(job, ChatbotLabels.REASON_SAME_DEPARTMENT));
            }
        }
        for (JobPosting job : notApplied) {
            boolean alreadyShown = appliedDepartments.contains(departmentIdOf(job));
            boolean sameLocation = appliedLocations.contains(ChatbotText.normalize(job.getWorkLocation()));
            if (cards.size() < MAX_JOB_CARDS && !alreadyShown && sameLocation) {
                cards.add(toCard(job, ChatbotLabels.REASON_SAME_LOCATION));
            }
        }
        if (cards.isEmpty()) {
            return new RecommendResult(false, latestCards(notApplied));
        }
        return new RecommendResult(true, cards);
    }

    private List<JobPosting> openPostings() {
        return postings.findOpenWithDepartment(JobPostingAccess.STATUS_PUBLISHED, LocalDateTime.now(clock));
    }

    private static List<JobCard> latestCards(List<JobPosting> jobs) {
        List<JobCard> cards = new ArrayList<>();
        for (JobPosting job : firstN(jobs)) {
            cards.add(toCard(job, ChatbotLabels.REASON_LATEST));
        }
        return cards;
    }

    /** Từ có nghĩa của câu hỏi: đã bỏ dấu, bỏ từ thường gặp, bỏ trùng. */
    static List<String> searchTerms(String message) {
        List<String> terms = new ArrayList<>();
        for (String word : ChatbotText.words(ChatbotText.normalize(message))) {
            if (!STOP_WORDS.contains(word) && !terms.contains(word)) {
                terms.add(word);
            }
        }
        return terms;
    }

    private static boolean matchesAll(JobPosting job, List<String> terms) {
        String haystack = ChatbotText.normalize(String.join(" ", job.getPostingTitle(), nullToEmpty(job.getWorkLocation()),
                nullToEmpty(departmentNameOf(job)), nullToEmpty(employmentTypeOf(job))));
        for (String term : terms) {
            if (!ChatbotText.containsPhrase(haystack, term)) {
                return false;
            }
        }
        return true;
    }

    private static JobCard toCard(JobPosting job, String reason) {
        return new JobCard(job.getJobPostingId(), job.getPostingTitle(), departmentNameOf(job), job.getWorkLocation(),
                blankToNull(job.getSalaryDisplay()), ChatbotLabels.deadline(job.getApplicationDeadline()),
                ChatbotLabels.jobDetailHref(job.getJobPostingId()), reason);
    }

    private static List<JobPosting> firstN(List<JobPosting> jobs) {
        return jobs.subList(0, Math.min(MAX_JOB_CARDS, jobs.size()));
    }

    private static List<String> firstLines(List<String> lines) {
        if (lines == null) {
            return List.of();
        }
        return lines.subList(0, Math.min(MAX_DETAIL_LINES, lines.size()));
    }

    private static Integer departmentIdOf(JobPosting job) {
        if (job.getRequisition() == null || job.getRequisition().getDepartment() == null) {
            return null;
        }
        return job.getRequisition().getDepartment().getDepartmentId();
    }

    private static String departmentNameOf(JobPosting job) {
        if (job.getRequisition() == null || job.getRequisition().getDepartment() == null) {
            return null;
        }
        return job.getRequisition().getDepartment().getDepartmentName();
    }

    private static String employmentTypeOf(JobPosting job) {
        if (job.getRequisition() == null) {
            return null;
        }
        return job.getRequisition().getEmploymentType();
    }

    private static String nullToEmpty(String value) {
        if (value == null) {
            return "";
        }
        return value;
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }
}
