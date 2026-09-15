package com.agentflow.managerservice.integration;

import com.agentflow.managerservice.config.JpaConfig;
import com.agentflow.managerservice.dto.request.ManagerRequest;
import com.agentflow.managerservice.dto.request.ManagerStatusUpdateRequest;
import com.agentflow.managerservice.dto.request.ManagerUpdateRequest;
import com.agentflow.managerservice.dto.response.ManagerResponse;
import com.agentflow.managerservice.entity.Manager;
import com.agentflow.managerservice.entity.ManagerStatus;
import com.agentflow.managerservice.exception.InvalidManagerLoadException;
import com.agentflow.managerservice.exception.ManagerCapacityExceededException;
import com.agentflow.managerservice.exception.NotFoundException;
import com.agentflow.managerservice.mapper.ManagerMapperImpl;
import com.agentflow.managerservice.repository.ManagerRepository;
import com.agentflow.managerservice.service.ManagerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
        JpaConfig.class,
        ManagerService.class,
        ManagerMapperImpl.class
})
class ManagerServiceIntegrationTest {

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
    private ManagerService managerService;

    @Autowired
    private ManagerRepository managerRepository;

    @BeforeEach
    void setUp() {
        managerRepository.deleteAll();
    }

    @Test
    void create_SavesManagerAndReturnsResponse() {
        ManagerRequest request = createManagerRequest(
                "John",
                "john@example.com"
        );

        ManagerResponse response = managerService.create(request);

        assertNotNull(response.getId());
        assertEquals("John", response.getFirstName());
        assertEquals("Doe", response.getLastName());
        assertEquals("john@example.com", response.getEmail());
        assertEquals("+375291234567", response.getPhone());
        assertEquals(ManagerStatus.ACTIVE, response.getStatus());
        assertEquals(10, response.getMaxCapacity());
        assertEquals(0, response.getCurrentLoad());

        assertTrue(managerRepository.findById(response.getId()).isPresent());
    }

    @Test
    void getById_ReturnsManager() {
        Manager manager = managerRepository.save(
                createManager("John", "john@example.com", false)
        );

        ManagerResponse response = managerService.getById(manager.getId());

        assertEquals(manager.getId(), response.getId());
        assertEquals("John", response.getFirstName());
        assertEquals("john@example.com", response.getEmail());
    }

    @Test
    void getById_ThrowsException_WhenManagerNotFound() {
        assertThrows(
                NotFoundException.class,
                () -> managerService.getById(999L)
        );
    }

    @Test
    void getAll_ReturnsOnlyNotDeletedManagers() {
        managerRepository.save(
                createManager("John", "john@example.com", false)
        );
        managerRepository.save(
                createManager("Jane", "jane@example.com", false)
        );
        managerRepository.save(
                createManager("Deleted", "deleted@example.com", true)
        );

        var result = managerService.getAll(
                org.springframework.data.domain.PageRequest.of(0, 10)
        );

        assertEquals(2, result.getTotalElements());
        assertEquals(2, result.getContent().size());
    }

    @Test
    void update_UpdatesManagerProfile() {
        Manager manager = managerRepository.save(
                createManager("John", "john@example.com", false)
        );

        ManagerUpdateRequest request = ManagerUpdateRequest.builder()
                .firstName("Updated")
                .lastName("Smith")
                .email("updated@example.com")
                .phone("+375291111111")
                .build();

        ManagerResponse response =
                managerService.update(manager.getId(), request);

        assertEquals("Updated", response.getFirstName());
        assertEquals("Smith", response.getLastName());
        assertEquals("updated@example.com", response.getEmail());
        assertEquals("+375291111111", response.getPhone());
    }

    @Test
    void updateStatus_UpdatesManagerStatus() {
        Manager manager = managerRepository.save(
                createManager("John", "john@example.com", false)
        );

        ManagerStatusUpdateRequest request =
                ManagerStatusUpdateRequest.builder()
                        .status(ManagerStatus.BUSY)
                        .comment("Currently handling clients")
                        .build();

        ManagerResponse response =
                managerService.updateStatus(manager.getId(), request);

        assertEquals(ManagerStatus.BUSY, response.getStatus());
    }

    @Test
    void delete_SoftDeletesManager() {
        Manager manager = managerRepository.save(
                createManager("John", "john@example.com", false)
        );

        managerService.delete(manager.getId());

        assertTrue(
                managerRepository
                        .findByIdAndIsDeletedFalse(manager.getId())
                        .isEmpty()
        );
    }

