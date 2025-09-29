package com.todo.eod.app;

import com.todo.eod.domain.*;
import com.todo.eod.infra.repo.EvidenceRepository;
import com.todo.eod.infra.repo.TaskRepository;
import com.todo.eod.infra.repo.WebhookInboxRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WebhookIngestServiceMoreTest {

    @Test
    void sets_verification_when_in_review_and_pr_merged() {
        var inbox = mock(WebhookInboxRepository.class);
        var tasks = mock(TaskRepository.class);
        var evidence = mock(EvidenceRepository.class);
        var eval = mock(EvaluationService.class);

        when(inbox.findById("evt-3")).thenReturn(Optional.empty());
        Task task = Task.builder().key("TSK-3").state(TaskState.REVIEW).build();
        when(tasks.findByKey("TSK-3")).thenReturn(Optional.of(task));
        when(eval.evaluate(any())).thenReturn(new EvaluationService.EvalResult(java.util.List.of(), false));

        var svc = new WebhookIngestService(inbox, tasks, evidence, eval);
        var res = svc.ingest("evt-3", "PR_MERGED", "TSK-3", new Object());

        assertTrue(res.accepted());
        assertEquals(TaskState.VERIFICATION, task.getState());
    }

    @Test
    void returns_error_when_task_missing() {
        var inbox = mock(WebhookInboxRepository.class);
        var tasks = mock(TaskRepository.class);
        var evidence = mock(EvidenceRepository.class);
        var eval = mock(EvaluationService.class);

        when(inbox.findById("evt-4")).thenReturn(Optional.empty());
        when(tasks.findByKey("TSK-404")).thenReturn(Optional.empty());

        var svc = new WebhookIngestService(inbox, tasks, evidence, eval);
        var res = svc.ingest("evt-4", "PR_MERGED", "TSK-404", new Object());

        assertFalse(res.accepted());
        assertTrue(res.reason().startsWith("task not found"));
    }
}

