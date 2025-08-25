package com.aireviewer.e2e;

import com.aireviewer.AIReviewerApplication;
import com.aireviewer.client.GitLabClient;
import com.aireviewer.client.JiraClient;
import com.aireviewer.model.JiraContext;
import com.aireviewer.notify.Notifier;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Smoke/E2E test: webhook → ReviewProcessor → GitLabClient.postMergeRequestComment
 * All external clients are mocked to avoid network calls. Verifies the full
 * controller pipeline executes and produces a markdown comment.
 */
@SpringBootTest(classes = AIReviewerApplication.class)
@AutoConfigureMockMvc
public class WebhookSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JiraClient jiraClient;

    @MockBean
    private GitLabClient gitLabClient;

    @MockBean
    private Notifier notifier;

    @Test
    void webhook_end_to_end_posts_comment() throws Exception {
        // Arrange mocks
        when(jiraClient.fetchIssue("ABC-123"))
                .thenReturn(new JiraContext("ABC-123", "Summary", null, null, List.of()));
        when(gitLabClient.fetchChangedFiles(101L, 7L))
                .thenReturn(List.of("src/A.java", "src/B.java"));

        String payload = "{" +
                "\"object_kind\":\"merge_request\"," +
                "\"project\":{\"id\":101}," +
                "\"user\":{\"name\":\"Author\"}," +
                "\"object_attributes\":{\"action\":\"open\",\"iid\":7,\"title\":\"ABC-123 Fix bug\"}" +
                "}";

        // Act
        mockMvc.perform(post("/webhook/gitlab")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        // Assert that comment was posted with markdown
        ArgumentCaptor<String> markdownCaptor = ArgumentCaptor.forClass(String.class);
        verify(gitLabClient, times(1))
                .postMergeRequestComment(eq(101L), eq(7L), markdownCaptor.capture());
        String md = markdownCaptor.getValue();
        assertThat(md).isNotNull();
        assertThat(md).startsWith("[AI-Reviewer | Summary]");
    }
}
