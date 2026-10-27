package com.group2.rms.candidate.application.portal.service;

import com.group2.rms.candidate.application.portal.dto.MyApplicationStep;
import com.group2.rms.candidate.domain.rule.ApplicationProgressResolver;
import com.group2.rms.core.web.TimelineItem;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Quy đổi dữ kiện một đơn thành điều ứng viên thấy ở "Đơn của tôi" (hàm thuần, không đọc DB): thanh 4 bước
 * Nộp CV → Phỏng vấn → Thư mời → Nhận việc, chữ trạng thái kèm một dòng giải thích, nút việc tiếp theo, đơn gấp,
 * hành trình.
 * <p>
 * Nguyên tắc (L6 luồng 5): chữ ứng viên thấy chỉ đổi khi ứng viên đã được báo (có lịch, có thư mời). Dữ kiện đầu vào
 * đã bỏ sẵn phần nội bộ (offer chưa gửi, điểm AI, lượt duyệt, kết quả chốt phỏng vấn: GBR-02), nên mọi bước duyệt
 * nội bộ trước phỏng vấn đều là "Đã nộp CV", và HM chốt "Đạt" mà HR chưa gửi offer thì đơn vẫn ở Phỏng vấn. Bảng
 * quy đổi là bảng 5.3 của {@code nghiep-vu.md}, xét từ trên xuống, gặp điều kiện đầu tiên đúng thì dừng.
 * Giống {@link ApplicationProgressResolver} nhưng cho góc nhìn ứng viên.
 */
public final class MyApplicationJourney {

    static final String WITHDRAWN = "Withdrawn";
    static final String POSTING_CLOSED = "PostingClosed";
    static final String INTERVIEWS_HREF = "/portal/interviews";
    static final String JOBS_HREF = "/jobs";

    /** Lịch phỏng vấn bắt đầu trong khoảng này tính từ bây giờ là đơn gấp (ux mục 2). */
    static final Duration INTERVIEW_URGENT_WITHIN = Duration.ofHours(48);

    /** Câu giải thích chung cho mọi bước duyệt nội bộ trước phỏng vấn: không lộ bước nào đang xét (L6). */
    static final String SUBMITTED_NOTE =
            "Hồ sơ của bạn đang được xem xét. Bạn sẽ nhận được thông báo khi có lịch phỏng vấn.";

    private static final DateTimeFormatter DAY_MONTH = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter TIME_DAY_MONTH = DateTimeFormatter.ofPattern("HH:mm dd/MM");

    private MyApplicationJourney() {
    }

    /** 4 bước ứng viên thấy, theo thứ tự luồng (Q5.4). */
    public enum Round {
        SUBMIT("Nộp CV"),
        INTERVIEW("Phỏng vấn"),
        OFFER("Thư mời"),
        HIRED("Nhận việc");

        private final String label;

