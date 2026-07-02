package com.platform.audit.siem;

/**
 * Supported SIEM export formats.
 *
 * CEF  — ArcSight Common Event Format (most widely supported SIEM format).
 * LEEF — Log Event Extended Format (IBM QRadar native format).
 * JSON — Raw JSON, suitable for Elasticsearch / Splunk HEC ingestion.
 */
public enum SiemFormat {
    CEF,
    LEEF,
    JSON
}
