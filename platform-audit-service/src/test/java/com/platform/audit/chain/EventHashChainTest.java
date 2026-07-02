package com.platform.audit.chain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EventHashChainTest {

    @Test
    void genesis_isConsistentForSameTenant() {
        String h1 = EventHashChain.genesis("acme");
        String h2 = EventHashChain.genesis("acme");
        assertThat(h1).isEqualTo(h2).hasSize(64);
    }

    @Test
    void genesis_differsPerTenant() {
        assertThat(EventHashChain.genesis("acme"))
                .isNotEqualTo(EventHashChain.genesis("globex"));
    }

    @Test
    void compute_deterministicForSameInputs() {
        String h = EventHashChain.compute("prev", "evt-1", "acme",
                "2024-01-01T00:00:00Z", "CREATE", "res-123", "user-1");
        assertThat(h).isEqualTo(EventHashChain.compute("prev", "evt-1", "acme",
                "2024-01-01T00:00:00Z", "CREATE", "res-123", "user-1"));
        assertThat(h).hasSize(64);
    }

    @Test
    void compute_changesWhenPrevHashChanges() {
        String h1 = EventHashChain.compute("hash-a", "evt-1", "acme",
                "2024-01-01T00:00:00Z", "CREATE", "res-1", "user-1");
        String h2 = EventHashChain.compute("hash-b", "evt-1", "acme",
                "2024-01-01T00:00:00Z", "CREATE", "res-1", "user-1");
        assertThat(h1).isNotEqualTo(h2);
    }

    @Test
    void compute_formsChain() {
        String genesis = EventHashChain.genesis("acme");
        String h1 = EventHashChain.compute(genesis, "evt-1", "acme",
                "2024-01-01T00:00:00Z", "CREATE", "res-1", "user-1");
        String h2 = EventHashChain.compute(h1, "evt-2", "acme",
                "2024-01-01T00:01:00Z", "UPDATE", "res-1", "user-1");
        // tampering with h1 breaks h2's derivation
        String h2Tampered = EventHashChain.compute("tampered-hash", "evt-2", "acme",
                "2024-01-01T00:01:00Z", "UPDATE", "res-1", "user-1");
        assertThat(h2).isNotEqualTo(h2Tampered);
    }

    @Test
    void compute_toleratesNullResourceAndActor() {
        String h = EventHashChain.compute("prev", "evt-1", "acme",
                "2024-01-01T00:00:00Z", "LOGIN", null, null);
        assertThat(h).hasSize(64);
    }

    @Test
    void sha256_returnsLowercaseHex() {
        String hash = EventHashChain.sha256("hello");
        assertThat(hash).matches("[0-9a-f]{64}");
    }
}
