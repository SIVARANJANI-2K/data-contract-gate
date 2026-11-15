package com.cap34.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "datasets")
public class Dataset {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dataset_id")
    private Long datasetId;
    @Column(name = "dataset_name", nullable = false, unique = true, length = 100)
    private String datasetName;
    @Column(name = "producer_name", nullable = false, length = 150)
    private String producerName;
    @Column(columnDefinition = "TEXT")
    private String description;
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public Long getDatasetId() {
        return datasetId;
    }

    public String getDatasetName() {
        return datasetName;
    }

    public void setDatasetName(String v) {
        datasetName = v;
    }

    public String getProducerName() {
        return producerName;
    }

    public void setProducerName(String v) {
        producerName = v;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String v) {
        description = v;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
