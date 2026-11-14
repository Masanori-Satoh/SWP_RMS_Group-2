package com.group2.rms.requisition.service;

import com.group2.rms.user.entity.Department;
import com.group2.rms.user.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/** Dữ liệu dropdown cho form yêu cầu tuyển dụng, để controller không gọi thẳng repository. */
@Service
@RequiredArgsConstructor
public class RequisitionFormOptions {

    private final DepartmentRepository departments;

    public List<Department> allDepartments() {
        return departments.findAll();
    }
}
