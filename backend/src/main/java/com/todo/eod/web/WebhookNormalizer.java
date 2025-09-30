package com.todo.eod.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.todo.eod.web.dto.WebhookPayload;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class WebhookNormalizer {

    public WebhookPayload normalizeGitHub(String ghEvent, String deliveryId, JsonNode root, String taskKeyHeader, String taskKeyParam) {
        WebhookPayload p = new WebhookPayload();

        String repo = text(root, "repository.full_name");
        p.setRepo(repo);

        if ("pull_request".equalsIgnoreCase(ghEvent)) {
            String action = text(root, "action");
            boolean merged = bool(root, "pull_request.merged");
            if ("closed".equalsIgnoreCase(action) && merged) {
                p.setType("PR_MERGED");
                p.setPr(intVal(root, "number"));
                p.setBranch(text(root, "pull_request.base.ref"));
                p.setSha(text(root, "pull_request.merge_commit_sha"));
                p.setUrl(text(root, "pull_request.html_url"));
            }
        }
        if ("workflow_run".equalsIgnoreCase(ghEvent)) {
            String action = text(root, "action");
            String conclusion = text(root, "workflow_run.conclusion");
            if ("completed".equalsIgnoreCase(action) && "success".equalsIgnoreCase(conclusion)) {
                p.setType("CI_GREEN");
                p.setWorkflow(text(root, "workflow.name", text(root, "workflow_run.name")));
                p.setSha(text(root, "workflow_run.head_sha"));
                p.setBranch(text(root, "workflow_run.head_branch"));
                p.setUrl(text(root, "workflow_run.html_url"));
            }
        }

        // Use GitHub delivery id as eventId when present
        p.setEventId(StringUtils.hasText(deliveryId) ? deliveryId : fallbackEventId(root));

        String tk = firstNonBlank(taskKeyHeader, taskKeyParam, deriveTaskKeyFromGithub(p));
        p.setTaskKey(tk);

        return p;
    }

    public WebhookPayload normalizeGitLab(String glEvent, String eventUuid, JsonNode root, String taskKeyHeader, String taskKeyParam) {
        WebhookPayload p = new WebhookPayload();
        String repo = text(root, "project.path_with_namespace");
        p.setRepo(repo);

        if ("Merge Request Hook".equalsIgnoreCase(glEvent) || "merge_request".equalsIgnoreCase(text(root, "object_kind"))) {
            String state = text(root, "object_attributes.state");
            boolean merged = "merged".equalsIgnoreCase(state);
            if (merged) {
                p.setType("PR_MERGED");
                p.setPr(intVal(root, "object_attributes.iid"));
                p.setBranch(text(root, "object_attributes.target_branch"));
                p.setSha(text(root, "object_attributes.merge_commit_sha"));
                p.setUrl(text(root, "object_attributes.url"));
            }
        }

        if ("Pipeline Hook".equalsIgnoreCase(glEvent) || "pipeline".equalsIgnoreCase(text(root, "object_kind"))) {
            String status = text(root, "object_attributes.status");
            if ("success".equalsIgnoreCase(status)) {
                p.setType("CI_GREEN");
                p.setWorkflow(text(root, "object_attributes.name"));
                p.setSha(text(root, "object_attributes.sha"));
                p.setBranch(text(root, "object_attributes.ref"));
                p.setUrl(text(root, "project.web_url"));
            }
        }

        p.setEventId(StringUtils.hasText(eventUuid) ? eventUuid : fallbackEventId(root));
        String tk = firstNonBlank(taskKeyHeader, taskKeyParam, deriveTaskKeyFromGitlab(p));
        p.setTaskKey(tk);
        return p;
    }

    private String deriveTaskKeyFromGithub(WebhookPayload p) {
        if (p.getRepo() != null && p.getPr() != null) {
            return p.getRepo() + "#" + p.getPr();
        }
        if (p.getRepo() != null && p.getSha() != null && p.getSha().length() >= 7) {
            return p.getRepo() + "@" + p.getSha().substring(0, 7);
        }
        return null;
    }

    private String deriveTaskKeyFromGitlab(WebhookPayload p) {
        return deriveTaskKeyFromGithub(p);
    }

    private String firstNonBlank(String... vals) {
        if (vals == null) return null;
        for (String v : vals) if (StringUtils.hasText(v)) return v;
        return null;
    }

    private String text(JsonNode root, String dottedPath) {
        return text(root, dottedPath, null);
    }

    private String text(JsonNode root, String dottedPath, String def) {
        JsonNode n = node(root, dottedPath);
        if (n != null && !n.isNull()) return n.asText();
        return def;
    }

    private boolean bool(JsonNode root, String dottedPath) {
        JsonNode n = node(root, dottedPath);
        return n != null && n.asBoolean(false);
    }

    private Integer intVal(JsonNode root, String dottedPath) {
        JsonNode n = node(root, dottedPath);
        return (n != null && n.canConvertToInt()) ? n.asInt() : null;
    }

    private JsonNode node(JsonNode root, String dottedPath) {
        if (root == null || dottedPath == null) return null;
        String[] parts = dottedPath.split("\\.");
        JsonNode cur = root;
        for (String p : parts) {
            if (cur == null) return null;
            cur = cur.get(p);
        }
        return cur;
    }

    private String fallbackEventId(JsonNode root) {
        // Attempt to find a known id field; otherwise null
        String[] candidates = new String[]{
                "workflow_run.id", "check_suite.id", "pull_request.id",
                "object_attributes.id", "object_attributes.iid", "event_id"
        };
        for (String c : candidates) {
            String v = text(root, c);
            if (StringUtils.hasText(v)) return v;
        }
        return null;
    }
}

