package com.aireviewer.agent;

import com.aireviewer.i18n.Messages;
import com.aireviewer.model.AIReviewComment;
import com.aireviewer.model.AIReviewIssue;
import com.aireviewer.model.JiraContext;
import com.aireviewer.model.MergeRequestContext;

import java.util.List;

/*
  The ArchitectureAgent inspects the merge request for high‑level architectural
  concerns. In a production system, this would consult documentation like Tech
  Radar and microservice guidelines. Here, we implement a few trivial checks
  such as discouraging the use of outdated libraries based on file names.
 */
import org.springframework.stereotype.Component;

@Component
public class ArchitectureAgent implements Agent {
    @Override
    public void analyse(MergeRequestContext mrContext, JiraContext jiraContext, AIReviewComment comment) {
        List<String> files = mrContext.getChangedFiles();
        if (files != null) {
            for (String file : files) {
                String lower = file.toLowerCase();
                // Warn if a use of a technology flagged as "deprecated" appears in file names
                if (lower.contains("legacy") || lower.contains("deprecated")) {
                    comment.addIssue(new AIReviewIssue(
                            Messages.get("arch.deprecated.title", file),
                            Messages.get("arch.deprecated.action"),
                            Messages.get("agent.architecture")));
                }
            }
        }
    }
}