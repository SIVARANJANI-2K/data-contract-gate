package com.cap34.backend.service;

import com.cap34.backend.dto.DependencyRequest;
import com.cap34.backend.entity.*;
import com.cap34.backend.repository.*;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class DependencyService {
    private final ConsumerRepository consumers;
    private final DatasetRepository datasets;
    private final ConsumerDependencyRepository deps;

    public DependencyService(ConsumerRepository c, DatasetRepository d, ConsumerDependencyRepository x) {
        consumers = c;
        datasets = d;
        deps = x;
    }

    public ConsumerDependency create(DependencyRequest r) {
        ConsumerDependency x = new ConsumerDependency();
        x.setConsumer(
                consumers.findById(r.consumerId()).orElseThrow(() -> new NoSuchElementException("Consumer not found")));
        x.setDataset(
                datasets.findById(r.datasetId()).orElseThrow(() -> new NoSuchElementException("Dataset not found")));
        x.setFieldName(r.fieldName());
        x.setUsageType(r.usageType());
        return deps.save(x);
    }

    public List<ConsumerDependency> all() {
        return deps.findAll();
    }

    public List<ConsumerDependency> impacted(Long datasetId, String field) {
        return deps.findByDatasetDatasetIdAndFieldName(datasetId, field);
    }
}
