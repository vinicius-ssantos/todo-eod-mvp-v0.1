package com.todo.eod.web;

import com.todo.eod.app.EvaluationService;
import com.todo.eod.app.TaskService;
import com.todo.eod.domain.DodPolicy;
import com.todo.eod.domain.Task;
import com.todo.eod.domain.TaskState;
import com.todo.eod.infra.repo.TaskRepository;
import com.todo.eod.web.dto.TaskResponse;
import com.todo.eod.web.mapper.TaskMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TaskController.class)
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(addFilters = false)
class TaskControllerTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    TaskService taskService;

    @MockBean
    TaskRepository taskRepository;

    @MockBean
    TaskMapper mapper;

    @MockBean
    EvaluationService evaluationService;

    @Test
    void create_returns201_and_location_with_basic_body() throws Exception {
        UUID policyId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID taskId = UUID.randomUUID();
        UUID cid = UUID.randomUUID();

        DodPolicy policy = DodPolicy.builder()
                .id(policyId)
                .name("Default")
                .spec("{}")
                .createdAt(OffsetDateTime.now())
                .build();

        Task task = Task.builder()
                .id(taskId)
                .key("TSK-1")
                .title("Implementar /login")
                .state(TaskState.BACKLOG)
                .dodPolicy(policy)
                .assignee("vinicius")
                .labels(List.of("security"))
                .correlationId(cid)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        when(taskService.create(eq("TSK-1"), eq("Implementar /login"), eq(policyId), eq("vinicius"), anyList(), any(UUID.class)))
                .thenReturn(task);

        TaskResponse mapped = TaskResponse.builder()
                .id(taskId.toString())
                .key("TSK-1")
                .title("Implementar /login")
                .state(TaskState.BACKLOG)
                .dodPolicyId(policyId.toString())
                .assignee("vinicius")
                .labels(List.of("security"))
                .correlationId(cid)
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
        when(mapper.toResponse(task)).thenReturn(mapped);

        String body = "{" +
                "\"key\":\"TSK-1\"," +
                "\"title\":\"Implementar /login\"," +
                "\"dodPolicyId\":\"" + policyId + "\"," +
                "\"assignee\":\"vinicius\"," +
                "\"labels\":[\"security\"]," +
                "\"correlationId\":\"" + cid + "\"" +
                "}";

        mvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/tasks/" + taskId))
                .andExpect(jsonPath("$.key").value("TSK-1"))
                .andExpect(jsonPath("$.state").value("BACKLOG"));
    }
}
