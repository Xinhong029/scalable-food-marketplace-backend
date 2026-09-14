package com.lastcalleats.review.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Read-only view of a post returned to the client.
 * Includes resolved display names for the author and merchant so the
 * frontend does not need a separate lookup on each list item.
 */
@Getter
@Builder
public class PostResponse {

    private Long id;
    private Long userId;
    private String userNickname;
    private String userAvatarUrl;
    private Long merchantId;
    private String merchantName;
    private String content;
    private List<String> imageUrls;
    private Integer likeCount;
    private Integer commentCount;
    private LocalDateTime createdAt;
}
