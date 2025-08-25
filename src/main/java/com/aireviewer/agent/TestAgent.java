package com.aireviewer.agent;

import com.aireviewer.i18n.Messages;
import com.aireviewer.model.AIReviewComment;
import com.aireviewer.model.JiraContext;
import com.aireviewer.model.MergeRequestContext;

import java.util.List;

/*
  The TestAgent suggests where additional unit tests may be needed. In this
  simplified version it looks at file names and diff size to infer the need
  for tests. Real implementations would parse the AST and identify new public
  methods or complex logic requiring coverage.
 */
import org.springframework.stereotype.Component;

@Component
public class TestAgent implements Agent {
    @Override
    public void analyse(MergeRequestContext mrContext, JiraContext jiraContext, AIReviewComment comment) {
        List<String> files = mrContext.getChangedFiles();
        if (files != null) {
            for (String file : files) {
                // Suggest tests for new or modified service classes
                if (file.toLowerCase().contains("service")) {
                    comment.addTestAdvice(Messages.get("test.advice.service", file));
                }
                // Suggest tests for controller changes
                if (file.toLowerCase().contains("controller")) {
                    comment.addTestAdvice(Messages.get("test.advice.controller", file));
                }
            }
        }
        // Additionally, if no advice has been added yet and diff is long, suggest generic testing
        if (comment.getTestAdvice().isEmpty() && mrContext.getDiff() != null) {
            int lines = mrContext.getDiff().split("\n").length;
            if (lines > 50) {
                comment.addTestAdvice(Messages.get("test.advice.longDiff"));
            }
        }
    }
}