package com.todo.eod.infra.repo;

import com.todo.eod.domain.DodPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DodPolicyRepository extends JpaRepository<DodPolicy, UUID> {
    Optional<DodPolicy> findByName(String name);
    Optional<DodPolicy> findById(UUID id);
}
