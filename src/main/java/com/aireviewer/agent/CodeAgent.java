package com.aireviewer.agent;

import com.aireviewer.model.AIReviewComment;
import com.aireviewer.model.AIReviewIssue;
import com.aireviewer.model.JiraContext;
import com.aireviewer.model.MergeRequestContext;

import java.util.List;

/**
 * CodeAgent performs static heuristics on the changed code. It checks for simple patterns
 * such as long methods or classes that may benefit from refactoring. For the
 * MVP we implement a few naive checks based on filenames and diff length.
 */
import org.springframework.stereotype.Component;

@Component
public class CodeAgent implements Agent {
    @Override
    public void analyse(MergeRequestContext mrContext, JiraContext jiraContext, AIReviewComment comment) {
        // Very basic heuristics: if the diff is too large or too small we comment accordingly.
        String diff = mrContext.getDiff();
        if (diff != null) {
            int lines = diff.split("\n").length;
            if (lines > 300) {
                comment.addIssue(new AIReviewIssue(
                        "Диф містить понад 300 рядків. Великі зміни важче перевіряти і тестувати.",
                        "Розділіть зміни на кілька менших Merge Request для полегшення ревʼю.",
                        "Code Agent"));
            } else if (lines < 5) {
                comment.addIssue(new AIReviewIssue(
                        "Диф містить дуже мало змін. Це може бути несуттєва зміна (наприклад, форматування).",
                        "Переконайтесь, що MR повʼязаний з Jira і має змістовний опис.",
                        "Code Agent"));
            }
        }
        // Check file names for common anti‑patterns
        List<String> files = mrContext.getChangedFiles();
        if (files != null) {
            files.stream()
                    .filter(f -> f.toLowerCase().contains("util") || f.toLowerCase().contains("helper"))
                    .forEach(f -> comment.addIssue(new AIReviewIssue(
                            "Файл '" + f + "' виглядає як загальний утилітарний клас.",
                            "Переконайтесь, що утилітарні класи не ростуть безконтрольно. Розгляньте застосування принципів SOLID для розділення відповідальності.",
                            "Code Agent")));
        }
    }
}