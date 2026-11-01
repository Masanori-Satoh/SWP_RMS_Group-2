package com.group2.rms.candidate.talentpool.dto;

import jakarta.validation.constraints.AssertTrue;

// require agree from candidate before use old CV
public record TalentPoolConfirmRequest(
                // must verify when called candidate
                @AssertTrue(message = "Cần xác nhận ứng viên đã đồng ý ứng tuyển bằng CV cũ.") boolean consent) {
}
