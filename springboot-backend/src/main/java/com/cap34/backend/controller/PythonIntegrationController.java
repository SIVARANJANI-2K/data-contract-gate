package com.cap34.backend.controller;

import com.cap34.backend.service.PythonEngineClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/python")
public class PythonIntegrationController {

    private final PythonEngineClient pythonEngineClient;

    public PythonIntegrationController(
            PythonEngineClient pythonEngineClient) {
        this.pythonEngineClient = pythonEngineClient;
    }

    @GetMapping("/health")
    public Map<String, Object> checkPythonHealth() {
        // Temporary connectivity test
        return Map.of(
                "message", "Python engine is reachable",
                "pythonUrl", "http://localhost:8001");
    }

    @PostMapping("/compatibility/check")
    public Map checkCompatibility(
            @RequestBody Map<String, Object> request) {

        Map<String, Object> oldContract = (Map<String, Object>) request.get("old_contract");

        Map<String, Object> newContract = (Map<String, Object>) request.get("new_contract");
        if (oldContract == null || newContract == null) {
            throw new IllegalArgumentException(
                    "Both old_contract and new_contract are required");
        }
        return pythonEngineClient.checkCompatibility(
                oldContract, newContract);
    }
}