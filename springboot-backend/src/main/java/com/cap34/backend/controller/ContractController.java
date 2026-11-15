package com.cap34.backend.controller;

import com.cap34.backend.dto.ContractCreationResponse;
import com.cap34.backend.dto.ContractRequest;
import com.cap34.backend.entity.*;
import com.cap34.backend.service.ContractService;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/contracts")
public class ContractController {
    private final ContractService s;

    public ContractController(ContractService s) {
        this.s = s;
    }

    @PostMapping
    public ResponseEntity<ContractCreationResponse> create(
            @Valid @RequestBody ContractRequest request) {

        ContractCreationResponse response = s.create(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ContractVersion get(@PathVariable Long id) {
        return s.get(id);
    }

    @GetMapping("/dataset/{datasetId}")
    public List<ContractVersion> byDataset(@PathVariable Long datasetId) {
        return s.byDataset(datasetId);
    }

    @GetMapping("/{id}/fields")
    public List<ContractField> fields(@PathVariable Long id) {
        return s.fields(id);
    }

    @PatchMapping("/{id}/status")
    public ContractVersion status(@PathVariable Long id, @RequestParam String value) {
        return s.updateStatus(id, value);
    }
}
