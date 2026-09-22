package com.foody.reviews.controller;

import com.foody.reviews.dto.AdminReviewResponse;
import com.foody.reviews.entity.ReviewModerationStatus;
import com.foody.reviews.service.ReviewModerationService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/reviews")
@PreAuthorize("hasRole('ADMIN')")
public class AdminReviewController {
    private final ReviewModerationService moderation;
    public AdminReviewController(ReviewModerationService moderation) { this.moderation = moderation; }

    @GetMapping
    public List<AdminReviewResponse> list(@RequestParam(defaultValue="PENDING") ReviewModerationStatus status) { return moderation.list(status); }

    @PatchMapping("/{type}/{id}/approve")
    public AdminReviewResponse approve(@PathVariable String type,@PathVariable Long id) { return moderation.moderate(type.toUpperCase(),id,ReviewModerationStatus.APPROVED); }

    @PatchMapping("/{type}/{id}/reject")
    public AdminReviewResponse reject(@PathVariable String type,@PathVariable Long id) { return moderation.moderate(type.toUpperCase(),id,ReviewModerationStatus.REJECTED); }
}
