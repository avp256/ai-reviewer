package com.aireviewer.model;

import com.aireviewer.i18n.Messages;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents the combined feedback from all AI agents for a given merge request.
 * The comment follows the format outlined in the specification: includes context
 * from Jira, things done well, detected issues, suggested unit tests and a
 * summary of sources. This model is transformed into Markdown when posted
 * to GitLab.
 */
public class AIReviewComment {
    private JiraContext jiraContext;
    private String doneWell;
    private final List<AIReviewIssue> issues = new ArrayList<>();
    private final List<String> testAdvice = new ArrayList<>();

    public void setJiraContext(JiraContext jiraContext) {
        this.jiraContext = jiraContext;
    }

    public void setDoneWell(String doneWell) {
        this.doneWell = doneWell;
    }

    public void addIssue(AIReviewIssue issue) {
        this.issues.add(issue);
    }

    public void addTestAdvice(String advice) {
        this.testAdvice.add(advice);
    }

    public JiraContext getJiraContext() {
        return jiraContext;
    }

    public String getDoneWell() {
        return doneWell;
    }

    public List<AIReviewIssue> getIssues() {
        return issues;
    }

    public List<String> getTestAdvice() {
        return testAdvice;
    }

    /**
     * Serialises the review comment into a Markdown-formatted string matching
     * the specification. Each section is clearly labelled to improve
     * readability. Only sections with content are included.
     *
     * @return formatted Markdown string
     */
    public String toMarkdown() {
        StringBuilder sb = new StringBuilder();
        sb.append(Messages.get("heading.summary")).append("\n\n");
        if (jiraContext != null) {
            sb.append(Messages.get("section.jira")).append("\n");
            sb.append(Messages.get("jira.key", jiraContext.getKey())).append("\n");
            sb.append(Messages.get("jira.summary", jiraContext.getSummary())).append("\n");
            if (jiraContext.getDescription() != null && !jiraContext.getDescription().isEmpty()) {
                sb.append(Messages.get("jira.description", jiraContext.getDescription())).append("\n");
            }
            if (jiraContext.getComments() != null && !jiraContext.getComments().isEmpty()) {
                sb.append(Messages.get("jira.comments")).append("\n");
                for (String c : jiraContext.getComments()) {
                    sb.append("  - ").append(c).append("\n");
                }
            }
            sb.append("\n");
        }
        if (doneWell != null && !doneWell.isBlank()) {
            sb.append(Messages.get("section.doneWell")).append("\n");
            sb.append(doneWell).append("\n\n");
        }
        if (!issues.isEmpty()) {
            sb.append(Messages.get("section.issues")).append("\n");
            int i = 1;
            for (AIReviewIssue issue : issues) {
                sb.append(i++).append(". ").append(issue.getDescription()).append("\n");
                sb.append(Messages.get("issues.recommendation", issue.getRecommendation())).append("\n");
                sb.append(Messages.get("issues.source", issue.getSource())).append("\n");
            }
            sb.append("\n");
        }
        if (!testAdvice.isEmpty()) {
            sb.append(Messages.get("section.testAdvice")).append("\n");
            int i = 1;
            for (String advice : testAdvice) {
                sb.append(i++).append(". ").append(advice).append("\n");
            }
            sb.append("\n");
        }
        // Global source note (per spec: final 'Джерело' section)
        List<String> uniqueSources = new ArrayList<>();
        for (AIReviewIssue issue : issues) {
            String src = issue.getSource();
            if (src != null && !src.isBlank() && !uniqueSources.contains(src)) {
                uniqueSources.add(src);
            }
        }
        if (!uniqueSources.isEmpty()) {
            sb.append(Messages.get("section.source")).append(" ").append(String.join(", ", uniqueSources)).append("\n");
        }
        return sb.toString();
    }
}