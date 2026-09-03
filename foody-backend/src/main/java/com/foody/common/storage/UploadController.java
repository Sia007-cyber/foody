package com.foody.common.storage;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Generic authenticated image upload, decoupled from any single feature so the profile
 * picture and (later) the business cover-image checklist item can both use it. Requires
 * login (not in WebSecurityConfig's PUBLIC_MATCHERS) — only the resulting /uploads/** URL
 * is public, not the upload action itself.
 */
@RestController
@RequestMapping("/api/uploads")
public class UploadController {

    private final FileStorageService fileStorageService;

    public UploadController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UploadResponse uploadImage(@RequestParam("file") MultipartFile file) {
        String url = fileStorageService.storeImage(file);
        return new UploadResponse(url);
    }
}
