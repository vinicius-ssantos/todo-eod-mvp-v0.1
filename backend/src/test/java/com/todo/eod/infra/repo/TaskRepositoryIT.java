package com.todo.eod.infra.repo;

import com.todo.eod.domain.DodPolicy;
import com.todo.eod.domain.Task;
import com.todo.eod.domain.TaskState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestInstance(Lifecycle.PER_CLASS)
class TaskRepositoryIT {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void dbProps(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    TaskRepository taskRepository;

    @Autowired
    DodPolicyRepository policyRepository;

    @Test
    void search_filters_by_state_assignee_and_label() {
        var policy = policyRepository.save(DodPolicy.builder()
                .id(UUID.randomUUID())
                .name("test")
                .spec("{}")
                .createdAt(OffsetDateTime.now())
                .build());

        var t1 = Task.builder()
                .key("TSK-A")
                .title("A")
                .state(TaskState.BACKLOG)
                .dodPolicy(policy)
                .assignee("alice")
                .labels(List.of("security", "observability"))
                .correlationId(UUID.randomUUID())
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        var t2 = Task.builder()
                .key("TSK-B")
                .title("B")
                .state(TaskState.REVIEW)
                .dodPolicy(policy)
                .assignee("bob")
                .labels(List.of("infra"))
                .correlationId(UUID.randomUUID())
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        taskRepository.saveAll(List.of(t1, t2));

        var byState = taskRepository.search(TaskState.BACKLOG, null, null);
        assertEquals(1, byState.size());

        var byAssignee = taskRepository.search(null, "bob", null);
        assertEquals(1, byAssignee.size());

        var byLabel = taskRepository.search(null, null, "security");
        assertEquals(1, byLabel.size());

        var combined = taskRepository.search(TaskState.BACKLOG, "alice", "observability");
        assertEquals(1, combined.size());
    }
}

