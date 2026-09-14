package com.lastcalleats.common.exception;

import lombok.Getter;

/**
 * Runtime exception for known business rule violations.
 * Carries an {@link ErrorCode} so the global handler can map it to the correct HTTP status.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    /**
     * @param errorCode the error code describing the violation; its message is used as the detail
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /**
     * @param errorCode the error code describing the violation
     * @param message   overrides the error code's default message (e.g. a Stripe error string)
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
