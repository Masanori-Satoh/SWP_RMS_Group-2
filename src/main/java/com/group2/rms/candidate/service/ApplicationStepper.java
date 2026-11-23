package com.group2.rms.candidate.service;

import com.group2.rms.candidate.dto.ApplicationProgressResponse;
import com.group2.rms.candidate.entity.ApplicationProgressStep;
import com.group2.rms.candidate.entity.ApplicationStage;
import com.group2.rms.core.web.StageItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Tiện ích xây dựng các bước của thanh tiến độ ứng tuyển từ trạng thái hiện tại.
 */
public final class ApplicationStepper {

    private ApplicationStepper() {
    }

    /**
     * Tạo danh sách các bước tiến độ tương ứng với trạng thái của hồ sơ.
     */
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

    static ApplicationStage lastStagePassed(ApplicationProgressStep closedStep) {
        return switch (closedStep) {
            case WITHDRAWN -> ApplicationStage.APPLIED;
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
