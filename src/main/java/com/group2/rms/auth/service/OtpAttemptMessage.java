package com.group2.rms.auth.service;

final class OtpAttemptMessage {

    private OtpAttemptMessage() {
    }

    static String format(int failedAttempts, int maxFailedAttempts, String nextStep) {
        return "Mã xác minh không đúng. Bạn đã nhập sai %d lần, còn %d lần thử. %s"
                .formatted(failedAttempts, Math.max(0, maxFailedAttempts - failedAttempts), nextStep);
    }
}
