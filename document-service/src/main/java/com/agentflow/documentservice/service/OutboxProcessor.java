package com.agentflow.documentservice.service;

import com.agentflow.documentservice.config.KafkaConfig;
import com.agentflow.documentservice.entity.outbox.OutboxEvent;
import com.agentflow.documentservice.entity.outbox.OutboxEventStatus;
import com.agentflow.documentservice.entity.outbox.OutboxEventType;
import com.agentflow.documentservice.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxProcessor {
    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void processOutboxEvents() {
        List<OutboxEvent> outboxEventList = outboxRepository.findTop10ByStatusOrderByCreatedAtAsc(OutboxEventStatus.NEW);

        if (outboxEventList.isEmpty()) {
            return;
        }

        for (OutboxEvent event : outboxEventList) {
            try {
                String targetTopic = resolveTopic(event.getEventType());

                kafkaTemplate.send(
                        targetTopic,
                        String.valueOf(event.getPartitionKey()),
                        event.getPayload()
                ).get(5, TimeUnit.SECONDS);

                event.setStatus(OutboxEventStatus.SENT);
                log.info("Successfully sent Outbox event ID: {}, type: {}", event.getId(), event.getEventType());

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Sending thread interrupted for Outbox event ID: {}", event.getId(), e);
                break;
            } catch (ExecutionException | TimeoutException e) {
                log.error("Failed to send Outbox event ID: {}", event.getId(), e);

                event.setRetryCount(event.getRetryCount() + 1);
                if (event.getRetryCount() >= 3) {
                    event.setStatus(OutboxEventStatus.FAILED);
                    log.warn("Outbox event ID: {} marked as FAILED after max retries", event.getId());
                }
            }
            outboxRepository.save(event);
        }
    }

    private String resolveTopic(OutboxEventType eventType) {
        if (eventType == OutboxEventType.DOCUMENT_UPLOADED) {
            return KafkaConfig.DOCUMENT_UPLOADED_TOPIC;
        }
        return KafkaConfig.DOCUMENT_UPLOADED_TOPIC;
    }

}
