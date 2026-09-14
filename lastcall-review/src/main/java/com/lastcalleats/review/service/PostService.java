package com.lastcalleats.review.service;

import com.lastcalleats.review.dto.CreatePostRequest;
import com.lastcalleats.review.dto.PostResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service contract for community post operations.
 * Posts are optionally attached to a merchant and support likes and comments via separate services.
 */
public interface PostService {

    /**
     * @param userId  the authenticated author
     * @param request post content and optional merchant reference
     * @return the persisted post with resolved nickname and merchant name
     */
    PostResponse createPost(Long userId, CreatePostRequest request);

    /**
     * @param pageable pagination parameters
     * @return all visible posts, newest first
     */
    Page<PostResponse> listAllPosts(Pageable pageable);

    /**
     * @param userId   target user's ID
     * @param pageable pagination parameters
     * @return visible posts by that user, newest first
     */
    Page<PostResponse> listPostsByUser(Long userId, Pageable pageable);

    /**
     * @param merchantId the merchant's ID
     * @param pageable   pagination parameters
     * @return visible posts tagged to that merchant, newest first
     */
    Page<PostResponse> listPostsByMerchant(Long merchantId, Pageable pageable);

    /**
     * @param postId the post's primary key
     * @return the matching post response
     * @throws com.lastcalleats.common.exception.BusinessException with {@code POST_NOT_FOUND} if missing or hidden
     */
    PostResponse getPost(Long postId);

    /**
     * @param userId the authenticated requester; must be the post owner
     * @param postId the post to remove
     * @throws com.lastcalleats.common.exception.BusinessException with {@code POST_FORBIDDEN} if not the owner
     */
    void deletePost(Long userId, Long postId);
}
