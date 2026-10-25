package com.group2.rms.candidate.application.pipeline.service;

import com.group2.rms.candidate.domain.rule.ApplicationProgressResponse;
import com.group2.rms.candidate.domain.rule.ApplicationProgressStep;
import com.group2.rms.candidate.domain.rule.ApplicationStage;
import com.group2.rms.core.web.StageItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Dựng các bước của thanh tiến độ ({@code stageBar} mode {@code stepper}) từ giai đoạn hiện tại của hồ sơ
 * (hàm thuần, không đọc DB). Đây là nơi duy nhất quyết định bước nào đã qua, bước nào đang đứng, và hồ sơ đã dừng
 * thì dừng sau bước nào.
 * <p>
 * Hồ sơ còn trong luồng: các bước trước giai đoạn hiện tại là {@code done}, giai đoạn hiện tại là {@code current}
 * (kèm nhãn chi tiết và người đang cầm bóng), các bước sau là {@code upcoming}. Hồ sơ đã dừng: chỉ giữ các bước đã
 * qua, rồi một ô {@link ApplicationStage#CLOSED} mang lý do dừng; các bước sau điểm dừng không hiện.
 */
public final class ApplicationStepper {

    private ApplicationStepper() {
    }

    public static List<StageItem> items(ApplicationProgressResponse progress) {
        ApplicationStage current = progress.stage();
        List<StageItem> items = new ArrayList<>();
        if (current == ApplicationStage.CLOSED) {
            ApplicationStage reached = lastStagePassed(progress.step());
            for (ApplicationStage stage : ApplicationStage.values()) {
                if (stage.compareTo(reached) <= 0) {
                    items.add(item(stage, "done", null));
                }
            }
            items.add(item(ApplicationStage.CLOSED, "current", progress.detailLabel()));
            return items;
        }
        for (ApplicationStage stage : ApplicationStage.values()) {
            if (stage == ApplicationStage.CLOSED) {
                continue;
            }
            int order = stage.compareTo(current);
            items.add(item(stage, order < 0 ? "done" : order == 0 ? "current" : "upcoming",
                    order == 0 ? currentDetail(progress) : null));
        }
        return items;
    }

    /** Giai đoạn cuối cùng hồ sơ đã đi qua trước khi dừng, suy từ lý do dừng. */
    static ApplicationStage lastStagePassed(ApplicationProgressStep closedStep) {
        return switch (closedStep) {
            case WITHDRAWN -> ApplicationStage.APPLIED;
            // Đợt có thể đóng ở bất kỳ vòng nào; chỉ khẳng định hồ sơ đã được nộp.
            case POSTING_CLOSED -> ApplicationStage.APPLIED;
            case CV_REJECTED -> ApplicationStage.SCREENING;
            case INTERVIEW_FAILED -> ApplicationStage.INTERVIEW;
            case OFFER_DECLINED -> ApplicationStage.OFFER;
            default -> throw new IllegalStateException("Không phải bước dừng: " + closedStep);
        };
    }

    private static String currentDetail(ApplicationProgressResponse progress) {
        return progress.ownerLabel() == null
                ? progress.detailLabel()
                : progress.detailLabel() + " · Đang chờ: " + progress.ownerLabel();
    }

    private static StageItem item(ApplicationStage stage, String state, String detail) {
        return new StageItem(stage.name(), stage.label(), null, null, state, detail, false);
    }
}
