package com.todo.eod.web;

import com.todo.eod.domain.DodPolicy;
import com.todo.eod.infra.repo.DodPolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/dod-policies")
@RequiredArgsConstructor
public class PolicyController {

    private final DodPolicyRepository repo;

    @GetMapping
    public List<DodPolicy> list() {
        return repo.findAll();
    }

    @PostMapping
    public DodPolicy create(@RequestBody DodPolicy p) {
        return repo.save(p);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DodPolicy> get(@PathVariable UUID id) {
        return repo.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
}
