package com.lastcalleats.common.util;

import com.lastcalleats.common.exception.BusinessException;
import com.lastcalleats.common.exception.ErrorCode;

/**
 * Guard utilities for validating preconditions in service methods.
 * Each method throws a {@link BusinessException} with the given error code when the check fails.
 */
public class Assert {

    private Assert() {}

    /**
     * @param condition must be {@code true}; throws otherwise
     * @param errorCode raised when the condition is false
     */
    public static void isTrue(boolean condition, ErrorCode errorCode) {
        if (!condition) throw new BusinessException(errorCode);
    }

    /**
     * @param obj       must be non-null; throws otherwise
     * @param errorCode raised when the object is null
     */
    public static void notNull(Object obj, ErrorCode errorCode) {
        if (obj == null) throw new BusinessException(errorCode);
    }

    /**
     * @param a         first value
     * @param b         second value; throws if not equal to {@code a}
     * @param errorCode raised when the values differ
     */
    public static void equals(Object a, Object b, ErrorCode errorCode) {
        if (!a.equals(b)) throw new BusinessException(errorCode);
    }

    /**
     * @param str       must be non-null and non-blank; throws otherwise
     * @param errorCode raised when the string is null or blank
     */
    public static void notBlank(String str, ErrorCode errorCode) {
        if (str == null || str.isBlank()) throw new BusinessException(errorCode);
    }
}
