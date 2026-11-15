package com.cap34.backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "contract_fields", uniqueConstraints = @UniqueConstraint(columnNames = { "contract_id", "field_name" }))
public class ContractField {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "field_id")
    private Long fieldId;

    @ManyToOne
    @JoinColumn(name = "contract_id", nullable = false)
    private ContractVersion contract;

    @Column(name = "field_name", nullable = false, length = 100)
    private String fieldName;

    @Column(name = "data_type", nullable = false, length = 50)
    private String dataType;

    @Column(name = "is_nullable")
    private Boolean nullable = false;
    
    @Column(name = "is_required")
    private Boolean required = true;

    public Long getFieldId() {
        return fieldId;
    }

    public ContractVersion getContract() {
        return contract;
    }

    public void setContract(ContractVersion v) {
        contract = v;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String v) {
        fieldName = v;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String v) {
        dataType = v;
    }

    public Boolean getNullable() {
        return nullable;
    }

    public void setNullable(Boolean v) {
        nullable = v;
    }

    public Boolean getRequired() {
        return required;
    }

    public void setRequired(Boolean v) {
        required = v;
    }
}
