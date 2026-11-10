package com.F_A_T.F_A_T.profile;

import com.F_A_T.F_A_T.domain.mypage.storage.AvatarStorage;
import com.F_A_T.F_A_T.global.exception.ApiException;
import com.F_A_T.F_A_T.global.exception.ErrorCode;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.CRC32;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import static org.junit.jupiter.api.Assertions.*;

class AvatarStorageTests {
    @TempDir Path directory;
    byte[] png() throws Exception {
        var bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(32, 16, BufferedImage.TYPE_INT_RGB), "png", bytes);
        return bytes.toByteArray();
    }
    @Test void savesNormalizedImageWithGeneratedFilenameAndReadableUrl() throws Exception {
        var storage = new AvatarStorage(directory.toString());
        String url = storage.save(new MockMultipartFile("file", "../../untrusted.svg", "image/png", png()));
        assertTrue(url.matches("/avatars/[a-f0-9-]{36}\\.png"));
        String filename = url.substring(url.lastIndexOf('/') + 1);
        assertEquals(32, ImageIO.read(storage.read(filename).getFile()).getWidth());
        assertEquals(16, ImageIO.read(storage.read(filename).getFile()).getHeight());
        try (var files = Files.list(directory)) { assertEquals(1, files.count()); }
    }
    @Test void rejectsFakeImageUnsupportedTypeAndTraversal() {
        var storage = new AvatarStorage(directory.toString());
        assertEquals(ErrorCode.INVALID_IMAGE_TYPE, assertThrows(ApiException.class, () -> storage.save(
                new MockMultipartFile("file", "fake.png", "image/png", "<svg>script</svg>".getBytes()))).getErrorCode());
        assertEquals(ErrorCode.INVALID_IMAGE_TYPE, assertThrows(ApiException.class, () -> storage.save(
                new MockMultipartFile("file", "fake.svg", "image/svg+xml", "<svg/>".getBytes()))).getErrorCode());
        assertThrows(ApiException.class, () -> storage.read("../../etc/passwd"));
    }
    @Test void rejectsOversizedFilesAndDecompressionBombDimensionsBeforeDecode() throws Exception {
        var storage = new AvatarStorage(directory.toString());
        assertEquals(ErrorCode.IMAGE_TOO_LARGE, assertThrows(ApiException.class, () -> storage.save(
                new MockMultipartFile("file", "big.png", "image/png", new byte[5 * 1024 * 1024 + 1]))).getErrorCode());
        byte[] encoded = png();
        ByteBuffer.wrap(encoded, 16, 8).putInt(5000).putInt(5000);
        CRC32 crc = new CRC32(); crc.update(encoded, 12, 17);
        ByteBuffer.wrap(encoded, 29, 4).putInt((int) crc.getValue());
        assertEquals(ErrorCode.IMAGE_TOO_LARGE, assertThrows(ApiException.class, () -> storage.save(
                new MockMultipartFile("file", "bomb.png", "image/png", encoded))).getErrorCode());
    }
    @Test void rejectsMismatchedMimeAndNullMime() throws Exception {
        var storage = new AvatarStorage(directory.toString());
        assertEquals(ErrorCode.INVALID_IMAGE_TYPE, assertThrows(ApiException.class, () -> storage.save(
                new MockMultipartFile("file", "image.jpg", "image/jpeg", png()))).getErrorCode());
        assertEquals(ErrorCode.INVALID_IMAGE_TYPE, assertThrows(ApiException.class, () -> storage.save(
                new MockMultipartFile("file", "image.png", null, png()))).getErrorCode());
    }
}
