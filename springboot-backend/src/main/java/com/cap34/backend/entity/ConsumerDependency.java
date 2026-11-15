package com.cap34.backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "consumer_dependencies", uniqueConstraints = @UniqueConstraint(columnNames = { "consumer_id",
        "dataset_id", "field_name" }))
public class ConsumerDependency {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dependency_id")
    private Long dependencyId;

    @ManyToOne
    @JoinColumn(name = "consumer_id", nullable = false)
    private Consumer consumer;

    @ManyToOne
    @JoinColumn(name = "dataset_id", nullable = false)
    private Dataset dataset;

    @Column(name = "field_name", nullable = false, length = 100)
    private String fieldName;
    
    @Column(name = "usage_type", length = 100)
    private String usageType;

    public Long getDependencyId() {
        return dependencyId;
    }

    public Consumer getConsumer() {
        return consumer;
    }

    public void setConsumer(Consumer v) {
        consumer = v;
    }

    public Dataset getDataset() {
        return dataset;
    }

    public void setDataset(Dataset v) {
        dataset = v;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String v) {
        fieldName = v;
    }

    public String getUsageType() {
        return usageType;
    }

    public void setUsageType(String v) {
        usageType = v;
    }
}
