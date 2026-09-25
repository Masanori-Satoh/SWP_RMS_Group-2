package com.group2.rms.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 3.1 User - Tài khoản người dùng trong hệ thống.
 * Các trạng thái AccountStatus: Active, Inactive, Blocked
 */
@Entity
@Table(name = "[User]") // Dùng bracket vì "User" là từ khóa reserved trong SQL Server
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "UserId")
    private Integer userId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "RoleId", nullable = false, referencedColumnName = "RoleId")
    private Role role;

    @Column(name = "Username", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "PasswordHash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "Email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "FullName", nullable = false, length = 100)
    private String fullName;

    @Column(name = "PhoneNumber", length = 20)
    private String phoneNumber;

    @Column(name = "AvatarUrl", length = 500)
    private String avatarUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DepartmentId", referencedColumnName = "DepartmentId")
    private Department department;

    /**
     * Trạng thái tài khoản: Active, Inactive, Blocked
     */
    @Column(name = "AccountStatus", nullable = false, length = 20)
    private String accountStatus;
}
