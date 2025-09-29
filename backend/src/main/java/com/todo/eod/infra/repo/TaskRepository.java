package com.todo.eod.infra.repo;

import com.todo.eod.domain.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {
    Optional<Task> findByKey(String key);

    @Query("select distinct t from Task t left join t.labels l " +
           "where (:state is null or t.state = :state) " +
           "and (:assignee is null or t.assignee = :assignee) " +
           "and (:label is null or l = :label)")
    java.util.List<Task> search(@Param("state") com.todo.eod.domain.TaskState state,
                                @Param("assignee") String assignee,
                                @Param("label") String label);
}
