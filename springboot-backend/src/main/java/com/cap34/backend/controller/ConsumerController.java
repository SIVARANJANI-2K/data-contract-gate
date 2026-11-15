package com.cap34.backend.controller;

import com.cap34.backend.dto.ConsumerRequest;
import com.cap34.backend.entity.Consumer;
import com.cap34.backend.service.ConsumerService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/consumers")
public class ConsumerController {
    private final ConsumerService s;

    public ConsumerController(ConsumerService s) {
        this.s = s;
    }

    @PostMapping
    public Consumer create(@Valid @RequestBody ConsumerRequest r) {
        return s.create(r);
    }

    @GetMapping
    public List<Consumer> all() {
        return s.all();
    }

    @GetMapping("/{id}")
    public Consumer get(@PathVariable Long id) {
        return s.get(id);
    }
}
