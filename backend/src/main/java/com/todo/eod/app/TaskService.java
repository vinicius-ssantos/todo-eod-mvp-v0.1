package com.todo.eod.app;

import com.todo.eod.domain.*;
import com.todo.eod.infra.repo.DodPolicyRepository;
import com.todo.eod.infra.repo.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final DodPolicyRepository policyRepository;

    @Transactional
    public Task create(String key, String title, UUID policyId, String assignee, java.util.List<String> labels, UUID correlationId) {
        DodPolicy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new IllegalArgumentException("DoD policy not found: " + policyId));
        Task task = Task.builder()
                .key(key)
                .title(title)
                .state(TaskState.BACKLOG)
                .dodPolicy(policy)
                .assignee(assignee)
                .labels(labels)
                .correlationId(correlationId)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        return taskRepository.save(task);
    }

    @Transactional
    public Task changeState(Task task, TaskState newState) {
        task.setState(newState);
        task.setUpdatedAt(OffsetDateTime.now());
        return taskRepository.save(task);
    }
}
