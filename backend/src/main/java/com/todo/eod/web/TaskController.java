package com.todo.eod.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.todo.eod.app.EvaluationService;
import com.todo.eod.app.TaskService;
import com.todo.eod.domain.Task;
import com.todo.eod.domain.TaskState;
import com.todo.eod.infra.repo.TaskRepository;
import com.todo.eod.web.dto.TaskCreateRequest;
import com.todo.eod.web.dto.TaskResponse;
import com.todo.eod.web.dto.TaskStatePatchRequest;
import com.todo.eod.web.mapper.TaskMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final TaskRepository taskRepository;
    private final TaskMapper mapper;
    private final EvaluationService evaluationService;
    private final ObjectMapper om = new ObjectMapper();

    @PostMapping
    public ResponseEntity<TaskResponse> create(@Valid @RequestBody TaskCreateRequest req) {
        UUID policyId = UUID.fromString(req.getDodPolicyId());
        Task t = taskService.create(req.getKey(), req.getTitle(), policyId, req.getAssignee(), req.getLabels(), req.getCorrelationId());
        TaskResponse resp = mapper.toResponse(t);
        resp.setDod(TaskResponse.DodSummary.builder()
                .policyId(t.getDodPolicy().getId().toString())
                .progress(List.of())
                .complete(false)
                .build());
        return ResponseEntity.created(URI.create("/tasks/" + t.getId())).body(resp);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskResponse> get(@PathVariable UUID id) {
        return taskRepository.findById(id)
                .map(t -> {
                    var r = mapper.toResponse(t);
                    var eval = evaluationService.evaluate(t);
                    r.setDod(TaskResponse.DodSummary.builder()
                            .policyId(t.getDodPolicy().getId().toString())
                            .progress(eval.progress().stream().map(p -> TaskResponse.DodSummary.Progress.builder()
                                    .req(p.req()).ok(p.ok()).details(p.details()).build()).toList())
                            .complete(eval.complete())
                            .build());
                    return ResponseEntity.ok(r);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/state")
    public ResponseEntity<TaskResponse> patchState(@PathVariable UUID id, @Valid @RequestBody TaskStatePatchRequest req) {
        return taskRepository.findById(id)
                .map(t -> {
                    TaskState ns = req.getState();
                    Task updated = taskService.changeState(t, ns);
                    var r = mapper.toResponse(updated);
                    var eval = evaluationService.evaluate(updated);
                    r.setDod(TaskResponse.DodSummary.builder()
                            .policyId(updated.getDodPolicy().getId().toString())
                            .progress(eval.progress().stream().map(p -> TaskResponse.DodSummary.Progress.builder()
                                    .req(p.req()).ok(p.ok()).details(p.details()).build()).toList())
                            .complete(eval.complete())
                            .build());
                    return ResponseEntity.ok(r);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public List<TaskResponse> list(@RequestParam(required = false) TaskState state,
                                   @RequestParam(required = false) String assignee,
                                   @RequestParam(required = false) String label) {
        return taskRepository.search(state, assignee, label).stream()
                .map(mapper::toResponse)
                .toList();
    }
}
