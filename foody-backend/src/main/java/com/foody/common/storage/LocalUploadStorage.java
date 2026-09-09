package com.foody.common.storage;

import com.foody.common.exception.StorageOperationException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"local", "tc"})
public class LocalUploadStorage implements UploadStorage {
    private static final String PUBLIC_PREFIX = "/uploads/";
    private final Path root;

    public LocalUploadStorage(@Value("${foody.storage.upload-dir}") String uploadDir) {
        root = Path.of(uploadDir).toAbsolutePath().normalize();
        try { Files.createDirectories(root); }
        catch (IOException ex) { throw new IllegalStateException("Could not initialize local upload storage", ex); }
    }

    @Override
    public String store(String objectKey, InputStream content, long contentLength, String contentType) {
        Path target = target(objectKey);
        try {
            Files.createDirectories(target.getParent());
            Files.copy(content, target);
            return PUBLIC_PREFIX + objectKey;
        } catch (IOException ex) {
            throw new StorageOperationException("Could not store the uploaded image", ex);
        }
    }

    @Override
    public void delete(String objectKey) {
        try { Files.deleteIfExists(target(objectKey)); }
        catch (IOException ex) { throw new StorageOperationException("Could not remove the obsolete image", ex); }
    }

    @Override
    public String objectKeyFromPublicUrl(String publicUrl) {
        return publicUrl != null && publicUrl.startsWith(PUBLIC_PREFIX)
                ? safeKey(publicUrl.substring(PUBLIC_PREFIX.length())) : null;
    }

    private Path target(String objectKey) {
        String safe = safeKey(objectKey);
        if (safe == null) throw new IllegalArgumentException("Unsafe upload object key");
        Path target = root.resolve(safe).normalize();
        if (!target.startsWith(root)) throw new IllegalArgumentException("Unsafe upload object key");
        return target;
    }

    private String safeKey(String key) {
        return key != null && key.matches("[a-z-]+/[0-9a-f-]+\\.(jpg|png|webp)") ? key : null;
    }
}
