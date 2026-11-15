
package com.cap34.backend.service;

import com.cap34.backend.dto.ContractRequest;
import com.cap34.backend.dto.ContractCreationResponse;
import com.cap34.backend.dto.ValidationRequest;
import com.cap34.backend.dto.DriftRequest;

import com.cap34.backend.entity.*;
import com.cap34.backend.repository.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class ContractService {

    private final DatasetRepository datasets;
    private final ContractVersionRepository contracts;
    private final ContractFieldRepository fields;

    private final ObjectMapper mapper;
    private final PythonEngineClient pythonEngineClient;

    private final ValidationService validationService;
    private final DriftService driftService;

    public ContractService(
            DatasetRepository datasets,
            ContractVersionRepository contracts,
            ContractFieldRepository fields,
            ObjectMapper mapper,
            PythonEngineClient pythonEngineClient,
            ValidationService validationService,
            DriftService driftService) {

        this.datasets = datasets;
        this.contracts = contracts;
        this.fields = fields;
        this.mapper = mapper;
        this.pythonEngineClient = pythonEngineClient;
        this.validationService = validationService;
        this.driftService = driftService;
    }

    @Transactional
    public ContractCreationResponse create(ContractRequest request) {

        // 1. Find dataset
        Dataset dataset = datasets.findById(request.datasetId())
                .orElseThrow(() ->
                        new NoSuchElementException("Dataset not found"));

        // 2. Check duplicate version
        if (contracts.findByDatasetDatasetIdAndVersion(
                dataset.getDatasetId(), request.version()).isPresent()) {

            throw new IllegalArgumentException(
                    "Contract version already exists for this dataset");
        }

        // 3. Check duplicate field names
        Set<String> names = new HashSet<>();

        for (var field : request.fields()) {
            if (!names.add(field.name())) {
                throw new IllegalArgumentException(
                        "Duplicate field: " + field.name());
            }
        }

        // 4. Build contract JSON as an object
        Map<String, Object> proposedContract =
                buildContractJson(dataset, request);

        // 5. Find latest approved contract
        Optional<ContractVersion> approvedContract =
                contracts.findFirstByDatasetDatasetIdAndStatusOrderByCreatedAtDesc(
                        dataset.getDatasetId(),
                        ContractVersion.Status.APPROVED
                );

        Map<String, Object> compatibilityResult;

        ContractVersion baseContract = null;

        // 6. First contract: no baseline available
        if (approvedContract.isEmpty()) {

            compatibilityResult = new HashMap<>();

            compatibilityResult.put("compatible", true);
            compatibilityResult.put("summary",
                    "First contract version. No previous approved contract exists.");

            compatibilityResult.put("breaking_changes", new ArrayList<>());
            compatibilityResult.put("compatible_changes", new ArrayList<>());

        } else {

            // 7. Compare with latest approved contract
            baseContract = approvedContract.get();

            Map<String, Object> oldContract =
                    parseContractJson(baseContract.getSchemaDefinition());

            compatibilityResult =
                    pythonEngineClient.checkCompatibility(
                            oldContract,
                            proposedContract
                    );
        }

        // 8. Determine compatibility status
        boolean compatible = Boolean.TRUE.equals(
                compatibilityResult.get("compatible"));

        String compatibilityStatus;

        if (baseContract == null) {
            compatibilityStatus = "FIRST_VERSION";
        } else if (compatible) {
            compatibilityStatus = "COMPATIBLE";
        } else {
            compatibilityStatus = "BREAKING";
        }

        String summary = String.valueOf(
                compatibilityResult.getOrDefault(
                        "summary",
                        "Compatibility check completed."
                )
        );

        // 9. Save the proposed contract as DRAFT
        String schema;

        try {
            schema = mapper.writeValueAsString(proposedContract);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Could not serialize contract JSON", e);
        }

        ContractVersion contract = new ContractVersion();

        contract.setDataset(dataset);
        contract.setVersion(request.version());
        contract.setSchemaDefinition(schema);
        contract.setStatus(ContractVersion.Status.DRAFT);

        contract = contracts.saveAndFlush(contract);

        // 10. Save normalized field records
        for (var field : request.fields()) {

            ContractField contractField = new ContractField();

            contractField.setContract(contract);
            contractField.setFieldName(field.name());
            contractField.setDataType(field.type());

            contractField.setRequired(
                    field.required() == null || field.required()
            );

            contractField.setNullable(
                    field.nullable() != null && field.nullable()
            );

            fields.save(contractField);
        }

        // 11. Save compatibility validation history
        ValidationRun validationRun = validationService.create(
                new ValidationRequest(
                        dataset.getDatasetId(),
                        contract.getContractId(),
                        baseContract == null
                                ? null
                                : baseContract.getContractId(),
                        "CONTRACT_COMPATIBILITY",
                        null,
                        compatibilityStatus,
                        summary
                )
        );

        // 12. Extract changes returned by Python
        List<Map<String, Object>> breakingChanges =
                getChangeList(compatibilityResult, "breaking_changes");

        List<Map<String, Object>> compatibleChanges =
                getChangeList(compatibilityResult, "compatible_changes");

        // 13. Save breaking changes
        for (Map<String, Object> change : breakingChanges) {

            saveDrift(validationRun.getValidationId(), change, "BREAKING");
        }

        // 14. Save compatible changes
        for (Map<String, Object> change : compatibleChanges) {

            saveDrift(validationRun.getValidationId(), change, "COMPATIBLE");
        }

        // 15. Return response
        return new ContractCreationResponse(
                contract.getContractId(),
                contract.getVersion(),
                contract.getStatus().name(),
                compatibilityStatus,
                validationRun.getValidationId(),
                summary,
                breakingChanges,
                compatibleChanges
        );
    }

    // Build contract JSON using fields object
    private Map<String, Object> buildContractJson(
            Dataset dataset,
            ContractRequest request) {

        Map<String, Object> contractJson = new LinkedHashMap<>();

        contractJson.put("dataset", dataset.getDatasetName());
        contractJson.put("version", request.version());

        Map<String, Object> fieldMap = new LinkedHashMap<>();

        for (var field : request.fields()) {

            Map<String, Object> details = new LinkedHashMap<>();

            details.put("type", field.type());

            details.put(
                    "required",
                    field.required() == null || field.required()
            );

            details.put(
                    "nullable",
                    field.nullable() != null && field.nullable()
            );

            fieldMap.put(field.name(), details);
        }

        contractJson.put("fields", fieldMap);

        return contractJson;
    }

    // Convert stored JSON string into a Map
    private Map<String, Object> parseContractJson(String schema) {

        try {
            return mapper.readValue(
                    schema,
                    new TypeReference<Map<String, Object>>() {}
            );

        } catch (Exception e) {

            throw new IllegalArgumentException(
                    "Could not read stored contract JSON", e);
        }
    }

    // Safely extract change lists from Python response
    private List<Map<String, Object>> getChangeList(
            Map<String, Object> result,
            String key) {

        Object value = result.get(key);

        if (!(value instanceof List<?> list)) {
            return new ArrayList<>();
        }

        List<Map<String, Object>> changes = new ArrayList<>();

        for (Object item : list) {

            if (item instanceof Map<?, ?> map) {

                Map<String, Object> change = new LinkedHashMap<>();

                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    change.put(
                            String.valueOf(entry.getKey()),
                            entry.getValue()
                    );
                }

                changes.add(change);
            }
        }

        return changes;
    }

    // Save one drift record using DriftService
    private void saveDrift(
            Long validationId,
            Map<String, Object> change,
            String severity) {

        String fieldName = firstValue(
                change, "field", "field_name", "name");

        String changeType = firstValue(
                change, "change_type", "type", "change");

        String oldValue = firstValue(
                change, "old_value", "old");

        String newValue = firstValue(
                change, "new_value", "new");

        String reason = firstValue(
                change, "reason", "description", "message");

        if (fieldName == null) {
            fieldName = "UNKNOWN";
        }

        if (changeType == null) {
            changeType = "SCHEMA_CHANGE";
        }

        driftService.create(
                new DriftRequest(
                        validationId,
                        fieldName,
                        changeType,
                        oldValue,
                        newValue,
                        severity,
                        reason
                )
        );
    }

    // Read the first available key
    private String firstValue(
            Map<String, Object> map,
            String... keys) {

        for (String key : keys) {

            Object value = map.get(key);

            if (value != null) {
                return String.valueOf(value);
            }
        }

        return null;
    }

    // Existing methods

    public List<ContractVersion> byDataset(Long id) {
        return contracts.findByDatasetDatasetIdOrderByCreatedAtDesc(id);
    }

    public ContractVersion get(Long id) {
        return contracts.findById(id)
                .orElseThrow(() ->
                        new NoSuchElementException(
                                "Contract not found: " + id));
    }

    @Transactional
    public ContractVersion updateStatus(Long id, String status) {

        ContractVersion contract = get(id);

        try {
            contract.setStatus(
                    ContractVersion.Status.valueOf(
                            status.toUpperCase()
                    )
            );

        } catch (Exception e) {

            throw new IllegalArgumentException(
                    "Status must be DRAFT, APPROVED or DEPRECATED");
        }

        return contracts.save(contract);
    }

    public List<ContractField> fields(Long id) {
        get(id);
        return fields.findByContractContractId(id);
    }
}
