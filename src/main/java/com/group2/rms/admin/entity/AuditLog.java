package com.group2.rms.admin.entity;

import com.group2.rms.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 3.5 AuditLog - Nhật ký hành động trong hệ thống.
 * Lưu các thao tác được schema hỗ trợ: CREATE, UPDATE, DELETE, LOGIN.
 * Dùng BIGINT cho AuditLogId do log có thể rất lớn.
 */
@Entity
@Table(name = "AuditLog")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "AuditLogId")
    private Long auditLogId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UserId", nullable = false, referencedColumnName = "UserId")
    private User user;

    /**
     * Loại hành động: CREATE, UPDATE, DELETE, LOGIN
     */
    @Column(name = "Action", nullable = false, length = 50)
    private String action;

    @Column(name = "EntityName", nullable = false, length = 100)
    private String entityName;

    @Column(name = "EntityId", nullable = false, length = 50)
    private String entityId;

    /**
     * Dữ liệu cũ dạng JSON (nvarchar(max))
     */
    @Column(name = "OldValue", columnDefinition = "NVARCHAR(MAX)")
    private String oldValue;

    /**
     * Dữ liệu mới dạng JSON (nvarchar(max))
     */
    @Column(name = "NewValue", columnDefinition = "NVARCHAR(MAX)")
    private String newValue;

    @Column(name = "IpAddress", length = 45)
    private String ipAddress;

    @Column(name = "Timestamp", nullable = false)
    private LocalDateTime timestamp;

    @PrePersist
    protected void onPersist() {
        if (this.timestamp == null) {
            this.timestamp = LocalDateTime.now();
        }
    }
}
