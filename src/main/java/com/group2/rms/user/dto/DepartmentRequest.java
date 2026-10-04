package com.group2.rms.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record DepartmentRequest(
        @NotBlank(message = "Vui lòng nhập tên phòng ban.")
        @Size(max = 100, message = "Tên phòng ban không được quá 100 ký tự.") String departmentName,
        @Positive(message = "Trưởng phòng không hợp lệ.") Integer managerId) { }
