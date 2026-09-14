package com.lastcalleats.review.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * Request body for creating a community post.
 * The merchant reference is optional; a post without one appears in the
 * general feed but not on any merchant's profile page.
 */
@Data
public class CreatePostRequest {

    @NotBlank(message = "Post content cannot be blank")
    @Size(max = 1000, message = "Post content cannot exceed 1000 characters")
    private String content;

    private Long merchantId; // optional

    /** Image URLs uploaded to MinIO beforehand; max 9. */
    @Size(max = 9, message = "Cannot attach more than 9 images")
    private List<String> imageUrls;
}
