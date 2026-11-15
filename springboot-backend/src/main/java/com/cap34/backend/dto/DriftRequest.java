package com.cap34.backend.dto;

import jakarta.validation.constraints.*;

public record DriftRequest(@NotNull Long validationId, @NotBlank String fieldName, @NotBlank String changeType,
        String oldValue, String newValue, String severity, String reason) {
}
