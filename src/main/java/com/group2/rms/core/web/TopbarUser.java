package com.group2.rms.core.web;

/** Thông tin người dùng hiển thị ở góc phải topbar (nội bộ và công khai). */
public record TopbarUser(
    String fullName,
    String email,
    String roleName,
    String avatarUrl
) {}
