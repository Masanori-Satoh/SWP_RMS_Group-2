package com.group2.rms.user.service;

import com.group2.rms.core.exception.ResourceNotFoundException;
import com.group2.rms.core.security.RoleAuthorities;
import com.group2.rms.user.dto.*;
import com.group2.rms.user.entity.Department;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.exception.DepartmentFieldException;
import com.group2.rms.user.repository.DepartmentRepository;
import com.group2.rms.user.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;

@Service
public class DepartmentService {
    private final DepartmentRepository departments;
    private final UserRepository users;

    public DepartmentService(DepartmentRepository departments, UserRepository users) {
        this.departments = departments;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public Page<DepartmentResponse> findDepartments(String search, String status, int page) {
        String term = search == null ? "" : search.strip().toLowerCase(Locale.ROOT);
        Specification<Department> filter = (root, query, builder) -> {
            var byName = builder.like(builder.lower(root.get("departmentName")),
                    "%" + term.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%", '\\');
            return "Active".equals(status) || "Inactive".equals(status)
                    ? builder.and(byName, builder.equal(root.get("departmentStatus"), status)) : byName;
        };
        return departments.findAll(filter, PageRequest.of(Math.max(0, page), 10,
                Sort.by("departmentName", "departmentId"))).map(this::response);
    }

    @Transactional(readOnly = true)
    public DepartmentResponse findForEdit(int id) { return response(find(id)); }

    @Transactional(readOnly = true)
    public List<DepartmentManagerResponse> managerChoices(int departmentId) {
        return users.findAllByDepartmentDepartmentIdAndAccountStatusOrderByFullNameAscUserIdAsc(departmentId, "Active")
                .stream().filter(user -> RoleAuthorities.INTERNAL_ROLE_NAMES.contains(user.getRole().getRoleName()))
                .map(user -> new DepartmentManagerResponse(user.getUserId(), user.getFullName(), user.getUsername())).toList();
    }

    @Transactional
    public int create(DepartmentRequest form) {
        String name = validateName(null, form);
        if (form.managerId() != null) {
            throw invalid("managerId", "Hãy tạo phòng ban và gán nhân viên vào phòng ban trước khi chọn trưởng phòng.", null, form);
        }
        var department = Department.builder().departmentName(name).departmentStatus("Active").build();
        persist(department, null, form);
        return department.getDepartmentId();
    }

    @Transactional
    public void update(int id, DepartmentRequest form) {
        Department department = find(id);
        String name = validateName(id, form);
        User manager = null;
        if (form.managerId() != null) {
            manager = users.findById(form.managerId()).orElse(null);
            if (manager == null || !"Active".equals(manager.getAccountStatus())
                    || !RoleAuthorities.INTERNAL_ROLE_NAMES.contains(manager.getRole().getRoleName())
                    || manager.getDepartment() == null || !Integer.valueOf(id).equals(manager.getDepartment().getDepartmentId())) {
                throw invalid("managerId", "Trưởng phòng phải là tài khoản nội bộ đang hoạt động thuộc chính phòng ban này.", id, form);
            }
        }
        department.setDepartmentName(name);
        department.setManager(manager);
        persist(department, id, form);
    }

    @Transactional
    public void deactivate(int id) { setStatus(id, "Inactive"); }

    @Transactional
    public void activate(int id) { setStatus(id, "Active"); }

    private void setStatus(int id, String status) {
        Department department = find(id);
        if (!status.equals(department.getDepartmentStatus())) {
            department.setDepartmentStatus(status);
            departments.saveAndFlush(department);
        }
    }

    private String validateName(Integer id, DepartmentRequest form) {
        String name = form.departmentName() == null ? "" : form.departmentName().strip();
        if (name.isBlank() || name.length() > 100) {
            throw invalid("departmentName", "Tên phòng ban bắt buộc và không được quá 100 ký tự.", id, form);
        }
        boolean duplicate = id == null ? departments.existsByDepartmentNameIgnoreCase(name)
                : departments.existsByDepartmentNameIgnoreCaseAndDepartmentIdNot(name, id);
        if (duplicate) throw invalid("departmentName", "Tên phòng ban đã được sử dụng, kể cả phòng ban đã vô hiệu hóa.", id, form);
        return name;
    }

    private void persist(Department department, Integer id, DepartmentRequest form) {
        try {
            departments.saveAndFlush(department);
        } catch (DataIntegrityViolationException exception) {
            // SQL Server duplicate-key errors only. FK/configuration/system errors are not hidden.
            for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
                if (cause instanceof SQLException sql && (sql.getErrorCode() == 2601 || sql.getErrorCode() == 2627)) {
                    throw invalid("departmentName", "Tên phòng ban đã được sử dụng.", id, form);
                }
            }
            throw exception;
        }
    }

    private Department find(int id) {
        return departments.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phòng ban."));
    }

    private DepartmentFieldException invalid(String field, String message, Integer id, DepartmentRequest form) {
        return new DepartmentFieldException(field, message, id, form);
    }

    private DepartmentResponse response(Department department) {
        User manager = department.getManager();
        return new DepartmentResponse(department.getDepartmentId(), department.getDepartmentName(),
                manager == null ? null : manager.getUserId(), manager == null ? null : manager.getFullName(),
                manager == null ? null : manager.getUsername(), department.getDepartmentStatus());
    }
}
