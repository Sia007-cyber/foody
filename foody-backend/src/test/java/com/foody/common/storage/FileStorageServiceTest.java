package com.foody.common.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.foody.common.exception.InvalidRequestException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class FileStorageServiceTest {

    @TempDir
    Path tempDir;

    private FileStorageService service() {
        return new FileStorageService(tempDir.toString());
    }

    @Test
    void storeImage_savesJpegAndReturnsPublicUrl() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[]{1, 2, 3});

        String url = service().storeImage(file);

        assertThat(url).startsWith("/uploads/").endsWith(".jpg");
        Path saved = tempDir.resolve(url.substring("/uploads/".length()));
        assertThat(Files.exists(saved)).isTrue();
    }

    @Test
    void storeImage_rejectsEmptyFile() {
        MockMultipartFile empty = new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[0]);

        assertThatThrownBy(() -> service().storeImage(empty))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void storeImage_rejectsDisallowedContentType() {
        MockMultipartFile pdf = new MockMultipartFile("file", "doc.pdf", "application/pdf", new byte[]{1, 2, 3});

        assertThatThrownBy(() -> service().storeImage(pdf))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void storeImage_rejectsOversizedFile() {
        byte[] tooBig = new byte[6 * 1024 * 1024];
        MockMultipartFile huge = new MockMultipartFile("file", "big.png", "image/png", tooBig);

        assertThatThrownBy(() -> service().storeImage(huge))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void storeImage_generatesUniqueNamesForRepeatedUploads() {
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", new byte[]{9, 9});

        String first = service().storeImage(file);
        String second = service().storeImage(file);

        assertThat(first).isNotEqualTo(second);
    }
}
