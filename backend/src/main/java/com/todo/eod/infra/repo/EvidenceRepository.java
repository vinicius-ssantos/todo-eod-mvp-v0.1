package com.todo.eod.infra.repo;

import com.todo.eod.domain.Evidence;
import com.todo.eod.domain.EvidenceType;
import com.todo.eod.domain.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EvidenceRepository extends JpaRepository<Evidence, UUID> {
    List<Evidence> findByTask(Task task);
    List<Evidence> findByTaskAndType(Task task, EvidenceType type);
}
