package com.group2.rms.candidate.service;

import com.group2.rms.core.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** Lưu/đọc/xóa CV trên thư mục tạm; không Spring context, không database. */
class LocalCvStorageTests {

    private static final byte[] PDF = "%PDF-1.7 sample".getBytes(StandardCharsets.US_ASCII);

    @TempDir
    Path root;

    private LocalCvStorage storage;

    @BeforeEach
    void setUp() {
        storage = new LocalCvStorage(root.toString());
    }

    @Test
    void storeWritesFileUnderCandidateFolderWithGeneratedName() throws Exception {
        StoredFile stored = storage.store(cv("Nguyen Van A - CV.pdf"), 15);

        assertTrue(stored.key().matches("cv/15/[0-9a-f-]{36}\\.pdf"), stored.key());
        assertFalse(stored.key().contains("Nguyen"));
        assertEquals(LocalCvStorage.URL_PREFIX + stored.key(), stored.url());
        assertArrayEquals(PDF, Files.readAllBytes(root.resolve(stored.key())));
    }

    @Test
    void storeGivesEachUploadItsOwnFile() {
        StoredFile first = storage.store(cv("cv.pdf"), 15);
        StoredFile second = storage.store(cv("cv.pdf"), 15);

        assertNotEquals(first.key(), second.key());
    }

    @Test
    void loadReturnsStoredContent() throws Exception {
        StoredFile stored = storage.store(cv("cv.pdf"), 15);

        Resource resource = storage.load(stored.key());

        assertArrayEquals(PDF, resource.getContentAsByteArray());
    }

    @Test
    void loadMissingFileThrowsNotFound() {
        assertThrows(ResourceNotFoundException.class, () -> storage.load("cv/15/missing.pdf"));
    }

    @Test
    void deleteRemovesFile() {
        StoredFile stored = storage.store(cv("cv.pdf"), 15);

        storage.delete(stored.key());

        assertFalse(Files.exists(root.resolve(stored.key())));
    }

    @Test
    void deleteMissingFileDoesNotThrow() {
        assertDoesNotThrow(() -> storage.delete("cv/15/missing.pdf"));
    }

    @Test
    void keyOutsideRootIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> storage.load("../outside.pdf"));
        assertThrows(IllegalArgumentException.class, () -> storage.delete("cv/../../outside.pdf"));
    }

    private static MockMultipartFile cv(String originalName) {
        return new MockMultipartFile("cvFile", originalName, "application/pdf", PDF);
    }
}
