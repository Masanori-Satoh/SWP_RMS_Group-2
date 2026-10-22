package com.group2.rms.chatbot.service;

import com.group2.rms.candidate.dto.MyApplicationResponse;
import com.group2.rms.candidate.service.MyApplicationService;
import com.group2.rms.chatbot.dto.ChatRequest;
import com.group2.rms.chatbot.dto.ChatResponse;
import com.group2.rms.chatbot.dto.ChatResponse.ApplicationCard;
import com.group2.rms.chatbot.dto.ChatResponse.Contact;
import com.group2.rms.chatbot.dto.ChatResponse.Link;
import com.group2.rms.chatbot.dto.ChatResponse.QuickReply;
import com.group2.rms.chatbot.service.ChatbotIntentResolver.Resolution;
import com.group2.rms.chatbot.validator.ChatbotValidator;
import com.group2.rms.core.security.CurrentUserService;
import com.group2.rms.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/*
 * Điều phối một lượt hỏi - đáp của chatbot (GĐ 1: theo luật, chưa dùng LLM, không lưu lịch sử vào DB).
 * Đoán ý định (ChatbotIntentResolver) → gọi đúng phần việc → dựng ChatResponse. Không ghi gì vào DB.
 * Người hỏi luôn lấy từ phiên đăng nhập; hồ sơ chỉ đọc của chính ứng viên đó (MyApplicationService).
 */
@Service
@RequiredArgsConstructor
public class ChatbotService {

    /** Số thẻ hồ sơ tối đa trong một câu trả lời. */
    public static final int MAX_APPLICATION_CARDS = 5;

    private final ChatbotIntentResolver resolver;
    private final ChatbotConfigService config;
    private final ChatbotJobService jobs;
    private final ChatbotAccess access;
    private final ChatbotValidator validator;
    private final MyApplicationService myApplications;
    private final CurrentUserService currentUserService;

    /**
     * Trả lời một câu hỏi.
     *
     * @param signedIn người hỏi đã đăng nhập chưa (controller biết từ phiên); khách thì {@code false}
     */
    public ChatResponse reply(ChatRequest request, boolean signedIn) {
        if (!config.isEnabled()) {
            return answer(ChatbotIntent.DISABLED, ChatbotLabels.DISABLED, List.of()).build();
        }
        User viewer = null;
        if (signedIn) {
            viewer = currentUserService.requireUser();
        }
        List<QuickReply> menu = menuFor(viewer);

        if (request.jobPostingId() != null) {
            return jobDetail(request.jobPostingId(), viewer, menu);
        }
        String message = validator.cleanMessage(request.message());
        if (message == null) {
            return welcome(viewer, menu);
        }

        Resolution resolution = resolver.resolve(message, config.loadFaqs());
        return switch (resolution.intent()) {
            case CHECK_STATUS -> applicationStatus(viewer, menu);
            case RECOMMEND -> recommendation(viewer, menu);
            case CONTACT_HR -> contactHr(ChatbotIntent.CONTACT_HR, ChatbotLabels.CONTACT, menu);
            case FAQ -> answer(ChatbotIntent.FAQ, resolution.faq().answer(), menu).build();
            case SEARCH_JOB -> searchJobs(message, menu);
            default -> unknown(message, menu);
        };
    }

    /** Tạo lời chào và menu theo quyền của người đang xem. */
    private ChatResponse welcome(User viewer, List<QuickReply> menu) {
        String reply = ChatbotLabels.WELCOME;
        if (access.canUsePersonalFeatures(viewer)) {
            reply = String.format(ChatbotLabels.WELCOME_CANDIDATE, viewer.getFullName());
        }
        return answer(ChatbotIntent.WELCOME, reply, menu).build();
    }

