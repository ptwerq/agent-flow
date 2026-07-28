package com.agentflow.clientservice.integration;

import com.agentflow.clientservice.entity.Client;
import com.agentflow.clientservice.repository.ClientRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnableJpaAuditing
public class ClientRepositoryTest {
    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ClientRepository clientRepository;

    @Test
    public void save_DuplicateEmail_ThrowsException() {
        Client firstClient = Client.builder()
                .firstName("Иван")
                .lastName("Иванов")
                .email("duplicate@example.com")
                .phone("+375291111111")
                .build();

        Client secondClient = Client.builder()
                .firstName("Петр")
                .lastName("Петров")
                .email("duplicate@example.com")
                .phone("+375292222222")
                .build();
        entityManager.persistAndFlush(firstClient);
        entityManager.clear();

        assertThrows(ConstraintViolationException.class, () -> {
            entityManager.persistAndFlush(secondClient);
        });
    }

    @Test
    public void save_DuplicatePhone_ThrowsException() {
        Client firstClient = Client.builder()
                .firstName("Иван")
                .lastName("Иванов")
                .email("ivan@example.com")
                .phone("+375291111111")
                .build();

        Client secondClient = Client.builder()
                .firstName("Петр")
                .lastName("Петров")
                .email("petr@example.com")
                .phone("+375291111111")
                .build();
        entityManager.persistAndFlush(firstClient);
        entityManager.clear();

        assertThrows(ConstraintViolationException.class, () -> {
            entityManager.persistAndFlush(secondClient);
        });
    }

    @Test
    public void findByIdAndIsDeletedFalse_Success() {
        Client client = Client.builder()
                .firstName("Иван")
                .lastName("Иванов")
                .email("ivan@example.com")
                .phone("+375291111111")
                .isDeleted(false)
                .build();
        Client deletedClient = Client.builder()
                .firstName("Петр")
                .lastName("Петров")
                .email("petr@example.com")
                .phone("+375291111122")
                .isDeleted(true)
                .build();
        entityManager.persistAndFlush(client);
        entityManager.persistAndFlush(deletedClient);
        entityManager.clear();

        Optional<Client> result = clientRepository.findByIdAndIsDeletedFalse(client.getId());

        assertTrue(result.isPresent());
        Client actualClient = result.get();
        assertEquals(client.getEmail(), actualClient.getEmail());

        result = clientRepository.findByIdAndIsDeletedFalse(deletedClient.getId());
        assertTrue(result.isEmpty());
    }
}
