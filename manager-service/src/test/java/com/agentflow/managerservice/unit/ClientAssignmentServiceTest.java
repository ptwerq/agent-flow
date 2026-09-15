package com.agentflow.managerservice.unit;

import com.agentflow.managerservice.dto.event.ClientAssignedEvent;
import com.agentflow.managerservice.dto.event.ClientReleasedEvent;
import com.agentflow.managerservice.entity.AssignmentStatus;
import com.agentflow.managerservice.entity.ClientAssignment;
import com.agentflow.managerservice.entity.Manager;
import com.agentflow.managerservice.entity.ManagerStatus;
import com.agentflow.managerservice.entity.outbox.OutboxEvent;
import com.agentflow.managerservice.exception.NoAvailableManagerException;
import com.agentflow.managerservice.mapper.ClientAssignmentMapper;
import com.agentflow.managerservice.mapper.OutboxMapper;
import com.agentflow.managerservice.repository.ClientAssignmentRepository;
import com.agentflow.managerservice.repository.OutboxRepository;
import com.agentflow.managerservice.exception.NotFoundException;
import com.agentflow.managerservice.service.ClientAssignmentService;
import com.agentflow.managerservice.service.ManagerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientAssignmentServiceTest {

    @Mock
    private ManagerService managerService;

    @Mock
    private ClientAssignmentMapper clientAssignmentMapper;

    @Mock
    private ClientAssignmentRepository clientAssignmentRepository;

    @Mock
    private OutboxMapper outboxMapper;

    @Mock
    private OutboxRepository outboxRepository;

    @InjectMocks
    private ClientAssignmentService clientAssignmentService;

    private Manager manager;
    private ClientAssignment assignment;
    private OutboxEvent outboxEvent;

    @BeforeEach
    void setUp() {
        manager = Manager.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .phone("+375291234567")
                .status(ManagerStatus.ACTIVE)
                .maxCapacity(10)
                .currentLoad(2)
                .isDeleted(false)
                .build();

        assignment = ClientAssignment.builder()
                .id(1L)
                .clientId(100L)
                .manager(manager)
                .status(AssignmentStatus.ACTIVE)
                .build();

        outboxEvent = OutboxEvent.builder()
                .id(1L)
                .build();
    }

    @Test
    void assignClient_Skips_WhenClientAlreadyAssigned() {
        when(clientAssignmentRepository.existsByClientId(100L))
                .thenReturn(true);

        clientAssignmentService.assignClient(100L);

        verify(clientAssignmentRepository).existsByClientId(100L);
        verify(managerService, never()).findLeastLoadedAvailableManager();
        verify(managerService, never()).increaseLoad(1L);
        verify(clientAssignmentRepository, never()).save(assignment);
        verify(outboxRepository, never()).save(outboxEvent);
    }

    @Test
    void assignClient_CreatesAssignmentAndOutboxEvent() {
        when(clientAssignmentRepository.existsByClientId(100L))
                .thenReturn(false);
        when(managerService.findLeastLoadedAvailableManager())
                .thenReturn(manager);
        when(clientAssignmentMapper.toEntity(100L, manager))
                .thenReturn(assignment);
        when(outboxMapper.toEntity(
                org.mockito.ArgumentMatchers.any(
                        ClientAssignedEvent.class
                ),
                org.mockito.ArgumentMatchers.eq(100L)
        )).thenReturn(outboxEvent);

        clientAssignmentService.assignClient(100L);

        verify(managerService).findLeastLoadedAvailableManager();
        verify(managerService).increaseLoad(1L);
        verify(clientAssignmentMapper).toEntity(100L, manager);
        verify(clientAssignmentRepository).save(assignment);
        verify(outboxRepository).save(outboxEvent);
    }

    @Test
    void assignClient_DoesNotCreateAssignment_WhenNoManagerAvailable() {
        when(clientAssignmentRepository.existsByClientId(100L))
                .thenReturn(false);

        when(managerService.findLeastLoadedAvailableManager())
                .thenThrow(new NoAvailableManagerException(
                        "No available manager found"
                ));

        assertThrows(
                NoAvailableManagerException.class,
                () -> clientAssignmentService.assignClient(100L)
        );

        verify(managerService).findLeastLoadedAvailableManager();
        verify(managerService, never()).increaseLoad(1L);
        verify(clientAssignmentRepository, never()).save(assignment);
        verify(outboxRepository, never()).save(outboxEvent);
    }

    @Test
    void releaseClient_CompletesAssignmentAndCreatesOutboxEvent() {
        LocalDateTime before = LocalDateTime.now();

        when(clientAssignmentRepository
                .findByClientIdAndStatus(100L, AssignmentStatus.ACTIVE))
                .thenReturn(Optional.of(assignment));

        when(outboxMapper.toEntity(
                org.mockito.ArgumentMatchers.any(
                        ClientReleasedEvent.class
                ),
                org.mockito.ArgumentMatchers.eq(100L)
        )).thenReturn(outboxEvent);

        clientAssignmentService.releaseClient(100L);

        LocalDateTime after = LocalDateTime.now();

        assertEquals(AssignmentStatus.COMPLETED, assignment.getStatus());
        assertEquals(1L, assignment.getManager().getId());

        assertTrue(!assignment.getReleasedAt().isBefore(before)
                && !assignment.getReleasedAt().isAfter(after));

        verify(clientAssignmentRepository).save(assignment);
        verify(managerService).decreaseLoad(1L);
        verify(outboxRepository).save(outboxEvent);
    }

    @Test
    void releaseClient_ThrowsException_WhenActiveAssignmentNotFound() {
        when(clientAssignmentRepository
                .findByClientIdAndStatus(100L, AssignmentStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThrows(
                NotFoundException.class,
                () -> clientAssignmentService.releaseClient(100L)
        );

        verify(clientAssignmentRepository)
                .findByClientIdAndStatus(100L, AssignmentStatus.ACTIVE);

        verify(managerService, never()).decreaseLoad(1L);
        verify(outboxRepository, never()).save(outboxEvent);
    }
}