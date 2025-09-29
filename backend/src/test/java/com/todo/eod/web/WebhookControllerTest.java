package com.todo.eod.web;

import com.todo.eod.app.WebhookIngestService;
import org.junit.jupiter.api.Test;
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
class WebhookControllerTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    WebhookIngestService ingest;

    @Test
    void postGithubWebhook_returns200_whenAccepted() throws Exception {
        when(ingest.ingest(anyString(), anyString(), anyString(), any())).thenReturn(WebhookIngestService.IngestResult.ok());

        String body = "{" +
                "\"eventId\":\"gh-evt-123\"," +
                "\"type\":\"PR_MERGED\"," +
                "\"repo\":\"org/app\"," +
                "\"branch\":\"main\"," +
                "\"pr\":42," +
                "\"taskKey\":\"TSK-123\"" +
                "}";

        mvc.perform(post("/webhooks/github")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }
}

