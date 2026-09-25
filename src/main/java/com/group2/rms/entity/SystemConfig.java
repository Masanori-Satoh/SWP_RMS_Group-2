package com.group2.rms.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 3.6 SystemConfig - Cấu hình hệ thống dạng key-value.
 * Primary Key là ConfigKey (String) thay vì IDENTITY.
 * Ví dụ: AI_SCREENING_ENDPOINT, EMAIL_SMTP
 */
@Entity
@Table(name = "SystemConfig")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemConfig {

    @Id
    @Column(name = "ConfigKey", length = 100)
    private String configKey;

    @Column(name = "ConfigValue", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String configValue;

    @Column(name = "Description", length = 500)
    private String description;

    @Column(name = "UpdatedAt", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
