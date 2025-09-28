package com.todo.eod.web;

import com.todo.eod.app.WebhookIngestService;
import com.todo.eod.web.dto.WebhookPayload;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookIngestService ingest;

    @PostMapping("/webhooks/github")
    public ResponseEntity<?> github(@Valid @RequestBody WebhookPayload body) {
        var res = ingest.ingest(body.getEventId(), body.getType(), body.getTaskKey(), body);
        if (!res.accepted()) return ResponseEntity.accepted().body(res);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/webhooks/observability")
    public ResponseEntity<?> observability(@Valid @RequestBody WebhookPayload body) {
        var res = ingest.ingest(body.getEventId(), body.getType(), body.getTaskKey(), body);
        if (!res.accepted()) return ResponseEntity.accepted().body(res);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/webhooks/flags")
    public ResponseEntity<?> flags(@Valid @RequestBody WebhookPayload body) {
        var res = ingest.ingest(body.getEventId(), body.getType(), body.getTaskKey(), body);
        if (!res.accepted()) return ResponseEntity.accepted().body(res);
        return ResponseEntity.ok(res);
    }
}
