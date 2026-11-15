package com.cap34.backend.dto;

import jakarta.validation.constraints.*;

public record DatasetRequest(@NotBlank String datasetName, @NotBlank String producerName, String description) {
}
