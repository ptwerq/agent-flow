package com.agentflow.managerservice.listener;

import com.agentflow.managerservice.config.KafkaConfig;
import com.agentflow.managerservice.dto.event.ClientCreatedEvent;
import com.agentflow.managerservice.service.ClientAssignmentService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClientEventListener {
    private final ClientAssignmentService clientAssignmentService;

    @KafkaListener(
            topics = KafkaConfig.CLIENT_CREATED_TOPIC,
            groupId = "manager-service-group"
    )
    public void handleClientCreated(
            @Payload ClientCreatedEvent event,
            Acknowledgment ack
    ) {
        log.info("Processing ClientCreatedEvent for client ID: {}", event.clientId());
        clientAssignmentService.assignClient(event.clientId());
        ack.acknowledge();

        log.info("Successfully processed and acknowledged ClientCreatedEvent for client ID: {}", event.clientId());
    }
}
