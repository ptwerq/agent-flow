package com.agentflow.clientservice.integration;

import com.agentflow.clientservice.entity.outbox.OutboxEvent;
import com.agentflow.clientservice.entity.outbox.OutboxEventStatus;
import com.agentflow.clientservice.entity.outbox.OutboxEventType;
import com.agentflow.clientservice.repository.OutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class OutboxRepositoryIntegrationTest {

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
    private OutboxRepository outboxRepository;


    @BeforeEach
    void setUp() {
        outboxRepository.deleteAll();
    }

    @Test
    void findTop10ByStatusOrderByCreatedAtAsc_ReturnsOnlyNewEventsInCorrectOrder() {

        OutboxEvent firstEvent = createEvent(
                OutboxEventStatus.NEW,
                1L
        );

        OutboxEvent secondEvent = createEvent(
                OutboxEventStatus.NEW,
                2L
        );

        OutboxEvent sentEvent = createEvent(
                OutboxEventStatus.SENT,
                3L
        );

        outboxRepository.save(firstEvent);
        outboxRepository.save(secondEvent);
        outboxRepository.save(sentEvent);

        List<OutboxEvent> result =
                outboxRepository.findTop10ByStatusOrderByCreatedAtAsc(
                        OutboxEventStatus.NEW
                );

        assertEquals(2, result.size());

        assertEquals(
                firstEvent.getId(),
                result.get(0).getId()
        );

        assertEquals(
                secondEvent.getId(),
                result.get(1).getId()
        );

        assertTrue(
                result.stream()
                        .allMatch(event ->
                                event.getStatus() == OutboxEventStatus.NEW
                        )
        );
    }

    @Test
    void findTop10ByStatusOrderByCreatedAtAsc_ReturnsMaximum10Events() {

        for (int i = 1; i <= 12; i++) {
            outboxRepository.save(
                    createEvent(
                            OutboxEventStatus.NEW,
                            (long) i
                    )
            );
        }

        List<OutboxEvent> result =
                outboxRepository.findTop10ByStatusOrderByCreatedAtAsc(
                        OutboxEventStatus.NEW
                );

        assertEquals(10, result.size());
    }

    @Test
    void save_PersistsEventToDatabase() {

        OutboxEvent event = createEvent(
                OutboxEventStatus.NEW,
                1L
        );

        OutboxEvent savedEvent = outboxRepository.save(event);

        assertNotNull(savedEvent.getId());
        assertNotNull(savedEvent.getCreatedAt());

        OutboxEvent foundEvent = outboxRepository
                .findById(savedEvent.getId())
                .orElseThrow();

        assertEquals(savedEvent.getId(), foundEvent.getId());
        assertEquals("client payload", foundEvent.getPayload());
        assertEquals(
                OutboxEventType.CLIENT_CREATED,
                foundEvent.getEventType()
        );
        assertEquals(1L, foundEvent.getPartitionKey());
        assertEquals(
                OutboxEventStatus.NEW,
                foundEvent.getStatus()
        );
        assertEquals(0, foundEvent.getRetryCount());
    }

    private OutboxEvent createEvent(
            OutboxEventStatus status,
            Long partitionKey
    ) {
        return OutboxEvent.builder()
                .payload("client payload")
                .eventType(OutboxEventType.CLIENT_CREATED)
                .partitionKey(partitionKey)
                .status(status)
                .retryCount(0)
                .build();
    }

}
