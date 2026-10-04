package com.group2.rms.user.dto;

public record DepartmentResponse(Integer id, String name, Integer managerId,
                                 String managerName, String managerUsername, String status) { }
