package com.todo.eod.infra.repo;

import com.todo.eod.domain.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {
    Optional<Task> findByKey(String key);
}
