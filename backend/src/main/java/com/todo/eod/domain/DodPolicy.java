package com.todo.eod.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "dod_policy")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class DodPolicy {
    @Id
    @UuidGenerator
    private UUID id;

    @Column(nullable = false, length = 120)
    private String name;

    /** JSON (YAML carregado e convertido) **/
    @Column(nullable = false, columnDefinition = "jsonb")
    private String spec;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}
