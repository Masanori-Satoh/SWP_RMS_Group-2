package com.group2.rms.candidate.service;

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
 * Lưu CV vào ổ đĩa, dưới thư mục {@code app.storage.local-root} (mặc định {@code uploads/}, đã gitignore).
 * File nằm ngoài {@code static/} nên không ai tải trực tiếp được; chỉ đọc qua {@link #load(String)}.
 */
@Slf4j
@Component
public class LocalCvStorage implements CvStorage {

    /** Tiền tố của {@code AppliedCvUrl} cho file lưu cục bộ, ví dụ {@code local:cv/15/<uuid>.pdf}. */
    public static final String URL_PREFIX = "local:";

    private final Path root;

    public LocalCvStorage(@Value("${app.storage.local-root:uploads}") String root) {
        this.root = Path.of(root).toAbsolutePath().normalize();
    }

    @Override
    public StoredFile store(MultipartFile file, Integer candidateId) {
        String key = "cv/" + candidateId + "/" + UUID.randomUUID() + ".pdf";
        Path target = resolve(key);
        try {
            Files.createDirectories(target.getParent());
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not store CV file " + key, e);
        }
        return new StoredFile(key, URL_PREFIX + key);
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException e) {
            log.warn("Could not delete CV file {}", key, e);
        }
    }

    @Override
    public Resource load(String key) {
        Path file = resolve(key);
        if (!Files.isRegularFile(file)) {
            throw new ResourceNotFoundException("Không tìm thấy file CV.");
        }
        return new PathResource(file);
    }

    /** Khóa luôn do hệ thống sinh; vẫn chặn khóa trỏ ra ngoài thư mục gốc (ví dụ {@code ../}). */
    private Path resolve(String key) {
        Path file = root.resolve(key).normalize();
        if (!file.startsWith(root)) {
            throw new IllegalArgumentException("CV storage key points outside the storage root: " + key);
        }
        return file;
    }
}
