package com.aireviewer.client;

import com.aireviewer.model.JiraContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Simple Jira client for retrieving issue details. In this MVP the client
 * performs a GET request to the Jira REST API and extracts a few fields into
 * a {@link JiraContext}. Error handling is minimal; failures are logged and
 * an empty context is returned. For a fully featured client consider using
 * asynchronous HTTP libraries and handling pagination, rate limiting, etc.
 */
@Component
public class JiraClient {
    private static final Logger log = LoggerFactory.getLogger(JiraClient.class);

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${jira.base-url:}")
    private String baseUrl;
    @Value("${jira.username:}")
    private String username;
    @Value("${jira.api-token:}")
    private String apiToken;

    /**
     * Fetch a Jira issue by its key. If the call fails, an empty JiraContext
     * is returned and the error is logged.
     *
     * @param key issue key (e.g. PROJECT-123)
     * @return Jira context with basic fields
     */
    public JiraContext fetchIssue(String key) {
        if (key == null || key.isBlank() || baseUrl == null || baseUrl.isBlank()) {
            // Return empty context if configuration is missing or key invalid
            return new JiraContext(key, null, null, null, Collections.emptyList());
        }
        try {
            String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                    .pathSegment("rest", "api", "2", "issue", key)
                    .toUriString();
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            // Basic auth header
            String auth = username + ":" + apiToken;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
            headers.set("Authorization", "Basic " + encodedAuth);

            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map body = response.getBody();
                Map fields = (Map) body.get("fields");
                String summary = fields != null ? (String) fields.get("summary") : null;
                String description = fields != null ? (String) fields.get("description") : null;
                Map issuetype = fields != null ? (Map) fields.get("issuetype") : null;
                String typeName = issuetype != null ? (String) issuetype.get("name") : null;
                // Extract comments if available
                Map comment = fields != null ? (Map) fields.get("comment") : null;
                List<String> commentsList = Collections.emptyList();
                if (comment != null) {
                    List<Map> comments = (List<Map>) comment.get("comments");
                    if (comments != null) {
                        commentsList = comments.stream()
                                .map(c -> (String) c.get("body"))
                                .toList();
                    }
                }
                return new JiraContext(key, summary, description, typeName, commentsList);
            }
        } catch (Exception ex) {
            log.warn("Failed to fetch Jira issue {}: {}", key, ex.getMessage());
        }
        return new JiraContext(key, null, null, null, Collections.emptyList());
    }
}