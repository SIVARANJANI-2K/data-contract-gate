package com.cap34.backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "drift_details")
public class DriftDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "drift_id")
    private Long driftId;

    @ManyToOne
    @JoinColumn(name = "validation_id", nullable = false)
    private ValidationRun validationRun;

    @Column(name = "field_name", nullable = false, length = 100)
    private String fieldName;

    @Column(name = "change_type", nullable = false, length = 40)
    private String changeType;

    @Column(name = "old_value", length = 255)
    private String oldValue;

    @Column(name = "new_value", length = 255)
    private String newValue;

    @Column(length = 20)
    private String severity;
    
    @Column(columnDefinition = "TEXT")
    private String reason;

    public Long getDriftId() {
        return driftId;
    }

    public ValidationRun getValidationRun() {
        return validationRun;
    }

    public void setValidationRun(ValidationRun v) {
        validationRun = v;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String v) {
        fieldName = v;
    }

    public String getChangeType() {
        return changeType;
    }

    public void setChangeType(String v) {
        changeType = v;
    }

    public String getOldValue() {
        return oldValue;
    }

    public void setOldValue(String v) {
        oldValue = v;
    }

    public String getNewValue() {
        return newValue;
    }

    public void setNewValue(String v) {
        newValue = v;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String v) {
        severity = v;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String v) {
        reason = v;
    }
}
