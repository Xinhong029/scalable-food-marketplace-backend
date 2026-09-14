package com.lastcalleats.merchant.service.impl;

import com.lastcalleats.common.exception.BusinessException;
import com.lastcalleats.common.exception.ErrorCode;
import com.lastcalleats.merchant.dto.MerchantProfileRequest;
import com.lastcalleats.merchant.dto.MerchantProfileResponse;
import com.lastcalleats.merchant.entity.MerchantDO;
import com.lastcalleats.merchant.repository.MerchantRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MerchantServiceImpl.
 * Cover getProfile, updateProfile, and getPublicProfile with success and not-found cases.
 */
@ExtendWith(MockitoExtension.class)
class MerchantServiceImplTest {

    @Mock
    private MerchantRepo merchantRepo;

    @InjectMocks
    private MerchantServiceImpl merchantService;

    private MerchantDO testMerchant;

    @BeforeEach
    void setUp() {
        testMerchant = new MerchantDO();
        testMerchant.setId(1L);
        testMerchant.setEmail("merchant@example.com");
        testMerchant.setName("Good Eats");
        testMerchant.setAddress("123 Main St");
        testMerchant.setBusinessHours("09:00-21:00");
        testMerchant.setIsActive(true);
    }

    @Test
    void getProfile_merchantExists_returnsProfile() {
        when(merchantRepo.findById(1L)).thenReturn(Optional.of(testMerchant));

        MerchantProfileResponse response = merchantService.getProfile(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("merchant@example.com", response.getEmail());
        assertEquals("Good Eats", response.getName());
        assertEquals("123 Main St", response.getAddress());
        assertEquals("09:00-21:00", response.getBusinessHours());
        assertTrue(response.getIsActive());
        verify(merchantRepo).findById(1L);
    }

    @Test
    void getProfile_merchantNotFound_throwsException() {
        when(merchantRepo.findById(99L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> merchantService.getProfile(99L));
        assertEquals(ErrorCode.MERCHANT_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void updateProfile_validRequest_updatesFields() {
        when(merchantRepo.findById(1L)).thenReturn(Optional.of(testMerchant));
        when(merchantRepo.save(any(MerchantDO.class))).thenReturn(testMerchant);

        MerchantProfileRequest request = new MerchantProfileRequest();
        request.setName("New Name");
        request.setAddress("456 New Ave");
        request.setBusinessHours("10:00-22:00");

        merchantService.updateProfile(1L, request);

        assertEquals("New Name", testMerchant.getName());
        assertEquals("456 New Ave", testMerchant.getAddress());
        assertEquals("10:00-22:00", testMerchant.getBusinessHours());
        verify(merchantRepo).save(testMerchant);
    }

    @Test
    void updateProfile_merchantNotFound_throwsException() {
        when(merchantRepo.findById(99L)).thenReturn(Optional.empty());

        MerchantProfileRequest request = new MerchantProfileRequest();
        request.setName("Name");
        request.setAddress("Addr");

        assertThrows(BusinessException.class, () -> merchantService.updateProfile(99L, request));
        verify(merchantRepo, never()).save(any());
    }

    @Test
    void getPublicProfile_merchantExists_returnsProfile() {
        when(merchantRepo.findById(1L)).thenReturn(Optional.of(testMerchant));

        MerchantProfileResponse response = merchantService.getPublicProfile(1L);

        assertNotNull(response);
        assertEquals("Good Eats", response.getName());
    }

    @Test
    void getPublicProfile_merchantNotFound_throwsException() {
        when(merchantRepo.findById(99L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> merchantService.getPublicProfile(99L));
        assertEquals(ErrorCode.MERCHANT_NOT_FOUND, ex.getErrorCode());
    }
}
