package com.group2.rms.user.validator;

import com.group2.rms.user.exception.AvatarUploadException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Arrays;

/**
 * Kiểm tra ảnh đại diện trước khi lưu: bắt buộc có, tối đa 2 MB, là JPEG/PNG/WebP thật.
 * Không tin tên file hay Content-Type trình duyệt gửi; loại ảnh nhận diện bằng byte đầu.
 * SVG bị chặn vì chứa được script; HEIC, GIF không nằm trong danh sách nhận.
 */
@Component
public class AvatarFileValidator {

    public static final long MAX_SIZE_BYTES = 2L * 1024 * 1024;

    private static final byte[] JPEG_SIGNATURE = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};
    private static final byte[] RIFF = {'R', 'I', 'F', 'F'};
    private static final byte[] WEBP = {'W', 'E', 'B', 'P'};

    /** Loại ảnh được nhận; đuôi file lưu xuống lấy theo loại này, không theo tên gốc. */
    public enum ImageType {
        JPEG("jpg", MediaType.IMAGE_JPEG_VALUE),
        PNG("png", MediaType.IMAGE_PNG_VALUE),
        WEBP("webp", "image/webp");

        private final String extension;
        private final String mediaType;

        ImageType(String extension, String mediaType) {
            this.extension = extension;
            this.mediaType = mediaType;
        }

        public String extension() {
            return extension;
        }

        public String mediaType() {
            return mediaType;
        }

        /** Loại ảnh theo đuôi file đã lưu; {@code null} nếu không phải đuôi hệ thống sinh ra. */
        public static ImageType fromExtension(String extension) {
            return Arrays.stream(values()).filter(t -> t.extension.equals(extension)).findFirst().orElse(null);
        }
    }

    public ImageType validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AvatarUploadException("Vui lòng chọn ảnh.");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new AvatarUploadException("Ảnh tối đa 2 MB.");
        }
        ImageType type = detect(file);
        if (type == null) {
            throw new AvatarUploadException("Chỉ nhận ảnh JPEG, PNG hoặc WebP.");
        }
        return type;
    }

    private static ImageType detect(MultipartFile file) {
        byte[] head;
        try (InputStream in = file.getInputStream()) {
            head = in.readNBytes(12);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read uploaded avatar file", e);
        }
        if (startsWith(head, 0, JPEG_SIGNATURE)) {
            return ImageType.JPEG;
        }
        if (startsWith(head, 0, PNG_SIGNATURE)) {
            return ImageType.PNG;
        }
        if (startsWith(head, 0, RIFF) && startsWith(head, 8, WEBP)) {
            return ImageType.WEBP;
        }
        return null;
    }

    private static boolean startsWith(byte[] data, int offset, byte[] signature) {
        return data.length >= offset + signature.length
                && Arrays.equals(data, offset, offset + signature.length, signature, 0, signature.length);
    }
}
