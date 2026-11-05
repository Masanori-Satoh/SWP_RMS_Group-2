package com.group2.rms.admin.service;

import com.group2.rms.admin.dto.PolicyDocumentResponse;
import com.group2.rms.admin.entity.AuditLog;
import com.group2.rms.admin.repository.AuditLogRepository;
import com.group2.rms.core.security.CurrentUserService;
import com.group2.rms.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/*
 * Màn System Admin "Tài liệu chính sách": liệt kê 2 tài liệu và thay file PDF.
 * Mỗi lần thay ghi 1 dòng AuditLog (ai, lúc nào) — màn này đọc lại dòng đó, không cần bảng riêng.
 * Kiểm tra / ghi file: PolicyDocumentStorage. Quyền: SecurityConfig (/admin/** chỉ System Admin).
 */
@Service
@RequiredArgsConstructor
public class PolicyDocumentService {

    public static final String AUDIT_ENTITY = "PolicyDocument";
    private static final String AUDIT_UPDATE = "UPDATE";

    private final PolicyDocumentStorage storage;
    private final AuditLogRepository auditLogs;
    private final CurrentUserService currentUser;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<PolicyDocumentResponse> listDocuments() {
        List<PolicyDocumentResponse> rows = new ArrayList<>();
        for (PolicyDocument document : PolicyDocument.values()) {
            rows.add(toResponse(document));
        }
        return rows;
    }

    /** Thay file rồi mới ghi audit: file sai thì dừng ở storage, không có dòng audit nào. */
    @Transactional
    public void replace(PolicyDocument document, MultipartFile file) {
        User admin = currentUser.requireUser();
        storage.replace(document, file);

        AuditLog log = AuditLog.builder()
                .user(admin)
                .action(AUDIT_UPDATE)
                .entityName(AUDIT_ENTITY)
                .entityId(document.code())
                .newValue(document.title() + ": thay file " + originalName(file) + " (" + sizeLabel(file.getSize()) + ")")
                .timestamp(LocalDateTime.now(clock))
                .build();
        auditLogs.save(log);
    }

    private PolicyDocumentResponse toResponse(PolicyDocument document) {
        LocalDateTime updatedAt = null;
        String updatedBy = null;
        Optional<AuditLog> lastChange = auditLogs.findFirstByEntityNameAndEntityIdOrderByTimestampDesc(AUDIT_ENTITY, document.code());
        if (lastChange.isPresent()) {
            updatedAt = lastChange.get().getTimestamp();
            updatedBy = lastChange.get().getUser().getFullName();
        }
        return new PolicyDocumentResponse(document.code(), document.title(), document.publicPath(), document.fileName(),
                storage.isUploaded(document), sizeLabel(storage.size(document)), updatedAt, updatedBy);
    }

    private static String originalName(MultipartFile file) {
        String name = file.getOriginalFilename();
        if (name == null || name.isBlank()) {
            return "(không tên)";
        }
        return name;
    }

    static String sizeLabel(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            return (bytes / 1024) + " KB";
        }
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }
}
