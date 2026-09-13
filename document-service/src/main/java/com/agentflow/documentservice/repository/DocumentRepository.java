package com.agentflow.documentservice.repository;

import com.agentflow.documentservice.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    Optional<Document> findByIdAndIsDeletedFalse(Long id);
}
