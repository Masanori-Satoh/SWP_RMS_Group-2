package com.group2.rms.interview;

import lombok.Getter;

import java.util.Set;

/**
 * Trạng thái lịch phỏng vấn.
 * Mapping tương ứng với CHECK constraint trong MS SQL Server:
 * CK_InterviewSchedule_Status: CHECK (InterviewStatus IN (N'Scheduled', N'Completed', N'Cancelled', N'Rescheduled'))
 *
 * Tuân thủ Business Rule GBR-01: Trạng thái chỉ đi tiến, không lùi.
 * - Scheduled -> Rescheduled / Completed / Cancelled
 * - Rescheduled -> Completed / Cancelled / Rescheduled
 * - Completed -> Terminal state (không thể lùi)
 * - Cancelled -> Terminal state (không thể lùi)
 */
@Getter
public enum InterviewStatus {
    Scheduled("Đã lên lịch"),
    Rescheduled("Đã đổi lịch"),
    Completed("Đã hoàn thành"),
    Cancelled("Đã hủy");

    private final String displayName;

    InterviewStatus(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Business Rule GBR-01: Kiểm tra tính hợp lệ của việc chuyển dịch trạng thái.
     *
     * @param target Trạng thái muốn chuyển sang
     * @return true nếu chuyển dịch hợp lệ theo luồng tiến, false nếu cố tình đi lùi hoặc không hợp lệ.
     */
    public boolean canTransitionTo(InterviewStatus target) {
        if (target == null) {
            return false;
        }
        // Giữ nguyên trạng thái hiện tại thì coi như hợp lệ
        if (this == target) {
            return true;
        }

        return switch (this) {
            case Scheduled -> target == Rescheduled || target == Completed || target == Cancelled;
            case Rescheduled -> target == Completed || target == Cancelled || target == Rescheduled;
            case Completed, Cancelled -> false; // Terminal states: không cho phép quay lui về Scheduled hoặc Rescheduled
        };
    }

    /**
     * Danh sách các trạng thái tiếp theo hợp lệ có thể chuyển đến.
     */
    public Set<InterviewStatus> nextValidStatuses() {
        return switch (this) {
            case Scheduled -> Set.of(Rescheduled, Completed, Cancelled);
            case Rescheduled -> Set.of(Completed, Cancelled);
            case Completed, Cancelled -> Set.of();
        };
    }
}
