package com.cap34.backend.controller;

import com.cap34.backend.dto.DriftRequest;
import com.cap34.backend.entity.DriftDetail;
import com.cap34.backend.service.DriftService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/drifts")
public class DriftController {
    private final DriftService s;

    public DriftController(DriftService s) {
        this.s = s;
    }

    @PostMapping
    public DriftDetail create(@Valid @RequestBody DriftRequest r) {
        return s.create(r);
    }

    @GetMapping("/validation/{validationId}")
    public List<DriftDetail> byValidation(@PathVariable Long validationId) {
        return s.byValidation(validationId);
    }
}
