package com.foody.common.storage;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.foody.common.exception.InvalidRequestException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class ImageUploadServiceTest {
    UploadStorage storage = mock(UploadStorage.class);
    ImageUploadService service = new ImageUploadService(storage);

    @Test void validUploadUsesSafeUniqueGeneratedKeysAndIgnoresUnsafeFilename() {
        when(storage.store(anyString(), any(InputStream.class), anyLong(), eq("image/jpeg")))
                .thenAnswer(inv -> "https://cdn.example.com/" + inv.getArgument(0));
        var file = new MockMultipartFile("file", "../../credentials.png", "image/jpeg",
                new byte[]{(byte) 0xff, (byte) 0xd8, (byte) 0xff, 1});
        Set<String> keys = new HashSet<>();
        keys.add(service.store(ImageUploadService.UploadCategory.PROFILE, file).objectKey());
        keys.add(service.store(ImageUploadService.UploadCategory.PROFILE, file).objectKey());
        assertThat(keys).hasSize(2).allMatch(k -> k.matches("profiles/[0-9a-f-]+\\.jpg"));
        assertThat(keys).allMatch(k -> !k.contains("credentials") && !k.contains(".."));
        verify(storage, times(2)).store(anyString(), any(InputStream.class), eq(4L), eq("image/jpeg"));
    }

    @Test void rejectsEmptyInvalidTypeOversizeAndMismatchedContent() {
        assertThatThrownBy(() -> service.store(ImageUploadService.UploadCategory.PROFILE,
                new MockMultipartFile("file", "x.jpg", "image/jpeg", new byte[0])))
                .isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> service.store(ImageUploadService.UploadCategory.PROFILE,
                new MockMultipartFile("file", "x.pdf", "application/pdf", new byte[]{1})))
                .isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> service.store(ImageUploadService.UploadCategory.PROFILE,
                new MockMultipartFile("file", "x.png", "image/png", new byte[6 * 1024 * 1024])))
                .isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> service.store(ImageUploadService.UploadCategory.PROFILE,
                new MockMultipartFile("file", "x.jpg", "image/jpeg", new byte[]{1, 2, 3})))
                .isInstanceOf(InvalidRequestException.class);
        verifyNoInteractions(storage);
    }
}
