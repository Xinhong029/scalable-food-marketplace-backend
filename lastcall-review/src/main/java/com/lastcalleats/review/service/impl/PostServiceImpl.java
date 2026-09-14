package com.lastcalleats.review.service.impl;

import com.lastcalleats.common.exception.BusinessException;
import com.lastcalleats.common.exception.ErrorCode;
import com.lastcalleats.common.util.Assert;
import com.lastcalleats.merchant.repository.MerchantRepo;
import com.lastcalleats.review.dto.CreatePostRequest;
import com.lastcalleats.review.dto.PostResponse;
import com.lastcalleats.review.entity.PostDO;
import com.lastcalleats.review.repository.PostRepo;
import com.lastcalleats.review.service.PostService;
import com.lastcalleats.user.repository.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default implementation of {@link PostService}. Resolves author nickname and merchant name
 * at query time so the post table stays free of denormalized name fields.
 */
@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostRepo postRepo;
    private final UserRepo userRepo;
    private final MerchantRepo merchantRepo;

    /** {@inheritDoc} */
    @Override
    @Transactional
    public PostResponse createPost(Long userId, CreatePostRequest request) {
        PostDO post = PostDO.builder()
                .userId(userId)
                .merchantId(request.getMerchantId())
                .content(request.getContent())
                .imageUrls(request.getImageUrls())
                .build();
        postRepo.save(post);
        return toResponse(post);
    }

    /** {@inheritDoc} */
    @Override
    public Page<PostResponse> listAllPosts(Pageable pageable) {
        return postRepo.findByIsVisibleTrueOrderByCreatedAtDesc(pageable)
                .map(this::toResponse);
    }

    /** {@inheritDoc} */
    @Override
    public Page<PostResponse> listPostsByUser(Long userId, Pageable pageable) {
        return postRepo.findByUserIdAndIsVisibleTrueOrderByCreatedAtDesc(userId, pageable)
                .map(this::toResponse);
    }

    /** {@inheritDoc} */
    @Override
    public Page<PostResponse> listPostsByMerchant(Long merchantId, Pageable pageable) {
        return postRepo.findByMerchantIdAndIsVisibleTrueOrderByCreatedAtDesc(merchantId, pageable)
                .map(this::toResponse);
    }

    /** {@inheritDoc} */
    @Override
    public PostResponse getPost(Long postId) {
        return toResponse(findPostById(postId));
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void deletePost(Long userId, Long postId) {
        PostDO post = findPostById(postId);
        Assert.equals(post.getUserId(), userId, ErrorCode.POST_FORBIDDEN);
        postRepo.delete(post);
    }

    /** Throws POST_NOT_FOUND if the post does not exist or is hidden. */
    private PostDO findPostById(Long postId) {
        PostDO post = postRepo.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
        Assert.isTrue(post.getIsVisible(), ErrorCode.POST_NOT_FOUND);
        return post;
    }

    private PostResponse toResponse(PostDO post) {
        String nickname = userRepo.findById(post.getUserId())
                .map(u -> u.getNickname()).orElse("Unknown");
        String merchantName = post.getMerchantId() == null ? null :
                merchantRepo.findById(post.getMerchantId())
                        .map(m -> m.getName()).orElse(null);
        return PostResponse.builder()
                .id(post.getId())
                .userId(post.getUserId())
                .userNickname(nickname)
                .merchantId(post.getMerchantId())
                .merchantName(merchantName)
                .content(post.getContent())
                .imageUrls(post.getImageUrls())
                .likeCount(post.getLikeCount())
                .commentCount(post.getCommentCount())
                .createdAt(post.getCreatedAt())
                .build();
    }
}
