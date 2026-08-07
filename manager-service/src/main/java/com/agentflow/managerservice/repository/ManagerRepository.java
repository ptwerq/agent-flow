package com.agentflow.managerservice.repository;

import com.agentflow.managerservice.entity.Manager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ManagerRepository extends JpaRepository<Manager, Long> {
    Optional<Manager> findByIdAndIsDeletedFalse(Long id);
    @Query("SELECT m FROM Manager m " +
            "WHERE m.isDeleted = false " +
            "AND m.status = 'ACTIVE' " +
            "AND m.currentLoad < m.maxCapacity " +
            "ORDER BY m.currentLoad ASC " +
            "LIMIT 1")
    Optional<Manager> findLeastLoadedAvailableManager();
    Page<Manager> findAllByIsDeletedFalse(Pageable pageable);
}
