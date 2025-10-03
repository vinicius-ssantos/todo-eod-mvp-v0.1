package com.todo.eod;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.todo.eod.infra.repo.DodPolicyRepository;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.io.IOException;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TaskFlowE2ETest {

    private static final String JWT_SECRET = "0123456789ABCDEF0123456789ABCDEF";
    private static final String GITHUB_SECRET = "gh-test-secret";

    private static EmbeddedPostgres postgres;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        ensurePostgres();
        registry.add("spring.datasource.url", () -> postgres.getJdbcUrl("postgres", "postgres"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "postgres");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("eod.security.jwt.secret", () -> JWT_SECRET);
        registry.add("eod.webhooks.github.secret", () -> GITHUB_SECRET);
        registry.add("eod.webhooks.gitlab.secret", () -> "gitlab-secret");
    }

    @AfterAll
    static void stopPostgres() {
        if (postgres != null) {
            try {
                postgres.close();
            } catch (IOException ignored) {
            }
        }
    }

    private static void ensurePostgres() {
        if (postgres == null) {
            try {
                postgres = EmbeddedPostgres.builder().setPort(0).start();
            } catch (IOException e) {
                throw new IllegalStateException("Failed to start embedded Postgres", e);
            }
        }
    }

    @Autowired
    TestRestTemplate rest;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    DodPolicyRepository policyRepository;

    @BeforeEach
    void configureRestTemplate() {
        var httpClient = HttpClients.custom().build();
        rest.getRestTemplate().setRequestFactory(new HttpComponentsClientHttpRequestFactory(httpClient));
    }

    @Test
    void taskLifecycleEndToEnd() throws Exception {
        var policy = policyRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("expected seeded DoD policy"));

        UUID correlationId = UUID.randomUUID();
        String taskKey = "TASK-" + correlationId.toString().substring(0, 8);

        Map<String, Object> createPayload = new LinkedHashMap<>();
        createPayload.put("key", taskKey);
        createPayload.put("title", "Ship integration coverage");
        createPayload.put("dodPolicyId", policy.getId().toString());
        createPayload.put("assignee", "integration");
        createPayload.put("labels", List.of("backend", "jacoco"));
        createPayload.put("correlationId", correlationId);

        String tasksToken = jwtWithScopes("tasks:*");

        ResponseEntity<String> createResponse = rest.exchange(
                "/tasks",
                HttpMethod.POST,
                new HttpEntity<>(createPayload, jsonHeaders(tasksToken)),
                String.class);

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode createdTask = objectMapper.readTree(createResponse.getBody());
        assertThat(createdTask.get("dod").get("complete").asBoolean()).isFalse();
        String taskId = createdTask.get("id").asText();

        Map<String, Object> reviewPatch = Map.of("state", "REVIEW");
        ResponseEntity<String> patchResponse = rest.exchange(
                "/tasks/" + taskId + "/state",
                HttpMethod.PATCH,
                new HttpEntity<>(reviewPatch, jsonHeaders(tasksToken)),
                String.class);
        assertThat(patchResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        String ingestToken = jwtWithScopes("webhooks:ingest");

        assertThat(postWebhook("/webhooks/observability", ingestToken, payload("PR_MERGED", taskKey)).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(postWebhook("/webhooks/observability", ingestToken, payload("CI_GREEN", taskKey)).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(postWebhook("/webhooks/observability", ingestToken,
                payload("DOC_PUBLISHED", taskKey, Map.of("url", "https://docs.meuapp.io/" + taskKey))).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(postWebhook("/webhooks/observability", ingestToken,
                payload("LOG_SEEN", taskKey, Map.of("message", "StartedApp", "correlationId", correlationId.toString()))).getStatusCode())
                .isEqualTo(HttpStatus.OK);

        String flagKey = "feature." + taskKey;
        updateFlag(flagKey, tasksToken, 55);

        String flagEventId = UUID.randomUUID().toString();
        var flagPayload = payload(flagEventId, "FLAG_ENABLED", taskKey, Map.of("percentage", 55, "flagKey", flagKey));
        assertThat(postWebhook("/webhooks/flags", ingestToken, flagPayload).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(postWebhook("/webhooks/flags", ingestToken, flagPayload).getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);

        ResponseEntity<String> fetchResponse = rest.exchange(
                "/tasks/" + taskId,
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(tasksToken)),
                String.class);
        assertThat(fetchResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode fetched = objectMapper.readTree(fetchResponse.getBody());
        assertThat(fetched.get("state").asText()).isEqualTo("DONE");
        JsonNode dod = fetched.get("dod");
        assertThat(dod.get("complete").asBoolean()).isTrue();
        assertThat(dod.get("policyId").asText()).isEqualTo(policy.getId().toString());
        JsonNode progress = dod.get("progress");
        assertThat(progress).isNotNull();
        assertThat(progress.size()).isEqualTo(5);
        for (JsonNode p : progress) {
            assertThat(p.get("ok").asBoolean()).isTrue();
        }

        ResponseEntity<String> listResponse = rest.exchange(
                "/tasks?state=DONE",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(tasksToken)),
                String.class);
        assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode list = objectMapper.readTree(listResponse.getBody());
        assertThat(list.isArray()).isTrue();
        boolean found = false;
        for (JsonNode node : list) {
            if (taskId.equals(node.get("id").asText())) {
                found = true;
                break;
            }
        }
        assertThat(found).isTrue();
    }

    @Test
    void githubWebhookRequiresValidSignature() throws Exception {
        var policy = policyRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("expected seeded DoD policy"));

        UUID correlationId = UUID.randomUUID();
        String taskKey = "TASK-" + correlationId.toString().substring(0, 8);

        Map<String, Object> createPayload = new LinkedHashMap<>();
        createPayload.put("key", taskKey);
        createPayload.put("title", "GitHub webhook validation");
        createPayload.put("dodPolicyId", policy.getId().toString());
        createPayload.put("correlationId", correlationId);

        String tasksToken = jwtWithScopes("tasks:*");
        ResponseEntity<String> createResponse = rest.exchange(
                "/tasks",
                HttpMethod.POST,
                new HttpEntity<>(createPayload, jsonHeaders(tasksToken)),
                String.class);
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String taskId = objectMapper.readTree(createResponse.getBody()).get("id").asText();

        ObjectNode root = objectMapper.createObjectNode();
        root.put("action", "closed");
        root.put("number", 42);
        ObjectNode pr = root.putObject("pull_request");
        pr.put("merged", true);
        pr.put("merge_commit_sha", "abcdef1234567890");
        pr.put("html_url", "https://github.com/acme/repo/pull/42");
        pr.putObject("base").put("ref", "main");
        root.putObject("repository").put("full_name", "acme/repo");
        String body = objectMapper.writeValueAsString(root);

        HttpHeaders invalidHeaders = new HttpHeaders();
        invalidHeaders.setContentType(MediaType.APPLICATION_JSON);
        invalidHeaders.set("X-GitHub-Event", "pull_request");
        invalidHeaders.set("X-GitHub-Delivery", UUID.randomUUID().toString());
        invalidHeaders.set("X-Hub-Signature-256", "sha256=0000");
        invalidHeaders.set("X-EOD-Task-Key", taskKey);

        ResponseEntity<String> invalid = rest.exchange(
                "/webhooks/github",
                HttpMethod.POST,
                new HttpEntity<>(body, invalidHeaders),
                String.class);
        assertThat(invalid.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        HttpHeaders validHeaders = new HttpHeaders();
        validHeaders.setContentType(MediaType.APPLICATION_JSON);
        validHeaders.set("X-GitHub-Event", "pull_request");
        validHeaders.set("X-GitHub-Delivery", UUID.randomUUID().toString());
        validHeaders.set("X-Hub-Signature-256", "sha256=" + hmacHex(body, GITHUB_SECRET));
        validHeaders.set("X-EOD-Task-Key", taskKey);

        ResponseEntity<String> ok = rest.exchange(
                "/webhooks/github",
                HttpMethod.POST,
                new HttpEntity<>(body, validHeaders),
                String.class);
        assertThat(ok.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> taskResponse = rest.exchange(
                "/tasks/" + taskId,
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(tasksToken)),
                String.class);
        assertThat(taskResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode task = objectMapper.readTree(taskResponse.getBody());
        JsonNode progress = task.get("dod").get("progress");
        assertThat(progress.size()).isEqualTo(5);
        assertThat(progress.get(0).get("req").asText()).isEqualTo("PR_MERGED");
        assertThat(progress.get(0).get("ok").asBoolean()).isTrue();
        assertThat(task.get("dod").get("complete").asBoolean()).isFalse();
    }

    @Test
    void dodPolicyCrudLifecycle() throws Exception {
        ResponseEntity<String> listResponse = rest.getForEntity("/dod-policies", String.class);
        assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode initial = objectMapper.readTree(listResponse.getBody());
        assertThat(initial.isArray()).isTrue();
        int before = initial.size();

        String specJson = objectMapper.writeValueAsString(Map.of(
                "id", "custom-policy-" + UUID.randomUUID(),
                "requirements", List.of()
        ));
        String policyName = "Temporary DoD " + UUID.randomUUID();

        Map<String, Object> createPayload = new LinkedHashMap<>();
        createPayload.put("name", policyName);
        createPayload.put("spec", specJson);
        createPayload.put("createdAt", OffsetDateTime.now().toString());

        ResponseEntity<String> createResponse = rest.postForEntity(
                "/dod-policies",
                createPayload,
                String.class);
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode created = objectMapper.readTree(createResponse.getBody());
        assertThat(created.get("id").asText()).isNotBlank();
        assertThat(created.get("name").asText()).isEqualTo(policyName);
        assertThat(created.get("spec").asText()).isEqualTo(specJson);

        String newId = created.get("id").asText();
        ResponseEntity<String> getResponse = rest.getForEntity("/dod-policies/" + newId, String.class);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode fetched = objectMapper.readTree(getResponse.getBody());
        assertThat(fetched.get("id").asText()).isEqualTo(newId);
        assertThat(fetched.get("name").asText()).isEqualTo(policyName);

        ResponseEntity<String> afterList = rest.getForEntity("/dod-policies", String.class);
        assertThat(afterList.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode after = objectMapper.readTree(afterList.getBody());
        assertThat(after.isArray()).isTrue();
        assertThat(after.size()).isGreaterThan(before);
    }

    @Test
    void flagControllerHandlesMissingPayloadAndNotFound() {
        String tasksToken = jwtWithScopes("tasks:*");

        ResponseEntity<String> missingBody = rest.exchange(
                "/flags/test-missing",
                HttpMethod.PUT,
                new HttpEntity<>(Map.of(), jsonHeaders(tasksToken)),
                String.class);
        assertThat(missingBody.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(missingBody.getBody()).contains("missing percentage");

        ResponseEntity<String> notFound = rest.exchange(
                "/flags/does-not-exist",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(tasksToken)),
                String.class);
        assertThat(notFound.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void githubFallbackDuplicateAndLegacyFlow() throws Exception {
        var policy = policyRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("expected seeded DoD policy"));

        String repo = "acme/repo";
        String workflowSha = "deadbeefcafebabe1234567";
        String derivedKey = repo + "@" + workflowSha.substring(0, 7);

        UUID correlationId = UUID.randomUUID();
        Map<String, Object> createPayload = new LinkedHashMap<>();
        createPayload.put("key", derivedKey);
        createPayload.put("title", "Workflow derived task");
        createPayload.put("dodPolicyId", policy.getId().toString());
        createPayload.put("correlationId", correlationId);

        String tasksToken = jwtWithScopes("tasks:*");
        ResponseEntity<String> createResponse = rest.exchange(
                "/tasks",
                HttpMethod.POST,
                new HttpEntity<>(createPayload, jsonHeaders(tasksToken)),
                String.class);
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        String workflowBody = objectMapper.writeValueAsString(Map.of(
                "action", "completed",
                "workflow_run", Map.of(
                        "conclusion", "success",
                        "name", "Build",
                        "head_sha", workflowSha,
                        "head_branch", "main",
                        "html_url", "https://ci.example.com/run/1",
                        "id", 987654321
                ),
                "repository", Map.of("full_name", repo)
        ));

        HttpHeaders workflowHeaders = githubHeaders("workflow_run", workflowBody, null, null);

        ResponseEntity<String> workflowResp = rest.exchange(
                "/webhooks/github",
                HttpMethod.POST,
                new HttpEntity<>(workflowBody, workflowHeaders),
                String.class);
        assertThat(workflowResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> duplicateResp = rest.exchange(
                "/webhooks/github",
                HttpMethod.POST,
                new HttpEntity<>(workflowBody, workflowHeaders),
                String.class);
        assertThat(duplicateResp.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(duplicateResp.getBody()).contains("duplicate");

        String pullBody = objectMapper.writeValueAsString(Map.of(
                "action", "opened",
                "pull_request", Map.of("merged", false),
                "repository", Map.of("full_name", repo)
        ));

        HttpHeaders pullHeaders = githubHeaders("pull_request", pullBody, UUID.randomUUID().toString(), null);

        ResponseEntity<String> ignoredResp = rest.exchange(
                "/webhooks/github",
                HttpMethod.POST,
                new HttpEntity<>(pullBody, pullHeaders),
                String.class);
        assertThat(ignoredResp.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(ignoredResp.getBody()).contains("ignored event: pull_request");

        String invalidJson = "{not-json";
        HttpHeaders invalidHeaders = githubHeaders("pull_request", invalidJson, UUID.randomUUID().toString(), null);

        ResponseEntity<String> invalidResp = rest.exchange(
                "/webhooks/github",
                HttpMethod.POST,
                new HttpEntity<>(invalidJson, invalidHeaders),
                String.class);
        assertThat(invalidResp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        Map<String, Object> legacyPayload = Map.of(
                "eventId", UUID.randomUUID().toString(),
                "type", "CI_GREEN",
                "taskKey", derivedKey
        );
        String legacyBody = objectMapper.writeValueAsString(legacyPayload);

        HttpHeaders legacyHeaders = new HttpHeaders();
        legacyHeaders.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> legacyResp = rest.exchange(
                "/webhooks/github",
                HttpMethod.POST,
                new HttpEntity<>(legacyBody, legacyHeaders),
                String.class);
        assertThat(legacyResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        String badLegacy = "{";
        ResponseEntity<String> badLegacyResp = rest.exchange(
                "/webhooks/github",
                HttpMethod.POST,
                new HttpEntity<>(badLegacy, legacyHeaders),
                String.class);
        assertThat(badLegacyResp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void gitlabWebhookSignatureFlow() throws Exception {
        var policy = policyRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("expected seeded DoD policy"));

        String repo = "group/project";
        int prNumber = 21;
        String mergeTaskKey = repo + "#" + prNumber;

        UUID correlationId = UUID.randomUUID();
        Map<String, Object> createPayload = new LinkedHashMap<>();
        createPayload.put("key", mergeTaskKey);
        createPayload.put("title", "GitLab merge request");
        createPayload.put("dodPolicyId", policy.getId().toString());
        createPayload.put("correlationId", correlationId);

        String tasksToken = jwtWithScopes("tasks:*");
        ResponseEntity<String> createResponse = rest.exchange(
                "/tasks",
                HttpMethod.POST,
                new HttpEntity<>(createPayload, jsonHeaders(tasksToken)),
                String.class);
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        String mergeBody = objectMapper.writeValueAsString(Map.of(
                "object_kind", "merge_request",
                "project", Map.of("path_with_namespace", repo, "web_url", "https://gitlab.example.com/" + repo),
                "object_attributes", Map.of(
                        "state", "merged",
                        "iid", prNumber,
                        "target_branch", "main",
                        "merge_commit_sha", "abcdef1234567890",
                        "url", "https://gitlab.example.com/merge/" + prNumber
                )
        ));

        HttpHeaders invalidHeaders = new HttpHeaders();
        invalidHeaders.setContentType(MediaType.APPLICATION_JSON);
        invalidHeaders.set("X-Gitlab-Event", "Merge Request Hook");
        invalidHeaders.set("X-Gitlab-Signature", "invalid");

        ResponseEntity<String> unauthorized = rest.exchange(
                "/webhooks/gitlab",
                HttpMethod.POST,
                new HttpEntity<>(mergeBody, invalidHeaders),
                String.class);
        assertThat(unauthorized.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        HttpHeaders validHeaders = new HttpHeaders();
        validHeaders.setContentType(MediaType.APPLICATION_JSON);
        validHeaders.set("X-Gitlab-Event", "Merge Request Hook");
        validHeaders.set("X-Gitlab-Signature", hmacBase64(mergeBody, "gitlab-secret"));

        ResponseEntity<String> mergeResp = rest.exchange(
                "/webhooks/gitlab",
                HttpMethod.POST,
                new HttpEntity<>(mergeBody, validHeaders),
                String.class);
        assertThat(mergeResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> duplicateMerge = rest.exchange(
                "/webhooks/gitlab",
                HttpMethod.POST,
                new HttpEntity<>(mergeBody, validHeaders),
                String.class);
        assertThat(duplicateMerge.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(duplicateMerge.getBody()).contains("duplicate");

        String pipelineBody = objectMapper.writeValueAsString(Map.of(
                "object_kind", "pipeline",
                "project", Map.of("path_with_namespace", repo, "web_url", "https://gitlab.example.com/" + repo),
                "object_attributes", Map.of(
                        "status", "failed",
                        "name", "Pipeline",
                        "sha", "feedfacecafebabe1234567",
                        "ref", "main"
                )
        ));

        HttpHeaders pipelineHeaders = new HttpHeaders();
        pipelineHeaders.setContentType(MediaType.APPLICATION_JSON);
        pipelineHeaders.set("X-Gitlab-Event", "Pipeline Hook");
        pipelineHeaders.set("X-Gitlab-Token", "gitlab-secret");

        ResponseEntity<String> ignoredPipeline = rest.exchange(
                "/webhooks/gitlab",
                HttpMethod.POST,
                new HttpEntity<>(pipelineBody, pipelineHeaders),
                String.class);
        assertThat(ignoredPipeline.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(ignoredPipeline.getBody()).contains("ignored event: Pipeline Hook");

        String invalidPipeline = "{invalid";
        HttpHeaders invalidPipelineHeaders = new HttpHeaders();
        invalidPipelineHeaders.setContentType(MediaType.APPLICATION_JSON);
        invalidPipelineHeaders.set("X-Gitlab-Event", "Pipeline Hook");
        invalidPipelineHeaders.set("X-Gitlab-Signature", hmacBase64(invalidPipeline, "gitlab-secret"));

        ResponseEntity<String> invalidPipelineResp = rest.exchange(
                "/webhooks/gitlab",
                HttpMethod.POST,
                new HttpEntity<>(invalidPipeline, invalidPipelineHeaders),
                String.class);
        assertThat(invalidPipelineResp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private ResponseEntity<String> postWebhook(String path, String token, Map<String, Object> payload) {
        return rest.exchange(
                path,
                HttpMethod.POST,
                new HttpEntity<>(payload, jsonHeaders(token)),
                String.class);
    }

    private Map<String, Object> payload(String type, String taskKey) {
        return payload(UUID.randomUUID().toString(), type, taskKey, Map.of());
    }

    private Map<String, Object> payload(String type, String taskKey, Map<String, Object> extra) {
        return payload(UUID.randomUUID().toString(), type, taskKey, extra);
    }

    private Map<String, Object> payload(String eventId, String type, String taskKey, Map<String, Object> extra) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("eventId", eventId);
        map.put("type", type);
        map.put("taskKey", taskKey);
        map.put("at", Instant.now().toString());
        if (extra != null) {
            map.putAll(extra);
        }
        return map;
    }

    private void updateFlag(String key, String token, int percentage) {
        Map<String, Object> body = Map.of("percentage", percentage);
        ResponseEntity<String> response = rest.exchange(
                "/flags/" + key,
                HttpMethod.PUT,
                new HttpEntity<>(body, jsonHeaders(token)),
                String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private HttpHeaders jsonHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return headers;
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return headers;
    }

    private HttpHeaders githubHeaders(String event, String body, String deliveryId, String taskKeyHeader) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (event != null) {
            headers.set("X-GitHub-Event", event);
        }
        if (deliveryId != null) {
            headers.set("X-GitHub-Delivery", deliveryId);
        }
        if (taskKeyHeader != null) {
            headers.set("X-EOD-Task-Key", taskKeyHeader);
        }
        headers.set("X-Hub-Signature-256", "sha256=" + hmacHex(body, GITHUB_SECRET));
        return headers;
    }

    private String jwtWithScopes(String... scopes) {
        Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("iss", "e2e-tests");
        claims.put("sub", "integration");
        claims.put("scope", String.join(" ", scopes));
        long now = Instant.now().getEpochSecond();
        claims.put("iat", now);
        claims.put("exp", now + 3600);

        String headerPart = base64UrlEncode(header);
        String payloadPart = base64UrlEncode(claims);
        String signature = hmacSha256(headerPart + "." + payloadPart, JWT_SECRET);
        return headerPart + "." + payloadPart + "." + signature;
    }

    private String base64UrlEncode(Map<String, Object> payload) {
        try {
            byte[] json = objectMapper.writeValueAsBytes(payload);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(json);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to encode JWT segment", e);
        }
    }

    private String hmacSha256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to sign JWT", e);
        }
    }

    private String hmacHex(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(raw.length * 2);
            for (byte b : raw) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to compute HMAC", e);
        }
    }

    private String hmacBase64(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(raw);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to compute Base64 HMAC", e);
        }
    }
}
