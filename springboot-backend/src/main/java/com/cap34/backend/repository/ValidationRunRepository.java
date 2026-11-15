package com.cap34.backend.repository;

import com.cap34.backend.entity.ValidationRun;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface ValidationRunRepository extends JpaRepository<ValidationRun, Long> {
    List<ValidationRun> findByDatasetDatasetIdOrderByCreatedAtDesc(Long datasetId);
}
