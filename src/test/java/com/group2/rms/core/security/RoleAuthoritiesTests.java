package com.group2.rms.core.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RoleAuthoritiesTests {
    @Test
    @DisplayName("SEC-ROLE-1 | Role System Admin: trim và trả đúng authority")
    void fromRoleName_admin_returnsExpectedAuthority() {
        // Given: Role System Admin có khoảng trắng đầu/cuối.
        String input = "  System Admin  ";

        // When: Ánh xạ tên role sang authority.
        String result = RoleAuthorities.fromRoleName(input);

        // Then: Trả đúng authority đã định nghĩa.
        assertEquals("ROLE_SYSTEM_ADMIN", result);
    }

    @Test
    @DisplayName("SEC-ROLE-2 | Role HR: trim và trả đúng authority")
    void fromRoleName_hr_returnsExpectedAuthority() {
        // Given: Role HR có khoảng trắng đầu/cuối.
        String input = "  HR  ";

        // When: Ánh xạ tên role sang authority.
        String result = RoleAuthorities.fromRoleName(input);

        // Then: Trả đúng authority đã định nghĩa.
        assertEquals("ROLE_HR", result);
    }

    @Test
    @DisplayName("SEC-ROLE-3 | Role Hiring Manager: trim và trả đúng authority")
    void fromRoleName_hm_returnsExpectedAuthority() {
        // Given: Role Hiring Manager có khoảng trắng đầu/cuối.
        String input = "  Hiring Manager  ";

        // When: Ánh xạ tên role sang authority.
        String result = RoleAuthorities.fromRoleName(input);

        // Then: Trả đúng authority đã định nghĩa.
        assertEquals("ROLE_HIRING_MANAGER", result);
    }

    @Test
    @DisplayName("SEC-ROLE-4 | Role Director: trim và trả đúng authority")
    void fromRoleName_director_returnsExpectedAuthority() {
        // Given: Role Director có khoảng trắng đầu/cuối.
        String input = "  Director  ";

        // When: Ánh xạ tên role sang authority.
        String result = RoleAuthorities.fromRoleName(input);

        // Then: Trả đúng authority đã định nghĩa.
        assertEquals("ROLE_DIRECTOR", result);
    }

    @Test
    @DisplayName("SEC-ROLE-5 | Role Interviewer: trim và trả đúng authority")
    void fromRoleName_interviewer_returnsExpectedAuthority() {
        // Given: Role Interviewer có khoảng trắng đầu/cuối.
        String input = "  Interviewer  ";

        // When: Ánh xạ tên role sang authority.
        String result = RoleAuthorities.fromRoleName(input);

        // Then: Trả đúng authority đã định nghĩa.
        assertEquals("ROLE_INTERVIEWER", result);
    }

    @Test
    @DisplayName("SEC-ROLE-6 | Role Candidate: trim và trả đúng authority")
    void fromRoleName_candidate_returnsExpectedAuthority() {
        // Given: Role Candidate có khoảng trắng đầu/cuối.
        String input = "  Candidate  ";

        // When: Ánh xạ tên role sang authority.
        String result = RoleAuthorities.fromRoleName(input);

        // Then: Trả đúng authority đã định nghĩa.
        assertEquals("ROLE_CANDIDATE", result);
    }

    @Test
    @DisplayName("SEC-ROLE-7 | Role null: từ chối giá trị không hợp lệ")
    void fromRoleName_null_throwsIllegalArgumentException() {
        // Given: Đầu vào role là null.

        // When: Ánh xạ role không hợp lệ.
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
            () -> RoleAuthorities.fromRoleName(null));

        // Then: Kiểm tra đúng exception và thông báo.
        assertEquals("Role is missing", error.getMessage());
    }

    @Test
    @DisplayName("SEC-ROLE-8 | Role empty: từ chối giá trị không hợp lệ")
    void fromRoleName_empty_throwsIllegalArgumentException() {
        // Given: Đầu vào role là "".

        // When: Ánh xạ role không hợp lệ.
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
            () -> RoleAuthorities.fromRoleName(""));

        // Then: Kiểm tra đúng exception và thông báo.
        assertEquals("Unsupported role", error.getMessage());
    }

    @Test
    @DisplayName("SEC-ROLE-9 | Role blank: từ chối giá trị không hợp lệ")
    void fromRoleName_blank_throwsIllegalArgumentException() {
        // Given: Đầu vào role là " ".

        // When: Ánh xạ role không hợp lệ.
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
            () -> RoleAuthorities.fromRoleName(" "));

        // Then: Kiểm tra đúng exception và thông báo.
        assertEquals("Unsupported role", error.getMessage());
    }

    @Test
    @DisplayName("SEC-ROLE-10 | Role unknown: từ chối giá trị không hợp lệ")
    void fromRoleName_unknown_throwsIllegalArgumentException() {
        // Given: Đầu vào role là "Unknown".

        // When: Ánh xạ role không hợp lệ.
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
            () -> RoleAuthorities.fromRoleName("Unknown"));

        // Then: Kiểm tra đúng exception và thông báo.
        assertEquals("Unsupported role", error.getMessage());
    }

    @Test
    @DisplayName("SEC-ROLE-11 | Role wrongCase: từ chối giá trị không hợp lệ")
    void fromRoleName_wrongCase_throwsIllegalArgumentException() {
        // Given: Đầu vào role là "hr".

        // When: Ánh xạ role không hợp lệ.
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
            () -> RoleAuthorities.fromRoleName("hr"));

        // Then: Kiểm tra đúng exception và thông báo.
        assertEquals("Unsupported role", error.getMessage());
    }

}

