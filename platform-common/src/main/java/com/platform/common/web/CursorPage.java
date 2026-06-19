package com.platform.common.web;

import java.util.List;

/**
 * Cursor-based pagination envelope.
 *
 * Cursor pagination is mandatory across the platform (never offset) because offset
 * pagination produces incorrect results under concurrent inserts and degrades
 * at scale on large tables.
 */
public record CursorPage<T>(List<T> data, Pagination pagination) {

    public record Pagination(String cursor, boolean hasMore, int pageSize) {}

    public static <T> CursorPage<T> of(List<T> data, String cursor, boolean hasMore, int pageSize) {
        return new CursorPage<>(data, new Pagination(cursor, hasMore, pageSize));
    }
}
