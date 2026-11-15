package com.cap34.backend.controller;

import com.cap34.backend.dto.ValidationRequest;
import com.cap34.backend.entity.ValidationRun;
import com.cap34.backend.service.ValidationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/validations")
public class ValidationController {
    private final ValidationService s;

    public ValidationController(ValidationService s) {
        this.s = s;
    }

    @PostMapping
    public ValidationRun create(@Valid @RequestBody ValidationRequest r) {
        return s.create(r);
    }

    @GetMapping
    public List<ValidationRun> all() {
        return s.all();
    }

    @GetMapping("/dataset/{datasetId}")
    public List<ValidationRun> byDataset(@PathVariable Long datasetId) {
        return s.byDataset(datasetId);
    }
}
