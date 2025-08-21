package com.aireviewer.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * GitLab client responsible for interacting with the GitLab REST API. The
 * primary function implemented here is posting a note (comment) on a merge
 * request. Additional helper methods may be added to query changed files,
 * diffs, etc. For the MVP we minimise API usage to avoid network dependency
 * during testing.
 */
@Component
public class GitLabClient {
    private static final Logger log = LoggerFactory.getLogger(GitLabClient.class);
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${gitlab.base-url:}")
    private String baseUrl;
    @Value("${gitlab.api-token:}")
    private String apiToken;

    /**
     * Post a Markdown comment on a merge request. If the call fails, the
     * exception is logged. GitLab will reject comments with an empty body.
     *
     * @param projectId the ID of the project
     * @param mergeRequestIid the internal ID of the merge request
     * @param body the markdown formatted comment
     */
    public void postMergeRequestComment(Long projectId, Long mergeRequestIid, String body) {
        if (baseUrl == null || baseUrl.isBlank()) {
            log.info("GitLabClient not configured; skipping comment posting");
            return;
        }
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .pathSegment("api", "v4", "projects", projectId.toString(), "merge_requests", mergeRequestIid.toString(), "notes")
                .toUriString();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("PRIVATE-TOKEN", apiToken);
        Map<String, String> payload = Map.of("body", body);
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(payload, headers), String.class);
            log.info("Posted comment to MR {}: status {}", mergeRequestIid, response.getStatusCode());
        } catch (Exception ex) {
            log.warn("Failed to post comment to GitLab MR {}: {}", mergeRequestIid, ex.getMessage());
        }
    }

    /**
     * Retrieve the list of changed file paths for a merge request. Some fields
     * are truncated for brevity. In case of failure an empty list is returned.
     * This method can be extended to fetch the diff too.
     *
     * @param projectId the ID of the project
     * @param mergeRequestIid the internal ID of the merge request
     * @return list of file paths
     */
    public List<String> fetchChangedFiles(Long projectId, Long mergeRequestIid) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return Collections.emptyList();
        }
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .pathSegment("api", "v4", "projects", projectId.toString(), "merge_requests", mergeRequestIid.toString(), "changes")
                .toUriString();
        HttpHeaders headers = new HttpHeaders();
        headers.set("PRIVATE-TOKEN", apiToken);
        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
            Map body = response.getBody();
            if (body != null) {
                List<Map> changes = (List<Map>) body.get("changes");
                if (changes != null) {
                    return changes.stream()
                            .map(c -> (String) c.get("new_path"))
                            .toList();
                }
            }
        } catch (Exception ex) {
            log.warn("Failed to fetch changed files for MR {}: {}", mergeRequestIid, ex.getMessage());
        }
        return Collections.emptyList();
    }
}