package com.cap34.backend.dto;

import jakarta.validation.constraints.*;

public record ConsumerRequest(@NotBlank String consumerName, @NotBlank String consumerType, String owner) {
}
