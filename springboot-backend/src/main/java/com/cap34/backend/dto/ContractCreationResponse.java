
package com.cap34.backend.dto;

import java.util.List;
import java.util.Map;

public record ContractCreationResponse(
        Long contractId,
        String version,
        String status,
        String compatibilityStatus,
        Long validationId,
        String summary,
        List<Map<String, Object>> breakingChanges,
        List<Map<String, Object>> compatibleChanges
) {
}
