package com.agentflow.clientservice.service;

import com.agentflow.clientservice.config.KafkaConfig;
import com.agentflow.clientservice.entity.outbox.OutboxEvent;
import com.agentflow.clientservice.entity.outbox.OutboxEventStatus;
import com.agentflow.clientservice.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxProcessor {
    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void processOutboxEvents() {
        List<OutboxEvent> outboxEventList = outboxRepository.findTop10ByStatusOrderByCreatedAtAsc(OutboxEventStatus.NEW);
        if (outboxEventList.isEmpty()) {
            return;
        }

        for (var event : outboxEventList) {
            kafkaTemplate.send(KafkaConfig.CLIENT_CREATED_TOPIC,
                            String.valueOf(event.getPartitionKey()),
                            event.getPayload())
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            event.setStatus(OutboxEventStatus.SENT);
                            outboxRepository.save(event);
                            log.info("Event has been successfully sent: {}", event.getPartitionKey());
                        } else {
                            log.error("Failed to send event id={}: {}", event.getId(), ex.getMessage(), ex);
                            event.setRetryCount(event.getRetryCount() + 1);
                            if (event.getRetryCount() >= 3) {
                                event.setStatus(OutboxEventStatus.FAILED);
                            }
                            outboxRepository.save(event);
                        }

                    });
        }
    }
}
