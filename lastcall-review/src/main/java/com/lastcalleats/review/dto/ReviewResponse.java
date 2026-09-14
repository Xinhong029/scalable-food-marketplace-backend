package com.lastcalleats.review.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Read-only view of a review returned to the client.
 * Internal fields such as {@code isVisible} are excluded; user display
 * names are left null and fetched by the frontend via the user API.
 */
@Getter
@Builder
public class ReviewResponse {

    private Long id;
    private Long orderId;
    private Long userId;
    private String userNickname;
    private String userAvatarUrl;
    private Long merchantId;
    private Long templateId;
    private Integer rating;
    private String content;
    private List<String> imageUrls;
    private LocalDateTime createdAt;
}
