package com.group2.rms.admin.service;

import com.group2.rms.admin.exception.PolicyDocumentException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/*
 * UNIT TEST (thư mục tạm thật): PolicyDocumentStorage — đọc bản mặc định / bản Admin tải, kiểm tra file PDF.
 */
class PolicyDocumentStorageTests {

    private static final byte[] PDF = "%PDF-1.4\nnoi dung moi".getBytes(StandardCharsets.US_ASCII);

    @TempDir
    Path root;

    private PolicyDocumentStorage storage;

    @BeforeEach
    void setUp() {
        storage = new PolicyDocumentStorage(root.toString());
    }

    @Test
    @DisplayName("Chưa ai tải file -> Đọc bản mặc định trong classpath (có thật)")
    void read_shouldUseClasspathDefault_whenNothingUploaded() {
        Resource file = storage.read(PolicyDocument.TERMS);

        assertInstanceOf(ClassPathResource.class, file);
        assertTrue(file.exists());
        assertFalse(storage.isUploaded(PolicyDocument.TERMS));
    }

    @Test
    @DisplayName("Admin tải PDF hợp lệ -> Đọc ra đúng file mới, tài liệu kia không đổi")
    void replace_shouldServeNewFile_whenPdfIsValid() throws Exception {
        storage.replace(PolicyDocument.PRIVACY, pdf("chinh-sach.pdf", PDF));

        assertTrue(storage.isUploaded(PolicyDocument.PRIVACY));
        assertArrayEquals(PDF, storage.read(PolicyDocument.PRIVACY).getInputStream().readAllBytes());
        assertFalse(storage.isUploaded(PolicyDocument.TERMS));
        assertEquals(PDF.length, storage.size(PolicyDocument.PRIVACY));
    }

    @Test
    @DisplayName("File .docx đổi đuôi thành .pdf -> Báo không phải PDF, giữ nguyên bản đang dùng")
    void replace_shouldRejectAndKeepOldFile_whenContentIsNotPdf() throws Exception {
        // Arrange: đã có một bản hợp lệ
        storage.replace(PolicyDocument.TERMS, pdf("cu.pdf", PDF));
        MultipartFile fake = pdf("gia.pdf", "PK\u0003\u0004 word".getBytes(StandardCharsets.ISO_8859_1));

        // Act
        PolicyDocumentException error = assertThrows(PolicyDocumentException.class,
                () -> storage.replace(PolicyDocument.TERMS, fake));

        // Assert
        assertEquals(PolicyDocumentStorage.NOT_PDF, error.getMessage());
        assertArrayEquals(PDF, Files.readAllBytes(root.resolve("policy").resolve("terms.pdf")));
    }

    @Test
    @DisplayName("Không chọn file / file rỗng -> Báo bắt buộc chọn file, không tạo file")
    void replace_shouldReject_whenFileMissingOrEmpty() {
        PolicyDocumentException missing = assertThrows(PolicyDocumentException.class,
                () -> storage.replace(PolicyDocument.TERMS, null));
        PolicyDocumentException empty = assertThrows(PolicyDocumentException.class,
                () -> storage.replace(PolicyDocument.TERMS, pdf("rong.pdf", new byte[0])));

        assertEquals(PolicyDocumentStorage.EMPTY_FILE, missing.getMessage());
        assertEquals(PolicyDocumentStorage.EMPTY_FILE, empty.getMessage());
        assertFalse(storage.isUploaded(PolicyDocument.TERMS));
    }

    @Test
    @DisplayName("File lớn hơn 10MB -> Báo vượt dung lượng, không tạo file")
    void replace_shouldReject_whenFileTooLarge() {
        MultipartFile huge = mock(MultipartFile.class);
        when(huge.isEmpty()).thenReturn(false);
        when(huge.getSize()).thenReturn(PolicyDocumentStorage.MAX_BYTES + 1);

        PolicyDocumentException error = assertThrows(PolicyDocumentException.class,
                () -> storage.replace(PolicyDocument.TERMS, huge));

        assertEquals(PolicyDocumentStorage.TOO_LARGE, error.getMessage());
        assertFalse(storage.isUploaded(PolicyDocument.TERMS));
    }

    private static MultipartFile pdf(String name, byte[] content) {
        return new MockMultipartFile("file", name, "application/pdf", content);
    }
}
