package com.group2.rms.user.validator;

import com.group2.rms.user.exception.AvatarUploadException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AvatarFileValidatorTests {

    static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10, 'J', 'F', 'I', 'F', 0, 1};
    static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0, 0, 0, 0x0D};
    static final byte[] WEBP = {'R', 'I', 'F', 'F', 0x24, 0, 0, 0, 'W', 'E', 'B', 'P', 'V', 'P', '8', ' '};

    private final AvatarFileValidator validator = new AvatarFileValidator();

    @Test
    void acceptsJpegPngWebpByContent() {
        assertEquals(AvatarFileValidator.ImageType.JPEG, validator.validate(file("a.jpg", JPEG)));
        assertEquals(AvatarFileValidator.ImageType.PNG, validator.validate(file("a.png", PNG)));
        assertEquals(AvatarFileValidator.ImageType.WEBP, validator.validate(file("a.webp", WEBP)));
    }

    @Test
    void typeComesFromBytesNotFromFileName() {
        assertEquals(AvatarFileValidator.ImageType.PNG, validator.validate(file("photo.jpg", PNG)));
    }

    @Test
    void rejectsMissingOrEmptyFile() {
        assertEquals("Vui lòng chọn ảnh.", assertThrows(AvatarUploadException.class, () -> validator.validate(null)).getMessage());
        assertEquals("Vui lòng chọn ảnh.", assertThrows(AvatarUploadException.class,
                () -> validator.validate(file("a.jpg", new byte[0]))).getMessage());
    }

    @Test
    void rejectsFilesOverTwoMegabytes() {
        byte[] big = new byte[(int) AvatarFileValidator.MAX_SIZE_BYTES + 1];
        System.arraycopy(JPEG, 0, big, 0, JPEG.length);
        assertEquals("Ảnh tối đa 2 MB.", assertThrows(AvatarUploadException.class,
                () -> validator.validate(file("big.jpg", big))).getMessage());
    }

    @Test
    void acceptsExactlyTwoMegabytes() {
        byte[] edge = new byte[(int) AvatarFileValidator.MAX_SIZE_BYTES];
        System.arraycopy(PNG, 0, edge, 0, PNG.length);
        assertEquals(AvatarFileValidator.ImageType.PNG, validator.validate(file("edge.png", edge)));
    }

    @Test
    void rejectsSvgHeicGifAndRenamedText() {
        byte[] svg = "<svg xmlns=\"http://www.w3.org/2000/svg\"><script>alert(1)</script></svg>".getBytes(StandardCharsets.UTF_8);
        byte[] heic = {0, 0, 0, 0x18, 'f', 't', 'y', 'p', 'h', 'e', 'i', 'c'};
        byte[] gif = "GIF89a......".getBytes(StandardCharsets.US_ASCII);
        byte[] text = "hello world, not an image".getBytes(StandardCharsets.US_ASCII);
        byte[] riffWave = {'R', 'I', 'F', 'F', 0x24, 0, 0, 0, 'W', 'A', 'V', 'E'};
        for (byte[] content : new byte[][]{svg, heic, gif, text, riffWave}) {
            assertEquals("Chỉ nhận ảnh JPEG, PNG hoặc WebP.", assertThrows(AvatarUploadException.class,
                    () -> validator.validate(file("fake.jpg", content))).getMessage());
        }
    }

    @Test
    void imageTypeFromExtensionOnlyKnowsSystemExtensions() {
        assertEquals(AvatarFileValidator.ImageType.WEBP, AvatarFileValidator.ImageType.fromExtension("webp"));
        assertEquals(null, AvatarFileValidator.ImageType.fromExtension("svg"));
    }

    static MockMultipartFile file(String name, byte[] content) {
        return new MockMultipartFile("avatarFile", name, "application/octet-stream", content);
    }
}
