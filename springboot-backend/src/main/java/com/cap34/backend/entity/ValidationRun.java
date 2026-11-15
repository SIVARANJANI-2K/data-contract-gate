package com.cap34.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "validation_runs")
public class ValidationRun {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "validation_id")
    private Long validationId;

    @ManyToOne
    @JoinColumn(name = "dataset_id", nullable = false)
    private Dataset dataset;

    @ManyToOne
    @JoinColumn(name = "contract_id")
    private ContractVersion contract;

    @ManyToOne
    @JoinColumn(name = "base_contract_id")
    private ContractVersion baseContract;

    @Column(name = "validation_type", nullable = false, length = 40)
    private String validationType;
    
    @Column(name = "source_file", length = 500)
    private String sourceFile;
    @Column(nullable = false, length = 20)
    private String result;
    @Column(name = "error_summary", columnDefinition = "TEXT")
    private String errorSummary;
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public Long getValidationId() {
        return validationId;
    }

    public Dataset getDataset() {
        return dataset;
    }

    public void setDataset(Dataset v) {
        dataset = v;
    }

    public ContractVersion getContract() {
        return contract;
    }

    public void setContract(ContractVersion v) {
        contract = v;
    }

    public ContractVersion getBaseContract() {
        return baseContract;
    }

    public void setBaseContract(ContractVersion v) {
        baseContract = v;
    }

    public String getValidationType() {
        return validationType;
    }

    public void setValidationType(String v) {
        validationType = v;
    }

    public String getSourceFile() {
        return sourceFile;
    }

    public void setSourceFile(String v) {
        sourceFile = v;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String v) {
        result = v;
    }

    public String getErrorSummary() {
        return errorSummary;
    }

    public void setErrorSummary(String v) {
        errorSummary = v;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime v) {
        completedAt = v;
    }
}
