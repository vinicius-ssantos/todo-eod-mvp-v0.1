package com.todo.eod.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.todo.eod.domain.*;
import com.todo.eod.infra.repo.EvidenceRepository;
import com.todo.eod.infra.repo.TaskRepository;
import com.todo.eod.infra.repo.WebhookInboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WebhookIngestService {

    private final WebhookInboxRepository inboxRepository;
    private final TaskRepository taskRepository;
    private final EvidenceRepository evidenceRepository;
    private final EvaluationService evaluationService;
    private final ObjectMapper om = new ObjectMapper();

    @Transactional
    public IngestResult ingest(String eventId, String type, String taskKey, Object payload) {
        // Idempotency gate
        boolean fresh = inboxRepository.findById(eventId).isEmpty();
        if (!fresh) return IngestResult.ignored("duplicate");

        var fp = fingerprint(payload);
        inboxRepository.save(WebhookInbox.builder()
                .eventId(eventId)
                .fingerprint(fp)
                .receivedAt(OffsetDateTime.now())
                .status("RECEIVED")
                .build());

        Optional<Task> ot = taskRepository.findByKey(taskKey);
        if (ot.isEmpty()) return IngestResult.error("task not found: " + taskKey);
        Task task = ot.get();

        EvidenceType eType = EvidenceType.valueOf(type);
        try {
            String payloadJson = om.writeValueAsString(payload);
            evidenceRepository.save(Evidence.builder()
                    .task(task)
                    .type(eType)
                    .source(sourceFor(eType))
                    .payload(payloadJson)
                    .createdAt(OffsetDateTime.now())
                    .build());
        } catch (Exception e) {
            return IngestResult.error("invalid payload");
        }

        // State transition hints (simple rules for MVP)
        if (task.getState() == TaskState.REVIEW && (eType == EvidenceType.PR_MERGED || eType == EvidenceType.CI_GREEN)) {
            task.setState(TaskState.VERIFICATION);
        }

        var eval = evaluationService.evaluate(task);
        if (eval.complete() && task.getState() != TaskState.DONE) {
            task.setState(TaskState.DONE);
        }
        task.setUpdatedAt(OffsetDateTime.now());
        taskRepository.save(task);

        return IngestResult.ok();
    }

    private String sourceFor(EvidenceType t) {
        return switch (t) {
            case PR_MERGED, CI_GREEN -> "github";
            case DOC_PUBLISHED, LOG_SEEN -> "observability";
            case FLAG_ENABLED -> "flags";
        };
    }

    private String fingerprint(Object payload) {
        try {
            var md = MessageDigest.getInstance("SHA-256");
            var bytes = om.writeValueAsBytes(payload);
            var hash = md.digest(bytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString().substring(0, 64);
        } catch (Exception e) {
            return "na";
        }
    }

    public record IngestResult(boolean accepted, String reason){
        public static IngestResult ok(){ return new IngestResult(true, "ok"); }
        public static IngestResult ignored(String reason){ return new IngestResult(false, reason); }
        public static IngestResult error(String reason){ return new IngestResult(false, reason); }
    }
}
