package com.cap34.backend.repository;

import com.cap34.backend.entity.ContractVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface ContractVersionRepository extends JpaRepository<ContractVersion, Long> {
    Optional<ContractVersion> findByDatasetDatasetIdAndVersion(Long datasetId, String version);

    List<ContractVersion> findByDatasetDatasetIdOrderByCreatedAtDesc(Long datasetId);

    Optional<ContractVersion> findFirstByDatasetDatasetIdAndStatusOrderByCreatedAtDesc(Long datasetId,
            ContractVersion.Status status);
}
