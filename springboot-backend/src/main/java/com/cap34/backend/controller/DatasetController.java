package com.cap34.backend.controller;

import com.cap34.backend.dto.DatasetRequest;
import com.cap34.backend.entity.Dataset;
import com.cap34.backend.service.DatasetService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/datasets")
public class DatasetController {
    private final DatasetService s;

    public DatasetController(DatasetService s) {
        this.s = s;
    }

    @PostMapping
    public Dataset create(@Valid @RequestBody DatasetRequest r) {
        return s.create(r);
    }

    @GetMapping
    public List<Dataset> all() {
        return s.all();
    }

    @GetMapping("/{id}")
    public Dataset get(@PathVariable Long id) {
        return s.get(id);
    }
}
