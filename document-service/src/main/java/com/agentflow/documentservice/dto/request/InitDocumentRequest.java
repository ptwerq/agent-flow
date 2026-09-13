package com.agentflow.documentservice.dto.request;

import com.agentflow.documentservice.entity.DocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InitDocumentRequest {
    @Positive
    @NotNull(message = "Client id is required")
    private Long clientId;
    @NotNull(message = "Document type is required")
    private DocumentType documentType;

    @NotBlank(message = "File name is required")
    private String fileName;
    @NotBlank(message = "Content type is required")
    private String contentType;
}
