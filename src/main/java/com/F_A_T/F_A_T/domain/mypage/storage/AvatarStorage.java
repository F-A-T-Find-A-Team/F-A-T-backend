package com.F_A_T.F_A_T.domain.mypage.storage;

import com.F_A_T.F_A_T.global.exception.ApiException;
import com.F_A_T.F_A_T.global.exception.ErrorCode;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AvatarStorage {
    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final long MAX_PIXELS = 20_000_000;
    private final Path directory;

    public AvatarStorage(@Value("${app.avatar.storage-path:/workspace/shared/fat-avatars}") String directory) {
        this.directory = Path.of(directory).toAbsolutePath().normalize();
    }

    public String save(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new ApiException(ErrorCode.INVALID_IMAGE_TYPE);
        if (file.getSize() > MAX_BYTES) throw new ApiException(ErrorCode.IMAGE_TOO_LARGE);
        if (file.getContentType() == null || !Set.of("image/png", "image/jpeg").contains(file.getContentType())) {
            throw new ApiException(ErrorCode.INVALID_IMAGE_TYPE);
        }
        try {
            // Inspect dimensions before decoding; a tiny compressed image can expand enormously.
            byte[] bytes = file.getBytes();
            if (bytes.length > MAX_BYTES) throw new ApiException(ErrorCode.IMAGE_TOO_LARGE);
            try (var stream = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
                Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
                if (!readers.hasNext()) throw new ApiException(ErrorCode.INVALID_IMAGE_TYPE);
                ImageReader reader = readers.next();
                try {
                    reader.setInput(stream, true, true);
                    String format = reader.getFormatName();
                    if (!(format.equalsIgnoreCase("png") || format.equalsIgnoreCase("jpeg") || format.equalsIgnoreCase("jpg"))) {
                        throw new ApiException(ErrorCode.INVALID_IMAGE_TYPE);
                    }
                    if ((format.equalsIgnoreCase("png") && !"image/png".equals(file.getContentType())) ||
                            (!format.equalsIgnoreCase("png") && !"image/jpeg".equals(file.getContentType()))) {
                        throw new ApiException(ErrorCode.INVALID_IMAGE_TYPE);
                    }
                    int width = reader.getWidth(0), height = reader.getHeight(0);
                    if (width < 1 || height < 1 || (long) width * height > MAX_PIXELS) {
                        throw new ApiException(ErrorCode.IMAGE_TOO_LARGE);
                    }
                    BufferedImage original = reader.read(0);
                    double scale = Math.min(1.0, 1024.0 / Math.max(width, height));
                    int scaledWidth = Math.max(1, (int) (width * scale));
                    int scaledHeight = Math.max(1, (int) (height * scale));
                    BufferedImage normalized = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D graphics = normalized.createGraphics();
                    try {
                        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                        graphics.drawImage(original, 0, 0, scaledWidth, scaledHeight, null);
                    } finally { graphics.dispose(); }
                    // Re-encode instead of storing uploaded bytes, names, or metadata.
                    String filename = UUID.randomUUID() + ".png";
                    try {
                        Files.createDirectories(directory);
                        Path target = directory.resolve(filename);
                        try (var output = Files.newOutputStream(target, java.nio.file.StandardOpenOption.CREATE_NEW, java.nio.file.StandardOpenOption.WRITE)) {
                            if (!ImageIO.write(normalized, "png", output)) throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR);
                        }
                    } catch (IOException ex) { throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR); }
                    return "/avatars/" + filename;
                } finally { reader.dispose(); }
            }
        } catch (ApiException e) { throw e; }
        catch (IOException e) { throw new ApiException(ErrorCode.INVALID_IMAGE_TYPE); }
    }

    public Resource read(String filename) {
        if (filename == null || !filename.matches("[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}\\.png")) {
            throw new ApiException(ErrorCode.USER_NOT_FOUND);
        }
        Path path = directory.resolve(filename).normalize();
        if (!path.startsWith(directory) || !Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
            throw new ApiException(ErrorCode.USER_NOT_FOUND);
        }
        return new FileSystemResource(path);
    }
}
