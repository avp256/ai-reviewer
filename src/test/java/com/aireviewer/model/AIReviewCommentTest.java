package com.aireviewer.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AIReviewCommentTest {

    @Test
    void toMarkdown_containsRequiredSections() {
        AIReviewComment c = new AIReviewComment();
        c.setDoneWell("Nice structure");
        JiraContext jc = new JiraContext("PRJ-1", "Summary", "Desc", "Bug", List.of("c1", "c2"));
        c.setJiraContext(jc);
        c.addIssue(new AIReviewIssue("Problem A", "Fix A", "Code Agent"));
        c.addTestAdvice("Add unit test for service X");

        String md = c.toMarkdown();
        assertTrue(md.startsWith("[AI-Reviewer | Summary]"));
        assertTrue(md.contains("**Jira Context:**"));
        assertTrue(md.contains("**Зроблено добре:**"));
        assertTrue(md.contains("**Знайдені проблеми:**"));
        assertTrue(md.contains("**Поради по unit-тестам:**"));
        assertTrue(md.contains("**Джерело:**"));
    }
}
