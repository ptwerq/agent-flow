package com.agentflow.clientservice.listener;

import com.agentflow.clientservice.config.KafkaConfig;
import com.agentflow.clientservice.dto.event.ClientAssignedEvent;
import com.agentflow.clientservice.dto.event.ClientReleasedEvent;
import com.agentflow.clientservice.service.ClientService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClientAssignmentListener {
    private final ClientService clientService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = KafkaConfig.CLIENT_ASSIGNED_TOPIC, groupId = "client-service-group")
    public void handleClientAssigned(String payload) {
        log.info("Received assignment event from [{}]: {}", KafkaConfig.CLIENT_ASSIGNED_TOPIC, payload);
        try {
            ClientAssignedEvent event = objectMapper.readValue(payload, ClientAssignedEvent.class);

            clientService.assignManager(event.clientId(), event.managerId());

            log.info("Successfully assigned manager [ID: {}] to client [ID: {}]",
                    event.managerId(), event.clientId());

        } catch (JsonProcessingException e) {
            log.error("Failed to parse Kafka message payload on topic [{}]. Payload: {}",
                    KafkaConfig.CLIENT_ASSIGNED_TOPIC, payload, e);
        } catch (Exception e) {
            log.error("Unexpected error processing assignment for payload: {}", payload, e);
        }
    }

    @KafkaListener(topics = KafkaConfig.CLIENT_RELEASED_TOPIC, groupId = "client-service-group")
    public void handleClientReleased(String payload) {
        log.info("Received release event from [{}]: {}", KafkaConfig.CLIENT_RELEASED_TOPIC, payload);
        try {
            ClientReleasedEvent event = objectMapper.readValue(payload, ClientReleasedEvent.class);

            clientService.releaseManager(event.clientId());

            log.info("Successfully released client [ID: {}]", event.clientId());

        } catch (JsonProcessingException e) {
            log.error("Failed to parse Kafka message payload on topic [{}]. Payload: {}",
                    KafkaConfig.CLIENT_RELEASED_TOPIC, payload, e);
        } catch (Exception e) {
            log.error("Unexpected error processing release for payload: {}", payload, e);
        }
    }

}
