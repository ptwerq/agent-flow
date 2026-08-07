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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ManagerService {
    private final ManagerRepository managerRepository;
    private final ManagerMapper managerMapper;

    @Transactional
    public ManagerResponse create(ManagerRequest managerRequest) {
        Manager manager = managerMapper.toEntity(managerRequest);
        Manager savedManager = managerRepository.save(manager);
        return managerMapper.toResponse(savedManager);
    }

    public ManagerResponse getById(Long id) {
        Manager manager = getEntityById(id);
        return managerMapper.toResponse(manager);
    }

    public Page<ManagerResponse> getAll(Pageable pageable) {
        return managerRepository.findAllByIsDeletedFalse(pageable)
                .map(managerMapper::toResponse);
    }

    @Transactional
    public ManagerResponse update(Long id, ManagerUpdateRequest updated) {
        Manager manager = getEntityById(id);
        managerMapper.updateManagerFromUpdateRequest(updated, manager);
        return managerMapper.toResponse(manager);
    }

    @Transactional
    public ManagerResponse updateStatus(Long id, ManagerStatusUpdateRequest updated) {
        Manager manager = getEntityById(id);
        managerMapper.updateManagerFromStatusRequest(updated, manager);
        return managerMapper.toResponse(manager);
    }


    @Transactional
    public void delete(Long id) {
        Manager manager = getEntityById(id);
        if (manager.getCurrentLoad() > 0) {
            throw new IllegalStateException("Cannot delete manager with active clients: " + id);
        }
        manager.setIsDeleted(true);
    }

    @Transactional
    public void increaseLoad(Long id) {
        Manager manager = getEntityById(id);
        if (manager.getCurrentLoad() >= manager.getMaxCapacity()) {
            throw new ManagerCapacityExceededException("Manager capacity exceeded: " + id);
        }
        int newLoad = manager.getCurrentLoad() + 1;
        manager.setCurrentLoad(newLoad);
        if (newLoad == manager.getMaxCapacity()) {
            manager.setStatus(ManagerStatus.BUSY);
        }
    }


    @Transactional
    public void decreaseLoad(Long id) {
        Manager manager = getEntityById(id);
        if (manager.getCurrentLoad() <= 0) {
            throw new InvalidManagerLoadException("Manager's current load is invalid: " + id);
        }
        Integer newLoad = Math.max(0, manager.getCurrentLoad() - 1);
        manager.setCurrentLoad(newLoad);
        if (manager.getStatus() == ManagerStatus.BUSY && newLoad < manager.getMaxCapacity()) {
            manager.setStatus(ManagerStatus.ACTIVE);
        }
    }

    public Manager findLeastLoadedAvailableManager() {
        return managerRepository.findLeastLoadedAvailableManager()
                .orElseThrow(() -> new NoAvailableManagerException("No available manager found"));
    }

    private Manager getEntityById(Long id) {
        return managerRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new NotFoundException("Manager not found: " + id));
    }

}
