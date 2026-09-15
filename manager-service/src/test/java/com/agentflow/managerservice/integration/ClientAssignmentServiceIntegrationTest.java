package com.agentflow.managerservice.integration;

import com.agentflow.managerservice.config.JpaConfig;
import com.agentflow.managerservice.entity.AssignmentStatus;
import com.agentflow.managerservice.entity.ClientAssignment;
import com.agentflow.managerservice.entity.Manager;
import com.agentflow.managerservice.entity.ManagerStatus;
import com.agentflow.managerservice.entity.outbox.OutboxEvent;
import com.agentflow.managerservice.entity.outbox.OutboxEventStatus;
import com.agentflow.managerservice.entity.outbox.OutboxEventType;
import com.agentflow.managerservice.exception.NoAvailableManagerException;
import com.agentflow.managerservice.exception.NotFoundException;
import com.agentflow.managerservice.mapper.ClientAssignmentMapperImpl;
import com.agentflow.managerservice.mapper.ManagerMapperImpl;
import com.agentflow.managerservice.mapper.OutboxMapper;
import com.agentflow.managerservice.repository.ClientAssignmentRepository;
import com.agentflow.managerservice.repository.ManagerRepository;
import com.agentflow.managerservice.repository.OutboxRepository;
import com.agentflow.managerservice.service.ClientAssignmentService;
import com.agentflow.managerservice.service.ManagerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
        JpaConfig.class,
        ClientAssignmentService.class,
        ManagerService.class,
        ManagerMapperImpl.class,
        ClientAssignmentMapperImpl.class,
        OutboxMapper.class
})
class ClientAssignmentServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private ClientAssignmentService clientAssignmentService;

    @Autowired
    private ManagerRepository managerRepository;

    @Autowired
    private ClientAssignmentRepository clientAssignmentRepository;

    @Autowired
    private OutboxRepository outboxRepository;

    @Test
    void assignClient_shouldCreateAssignmentIncreaseManagerLoadAndCreateOutboxEvent() {
        Manager manager = saveManager(
                "John",
                "Doe",
                "john@example.com",
                10,
                0,
                ManagerStatus.ACTIVE
        );

        Long clientId = 100L;

        clientAssignmentService.assignClient(clientId);

        List<ClientAssignment> assignments = clientAssignmentRepository.findAll();
        List<OutboxEvent> outboxEvents = outboxRepository.findAll();

        assertThat(assignments).hasSize(1);

        ClientAssignment assignment = assignments.get(0);

        assertThat(assignment.getClientId()).isEqualTo(clientId);
        assertThat(assignment.getManager().getId()).isEqualTo(manager.getId());
        assertThat(assignment.getStatus()).isEqualTo(AssignmentStatus.ACTIVE);
        assertThat(assignment.getAssignedAt()).isNotNull();
        assertThat(assignment.getReleasedAt()).isNull();

        Manager updatedManager = managerRepository.findById(manager.getId())
                .orElseThrow();

        assertThat(updatedManager.getCurrentLoad()).isEqualTo(1);
        assertThat(updatedManager.getStatus()).isEqualTo(ManagerStatus.ACTIVE);

        assertThat(outboxEvents).hasSize(1);

        OutboxEvent outboxEvent = outboxEvents.get(0);

        assertThat(outboxEvent.getEventType())
                .isEqualTo(OutboxEventType.CLIENT_ASSIGNED);
        assertThat(outboxEvent.getPartitionKey())
                .isEqualTo(clientId);
        assertThat(outboxEvent.getStatus())
                .isEqualTo(OutboxEventStatus.NEW);
        assertThat(outboxEvent.getRetryCount())
                .isEqualTo(0);
        assertThat(outboxEvent.getPayload())
                .contains("\"clientId\":100")
                .contains("\"managerId\":" + manager.getId());
    }

    @Test
    void assignClient_shouldSkipDuplicateAssignment() {
        Manager manager = saveManager(
                "John",
                "Doe",
                "john@example.com",
                10,
                0,
                ManagerStatus.ACTIVE
        );

        Long clientId = 100L;

        ClientAssignment existingAssignment = ClientAssignment.builder()
                .clientId(clientId)
                .manager(manager)
                .status(AssignmentStatus.ACTIVE)
                .assignedAt(LocalDateTime.now())
                .build();

        clientAssignmentRepository.save(existingAssignment);

        clientAssignmentService.assignClient(clientId);

        assertThat(clientAssignmentRepository.findAll())
                .hasSize(1);

        Manager updatedManager = managerRepository.findById(manager.getId())
                .orElseThrow();

        assertThat(updatedManager.getCurrentLoad())
                .isZero();

        assertThat(outboxRepository.findAll())
                .isEmpty();
    }

    @Test
    void assignClient_shouldThrowException_whenNoManagerIsAvailable() {
        Long clientId = 100L;

        assertThatThrownBy(() ->
                clientAssignmentService.assignClient(clientId)
        )
                .isInstanceOf(NoAvailableManagerException.class)
                .hasMessage("No available manager found");

        assertThat(clientAssignmentRepository.findAll())
                .isEmpty();

        assertThat(outboxRepository.findAll())
                .isEmpty();
    }

    @Test
    void releaseClient_shouldCompleteAssignmentDecreaseManagerLoadAndCreateOutboxEvent() {
        Manager manager = saveManager(
                "John",
                "Doe",
                "john@example.com",
                10,
                1,
                ManagerStatus.ACTIVE
        );

        Long clientId = 100L;

        ClientAssignment assignment = ClientAssignment.builder()
                .clientId(clientId)
                .manager(manager)
                .status(AssignmentStatus.ACTIVE)
                .assignedAt(LocalDateTime.now().minusHours(1))
                .build();

        clientAssignmentRepository.save(assignment);

        clientAssignmentService.releaseClient(clientId);

        ClientAssignment updatedAssignment =
                clientAssignmentRepository.findByClientIdAndStatus(
                        clientId,
                        AssignmentStatus.COMPLETED
                ).orElseThrow();

        assertThat(updatedAssignment.getStatus())
                .isEqualTo(AssignmentStatus.COMPLETED);
        assertThat(updatedAssignment.getReleasedAt())
                .isNotNull();

        Manager updatedManager = managerRepository.findById(manager.getId())
                .orElseThrow();

        assertThat(updatedManager.getCurrentLoad())
                .isZero();
        assertThat(updatedManager.getStatus())
                .isEqualTo(ManagerStatus.ACTIVE);

        List<OutboxEvent> outboxEvents = outboxRepository.findAll();

        assertThat(outboxEvents).hasSize(1);

        OutboxEvent outboxEvent = outboxEvents.get(0);

        assertThat(outboxEvent.getEventType())
                .isEqualTo(OutboxEventType.CLIENT_RELEASED);
        assertThat(outboxEvent.getPartitionKey())
                .isEqualTo(clientId);
        assertThat(outboxEvent.getStatus())
                .isEqualTo(OutboxEventStatus.NEW);
        assertThat(outboxEvent.getRetryCount())
                .isEqualTo(0);
        assertThat(outboxEvent.getPayload())
                .contains("\"clientId\":100")
                .contains("\"managerId\":" + manager.getId());
    }

    @Test
    void releaseClient_shouldThrowException_whenActiveAssignmentDoesNotExist() {
        Long clientId = 100L;

        assertThatThrownBy(() ->
                clientAssignmentService.releaseClient(clientId)
        )
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Active assignment not found for client ID: " + clientId);

        assertThat(outboxRepository.findAll())
                .isEmpty();
    }

    private Manager saveManager(
            String firstName,
            String lastName,
            String email,
            int maxCapacity,
            int currentLoad,
            ManagerStatus status
    ) {
        Manager manager = Manager.builder()
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .phone("+375291234567")
                .maxCapacity(maxCapacity)
                .currentLoad(currentLoad)
                .status(status)
                .build();

        return managerRepository.saveAndFlush(manager);
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper()
                    .registerModule(new JavaTimeModule());
        }
    }
}
