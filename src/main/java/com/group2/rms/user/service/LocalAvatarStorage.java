package com.group2.rms.user.service;

import com.group2.rms.core.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Lưu ảnh đại diện vào ổ đĩa, dưới thư mục {@code app.storage.local-root} (mặc định {@code uploads/}, đã gitignore),
 * cùng gốc với CV ({@code LocalCvStorage}). File nằm ngoài {@code static/}: chỉ xem qua {@code /users/{id}/avatar}.
 * Máy nào tải thì máy đó có file; DB trỏ tới file không có trên máy thì coi như chưa có ảnh.
 */
@Slf4j
@Component
public class LocalAvatarStorage {

    /** Tiền tố của {@code [User].AvatarUrl} cho ảnh tải lên, ví dụ {@code local:avatar/7/<uuid>.png}. */
    public static final String URL_PREFIX = "local:";

    private static final String AVATAR_DIR = "avatar/";

    private final Path root;

    public LocalAvatarStorage(@Value("${app.storage.local-root:uploads}") String root) {
        this.root = Path.of(root).toAbsolutePath().normalize();
    }

    /**
     * Khóa file của một giá trị {@code AvatarUrl}; {@code null} nếu không phải ảnh tải lên
     * (NULL, link ngoài như {@code ui-avatars} của seed cũ, hoặc khóa của loại file khác).
     */
    public static String keyOf(String avatarUrl) {
        if (avatarUrl == null || !avatarUrl.startsWith(URL_PREFIX + AVATAR_DIR)) {
            return null;
        }
        return avatarUrl.substring(URL_PREFIX.length());
    }

    /** Lưu file đã được kiểm tra hợp lệ; tên file do hệ thống sinh, không dùng tên gốc. Trả về khóa. */
    public String store(MultipartFile file, Integer userId, String extension) {
        String key = AVATAR_DIR + userId + "/" + UUID.randomUUID() + "." + extension;
        Path target = resolve(key);
        try {
            Files.createDirectories(target.getParent());
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not store avatar file " + key, e);
        }
        return key;
    }

    /** Xóa file theo khóa; không ném lỗi nếu file không còn. */
    public void delete(String key) {
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException | IllegalArgumentException e) {
            log.warn("Could not delete avatar file {}", key, e);
        }
    }

    /** File có thật trên máy này không; khóa trỏ ra ngoài thư mục gốc coi như không có. */
    public boolean exists(String key) {
        try {
            return Files.isRegularFile(resolve(key));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /** Đọc file theo khóa; không có file thì ném {@code ResourceNotFoundException}. */
    public Resource load(String key) {
        if (!exists(key)) {
            throw new ResourceNotFoundException("Không tìm thấy ảnh đại diện.");
        }
        return new PathResource(resolve(key));
    }

    /** Khóa luôn do hệ thống sinh; vẫn chặn khóa trỏ ra ngoài thư mục gốc (ví dụ {@code ../}). */
    private Path resolve(String key) {
        Path file = root.resolve(key).normalize();
        if (!file.startsWith(root)) {
            throw new IllegalArgumentException("Avatar storage key points outside the storage root: " + key);
        }
        return file;
    }
}
