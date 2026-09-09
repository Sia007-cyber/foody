package com.foody.common.storage;

import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Product creation needs an upload before the product exists. Profile, cover, and existing
 * product replacements use entity-specific endpoints so database and cleanup ordering is safe.
 */
@RestController
@RequestMapping("/api/uploads")
public class UploadController {

    private final ImageUploadService imageUploadService;

    public UploadController(ImageUploadService imageUploadService) {
        this.imageUploadService = imageUploadService;
    }

    @PostMapping(value = "/product-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('BUSINESS_OWNER')")
    public UploadResponse uploadProductImage(@RequestParam("file") MultipartFile file) {
        var upload = imageUploadService.store(ImageUploadService.UploadCategory.PRODUCT, file);
        return new UploadResponse(upload.publicUrl());
    }
}
