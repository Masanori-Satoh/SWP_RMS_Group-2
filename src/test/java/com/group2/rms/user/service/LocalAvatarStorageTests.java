package com.group2.rms.user.service;

import com.group2.rms.core.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class LocalAvatarStorageTests {

    @TempDir
    Path root;

    @Test
    void storesUnderAvatarFolderWithSystemNameAndExtension() throws Exception {
        LocalAvatarStorage storage = new LocalAvatarStorage(root.toString());
        String key = storage.store(new MockMultipartFile("avatarFile", "../../evil.svg", "image/svg+xml", new byte[]{1, 2, 3}), 7, "png");

        assertTrue(key.matches("avatar/7/[0-9a-f-]{36}\\.png"), key);
        assertArrayEquals(new byte[]{1, 2, 3}, Files.readAllBytes(root.resolve(key)));
        assertTrue(storage.exists(key));
        assertTrue(storage.load(key).exists());

        storage.delete(key);
        assertFalse(storage.exists(key));
        assertThrows(ResourceNotFoundException.class, () -> storage.load(key));
        storage.delete(key); // xóa lần hai không lỗi
    }

    @Test
    void keyOfOnlyAcceptsUploadedAvatars() {
        assertEquals("avatar/7/x.png", LocalAvatarStorage.keyOf("local:avatar/7/x.png"));
        assertNull(LocalAvatarStorage.keyOf(null));
        assertNull(LocalAvatarStorage.keyOf("https://ui-avatars.com/api/?name=A&background=random"));
        assertNull(LocalAvatarStorage.keyOf("local:cv/15/x.pdf"));
    }

    @Test
    void keysOutsideRootAreTreatedAsMissing() {
        LocalAvatarStorage storage = new LocalAvatarStorage(root.resolve("uploads").toString());
        assertFalse(storage.exists("avatar/../../secret.txt"));
        assertThrows(ResourceNotFoundException.class, () -> storage.load("avatar/../../secret.txt"));
        storage.delete("avatar/../../secret.txt");
    }
}
