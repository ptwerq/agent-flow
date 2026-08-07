package com.agentflow.managerservice.service;

import com.agentflow.managerservice.dto.request.ManagerRequest;
import com.agentflow.managerservice.dto.request.ManagerStatusUpdateRequest;
import com.agentflow.managerservice.dto.request.ManagerUpdateRequest;
import com.agentflow.managerservice.dto.response.ManagerResponse;
import com.agentflow.managerservice.entity.Manager;
import com.agentflow.managerservice.entity.ManagerStatus;
import com.agentflow.managerservice.exception.InvalidManagerLoadException;
import com.agentflow.managerservice.exception.ManagerCapacityExceededException;
import com.agentflow.managerservice.exception.NoAvailableManagerException;
import com.agentflow.managerservice.exception.NotFoundException;
import com.agentflow.managerservice.mapper.ManagerMapper;
import com.agentflow.managerservice.repository.ManagerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ManagerServiceTest {

    @Mock
    private ManagerRepository managerRepository;

    @Mock
    private ManagerMapper managerMapper;

    @InjectMocks
    private ManagerService managerService;

    private Manager manager;
    private ManagerResponse managerResponse;
    private final Long managerId = 1L;

    @BeforeEach
    void setUp() {
        manager = Manager.builder()
                .id(managerId)
                .firstName("John")
                .lastName("Doe")
                .status(ManagerStatus.ACTIVE)
                .currentLoad(1)
                .maxCapacity(5)
                .isDeleted(false)
                .build();

        managerResponse = ManagerResponse.builder()
                .id(managerId)
                .firstName("John")
                .lastName("Doe")
                .status(ManagerStatus.ACTIVE)
                .currentLoad(1)
                .maxCapacity(5)
                .build();
    }

    @Nested
    @DisplayName("Create Manager Tests")
    class CreateTests {

        @Test
        @DisplayName("Should create manager successfully")
        void create_Success() {
            ManagerRequest request = new ManagerRequest();

            when(managerMapper.toEntity(request)).thenReturn(manager);
            when(managerRepository.save(manager)).thenReturn(manager);
            when(managerMapper.toResponse(manager)).thenReturn(managerResponse);

            ManagerResponse result = managerService.create(request);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(managerId);

            verify(managerMapper).toEntity(request);
            verify(managerRepository).save(manager);
            verify(managerMapper).toResponse(manager);
        }
    }

    @Nested
    @DisplayName("Get Manager Tests")
    class GetTests {

        @Test
        @DisplayName("Should return manager by id when found")
        void getById_Success() {
            when(managerRepository.findByIdAndIsDeletedFalse(managerId)).thenReturn(Optional.of(manager));
            when(managerMapper.toResponse(manager)).thenReturn(managerResponse);

            ManagerResponse result = managerService.getById(managerId);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(managerId);
        }

        @Test
        @DisplayName("Should throw NotFoundException when manager not found")
        void getById_NotFound_ThrowsException() {
            when(managerRepository.findByIdAndIsDeletedFalse(managerId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> managerService.getById(managerId))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Manager not found: " + managerId);
        }

        @Test
        @DisplayName("Should return page of managers")
        void getAll_Success() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Manager> managerPage = new PageImpl<>(List.of(manager));

            when(managerRepository.findAllByIsDeletedFalse(pageable)).thenReturn(managerPage);
            when(managerMapper.toResponse(any(Manager.class))).thenReturn(managerResponse);

            Page<ManagerResponse> result = managerService.getAll(pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            verify(managerRepository).findAllByIsDeletedFalse(pageable);
        }
    }

    @Nested
    @DisplayName("Update Manager Tests")
    class UpdateTests {

        @Test
        @DisplayName("Should update manager details successfully")
        void update_Success() {
            ManagerUpdateRequest updateRequest = new ManagerUpdateRequest();
            when(managerRepository.findByIdAndIsDeletedFalse(managerId)).thenReturn(Optional.of(manager));
            when(managerMapper.toResponse(manager)).thenReturn(managerResponse);

            ManagerResponse result = managerService.update(managerId, updateRequest);

            assertThat(result).isNotNull();
            verify(managerMapper).updateManagerFromUpdateRequest(updateRequest, manager);
        }

        @Test
        @DisplayName("Should update manager status successfully")
        void updateStatus_Success() {
            ManagerStatusUpdateRequest statusRequest = new ManagerStatusUpdateRequest();
            when(managerRepository.findByIdAndIsDeletedFalse(managerId)).thenReturn(Optional.of(manager));
            when(managerMapper.toResponse(manager)).thenReturn(managerResponse);

            ManagerResponse result = managerService.updateStatus(managerId, statusRequest);

            assertThat(result).isNotNull();
            verify(managerMapper).updateManagerFromStatusRequest(statusRequest, manager);
        }
    }

    @Nested
    @DisplayName("Delete Manager Tests")
    class DeleteTests {

        @Test
        @DisplayName("Should soft delete manager when currentLoad is 0")
        void delete_Success() {
            manager.setCurrentLoad(0);
            when(managerRepository.findByIdAndIsDeletedFalse(managerId)).thenReturn(Optional.of(manager));

            managerService.delete(managerId);

            assertThat(manager.getIsDeleted()).isTrue();
        }

        @Test
        @DisplayName("Should throw IllegalStateException when deleting manager with active load")
        void delete_WithActiveLoad_ThrowsException() {
            manager.setCurrentLoad(2);
            when(managerRepository.findByIdAndIsDeletedFalse(managerId)).thenReturn(Optional.of(manager));

            assertThatThrownBy(() -> managerService.delete(managerId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot delete manager with active clients");
        }
    }

    @Nested
    @DisplayName("Increase/Decrease Load Tests")
    class LoadManagementTests {

        @Test
        @DisplayName("Should set status to BUSY when new load equals/exceeds capacity")
        void increaseLoad_ReachesCapacity_SetsStatusBusy() {
            manager.setCurrentLoad(4);
            manager.setMaxCapacity(5);
            when(managerRepository.findByIdAndIsDeletedFalse(managerId)).thenReturn(Optional.of(manager));

            managerService.increaseLoad(managerId);

            assertThat(manager.getCurrentLoad()).isEqualTo(5);
            assertThat(manager.getStatus()).isEqualTo(ManagerStatus.BUSY);
        }

        @Test
        @DisplayName("Should throw ManagerCapacityExceededException when capacity is not reached")
        void increaseLoad_UnderCapacity_ThrowsException() {
            manager.setCurrentLoad(1);
            manager.setMaxCapacity(5);
            when(managerRepository.findByIdAndIsDeletedFalse(managerId)).thenReturn(Optional.of(manager));

            assertThatThrownBy(() -> managerService.increaseLoad(managerId))
                    .isInstanceOf(ManagerCapacityExceededException.class)
                    .hasMessageContaining("Manager capacity exceeded");

            assertThat(manager.getCurrentLoad()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should decrease load successfully and change status back to ACTIVE if BUSY")
        void decreaseLoad_Success_UpdatesStatus() {
            manager.setCurrentLoad(5);
            manager.setMaxCapacity(5);
            manager.setStatus(ManagerStatus.BUSY);
            when(managerRepository.findByIdAndIsDeletedFalse(managerId)).thenReturn(Optional.of(manager));

            managerService.decreaseLoad(managerId);

            assertThat(manager.getCurrentLoad()).isEqualTo(4);
            assertThat(manager.getStatus()).isEqualTo(ManagerStatus.ACTIVE);
        }

        @Test
        @DisplayName("Should throw InvalidManagerLoadException when decreasing load with currentLoad <= 0")
        void decreaseLoad_ZeroLoad_ThrowsException() {
            manager.setCurrentLoad(0);
            when(managerRepository.findByIdAndIsDeletedFalse(managerId)).thenReturn(Optional.of(manager));

            assertThatThrownBy(() -> managerService.decreaseLoad(managerId))
                    .isInstanceOf(InvalidManagerLoadException.class)
                    .hasMessageContaining("Manager's current load is invalid");
        }
    }

    @Nested
    @DisplayName("Find Least Loaded Manager Tests")
    class FindLeastLoadedTests {

        @Test
        @DisplayName("Should return least loaded available manager")
        void findLeastLoaded_Success() {
            when(managerRepository.findLeastLoadedAvailableManager()).thenReturn(Optional.of(manager));

            Manager result = managerService.findLeastLoadedAvailableManager();

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(managerId);
        }

        @Test
        @DisplayName("Should throw NoAvailableManagerException when no manager available")
        void findLeastLoaded_NotFound_ThrowsException() {
            when(managerRepository.findLeastLoadedAvailableManager()).thenReturn(Optional.empty());

            assertThatThrownBy(() -> managerService.findLeastLoadedAvailableManager())
                    .isInstanceOf(NoAvailableManagerException.class)
                    .hasMessageContaining("No available manager found");
        }
    }
}
