package com.agentflow.documentservice.dto.event;

import com.agentflow.documentservice.entity.DocumentType;

public record DocumentUploadedEvent(
        Long documentId,
        Long clientId,
        Integer version,
        DocumentType documentType
) {
}
