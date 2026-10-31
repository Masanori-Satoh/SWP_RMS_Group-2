package com.group2.rms.admin.service;

import com.group2.rms.admin.entity.AuditLog;
import com.group2.rms.admin.repository.AuditLogTargetRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
class AuditLogTargetRepositoryTests {
    final NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
    final AuditLogTargetRepository targets = new AuditLogTargetRepository(jdbc);
    private AuditLog log(String entity, String id) { return AuditLog.builder().entityName(entity).entityId(id).build(); }

    @Test void batchesDuplicateApplicationIdsIntoOneLookup() {
        when(jdbc.query(anyString(), anyMap(), any(ResultSetExtractor.class)))
                .thenReturn(Map.of("1226", "Nguyễn Văn An · Java Developer", "1225", "Trần Thị Bình · Backend Developer"));
        var names = targets.findNames(List.of(log("Application", "1226"), log("Application", "1226"), log("Application", "1225")));
        assertEquals(2, names.size());
        assertEquals("Nguyễn Văn An · Java Developer", names.get(new AuditLogTargetRepository.Target("Application", "1226")));
        ArgumentCaptor<Map<String, ?>> params = ArgumentCaptor.forClass(Map.class);
        verify(jdbc).query(contains("FROM Application a"), params.capture(), any(ResultSetExtractor.class));
        assertEquals(Set.of(1226, 1225), params.getValue().get("ids"));
    }

    @Test void unknownTypesAndMalformedNumericIdsDoNotRunQueries() {
        assertTrue(targets.findNames(List.of(log("Unknown", "1"), log("User", "bad-id"), log("Department", "99999999999999999999"))).isEmpty());
        verifyNoInteractions(jdbc);
    }

    @Test void missingRecordsHaveNoFabricatedName() {
        when(jdbc.query(anyString(), anyMap(), any(ResultSetExtractor.class))).thenReturn(Map.of());
        assertTrue(targets.findNames(List.of(log("JobPosting", "99"))).isEmpty());
    }

    @Test void configurationUsesStringKeysAndNeverReadsConfigurationValues() {
        when(jdbc.query(anyString(), anyMap(), any(ResultSetExtractor.class)))
                .thenReturn(Map.of("AI_SCREENING_ENDPOINT", "Điểm kết nối sàng lọc AI"));
        var names = targets.findNames(List.of(log("SystemConfig", "AI_SCREENING_ENDPOINT")));
        assertEquals("Điểm kết nối sàng lọc AI", names.get(new AuditLogTargetRepository.Target("SystemConfig", "AI_SCREENING_ENDPOINT")));
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map<String, ?>> params = ArgumentCaptor.forClass(Map.class);
        verify(jdbc).query(sql.capture(), params.capture(), any(ResultSetExtractor.class));
        assertFalse(sql.getValue().contains("ConfigValue"));
        assertEquals(Set.of("AI_SCREENING_ENDPOINT"), params.getValue().get("ids"));
    }
}
