package com.foody.users.service;

import java.security.SecureRandom;

/** Generates opaque, human-readable IDs with 64 bits of random entropy. */
public final class PublicCustomerIdGenerator {
    private static final char[] ALPHABET = "0123456789ABCDEF".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    private PublicCustomerIdGenerator() {}

    public static String generate() {
        StringBuilder value = new StringBuilder("F-");
        for (int i = 0; i < 16; i++) value.append(ALPHABET[RANDOM.nextInt(ALPHABET.length)]);
        return value.toString();
    }
}
