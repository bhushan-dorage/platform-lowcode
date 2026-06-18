package com.platform.common.tenant;

/**
 * Thread-local carrier for per-request tenant identity.
 *
 * Contract: callers MUST invoke {@link #clear()} in a finally block to avoid
 * ThreadLocal leaks when threads are reused from a pool (e.g. Tomcat threads).
 */
public final class TenantContext {

    private static final ThreadLocal<TenantContext> HOLDER = new ThreadLocal<>();

    private final String tenantId;
    private final TenantTier tier;
    private final String schema;
    private final String kafkaPrefix;

    private TenantContext(String tenantId, TenantTier tier, String schema, String kafkaPrefix) {
        this.tenantId = tenantId;
        this.tier = tier;
        this.schema = schema;
        this.kafkaPrefix = kafkaPrefix;
    }

    /**
     * Initialises context for the current thread.
     *
     * Schema isolation strategy:
     *   STARTER → all starter tenants share "shared_starter" to keep cluster size manageable.
     *   ENTERPRISE / PROFESSIONAL → dedicated schema "{tenantId}_platform".
     */
    public static void set(String tenantId, TenantTier tier) {
        String schema = (tier == TenantTier.STARTER)
                ? "shared_starter"
                : tenantId + "_platform";
        String kafkaPrefix = tenantId + ".";
        HOLDER.set(new TenantContext(tenantId, tier, schema, kafkaPrefix));
    }

    /** Returns the full context bound to this thread, or {@code null} if not set. */
    public static TenantContext get() {
        return HOLDER.get();
    }

    /** Must be called in finally blocks — clears ThreadLocal to prevent pool-thread leaks. */
    public static void clear() {
        HOLDER.remove();
    }

    // -------------------------------------------------------------------------
    // Static convenience accessors — throw IllegalStateException if context absent
    // -------------------------------------------------------------------------

    public static String getTenantId() {
        return require().tenantId;
    }

    public static TenantTier getTier() {
        return require().tier;
    }

    public static String getSchema() {
        return require().schema;
    }

    public static String getKafkaPrefix() {
        return require().kafkaPrefix;
    }

    // -------------------------------------------------------------------------
    // Instance accessors
    // -------------------------------------------------------------------------

    public String tenantId() {
        return tenantId;
    }

    public TenantTier tier() {
        return tier;
    }

    public String schema() {
        return schema;
    }

    public String kafkaPrefix() {
        return kafkaPrefix;
    }

    // -------------------------------------------------------------------------

    private static TenantContext require() {
        TenantContext ctx = HOLDER.get();
        if (ctx == null) {
            throw new IllegalStateException(
                    "TenantContext is not set on this thread — ensure TenantResolutionFilter ran");
        }
        return ctx;
    }
}
