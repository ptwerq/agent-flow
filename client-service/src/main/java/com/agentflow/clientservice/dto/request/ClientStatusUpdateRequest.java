package com.agentflow.clientservice.dto.request;

import com.agentflow.clientservice.entity.DealStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientStatusUpdateRequest {
    @NotNull(message = "Deal status is required")
    private DealStatus dealStatus;
    private String comment;
}
