package com.todo.eod.app;

import com.todo.eod.domain.DodPolicy;
import com.todo.eod.domain.Task;
import com.todo.eod.infra.repo.EvidenceRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class EvaluationServiceFlagProviderTest {
    @Test
    void provider_percentage_satisfies_requirement() {
        var repo = Mockito.mock(EvidenceRepository.class);
        var flags = Mockito.mock(FeatureFlagProvider.class);
        var svc = new EvaluationService(repo, flags);
        var policy = DodPolicy.builder()
                .name("policy")
                .spec("""
                        {"requirements":[{"type":"FLAG_ENABLED","flagKey":"FF_A","minPercentage":25}]}
                        """)
                .build();
        var task = Task.builder().dodPolicy(policy).build();

        when(flags.getPercentage("FF_A")).thenReturn(java.util.Optional.of(30));

        var res = svc.evaluate(task);
        assertTrue(res.complete());
    }

    @Test
    void provider_percentage_below_min_fails() {
        var repo = Mockito.mock(EvidenceRepository.class);
        var flags = Mockito.mock(FeatureFlagProvider.class);
        var svc = new EvaluationService(repo, flags);
        var policy = DodPolicy.builder()
                .name("policy")
                .spec("""
                        {"requirements":[{"type":"FLAG_ENABLED","flagKey":"FF_A","minPercentage":50}]}
                        """)
                .build();
        var task = Task.builder().dodPolicy(policy).build();

        when(flags.getPercentage("FF_A")).thenReturn(java.util.Optional.of(25));

        var res = svc.evaluate(task);
        assertFalse(res.complete());
    }
}

