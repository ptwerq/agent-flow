package com.agentflow.managerservice.repository;

import com.agentflow.managerservice.entity.outbox.OutboxEvent;
import com.agentflow.managerservice.entity.outbox.OutboxEventStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {
    List<OutboxEvent> findTop10ByStatusOrderByCreatedAtAsc(OutboxEventStatus status);
}
