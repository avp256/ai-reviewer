package com.aireviewer.client;

import com.aireviewer.model.JiraContext;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ClientsEmptyConfigTest {

    @Test
    void jiraClient_returnsEmptyContext_whenBaseUrlMissing() {
        JiraClient client = new JiraClient();
        ReflectionTestUtils.setField(client, "baseUrl", "");
        ReflectionTestUtils.setField(client, "username", "user");
        ReflectionTestUtils.setField(client, "apiToken", "token");
        JiraContext ctx = client.fetchIssue("KEY-1");
        assertNotNull(ctx);
        assertEquals("KEY-1", ctx.getKey());
        assertNull(ctx.getSummary());
    }

    @Test
    void gitlabClient_returnsEmptyList_whenBaseUrlMissing() {
        GitLabClient client = new GitLabClient();
        ReflectionTestUtils.setField(client, "baseUrl", "");
        ReflectionTestUtils.setField(client, "apiToken", "token");
        List<String> files = client.fetchChangedFiles(1L, 1L);
        assertNotNull(files);
        assertTrue(files.isEmpty());
        // Should not throw when posting comment
        client.postMergeRequestComment(1L, 1L, "body");
    }
}
