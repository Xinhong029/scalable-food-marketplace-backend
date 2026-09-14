package com.lastcalleats.common.util;

import java.security.SecureRandom;

/**
 * Generates pickup codes handed to customers after payment.
 * Uses {@link SecureRandom} to avoid predictable sequences.
 */
public class PickupCodeUtil {

    private static final SecureRandom RANDOM = new SecureRandom();

    private PickupCodeUtil() {}

    /** @return a random 6-digit string in the range [100000, 999999] */
    public static String generateNumericCode() {
        int code = 100000 + RANDOM.nextInt(900000);
        return String.valueOf(code);
    }
}