        Round(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    /** Một lịch phỏng vấn ({@code InterviewSchedule}), kể cả lịch đã hủy. */
    public record Interview(LocalDateTime start, String format, String status) {
    }

    /** Offer ứng viên được thấy ({@code Sent_Candidate}, {@code Accepted}, {@code Declined}). */
    public record Offer(Integer offerId, String status, LocalDateTime responseDeadline, LocalDateTime at) {
    }

    /** Dấu dừng trong {@code AuditLog}: {@code Withdrawn} hoặc {@code PostingClosed}, kèm trạng thái ngay trước đó. */
    public record Mark(String kind, String oldStatus, LocalDateTime at) {
    }

    /**
     * Dữ kiện một đơn.
     *
     * @param interviews mọi lịch của đơn, cũ nhất trước
     * @param offer      {@code null} nếu ứng viên chưa thấy offer nào
     * @param marks      dấu dừng của đơn, cũ nhất trước
     */
    public record Facts(String applicationStatus, LocalDateTime submittedAt, List<Interview> interviews,
                        Offer offer, List<Mark> marks) {
    }

    /**
     * Điều trang hiển thị cho một đơn.
     *
     * @param tone           màu chữ trạng thái: {@code neutral}, {@code success}, {@code warning}, {@code danger}
     * @param statusNote     dòng giải thích dưới chữ trạng thái; {@code null} nếu không có
     * @param actionLabel    nút việc tiếp theo; {@code null} nếu không có (nút Rút đơn do Service quyết định riêng)
     * @param journey        các mốc ứng viên được biết, cũ nhất trước
     * @param lastActivityAt mốc mới nhất đã xảy ra (không tính lịch hẹn ở tương lai): thứ tự mặc định của danh sách
     * @param urgentUntil    hạn của việc cần làm ngay (hạn trả lời thư mời, giờ phỏng vấn); {@code null} nếu không gấp
     * @param urgentLabel    dòng "còn …" của việc gấp; {@code null} nếu không gấp
     * @param ended          đơn đã dừng hoặc đã nhận việc (tab "Đã kết thúc")
     */
    public record View(List<MyApplicationStep> steps, String statusLabel, String statusNote, String tone,
                       String actionLabel, String actionHref, List<TimelineItem> journey,
                       LocalDateTime lastActivityAt, LocalDateTime urgentUntil, String urgentLabel, boolean ended) {

        /** Số thứ tự (1–4) của bước đơn đang đứng hoặc dừng; xong cả 4 bước thì là bước cuối. */
        public int stepNumber() {
            for (int i = 0; i < steps.size(); i++) {
                String state = steps.get(i).state();
                if ("current".equals(state) || "closed".equals(state)) {
                    return i + 1;
                }
            }
            return steps.size();
        }

        /** Bước ứng với {@link #stepNumber()}: nhãn và trạng thái cho tag trên thẻ. */
        public MyApplicationStep step() {
            return steps.get(stepNumber() - 1);
        }
    }

    public static View resolve(Facts facts, LocalDateTime now) {
        Status status = status(facts, now);
        List<TimelineItem> journey = journey(facts);
        Optional<Urgency> urgency = status.mode() == Mode.IN_FLOW ? urgency(facts, now) : Optional.empty();
        return new View(steps(status.round(), status.mode()), status.label(), status.note(), status.tone(),
                status.actionLabel(), status.actionHref(), journey, lastActivityAt(journey, facts.submittedAt(), now),
                urgency.map(Urgency::until).orElse(null), urgency.map(Urgency::label).orElse(null),
                status.mode() != Mode.IN_FLOW);
    }

    /**
     * Lần cập nhật gần nhất ứng viên thấy được: mốc mới nhất trên hành trình đã xảy ra. Không dùng
     * {@code Application.UpdatedAt} vì nó đổi cả khi HR/HM duyệt nội bộ (thẻ sẽ nhảy lên mà ứng viên không thấy gì mới).
     */
    static LocalDateTime lastActivityAt(List<TimelineItem> journey, LocalDateTime submittedAt, LocalDateTime now) {
        return journey.stream()
                .map(TimelineItem::at)
                .filter(at -> at != null && !at.isAfter(now))
                .max(Comparator.naturalOrder())
                .orElse(submittedAt);
    }

    /** Đơn đang đi ({@code IN_FLOW}), đã dừng ở một bước ({@code STOPPED}), hay đã xong cả 4 bước ({@code FINISHED}). */
    enum Mode { IN_FLOW, STOPPED, FINISHED }

    private record Status(Round round, Mode mode, String label, String note, String tone,
                          String actionLabel, String actionHref) {
    }

    private record Urgency(LocalDateTime until, String label) {
    }

    // ---- Chữ trạng thái + bước (bảng 5.3) ----

    private static Status status(Facts facts, LocalDateTime now) {
        Offer offer = facts.offer();
        return switch (facts.applicationStatus()) {
            case "Rejected" -> stopped(facts);
            case "Hired" -> new Status(Round.HIRED, Mode.FINISHED, "Đã nhận việc", "Chúc mừng bạn!", "success",
                    offer == null ? null : "Xem thư mời", offerHref(offer));
            case "Offered" -> offered(offer);
            case "HM_Passed", "Interviewing" -> interviewing(facts.interviews(), now);
            case "Applied", "AI_Screened", "HR_Passed" -> submitted();
            // Trạng thái lạ (ngoài CK_Application_Status): không đoán, chỉ báo đang xử lý.
            default -> new Status(Round.SUBMIT, Mode.IN_FLOW, "Đang xử lý", null, "neutral", null, null);
        };
    }

    /** Đơn {@code Rejected}: vì sao dừng và dừng ở bước nào. */
    private static Status stopped(Facts facts) {
        Optional<Mark> withdrawn = mark(facts, WITHDRAWN);
        if (withdrawn.isPresent()) {
            LocalDateTime at = withdrawn.get().at();
            String note = at == null ? "Bạn đã rút hồ sơ." : "Bạn đã rút hồ sơ ngày " + at.format(DAY_MONTH) + ".";
            return new Status(roundWhenStopped(facts, withdrawn.get().oldStatus()), Mode.STOPPED, "Đã tự rút hồ sơ",
                    note, "neutral", null, null);
        }
        Optional<Mark> closed = mark(facts, POSTING_CLOSED);
        if (closed.isPresent()) {
            return new Status(roundWhenStopped(facts, closed.get().oldStatus()), Mode.STOPPED, "Vị trí đã ngừng tuyển",
                    "Công ty đã ngừng tuyển vị trí này nên hồ sơ của bạn dừng lại. Bạn có thể ứng tuyển vị trí khác.",
                    "neutral", "Xem vị trí đang tuyển", JOBS_HREF);
        }
        Offer offer = facts.offer();
        if (offer != null && "Declined".equals(offer.status())) {
            return new Status(Round.OFFER, Mode.STOPPED, "Đã từ chối thư mời", null, "neutral",
                    "Xem thư mời", offerHref(offer));
        }
        if (hasActiveInterview(facts.interviews())) {
            return new Status(Round.INTERVIEW, Mode.STOPPED, "Không đạt phỏng vấn",
                    "Cảm ơn bạn đã tham gia phỏng vấn.", "danger", null, null);
        }
        // Q2.4: không nêu lý do (GBR-02), không hứa "lưu hồ sơ".
        return new Status(Round.SUBMIT, Mode.STOPPED, "Không đạt vòng CV",
                "Cảm ơn bạn đã quan tâm. Hồ sơ của bạn chưa phù hợp với vị trí này.", "danger",
                "Xem vị trí khác", JOBS_HREF);
    }

    /**
     * Bước dừng khi đơn bị dừng giữa chừng (đóng tin, ứng viên tự rút): có offer → Thư mời; có lịch chưa hủy →
     * Phỏng vấn; còn lại → Nộp CV. Đóng tin hủy luôn các lịch chưa diễn ra, nên xét thêm trạng thái ngay trước khi
     * dừng ({@code OldValue} của dấu) và lấy bước xa hơn. {@code HM_Passed} là Nộp CV: chưa có lịch thì ứng viên chưa
     * được báo gì (L6; xếp lịch chuyển đơn sang {@code Interviewing}).
     */
    static Round roundWhenStopped(Facts facts, String statusBeforeStop) {
        Round fromFacts = facts.offer() != null ? Round.OFFER
                : hasActiveInterview(facts.interviews()) ? Round.INTERVIEW : Round.SUBMIT;
        Round fromStatus = statusBeforeStop == null ? Round.SUBMIT : switch (statusBeforeStop) {
            case "Offered" -> Round.OFFER;
            case "Interviewing" -> Round.INTERVIEW;
            default -> Round.SUBMIT;
        };
        return fromFacts.compareTo(fromStatus) >= 0 ? fromFacts : fromStatus;
    }

    /** Đơn {@code Offered}. */
    private static Status offered(Offer offer) {
        if (offer != null && "Sent_Candidate".equals(offer.status())) {
            String note = offer.responseDeadline() == null
                    ? "Vui lòng xem và trả lời thư mời."
                    : "Vui lòng trả lời trước " + offer.responseDeadline().format(DAY_MONTH) + ".";
            return new Status(Round.OFFER, Mode.IN_FLOW, "Có thư mời", note, "warning",
                    "Trả lời thư mời", offerHref(offer));
        }
        // Dữ liệu lệch: trạng thái đã là Offered nhưng ứng viên chưa thấy offer nào.
        return new Status(Round.OFFER, Mode.IN_FLOW, "Có thư mời", "Bộ phận tuyển dụng sẽ liên hệ với bạn.",
                "neutral", null, null);
    }

    /** Đơn {@code HM_Passed}/{@code Interviewing}: xét lịch sắp tới trước, rồi lịch đã qua; chỉ có lịch đã hủy = chưa có lịch. */
    private static Status interviewing(List<Interview> interviews, LocalDateTime now) {
        Optional<Interview> upcoming = upcomingInterview(interviews, now);
        if (upcoming.isPresent()) {
            String format = formatLabel(upcoming.get().format());
            String note = (format == null ? "" : format + ". ") + "Xem chi tiết ở Lịch phỏng vấn.";
            return new Status(Round.INTERVIEW, Mode.IN_FLOW, "Phỏng vấn " + upcoming.get().start().format(TIME_DAY_MONTH),
                    note, "warning", "Xem lịch phỏng vấn", INTERVIEWS_HREF);
        }
        if (hasActiveInterview(interviews)) {
            return new Status(Round.INTERVIEW, Mode.IN_FLOW, "Đã phỏng vấn, chờ kết quả",
                    "Kết quả sẽ được thông báo cho bạn.", "neutral", "Xem lịch phỏng vấn", INTERVIEWS_HREF);
        }
        return submitted();
    }

    /** Mọi bước duyệt nội bộ trước khi có lịch phỏng vấn. */
    private static Status submitted() {
        return new Status(Round.SUBMIT, Mode.IN_FLOW, "Đã nộp CV", SUBMITTED_NOTE, "neutral", null, null);
    }

    /** Thanh 4 bước: trước bước hiện tại là {@code done}, bước hiện tại {@code current}/{@code closed}, sau là {@code upcoming}. */
    static List<MyApplicationStep> steps(Round round, Mode mode) {
        List<MyApplicationStep> steps = new ArrayList<>();
        for (Round r : Round.values()) {
            int order = r.compareTo(round);
            String state = mode == Mode.FINISHED || order < 0 ? "done"
                    : order > 0 ? "upcoming"
                    : mode == Mode.STOPPED ? "closed" : "current";
            steps.add(new MyApplicationStep(r.label(), state));
        }
        return steps;
    }

    // ---- Đơn gấp (ux mục 2) ----

    /**
     * Việc ứng viên cần làm ngay trên một đơn đang đi: thư mời {@code Sent_Candidate} còn hạn, hoặc lịch phỏng vấn bắt
     * đầu trong {@link #INTERVIEW_URGENT_WITHIN} tới. Cả hai cùng có thì lấy mốc gần hơn. Thư mời không có hạn hoặc đã
     * quá hạn thì không gấp.
     */
    private static Optional<Urgency> urgency(Facts facts, LocalDateTime now) {
        List<Urgency> candidates = new ArrayList<>();
        Offer offer = facts.offer();
        if ("Offered".equals(facts.applicationStatus()) && offer != null && "Sent_Candidate".equals(offer.status())
                && offer.responseDeadline() != null && offer.responseDeadline().isAfter(now)) {
            candidates.add(new Urgency(offer.responseDeadline(), offerUrgentLabel(Duration.between(now, offer.responseDeadline()))));
        }
        upcomingInterview(facts.interviews(), now)
                .filter(i -> !i.start().isAfter(now.plus(INTERVIEW_URGENT_WITHIN)))
                .ifPresent(i -> candidates.add(new Urgency(i.start(), interviewUrgentLabel(Duration.between(now, i.start())))));
        return candidates.stream().min(Comparator.comparing(Urgency::until));
    }

    /** "Còn N ngày" (N làm tròn xuống); dưới 24 giờ "Còn N giờ"; dưới 1 giờ "Còn chưa tới 1 giờ". */
    private static String offerUrgentLabel(Duration left) {
        long hours = left.toHours();
        String amount = hours >= 24 ? (hours / 24) + " ngày" : hours >= 1 ? hours + " giờ" : "chưa tới 1 giờ";
        return "Còn " + amount + " để trả lời thư mời";
    }

    private static String interviewUrgentLabel(Duration left) {
        long hours = left.toHours();
        return hours >= 1 ? "Phỏng vấn sau " + hours + " giờ" : "Phỏng vấn trong vòng 1 giờ tới";
    }

    // ---- Hành trình ----

    /** Các mốc ứng viên được biết, cũ nhất trước. Không có người thực hiện, không có nhận xét (GBR-02). */
    static List<TimelineItem> journey(Facts facts) {
        List<TimelineItem> items = new ArrayList<>();
        items.add(new TimelineItem(facts.submittedAt(), "Đã nộp hồ sơ", null, null, "brand"));
        for (Interview interview : facts.interviews()) {
            items.add(new TimelineItem(interview.start(), interviewTitle(interview), null, null,
                    "Cancelled".equals(interview.status()) ? "neutral" : "brand"));
        }
        Offer offer = facts.offer();
        if (offer != null) {
            switch (offer.status()) {
                case "Sent_Candidate" -> items.add(new TimelineItem(offer.at(), "Nhận thư mời nhận việc", null, null, "warning"));
                case "Accepted" -> items.add(new TimelineItem(offer.at(), "Bạn đã nhận lời", null, null, "success"));
                case "Declined" -> items.add(new TimelineItem(offer.at(), "Bạn đã từ chối thư mời", null, null, "neutral"));
                default -> { }
            }
        }
        for (Mark mark : facts.marks()) {
            if (WITHDRAWN.equals(mark.kind())) {
                items.add(new TimelineItem(mark.at(), "Đã rút hồ sơ", null, null, "neutral"));
            } else if (POSTING_CLOSED.equals(mark.kind())) {
                items.add(new TimelineItem(mark.at(), "Vị trí đã ngừng tuyển", null, null, "neutral"));
            }
        }
        // Sắp xếp ổn định: mốc cùng thời điểm giữ thứ tự luồng ở trên.
        items.sort(Comparator.comparing(TimelineItem::at, Comparator.nullsLast(Comparator.naturalOrder())));
        return items;
    }

    private static String interviewTitle(Interview interview) {
        return switch (interview.status()) {
            case "Scheduled", "Rescheduled" -> {
                String format = formatLabel(interview.format());
                yield format == null ? "Hẹn phỏng vấn" : "Hẹn phỏng vấn (" + format + ")";
            }
            case "Completed" -> "Đã phỏng vấn";
            case "Cancelled" -> "Lịch phỏng vấn đã hủy";
            default -> "Lịch phỏng vấn";
        };
    }

    // ---- Tiện ích ----

    /** {@code null} nếu không rõ hình thức (ngoài {@code CK_InterviewSchedule_Format}). */
    static String formatLabel(String format) {
        if (format == null) {
            return null;
        }
        return switch (format) {
            case "Online_GoogleMeet" -> "Trực tuyến";
            case "Offline_Office" -> "Tại văn phòng";
            default -> null;
        };
    }

    /** Lịch {@code Scheduled}/{@code Rescheduled} gần nhất còn ở tương lai. */
    private static Optional<Interview> upcomingInterview(List<Interview> interviews, LocalDateTime now) {
        return interviews.stream()
                .filter(i -> "Scheduled".equals(i.status()) || "Rescheduled".equals(i.status()))
                .filter(i -> i.start() != null && i.start().isAfter(now))
                .min(Comparator.comparing(Interview::start));
    }

    private static boolean hasActiveInterview(List<Interview> interviews) {
        return interviews.stream().anyMatch(i -> !"Cancelled".equals(i.status()));
    }

    /** Dấu mới nhất loại {@code kind}. */
    private static Optional<Mark> mark(Facts facts, String kind) {
        return facts.marks().stream().filter(m -> kind.equals(m.kind())).reduce((first, second) -> second);
    }

    private static String offerHref(Offer offer) {
        return offer == null ? null : "/portal/offers/" + offer.offerId();
    }
}
