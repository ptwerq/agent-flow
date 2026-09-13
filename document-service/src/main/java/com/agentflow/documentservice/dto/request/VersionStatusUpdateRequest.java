package com.agentflow.documentservice.dto.request;

import com.agentflow.documentservice.entity.DocumentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VersionStatusUpdateRequest {
    @NotNull(message = "Status is required")
    private DocumentStatus status;
}
