package com.aireviewer.agent;

import com.aireviewer.model.AIReviewComment;
import com.aireviewer.model.JiraContext;
import com.aireviewer.model.MergeRequestContext;

/**
 * Marker interface for all agents participating in the review pipeline.
 *
 * Each agent is responsible for analysing a specific aspect of the merge request
 * and mutating the {@link AIReviewComment} accordingly. Agents should be
 * stateless and thread-safe.
 */
@FunctionalInterface
public interface Agent {
    /**
     * Analyse the merge request in conjunction with the Jira context and mutate
     * the provided review comment. Agents should not block for long periods of
     * time or perform network calls that could significantly delay the
     * pipeline.
     *
     * @param mrContext  the merge request information
     * @param jiraContext the business context from Jira
     * @param comment the in-progress review comment to modify
     */
    void analyse(MergeRequestContext mrContext, JiraContext jiraContext, AIReviewComment comment);
}