package com.foody.common.storage;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import java.io.ByteArrayInputStream;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

class S3UploadStorageTest {
    @Test void customPublicBaseUrlBuildsStableUrlAndHidesCredentialsAndBucket() {
        S3Client client = mock(S3Client.class);
        when(client.putObject(any(software.amazon.awssdk.services.s3.model.PutObjectRequest.class),
                any(software.amazon.awssdk.core.sync.RequestBody.class))).thenReturn(PutObjectResponse.builder().build());
        var storage = new S3UploadStorage(client, "private-bucket", "https://media.foody.example/assets/");
        String key = "products/123e4567-e89b-12d3-a456-426614174000.webp";
        String url = storage.store(key, new ByteArrayInputStream(new byte[]{1}), 1, "image/webp");
        assertThat(url).isEqualTo("https://media.foody.example/assets/" + key)
                .doesNotContain("private-bucket", "access", "secret");
        assertThat(storage.objectKeyFromPublicUrl(url)).isEqualTo(key);
        assertThat(storage.objectKeyFromPublicUrl("https://attacker.example/" + key)).isNull();
    }

    @Test void publicBaseUrlRejectsCredentialsAndNonHttps() {
        assertThatThrownBy(() -> new S3UploadStorage(mock(S3Client.class), "bucket", "http://cdn.example.com"))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new S3UploadStorage(mock(S3Client.class), "bucket", "https://user:secret@cdn.example.com"))
                .isInstanceOf(IllegalStateException.class);
    }
}
