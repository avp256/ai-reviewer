package com.aireviewer.service;

import com.aireviewer.agent.Agent;
import com.aireviewer.model.AIReviewComment;
import com.aireviewer.model.JiraContext;
import com.aireviewer.model.MergeRequestContext;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link AggregatorService}. These tests verify that
 * registered agents are invoked and that they can mutate the review comment.
 */
public class AggregatorServiceTest {
    @Test
    public void testAggregatorInvokesAgents() {
        List<String> order = new ArrayList<>();
        Agent first = (mr, jira, comment) -> order.add("first");
        Agent second = (mr, jira, comment) -> order.add("second");
        AggregatorService aggregator = new AggregatorService(List.of(first, second));
        AIReviewComment comment = aggregator.review(new MergeRequestContext(1L, 1L, "author", "title", "desc", List.of(), ""), new JiraContext("KEY", "summary", "desc", "Bug", List.of()));
        assertEquals(List.of("first", "second"), order, "Agents should be invoked in the order registered");
    }

    @Test
    public void testAggregatorReturnsNonNullComment() {
        AggregatorService aggregator = new AggregatorService(List.of());
        AIReviewComment comment = aggregator.review(new MergeRequestContext(1L, 1L, "author", "title", "desc", List.of(), ""), null);
        assertNotNull(comment, "Aggregator should return a comment even with no agents");
    }
}