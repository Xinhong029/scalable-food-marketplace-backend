package com.lastcalleats.review.factory;

import com.lastcalleats.common.exception.BusinessException;
import com.lastcalleats.common.exception.ErrorCode;
import com.lastcalleats.common.util.Assert;
import com.lastcalleats.order.entity.OrderDO;
import com.lastcalleats.order.repository.OrderRepo;
import com.lastcalleats.product.entity.ProductListingDO;
import com.lastcalleats.product.repository.ProductListingRepo;
import com.lastcalleats.review.dto.CreateReviewRequest;
import com.lastcalleats.review.entity.ReviewDO;
import com.lastcalleats.review.repository.ReviewRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Validates review preconditions and assembles a {@link ReviewDO} ready for persistence.
 * Kept separate from the service layer so the validation logic is independently testable.
 */
@Component
@RequiredArgsConstructor
public class ReviewFactory {

    private final OrderRepo orderRepo;
    private final ProductListingRepo listingRepo;
    private final ReviewRepo reviewRepo;

    /**
     * Checks order exists → belongs to user → is COMPLETED → not yet reviewed, then builds the entity.
     *
     * @param userId  must match the order's user ID
     * @param request rating, content, and image URLs from the client
     * @return a populated {@link ReviewDO} that has not yet been saved
     * @throws com.lastcalleats.common.exception.BusinessException on any failed precondition
     */
    public ReviewDO create(Long userId, CreateReviewRequest request) {
        OrderDO order = orderRepo.findById(request.getOrderId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        Assert.equals(order.getUserId(), userId, ErrorCode.FORBIDDEN);
        Assert.isTrue(order.getStatus() == OrderDO.OrderStatus.COMPLETED, ErrorCode.REVIEW_NOT_ALLOWED);
        Assert.isTrue(!reviewRepo.existsByOrderId(request.getOrderId()), ErrorCode.REVIEW_ALREADY_EXISTS);

        ProductListingDO listing = listingRepo.findById(order.getListingId())
                .orElseThrow(() -> new BusinessException(ErrorCode.LISTING_NOT_FOUND));

        return ReviewDO.builder()
                .orderId(order.getId())
                .userId(userId)
                .merchantId(order.getMerchantId())
                .templateId(listing.getTemplateId())
                .rating(request.getRating())
                .content(request.getContent())
                .imageUrls(request.getImageUrls())
                .build();
    }
}
