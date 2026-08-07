package com.agentflow.managerservice.listener;

import com.agentflow.managerservice.config.KafkaConfig;
import com.agentflow.managerservice.dto.event.ClientCreatedEvent;
import com.agentflow.managerservice.service.ClientAssignmentService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClientEventListener {

    private final ObjectMapper objectMapper;
    private final ClientAssignmentService clientAssignmentService;

    @KafkaListener(topics = KafkaConfig.CLIENT_CREATED_TOPIC, groupId = "manager-service-group")
    public void handleClientCreated(String payload) {
        log.debug("Received raw message from Kafka topic [{}]: {}", KafkaConfig.CLIENT_CREATED_TOPIC, payload);

        try {
            ClientCreatedEvent event = objectMapper.readValue(payload, ClientCreatedEvent.class);
            log.info("Successfully parsed ClientCreatedEvent for client ID: {}", event.clientId());

            clientAssignmentService.assignClient(event.clientId());

            log.info("Finished processing ClientCreatedEvent for client ID: {}", event.clientId());

        } catch (JsonProcessingException e) {
            log.error("Failed to parse Kafka message payload. Invalid JSON structure: {}", payload, e);
        } catch (Exception e) {
            log.error("Unexpected error occurred while processing Kafka message: {}", payload, e);
        }
    }
}
