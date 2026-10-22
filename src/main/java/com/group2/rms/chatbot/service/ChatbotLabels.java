package com.group2.rms.chatbot.service;

import com.group2.rms.chatbot.dto.ChatResponse.Link;
import com.group2.rms.chatbot.dto.ChatResponse.QuickReply;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/*
 * Nơi duy nhất chứa chữ hiển thị của chatbot: câu trả lời mẫu, nút gợi ý, link sang trang có sẵn.
 * Sửa câu chữ chatbot thì sửa ở đây.
 */
public final class ChatbotLabels {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ----- Câu trả lời -----
    public static final String WELCOME = "Xin chào! Mình là trợ lý tuyển dụng. Mình có thể giúp bạn tìm việc, "
            + "giải đáp quy trình ứng tuyển hoặc kiểm tra hồ sơ (cần đăng nhập). Bạn muốn bắt đầu với mục nào?";
    public static final String WELCOME_CANDIDATE = "Xin chào %s! Mình có thể giúp bạn tìm việc, xem trạng thái "
            + "hồ sơ hoặc gợi ý việc phù hợp. Bạn muốn bắt đầu với mục nào?";
    public static final String DISABLED = "Trợ lý tuyển dụng đang tạm ngưng. Bạn vui lòng quay lại sau nhé.";
    public static final String SEARCH_FOUND = "Mình tìm thấy %d tin đang tuyển phù hợp:";
    public static final String SEARCH_FOUND_MORE = "Mình tìm thấy %d tin đang tuyển phù hợp, đây là %d tin mới nhất:";
    public static final String SEARCH_LATEST = "Đây là các tin đang tuyển mới nhất:";
    public static final String SEARCH_NONE = "Hiện chưa có tin đang tuyển nào khớp với câu hỏi của bạn. "
            + "Bạn thử từ khóa khác hoặc xem tất cả vị trí nhé.";
    public static final String NO_OPEN_JOBS = "Hiện công ty chưa có tin nào đang nhận hồ sơ.";
    public static final String JOB_DETAIL = "Thông tin tin tuyển dụng bạn hỏi:";
    public static final String JOB_NOT_AVAILABLE = "Tin tuyển dụng này không còn khả dụng hoặc đã ngừng nhận hồ sơ.";
    public static final String LOGIN_REQUIRED = "Để xem hồ sơ và nhận gợi ý riêng, bạn cần đăng nhập tài khoản "
            + "ứng viên. Sau khi đăng nhập, quay lại đây và hỏi lại nhé.";
    public static final String CANDIDATE_ONLY = "Chức năng này chỉ dành cho tài khoản ứng viên.";
    public static final String STATUS_FOUND = "Bạn đang có %d hồ sơ ứng tuyển:";
    public static final String STATUS_FOUND_MORE = "Bạn đang có %d hồ sơ ứng tuyển, đây là %d hồ sơ mới cập nhật nhất:";
    public static final String STATUS_NONE = "Bạn chưa nộp hồ sơ nào. Bạn có muốn mình tìm việc giúp không?";
    public static final String RECOMMEND_FROM_HISTORY = "Dựa trên các vị trí bạn đã ứng tuyển, đây là một số tin "
            + "đang tuyển bạn có thể quan tâm:";
    public static final String RECOMMEND_LATEST = "Bạn chưa ứng tuyển vị trí nào, đây là các tin đang tuyển mới nhất:";
    public static final String RECOMMEND_NONE = "Hiện chưa có tin đang tuyển nào bạn chưa ứng tuyển.";
    public static final String RECOMMEND_ASK_SKILLS = "Muốn gợi ý chính xác hơn, bạn hãy gõ kỹ năng hoặc vị trí "
            + "mong muốn, ví dụ \"tìm việc Java ở Hà Nội\".";
    public static final String CONTACT = "Bạn có thể liên hệ trực tiếp bộ phận Nhân sự:";
    public static final String CONTACT_MISSING = "Hiện chưa có thông tin liên hệ bộ phận Nhân sự trên hệ thống.";
    public static final String UNKNOWN = "Xin lỗi, mình chưa có thông tin cho câu hỏi này. Bạn có thể chọn một mục "
            + "bên dưới hoặc liên hệ trực tiếp bộ phận Nhân sự.";

    // ----- Lý do gợi ý việc -----
    public static final String REASON_SAME_DEPARTMENT = "Cùng phòng ban với vị trí bạn đã nộp";
    public static final String REASON_SAME_LOCATION = "Cùng địa điểm làm việc";
    public static final String REASON_LATEST = "Tin mới đăng";

    public static final String NO_DEADLINE = "Không giới hạn";

    // ----- Nút gợi ý (bấm = gửi câu hỏi tương ứng) -----
    public static final QuickReply ASK_SEARCH = ask("Tìm việc làm", "Tìm việc làm");
    public static final QuickReply ASK_HOW_TO_APPLY = ask("Cách nộp hồ sơ", "Cách nộp hồ sơ");
    public static final QuickReply ASK_PROCESS = ask("Quy trình tuyển dụng", "Quy trình tuyển dụng");
    public static final QuickReply ASK_STATUS = ask("Hồ sơ của tôi", "Hồ sơ của tôi");
    public static final QuickReply ASK_RECOMMEND = ask("Gợi ý việc làm", "Gợi ý việc phù hợp với tôi");
    public static final QuickReply ASK_CONTACT = ask("Liên hệ HR", "Liên hệ HR");

    public static final List<QuickReply> MAIN_MENU =
            List.of(ASK_SEARCH, ASK_HOW_TO_APPLY, ASK_PROCESS, ASK_STATUS, ASK_CONTACT);
    public static final List<QuickReply> CANDIDATE_MENU =
            List.of(ASK_SEARCH, ASK_STATUS, ASK_RECOMMEND, ASK_HOW_TO_APPLY, ASK_CONTACT);

    // ----- Link sang trang có sẵn -----
    public static final Link LINK_LOGIN = new Link("Đăng nhập", "/login", true);
    public static final Link LINK_REGISTER = new Link("Tạo tài khoản", "/register", false);
    public static final Link LINK_ALL_JOBS = new Link("Xem tất cả vị trí", "/jobs", false);
    public static final Link LINK_MY_APPLICATIONS = new Link("Mở trang Hồ sơ của tôi", "/portal/applications", false);

    private ChatbotLabels() {
    }

    public static String jobDetailHref(Integer jobPostingId) {
        return "/jobs/" + jobPostingId;
    }

    /** Mở trang chi tiết tin và tự bật hộp thoại ứng tuyển (job-detail.js mở dialog khi URL có #apply). */
    public static Link applyLink(Integer jobPostingId) {
        return new Link("Ứng tuyển ngay", jobDetailHref(jobPostingId) + "#apply", true);
    }

    public static Link jobDetailLink(Integer jobPostingId) {
        return new Link("Xem trang chi tiết", jobDetailHref(jobPostingId), false);
    }

    public static String deadline(LocalDateTime applicationDeadline) {
        if (applicationDeadline == null) {
            return NO_DEADLINE;
        }
        return applicationDeadline.format(DATE);
    }

    private static QuickReply ask(String label, String message) {
        return new QuickReply(label, message, null);
    }
}
