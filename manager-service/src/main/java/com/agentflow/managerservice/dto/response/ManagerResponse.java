package com.agentflow.managerservice.dto.response;

import com.agentflow.managerservice.entity.ManagerStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManagerResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private ManagerStatus status;
    private Integer maxCapacity;
    private Integer currentLoad;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
