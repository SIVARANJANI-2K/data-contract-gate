package com.cap34.backend.repository;

import com.cap34.backend.entity.ContractField;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface ContractFieldRepository extends JpaRepository<ContractField, Long> {
    List<ContractField> findByContractContractId(Long contractId);
}
