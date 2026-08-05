package com.agentflow.clientservice.mapper;

import com.agentflow.clientservice.dto.event.ClientCreatedEvent;
import com.agentflow.clientservice.entity.outbox.OutboxEvent;
import com.agentflow.clientservice.entity.outbox.OutboxEventType;
import com.agentflow.clientservice.entity.outbox.OutboxEventStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxMapper {
    private final ObjectMapper objectMapper;

    public OutboxEvent toEntity(ClientCreatedEvent event, Long clientId) {
        try {
            String payload = objectMapper.writeValueAsString(event);

            return OutboxEvent.builder()
                    .eventType(OutboxEventType.CLIENT_CREATED)
                    .partitionKey(clientId)
                    .payload(payload)
                    .status(OutboxEventStatus.NEW)
                    .retryCount(0)
                    .build();
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Error with serializing event " + event.getClass().getSimpleName() + " to JSON", e);
        }
    }
}
