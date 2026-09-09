package com.foody.common.storage;

import static org.assertj.core.api.Assertions.*;
import org.junit.jupiter.api.Test;

class S3StorageConfigurationTest {
    private final S3StorageConfiguration configuration = new S3StorageConfiguration();

    @Test void acceptsCustomS3EndpointAndPublicBaseUrlWithoutLiveCredentials() {
        UploadStorage storage = configuration.uploadStorage("foody-media", "auto",
                "https://account.example.r2.cloudflarestorage.com", "test-access", "test-secret",
                "https://media.foody.example");
        assertThat(storage).isInstanceOf(S3UploadStorage.class);
    }

    @Test void missingProductionConfigurationCannotFallBackToLocalDisk() {
        assertThatThrownBy(() -> configuration.uploadStorage("", "", "", "", "", ""))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("FOODY_STORAGE_REGION");
    }

    @Test void endpointRejectsCredentialsAndPlainHttp() {
        assertThatThrownBy(() -> configuration.uploadStorage("bucket", "auto", "http://storage.example.com",
                "access", "secret", "https://media.example.com")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> configuration.uploadStorage("bucket", "auto", "https://user:secret@storage.example.com",
                "access", "secret", "https://media.example.com")).isInstanceOf(IllegalStateException.class);
    }
}
