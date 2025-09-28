package com.todo.eod.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "task")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class Task {
    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "key", nullable = false, unique = true, length = 32)
    private String key;

    @Column(nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskState state;

    @ManyToOne(optional = false)
    @JoinColumn(name = "dod_policy_id")
    private DodPolicy dodPolicy;

    @Column(length = 120)
    private String assignee;

    @ElementCollection
    @CollectionTable(name = "task_labels", joinColumns = @JoinColumn(name = "task_id"))
    @Column(name = "label")
    private List<String> labels;

    @Column(name = "correlation_id", nullable = false)
    private UUID correlationId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