    /** Tìm tin công khai theo câu hỏi và trả tối đa số thẻ mà khung chat hiển thị. */
    private ChatResponse searchJobs(String message, List<QuickReply> menu) {
        ChatbotJobService.SearchResult result = jobs.search(message);
        boolean hasTerms = jobs.hasSearchTerms(message);
        if (result.total() == 0) {
            String reply = ChatbotLabels.SEARCH_NONE;
            if (!hasTerms) {
                reply = ChatbotLabels.NO_OPEN_JOBS;
            }
            return answer(ChatbotIntent.SEARCH_JOB, reply, menu).links(List.of(ChatbotLabels.LINK_ALL_JOBS)).build();
        }
        String reply = String.format(ChatbotLabels.SEARCH_FOUND, result.total());
        if (!hasTerms) {
            reply = ChatbotLabels.SEARCH_LATEST;
        } else if (result.total() > result.jobs().size()) {
            reply = String.format(ChatbotLabels.SEARCH_FOUND_MORE, result.total(), result.jobs().size());
        }
        return answer(ChatbotIntent.SEARCH_JOB, reply, menu)
                .jobs(result.jobs())
                .links(List.of(ChatbotLabels.LINK_ALL_JOBS))
                .build();
    }

    /** Lấy chi tiết tin còn công khai và chỉ thêm link ứng tuyển khi được phép. */
    private ChatResponse jobDetail(Integer jobPostingId, User viewer, List<QuickReply> menu) {
        String username = null;
        if (viewer != null) {
            username = viewer.getUsername();
        }
        Optional<ChatbotJobService.DetailResult> result = jobs.detail(jobPostingId, username);
        if (result.isEmpty()) {
            return answer(ChatbotIntent.JOB_DETAIL, ChatbotLabels.JOB_NOT_AVAILABLE, menu)
                    .links(List.of(ChatbotLabels.LINK_ALL_JOBS))
                    .build();
        }
        List<Link> links = new ArrayList<>();
        if (result.get().canApply()) {
            links.add(ChatbotLabels.applyLink(jobPostingId));
        }
        links.add(ChatbotLabels.jobDetailLink(jobPostingId));
        return answer(ChatbotIntent.JOB_DETAIL, ChatbotLabels.JOB_DETAIL, List.of(ChatbotLabels.ASK_HOW_TO_APPLY))
                .jobDetail(result.get().detail())
                .links(links)
                .build();
    }

    /** Kiểm tra quyền rồi trả các hồ sơ ứng tuyển của chính Candidate đang đăng nhập. */
    private ChatResponse applicationStatus(User viewer, List<QuickReply> menu) {
        Optional<ChatResponse> blocked = personalFeatureBlocked(ChatbotIntent.CHECK_STATUS, viewer, menu);
        if (blocked.isPresent()) {
            return blocked.get();
        }
        List<MyApplicationResponse> owned = myApplications.list(null).applications();
        if (owned.isEmpty()) {
            return answer(ChatbotIntent.CHECK_STATUS, ChatbotLabels.STATUS_NONE, List.of(ChatbotLabels.ASK_SEARCH))
                    .build();
        }
        List<ApplicationCard> cards = new ArrayList<>();
        for (MyApplicationResponse application : owned.subList(0, Math.min(MAX_APPLICATION_CARDS, owned.size()))) {
            cards.add(new ApplicationCard(application.postingTitle(), application.statusLabel(),
                    application.statusNote(), application.statusTone()));
        }
        String reply = String.format(ChatbotLabels.STATUS_FOUND, owned.size());
        if (owned.size() > cards.size()) {
            reply = String.format(ChatbotLabels.STATUS_FOUND_MORE, owned.size(), cards.size());
        }
        return answer(ChatbotIntent.CHECK_STATUS, reply, List.of(ChatbotLabels.ASK_RECOMMEND))
                .applications(cards)
                .links(List.of(ChatbotLabels.LINK_MY_APPLICATIONS))
                .build();
    }

