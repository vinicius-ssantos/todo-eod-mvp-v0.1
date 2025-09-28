package com.todo.eod.infra.repo;

import com.todo.eod.domain.WebhookInbox;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WebhookInboxRepository extends JpaRepository<WebhookInbox, String> {
}
