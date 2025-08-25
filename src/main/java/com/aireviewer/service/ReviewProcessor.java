package com.aireviewer.service;

import com.aireviewer.client.GitLabClient;
import com.aireviewer.client.JiraClient;
import com.aireviewer.model.AIReviewComment;
import com.aireviewer.model.JiraContext;
import com.aireviewer.model.MergeRequestContext;
import com.aireviewer.notify.Notifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The ReviewProcessor orchestrates the end‑to‑end workflow whenever a merge
 * request webhook event is received. It extracts the necessary data from
 * the payload, retrieves additional context from Jira and GitLab, invokes
 * the aggregator service and finally posts the resulting comment back to
 * GitLab. Errors are logged but do not throw exceptions to avoid blocking
 * CI/CD pipelines.
 */
@Service
public class ReviewProcessor {
    private static final Logger log = LoggerFactory.getLogger(ReviewProcessor.class);

    private final AggregatorService aggregatorService;
    private final JiraClient jiraClient;
    private final GitLabClient gitLabClient;
    private final Notifier notifier;

    private static final Pattern JIRA_KEY_PATTERN = Pattern.compile("[A-Z][A-Z0-9]+-\\d+");

    public ReviewProcessor(AggregatorService aggregatorService, JiraClient jiraClient, GitLabClient gitLabClient, Notifier notifier) {
        this.aggregatorService = aggregatorService;
        this.jiraClient = jiraClient;
        this.gitLabClient = gitLabClient;
        this.notifier = notifier;
    }

    /**
     * Entry point for processing merge request events. The payload shape is
     * defined by GitLab's webhook documentation. Only a subset of fields are
     * consumed here. Any errors encountered during processing are logged and
     * will not throw an exception to the caller.
     *
     * @param payload the raw webhook payload deserialised into a map
     */
    public void handleMergeRequestEvent(Map<String, Object> payload) {
        try {
            log.info("Received merge request event");
            // Extract basic merge request attributes
            Map<String, Object> objectAttributes = (Map<String, Object>) payload.get("object_attributes");
            Map<String, Object> project = (Map<String, Object>) payload.get("project");
            Map<String, Object> user = (Map<String, Object>) payload.get("user");
            if (objectAttributes == null) {
                log.warn("No object_attributes found in webhook payload");
                return;
            }
            Long projectId = null;
            if (project != null && project.get("id") != null) {
                projectId = ((Number) project.get("id")).longValue();
            } else if (objectAttributes.get("target_project_id") != null) {
                projectId = ((Number) objectAttributes.get("target_project_id")).longValue();
            }
            Long iid = objectAttributes.get("iid") != null ? ((Number) objectAttributes.get("iid")).longValue() : null;
            String title = (String) objectAttributes.get("title");
            String description = (String) objectAttributes.get("description");
            String author = user != null ? (String) user.get("name") : null;
            // Determine Jira key from title if present
            String jiraKey = null;
            if (title != null) {
                Matcher m = JIRA_KEY_PATTERN.matcher(title);
                if (m.find()) {
                    jiraKey = m.group();
                }
            }
            JiraContext jiraContext = jiraClient.fetchIssue(jiraKey);
            // Retrieve changed files from GitLab (may be empty)
            List<String> changedFiles = Collections.emptyList();
            if (projectId != null && iid != null) {
                changedFiles = gitLabClient.fetchChangedFiles(projectId, iid);
            }
            // For MVP we set diff equal to joined file names. In real implementation you'd fetch the diff.
            String diff = String.join("\n", changedFiles);
            MergeRequestContext mrContext = new MergeRequestContext(projectId, iid, author, title, description, changedFiles, diff);
            // Run agents
            AIReviewComment comment = aggregatorService.review(mrContext, jiraContext);
            // Post comment back to GitLab if possible
            if (projectId != null && iid != null) {
                gitLabClient.postMergeRequestComment(projectId, iid, comment.toMarkdown());
            } else {
                log.warn("Missing projectId or iid; skipping posting comment");
            }
        } catch (Exception ex) {
            // Catch all exceptions to prevent pipeline failures
            log.error("Error processing merge request event: {}", ex.getMessage(), ex);
            try {
                Long projectId = null;
                Long iid = null;
                Object oa = payload.get("object_attributes");
                if (oa instanceof Map<?, ?> oaMap) {
                    Object pid = ((Map<?, ?>) oaMap).get("target_project_id");
                    if (pid instanceof Number n) projectId = n.longValue();
                    Object iidObj = ((Map<?, ?>) oaMap).get("iid");
                    if (iidObj instanceof Number n2) iid = n2.longValue();
                }
                String subject = "AI-Reviewer failure";
                String body = String.format("Review failed. MR: projectId=%s, iid=%s. Reason: %s",
                        String.valueOf(projectId), String.valueOf(iid), ex.getMessage());
                if (notifier != null) {
                    notifier.notifyAdmin(subject, body);
                }
            } catch (Exception notifyEx) {
                log.error("Failed to send admin notification: {}", notifyEx.getMessage(), notifyEx);
            }
        }
    }
}