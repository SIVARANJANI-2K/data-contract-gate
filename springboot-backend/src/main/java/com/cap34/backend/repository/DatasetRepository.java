package com.cap34.backend.repository;

import com.cap34.backend.entity.Dataset;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface DatasetRepository extends JpaRepository<Dataset, Long> {
    Optional<Dataset> findByDatasetName(String name);
}
