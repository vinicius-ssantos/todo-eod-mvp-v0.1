package com.todo.eod.app;

import com.todo.eod.domain.DodPolicy;
import com.todo.eod.domain.Evidence;
import com.todo.eod.domain.EvidenceType;
import com.todo.eod.domain.Task;
import com.todo.eod.infra.repo.EvidenceRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

public class EvaluationServiceTest {

    @Test
    void evaluate_returns_complete_when_all_requirements_present() throws Exception {
        var repo = Mockito.mock(EvidenceRepository.class);
        var svc = new EvaluationService(repo);
        var policy = DodPolicy.builder()
                .name("policy")
                .spec("""
                        {"requirements":[{"type":"PR_MERGED"},{"type":"CI_GREEN"}]}
                        """)
                .build();
        var task = Task.builder().dodPolicy(policy).build();
        when(repo.findByTask(task)).thenReturn(List.of(
                Evidence.builder().type(EvidenceType.PR_MERGED).build(),
                Evidence.builder().type(EvidenceType.CI_GREEN).build()
        ));
        var res = svc.evaluate(task);
        assertTrue(res.complete());
    }

    @Test
    void evaluate_returns_incomplete_when_missing() {
        var repo = Mockito.mock(EvidenceRepository.class);
        var svc = new EvaluationService(repo);
        var policy = DodPolicy.builder()
                .name("policy")
                .spec("""
                        {"requirements":[{"type":"PR_MERGED"},{"type":"CI_GREEN"}]}
                        """)
                .build();
        var task = Task.builder().dodPolicy(policy).build();
        when(repo.findByTask(task)).thenReturn(List.of(Evidence.builder().type(EvidenceType.PR_MERGED).build()));
        var res = svc.evaluate(task);
        assertFalse(res.complete());
    }
}
