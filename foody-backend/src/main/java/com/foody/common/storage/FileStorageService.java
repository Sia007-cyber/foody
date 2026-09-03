package com.foody.common.storage;

import com.foody.common.exception.InvalidRequestException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Local-disk image storage (Phase 2 groundwork — no S3/cloud storage yet, deliberately
 * simple for a single-server deployment). Backs both the user profile picture and, later,
 * the business cover-image upload item from the Phase 2 checklist.
 *
 * Files are saved under {@code foody.storage.upload-dir} and served back publicly via
 * {@code /uploads/**} (see {@link StaticResourceConfig} and the public matcher in
 * WebSecurityConfig) since an <img src> can't attach an Authorization header.
 */
@Service
public class FileStorageService {

    private static final Map<String, String> ALLOWED_CONTENT_TYPES = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp");
    private static final Set<String> ALLOWED = ALLOWED_CONTENT_TYPES.keySet();
    private static final long MAX_SIZE_BYTES = 5L * 1024 * 1024; // 5MB

    private final Path root;

    public FileStorageService(@Value("${foody.storage.upload-dir}") String uploadDir) {
        this.root = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new IllegalStateException("Could not initialize upload directory: " + root, e);
        }
    }

    /** Validates, stores the file, and returns its public URL path (e.g. {@code /uploads/xxx.jpg}). */
    public String storeImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidRequestException("فایلی ارسال نشده");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED.contains(contentType)) {
            throw new InvalidRequestException("فقط تصاویر jpg، png یا webp مجاز هستن");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new InvalidRequestException("حجم فایل نباید بیشتر از ۵ مگابایت باشه");
        }

        String filename = UUID.randomUUID() + ALLOWED_CONTENT_TYPES.get(contentType);
        Path target = root.resolve(filename).normalize();
        if (!target.getParent().equals(root)) {
            // Defensive: filename is a fresh UUID so this can't actually happen, but guard
            // against path traversal categorically rather than trusting the input shape.
            throw new InvalidRequestException("نام فایل نامعتبر است");
        }
        try {
            file.transferTo(target);
        } catch (IOException e) {
            throw new IllegalStateException("آپلود فایل با خطا مواجه شد", e);
        }
        return "/uploads/" + filename;
    }
}
