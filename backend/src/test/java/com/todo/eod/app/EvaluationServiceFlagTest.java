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

class EvaluationServiceFlagTest {

    @Test
    void complete_is_false_when_flag_percentage_below_min() {
        var repo = Mockito.mock(EvidenceRepository.class);
        var svc = new EvaluationService(repo);
        var policy = DodPolicy.builder()
                .name("policy")
                .spec("""
                        {"requirements":[{"type":"FLAG_ENABLED","minPercentage":10}]}
                        """)
                .build();
        var task = Task.builder().dodPolicy(policy).build();

        when(repo.findByTask(task)).thenReturn(List.of(
                Evidence.builder().type(EvidenceType.FLAG_ENABLED).payload("{\"percentage\":5}").build()
        ));

        var res = svc.evaluate(task);
        assertFalse(res.complete());
    }

    @Test
    void complete_is_true_when_flag_percentage_meets_min() {
        var repo = Mockito.mock(EvidenceRepository.class);
        var svc = new EvaluationService(repo);
        var policy = DodPolicy.builder()
                .name("policy")
                .spec("""
                        {"requirements":[{"type":"FLAG_ENABLED","minPercentage":10}]}
                        """)
                .build();
        var task = Task.builder().dodPolicy(policy).build();

        when(repo.findByTask(task)).thenReturn(List.of(
                Evidence.builder().type(EvidenceType.FLAG_ENABLED).payload("{\"percentage\":10}").build()
        ));

        var res = svc.evaluate(task);
        assertTrue(res.complete());
    }
}

