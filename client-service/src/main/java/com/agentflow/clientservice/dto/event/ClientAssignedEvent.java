package com.agentflow.clientservice.dto.event;

public record ClientAssignedEvent(
        Long clientId,
        Long managerId
) {
}
