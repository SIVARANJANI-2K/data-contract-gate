package com.cap34.backend.controller;

import com.cap34.backend.dto.DependencyRequest;
import com.cap34.backend.entity.ConsumerDependency;
import com.cap34.backend.service.DependencyService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/dependencies")
public class DependencyController {
    private final DependencyService s;

    public DependencyController(DependencyService s) {
        this.s = s;
    }

    @PostMapping
    public ConsumerDependency create(@Valid @RequestBody DependencyRequest r) {
        return s.create(r);
    }

    @GetMapping
    public List<ConsumerDependency> all() {
        return s.all();
    }

    @GetMapping("/impact")
    public List<ConsumerDependency> impact(@RequestParam Long datasetId, @RequestParam String fieldName) {
        return s.impacted(datasetId, fieldName);
    }
}
