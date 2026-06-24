package com.platform.webhook.signature;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

public final class HmacSigner {

    private static final String ALGORITHM = "HmacSHA256";

    private HmacSigner() {}

    /**
     * Computes HMAC-SHA256 of the payload using the given secret, returned as lowercase hex.
     * The signature header value is: sha256={hex}
     */
    public static String sign(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            byte[] hmacBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hmacBytes.length * 2);
            for (byte b : hmacBytes) {
                hex.append(String.format("%02x", b));
            }
            return "sha256=" + hex;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to compute HMAC-SHA256", e);
        }
    }

    /**
     * Constant-time comparison to prevent timing attacks.
     */
    public static boolean verify(String payload, String secret, String expectedSignature) {
        String computed = sign(payload, secret);
        if (computed.length() != expectedSignature.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < computed.length(); i++) {
            result |= computed.charAt(i) ^ expectedSignature.charAt(i);
        }
        return result == 0;
    }
}
