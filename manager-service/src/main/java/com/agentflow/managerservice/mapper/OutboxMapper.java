package com.agentflow.managerservice.mapper;

import com.agentflow.managerservice.dto.event.ClientAssignedEvent;
import com.agentflow.managerservice.dto.event.ClientReleasedEvent;
import com.agentflow.managerservice.entity.outbox.OutboxEvent;
import com.agentflow.managerservice.entity.outbox.OutboxEventStatus;
import com.agentflow.managerservice.entity.outbox.OutboxEventType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxMapper {
    private final ObjectMapper objectMapper;

    public OutboxEvent toEntity(ClientAssignedEvent event, Long clientId) {
        return buildOutboxEvent(event, OutboxEventType.CLIENT_ASSIGNED, clientId);
    }

    public OutboxEvent toEntity(ClientReleasedEvent event, Long clientId) {
        return buildOutboxEvent(event, OutboxEventType.CLIENT_RELEASED, clientId);
    }

    private OutboxEvent buildOutboxEvent(Object event, OutboxEventType eventType, Long partitionKey) {
        try {
            String payload = objectMapper.writeValueAsString(event);

            return OutboxEvent.builder()
                    .eventType(eventType)
                    .partitionKey(partitionKey)
                    .payload(payload)
                    .status(OutboxEventStatus.NEW)
                    .retryCount(0)
                    .build();
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Error serializing event " + event.getClass().getSimpleName() + " to JSON", e);
        }
    }
}

