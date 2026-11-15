package com.cap34.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "contract_versions", uniqueConstraints = @UniqueConstraint(columnNames = { "dataset_id", "version" }))
public class ContractVersion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "contract_id")
    private Long contractId;

    @ManyToOne
    @JoinColumn(name = "dataset_id", nullable = false)
    private Dataset dataset;

    @Column(nullable = false, length = 20)
    private String version;

    @Column(name = "schema_definition", nullable = false, columnDefinition = "json")
    private String schemaDefinition;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.DRAFT;
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum Status {
        DRAFT, APPROVED, DEPRECATED
    }

    public Long getContractId() {
        return contractId;
    }

    public Dataset getDataset() {
        return dataset;
    }

    public void setDataset(Dataset v) {
        dataset = v;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String v) {
        version = v;
    }

    public String getSchemaDefinition() {
        return schemaDefinition;
    }

    public void setSchemaDefinition(String v) {
        schemaDefinition = v;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status v) {
        status = v;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
