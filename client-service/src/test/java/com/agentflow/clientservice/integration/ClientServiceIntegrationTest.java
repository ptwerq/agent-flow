package com.agentflow.clientservice.integration;

import com.agentflow.clientservice.dto.request.ClientRequest;
import com.agentflow.clientservice.dto.request.ClientStatusUpdateRequest;
import com.agentflow.clientservice.dto.request.ClientUpdateRequest;
import com.agentflow.clientservice.dto.response.ClientResponse;
import com.agentflow.clientservice.entity.Client;
import com.agentflow.clientservice.entity.DealStatus;
import com.agentflow.clientservice.entity.outbox.OutboxEvent;
import com.agentflow.clientservice.entity.outbox.OutboxEventStatus;
import com.agentflow.clientservice.mapper.ClientMapperImpl;
import com.agentflow.clientservice.mapper.OutboxMapper;
import com.agentflow.clientservice.repository.ClientRepository;
import com.agentflow.clientservice.repository.OutboxRepository;
import com.agentflow.clientservice.service.ClientService;
import com.agentflow.clientservice.exception.NotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
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
@Import({
        ClientService.class,
        ClientMapperImpl.class,
        OutboxMapper.class
})
class ClientServiceIntegrationTest {

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
    private ClientService clientService;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private OutboxRepository outboxRepository;

    @BeforeEach
    void setUp() {
        outboxRepository.deleteAll();
        clientRepository.deleteAll();
    }

    @Test
    void create_SavesClientAndCreatesOutboxEvent() {
        ClientRequest request = ClientRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.create@example.com")
                .phone("+375291111111")
                .build();

        ClientResponse response = clientService.create(request);

        assertNotNull(response.getId());
        assertEquals("John", response.getFirstName());
        assertEquals("Doe", response.getLastName());
        assertEquals("john.create@example.com", response.getEmail());
        assertEquals("+375291111111", response.getPhone());
        assertEquals(DealStatus.NEW, response.getDealStatus());

        Client savedClient = clientRepository
                .findById(response.getId())
                .orElseThrow();

        assertEquals("John", savedClient.getFirstName());
        assertFalse(savedClient.isDeleted());

        List<OutboxEvent> events = outboxRepository.findAll();

        assertEquals(1, events.size());

        OutboxEvent event = events.get(0);

        assertEquals(OutboxEventStatus.NEW, event.getStatus());
        assertEquals(response.getId(), event.getPartitionKey());
        assertNotNull(event.getPayload());
    }

    @Test
    void assignManager_UpdatesManagerAndStatus() {
        Client client = saveClient();

        clientService.assignManager(client.getId(), 100L);

        Client updatedClient = clientRepository
                .findById(client.getId())
                .orElseThrow();

        assertEquals(100L, updatedClient.getManagerId());
        assertEquals(DealStatus.IN_PROGRESS, updatedClient.getDealStatus());
    }

    @Test
    void releaseManager_RemovesManagerAndSetsSuccessStatus() {
        Client client = saveClient();

        client.setManagerId(100L);
        client.setDealStatus(DealStatus.IN_PROGRESS);
        clientRepository.save(client);

        clientService.releaseManager(client.getId());

        Client updatedClient = clientRepository
                .findById(client.getId())
                .orElseThrow();

        assertNull(updatedClient.getManagerId());
        assertEquals(DealStatus.SUCCESS, updatedClient.getDealStatus());
    }

    @Test
    void delete_MarksClientAsDeleted() {
        Client client = saveClient();

        clientService.delete(client.getId());

        Client deletedClient = clientRepository
                .findById(client.getId())
                .orElseThrow();

        assertTrue(deletedClient.isDeleted());
    }

    @Test
    void getById_ReturnsClient() {
        Client client = saveClient();

        ClientResponse response = clientService.getById(client.getId());

        assertEquals(client.getId(), response.getId());
        assertEquals(client.getFirstName(), response.getFirstName());
        assertEquals(client.getEmail(), response.getEmail());
    }

    @Test
    void getById_ThrowsException_WhenClientIsDeleted() {
        Client client = saveClient();

        client.setDeleted(true);
        clientRepository.save(client);

        assertThrows(
                NotFoundException.class,
                () -> clientService.getById(client.getId())
        );
    }

    @Test
    void getAll_ReturnsOnlyNotDeletedClients() {
        Client activeClient = saveClient();

        Client deletedClient = saveClient(
                "deleted@example.com",
                "+375292222222"
        );
        deletedClient.setDeleted(true);
        clientRepository.save(deletedClient);

        List<ClientResponse> result = clientService.getAll();

        assertEquals(1, result.size());
        assertEquals(activeClient.getId(), result.get(0).getId());
    }

    @Test
    void update_ChangesClientData() {
        Client client = saveClient();

        ClientUpdateRequest request = ClientUpdateRequest.builder()
                .firstName("Updated")
                .lastName("Name")
                .email("updated@example.com")
                .phone("+375293333333")
                .build();

        ClientResponse response =
                clientService.update(client.getId(), request);

        assertEquals("Updated", response.getFirstName());
        assertEquals("Name", response.getLastName());
        assertEquals("updated@example.com", response.getEmail());
        assertEquals("+375293333333", response.getPhone());

        Client updatedClient = clientRepository
                .findById(client.getId())
                .orElseThrow();

        assertEquals("Updated", updatedClient.getFirstName());
        assertEquals("updated@example.com", updatedClient.getEmail());
    }

    @Test
    void updateStatus_ChangesDealStatus() {
        Client client = saveClient();

        ClientStatusUpdateRequest request =
                ClientStatusUpdateRequest.builder()
                        .dealStatus(DealStatus.SUCCESS)
                        .comment("Deal completed")
                        .build();

        ClientResponse response =
                clientService.updateStatus(client.getId(), request);

        assertEquals(DealStatus.SUCCESS, response.getDealStatus());

        Client updatedClient = clientRepository
                .findById(client.getId())
                .orElseThrow();

        assertEquals(DealStatus.SUCCESS, updatedClient.getDealStatus());
    }

    private Client saveClient() {
        return saveClient(
                "test@example.com",
                "+375294444444"
        );
    }

    private Client saveClient(String email, String phone) {
        Client client = Client.builder()
                .firstName("John")
                .lastName("Doe")
                .email(email)
                .phone(phone)
                .dealStatus(DealStatus.NEW)
                .isDeleted(false)
                .build();

        return clientRepository.save(client);
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        ObjectMapper objectMapper() {
            return JsonMapper.builder()
                    .addModule(new JavaTimeModule())
                    .build();
        }
    }
}