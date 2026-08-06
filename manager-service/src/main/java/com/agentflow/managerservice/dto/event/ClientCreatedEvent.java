package com.agentflow.managerservice.dto.event;

import java.time.LocalDateTime;

public record ClientCreatedEvent(
        Long clientId,
        String firstName,
        String lastName,
        String email,
        String phone,
        LocalDateTime createdAt
) {
}
