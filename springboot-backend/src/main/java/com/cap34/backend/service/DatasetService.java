package com.cap34.backend.service;

import com.cap34.backend.dto.DatasetRequest;
import com.cap34.backend.entity.Dataset;
import com.cap34.backend.repository.DatasetRepository;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class DatasetService {
    private final DatasetRepository repo;

    public DatasetService(DatasetRepository repo) {
        this.repo = repo;
    }

    public Dataset create(DatasetRequest r) {
        if (repo.findByDatasetName(r.datasetName()).isPresent())
            throw new IllegalArgumentException("Dataset name already exists");
        Dataset d = new Dataset();
        d.setDatasetName(r.datasetName());
        d.setProducerName(r.producerName());
        d.setDescription(r.description());
        return repo.save(d);
    }

    public List<Dataset> all() {
        return repo.findAll();
    }

    public Dataset get(Long id) {
        return repo.findById(id).orElseThrow(() -> new NoSuchElementException("Dataset not found: " + id));
    }
}
