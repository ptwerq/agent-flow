package com.agentflow.managerservice.unit;

import com.agentflow.managerservice.config.KafkaConfig;
import com.agentflow.managerservice.entity.outbox.OutboxEvent;
import com.agentflow.managerservice.entity.outbox.OutboxEventStatus;
import com.agentflow.managerservice.entity.outbox.OutboxEventType;
import com.agentflow.managerservice.repository.OutboxRepository;
import com.agentflow.managerservice.service.OutboxProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxProcessorTest {

    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private OutboxProcessor outboxProcessor;

    private OutboxEvent event;

    @BeforeEach
    void setUp() {
        event = OutboxEvent.builder()
                .id(1L)
                .payload("{\"clientId\":100}")
                .eventType(OutboxEventType.CLIENT_ASSIGNED)
                .partitionKey(100L)
                .status(OutboxEventStatus.NEW)
                .retryCount(0)
                .build();
    }

    @Test
    void processOutboxEvents_DoesNothing_WhenNoEvents() {
        when(outboxRepository.findTop10ByStatusOrderByCreatedAtAsc(
                OutboxEventStatus.NEW
        )).thenReturn(List.of());

        outboxProcessor.processOutboxEvents();

        verify(kafkaTemplate, never())
                .send(anyString(), anyString(), anyString());

        verify(outboxRepository, never()).save(event);
    }

    @Test
    void processOutboxEvents_MarksEventAsSent_WhenKafkaSendSucceeds()
            throws Exception {

        when(outboxRepository.findTop10ByStatusOrderByCreatedAtAsc(
                OutboxEventStatus.NEW
        )).thenReturn(List.of(event));

        CompletableFuture<SendResult<String, Object>> future =
                CompletableFuture.completedFuture(null);

        when(kafkaTemplate.send(
                eq(KafkaConfig.CLIENT_ASSIGNED_TOPIC),
                eq("100"),
                eq(event.getPayload())
        )).thenReturn(future);

        outboxProcessor.processOutboxEvents();

        assertEquals(OutboxEventStatus.SENT, event.getStatus());
        assertEquals(0, event.getRetryCount());

        verify(kafkaTemplate).send(
                KafkaConfig.CLIENT_ASSIGNED_TOPIC,
                "100",
                event.getPayload()
        );

        verify(outboxRepository).save(event);
    }

    @Test
    void processOutboxEvents_IncrementsRetryCount_WhenKafkaSendFails()
            throws Exception {

        when(outboxRepository.findTop10ByStatusOrderByCreatedAtAsc(
                OutboxEventStatus.NEW
        )).thenReturn(List.of(event));

        CompletableFuture<SendResult<String, Object>> failedFuture =
                new CompletableFuture<>();

        failedFuture.completeExceptionally(
                new RuntimeException("Kafka error")
        );

        when(kafkaTemplate.send(
                eq(KafkaConfig.CLIENT_ASSIGNED_TOPIC),
                eq("100"),
                eq(event.getPayload())
        )).thenReturn(failedFuture);

        outboxProcessor.processOutboxEvents();

        assertEquals(1, event.getRetryCount());
        assertEquals(OutboxEventStatus.NEW, event.getStatus());

        verify(outboxRepository).save(event);
    }

    @Test
    void processOutboxEvents_MarksEventAsFailed_AfterThirdRetry()
            throws Exception {

        event.setRetryCount(2);

        when(outboxRepository.findTop10ByStatusOrderByCreatedAtAsc(
                OutboxEventStatus.NEW
        )).thenReturn(List.of(event));

        CompletableFuture<SendResult<String, Object>> failedFuture =
                new CompletableFuture<>();

        failedFuture.completeExceptionally(
                new RuntimeException("Kafka error")
        );

        when(kafkaTemplate.send(
                eq(KafkaConfig.CLIENT_ASSIGNED_TOPIC),
                eq("100"),
                eq(event.getPayload())
        )).thenReturn(failedFuture);

        outboxProcessor.processOutboxEvents();

        assertEquals(3, event.getRetryCount());
        assertEquals(OutboxEventStatus.FAILED, event.getStatus());

        verify(outboxRepository).save(event);
    }

    @Test
    void processOutboxEvents_UsesReleasedTopic_ForReleasedEvent()
            throws Exception {

        event.setEventType(OutboxEventType.CLIENT_RELEASED);

        when(outboxRepository.findTop10ByStatusOrderByCreatedAtAsc(
                OutboxEventStatus.NEW
        )).thenReturn(List.of(event));

        CompletableFuture<SendResult<String, Object>> future =
                CompletableFuture.completedFuture(null);

        when(kafkaTemplate.send(
                eq(KafkaConfig.CLIENT_RELEASED_TOPIC),
                eq("100"),
                eq(event.getPayload())
        )).thenReturn(future);

        outboxProcessor.processOutboxEvents();

        assertEquals(OutboxEventStatus.SENT, event.getStatus());

        verify(kafkaTemplate).send(
                KafkaConfig.CLIENT_RELEASED_TOPIC,
                "100",
                event.getPayload()
        );
    }
}