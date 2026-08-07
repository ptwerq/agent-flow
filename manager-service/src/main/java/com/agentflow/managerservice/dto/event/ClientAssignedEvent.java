package com.agentflow.managerservice.dto.event;

public record ClientAssignedEvent(
        Long clientId,
        Long managerId
) {
}

