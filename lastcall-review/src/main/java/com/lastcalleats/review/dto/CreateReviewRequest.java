package com.lastcalleats.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * Request body for submitting a review against a completed order.
 * The rating (1–5) is required; text content and images are both optional.
 */
@Data
public class CreateReviewRequest {

    @NotNull(message = "Order ID is required")
    private Long orderId;

    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must be at most 5")
    private Integer rating;

    @Size(max = 500, message = "Review content cannot exceed 500 characters")
    private String content;

    /** Image URLs uploaded to MinIO beforehand; max 9. */
    @Size(max = 9, message = "Cannot attach more than 9 images")
    private List<String> imageUrls;
}
