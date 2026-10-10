package com.group2.rms.candidate.validator;

import com.group2.rms.candidate.exception.ApplicationSubmissionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/** Kiểm tra file CV; không Spring context, không database. */
class CvFileValidatorTests {

    private static final byte[] PDF = "%PDF-1.7 sample".getBytes(StandardCharsets.US_ASCII);

    private final CvFileValidator validator = new CvFileValidator();

    @Test
    void acceptsRealPdf() {
        assertDoesNotThrow(() -> validator.validate(file("cv.pdf", PDF)));
    }

    @Test
    void acceptsUppercaseExtension() {
        assertDoesNotThrow(() -> validator.validate(file("CV.PDF", PDF)));
    }

    @Test
    void acceptsExactlyFiveMegabytes() {
        assertDoesNotThrow(() -> validator.validate(file("cv.pdf", pdfOfSize(CvFileValidator.MAX_SIZE_BYTES))));
    }

    @Test
    void rejectsMissingFile() {
        assertRejected(null, "Vui lòng chọn file CV.");
    }

    @Test
    void rejectsEmptyFile() {
        assertRejected(file("", new byte[0]), "Vui lòng chọn file CV.");
    }

    @Test
    void rejectsFileOverFiveMegabytes() {
        assertRejected(file("cv.pdf", pdfOfSize(CvFileValidator.MAX_SIZE_BYTES + 1)), "File CV tối đa 5 MB.");
    }

    @ParameterizedTest
    @ValueSource(strings = { "cv.docx", "cv.png", "cv.pdf.exe", "cv" })
    void rejectsNonPdfExtension(String filename) {
        assertRejected(file(filename, PDF), "Chỉ nhận file PDF.");
    }

    @Test
    void rejectsRenamedFileWithoutPdfSignature() {
        byte[] png = { (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A };
        assertRejected(file("photo.pdf", png), "Chỉ nhận file PDF.");
    }

    private void assertRejected(MultipartFile file, String message) {
        ApplicationSubmissionException e = assertThrows(ApplicationSubmissionException.class,
                () -> validator.validate(file));
        assertEquals(CvFileValidator.FIELD, e.getField());
        assertEquals(message, e.getMessage());
    }

    private static MockMultipartFile file(String name, byte[] content) {
        return new MockMultipartFile("cvFile", name, "application/pdf", content);
    }

    private static byte[] pdfOfSize(long size) {
        byte[] content = Arrays.copyOf(PDF, (int) size);
        Arrays.fill(content, PDF.length, content.length, (byte) ' ');
        return content;
    }
}
