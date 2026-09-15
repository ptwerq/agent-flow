package com.agentflow.clientservice.unit;

import com.agentflow.clientservice.dto.event.ClientCreatedEvent;
import com.agentflow.clientservice.entity.outbox.OutboxEvent;
import com.agentflow.clientservice.entity.outbox.OutboxEventStatus;
import com.agentflow.clientservice.entity.outbox.OutboxEventType;
import com.agentflow.clientservice.mapper.OutboxMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxMapperTest {

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private OutboxMapper outboxMapper;

    private ClientCreatedEvent event;
    private Long clientId;

    @BeforeEach
    void setUp() {
        clientId = 1L;

        event = new ClientCreatedEvent(
                clientId,
                "Name",
                "Surname",
                "name@gmail.com",
                "+375291234567",
                LocalDateTime.now()
        );
    }

    @Test
    void toEntity_Success_CreatesOutboxEvent() throws Exception {
        String jsonPayload = "{\"clientId\":1}";

        when(objectMapper.writeValueAsString(event))
                .thenReturn(jsonPayload);

        OutboxEvent result = outboxMapper.toEntity(event, clientId);

        assertEquals(OutboxEventType.CLIENT_CREATED, result.getEventType());
        assertEquals(clientId, result.getPartitionKey());
        assertEquals(jsonPayload, result.getPayload());
        assertEquals(OutboxEventStatus.NEW, result.getStatus());
        assertEquals(0, result.getRetryCount());

        verify(objectMapper).writeValueAsString(event);
    }

    @Test
    void toEntity_WhenSerializationFails_ThrowsIllegalStateException()
            throws Exception {

        when(objectMapper.writeValueAsString(event))
                .thenThrow(new JsonProcessingException("Serialization error") {});

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> outboxMapper.toEntity(event, clientId)
        );

        assertEquals(
                "Error with serializing event ClientCreatedEvent to JSON",
                exception.getMessage()
        );

        verify(objectMapper).writeValueAsString(event);
    }
}