package com.lastcalleats.user.service.impl;

import com.lastcalleats.common.exception.BusinessException;
import com.lastcalleats.common.exception.ErrorCode;
import com.lastcalleats.merchant.entity.MerchantDO;
import com.lastcalleats.merchant.repository.MerchantRepo;
import com.lastcalleats.product.entity.ProductListingDO;
import com.lastcalleats.product.entity.ProductTemplateDO;
import com.lastcalleats.product.repository.ProductListingRepo;
import com.lastcalleats.product.repository.ProductTemplateRepo;
import com.lastcalleats.user.dto.FavoriteListingResponse;
import com.lastcalleats.user.entity.UserFavoriteDO;
import com.lastcalleats.user.repository.UserFavoriteRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for FavoriteServiceImpl.
 * Cover add, remove, list favorites and the isFavorite check.
 */
@ExtendWith(MockitoExtension.class)
class FavoriteServiceImplTest {

    @Mock
    private UserFavoriteRepo userFavoriteRepo;
    @Mock
    private ProductListingRepo productListingRepo;
    @Mock
    private ProductTemplateRepo productTemplateRepo;
    @Mock
    private MerchantRepo merchantRepo;

    @InjectMocks
    private FavoriteServiceImpl favoriteService;

    private ProductListingDO testListing;
    private ProductTemplateDO testTemplate;
    private MerchantDO testMerchant;
    private UserFavoriteDO testFavorite;

    @BeforeEach
    void setUp() {
        testTemplate = new ProductTemplateDO();
        testTemplate.setId(10L);
        testTemplate.setMerchantId(1L);
        testTemplate.setName("Bento Box");
        testTemplate.setDescription("Delicious bento");
        testTemplate.setOriginalPrice(new BigDecimal("15.00"));

        testListing = new ProductListingDO();
        testListing.setId(20L);
        testListing.setMerchantId(1L);
        testListing.setTemplateId(10L);
        testListing.setDiscountPrice(new BigDecimal("8.00"));
        testListing.setRemainingQuantity(5);
        testListing.setPickupStart(LocalTime.of(18, 0));
        testListing.setPickupEnd(LocalTime.of(20, 0));
        testListing.setDate(LocalDate.now());
        testListing.setIsAvailable(true);

        testMerchant = new MerchantDO();
        testMerchant.setId(1L);
        testMerchant.setName("Good Eats");
        testMerchant.setAddress("123 Main St");

        testFavorite = new UserFavoriteDO();
        testFavorite.setId(100L);
        testFavorite.setUserId(5L);
        testFavorite.setListingId(20L);
        testFavorite.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void addFavorite_success() {
        when(productListingRepo.findById(20L)).thenReturn(Optional.of(testListing));
        when(userFavoriteRepo.existsByUserIdAndListingId(5L, 20L)).thenReturn(false);

        favoriteService.addFavorite(5L, 20L);

        verify(userFavoriteRepo).save(any(UserFavoriteDO.class));
    }

    @Test
    void addFavorite_listingNotFound_throwsException() {
        when(productListingRepo.findById(99L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> favoriteService.addFavorite(5L, 99L));
        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
        verify(userFavoriteRepo, never()).save(any());
    }

    @Test
    void addFavorite_alreadyFavourited_throwsException() {
        when(productListingRepo.findById(20L)).thenReturn(Optional.of(testListing));
        when(userFavoriteRepo.existsByUserIdAndListingId(5L, 20L)).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> favoriteService.addFavorite(5L, 20L));
        assertEquals(ErrorCode.BAD_REQUEST, ex.getErrorCode());
        verify(userFavoriteRepo, never()).save(any());
    }

    @Test
    void removeFavorite_success() {
        when(userFavoriteRepo.existsByUserIdAndListingId(5L, 20L)).thenReturn(true);

        favoriteService.removeFavorite(5L, 20L);

        verify(userFavoriteRepo).deleteByUserIdAndListingId(5L, 20L);
    }

    @Test
    void removeFavorite_notFound_throwsException() {
        when(userFavoriteRepo.existsByUserIdAndListingId(5L, 99L)).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> favoriteService.removeFavorite(5L, 99L));
        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
        verify(userFavoriteRepo, never()).deleteByUserIdAndListingId(any(), any());
    }

    @Test
    void listFavorites_oneFavourite_returnsResponse() {
        when(userFavoriteRepo.findByUserId(5L)).thenReturn(List.of(testFavorite));
        when(productListingRepo.findById(20L)).thenReturn(Optional.of(testListing));
        when(productTemplateRepo.findById(10L)).thenReturn(Optional.of(testTemplate));
        when(merchantRepo.findById(1L)).thenReturn(Optional.of(testMerchant));

        List<FavoriteListingResponse> result = favoriteService.listFavorites(5L);

        assertEquals(1, result.size());
        FavoriteListingResponse r = result.get(0);
        assertEquals(20L, r.getListingId());
        assertEquals(1L, r.getMerchantId());
        assertEquals("Good Eats", r.getMerchantName());
        assertEquals("Bento Box", r.getProductName());
        assertEquals(new BigDecimal("8.00"), r.getDiscountPrice());
    }

    @Test
    void listFavorites_empty_returnsEmptyList() {
        when(userFavoriteRepo.findByUserId(5L)).thenReturn(List.of());

        List<FavoriteListingResponse> result = favoriteService.listFavorites(5L);

        assertTrue(result.isEmpty());
    }

    @Test
    void isFavorite_exists_returnsTrue() {
        when(userFavoriteRepo.existsByUserIdAndListingId(5L, 20L)).thenReturn(true);
        assertTrue(favoriteService.isFavorite(5L, 20L));
    }

    @Test
    void isFavorite_absent_returnsFalse() {
        when(userFavoriteRepo.existsByUserIdAndListingId(5L, 20L)).thenReturn(false);
        assertFalse(favoriteService.isFavorite(5L, 20L));
    }
}
