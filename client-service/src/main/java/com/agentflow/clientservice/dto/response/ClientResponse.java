package com.agentflow.clientservice.dto.response;

import com.agentflow.clientservice.entity.DealStatus;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private Long managerId;
    private DealStatus dealStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
