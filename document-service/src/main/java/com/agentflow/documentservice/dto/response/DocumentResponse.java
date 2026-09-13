package com.agentflow.documentservice.dto.response;

import com.agentflow.documentservice.entity.DocumentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentResponse {
    private Long id;
    private Long clientId;
    private DocumentType documentType;
    private LocalDateTime createdAt;
}