    /** Gợi ý tin chưa nộp từ lịch sử của Candidate, hoặc tin mới nhất khi chưa có lịch sử. */
    private ChatResponse recommendation(User viewer, List<QuickReply> menu) {
        Optional<ChatResponse> blocked = personalFeatureBlocked(ChatbotIntent.RECOMMEND, viewer, menu);
        if (blocked.isPresent()) {
            return blocked.get();
        }
        List<Integer> appliedPostingIds = myApplications.list(null).applications().stream()
                .map(MyApplicationResponse::jobPostingId)
                .toList();
        ChatbotJobService.RecommendResult result = jobs.recommend(appliedPostingIds);
        if (result.jobs().isEmpty()) {
            return answer(ChatbotIntent.RECOMMEND, ChatbotLabels.RECOMMEND_NONE, menu)
                    .links(List.of(ChatbotLabels.LINK_ALL_JOBS))
                    .build();
        }
        String reply = ChatbotLabels.RECOMMEND_LATEST;
        if (result.fromHistory()) {
            reply = ChatbotLabels.RECOMMEND_FROM_HISTORY;
        }
        return answer(ChatbotIntent.RECOMMEND, reply + " " + ChatbotLabels.RECOMMEND_ASK_SKILLS, menu)
                .jobs(result.jobs())
                .build();
    }

    /** Gắn thông tin liên hệ HR từ cấu hình; báo thiếu nếu người dùng hỏi trực tiếp mà chưa cấu hình. */
    private ChatResponse contactHr(ChatbotIntent intent, String reply, List<QuickReply> menu) {
        Contact contact = config.loadHrContact();
        if (contact == null && intent == ChatbotIntent.CONTACT_HR) {
            reply = ChatbotLabels.CONTACT_MISSING;
        }
        return answer(intent, reply, menu).contact(contact).build();
    }

    /** Không hiểu câu hỏi: nếu câu có từ khớp tên / địa điểm của tin đang mở thì coi như tìm việc. */
    private ChatResponse unknown(String message, List<QuickReply> menu) {
        if (jobs.hasSearchTerms(message) && jobs.search(message).total() > 0) {
            return searchJobs(message, menu);
        }
        return contactHr(ChatbotIntent.UNKNOWN, ChatbotLabels.UNKNOWN, menu);
    }

    /**
     * Security: hồ sơ và gợi ý riêng chỉ cho ứng viên đã đăng nhập.
     * Khách → mời đăng nhập; tài khoản nội bộ → báo chỉ dành cho ứng viên. Được phép → rỗng.
     */
    private Optional<ChatResponse> personalFeatureBlocked(ChatbotIntent intent, User viewer, List<QuickReply> menu) {
        if (access.canUsePersonalFeatures(viewer)) {
            return Optional.empty();
        }
        if (viewer == null) {
            return Optional.of(answer(intent, ChatbotLabels.LOGIN_REQUIRED,
                    List.of(ChatbotLabels.ASK_SEARCH, ChatbotLabels.ASK_PROCESS))
                    .links(List.of(ChatbotLabels.LINK_LOGIN, ChatbotLabels.LINK_REGISTER))
                    .build());
        }
        return Optional.of(answer(intent, ChatbotLabels.CANDIDATE_ONLY, menu).build());
    }

    /** Chọn các nút gợi ý công khai hoặc dành riêng cho Candidate. */
    private List<QuickReply> menuFor(User viewer) {
        if (access.canUsePersonalFeatures(viewer)) {
            return ChatbotLabels.CANDIDATE_MENU;
        }
        return ChatbotLabels.MAIN_MENU;
    }

    /** Tạo phần chung của response trước khi mỗi nhánh thêm thẻ hoặc link riêng. */
    private static ChatResponse.ChatResponseBuilder answer(ChatbotIntent intent, String reply,
            List<QuickReply> quickReplies) {
        return ChatResponse.builder().intent(intent.name()).reply(reply).quickReplies(quickReplies);
    }
}
