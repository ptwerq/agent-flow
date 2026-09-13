package com.agentflow.clientservice.integration;

import com.agentflow.clientservice.entity.Client;
import com.agentflow.clientservice.entity.DealStatus;
import com.agentflow.clientservice.repository.ClientRepository;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ClientRepositoryIntegrationTest {

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
    private ClientRepository clientRepository;

    @BeforeEach
    void setUp() {
        clientRepository.deleteAll();
    }

    @Test
    void findByIdAndIsDeletedFalse_ReturnsClient_WhenClientIsNotDeleted() {
        Client client = createClient(false);
        Client savedClient = clientRepository.save(client);

        Optional<Client> result =
                clientRepository.findByIdAndIsDeletedFalse(savedClient.getId());

        assertTrue(result.isPresent());
        assertEquals(savedClient.getId(), result.get().getId());
        assertFalse(result.get().isDeleted());
    }

    @Test
    void findByIdAndIsDeletedFalse_ReturnsEmpty_WhenClientIsDeleted() {
        Client client = createClient(true);
        Client savedClient = clientRepository.save(client);

        Optional<Client> result =
                clientRepository.findByIdAndIsDeletedFalse(savedClient.getId());

        assertTrue(result.isEmpty());
    }

    @Test
    void findAllByIsDeletedFalse_ReturnsOnlyNotDeletedClients() {
        Client activeClient = clientRepository.save(createClient(false));
        Client deletedClient = clientRepository.save(createClient(true));

        List<Client> result = clientRepository.findAllByIsDeletedFalse();

        assertEquals(1, result.size());
        assertEquals(activeClient.getId(), result.get(0).getId());
        assertFalse(result.get(0).isDeleted());
        assertNotEquals(deletedClient.getId(), result.get(0).getId());
    }

    private Client createClient(boolean isDeleted) {
        return Client.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john" + System.nanoTime() + "@example.com")
                .phone("+375291234" + (100 + (int) (Math.random() * 900)))
                .dealStatus(DealStatus.NEW)
                .isDeleted(isDeleted)
                .build();
    }
}