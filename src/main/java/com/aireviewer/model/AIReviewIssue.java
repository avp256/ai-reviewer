package com.aireviewer.model;

/**
 * Represents a single issue detected by one of the agents. Each issue has a description,
 * a recommended fix and the name of the agent that reported it. This structure
 * allows the Aggregator to provide transparent provenance for the user.
 */
public class AIReviewIssue {
    private final String description;
    private final String recommendation;
    private final String source;

    public AIReviewIssue(String description, String recommendation, String source) {
        this.description = description;
        this.recommendation = recommendation;
        this.source = source;
    }

    public String getDescription() {
        return description;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public String getSource() {
        return source;
    }
}