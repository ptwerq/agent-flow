package com.agentflow.clientservice.dto;

import com.agentflow.clientservice.entity.DealStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClientStatusUpdateRequest {
    @NotNull(message = "Deal status is required")
    private DealStatus dealStatus;
    private String comment;
}
