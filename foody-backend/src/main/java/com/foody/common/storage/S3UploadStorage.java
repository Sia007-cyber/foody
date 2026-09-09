package com.foody.common.storage;

import com.foody.common.exception.StorageOperationException;
import java.io.InputStream;
import java.net.URI;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public class S3UploadStorage implements UploadStorage {
    private final S3Client client;
    private final String bucket;
    private final String publicBaseUrl;

    public S3UploadStorage(S3Client client, String bucket, String publicBaseUrl) {
        this.client = client;
        this.bucket = bucket;
        this.publicBaseUrl = normalizePublicBaseUrl(publicBaseUrl);
    }

    @Override
    public String store(String objectKey, InputStream content, long contentLength, String contentType) {
        try {
            client.putObject(PutObjectRequest.builder().bucket(bucket).key(objectKey)
                    .contentType(contentType).build(), RequestBody.fromInputStream(content, contentLength));
            return publicBaseUrl + "/" + objectKey;
        } catch (RuntimeException ex) {
            throw new StorageOperationException("Object storage upload failed", ex);
        }
    }

    @Override
    public void delete(String objectKey) {
        try { client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(objectKey).build()); }
        catch (RuntimeException ex) { throw new StorageOperationException("Object storage cleanup failed", ex); }
    }

    @Override
    public String objectKeyFromPublicUrl(String publicUrl) {
        String prefix = publicBaseUrl + "/";
        if (publicUrl == null || !publicUrl.startsWith(prefix)) return null;
        String key = publicUrl.substring(prefix.length());
        return key.matches("[a-z-]+/[0-9a-f-]+\\.(jpg|png|webp)") ? key : null;
    }

    static String normalizePublicBaseUrl(String value) {
        URI uri;
        try { uri = URI.create(require("FOODY_STORAGE_PUBLIC_BASE_URL", value)); }
        catch (IllegalArgumentException ex) { throw new IllegalStateException("FOODY_STORAGE_PUBLIC_BASE_URL must be a valid HTTPS origin or URL prefix", ex); }
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null
                || uri.getQuery() != null || uri.getFragment() != null) {
            throw new IllegalStateException("FOODY_STORAGE_PUBLIC_BASE_URL must be HTTPS and must not contain credentials, query, or fragment");
        }
        String normalized = value.trim();
        while (normalized.endsWith("/")) normalized = normalized.substring(0, normalized.length() - 1);
        if (normalized.length() > 430) {
            throw new IllegalStateException("FOODY_STORAGE_PUBLIC_BASE_URL is too long for persisted image URLs");
        }
        return normalized;
    }

    static String require(String name, String value) {
        if (value == null || value.isBlank()) throw new IllegalStateException(name + " is required in production");
        return value.trim();
    }
}
