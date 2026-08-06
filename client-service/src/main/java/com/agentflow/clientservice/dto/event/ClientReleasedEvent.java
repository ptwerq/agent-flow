package com.agentflow.clientservice.dto.event;

import java.time.LocalDateTime;

public record ClientReleasedEvent(
        Long clientId,
        Long managerId,
        LocalDateTime releasedAt
) {
}
