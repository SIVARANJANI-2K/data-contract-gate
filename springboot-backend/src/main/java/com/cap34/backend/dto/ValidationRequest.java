package com.cap34.backend.dto;

import jakarta.validation.constraints.*;

public record ValidationRequest(@NotNull Long datasetId, Long contractId, Long baseContractId,
        @NotBlank String validationType, String sourceFile, @NotBlank String result, String errorSummary) {
}
