package com.todo.eod.web;

import com.todo.eod.app.EvaluationService;
import com.todo.eod.app.TaskService;
import com.todo.eod.infra.conf.SecurityConfig;
import com.todo.eod.infra.repo.TaskRepository;
import com.todo.eod.web.mapper.TaskMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TaskController.class)
@Import(SecurityConfig.class)
class TaskSecurityTest {

    @Autowired MockMvc mvc;

    @org.springframework.boot.test.mock.mockito.MockBean TaskService taskService;
    @org.springframework.boot.test.mock.mockito.MockBean TaskRepository taskRepository;
    @org.springframework.boot.test.mock.mockito.MockBean TaskMapper mapper;
    @org.springframework.boot.test.mock.mockito.MockBean EvaluationService evaluationService;

    private String validBody() {
        return "{" +
                "\"key\":\"TSK-SEC-2\"," +
                "\"title\":\"Criar\"," +
                "\"dodPolicyId\":\"00000000-0000-0000-0000-000000000001\"," +
                "\"correlationId\":\"00000000-0000-0000-0000-000000000000\"" +
                "}";
    }

    @Test
    void tasks_without_token_returns401() throws Exception {
        mvc.perform(post("/tasks").contentType(MediaType.APPLICATION_JSON).content(validBody()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tasks_with_wrong_scope_returns403() throws Exception {
        mvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody())
                        .with(jwt().jwt(j -> j.claim("scope", "webhooks:ingest"))))
                .andExpect(status().isForbidden());
    }
}

