package com.aireviewer.model;

import java.util.List;

/**
 * Represents a simplified view of a GitLab merge request for use within the agent pipeline.
 *
 * This context contains only the information relevant for analysis. In a real implementation
 * you would enrich this class with full diff information and other metadata pulled from
 * GitLab. For the purposes of the MVP the fields below are sufficient.
 */
public class MergeRequestContext {

    private final Long projectId;
    private final Long mergeRequestIid;
    private final String author;
    private final String title;
    private final String description;
    private final List<String> changedFiles;
    private final String diff;

    public MergeRequestContext(Long projectId, Long mergeRequestIid, String author,
                               String title, String description,
                               List<String> changedFiles, String diff) {
        this.projectId = projectId;
        this.mergeRequestIid = mergeRequestIid;
        this.author = author;
        this.title = title;
        this.description = description;
        this.changedFiles = changedFiles;
        this.diff = diff;
    }

    public Long getProjectId() {
        return projectId;
    }

    public Long getMergeRequestIid() {
        return mergeRequestIid;
    }

    public String getAuthor() {
        return author;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getChangedFiles() {
        return changedFiles;
    }

    public String getDiff() {
        return diff;
    }
}