package com.platform.webhook.signature;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class HmacSignerTest {

    @Test
    void sign_producesExpectedPrefix() {
        String sig = HmacSigner.sign("hello", "secret");
        assertThat(sig).startsWith("sha256=");
        assertThat(sig).hasSize(71); // "sha256=" + 64 hex chars
    }

    @Test
    void sign_isDeterministic() {
        String sig1 = HmacSigner.sign("payload", "mysecret");
        String sig2 = HmacSigner.sign("payload", "mysecret");
        assertThat(sig1).isEqualTo(sig2);
    }

    @Test
    void sign_differentPayload_differentSignature() {
        String sig1 = HmacSigner.sign("payload-a", "secret");
        String sig2 = HmacSigner.sign("payload-b", "secret");
        assertThat(sig1).isNotEqualTo(sig2);
    }

    @Test
    void sign_differentSecret_differentSignature() {
        String sig1 = HmacSigner.sign("payload", "secret-1");
        String sig2 = HmacSigner.sign("payload", "secret-2");
        assertThat(sig1).isNotEqualTo(sig2);
    }

    @Test
    void verify_correctPayloadAndSecret_returnsTrue() {
        String payload = "test-payload";
        String secret = "test-secret";
        String signature = HmacSigner.sign(payload, secret);
        assertThat(HmacSigner.verify(payload, secret, signature)).isTrue();
    }

    @Test
    void verify_tamperedPayload_returnsFalse() {
        String secret = "test-secret";
        String signature = HmacSigner.sign("original", secret);
        assertThat(HmacSigner.verify("tampered", secret, signature)).isFalse();
    }

    @Test
    void verify_wrongSecret_returnsFalse() {
        String payload = "test-payload";
        String signature = HmacSigner.sign(payload, "correct-secret");
        assertThat(HmacSigner.verify(payload, "wrong-secret", signature)).isFalse();
    }
}
