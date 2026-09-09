package com.foody.common.storage;

import com.foody.common.exception.InvalidRequestException;
import com.foody.common.exception.StorageOperationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImageUploadService {
    static final long MAX_SIZE_BYTES = 5L * 1024 * 1024;
    private static final Map<String, FileType> TYPES = Map.of(
            "image/jpeg", new FileType(".jpg", new byte[]{(byte) 0xff, (byte) 0xd8, (byte) 0xff}),
            "image/png", new FileType(".png", new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a}),
            "image/webp", new FileType(".webp", new byte[]{0x52, 0x49, 0x46, 0x46}));
    private final UploadStorage storage;

    public ImageUploadService(UploadStorage storage) { this.storage = storage; }

    public StoredUpload store(UploadCategory category, MultipartFile file) {
        FileType type = validate(file);
        String key = category.prefix + "/" + UUID.randomUUID() + type.extension;
        try (InputStream input = file.getInputStream()) {
            return new StoredUpload(key, storage.store(key, input, file.getSize(), file.getContentType()));
        } catch (IOException ex) {
            throw new StorageOperationException("Could not read the uploaded image", ex);
        }
    }

    public void deleteManagedUrl(String url) {
        String key = storage.objectKeyFromPublicUrl(url);
        if (key != null) storage.delete(key);
    }

    private FileType validate(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new InvalidRequestException("فایلی ارسال نشده");
        if (file.getSize() > MAX_SIZE_BYTES) throw new InvalidRequestException("حجم فایل نباید بیشتر از ۵ مگابایت باشه");
        FileType type = TYPES.get(file.getContentType());
        if (type == null) throw new InvalidRequestException("فقط تصاویر jpg، png یا webp مجاز هستن");
        try (InputStream input = file.getInputStream()) {
            byte[] header = input.readNBytes(12);
            boolean valid = startsWith(header, type.magic);
            if ("image/webp".equals(file.getContentType())) valid = valid && header.length >= 12
                    && header[8] == 0x57 && header[9] == 0x45 && header[10] == 0x42 && header[11] == 0x50;
            if (!valid) throw new InvalidRequestException("محتوای فایل با نوع تصویر اعلام‌شده مطابقت ندارد");
            return type;
        } catch (IOException ex) {
            throw new InvalidRequestException("فایل تصویر قابل خواندن نیست");
        }
    }

    private boolean startsWith(byte[] value, byte[] prefix) {
        if (value.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) if (value[i] != prefix[i]) return false;
        return true;
    }

    public enum UploadCategory {
        PROFILE("profiles"), BUSINESS_COVER("business-covers"), PRODUCT("products");
        private final String prefix;
        UploadCategory(String prefix) { this.prefix = prefix; }
    }
    public record StoredUpload(String objectKey, String publicUrl) {}
    private record FileType(String extension, byte[] magic) {}
}
