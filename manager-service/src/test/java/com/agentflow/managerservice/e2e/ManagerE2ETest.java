package com.agentflow.managerservice.e2e;

import com.agentflow.managerservice.dto.request.ManagerStatusUpdateRequest;
import com.agentflow.managerservice.dto.request.ManagerUpdateRequest;
import com.agentflow.managerservice.dto.response.ManagerResponse;
import com.agentflow.managerservice.entity.Manager;
import com.agentflow.managerservice.entity.ManagerStatus;
import com.agentflow.managerservice.exception.InvalidManagerLoadException;
import com.agentflow.managerservice.exception.ManagerCapacityExceededException;
import com.agentflow.managerservice.exception.NotFoundException;
import com.agentflow.managerservice.repository.ManagerRepository;
import com.agentflow.managerservice.service.ManagerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
public class ManagerE2ETest {

    @Autowired
    private ManagerService managerService;

    @Autowired
    private ManagerRepository managerRepository;

    @BeforeEach
    void setUp() {
        managerRepository.deleteAll();
    }

    @Test
    @DisplayName("E2E: CRUD operations lifecycle for Manager")
    void fullManagerLifecycle() {
        Manager manager = createTestManager("John", "Doe", "john.doe@example.com", 5);
        Manager saved = managerRepository.save(manager);

        // 1. Get by ID
        ManagerResponse fetched = managerService.getById(saved.getId());
        assertThat(fetched.getId()).isEqualTo(saved.getId());
        assertThat(fetched.getEmail()).isEqualTo("john.doe@example.com");

        // 2. Update info
        ManagerUpdateRequest updateRequest = ManagerUpdateRequest.builder()
                .firstName("John")
                .lastName("Updated")
                .email("john.updated@example.com")
                .phone("+12345678901")
                .build();

        ManagerResponse updated = managerService.update(saved.getId(), updateRequest);
        assertThat(updated.getLastName()).isEqualTo("Updated");

        // 3. Update status
        ManagerStatusUpdateRequest statusRequest = ManagerStatusUpdateRequest.builder()
                .status(ManagerStatus.BUSY)
                .comment("Busy now")
                .build();

        ManagerResponse statusUpdated = managerService.updateStatus(saved.getId(), statusRequest);
        assertThat(statusUpdated.getStatus()).isEqualTo(ManagerStatus.BUSY);

        // 4. Soft Delete (когда currentLoad == 0)
        managerService.delete(saved.getId());

        assertThatThrownBy(() -> managerService.getById(saved.getId()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("E2E: Capacity & Load handling lifecycle")
    void capacityAndLoadLifecycle() {
        // 1. Создаем менеджеров
        Manager m1 = createTestManager("M1", "Test", "m1@example.com", 2);
        Manager m2 = createTestManager("M2", "Test", "m2@example.com", 2);
        m1.setCurrentLoad(1);
        m2.setCurrentLoad(0); // Наименьшая загрузка

        managerRepository.save(m1);
        managerRepository.save(m2);

        // 2. Находим наименее загруженного
        Manager leastLoaded = managerService.findLeastLoadedAvailableManager();
        assertThat(leastLoaded.getEmail()).isEqualTo("m2@example.com");

        // 3. Повышаем нагрузку на m2 (было 0, станет 1)
        managerService.increaseLoad(m2.getId());
        Manager m2AfterFirstIncrease = managerRepository.findById(m2.getId()).orElseThrow();
        assertThat(m2AfterFirstIncrease.getCurrentLoad()).isEqualTo(1);

        // 4. Доводим до лимита maxCapacity (станет 2 -> статус BUSY)
        managerService.increaseLoad(m2.getId());
        Manager m2Busy = managerRepository.findById(m2.getId()).orElseThrow();
        assertThat(m2Busy.getCurrentLoad()).isEqualTo(2);
        assertThat(m2Busy.getStatus()).isEqualTo(ManagerStatus.BUSY);

        assertThatThrownBy(() -> managerService.increaseLoad(m2.getId()))
                .isInstanceOf(ManagerCapacityExceededException.class);

        managerService.decreaseLoad(m2.getId());
        Manager m2ActiveAgain = managerRepository.findById(m2.getId()).orElseThrow();
        assertThat(m2ActiveAgain.getCurrentLoad()).isEqualTo(1);
        assertThat(m2ActiveAgain.getStatus()).isEqualTo(ManagerStatus.ACTIVE);
    }

    @Test
    @DisplayName("E2E: Should fail deleting manager with active load")
    void delete_WithActiveLoad_ShouldThrowException() {
        Manager manager = createTestManager("Busy", "Manager", "busy@example.com", 5);
        manager.setCurrentLoad(1);
        managerRepository.save(manager);

        assertThatThrownBy(() -> managerService.delete(manager.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot delete manager with active clients");
    }

    @Test
    @DisplayName("E2E: Should fail decreasing load when current load is 0")
    void decreaseLoad_ZeroLoad_ShouldThrowException() {
        Manager manager = createTestManager("Idle", "Manager", "idle@example.com", 5);
        managerRepository.save(manager);

        assertThatThrownBy(() -> managerService.decreaseLoad(manager.getId()))
                .isInstanceOf(InvalidManagerLoadException.class);
    }

    private Manager createTestManager(String firstName, String lastName, String email, int maxCapacity) {
        return Manager.builder()
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .phone("+12345678901")
                .status(ManagerStatus.ACTIVE)
                .currentLoad(0)
                .maxCapacity(maxCapacity)
                .isDeleted(false)
                .build();
    }
}
