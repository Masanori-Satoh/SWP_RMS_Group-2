package com.group2.rms.user.entity;

import com.group2.rms.core.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;

/**
 * 3.1 User - Tài khoản người dùng trong hệ thống.
 * Các trạng thái AccountStatus: Active, Inactive, Blocked
 */
@Entity
@Table(name = "\"User\"") // Quote the reserved SQL Server identifier without making brackets part of its name.
@DynamicUpdate
@Getter
@Setter
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

    @Column(name = "Email", nullable = false, unique = true, length = 150)
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

    public User() {
    }

    public User(Integer userId, Role role, String username, String passwordHash, String email,
                String fullName, String phoneNumber, String avatarUrl, Department department, String accountStatus) {
        this.userId = userId;
        this.role = role;
        this.username = username;
        this.passwordHash = passwordHash;
        this.email = email;
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.avatarUrl = avatarUrl;
        this.department = department;
        this.accountStatus = accountStatus;
    }

    public static UserBuilder builder() {
        return new UserBuilder();
    }

    public static class UserBuilder {
        private Integer userId;
        private Role role;
        private String username;
        private String passwordHash;
        private String email;
        private String fullName;
        private String phoneNumber;
        private String avatarUrl;
        private Department department;
        private String accountStatus;

        public UserBuilder userId(Integer userId) {
            this.userId = userId;
            return this;
        }

        public UserBuilder role(Role role) {
            this.role = role;
            return this;
        }

        public UserBuilder username(String username) {
            this.username = username;
            return this;
        }

        public UserBuilder passwordHash(String passwordHash) {
            this.passwordHash = passwordHash;
            return this;
        }

        public UserBuilder email(String email) {
            this.email = email;
            return this;
        }

        public UserBuilder fullName(String fullName) {
            this.fullName = fullName;
            return this;
        }

        public UserBuilder phoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
            return this;
        }

        public UserBuilder avatarUrl(String avatarUrl) {
            this.avatarUrl = avatarUrl;
            return this;
        }

        public UserBuilder department(Department department) {
            this.department = department;
            return this;
        }

        public UserBuilder accountStatus(String accountStatus) {
            this.accountStatus = accountStatus;
            return this;
        }

        public User build() {
            return new User(userId, role, username, passwordHash, email, fullName, phoneNumber, avatarUrl, department, accountStatus);
        }
    }
}
