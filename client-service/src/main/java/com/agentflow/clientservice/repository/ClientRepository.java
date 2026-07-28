package com.agentflow.clientservice.repository;

import com.agentflow.clientservice.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClientRepository extends JpaRepository<Client, Long> {
    Optional<Client> findByIdAndIsDeletedFalse(Long id);
    List<Client> findAllByIsDeletedFalse();
}
