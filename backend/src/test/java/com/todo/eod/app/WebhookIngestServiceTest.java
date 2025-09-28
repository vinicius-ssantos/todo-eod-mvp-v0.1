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
}
