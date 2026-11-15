package com.cap34.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "consumers")
public class Consumer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "consumer_id")
    private Long consumerId;
    @Column(name = "consumer_name", nullable = false, unique = true, length = 150)
    private String consumerName;
    @Column(name = "consumer_type", nullable = false, length = 50)
    private String consumerType;
    @Column(length = 150)
    private String owner;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.ACTIVE;
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum Status {
        ACTIVE, INACTIVE
    }

    public Long getConsumerId() {
        return consumerId;
    }

    public String getConsumerName() {
        return consumerName;
    }

    public void setConsumerName(String v) {
        consumerName = v;
    }

    public String getConsumerType() {
        return consumerType;
    }

    public void setConsumerType(String v) {
        consumerType = v;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String v) {
        owner = v;
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
