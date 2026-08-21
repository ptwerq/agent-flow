package com.agentflow.managerservice.service;

import com.agentflow.managerservice.config.KafkaConfig;
import com.agentflow.managerservice.dto.event.ClientAssignedEvent;
import com.agentflow.managerservice.dto.event.ClientReleasedEvent;
import com.agentflow.managerservice.entity.AssignmentStatus;
import com.agentflow.managerservice.entity.ClientAssignment;
import com.agentflow.managerservice.entity.Manager;
import com.agentflow.managerservice.entity.outbox.OutboxEvent;
import com.agentflow.managerservice.exception.NotFoundException;
import com.agentflow.managerservice.mapper.ClientAssignmentMapper;
import com.agentflow.managerservice.mapper.OutboxMapper;
import com.agentflow.managerservice.repository.ClientAssignmentRepository;
import com.agentflow.managerservice.repository.OutboxRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientAssignmentService {

    private final ManagerService managerService;
    private final ClientAssignmentMapper clientAssignmentMapper;
    private final ClientAssignmentRepository clientAssignmentRepository;
    private final OutboxMapper outboxMapper;
    private final OutboxRepository outboxRepository;

    @Transactional
    public void assignClient(Long clientId) {
        if (clientAssignmentRepository.existsByClientId(clientId)) {
            log.info("Client ID: {} already has an assignment record. Skipping duplicate event", clientId);
            return;
        }

        Manager manager = managerService.findLeastLoadedAvailableManager();
        managerService.increaseLoad(manager.getId());

        ClientAssignment assignment = clientAssignmentMapper.toEntity(clientId, manager);
        clientAssignmentRepository.save(assignment);

        ClientAssignedEvent clientAssignedEvent = new ClientAssignedEvent(clientId, manager.getId());
        OutboxEvent outboxEvent = outboxMapper.toEntity(clientAssignedEvent, clientId);
        outboxRepository.save(outboxEvent);

        log.info("Client ID: {} successfully assigned to Manager ID: {}", clientId, manager.getId());
    }

    @Transactional
    public void releaseClient(Long clientId) {
        ClientAssignment assignment = clientAssignmentRepository
                .findByClientIdAndStatus(clientId, AssignmentStatus.ACTIVE)
                .orElseThrow(() -> new NotFoundException("Active assignment not found for client ID: " + clientId));

        LocalDateTime releasedAt = LocalDateTime.now();
        assignment.setStatus(AssignmentStatus.COMPLETED);
        assignment.setReleasedAt(releasedAt);
        clientAssignmentRepository.save(assignment);

        managerService.decreaseLoad(assignment.getManager().getId());

        ClientReleasedEvent clientReleasedEvent = new ClientReleasedEvent(clientId, assignment.getManager().getId(), releasedAt);
        OutboxEvent outboxEvent = outboxMapper.toEntity(clientReleasedEvent, clientId);
        outboxRepository.save(outboxEvent);

        log.info("Client ID: {} released from Manager ID: {}", clientId, assignment.getManager().getId());
    }

}
