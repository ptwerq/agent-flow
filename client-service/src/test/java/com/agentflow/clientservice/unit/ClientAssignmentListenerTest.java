package com.agentflow.clientservice.unit;

import com.agentflow.clientservice.dto.event.ClientAssignedEvent;
import com.agentflow.clientservice.dto.event.ClientReleasedEvent;
import com.agentflow.clientservice.listener.ClientAssignmentListener;
import com.agentflow.clientservice.service.ClientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ClientAssignmentListenerTest {

    @Mock
    private ClientService clientService;

    @Mock
    private Acknowledgment acknowledgment;

    @InjectMocks
    private ClientAssignmentListener listener;

    private ClientAssignedEvent assignedEvent;
    private ClientReleasedEvent releasedEvent;

    @BeforeEach
    void setUp() {
        assignedEvent = new ClientAssignedEvent(
                1L,
                10L
        );

        releasedEvent = new ClientReleasedEvent(
                1L,
                10L,
                LocalDateTime.now()
        );
    }

    @Test
    void handleClientAssigned_Success_AssignsManagerAndAcknowledges() {
        listener.handleClientAssigned(assignedEvent, acknowledgment);

        verify(clientService).assignManager(1L, 10L);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void handleClientAssigned_WhenServiceFails_DoesNotAcknowledge() {
        doThrow(new RuntimeException("Client not found"))
                .when(clientService)
                .assignManager(1L, 10L);

        assertThrows(
                RuntimeException.class,
                () -> listener.handleClientAssigned(assignedEvent, acknowledgment)
        );

        verify(clientService).assignManager(1L, 10L);
        verify(acknowledgment, never()).acknowledge();
    }

    @Test
    void handleClientReleased_Success_ReleasesManagerAndAcknowledges() {
        listener.handleClientReleased(releasedEvent, acknowledgment);

        verify(clientService).releaseManager(1L);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void handleClientReleased_WhenServiceFails_DoesNotAcknowledge() {
        doThrow(new RuntimeException("Client not found"))
                .when(clientService)
                .releaseManager(1L);

        assertThrows(
                RuntimeException.class,
                () -> listener.handleClientReleased(releasedEvent, acknowledgment)
        );

        verify(clientService).releaseManager(1L);
        verify(acknowledgment, never()).acknowledge();
    }
}