package com.agentflow.managerservice.service;

import com.agentflow.managerservice.config.KafkaConfig;
import com.agentflow.managerservice.dto.event.ClientAssignedEvent;
import com.agentflow.managerservice.dto.event.ClientReleasedEvent;
import com.agentflow.managerservice.entity.AssignmentStatus;
import com.agentflow.managerservice.entity.ClientAssignment;
import com.agentflow.managerservice.entity.Manager;
import com.agentflow.managerservice.exception.NotFoundException;
import com.agentflow.managerservice.mapper.ClientAssignmentMapper;
import com.agentflow.managerservice.repository.ClientAssignmentRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientAssignmentServiceTest {

    @Mock
    private ManagerService managerService;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private ClientAssignmentMapper clientAssignmentMapper;

    @Mock
    private ClientAssignmentRepository clientAssignmentRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ClientAssignmentService clientAssignmentService;

    private final Long clientId = 100L;
    private final Long managerId = 1L;
    private Manager manager;
    private ClientAssignment assignment;

    @BeforeEach
    void setUp() {
        manager = Manager.builder()
                .id(managerId)
                .firstName("John")
                .lastName("Doe")
                .build();

        assignment = ClientAssignment.builder()
                .id(10L)
                .clientId(clientId)
                .manager(manager)
                .status(AssignmentStatus.ACTIVE)
                .build();
    }

    @Nested
    @DisplayName("Assign Client Tests")
    class AssignClientTests {

        @Test
        @DisplayName("Should successfully assign client and send Kafka event")
        void assignClient_Success() throws JsonProcessingException {
            when(clientAssignmentRepository.existsByClientId(clientId)).thenReturn(false);
            when(managerService.findLeastLoadedAvailableManager()).thenReturn(manager);
            when(clientAssignmentMapper.toEntity(clientId, manager)).thenReturn(assignment);
            when(objectMapper.writeValueAsString(any(ClientAssignedEvent.class))).thenReturn("{}");

            clientAssignmentService.assignClient(clientId);

            verify(managerService).increaseLoad(managerId);
            verify(clientAssignmentRepository).save(assignment);
            verify(kafkaTemplate).send(eq(KafkaConfig.CLIENT_ASSIGNED_TOPIC), eq(String.valueOf(clientId)), eq("{}"));
        }

        @Test
        @DisplayName("Should skip processing if client assignment record already exists")
        void assignClient_DuplicateRecord_SkipsAssignment() {
            when(clientAssignmentRepository.existsByClientId(clientId)).thenReturn(true);

            clientAssignmentService.assignClient(clientId);

            verify(managerService, never()).findLeastLoadedAvailableManager();
            verify(managerService, never()).increaseLoad(anyLong());
            verify(clientAssignmentRepository, never()).save(any());
            verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("Should throw RuntimeException when JSON serialization fails")
        void assignClient_SerializationError_ThrowsException() throws JsonProcessingException {
            when(clientAssignmentRepository.existsByClientId(clientId)).thenReturn(false);
            when(managerService.findLeastLoadedAvailableManager()).thenReturn(manager);
            when(clientAssignmentMapper.toEntity(clientId, manager)).thenReturn(assignment);
            when(objectMapper.writeValueAsString(any(ClientAssignedEvent.class)))
                    .thenThrow(new JsonProcessingException("Serialization error") {});

            assertThatThrownBy(() -> clientAssignmentService.assignClient(clientId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Error serializing Kafka event");

            verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("Release Client Tests")
    class ReleaseClientTests {

        @Test
        @DisplayName("Should successfully release client and send Kafka event")
        void releaseClient_Success() throws JsonProcessingException {
            when(clientAssignmentRepository.findByClientIdAndStatus(clientId, AssignmentStatus.ACTIVE))
                    .thenReturn(Optional.of(assignment));
            when(objectMapper.writeValueAsString(any(ClientReleasedEvent.class))).thenReturn("{}");

            clientAssignmentService.releaseClient(clientId);

            verify(clientAssignmentRepository).save(assignment);
            verify(managerService).decreaseLoad(managerId);
            verify(kafkaTemplate).send(eq(KafkaConfig.CLIENT_RELEASED_TOPIC), eq(String.valueOf(clientId)), eq("{}"));
        }

        @Test
        @DisplayName("Should throw NotFoundException when active assignment not found")
        void releaseClient_NotFound_ThrowsException() {
            when(clientAssignmentRepository.findByClientIdAndStatus(clientId, AssignmentStatus.ACTIVE))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> clientAssignmentService.releaseClient(clientId))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Active assignment not found for client ID: " + clientId);

            verify(managerService, never()).decreaseLoad(anyLong());
            verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
        }
    }
}
