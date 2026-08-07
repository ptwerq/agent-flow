package com.agentflow.managerservice.dto.request;

import com.agentflow.managerservice.entity.ManagerStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManagerStatusUpdateRequest {
    @NotNull(message = "Manager status is required")
    private ManagerStatus status;
    private String comment;
}
