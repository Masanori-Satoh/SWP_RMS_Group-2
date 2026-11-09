package com.group2.rms.candidate.domain.rule;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/** Nhãn trạng thái thư mời nhận việc cho nhân viên (hành trình hồ sơ không còn hiện mã thô như {@code Sent_Candidate}). */
class ApplicationStatusLabelsTests {

    @ParameterizedTest
    @CsvSource({
            "Draft,Đang soạn",
            "Pending_Director,Chờ giám đốc duyệt",
            "Director_Approved,Giám đốc đã duyệt",
            "Director_Rejected,Giám đốc trả lại",
            "Sent_Candidate,'Đã gửi, chờ ứng viên trả lời'",
            "Accepted,Ứng viên đã nhận lời",
            "Declined,Ứng viên đã từ chối",
            "Expired,Đã quá hạn"
    })
    void offerLabelIsVietnamese(String status, String label) {
        assertEquals(label, ApplicationStatusLabels.offerLabel(status));
    }

    @Test
    void offerLabelKeepsUnknownStatusAndNeverShowsOfferWord() {
        assertEquals("Lạ", ApplicationStatusLabels.offerLabel("Lạ"));
        for (String status : new String[]{"Draft", "Pending_Director", "Director_Approved", "Director_Rejected",
                "Sent_Candidate", "Accepted", "Declined", "Expired"}) {
            assertFalse(ApplicationStatusLabels.offerLabel(status).toLowerCase().contains("offer"), status);
        }
    }
}
