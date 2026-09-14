package com.lastcalleats.review.service;

import com.lastcalleats.review.dto.CreateReviewRequest;
import com.lastcalleats.review.dto.ReviewResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * Service contract for review operations. Each review is tied to one completed order;
 * precondition enforcement is delegated to {@link com.lastcalleats.review.factory.ReviewFactory}.
 */
public interface ReviewService {

    /**
     * @param userId  the authenticated reviewer
     * @param request rating, optional text, and optional image URLs
     * @return the saved review
     * @throws com.lastcalleats.common.exception.BusinessException if the order is not found,
     *         not owned by the user, not completed, or already reviewed
     */
    ReviewResponse createReview(Long userId, CreateReviewRequest request);

    /**
     * @param orderId the order to look up
     * @return the review, or empty if the order has not been reviewed yet
     */
    Optional<ReviewResponse> getReviewByOrder(Long orderId);

    /**
     * @param merchantId the merchant's ID
     * @param pageable   pagination parameters
     * @return visible reviews for that merchant, newest first
     */
    Page<ReviewResponse> listReviewsByMerchant(Long merchantId, Pageable pageable);

    /**
     * Used on the Discovery screen to show historical ratings per template.
     *
     * @param templateId the product template's ID
     * @param pageable   pagination parameters
     * @return visible reviews for that template, newest first
     */
    Page<ReviewResponse> listReviewsByTemplate(Long templateId, Pageable pageable);
}
