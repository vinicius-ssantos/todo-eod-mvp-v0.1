package com.todo.eod.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "webhook_inbox")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class WebhookInbox {
    @Id
    @Column(name = "event_id", length = 100)
    private String eventId;

    @Column(nullable = false, length = 64)
    private String fingerprint;

    @Column(name = "received_at", nullable = false)
    private OffsetDateTime receivedAt;

    @Column(nullable = false, length = 20)
    private String status;
}
