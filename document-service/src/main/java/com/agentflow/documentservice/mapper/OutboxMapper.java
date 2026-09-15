package com.agentflow.documentservice.mapper;

import com.agentflow.documentservice.dto.event.DocumentUploadedEvent;
import com.agentflow.documentservice.entity.outbox.OutboxEvent;
import com.agentflow.documentservice.entity.outbox.OutboxEventStatus;
import com.agentflow.documentservice.entity.outbox.OutboxEventType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxMapper {
    private final ObjectMapper objectMapper;

    public OutboxEvent toEntity(DocumentUploadedEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);

            return OutboxEvent.builder()
                    .eventType(OutboxEventType.DOCUMENT_UPLOADED)
                    .partitionKey(event.clientId())
                    .payload(payload)
                    .status(OutboxEventStatus.NEW)
                    .retryCount(0)
                    .build();
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Error with serializing event " + event.getClass().getSimpleName() + " to JSON", e);
        }
    }

}
