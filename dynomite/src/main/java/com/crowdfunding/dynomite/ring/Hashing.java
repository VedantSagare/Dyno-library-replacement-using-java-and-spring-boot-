package com.crowdfunding.dynomite.ring;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

final class Hashing {

    private Hashing() {
    }

    static long hash64(String input) {
        // Good enough for a learning scaffold: stable, portable, no extra deps.
        // We take the first 8 bytes of SHA-256 as an unsigned 64-bit number.
        byte[] digest = sha256(input.getBytes(StandardCharsets.UTF_8));
        long value = 0;
        for (int i = 0; i < 8; i++) {
            value = (value << 8) | (digest[i] & 0xffL);
        }
        return value;
    }

    private static byte[] sha256(byte[] input) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            return messageDigest.digest(input);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}

