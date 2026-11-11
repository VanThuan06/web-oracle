package vn.newmen.store.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LocalImageStorage {
    private final Path root;
    private final long maxBytes;

    public LocalImageStorage(@Value("${app.uploads.storage-dir}") String directory,
                             @Value("${app.uploads.max-bytes:5242880}") long maxBytes) {
        this.root = Path.of(directory).toAbsolutePath().normalize();
        this.maxBytes = maxBytes;
    }

    public StoredImage store(MultipartFile upload) {
        if (upload == null || upload.isEmpty()) throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Ảnh tải lên đang trống");
        if (upload.getSize() > maxBytes) throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Ảnh vượt quá giới hạn 5 MiB");
        try {
            byte[] bytes = upload.getBytes();
            String actualType = detectedType(bytes);
            String declaredType = upload.getContentType() == null ? "" : upload.getContentType().toLowerCase(Locale.ROOT);
            if (actualType == null || (!declaredType.isBlank() && !"application/octet-stream".equals(declaredType)
                && !actualType.equals(declaredType))) {
                throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Chỉ nhận ảnh JPEG, PNG hoặc WebP hợp lệ");
            }
            String extension = switch (actualType) {
                case "image/jpeg" -> ".jpg";
                case "image/png" -> ".png";
                case "image/webp" -> ".webp";
                default -> throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Định dạng ảnh chưa hỗ trợ");
            };
            Files.createDirectories(root);
            String fileName = UUID.randomUUID() + extension;
            Path destination = root.resolve(fileName).normalize();
            if (!destination.startsWith(root) || destination.equals(root)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên tệp không hợp lệ");
            }
            Files.write(destination, bytes, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            return new StoredImage(fileName, "/api/v1/media/" + fileName);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể lưu ảnh lên máy chủ");
        }
    }

    public Path resolve(String fileName) {
        if (fileName == null || !fileName.matches("[a-f0-9-]{36}\\.(jpg|png|webp)")) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy ảnh");
        }
        Path path = root.resolve(fileName).normalize();
        if (!path.startsWith(root) || path.equals(root) || !Files.isRegularFile(path)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy ảnh");
        }
        return path;
    }

    public void delete(String fileName) {
        if (fileName == null || !fileName.matches("[a-f0-9-]{36}\\.(jpg|png|webp)")) return;
        Path path = root.resolve(fileName).normalize();
        if (path.startsWith(root) && !path.equals(root)) {
            try { Files.deleteIfExists(path); }
            catch (IOException exception) { throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể xóa ảnh"); }
        }
    }

    private String detectedType(byte[] bytes) {
        if (bytes.length >= 3 && (bytes[0] & 0xff) == 0xff && (bytes[1] & 0xff) == 0xd8 && (bytes[2] & 0xff) == 0xff) return "image/jpeg";
        if (bytes.length >= 8 && bytes[0] == (byte) 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4e && bytes[3] == 0x47
            && bytes[4] == 0x0d && bytes[5] == 0x0a && bytes[6] == 0x1a && bytes[7] == 0x0a) return "image/png";
        if (bytes.length >= 12 && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
            && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') return "image/webp";
        return null;
    }

    public record StoredImage(String fileName, String url) { }
}
