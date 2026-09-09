package com.foody.common.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.foody.common.exception.GlobalExceptionHandler;
import com.foody.common.exception.InvalidRequestException;
import com.foody.common.exception.StorageOperationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class UploadControllerTest {

    @Mock ImageUploadService imageUploadService;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new UploadController(imageUploadService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void uploadImage_returnsStoredUrl() throws Exception {
        when(imageUploadService.store(any(), any())).thenReturn(
                new ImageUploadService.StoredUpload("products/generated-name.jpg", "/uploads/products/generated-name.jpg"));
        MockMultipartFile file = new MockMultipartFile("file", "avatar.jpg", "image/jpeg", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/uploads/product-image").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("/uploads/products/generated-name.jpg"));
    }

    @Test
    void uploadImage_propagatesValidationErrorAsBadRequest() throws Exception {
        when(imageUploadService.store(any(), any())).thenThrow(new InvalidRequestException("فقط تصاویر مجاز هستن"));
        MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", new byte[]{1});

        mockMvc.perform(multipart("/api/uploads/product-image").file(file))
                .andExpect(status().isBadRequest());
    }

    @Test
    void storageFailureReturnsControlledResponseWithoutCredentials() throws Exception {
        when(imageUploadService.store(any(), any())).thenThrow(
                new StorageOperationException("Object storage upload failed",
                        new RuntimeException("access-key=should-never-appear secret-key=hidden")));
        MockMultipartFile file = new MockMultipartFile("file", "avatar.jpg", "image/jpeg", new byte[]{1});

        mockMvc.perform(multipart("/api/uploads/product-image").file(file))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("STORAGE_UNAVAILABLE"))
                .andExpect(jsonPath("$.message").value("Object storage upload failed"))
                .andExpect(result -> assertThat(result.getResponse().getContentAsString())
                        .doesNotContain("should-never-appear", "secret-key", "hidden"));
    }
}
