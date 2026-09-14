package com.lastcalleats.review.controller;

import com.lastcalleats.common.response.ApiResponse;
import com.lastcalleats.common.response.PageResult;
import com.lastcalleats.review.dto.CreatePostRequest;
import com.lastcalleats.review.dto.PostResponse;
import com.lastcalleats.review.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * REST endpoints for community post operations.
 * Supports creating posts, browsing by user or merchant, fetching a single
 * post, and owner-only deletion.
 */
@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @PostMapping
    public ApiResponse<PostResponse> createPost(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CreatePostRequest request) {
        return ApiResponse.success(postService.createPost(userId, request));
    }
    @GetMapping
    public ApiResponse<PageResult<PostResponse>> listAllPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<PostResponse> result = postService.listAllPosts(pageable);
        return ApiResponse.success(PageResult.of(result));
    }
    @GetMapping("/user/{userId}")
    public ApiResponse<PageResult<PostResponse>> listPostsByUser(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<PostResponse> result = postService.listPostsByUser(userId, pageable);
        return ApiResponse.success(PageResult.of(result));
    }
    @GetMapping("/merchant/{merchantId}")
    public ApiResponse<PageResult<PostResponse>> listPostsByMerchant(
            @PathVariable Long merchantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<PostResponse> result = postService.listPostsByMerchant(merchantId, pageable);
        return ApiResponse.success(PageResult.of(result));
    }
    @GetMapping("/{postId}")
    public ApiResponse<PostResponse> getPost(
            @PathVariable Long postId) {
        return ApiResponse.success(postService.getPost(postId));
    }
    @DeleteMapping("/{postId}")
    public ApiResponse<Void> deletePost(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long postId) {
        postService.deletePost(userId, postId);
        return ApiResponse.success();
    }
}
