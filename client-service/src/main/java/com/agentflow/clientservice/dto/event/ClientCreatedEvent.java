package com.agentflow.clientservice.dto.event;

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
