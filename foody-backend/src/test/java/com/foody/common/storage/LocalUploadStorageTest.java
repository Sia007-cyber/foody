package com.foody.common.storage;

import static org.assertj.core.api.Assertions.*;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalUploadStorageTest {
    @TempDir Path root;
    @Test void storesAndDeletesOnlySafeGeneratedShape() throws Exception {
        var storage = new LocalUploadStorage(root.toString());
        String key = "profiles/123e4567-e89b-12d3-a456-426614174000.jpg";
        assertThat(storage.store(key, new ByteArrayInputStream(new byte[]{1}), 1, "image/jpeg"))
                .isEqualTo("/uploads/" + key);
        assertThat(Files.exists(root.resolve(key))).isTrue();
        assertThat(storage.objectKeyFromPublicUrl("https://elsewhere.example/x.jpg")).isNull();
        assertThatThrownBy(() -> storage.store("../../secret.jpg", new ByteArrayInputStream(new byte[]{1}), 1, "image/jpeg"))
                .isInstanceOf(IllegalArgumentException.class);
        storage.delete(key);
        assertThat(Files.exists(root.resolve(key))).isFalse();
    }
}
