package com.lastcalleats.common.exception;

import lombok.Getter;

/**
 * All recognized application error codes, each pairing an HTTP status with a message.
 * Used by {@link GlobalExceptionHandler} to produce consistent error responses.
 */
@Getter
public enum ErrorCode {

    // 通用
    BAD_REQUEST(400, "Bad request"),
    UNAUTHORIZED(401, "Unauthorized"),
    FORBIDDEN(403, "Forbidden"),
    NOT_FOUND(404, "Resource not found"),
    INTERNAL_ERROR(500, "Internal server error"),

    // 认证
    INVALID_CREDENTIALS(401, "Invalid email or password"),
    TOKEN_EXPIRED(401, "Token expired"),
    TOKEN_INVALID(401, "Token invalid"),
    EMAIL_ALREADY_EXISTS(400, "Email already registered"),

    // 用户
    USER_NOT_FOUND(404, "User not found"),

    // 商家
    MERCHANT_NOT_FOUND(404, "Merchant not found"),

    // 商品
    TEMPLATE_NOT_FOUND(404, "Product template not found"),
    LISTING_NOT_FOUND(404, "Product listing not found"),
    LISTING_NOT_AVAILABLE(400, "Product listing is not available"),
    LISTING_SOLD_OUT(400, "Product listing is sold out"),

    // 订单
    ORDER_NOT_FOUND(404, "Order not found"),
    ORDER_ALREADY_EXISTS(400, "You have already ordered this item today"),
    ORDER_PAYMENT_EXPIRED(400, "Payment time has expired"),
    ORDER_STATUS_INVALID(400, "Invalid order status for this operation"),

    // 通知
    NOTIFICATION_NOT_FOUND(404, "Notification not found"),

    // 取货码
    PICKUP_CODE_INVALID(400, "Invalid pickup code"),
    PICKUP_CODE_ALREADY_USED(400, "Pickup code has already been used"),

    // 支付
    PAYMENT_FAILED(400, "Payment failed"),
    PAYMENT_METHOD_NOT_SUPPORTED(400, "Payment method not supported"),

    // 评价
    REVIEW_ALREADY_EXISTS(400, "You have already reviewed this order"),
    REVIEW_NOT_ALLOWED(400, "Order must be completed before reviewing"),

    // 帖子
    POST_NOT_FOUND(404, "Post not found"),
    POST_FORBIDDEN(403, "You are not allowed to delete this post");

    private final int httpStatus;
    private final String message;

    ErrorCode(int httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
