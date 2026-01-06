package com.neurogate.forge.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.io.File;
import java.util.Map;

@Service
@Slf4j
public class OpenAITrainingProvider implements TrainingProvider {

    private final RestClient restClient;

    public OpenAITrainingProvider(@Value("${spring.ai.openai.api-key}") String apiKey) {
        this.restClient = RestClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    @Override
    public String uploadFile(File file) {
        log.info("Uploading file: {}", file.getName());

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("purpose", "fine-tune");
        body.add("file", new FileSystemResource(file));

        Map<String, Object> response = restClient.post()
                .uri("/files")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .body(Map.class);

        if (response == null || !response.containsKey("id")) {
            throw new RuntimeException("Failed to upload file to OpenAI");
        }

        String fileId = (String) response.get("id");
        log.info("File uploaded successfully. ID: {}", fileId);
        return fileId;
    }

    @Override
    public Map<String, Object> startTrainingJob(String fileId, String baseModel) {
        log.info("Starting fine-tuning job for fileId: {} with model: {}", fileId, baseModel);

        Map<String, Object> request = Map.of(
                "training_file", fileId,
                "model", baseModel);

        return restClient.post()
                .uri("/fine_tuning/jobs")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(Map.class);
    }

    @Override
    public Map<String, Object> getJobStatus(String jobId) {
        log.debug("Checking status for job: {}", jobId);

        return restClient.get()
                .uri("/fine_tuning/jobs/{jobId}", jobId)
                .retrieve()
                .body(Map.class);
    }
}
