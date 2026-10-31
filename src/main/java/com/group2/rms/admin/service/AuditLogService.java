package com.group2.rms.admin.service;

import com.group2.rms.admin.entity.AuditLog;
import com.group2.rms.admin.dto.AuditLogLabels;
import com.group2.rms.admin.repository.AuditLogRepository;
import com.group2.rms.admin.repository.AuditLogTargetRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditLogService {
    private final AuditLogRepository logs;
    private final AuditLogTargetRepository targets;
    private static final Sort NEWEST = Sort.by(Sort.Order.desc("timestamp"), Sort.Order.desc("auditLogId"));

    public Page<Row> search(LocalDate from, LocalDate to, Integer actorId, String action,
            String entity, String search, int page) {
        Specification<AuditLog> filter = (root, query, cb) -> {
            List<Predicate> filters = new ArrayList<>();
            if (from != null) filters.add(cb.greaterThanOrEqualTo(root.get("timestamp"), from.atStartOfDay()));
            if (to != null) filters.add(cb.lessThan(root.get("timestamp"), to.plusDays(1).atStartOfDay()));
            if (actorId != null) filters.add(cb.equal(root.get("user").get("userId"), actorId));
            if (action != null && !action.isBlank()) filters.add(cb.equal(root.get("action"), action));
            if (entity != null && !entity.isBlank()) filters.add(cb.equal(root.get("entityName"), entity));
            if (search != null && !search.isBlank()) {
                String term = "%" + search.strip().toLowerCase(Locale.ROOT)
                        .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
                filters.add(cb.or(cb.like(cb.lower(root.get("user").get("fullName")), term, '\\'),
                        cb.like(cb.lower(root.get("user").get("username")), term, '\\'),
                        cb.like(cb.lower(root.get("entityName")), term, '\\'),
                        cb.like(cb.lower(root.get("entityId")), term, '\\')));
            }
            return cb.and(filters.toArray(Predicate[]::new));
        };
        Page<AuditLog> result = logs.findAll(filter, PageRequest.of(Math.max(0, page), 10, NEWEST));
        if (result.getTotalPages() > 0 && result.getNumber() >= result.getTotalPages())
            result = logs.findAll(filter, PageRequest.of(result.getTotalPages() - 1, 10, NEWEST));
        var names = targets.findNames(result.getContent());
        return result.map(log -> row(log, names.get(new AuditLogTargetRepository.Target(log.getEntityName(), log.getEntityId()))));
    }

    public List<Row> recent() {
        var result = logs.findAll(PageRequest.of(0, 5, NEWEST));
        var names = targets.findNames(result.getContent());
        return result.map(log -> row(log, names.get(new AuditLogTargetRepository.Target(log.getEntityName(), log.getEntityId())))).getContent();
    }

    public List<AuditLogRepository.ActorChoice> actors() { return logs.findActors(); }
    public List<String> entities() { return logs.findEntityNames(); }

    private static Row row(AuditLog log, String targetName) {
        return new Row(log.getTimestamp(), log.getUser().getFullName(), log.getAction(),
                log.getEntityName(), log.getEntityId(), log.getOldValue(), log.getNewValue(), targetName);
    }

    public record Row(LocalDateTime timestamp, String performedBy, String action, String entityName,
            String entityId, String oldValue, String newValue, String targetName) {
        public Row(LocalDateTime timestamp, String performedBy, String action, String entityName,
                String entityId, String oldValue, String newValue) {
            this(timestamp, performedBy, action, entityName, entityId, oldValue, newValue, null);
        }
        public String targetDescription() {
            return targetName == null || targetName.isBlank() ? "Không tìm thấy thông tin đối tượng" : targetName;
        }
        public String actionLabel() { return AuditLogLabels.action(action); }
        public String entityLabel() { return AuditLogLabels.entity(entityName); }
        public String entityIcon() { return AuditLogLabels.entityIcon(entityName); }
        public String initials() { return AuditLogLabels.initials(performedBy); }
        public String oldContent() { return AuditLogLabels.content(entityName, action, oldValue, true, newValue); }
        public String newContent() { return AuditLogLabels.content(entityName, action, newValue, false, newValue); }
        public String contentTone() {
            if ("Application".equals(entityName) && List.of("TalentPoolCancelled", "TalentPoolHrWithdrawn", "Withdrawn", "PostingClosed", "InterviewCancelledOnClose", "Rejected").contains(newValue == null ? "" : newValue)) return "danger";
            if (("Application".equals(entityName) || "InterviewSchedule".equals(entityName)) && newValue != null && newValue.startsWith("MailFailed:")) return "danger";
            return "LOGIN".equals(action) ? "neutral" : "success";
        }
    }
}
