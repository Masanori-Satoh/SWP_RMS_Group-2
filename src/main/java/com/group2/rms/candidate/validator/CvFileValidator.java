package com.group2.rms.candidate.validator;

import com.group2.rms.candidate.exception.ApplicationSubmissionException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;

/**
 * Kiểm tra file CV trước khi lưu: bắt buộc có, tối đa 5 MB, là PDF thật.
 * Không tin Content-Type trình duyệt gửi; PDF được nhận diện bằng 5 byte đầu {@code %PDF-}.
 */
@Component
public class CvFileValidator {

    public static final String FIELD = "cvFile";
    public static final long MAX_SIZE_BYTES = 5L * 1024 * 1024;

    private static final byte[] PDF_SIGNATURE = "%PDF-".getBytes(StandardCharsets.US_ASCII);

    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApplicationSubmissionException(FIELD, "Vui lòng chọn file CV.");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new ApplicationSubmissionException(FIELD, "File CV tối đa 5 MB.");
        }
        if (!hasPdfExtension(file.getOriginalFilename()) || !startsWithPdfSignature(file)) {
            throw new ApplicationSubmissionException(FIELD, "Chỉ nhận file PDF.");
        }
    }

    private static boolean hasPdfExtension(String filename) {
        return filename != null && filename.toLowerCase(Locale.ROOT).endsWith(".pdf");
    }

    private static boolean startsWithPdfSignature(MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            return Arrays.equals(in.readNBytes(PDF_SIGNATURE.length), PDF_SIGNATURE);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read uploaded CV file", e);
        }
    }
}
