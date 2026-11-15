package com.cap34.backend.dto;

import jakarta.validation.constraints.*;
import java.util.List;

public record ContractRequest(@NotNull Long datasetId, @NotBlank String version,
        @NotNull List<@NotNull FieldDefinition> fields) {
    public record FieldDefinition(@NotBlank String name, @NotBlank String type, Boolean required, Boolean nullable) {
    }
}
