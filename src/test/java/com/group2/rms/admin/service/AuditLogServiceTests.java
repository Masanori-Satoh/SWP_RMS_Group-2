package com.group2.rms.admin.service;

import com.group2.rms.admin.entity.AuditLog;
import com.group2.rms.admin.repository.AuditLogRepository;
import com.group2.rms.user.entity.User;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
class AuditLogServiceTests {
    final AuditLogRepository logs = mock(AuditLogRepository.class);
    final AuditLogService service = new AuditLogService(logs);

    @Test void combinesFiltersAndIncludesTheWholeEndDate() {
        LocalDate from = LocalDate.of(2026, 10, 1), to = LocalDate.of(2026, 10, 31);
        var at = LocalDateTime.of(2026, 10, 31, 23, 59, 59);
        AuditLog log = AuditLog.builder().auditLogId(9L).user(User.builder().fullName("Admin").build())
                .timestamp(at).action("UPDATE").entityName("User").entityId("7").newValue("Đã sửa").build();
        when(logs.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(log), PageRequest.of(0, 10), 1));
        var result = service.search(from, to, 1, "UPDATE", "User", "50%_", -3);
        assertEquals("Admin", result.getContent().getFirst().performedBy());
        assertEquals(at, result.getContent().getFirst().timestamp());
        ArgumentCaptor<Specification<AuditLog>> spec = ArgumentCaptor.forClass(Specification.class);
        ArgumentCaptor<Pageable> page = ArgumentCaptor.forClass(Pageable.class);
        verify(logs).findAll(spec.capture(), page.capture());
        assertEquals(0, page.getValue().getPageNumber());
        assertEquals(10, page.getValue().getPageSize());
        assertTrue(page.getValue().getSort().getOrderFor("timestamp").isDescending());
        assertTrue(page.getValue().getSort().getOrderFor("auditLogId").isDescending());
        Root<AuditLog> root = mock(Root.class, RETURNS_DEEP_STUBS);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        spec.getValue().toPredicate(root, mock(CriteriaQuery.class), cb);
        Path<LocalDateTime> timestamp = root.get("timestamp");
        verify(cb).greaterThanOrEqualTo(timestamp, from.atStartOfDay());
        verify(cb).lessThan(timestamp, to.plusDays(1).atStartOfDay());
        verify(cb).equal(root.get("user").get("userId"), 1);
        verify(cb).equal(root.get("action"), "UPDATE");
        verify(cb).equal(root.get("entityName"), "User");
        verify(cb, times(4)).like(any(), eq("%50\\%\\_%"), eq('\\'));
    }

    @Test void pagePastTheEndFallsBackToTheLastPage() {
        when(logs.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(99, 10), 12))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(1, 10), 12));
        var page = service.search(null, null, null, "", "", "", 99);
        assertEquals(1, page.getNumber());
        ArgumentCaptor<Pageable> pages = ArgumentCaptor.forClass(Pageable.class);
        verify(logs, times(2)).findAll(any(Specification.class), pages.capture());
        assertEquals(List.of(99, 1), pages.getAllValues().stream().map(Pageable::getPageNumber).toList());
    }

    @Test void dashboardLoadsOnlyFiveRecentRecords() {
        when(logs.findAll(any(Pageable.class))).thenReturn(org.springframework.data.domain.Page.empty());
        assertTrue(service.recent().isEmpty());
        ArgumentCaptor<Pageable> page = ArgumentCaptor.forClass(Pageable.class);
        verify(logs).findAll(page.capture());
        assertEquals(5, page.getValue().getPageSize());
        assertEquals(0, page.getValue().getPageNumber());
    }
}
