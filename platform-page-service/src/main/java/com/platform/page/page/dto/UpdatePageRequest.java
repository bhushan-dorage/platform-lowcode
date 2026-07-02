package com.platform.page.page.dto;

public record UpdatePageRequest(
        String name,
        String description,
        String schema
) {}
