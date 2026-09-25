package com.group2.rms.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 3.2 Role - Bảng vai trò trong hệ thống.
 * Các role: System Admin, HR, Hiring Manager, Director, Interviewer
 */
@Entity
@Table(name = "Role")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "RoleId")
    private Integer roleId;

    @Column(name = "RoleName", nullable = false, unique = true, length = 50)
    private String roleName;

    @Column(name = "Description", length = 255)
    private String description;
}
