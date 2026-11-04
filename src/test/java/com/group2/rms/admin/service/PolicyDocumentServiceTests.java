package com.group2.rms.admin.service;

import com.group2.rms.admin.dto.PolicyDocumentResponse;
import com.group2.rms.admin.entity.AuditLog;
import com.group2.rms.admin.exception.PolicyDocumentException;
import com.group2.rms.admin.repository.AuditLogRepository;
import com.group2.rms.core.security.CurrentUserService;
import com.group2.rms.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/*
 * UNIT TEST (Mockito): PolicyDocumentService — thay file + ghi AuditLog, dựng 2 dòng cho màn Admin.
 */
@ExtendWith(MockitoExtension.class)
class PolicyDocumentServiceTests {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-04T03:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));
    private static final LocalDateTime NOW = LocalDateTime.now(CLOCK);

    @Mock
    private PolicyDocumentStorage storage;
    @Mock
    private AuditLogRepository auditLogs;
    @Mock
    private CurrentUserService currentUser;

    private PolicyDocumentService service;
    private final User admin = User.builder().userId(1).fullName("Trần Minh Đức").build();
    private final MockMultipartFile file = new MockMultipartFile("file", "dieu-khoan.pdf", "application/pdf", new byte[2048]);

    @BeforeEach
    void setUp() {
        service = new PolicyDocumentService(storage, auditLogs, currentUser, CLOCK);
    }

    @Test
    @DisplayName("Thay file hợp lệ -> Ghi file rồi ghi 1 dòng AuditLog đúng người, đúng tài liệu, đúng giờ")
    void replace_shouldStoreFileAndAudit_whenFileValid() {
        // Arrange
        when(currentUser.requireUser()).thenReturn(admin);

        // Act
        service.replace(PolicyDocument.TERMS, file);

        // Assert
        verify(storage).replace(PolicyDocument.TERMS, file);
        ArgumentCaptor<AuditLog> saved = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogs).save(saved.capture());
        assertEquals(admin, saved.getValue().getUser());
        assertEquals("UPDATE", saved.getValue().getAction());
        assertEquals(PolicyDocumentService.AUDIT_ENTITY, saved.getValue().getEntityName());
        assertEquals("terms", saved.getValue().getEntityId());
        assertEquals(NOW, saved.getValue().getTimestamp());
        assertEquals("Điều khoản sử dụng: thay file dieu-khoan.pdf (2 KB)", saved.getValue().getNewValue());
    }

    @Test
    @DisplayName("File không hợp lệ -> Báo lỗi, không ghi AuditLog")
    void replace_shouldNotAudit_whenStorageRejectsFile() {
        // Arrange
        when(currentUser.requireUser()).thenReturn(admin);
        doThrow(new PolicyDocumentException(PolicyDocumentStorage.NOT_PDF)).when(storage).replace(PolicyDocument.TERMS, file);

        // Act + Assert
        assertThrows(PolicyDocumentException.class, () -> service.replace(PolicyDocument.TERMS, file));
        verify(auditLogs, never()).save(any());
    }

    @Test
    @DisplayName("Danh sách -> Đủ 2 tài liệu; có audit thì hiện ngày + người, chưa có thì để trống")
    void listDocuments_shouldShowLastChange_whenAuditExists() {
        // Arrange
        AuditLog lastChange = AuditLog.builder().user(admin).timestamp(NOW.minusHours(2)).build();
        when(auditLogs.findFirstByEntityNameAndEntityIdOrderByTimestampDesc(PolicyDocumentService.AUDIT_ENTITY, "terms"))
                .thenReturn(Optional.of(lastChange));
        when(auditLogs.findFirstByEntityNameAndEntityIdOrderByTimestampDesc(PolicyDocumentService.AUDIT_ENTITY, "privacy"))
                .thenReturn(Optional.empty());
        when(storage.isUploaded(PolicyDocument.TERMS)).thenReturn(true);
        when(storage.size(PolicyDocument.TERMS)).thenReturn(320L * 1024);

        // Act
        List<PolicyDocumentResponse> rows = service.listDocuments();

        // Assert
        assertEquals(2, rows.size());
        PolicyDocumentResponse terms = rows.get(0);
        assertEquals("/terms", terms.publicPath());
        assertTrue(terms.uploaded());
        assertEquals("320 KB", terms.sizeLabel());
        assertEquals(NOW.minusHours(2), terms.updatedAt());
        assertEquals("Trần Minh Đức", terms.updatedBy());
        PolicyDocumentResponse privacy = rows.get(1);
        assertFalse(privacy.uploaded());
        assertNull(privacy.updatedAt());
        assertNull(privacy.updatedBy());
    }

    @Test
    @DisplayName("sizeLabel -> B / KB / MB dễ đọc")
    void sizeLabel_shouldBeReadable() {
        assertEquals("823 B", PolicyDocumentService.sizeLabel(823));
        assertEquals("1 KB", PolicyDocumentService.sizeLabel(1024));
        assertEquals("2.5 MB", PolicyDocumentService.sizeLabel(2_621_440).replace(',', '.'));
    }
}
