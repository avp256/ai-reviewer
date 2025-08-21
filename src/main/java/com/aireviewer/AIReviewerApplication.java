package com.aireviewer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the AI‑Reviewer Spring Boot application. Running the main
 * method will start an embedded web server listening for GitLab webhook
 * events and expose a health-check endpoint.
 */
@SpringBootApplication
public class AIReviewerApplication {
    public static void main(String[] args) {
        SpringApplication.run(AIReviewerApplication.class, args);
    }
}