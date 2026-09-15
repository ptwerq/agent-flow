package com.agentflow.managerservice.unit;

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
import com.agentflow.managerservice.service.ManagerService;
import org.junit.jupiter.api.BeforeEach;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @BeforeEach
    void setUp() {
        manager = Manager.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .phone("+375291234567")
                .status(ManagerStatus.ACTIVE)
                .maxCapacity(10)
                .currentLoad(0)
                .isDeleted(false)
                .build();

        managerResponse = new ManagerResponse();
    }

    @Test
    void create_SavesManagerAndReturnsResponse() {
        ManagerRequest request = new ManagerRequest();

        when(managerMapper.toEntity(request)).thenReturn(manager);
        when(managerRepository.save(manager)).thenReturn(manager);
        when(managerMapper.toResponse(manager)).thenReturn(managerResponse);

        ManagerResponse result = managerService.create(request);

        assertSame(managerResponse, result);

        verify(managerMapper).toEntity(request);
        verify(managerRepository).save(manager);
        verify(managerMapper).toResponse(manager);
    }

    @Test
    void getById_ReturnsManager() {
        when(managerRepository.findByIdAndIsDeletedFalse(1L))
                .thenReturn(Optional.of(manager));
        when(managerMapper.toResponse(manager))
                .thenReturn(managerResponse);

        ManagerResponse result = managerService.getById(1L);

        assertSame(managerResponse, result);

        verify(managerRepository).findByIdAndIsDeletedFalse(1L);
        verify(managerMapper).toResponse(manager);
    }

    @Test
    void getById_ThrowsException_WhenManagerNotFound() {
        when(managerRepository.findByIdAndIsDeletedFalse(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                NotFoundException.class,
                () -> managerService.getById(1L)
        );

        verify(managerMapper, never()).toResponse(any());
    }

    @Test
    void getAll_ReturnsManagers() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Manager> managers = new PageImpl<>(List.of(manager));

        when(managerRepository.findAllByIsDeletedFalse(pageable))
                .thenReturn(managers);
        when(managerMapper.toResponse(manager))
                .thenReturn(managerResponse);

        Page<ManagerResponse> result = managerService.getAll(pageable);

        assertEquals(1, result.getTotalElements());
        assertSame(managerResponse, result.getContent().get(0));

        verify(managerRepository).findAllByIsDeletedFalse(pageable);
        verify(managerMapper).toResponse(manager);
    }

    @Test
    void update_UpdatesManagerAndReturnsResponse() {
        ManagerUpdateRequest request = new ManagerUpdateRequest();

        when(managerRepository.findByIdAndIsDeletedFalse(1L))
                .thenReturn(Optional.of(manager));
        when(managerMapper.toResponse(manager))
                .thenReturn(managerResponse);

        ManagerResponse result = managerService.update(1L, request);

        assertSame(managerResponse, result);

        verify(managerMapper).updateManagerFromUpdateRequest(request, manager);
        verify(managerMapper).toResponse(manager);
    }

    @Test
    void updateStatus_UpdatesStatusAndReturnsResponse() {
        ManagerStatusUpdateRequest request = new ManagerStatusUpdateRequest();

        when(managerRepository.findByIdAndIsDeletedFalse(1L))
                .thenReturn(Optional.of(manager));
        when(managerMapper.toResponse(manager))
                .thenReturn(managerResponse);

        ManagerResponse result = managerService.updateStatus(1L, request);

        assertSame(managerResponse, result);

        verify(managerMapper).updateManagerFromStatusRequest(request, manager);
        verify(managerMapper).toResponse(manager);
    }

    @Test
    void delete_MarksManagerAsDeleted_WhenNoActiveClients() {
        manager.setCurrentLoad(0);

        when(managerRepository.findByIdAndIsDeletedFalse(1L))
                .thenReturn(Optional.of(manager));

        managerService.delete(1L);

        assertTrue(manager.getIsDeleted());
    }

    @Test
    void delete_ThrowsException_WhenManagerHasActiveClients() {
        manager.setCurrentLoad(3);

        when(managerRepository.findByIdAndIsDeletedFalse(1L))
                .thenReturn(Optional.of(manager));

        assertThrows(
                IllegalStateException.class,
                () -> managerService.delete(1L)
        );

        assertFalse(manager.getIsDeleted());
    }

    @Test
    void increaseLoad_IncreasesCurrentLoad() {
        manager.setCurrentLoad(3);
        manager.setMaxCapacity(10);
        manager.setStatus(ManagerStatus.ACTIVE);

        when(managerRepository.findByIdAndIsDeletedFalse(1L))
                .thenReturn(Optional.of(manager));

        managerService.increaseLoad(1L);

        assertEquals(4, manager.getCurrentLoad());
        assertEquals(ManagerStatus.ACTIVE, manager.getStatus());
    }

    @Test
    void increaseLoad_SetsBusy_WhenCapacityReached() {
        manager.setCurrentLoad(9);
        manager.setMaxCapacity(10);
        manager.setStatus(ManagerStatus.ACTIVE);

        when(managerRepository.findByIdAndIsDeletedFalse(1L))
                .thenReturn(Optional.of(manager));

        managerService.increaseLoad(1L);

        assertEquals(10, manager.getCurrentLoad());
        assertEquals(ManagerStatus.BUSY, manager.getStatus());
    }

    @Test
    void increaseLoad_ThrowsException_WhenCapacityExceeded() {
        manager.setCurrentLoad(10);
        manager.setMaxCapacity(10);

        when(managerRepository.findByIdAndIsDeletedFalse(1L))
                .thenReturn(Optional.of(manager));

        assertThrows(
                ManagerCapacityExceededException.class,
                () -> managerService.increaseLoad(1L)
        );

        assertEquals(10, manager.getCurrentLoad());
    }

    @Test
    void decreaseLoad_DecreasesCurrentLoad() {
        manager.setCurrentLoad(5);
        manager.setMaxCapacity(10);
        manager.setStatus(ManagerStatus.ACTIVE);

        when(managerRepository.findByIdAndIsDeletedFalse(1L))
                .thenReturn(Optional.of(manager));

        managerService.decreaseLoad(1L);

        assertEquals(4, manager.getCurrentLoad());
    }

    @Test
    void decreaseLoad_SetsActive_WhenBusyManagerHasFreeCapacity() {
        manager.setCurrentLoad(10);
        manager.setMaxCapacity(10);
        manager.setStatus(ManagerStatus.BUSY);

        when(managerRepository.findByIdAndIsDeletedFalse(1L))
                .thenReturn(Optional.of(manager));

        managerService.decreaseLoad(1L);

        assertEquals(9, manager.getCurrentLoad());
        assertEquals(ManagerStatus.ACTIVE, manager.getStatus());
    }

    @Test
    void decreaseLoad_ThrowsException_WhenLoadIsZero() {
        manager.setCurrentLoad(0);

        when(managerRepository.findByIdAndIsDeletedFalse(1L))
                .thenReturn(Optional.of(manager));

        assertThrows(
                InvalidManagerLoadException.class,
                () -> managerService.decreaseLoad(1L)
        );

        assertEquals(0, manager.getCurrentLoad());
    }

    @Test
    void findLeastLoadedAvailableManager_ReturnsManager() {
        when(managerRepository.findLeastLoadedAvailableManager())
                .thenReturn(Optional.of(manager));

        Manager result = managerService.findLeastLoadedAvailableManager();

        assertSame(manager, result);

        verify(managerRepository).findLeastLoadedAvailableManager();
    }

    @Test
    void findLeastLoadedAvailableManager_ThrowsException_WhenNoManagerAvailable() {
        when(managerRepository.findLeastLoadedAvailableManager())
                .thenReturn(Optional.empty());

        assertThrows(
                NoAvailableManagerException.class,
                () -> managerService.findLeastLoadedAvailableManager()
        );
    }
}