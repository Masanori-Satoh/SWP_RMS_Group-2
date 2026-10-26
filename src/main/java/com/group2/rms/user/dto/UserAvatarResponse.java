package com.group2.rms.user.dto;

import org.springframework.core.io.Resource;

/** File ảnh đại diện để trả về trình duyệt, kèm Content-Type theo loại ảnh đã lưu. */
public record UserAvatarResponse(Resource file, String contentType) {
}
