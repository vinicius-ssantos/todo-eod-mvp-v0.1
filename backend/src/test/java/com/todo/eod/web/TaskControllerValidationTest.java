package com.todo.eod.web;

import com.todo.eod.app.EvaluationService;
import com.todo.eod.app.TaskService;
import com.todo.eod.infra.repo.TaskRepository;
import com.todo.eod.web.mapper.TaskMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TaskController.class)
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(addFilters = false)
class TaskControllerValidationTest {

    @Autowired
    MockMvc mvc;

    @MockBean TaskService taskService;
    @MockBean TaskRepository taskRepository;
    @MockBean TaskMapper mapper;
    @MockBean EvaluationService evaluationService;

    @Test
    void create_returns400_when_title_missing() throws Exception {
        String body = "{" +
                "\"key\":\"TSK-1\"," +
                "\"title\":\"\"," +
                "\"dodPolicyId\":\"00000000-0000-0000-0000-000000000001\"," +
                "\"correlationId\":\"00000000-0000-0000-0000-000000000000\"" +
                "}";
        mvc.perform(post("/tasks").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }
}
