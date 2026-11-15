package com.group2.rms.admin.service;

import com.group2.rms.admin.exception.PolicyDocumentException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;

/*
 * Đọc / thay file PDF của 2 tài liệu chính sách.
 * Admin đã tải → {local-root}/policy/<tên cố định>; chưa tải → bản mặc định trong classpath documents/policy/.
 * Không lo audit hay quyền (PolicyDocumentService, SecurityConfig).
 */
@Component
public class PolicyDocumentStorage {

    public static final long MAX_BYTES = 10L * 1024 * 1024;
    public static final String EMPTY_FILE = "Vui lòng chọn file PDF.";
    public static final String TOO_LARGE = "File vượt quá 10MB.";
    public static final String NOT_PDF = "File không phải PDF. Vui lòng chọn file .pdf.";

    private static final byte[] PDF_SIGNATURE = "%PDF-".getBytes(StandardCharsets.US_ASCII);
    private static final String DEFAULT_FOLDER = "documents/policy/";

    private final Path folder;

    public PolicyDocumentStorage(@Value("${app.storage.local-root:uploads}") String root) {
        this.folder = Path.of(root, "policy").toAbsolutePath().normalize();
    }

    /** File ứng viên đang đọc: bản Admin tải nếu có, không thì bản mặc định. */
    public Resource read(PolicyDocument document) {
        Path uploaded = uploadedPath(document);
        if (Files.isRegularFile(uploaded)) {
            return new PathResource(uploaded);
        }
        return new ClassPathResource(DEFAULT_FOLDER + document.fileName());
    }

    public boolean isUploaded(PolicyDocument document) {
        return Files.isRegularFile(uploadedPath(document));
    }

    /** Kích thước file đang dùng (byte); không đọc được → 0. */
    public long size(PolicyDocument document) {
        try {
            return read(document).contentLength();
        } catch (IOException e) {
            return 0;
        }
    }

    /** Kiểm tra rồi thay file: ghi ra file tạm trước, xong mới đổi chỗ để lỗi giữa chừng không làm hỏng bản đang dùng. */
    public void replace(PolicyDocument document, MultipartFile file) {
        validate(file);
        Path target = uploadedPath(document);
        Path temp = null;
        try {
            Files.createDirectories(folder);
            temp = Files.createTempFile(folder, document.code(), ".tmp");
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING);
            }
            moveIntoPlace(temp, target);
        } catch (IOException e) {
            deleteQuietly(temp);
            throw new UncheckedIOException("Could not store policy document " + document.fileName(), e);
        }
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new PolicyDocumentException(EMPTY_FILE);
        }
        if (file.getSize() > MAX_BYTES) {
            throw new PolicyDocumentException(TOO_LARGE);
        }
        // Rule: tin nội dung file (5 byte đầu "%PDF-"), không tin đuôi file hay Content-Type trình duyệt gửi
        if (!startsWithPdfSignature(file)) {
            throw new PolicyDocumentException(NOT_PDF);
        }
    }

    private boolean startsWithPdfSignature(MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            byte[] head = in.readNBytes(PDF_SIGNATURE.length);
            return Arrays.equals(head, PDF_SIGNATURE);
        } catch (IOException e) {
            return false;
        }
    }

    private void moveIntoPlace(Path temp, Path target) throws IOException {
        try {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void deleteQuietly(Path file) {
        if (file == null) {
            return;
        }
        try {
            Files.deleteIfExists(file);
        } catch (IOException ignored) {
            // File tạm sót lại không ảnh hưởng bản đang dùng
        }
    }

    private Path uploadedPath(PolicyDocument document) {
        return folder.resolve(document.fileName());
    }
}
