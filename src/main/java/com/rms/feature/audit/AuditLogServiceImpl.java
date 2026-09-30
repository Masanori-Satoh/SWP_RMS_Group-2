package com.rms.feature.audit;

import com.group2.rms.entity.AuditLog;
import com.group2.rms.entity.User;
import com.group2.rms.repository.AuditLogRepository;
import com.group2.rms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Triển khai AuditLogService ghi nhận thao tác người dùng vào cơ sở dữ liệu.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(Integer userId, String action, String entityName, String entityId, String oldValue, String newValue) {
        try {
            User user = null;
            if (userId != null) {
                user = userRepository.findById(userId).orElse(null);
            }

            if (user == null) {
                log.warn("[AUDIT-LOG] Bỏ qua ghi log do không tìm thấy User với ID: {}", userId);
                return;
            }

            String safeAction = (action != null && action.length() > 20) ? action.substring(0, 20) : action;

            AuditLog auditLog = AuditLog.builder()
                    .user(user)
                    .action(safeAction)
                    .entityName(entityName)
                    .entityId(entityId)
                    .oldValue(oldValue)
                    .newValue(newValue)
                    .timestamp(LocalDateTime.now())
                    .build();

            auditLogRepository.saveAndFlush(auditLog);
            log.info("[AUDIT-LOG] Đã lưu log: Action={}, Entity={}, EntityId={}, UserId={}",
                    safeAction, entityName, entityId, userId);
        } catch (Exception e) {
            log.error("[AUDIT-LOG] Lỗi khi lưu audit log: {}", e.getMessage(), e);
        }
    }
}
