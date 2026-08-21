package com.agentflow.clientservice.listener;

import com.agentflow.clientservice.config.KafkaConfig;
import com.agentflow.clientservice.dto.event.ClientAssignedEvent;
import com.agentflow.clientservice.dto.event.ClientReleasedEvent;
import com.agentflow.clientservice.service.ClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
@Slf4j
public class ClientAssignmentListener {

    private final ClientService clientService;

    @KafkaListener(topics = KafkaConfig.CLIENT_ASSIGNED_TOPIC, groupId = "client-service-group")
    public void handleClientAssigned(
            @Payload ClientAssignedEvent event,
            Acknowledgment ack
    ) {
        log.info("Processing assignment event for client [ID: {}] with manager [ID: {}]",
                event.clientId(), event.managerId());

        clientService.assignManager(event.clientId(), event.managerId());
        ack.acknowledge();

        log.info("Successfully assigned manager [ID: {}] to client [ID: {}]",
                event.managerId(), event.clientId());
    }

    @KafkaListener(topics = KafkaConfig.CLIENT_RELEASED_TOPIC, groupId = "client-service-group")
    public void handleClientReleased(
            @Payload ClientReleasedEvent event,
            Acknowledgment ack
    ) {
        log.info("Processing release event for client [ID: {}]", event.clientId());
        clientService.releaseManager(event.clientId());
        ack.acknowledge();

        log.info("Successfully released manager from client [ID: {}]", event.clientId());
    }
}
