package com.todo.eod.app;

import com.todo.eod.domain.*;
import com.todo.eod.infra.repo.EvidenceRepository;
import com.todo.eod.infra.repo.TaskRepository;
import com.todo.eod.infra.repo.WebhookInboxRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

public class WebhookIngestServiceTest {

    @Test
    void ignores_duplicate_eventId() {
        var inbox = mock(WebhookInboxRepository.class);
        var tasks = mock(TaskRepository.class);
        var evidence = mock(EvidenceRepository.class);
        var eval = mock(EvaluationService.class);
        when(inbox.findById("evt-1")).thenReturn(Optional.of(new WebhookInbox()));
        var svc = new WebhookIngestService(inbox, tasks, evidence, eval);
        var res = svc.ingest("evt-1", "PR_MERGED", "TSK-1", new Object());
        assertTrue(!res.accepted());
    }

    @Test
    void transitions_to_done_when_evaluation_complete_and_persists_task() {
        var inbox = mock(WebhookInboxRepository.class);
        var tasks = mock(TaskRepository.class);
        var evidence = mock(EvidenceRepository.class);
        var eval = mock(EvaluationService.class);

        when(inbox.findById("evt-2")).thenReturn(java.util.Optional.empty());
        Task task = Task.builder().key("TSK-2").state(TaskState.REVIEW).build();
        when(tasks.findByKey("TSK-2")).thenReturn(java.util.Optional.of(task));
        when(eval.evaluate(any())).thenReturn(new EvaluationService.EvalResult(java.util.List.of(), true));

        var svc = new WebhookIngestService(inbox, tasks, evidence, eval);
        var payload = new Object();
        var res = svc.ingest("evt-2", "FLAG_ENABLED", "TSK-2", payload);

        assertTrue(res.accepted());
        verify(tasks, times(1)).save(any(Task.class));
    }
}
