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
            // Validate event type and action
            Object kind = payload.get("object_kind");
            if (!(kind instanceof String) || !"merge_request".equals(kind)) {
                log.info("Ignoring event: object_kind={}", kind);
                return;
            }
            Object oaRaw = payload.get("object_attributes");
            if (!(oaRaw instanceof Map<?,?>)) {
                log.warn("No object_attributes found or wrong type in webhook payload");
                return;
            }
            Map<?,?> oa = (Map<?,?>) oaRaw;
            String action = null;
            Object act = oa.get("action");
            if (act instanceof String s) action = s;
            if (action == null || !(action.equals("open") || action.equals("update"))) {
                log.info("Ignoring MR action: {}", action);
                return;
            }

            // Extract basic merge request attributes with null-safety
            Map<String, Object> project = null;
            Object prj = payload.get("project");
            if (prj instanceof Map<?,?> pmap) {
                //noinspection unchecked
                project = (Map<String, Object>) pmap;
            }
            Map<String, Object> user = null;
            Object usr = payload.get("user");
            if (usr instanceof Map<?,?> umap) {
                //noinspection unchecked
                user = (Map<String, Object>) umap;
            }

            Long projectId = null;
            Object pid1 = project != null ? project.get("id") : null;
            if (pid1 instanceof Number n1) {
                projectId = n1.longValue();
            } else {
                Object pid2 = oa.get("target_project_id");
                if (pid2 instanceof Number n2) projectId = n2.longValue();
            }
            Long iid = null;
            Object iidObj = oa.get("iid");
            if (iidObj instanceof Number n) iid = n.longValue();
            String title = null;
            Object t = oa.get("title");
            if (t instanceof String s) title = s;
            String description = null;
            Object d = oa.get("description");
            if (d instanceof String s) description = s;
            String author = null;
            if (user != null) {
                Object nm = user.get("name");
                if (nm instanceof String s) author = s;
            }
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
                log.info("Posted AI-Reviewer comment to MR projectId={}, iid={}", projectId, iid);
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
                    Object iidObj2 = ((Map<?, ?>) oaMap).get("iid");
                    if (iidObj2 instanceof Number n2) iid = n2.longValue();
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