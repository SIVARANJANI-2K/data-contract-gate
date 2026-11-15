package com.cap34.backend.dto;

import jakarta.validation.constraints.*;

public record DependencyRequest(@NotNull Long consumerId, @NotNull Long datasetId, @NotBlank String fieldName,
        String usageType) {
}
