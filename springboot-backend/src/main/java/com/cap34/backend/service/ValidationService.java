package com.cap34.backend.service;

import com.cap34.backend.dto.ValidationRequest;
import com.cap34.backend.entity.*;
import com.cap34.backend.repository.*;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class ValidationService {
    private final DatasetRepository datasets;
    private final ContractVersionRepository contracts;
    private final ValidationRunRepository runs;

    public ValidationService(DatasetRepository d, ContractVersionRepository c, ValidationRunRepository r) {
        datasets = d;
        contracts = c;
        runs = r;
    }

    public ValidationRun create(ValidationRequest r) {
        ValidationRun v = new ValidationRun();
        v.setDataset(
                datasets.findById(r.datasetId()).orElseThrow(() -> new NoSuchElementException("Dataset not found")));
        if (r.contractId() != null)
            v.setContract(contracts.findById(r.contractId())
                    .orElseThrow(() -> new NoSuchElementException("Contract not found")));
        if (r.baseContractId() != null)
            v.setBaseContract(contracts.findById(r.baseContractId())
                    .orElseThrow(() -> new NoSuchElementException("Base contract not found")));
        v.setValidationType(r.validationType());
        v.setSourceFile(r.sourceFile());
        v.setResult(r.result());
        v.setErrorSummary(r.errorSummary());
        v.setCompletedAt(LocalDateTime.now());
        return runs.save(v);
    }

    public List<ValidationRun> all() {
        return runs.findAll();
    }

    public List<ValidationRun> byDataset(Long id) {
        return runs.findByDatasetDatasetIdOrderByCreatedAtDesc(id);
    }
}
