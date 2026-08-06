package com.agentflow.clientservice.unit;

import com.agentflow.clientservice.config.KafkaConfig;
import com.agentflow.clientservice.entity.outbox.OutboxEvent;
import com.agentflow.clientservice.entity.outbox.OutboxEventStatus;
import com.agentflow.clientservice.repository.OutboxRepository;
import com.agentflow.clientservice.service.OutboxProcessor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxProcessorTest {

    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    private OutboxProcessor outboxProcessor;

    @Test
    void processOutboxEvents_shouldSendEventAndSetStatusToSent_whenEventExists() {
        OutboxEvent event = OutboxEvent.builder()
                .id(1L)
                .partitionKey(100L)
                .payload("{\"clientId\": 100, \"name\": \"Ivan\"}")
                .status(OutboxEventStatus.NEW)
                .retryCount(0)
                .build();

        when(outboxRepository.findTop10ByStatusOrderByCreatedAtAsc(OutboxEventStatus.NEW))
                .thenReturn(List.of(event));

        when(kafkaTemplate.send(
                eq(KafkaConfig.CLIENT_CREATED_TOPIC),
                eq("100"),
                eq(event.getPayload())
        )).thenReturn(CompletableFuture.completedFuture(null));

        outboxProcessor.processOutboxEvents();

        assert event.getStatus() == OutboxEventStatus.SENT;

        verify(outboxRepository, times(1)).save(event);

        verify(kafkaTemplate, times(1)).send(
                KafkaConfig.CLIENT_CREATED_TOPIC,
                "100",
                "{\"clientId\": 100, \"name\": \"Ivan\"}"
        );
    }

    @Test
    void processOutboxEvents_shouldDoNothing_whenNoEventsFound() {
        when(outboxRepository.findTop10ByStatusOrderByCreatedAtAsc(OutboxEventStatus.NEW))
                .thenReturn(Collections.emptyList());

        outboxProcessor.processOutboxEvents();

        verifyNoInteractions(kafkaTemplate);
        verify(outboxRepository, never()).save(any());
    }
}

