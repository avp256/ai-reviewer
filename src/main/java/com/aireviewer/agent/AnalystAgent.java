package com.aireviewer.agent;

import com.aireviewer.i18n.Messages;
import com.aireviewer.model.AIReviewComment;
import com.aireviewer.model.JiraContext;
import com.aireviewer.model.MergeRequestContext;

/*
  The AnalystAgent is responsible for synthesising the Jira issue into a concise
  summary for the other agents. In this simple MVP implementation it only
  attaches the provided JiraContext to the review comment. A more advanced
  implementation could pre-process the description and comments to highlight
  key requirements or risks.
 */
import org.springframework.stereotype.Component;

@Component
public class AnalystAgent implements Agent {
    @Override
    public void analyse(MergeRequestContext mrContext, JiraContext jiraContext, AIReviewComment comment) {
        // For the MVP we simply propagate the Jira context into the review.
        comment.setJiraContext(jiraContext);
        // Add a generic positive remark based on the presence of a Jira issue.
        if (jiraContext != null && jiraContext.getSummary() != null) {
            comment.setDoneWell(Messages.get("analyst.doneWell.withJira", jiraContext.getSummary()));
        } else {
            comment.setDoneWell(Messages.get("analyst.doneWell.noJira"));
        }
    }
}