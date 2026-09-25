package com.group2.rms.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

/**
 * 3.4 Department - Phòng ban trong tổ chức.
 * ManagerId trỏ về User.UserId (self-referencing qua bảng User).
 */
@Entity
@Table(name = "Department")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "DepartmentId")
    private Integer departmentId;

    @Column(name = "DepartmentName", nullable = false, length = 100)
    private String departmentName;

    /**
     * Trưởng phòng - tham chiếu đến User.
     * FetchType.LAZY để tránh load toàn bộ User khi chỉ cần Department.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ManagerId", referencedColumnName = "UserId")
    private User manager;

    @Column(name = "Status", nullable = false, length = 20, columnDefinition = "NVARCHAR(20) DEFAULT 'Active'")
    private String status;
}
