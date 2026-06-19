package com.platform.data.entity.dto;

import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record UpsertEntityRecordRequest(@NotNull Map<String, Object> data) {}
