package com.group2.rms.user.exception;

import com.group2.rms.core.exception.BaseBusinessException;

/** Ảnh đại diện tải lên không hợp lệ (thiếu file, quá cỡ, sai loại). Controller hiện lời nhắn bằng flash. */
public class AvatarUploadException extends BaseBusinessException {

    public AvatarUploadException(String message) {
        super(message);
    }
}
