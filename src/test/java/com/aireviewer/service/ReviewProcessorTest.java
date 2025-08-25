package com.aireviewer.service;

import com.aireviewer.client.GitLabClient;
import com.aireviewer.client.JiraClient;
import com.aireviewer.model.AIReviewComment;
import com.aireviewer.model.JiraContext;
import com.aireviewer.notify.Notifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ReviewProcessorTest {

    private AggregatorService aggregatorService;
    private JiraClient jiraClient;
    private GitLabClient gitLabClient;
    private Notifier notifier;

    private ReviewProcessor reviewProcessor;

    @BeforeEach
    void setup() {
        aggregatorService = mock(AggregatorService.class);
        jiraClient = mock(JiraClient.class);
        gitLabClient = mock(GitLabClient.class);
        notifier = mock(Notifier.class);
        reviewProcessor = new ReviewProcessor(aggregatorService, jiraClient, gitLabClient, notifier);
    }

    @Test
    void processesOpenMergeRequestAndPostsComment() {
        // Arrange
        Map<String, Object> payload = new HashMap<>();
        payload.put("object_kind", "merge_request");
        Map<String, Object> oa = new HashMap<>();
        oa.put("action", "open");
        oa.put("iid", 7);
        oa.put("title", "ABC-123 Fix bug");
        payload.put("object_attributes", oa);
        Map<String, Object> project = new HashMap<>();
        project.put("id", 101);
        payload.put("project", project);
        Map<String, Object> user = new HashMap<>();
        user.put("name", "Author");
        payload.put("user", user);

        when(jiraClient.fetchIssue("ABC-123")).thenReturn(new JiraContext("ABC-123", "Summary", null, null, List.of()));
        when(gitLabClient.fetchChangedFiles(101L, 7L)).thenReturn(List.of("src/A.java", "src/B.java"));
        AIReviewComment comment = new AIReviewComment();
        comment.setDoneWell("Good work");
        when(aggregatorService.review(any(), any())).thenReturn(comment);

        // Act
        reviewProcessor.handleMergeRequestEvent(payload);

        // Assert
        verify(jiraClient).fetchIssue("ABC-123");
        verify(gitLabClient).fetchChangedFiles(101L, 7L);
        ArgumentCaptor<String> markdownCaptor = ArgumentCaptor.forClass(String.class);
        verify(gitLabClient).postMergeRequestComment(eq(101L), eq(7L), markdownCaptor.capture());
        String md = markdownCaptor.getValue();
        assertNotNull(md);
        assertTrue(md.startsWith("[AI-Reviewer | Summary]"));
    }

    @Test
    void ignoresUnsupportedAction() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("object_kind", "merge_request");
        Map<String, Object> oa = new HashMap<>();
        oa.put("action", "merge"); // unsupported per task
        payload.put("object_attributes", oa);

        reviewProcessor.handleMergeRequestEvent(payload);

        verifyNoInteractions(jiraClient, gitLabClient, aggregatorService);
    }
}
