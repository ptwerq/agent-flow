package com.agentflow.clientservice.unit;

import com.agentflow.clientservice.config.KafkaConfig;
import com.agentflow.clientservice.entity.outbox.OutboxEvent;
import com.agentflow.clientservice.entity.outbox.OutboxEventStatus;
import com.agentflow.clientservice.entity.outbox.OutboxEventType;
import com.agentflow.clientservice.repository.OutboxRepository;
import com.agentflow.clientservice.service.OutboxProcessor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;


import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxProcessorTest {

    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private CompletableFuture<SendResult<String, Object>> kafkaFuture;

    @InjectMocks
    private OutboxProcessor outboxProcessor;

    @Test
    void processOutboxEvents_NoEvents_DoesNothing() {
        when(outboxRepository.findTop10ByStatusOrderByCreatedAtAsc(
                OutboxEventStatus.NEW
        )).thenReturn(List.of());

        outboxProcessor.processOutboxEvents();

        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
        verify(outboxRepository, never()).save(any());
    }

    @Test
    void processOutboxEvents_Success_MarksEventAsSent() throws Exception {
        OutboxEvent event = createEvent();

        when(outboxRepository.findTop10ByStatusOrderByCreatedAtAsc(
                OutboxEventStatus.NEW
        )).thenReturn(List.of(event));

        when(kafkaTemplate.send(
                eq(KafkaConfig.CLIENT_CREATED_TOPIC),
                eq("1"),
                eq("client payload")
        )).thenReturn(kafkaFuture);

        when(kafkaFuture.get(5, TimeUnit.SECONDS))
                .thenReturn(null);

        outboxProcessor.processOutboxEvents();

        assertEquals(OutboxEventStatus.SENT, event.getStatus());

        verify(kafkaTemplate).send(
                KafkaConfig.CLIENT_CREATED_TOPIC,
                "1",
                "client payload"
        );

        verify(outboxRepository).save(event);
    }

    @Test
    void processOutboxEvents_ExecutionException_IncrementsRetryCount() throws Exception {
        OutboxEvent event = createEvent();
        event.setRetryCount(0);

        when(outboxRepository.findTop10ByStatusOrderByCreatedAtAsc(
                OutboxEventStatus.NEW
        )).thenReturn(List.of(event));

        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(kafkaFuture);

        when(kafkaFuture.get(5, TimeUnit.SECONDS))
                .thenThrow(new ExecutionException(new RuntimeException("Kafka error")));

        outboxProcessor.processOutboxEvents();

        assertEquals(1, event.getRetryCount());
        assertEquals(OutboxEventStatus.NEW, event.getStatus());

        verify(outboxRepository).save(event);
    }

    @Test
    void processOutboxEvents_ThirdFailure_MarksEventAsFailed() throws Exception {
        OutboxEvent event = createEvent();
        event.setRetryCount(2);

        when(outboxRepository.findTop10ByStatusOrderByCreatedAtAsc(
                OutboxEventStatus.NEW
        )).thenReturn(List.of(event));

        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(kafkaFuture);

        when(kafkaFuture.get(5, TimeUnit.SECONDS))
                .thenThrow(new ExecutionException(new RuntimeException("Kafka error")));

        outboxProcessor.processOutboxEvents();

        assertEquals(3, event.getRetryCount());
        assertEquals(OutboxEventStatus.FAILED, event.getStatus());

        verify(outboxRepository).save(event);
    }

    @Test
    void processOutboxEvents_TimeoutException_IncrementsRetryCount() throws Exception {
        OutboxEvent event = createEvent();
        event.setRetryCount(0);

        when(outboxRepository.findTop10ByStatusOrderByCreatedAtAsc(
                OutboxEventStatus.NEW
        )).thenReturn(List.of(event));

        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(kafkaFuture);

        when(kafkaFuture.get(5, TimeUnit.SECONDS))
                .thenThrow(new TimeoutException("Kafka timeout"));

        outboxProcessor.processOutboxEvents();

        assertEquals(1, event.getRetryCount());
        assertEquals(OutboxEventStatus.NEW, event.getStatus());

        verify(outboxRepository).save(event);
    }

    @Test
    void processOutboxEvents_InterruptedException_RestoresInterruptFlagAndStopsProcessing()
            throws Exception {

        OutboxEvent firstEvent = createEvent();
        OutboxEvent secondEvent = createEvent();
        secondEvent.setId(2L);

        when(outboxRepository.findTop10ByStatusOrderByCreatedAtAsc(
                OutboxEventStatus.NEW
        )).thenReturn(List.of(firstEvent, secondEvent));

        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(kafkaFuture);

        when(kafkaFuture.get(5, TimeUnit.SECONDS))
                .thenThrow(new InterruptedException("Thread interrupted"));

        try {
            outboxProcessor.processOutboxEvents();

            assertFalse(firstEvent.getStatus() == OutboxEventStatus.SENT);

            verify(kafkaTemplate, times(1))
                    .send(anyString(), anyString(), any());

            verify(outboxRepository, never()).save(any());

        } finally {
            Thread.interrupted();
        }
    }

    private OutboxEvent createEvent() {
        return OutboxEvent.builder()
                .id(1L)
                .eventType(OutboxEventType.CLIENT_CREATED)
                .partitionKey(1L)
                .payload("client payload")
                .status(OutboxEventStatus.NEW)
                .retryCount(0)
                .build();
    }
}
