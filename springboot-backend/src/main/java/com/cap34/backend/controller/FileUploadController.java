
package com.cap34.backend.controller;

import com.cap34.backend.service.IncomingFileService;
import com.cap34.backend.service.S3Service;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.*;

@RestController
@RequestMapping("/api/files")
public class FileUploadController {

    private final IncomingFileService incomingFileService;
    private final S3Service s3Service;

    public FileUploadController(
            IncomingFileService incomingFileService,
            S3Service s3Service) {

        this.incomingFileService = incomingFileService;
        this.s3Service = s3Service;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("datasetId") Long datasetId)
            throws Exception {

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Uploaded file is empty"));
        }

        String original = Path.of(
                Objects.requireNonNull(file.getOriginalFilename()))
                .getFileName().toString();

        String lower = original.toLowerCase(Locale.ROOT);

        if (!lower.endsWith(".csv") && !lower.endsWith(".json")) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "message",
                            "Only CSV and JSON files are accepted"
                    ));
        }

        // Step 1: Validate file using Python
        Map<String, Object> validation =
                incomingFileService.validateUpload(datasetId, file);

        boolean valid = Boolean.TRUE.equals(validation.get("valid"));

        // Step 2: Select S3 destination
        String folder = valid
                ? "accepted/orders/"
                : "quarantine/orders/";

        // Step 3: Upload file to S3
        String s3Key = s3Service.uploadFile(file, folder);

        // Step 4: Prepare response
        Map<String, Object> response =
                new LinkedHashMap<>(validation);

        response.put("originalFileName", original);
        response.put("s3Bucket", "cap34-data-contract-storage");
        response.put("s3Key", s3Key);

        if (!valid) {
            response.put(
                    "message",
                    "Validation failed. File moved to S3 quarantine."
            );

            return ResponseEntity
                    .status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(response);
        }

        response.put(
                "message",
                "File validated successfully and uploaded to S3 accepted folder."
        );

        return ResponseEntity.ok(response);
    }
}