    @Test
    void delete_ThrowsException_WhenManagerHasActiveClients() {
        Manager manager = createManager(
                "John",
                "john@example.com",
                false
        );
        manager.setCurrentLoad(2);
        managerRepository.save(manager);

        assertThrows(
                IllegalStateException.class,
                () -> managerService.delete(manager.getId())
        );
    }

    @Test
    void increaseLoad_IncreasesCurrentLoad() {
        Manager manager = createManager(
                "John",
                "john@example.com",
                false
        );
        manager.setCurrentLoad(2);
        manager.setMaxCapacity(5);
        managerRepository.save(manager);

        managerService.increaseLoad(manager.getId());

        Manager updated =
                managerRepository.findById(manager.getId()).orElseThrow();

        assertEquals(3, updated.getCurrentLoad());
        assertEquals(ManagerStatus.ACTIVE, updated.getStatus());
    }

    @Test
    void increaseLoad_SetsBusy_WhenCapacityReached() {
        Manager manager = createManager(
                "John",
                "john@example.com",
                false
        );
        manager.setCurrentLoad(4);
        manager.setMaxCapacity(5);
        managerRepository.save(manager);

        managerService.increaseLoad(manager.getId());

        Manager updated =
                managerRepository.findById(manager.getId()).orElseThrow();

        assertEquals(5, updated.getCurrentLoad());
        assertEquals(ManagerStatus.BUSY, updated.getStatus());
    }

    @Test
    void increaseLoad_ThrowsException_WhenCapacityExceeded() {
        Manager manager = createManager(
                "John",
                "john@example.com",
                false
        );
        manager.setCurrentLoad(5);
        manager.setMaxCapacity(5);
        managerRepository.save(manager);

        assertThrows(
                ManagerCapacityExceededException.class,
                () -> managerService.increaseLoad(manager.getId())
        );
    }

    @Test
    void decreaseLoad_DecreasesCurrentLoad() {
        Manager manager = createManager(
                "John",
                "john@example.com",
                false
        );
        manager.setCurrentLoad(3);
        manager.setMaxCapacity(5);
        managerRepository.save(manager);

        managerService.decreaseLoad(manager.getId());

        Manager updated =
                managerRepository.findById(manager.getId()).orElseThrow();

        assertEquals(2, updated.getCurrentLoad());
    }

    @Test
    void decreaseLoad_SetsActive_WhenBusyManagerBecomesAvailable() {
        Manager manager = createManager(
                "John",
                "john@example.com",
                false
        );
        manager.setCurrentLoad(5);
        manager.setMaxCapacity(5);
        manager.setStatus(ManagerStatus.BUSY);
        managerRepository.save(manager);

        managerService.decreaseLoad(manager.getId());

        Manager updated =
                managerRepository.findById(manager.getId()).orElseThrow();

        assertEquals(4, updated.getCurrentLoad());
        assertEquals(ManagerStatus.ACTIVE, updated.getStatus());
    }

    @Test
    void decreaseLoad_ThrowsException_WhenLoadIsZero() {
        Manager manager = createManager(
                "John",
                "john@example.com",
                false
        );
        manager.setCurrentLoad(0);
        managerRepository.save(manager);

        assertThrows(
                InvalidManagerLoadException.class,
                () -> managerService.decreaseLoad(manager.getId())
        );
    }

    @Test
    void findLeastLoadedAvailableManager_ReturnsLeastLoadedManager() {
        Manager manager1 = createManager(
                "Manager1",
                "manager1@example.com",
                false
        );
        manager1.setCurrentLoad(3);

        Manager manager2 = createManager(
                "Manager2",
                "manager2@example.com",
                false
        );
        manager2.setCurrentLoad(1);

        managerRepository.save(manager1);
        managerRepository.save(manager2);

        Manager result =
                managerService.findLeastLoadedAvailableManager();

        assertEquals(manager2.getId(), result.getId());
        assertEquals(1, result.getCurrentLoad());
    }

    @Test
    void findLeastLoadedAvailableManager_ThrowsException_WhenNoManagerAvailable() {
        Manager manager = createManager(
                "Busy",
                "busy@example.com",
                false
        );
        manager.setStatus(ManagerStatus.BUSY);
        managerRepository.save(manager);

        assertThrows(
                com.agentflow.managerservice.exception.NoAvailableManagerException.class,
                () -> managerService.findLeastLoadedAvailableManager()
        );
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
                .maxCapacity(10)
                .currentLoad(0)
                .isDeleted(isDeleted)
                .build();
    }

    private ManagerRequest createManagerRequest(
            String firstName,
            String email
    ) {
        return ManagerRequest.builder()
                .firstName(firstName)
                .lastName("Doe")
                .email(email)
                .phone("+375291234567")
                .build();
    }
}