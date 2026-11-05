package com.group2.rms.candidate.talentpool.service;

import com.group2.rms.candidate.domain.rule.ApplicationStatusService;
import com.group2.rms.candidate.talentpool.exception.TalentPoolException;
import com.group2.rms.candidate.talentpool.repository.TalentPoolAuditRepository;
import com.group2.rms.core.security.CurrentUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class RecruitmentEmailServiceTests {

    /** Thư mời phỏng vấn không gửi cho đơn đã kết thúc: danh sách trạng thái lấy từ một nguồn và truyền qua tham số. */
    @Test
    void interviewInvitationSkipsEveryTerminalStatusViaParameters() {
        List<Object> seenArgs = new ArrayList<>();
        String[] seenSql = new String[1];
        JdbcTemplate recording = new JdbcTemplate() {
            @Override
            public <T> List<T> query(String sql, RowMapper<T> rowMapper, Object... args) {
                seenSql[0] = sql;
                seenArgs.addAll(List.of(args));
                return List.of();
            }
        };
        @SuppressWarnings("unchecked")
        var service = new RecruitmentEmailService(mock(ObjectProvider.class), mock(CurrentUserService.class), recording,
                mock(TalentPoolAuditRepository.class));

        assertThrows(TalentPoolException.class, () -> service.resendInterview(5L));

        Set<Object> excluded = new HashSet<>(seenArgs);
        assertTrue(excluded.containsAll(ApplicationStatusService.TERMINAL), "mọi trạng thái kết thúc nằm trong tham số");
        assertTrue(excluded.contains("HM_Suggested"), "bản nháp đề xuất vẫn bị loại");
        assertEquals(1 + ApplicationStatusService.TERMINAL.size() + 1, seenArgs.size(), "id lịch + trạng thái");
        assertFalse(seenSql[0].contains("'Rejected'"), "không viết cứng trạng thái trong câu SQL");
    }
}
