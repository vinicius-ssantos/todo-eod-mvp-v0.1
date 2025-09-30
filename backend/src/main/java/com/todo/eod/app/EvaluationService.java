package com.todo.eod.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.todo.eod.domain.*;
import com.todo.eod.infra.repo.EvidenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EvaluationService {

    private final EvidenceRepository evidenceRepository;
    private final FeatureFlagProvider flagProvider;
    private final ObjectMapper om = new ObjectMapper();

    public EvalResult evaluate(Task task) {
        try {
            DodPolicy policy = task.getDodPolicy();
            JsonNode spec = om.readTree(policy.getSpec());
            List<JsonNode> reqs = new ArrayList<>();
            if (spec.has("requirements") && spec.get("requirements").isArray()) {
                spec.get("requirements").forEach(reqs::add);
            }
            Map<EvidenceType, Boolean> okMap = new EnumMap<>(EvidenceType.class);

            List<Evidence> evidences = evidenceRepository.findByTask(task);
            Set<EvidenceType> present = evidences.stream().map(Evidence::getType).collect(Collectors.toSet());

            List<EvalProgress> progress = new ArrayList<>();
            for (JsonNode r : reqs) {
                String t = r.get("type").asText();
                EvidenceType et = EvidenceType.valueOf(t);
                boolean ok = present.contains(et);
                okMap.put(et, ok);

                String details = null;
                if (et == EvidenceType.FLAG_ENABLED && r.has("minPercentage")) {
                    int min = r.get("minPercentage").asInt();
                    String flagKey = r.has("flagKey") ? r.get("flagKey").asText() : null;
                    if (flagKey != null && task.getKey() != null) {
                        flagKey = flagKey.replace("{task.key}", task.getKey());
                    }

                    // Prefer provider, fallback to latest evidence percentage
                    int providerPercent = flagProvider != null && flagKey != null
                            ? flagProvider.getPercentage(flagKey).orElse(-1)
                            : -1;

                    int evidencePercent = evidences.stream()
                            .filter(ev -> ev.getType() == EvidenceType.FLAG_ENABLED)
                            .reduce((a, b) -> b)
                            .map(ev -> {
                                try {
                                    JsonNode p = om.readTree(ev.getPayload());
                                    return p.has("percentage") ? p.get("percentage").asInt() : -1;
                                } catch (Exception e) { return -1; }
                            })
                            .orElse(-1);

                    int percent = providerPercent >= 0 ? providerPercent : evidencePercent;
                    ok = percent >= min && (providerPercent >= 0 || present.contains(EvidenceType.FLAG_ENABLED));
                    details = "percentage=" + percent + ", min=" + min + (flagKey != null ? ", key=" + flagKey : "");
                }
                progress.add(new EvalProgress(t, ok, details));
            }
            boolean complete = progress.stream().allMatch(p -> p.ok());
            return new EvalResult(progress, complete);
        } catch (Exception e) {
            return new EvalResult(List.of(), false);
        }
    }

    public record EvalProgress(String req, boolean ok, String details){}
    public record EvalResult(List<EvalProgress> progress, boolean complete){}
}
