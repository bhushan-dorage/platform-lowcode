package com.platform.audit.chain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class for computing SHA-256 hash chains over audit events per tenant.
 *
 * Hash chains provide tamper-evidence: each event's hash includes the previous
 * event's hash, so any modification to a historical event invalidates all
 * subsequent hashes in the chain.
 */
public final class EventHashChain {

    private EventHashChain() {
        // Utility class — no instances
    }

    /**
     * Returns the genesis hash for a tenant — the starting anchor of the hash chain.
     */
    public static String genesis(String tenantId) {
        return sha256("GENESIS:" + tenantId);
    }

    /**
     * Computes the hash for a single audit event by chaining it to the previous hash.
     *
     * All fields are joined with "|" so that any change to any field produces a
     * completely different hash.
     */
    public static String compute(String prevHash,
                                 String eventId,
                                 String tenantId,
                                 String timestamp,
                                 String operation,
                                 String resourceId,
                                 String actorUserId) {
        String input = String.join("|",
                prevHash,
                eventId,
                tenantId,
                timestamp,
                operation,
                orEmpty(resourceId),
                orEmpty(actorUserId));
        return sha256(input);
    }

    static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hashBytes.length * 2);
            for (byte b : hashBytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed to be available in every JVM
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    private static String orEmpty(String value) {
        return value != null ? value : "";
    }
}
