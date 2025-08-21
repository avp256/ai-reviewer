package com.aireviewer.controller;

import com.aireviewer.service.ReviewProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller that exposes endpoints for receiving webhooks from GitLab and
 * exposing health information. GitLab will send merge request events to the
 * configured webhook URL. The payload is forwarded to the {@link ReviewProcessor}.
 */
@RestController
public class GitLabWebhookController {
    private static final Logger log = LoggerFactory.getLogger(GitLabWebhookController.class);
    private final ReviewProcessor reviewProcessor;

    public GitLabWebhookController(ReviewProcessor reviewProcessor) {
        this.reviewProcessor = reviewProcessor;
    }

    /**
     * Endpoint to handle GitLab webhook events for merge requests. This method
     * accepts any JSON body and delegates processing to the {@link ReviewProcessor}.
     *
     * @param payload the webhook payload
     * @return simple response indicating reception
     */
    @PostMapping(path = "/webhook/gitlab")
    public ResponseEntity<String> handleWebhook(@RequestBody Map<String, Object> payload) {
        log.info("Webhook received");
        reviewProcessor.handleMergeRequestEvent(payload);
        return ResponseEntity.ok("Webhook processed");
    }

    /**
     * Health-check endpoint that can be used by monitoring tools or load balancers
     * to verify that the application is up and running.
     *
     * @return simple OK response
     */
    @GetMapping(path = "/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}