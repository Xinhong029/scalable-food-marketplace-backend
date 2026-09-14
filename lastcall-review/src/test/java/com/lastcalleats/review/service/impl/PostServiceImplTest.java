package com.lastcalleats.review.service.impl;

import com.lastcalleats.common.exception.BusinessException;
import com.lastcalleats.common.exception.ErrorCode;
import com.lastcalleats.merchant.entity.MerchantDO;
import com.lastcalleats.merchant.repository.MerchantRepo;
import com.lastcalleats.review.dto.CreatePostRequest;
import com.lastcalleats.review.dto.PostResponse;
import com.lastcalleats.review.entity.PostDO;
import com.lastcalleats.review.repository.PostRepo;
import com.lastcalleats.user.entity.UserDO;
import com.lastcalleats.user.repository.UserRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostServiceImplTest {

    @Mock private PostRepo postRepo;
    @Mock private UserRepo userRepo;
    @Mock private MerchantRepo merchantRepo;

    @InjectMocks private PostServiceImpl postService;

    private PostDO visiblePost;

    @BeforeEach
    void setUp() {
        visiblePost = PostDO.builder()
                .id(1L).userId(10L).merchantId(20L)
                .content("Loved it!").isVisible(true)
                .build();
    }

    @Test
    void createPost_shouldSaveAndReturnResponse() {
        CreatePostRequest request = new CreatePostRequest();
        request.setContent("Hello world");
        request.setMerchantId(20L);

        when(postRepo.save(any(PostDO.class))).thenReturn(visiblePost);
        when(userRepo.findById(10L)).thenReturn(Optional.of(userWithNickname("Alice")));
        when(merchantRepo.findById(20L)).thenReturn(Optional.of(merchantWithName("Sushi Bar")));

        PostResponse response = postService.createPost(10L, request);

        verify(postRepo).save(any(PostDO.class));
        assertEquals("Alice", response.getUserNickname());
        assertEquals("Sushi Bar", response.getMerchantName());
    }

    @Test
    void getPost_shouldReturnResponseWithNickname() {
        when(postRepo.findById(1L)).thenReturn(Optional.of(visiblePost));
        when(userRepo.findById(10L)).thenReturn(Optional.of(userWithNickname("Bob")));
        when(merchantRepo.findById(20L)).thenReturn(Optional.empty());

        PostResponse response = postService.getPost(1L);

        assertEquals(1L, response.getId());
        assertEquals("Bob", response.getUserNickname());
    }

    @Test
    void getPost_shouldThrowWhenPostNotFound() {
        when(postRepo.findById(1L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> postService.getPost(1L));
        assertEquals(ErrorCode.POST_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void getPost_shouldThrowWhenPostIsHidden() {
        visiblePost.setIsVisible(false);
        when(postRepo.findById(1L)).thenReturn(Optional.of(visiblePost));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> postService.getPost(1L));
        assertEquals(ErrorCode.POST_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void deletePost_shouldDeleteWhenUserIsOwner() {
        when(postRepo.findById(1L)).thenReturn(Optional.of(visiblePost));

        postService.deletePost(10L, 1L);

        verify(postRepo).delete(visiblePost);
    }

    @Test
    void deletePost_shouldThrowWhenUserIsNotOwner() {
        when(postRepo.findById(1L)).thenReturn(Optional.of(visiblePost));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> postService.deletePost(99L, 1L));
        assertEquals(ErrorCode.POST_FORBIDDEN, ex.getErrorCode());
    }

    private UserDO userWithNickname(String nickname) {
        UserDO user = new UserDO();
        user.setNickname(nickname);
        return user;
    }

    private MerchantDO merchantWithName(String name) {
        MerchantDO merchant = new MerchantDO();
        merchant.setName(name);
        return merchant;
    }
}
