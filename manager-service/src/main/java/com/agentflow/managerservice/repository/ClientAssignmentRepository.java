package com.agentflow.managerservice.repository;

import com.agentflow.managerservice.entity.AssignmentStatus;
import com.agentflow.managerservice.entity.ClientAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClientAssignmentRepository extends JpaRepository<ClientAssignment, Long> {
    Optional<ClientAssignment> findByClientIdAndStatus(Long clientId, AssignmentStatus status);

    boolean existsByClientId(Long clientId);
}
