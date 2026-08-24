package com.collabos.backend.service;

import com.collabos.backend.dto.AiAskResponse;
import com.collabos.backend.dto.AiProjectManagerResponse;
import com.collabos.backend.dto.AiStatusResponse;
import com.collabos.backend.dto.AiSummaryResponse;
import com.collabos.backend.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;
import java.util.Map;

/**
 * Thin proxy to the Python ai-service (FastAPI + LangChain, Phase 10). Kept
 * behind the existing org/role auth here rather than duplicating auth in
 * Python — ai-service trusts whatever project/document id it's handed, the
 * same way NotificationEventConsumer trusts a Kafka event.
 */
@Service
public class AiService {

    private final RestClient restClient;

    public AiService(@Value("${app.aiservice.url}") String aiServiceUrl) {
        // The JDK HttpClient RestClient uses by default prefers HTTP/2 and will attempt
        // an h2c upgrade over plain HTTP, which uvicorn (HTTP/1.1-only) can't handle —
        // it silently mangles the request body into a 422 rather than erroring cleanly.
        // Pinning HTTP/1.1 avoids the upgrade attempt entirely.
        HttpClient httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
        this.restClient = RestClient.builder()
                .baseUrl(aiServiceUrl)
                .requestFactory(new JdkClientHttpRequestFactory(httpClient))
                .build();
    }

    public AiStatusResponse status() {
        try {
            return restClient.get().uri("/status").retrieve().body(AiStatusResponse.class);
        } catch (RestClientException e) {
            return new AiStatusResponse(false, null, null);
        }
    }

    public AiAskResponse ask(Long projectId, String question) {
        try {
            return restClient.post().uri("/ask")
                    .body(Map.of("project_id", projectId, "question", question))
                    .retrieve()
                    .body(AiAskResponse.class);
        } catch (RestClientException e) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "The AI service is unavailable right now");
        }
    }

    public AiProjectManagerResponse projectManagerReport(Long projectId) {
        try {
            return restClient.post().uri("/agents/project-manager")
                    .body(Map.of("project_id", projectId))
                    .retrieve()
                    .body(AiProjectManagerResponse.class);
        } catch (RestClientException e) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "The AI service is unavailable right now");
        }
    }

    public AiSummaryResponse summarize(Long documentId) {
        try {
            return restClient.post().uri("/documents/{id}/summarize", documentId)
                    .retrieve()
                    .body(AiSummaryResponse.class);
        } catch (RestClientException e) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "The AI service is unavailable right now");
        }
    }
}
