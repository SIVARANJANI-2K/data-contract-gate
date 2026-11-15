package com.cap34.backend.repository;

import com.cap34.backend.entity.ConsumerDependency;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface ConsumerDependencyRepository extends JpaRepository<ConsumerDependency, Long> {
    List<ConsumerDependency> findByDatasetDatasetIdAndFieldName(Long datasetId, String fieldName);

    List<ConsumerDependency> findByConsumerConsumerId(Long consumerId);
}
