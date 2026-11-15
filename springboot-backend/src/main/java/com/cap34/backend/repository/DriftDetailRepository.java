package com.cap34.backend.repository;

import com.cap34.backend.entity.DriftDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface DriftDetailRepository extends JpaRepository<DriftDetail, Long> {
    List<DriftDetail> findByValidationRunValidationId(Long validationId);
}
