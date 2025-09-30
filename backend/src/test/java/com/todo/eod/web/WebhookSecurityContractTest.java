package com.todo.eod.web;

import com.todo.eod.app.WebhookIngestService;
import com.todo.eod.web.dto.WebhookPayload;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WebhookController.class)
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(addFilters = false)
class WebhookSecurityContractTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    WebhookIngestService ingest;

    @MockBean
    WebhookSecurity webhookSecurity;

    @MockBean
    WebhookNormalizer webhookNormalizer;

    @MockBean
    com.todo.eod.infra.ratelimit.RateLimiterService rateLimiter;

    @MockBean
    com.todo.eod.infra.idem.IdempotencyService idempotencyService;

    @BeforeEach
    void setupRateAndIdem() {
        org.mockito.Mockito.when(rateLimiter.allow(org.mockito.ArgumentMatchers.anyString())).thenReturn(true);
        org.mockito.Mockito.when(idempotencyService.isFirstProcessing(org.mockito.ArgumentMatchers.anyString())).thenReturn(true);
    }

    @Test
    void github_signed_pr_merged_returns200() throws Exception {
        when(webhookSecurity.verifyGitHub(anyMap(), anyString())).thenReturn(true);
        when(rateLimiter.allow(anyString())).thenReturn(true);
        when(idempotencyService.isFirstProcessing(anyString())).thenReturn(true);
        WebhookPayload p = new WebhookPayload();
        p.setEventId("gh-delivery-1");
        p.setType("PR_MERGED");
        p.setTaskKey("TSK-1");
        when(webhookNormalizer.normalizeGitHub(anyString(), anyString(), any(), any(), any())).thenReturn(p);
        when(ingest.ingest(anyString(), anyString(), anyString(), any())).thenReturn(WebhookIngestService.IngestResult.ok());

        mvc.perform(post("/webhooks/github")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-GitHub-Event", "pull_request")
                        .header("X-GitHub-Delivery", "gh-delivery-1")
                        .header("X-Hub-Signature-256", "sha256=deadbeef")
                        .content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    void github_invalid_signature_returns401() throws Exception {
        when(webhookSecurity.verifyGitHub(anyMap(), anyString())).thenReturn(false);

        mvc.perform(post("/webhooks/github")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-GitHub-Event", "pull_request")
                        .header("X-GitHub-Delivery", "gh-delivery-2")
                        .header("X-Hub-Signature-256", "sha256=badsig")
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void gitlab_signed_pipeline_success_returns200() throws Exception {
        when(webhookSecurity.verifyGitLab(anyMap(), anyString())).thenReturn(true);
        when(rateLimiter.allow(anyString())).thenReturn(true);
        when(idempotencyService.isFirstProcessing(anyString())).thenReturn(true);
        WebhookPayload p = new WebhookPayload();
        p.setEventId("gl-evt-1");
        p.setType("CI_GREEN");
        p.setTaskKey("TSK-2");
        when(webhookNormalizer.normalizeGitLab(anyString(), anyString(), any(), any(), any())).thenReturn(p);
        when(ingest.ingest(anyString(), anyString(), anyString(), any())).thenReturn(WebhookIngestService.IngestResult.ok());

        String body = "{" +
                "\"object_kind\":\"pipeline\"," +
                "\"project\":{\"path_with_namespace\":\"org/app\"}," +
                "\"object_attributes\":{\"status\":\"success\"}" +
                "}";

        mvc.perform(post("/webhooks/gitlab")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Gitlab-Event", "Pipeline Hook")
                        .header("X-Gitlab-Event-UUID", "gl-evt-1")
                        .header("X-Gitlab-Signature", "somesig")
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    void gitlab_invalid_signature_returns401() throws Exception {
        when(webhookSecurity.verifyGitLab(anyMap(), anyString())).thenReturn(false);

        mvc.perform(post("/webhooks/gitlab")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Gitlab-Event", "Pipeline Hook")
                        .header("X-Gitlab-Signature", "badsig")
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void github_signed_invalid_json_returns400() throws Exception {
        when(webhookSecurity.verifyGitHub(anyMap(), anyString())).thenReturn(true);

        mvc.perform(post("/webhooks/github")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-GitHub-Event", "workflow_run")
                        .header("X-GitHub-Delivery", "gh-delivery-3")
                        .header("X-Hub-Signature-256", "sha256=sig")
                        .content("not-json"))
                .andExpect(status().isBadRequest());
    }
}
