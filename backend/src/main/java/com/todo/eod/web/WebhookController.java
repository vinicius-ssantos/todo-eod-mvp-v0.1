package com.todo.eod.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.todo.eod.app.WebhookIngestService;
import com.todo.eod.web.dto.WebhookPayload;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookIngestService ingest;
    private final WebhookSecurity webhookSecurity;
    private final WebhookNormalizer webhookNormalizer;
    private final com.todo.eod.infra.ratelimit.RateLimiterService rateLimiter;
    private final com.todo.eod.infra.idem.IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping(value = "/webhooks/github", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> github(@RequestHeader Map<String, String> headers,
                                    @RequestBody String body,
                                    HttpServletRequest request,
                                    @RequestParam(value = "taskKey", required = false) String taskKeyParam) {
        String ghEvent = header(headers, "X-GitHub-Event");
        String deliveryId = header(headers, "X-GitHub-Delivery");

        // Rate limit by origin
        if (!rateLimiter.allow("github")) return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body("rate limit");

        if (StringUtils.hasText(ghEvent)) {
            boolean ok = webhookSecurity.verifyGitHub(headers, body);
            if (!ok) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("invalid signature");

            try {
                JsonNode root = objectMapper.readTree(body);
                WebhookPayload normalized = webhookNormalizer.normalizeGitHub(ghEvent, deliveryId, root, header(headers, "X-EOD-Task-Key"), taskKeyParam);
                if (!StringUtils.hasText(normalized.getEventId())) {
                    normalized.setEventId(hexSha256(body));
                }
                if (!idempotencyService.isFirstProcessing(normalized.getEventId())) {
                    return ResponseEntity.accepted().body("duplicate");
                }
                if (!StringUtils.hasText(normalized.getType())) {
                    return ResponseEntity.accepted().body("ignored event: " + ghEvent);
                }
                var res = ingest.ingest(normalized.getEventId(), normalized.getType(), normalized.getTaskKey(), normalized);
                if (!res.accepted()) return ResponseEntity.accepted().body(res);
                return ResponseEntity.ok(res);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body("invalid json");
            }
        }

        try {
            WebhookPayload legacy = objectMapper.readValue(body, WebhookPayload.class);
            var res = ingest.ingest(legacy.getEventId(), legacy.getType(), legacy.getTaskKey(), legacy);
            if (!res.accepted()) return ResponseEntity.accepted().body(res);
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("invalid payload");
        }
    }

    @PostMapping(value = "/webhooks/gitlab", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> gitlab(@RequestHeader Map<String, String> headers,
                                    @RequestBody String body,
                                    @RequestParam(value = "taskKey", required = false) String taskKeyParam) {
        // Rate limit by origin
        if (!rateLimiter.allow("gitlab")) return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body("rate limit");

        boolean ok = webhookSecurity.verifyGitLab(headers, body);
        if (!ok) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("invalid signature");

        try {
            String glEvent = header(headers, "X-Gitlab-Event");
            String evtUuid = header(headers, "X-Gitlab-Event-UUID");
            JsonNode root = objectMapper.readTree(body);
            WebhookPayload normalized = webhookNormalizer.normalizeGitLab(glEvent, evtUuid, root, header(headers, "X-EOD-Task-Key"), taskKeyParam);
            if (!StringUtils.hasText(normalized.getEventId())) {
                normalized.setEventId(hexSha256(body));
            }
            if (!idempotencyService.isFirstProcessing(normalized.getEventId())) {
                return ResponseEntity.accepted().body("duplicate");
            }
            if (!StringUtils.hasText(normalized.getType())) {
                return ResponseEntity.accepted().body("ignored event: " + glEvent);
            }
            var res = ingest.ingest(normalized.getEventId(), normalized.getType(), normalized.getTaskKey(), normalized);
            if (!res.accepted()) return ResponseEntity.accepted().body(res);
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("invalid json");
        }
    }

    @PostMapping("/webhooks/observability")
    public ResponseEntity<?> observability(@RequestBody WebhookPayload body) {
        if (!rateLimiter.allow("observability")) return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body("rate limit");
        var res = ingest.ingest(body.getEventId(), body.getType(), body.getTaskKey(), body);
        if (!res.accepted()) return ResponseEntity.accepted().body(res);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/webhooks/flags")
    public ResponseEntity<?> flags(@RequestBody WebhookPayload body) {
        if (!rateLimiter.allow("flags")) return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body("rate limit");
        var res = ingest.ingest(body.getEventId(), body.getType(), body.getTaskKey(), body);
        if (!res.accepted()) return ResponseEntity.accepted().body(res);
        return ResponseEntity.ok(res);
    }

    private String header(Map<String, String> headers, String name) {
        if (headers == null) return null;
        for (var e : headers.entrySet()) {
            if (e.getKey() != null && e.getKey().equalsIgnoreCase(name)) return e.getValue();
        }
        return null;
    }

    private String hexSha256(String s) {
        try {
            var md = java.security.MessageDigest.getInstance("SHA-256");
            var h = md.digest(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            var sb = new StringBuilder();
            for (byte b : h) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return "evt" + System.currentTimeMillis();
        }
    }
}
