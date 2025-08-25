package com.aireviewer.agent;

import com.aireviewer.model.AIReviewComment;
import com.aireviewer.model.JiraContext;
import com.aireviewer.model.MergeRequestContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CodeAgentTest {

    @Test
    void flagsLargeDiffAndUtilFiles() {
        CodeAgent agent = new CodeAgent();
        // Create diff with 301 lines
        StringBuilder diff = new StringBuilder();
        for (int i = 0; i < 301; i++) diff.append("line\n");
        MergeRequestContext ctx = new MergeRequestContext(1L, 1L, "a", "t", "d", List.of("src/UtilHelper.java"), diff.toString());
        AIReviewComment comment = new AIReviewComment();
        agent.analyse(ctx, new JiraContext(null, null, null, null, List.of()), comment);
        assertFalse(comment.getIssues().isEmpty());
    }
}
