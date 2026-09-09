package com.foody.common.storage;

import java.io.InputStream;

public interface UploadStorage {
    String store(String objectKey, InputStream content, long contentLength, String contentType);
    void delete(String objectKey);
    String objectKeyFromPublicUrl(String publicUrl);
}
