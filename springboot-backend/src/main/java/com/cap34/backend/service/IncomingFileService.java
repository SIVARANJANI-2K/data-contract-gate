
package com.cap34.backend.service;

import com.cap34.backend.dto.ValidationRequest;
import com.cap34.backend.entity.ContractVersion;
import com.cap34.backend.repository.ContractVersionRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

@Service
public class IncomingFileService {

        private final ContractVersionRepository contracts;
        private final PythonEngineClient pythonEngineClient;
        private final ValidationService validationService;
        private final ObjectMapper mapper;

        public IncomingFileService(
                        ContractVersionRepository contracts,
                        PythonEngineClient pythonEngineClient,
                        ValidationService validationService,
                        ObjectMapper mapper) {

                this.contracts = contracts;
                this.pythonEngineClient = pythonEngineClient;
                this.validationService = validationService;
                this.mapper = mapper;
        }

        public Map<String, Object> validateUpload(
                        Long datasetId,
                        MultipartFile file) {

                // 1. Find latest approved contract
                ContractVersion approvedContract = contracts.findFirstByDatasetDatasetIdAndStatusOrderByCreatedAtDesc(
                                datasetId,
                                ContractVersion.Status.APPROVED).orElseThrow(
                                                () -> new NoSuchElementException(
                                                                "No approved contract found for dataset: "
                                                                                + datasetId));

                try {
                        // 2. Convert stored contract JSON into Map
                        Map<String, Object> contract = mapper.readValue(
                                        approvedContract.getSchemaDefinition(),
                                        new TypeReference<Map<String, Object>>() {
                                        });

                        // 3. Validate uploaded file using Python
                        Map<String, Object> pythonResult = pythonEngineClient.validateFile(file, contract);

                        // 4. Determine validation result
                        boolean valid = Boolean.TRUE.equals(
                                        pythonResult.get("valid"));

                        String result = valid ? "SUCCESS" : "FAILED";

                        // 5. Prepare error summary
                        String errorSummary = mapper.writeValueAsString(
                                        pythonResult.get("errors"));

                        // 6. Save validation history in MySQL
                        var validationRun = validationService.create(
                                        new ValidationRequest(
                                                        datasetId,
                                                        approvedContract.getContractId(),
                                                        null,
                                                        "PAYLOAD_VALIDATION",
                                                        file.getOriginalFilename(),
                                                        result,
                                                        errorSummary));

                        // 7. Prepare API response
                        Map<String, Object> response = new LinkedHashMap<>(pythonResult);

                        response.put(
                                        "validationId",
                                        validationRun.getValidationId());

                        response.put("result", result);

                        // Add contract information from MySQL
                        response.put("contract_version", approvedContract.getVersion());
                        response.put("contract_id", approvedContract.getContractId());

                        return response;

                } catch (Exception e) {
                        throw new RuntimeException(
                                        "Incoming file validation failed: " + e.getMessage(),
                                        e);
                }
        }
}
