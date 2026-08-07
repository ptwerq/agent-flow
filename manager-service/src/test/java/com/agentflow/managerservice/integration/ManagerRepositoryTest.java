package com.agentflow.managerservice.integration;

import com.agentflow.managerservice.entity.Manager;
import com.agentflow.managerservice.entity.ManagerStatus;
import com.agentflow.managerservice.repository.ManagerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnableJpaAuditing
class ManagerRepositoryTest {

    @Autowired
    private ManagerRepository managerRepository;

    @BeforeEach
    void setUp() {
        managerRepository.deleteAll();
    }

    @Test
    @DisplayName("findByIdAndIsDeletedFalse() - Should return manager when active")
    void findByIdAndIsDeletedFalse_WhenActive_ReturnsManager() {
        Manager activeManager = managerRepository.save(createManager("John", "john@example.com", false));

        Optional<Manager> found = managerRepository.findByIdAndIsDeletedFalse(activeManager.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getFirstName()).isEqualTo("John");
    }

    @Test
    @DisplayName("findByIdAndIsDeletedFalse() - Should return empty when soft deleted")
    void findByIdAndIsDeletedFalse_WhenDeleted_ReturnsEmpty() {
        Manager deletedManager = managerRepository.save(createManager("Deleted", "deleted@example.com", true));

        Optional<Manager> found = managerRepository.findByIdAndIsDeletedFalse(deletedManager.getId());

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("findLeastLoadedAvailableManager() - Should return manager with minimal currentLoad")
    void findLeastLoadedAvailableManager_ReturnsLeastLoaded() {
        // Менеджер 1: загрузка 3 из 5
        Manager m1 = createManager("Busy", "m1@example.com", false);
        m1.setCurrentLoad(3);
        m1.setMaxCapacity(5);

        // Менеджер 2: загрузка 1 из 5 (наименее загружен)
        Manager m2 = createManager("LeastLoaded", "m2@example.com", false);
        m2.setCurrentLoad(1);
        m2.setMaxCapacity(5);

        // Менеджер 3: загрузка 4 из 5
        Manager m3 = createManager("MoreBusy", "m3@example.com", false);
        m3.setCurrentLoad(4);
        m3.setMaxCapacity(5);

        managerRepository.save(m1);
        managerRepository.save(m2);
        managerRepository.save(m3);

        Optional<Manager> result = managerRepository.findLeastLoadedAvailableManager();

        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("m2@example.com");
        assertThat(result.get().getCurrentLoad()).isEqualTo(1);
    }

    @Test
    @DisplayName("findLeastLoadedAvailableManager() - Should ignore BUSY, deleted or overcapacity managers")
    void findLeastLoadedAvailableManager_IgnoresUnavailable() {
        // Менеджер BUSY
        Manager busyManager = createManager("BusyStatus", "busy@example.com", false);
        busyManager.setStatus(ManagerStatus.BUSY);
        busyManager.setCurrentLoad(1);
        busyManager.setMaxCapacity(5);

        // Менеджер забит полностью (currentLoad == maxCapacity)
        Manager fullManager = createManager("FullCapacity", "full@example.com", false);
        fullManager.setCurrentLoad(5);
        fullManager.setMaxCapacity(5);

        // Менеджер удален
        Manager deletedManager = createManager("Deleted", "deleted@example.com", true);
        deletedManager.setCurrentLoad(0);

        managerRepository.save(busyManager);
        managerRepository.save(fullManager);
        managerRepository.save(deletedManager);

        Optional<Manager> result = managerRepository.findLeastLoadedAvailableManager();

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findAllByIsDeletedFalse() - Should return page without soft deleted managers")
    void findAllByIsDeletedFalse_ReturnsOnlyActivePage() {
        managerRepository.save(createManager("Active1", "a1@example.com", false));
        managerRepository.save(createManager("Active2", "a2@example.com", false));
        managerRepository.save(createManager("Deleted", "del@example.com", true));

        Page<Manager> page = managerRepository.findAllByIsDeletedFalse(PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent())
                .extracting(Manager::getEmail)
                .containsExactlyInAnyOrder("a1@example.com", "a2@example.com");
    }

    private Manager createManager(String firstName, String email, boolean isDeleted) {
        return Manager.builder()
                .firstName(firstName)
                .lastName("Doe")
                .email(email)
                .phone("+12345678901")
                .status(ManagerStatus.ACTIVE)
                .currentLoad(0)
                .maxCapacity(5)
                .isDeleted(isDeleted)
                .build();
    }
}
