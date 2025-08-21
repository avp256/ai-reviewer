package com.aireviewer.service;

import com.aireviewer.agent.Agent;
import com.aireviewer.model.AIReviewComment;
import com.aireviewer.model.JiraContext;
import com.aireviewer.model.MergeRequestContext;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Coordinates the execution of all registered agents. The service is
 * intentionally kept simple: it sequentially invokes each agent to enrich
 * the {@link AIReviewComment}. Agents should be stateless so that they can
 * safely be reused across requests.
 */
@Service
public class AggregatorService {

    private final List<Agent> agents;

    public AggregatorService(List<Agent> agents) {
        this.agents = agents;
    }

    /**
     * Execute the review pipeline. Each agent contributes to the final
     * {@link AIReviewComment}. The returned comment can then be transformed
     * into a Markdown post for GitLab.
     *
     * @param mrContext  merge request context
     * @param jiraContext business context
     * @return aggregated AI review comment
     */
    public AIReviewComment review(MergeRequestContext mrContext, JiraContext jiraContext) {
        AIReviewComment comment = new AIReviewComment();
        for (Agent agent : agents) {
            agent.analyse(mrContext, jiraContext, comment);
        }
        return comment;
    }
}