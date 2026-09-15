package com.agentflow.managerservice.integration;

import com.agentflow.managerservice.config.JpaConfig;
import com.agentflow.managerservice.entity.Manager;
import com.agentflow.managerservice.entity.ManagerStatus;
import com.agentflow.managerservice.repository.ManagerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaConfig.class)
class ManagerRepositoryIntegrationTest {

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
    private ManagerRepository managerRepository;

    @BeforeEach
    void setUp() {
        managerRepository.deleteAll();
    }

    @Test
    void findByIdAndIsDeletedFalse_ReturnsManager_WhenManagerIsNotDeleted() {
        Manager manager = managerRepository.save(
                createManager("John", "john@example.com", false)
        );

        Optional<Manager> result =
                managerRepository.findByIdAndIsDeletedFalse(manager.getId());

        assertTrue(result.isPresent());
        assertEquals(manager.getId(), result.get().getId());
        assertFalse(result.get().getIsDeleted());
    }

    @Test
    void findByIdAndIsDeletedFalse_ReturnsEmpty_WhenManagerIsDeleted() {
        Manager manager = managerRepository.save(
                createManager("Deleted", "deleted@example.com", true)
        );

        Optional<Manager> result =
                managerRepository.findByIdAndIsDeletedFalse(manager.getId());

        assertTrue(result.isEmpty());
    }

    @Test
    void findLeastLoadedAvailableManager_ReturnsManagerWithMinimalLoad() {
        Manager manager1 =
                createManager("Manager1", "manager1@example.com", false);
        manager1.setCurrentLoad(3);
        manager1.setMaxCapacity(5);

        Manager manager2 =
                createManager("Manager2", "manager2@example.com", false);
        manager2.setCurrentLoad(1);
        manager2.setMaxCapacity(5);

        Manager manager3 =
                createManager("Manager3", "manager3@example.com", false);
        manager3.setCurrentLoad(4);
        manager3.setMaxCapacity(5);

        managerRepository.save(manager1);
        managerRepository.save(manager2);
        managerRepository.save(manager3);

        Optional<Manager> result =
                managerRepository.findLeastLoadedAvailableManager();

        assertTrue(result.isPresent());
        assertEquals(manager2.getId(), result.get().getId());
        assertEquals(1, result.get().getCurrentLoad());
    }

    @Test
    void findLeastLoadedAvailableManager_IgnoresBusyManager() {
        Manager busyManager =
                createManager("Busy", "busy@example.com", false);
        busyManager.setStatus(ManagerStatus.BUSY);
        busyManager.setCurrentLoad(0);
        busyManager.setMaxCapacity(5);

        Manager activeManager =
                createManager("Active", "active@example.com", false);
        activeManager.setStatus(ManagerStatus.ACTIVE);
        activeManager.setCurrentLoad(2);
        activeManager.setMaxCapacity(5);

        managerRepository.save(busyManager);
        managerRepository.save(activeManager);

        Optional<Manager> result =
                managerRepository.findLeastLoadedAvailableManager();

        assertTrue(result.isPresent());
        assertEquals(activeManager.getId(), result.get().getId());
    }

    @Test
    void findLeastLoadedAvailableManager_IgnoresFullManager() {
        Manager fullManager =
                createManager("Full", "full@example.com", false);
        fullManager.setCurrentLoad(5);
        fullManager.setMaxCapacity(5);

        managerRepository.save(fullManager);

        Optional<Manager> result =
                managerRepository.findLeastLoadedAvailableManager();

        assertTrue(result.isEmpty());
    }

    @Test
    void findLeastLoadedAvailableManager_IgnoresDeletedManager() {
        Manager deletedManager =
                createManager("Deleted", "deleted@example.com", true);
        deletedManager.setCurrentLoad(0);
        deletedManager.setMaxCapacity(5);

        managerRepository.save(deletedManager);

        Optional<Manager> result =
                managerRepository.findLeastLoadedAvailableManager();

        assertTrue(result.isEmpty());
    }

    @Test
    void findLeastLoadedAvailableManager_ReturnsEmpty_WhenNoAvailableManagers() {
        Manager busyManager =
                createManager("Busy", "busy@example.com", false);
        busyManager.setStatus(ManagerStatus.BUSY);

        Manager fullManager =
                createManager("Full", "full@example.com", false);
        fullManager.setCurrentLoad(5);
        fullManager.setMaxCapacity(5);

        Manager deletedManager =
                createManager("Deleted", "deleted@example.com", true);

        managerRepository.save(busyManager);
        managerRepository.save(fullManager);
        managerRepository.save(deletedManager);

        Optional<Manager> result =
                managerRepository.findLeastLoadedAvailableManager();

        assertTrue(result.isEmpty());
    }

    @Test
    void findAllByIsDeletedFalse_ReturnsOnlyNotDeletedManagers() {
        Manager activeManager =
                managerRepository.save(
                        createManager("Active", "active@example.com", false)
                );

        Manager deletedManager =
                managerRepository.save(
                        createManager("Deleted", "deleted@example.com", true)
                );

        Page<Manager> result =
                managerRepository.findAllByIsDeletedFalse(PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals(activeManager.getId(), result.getContent().get(0).getId());
        assertNotEquals(deletedManager.getId(), result.getContent().get(0).getId());
    }

    @Test
    void findAllByIsDeletedFalse_SupportsPagination() {
        managerRepository.save(
                createManager("Manager1", "manager1@example.com", false)
        );
        managerRepository.save(
                createManager("Manager2", "manager2@example.com", false)
        );
        managerRepository.save(
                createManager("Manager3", "manager3@example.com", false)
        );

        Page<Manager> result =
                managerRepository.findAllByIsDeletedFalse(PageRequest.of(0, 2));

        assertEquals(3, result.getTotalElements());
        assertEquals(2, result.getContent().size());
        assertEquals(2, result.getTotalPages());
    }

    private Manager createManager(
            String firstName,
            String email,
            boolean isDeleted
    ) {
        return Manager.builder()
                .firstName(firstName)
                .lastName("Doe")
                .email(email)
                .phone("+375291234567")
                .status(ManagerStatus.ACTIVE)
                .currentLoad(0)
                .maxCapacity(5)
                .isDeleted(isDeleted)
                .build();
    }
}