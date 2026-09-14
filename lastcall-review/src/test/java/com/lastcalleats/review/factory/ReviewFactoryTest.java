package com.lastcalleats.review.factory;

import com.lastcalleats.common.exception.BusinessException;
import com.lastcalleats.common.exception.ErrorCode;
import com.lastcalleats.order.entity.OrderDO;
import com.lastcalleats.order.repository.OrderRepo;
import com.lastcalleats.product.entity.ProductListingDO;
import com.lastcalleats.product.repository.ProductListingRepo;
import com.lastcalleats.review.dto.CreateReviewRequest;
import com.lastcalleats.review.entity.ReviewDO;
import com.lastcalleats.review.repository.ReviewRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewFactoryTest {

    @Mock private OrderRepo orderRepo;
    @Mock private ProductListingRepo listingRepo;
    @Mock private ReviewRepo reviewRepo;

    @InjectMocks private ReviewFactory reviewFactory;

    private OrderDO completedOrder;
    private ProductListingDO listing;
    private CreateReviewRequest request;

    @BeforeEach
    void setUp() {
        completedOrder = OrderDO.builder()
                .id(1L).userId(10L).merchantId(20L).listingId(30L)
                .status(OrderDO.OrderStatus.COMPLETED)
                .build();

        listing = ProductListingDO.builder()
                .id(30L).templateId(40L).build();

        request = new CreateReviewRequest();
        request.setOrderId(1L);
        request.setRating(5);
        request.setContent("Great food!");
    }

    @Test
    void create_shouldBuildReviewWithCorrectFields() {
        when(orderRepo.findById(1L)).thenReturn(Optional.of(completedOrder));
        when(reviewRepo.existsByOrderId(1L)).thenReturn(false);
        when(listingRepo.findById(30L)).thenReturn(Optional.of(listing));

        ReviewDO review = reviewFactory.create(10L, request);

        assertEquals(1L, review.getOrderId());
        assertEquals(10L, review.getUserId());
        assertEquals(20L, review.getMerchantId());
        assertEquals(40L, review.getTemplateId());
        assertEquals(5, review.getRating());
        assertEquals("Great food!", review.getContent());
    }

    @Test
    void create_shouldThrowWhenOrderNotFound() {
        when(orderRepo.findById(1L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> reviewFactory.create(10L, request));
        assertEquals(ErrorCode.ORDER_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void create_shouldThrowWhenUserMismatch() {
        when(orderRepo.findById(1L)).thenReturn(Optional.of(completedOrder));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> reviewFactory.create(99L, request));
        assertEquals(ErrorCode.FORBIDDEN, ex.getErrorCode());
    }

    @Test
    void create_shouldThrowWhenOrderNotCompleted() {
        completedOrder.setStatus(OrderDO.OrderStatus.PAID);
        when(orderRepo.findById(1L)).thenReturn(Optional.of(completedOrder));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> reviewFactory.create(10L, request));
        assertEquals(ErrorCode.REVIEW_NOT_ALLOWED, ex.getErrorCode());
    }

    @Test
    void create_shouldThrowWhenReviewAlreadyExists() {
        when(orderRepo.findById(1L)).thenReturn(Optional.of(completedOrder));
        when(reviewRepo.existsByOrderId(1L)).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> reviewFactory.create(10L, request));
        assertEquals(ErrorCode.REVIEW_ALREADY_EXISTS, ex.getErrorCode());
    }
}
