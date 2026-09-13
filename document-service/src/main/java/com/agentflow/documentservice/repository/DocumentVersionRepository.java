package com.agentflow.documentservice.repository;

import com.agentflow.documentservice.entity.DocumentVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DocumentVersionRepository extends JpaRepository<DocumentVersion, Long> {
    @Query("SELECT MAX(dv.version) " +
            "FROM DocumentVersion dv " +
            "WHERE dv.document.id = :documentId")
    Optional<Integer> findMaxVersionByDocumentId(@Param("documentId") Long documentId);

    @Query("SELECT dv " +
            "FROM DocumentVersion dv " +
            "WHERE dv.document.id = :documentId " +
            "AND dv.version = :version " +
            "AND dv.document.isDeleted = false")
    Optional<DocumentVersion> findDocumentVersionByDocumentIdAndVersion(@Param("documentId") Long documentId,
                                                                        @Param("version") Integer version);

    @Query("SELECT dv " +
            "FROM DocumentVersion dv " +
            "WHERE dv.document.id = :documentId")
    List<DocumentVersion> findAllVersionsByDocumentId(@Param("documentId") Long documentId);
}
