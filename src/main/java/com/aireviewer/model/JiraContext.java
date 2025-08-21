package com.aireviewer.model;

import java.util.List;

/**
 * Encapsulates the relevant information extracted from a Jira issue. Agents rely
 * on this context to incorporate business requirements and stakeholder comments
 * into the review. Only a subset of the fields available from the Jira API
 * are captured here to keep the MVP minimal. Should your needs grow you can
 * extend this class accordingly.
 */
public class JiraContext {
    private final String key;
    private final String summary;
    private final String description;
    private final String issueType;
    private final List<String> comments;

    public JiraContext(String key, String summary, String description,
                       String issueType, List<String> comments) {
        this.key = key;
        this.summary = summary;
        this.description = description;
        this.issueType = issueType;
        this.comments = comments;
    }

    public String getKey() {
        return key;
    }

    public String getSummary() {
        return summary;
    }

    public String getDescription() {
        return description;
    }

    public String getIssueType() {
        return issueType;
    }

    public List<String> getComments() {
        return comments;
    }
}