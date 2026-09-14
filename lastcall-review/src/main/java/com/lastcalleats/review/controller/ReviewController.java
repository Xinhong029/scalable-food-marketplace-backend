package com.lastcalleats.review.controller;

import com.lastcalleats.common.response.ApiResponse;
import com.lastcalleats.common.response.PageResult;
import com.lastcalleats.review.dto.CreateReviewRequest;
import com.lastcalleats.review.dto.ReviewResponse;
import com.lastcalleats.review.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * REST endpoints for review operations.
 * Reviews are linked one-to-one with completed orders; the endpoints here
 * cover creation and read-only queries by order, merchant, or product template.
 */
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public ApiResponse<ReviewResponse> createReview(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CreateReviewRequest request) {
        return ApiResponse.success(reviewService.createReview(userId, request));
    }

    @GetMapping("/order/{orderId}")
    public ApiResponse<ReviewResponse> getReviewByOrder(
            @PathVariable Long orderId) {
        Optional<ReviewResponse> review = reviewService.getReviewByOrder(orderId);
        return review.map(ApiResponse::success)
                .orElse(ApiResponse.success(null));
    }

    @GetMapping("/merchant/{merchantId}")
    public ApiResponse<PageResult<ReviewResponse>> listReviewsByMerchant(
            @PathVariable Long merchantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.success(PageResult.of(reviewService.listReviewsByMerchant(merchantId, pageable)));
    }

    @GetMapping("/template/{templateId}")
    public ApiResponse<PageResult<ReviewResponse>> listReviewsByTemplate(
            @PathVariable Long templateId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.success(PageResult.of(reviewService.listReviewsByTemplate(templateId, pageable)));
    }
}
