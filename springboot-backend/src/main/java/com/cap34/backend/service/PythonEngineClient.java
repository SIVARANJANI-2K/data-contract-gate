
package com.cap34.backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.springframework.web.multipart.MultipartFile;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpRequest.BodyPublishers;
import java.util.UUID;

@Service
public class PythonEngineClient {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String pythonBaseUrl;

    public PythonEngineClient(
            @Value("${cap34.python.base-url}") String pythonBaseUrl,
            ObjectMapper objectMapper) {

        this.pythonBaseUrl = pythonBaseUrl;
        this.objectMapper = objectMapper;

        // Force HTTP/1.1 because this configuration
        // successfully transmitted the request body.
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    public Map<String, Object> checkCompatibility(
            Map<String, Object> oldContract,
            Map<String, Object> newContract) {

        try {
            // 1. Validate input contracts
            if (oldContract == null || newContract == null) {
                throw new IllegalArgumentException(
                        "Both old_contract and new_contract are required");
            }

            // 2. Prepare JSON request
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("old_contract", oldContract);
            requestBody.put("new_contract", newContract);

            String jsonRequest =
                    objectMapper.writeValueAsString(requestBody);

            System.out.println("Sending compatibility request to Python:");
            System.out.println(jsonRequest);

            // 3. Build HTTP request
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(
                            pythonBaseUrl + "/api/compatibility/check"))
                    .version(HttpClient.Version.HTTP_1_1)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(
                            jsonRequest,
                            StandardCharsets.UTF_8))
                    .build();

            // 4. Send request and receive response
            HttpResponse<String> httpResponse = httpClient.send(
                    httpRequest,
                    HttpResponse.BodyHandlers.ofString(
                            StandardCharsets.UTF_8));

            System.out.println(
                    "Python HTTP Status: " + httpResponse.statusCode());

            System.out.println(
                    "Python Response: " + httpResponse.body());

            // 5. Check HTTP status
            if (httpResponse.statusCode() < 200 ||
                    httpResponse.statusCode() >= 300) {

                throw new RuntimeException(
                        "Python engine returned HTTP " +
                        httpResponse.statusCode() +
                        ": " + httpResponse.body());
            }

            // 6. Check response body
            if (httpResponse.body() == null ||
                    httpResponse.body().isBlank()) {

                throw new RuntimeException(
                        "Python engine returned an empty response body");
            }

            // 7. Convert Python JSON response into Java Map
            Map<String, Object> result =
                    objectMapper.readValue(
                            httpResponse.body(),
                            new TypeReference<Map<String, Object>>() {}
                    );

            System.out.println("Compatibility result: " + result);

            return result;

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Failed to communicate with Python compatibility engine: "
                            + e.getMessage(),
                    e);
        }
    }

public Map<String, Object> validateFile(
        MultipartFile file,
        Map<String, Object> approvedContract) {

    try {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty");
        }

        if (approvedContract == null) {
            throw new IllegalArgumentException("Approved contract is required");
        }

        String boundary = "----CAP34Boundary" + UUID.randomUUID();

        String contractJson =
                objectMapper.writeValueAsString(approvedContract);

        byte[] fileBytes = file.getBytes();

        String fileName = file.getOriginalFilename() == null
                ? "upload.csv"
                : file.getOriginalFilename().replace("\"", "");

        String contentType = file.getContentType() == null
                ? "text/csv"
                : file.getContentType();

        byte[] prefix = (
                "--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"file\"; filename=\"" +
                fileName + "\"\r\n" +
                "Content-Type: " + contentType + "\r\n\r\n"
        ).getBytes(StandardCharsets.UTF_8);

        byte[] middle = (
                "\r\n--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"contract\"\r\n" +
                "Content-Type: application/json\r\n\r\n" +
                contractJson +
                "\r\n--" + boundary + "--\r\n"
        ).getBytes(StandardCharsets.UTF_8);

        byte[] requestBody = new byte[
                prefix.length + fileBytes.length + middle.length
        ];

        System.arraycopy(prefix, 0, requestBody, 0, prefix.length);
        System.arraycopy(fileBytes, 0, requestBody, prefix.length, fileBytes.length);
        System.arraycopy(
                middle, 0, requestBody,
                prefix.length + fileBytes.length, middle.length
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(
                        pythonBaseUrl + "/api/validation/file"))
                .version(HttpClient.Version.HTTP_1_1)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofByteArray(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException(
                    "Python validation failed: " + response.body()
            );
        }

        return objectMapper.readValue(
                response.body(),
                new TypeReference<Map<String, Object>>() {}
        );

    } catch (Exception e) {
        throw new RuntimeException(
                "Failed to validate uploaded file: " + e.getMessage(), e
        );
    }
}

}
