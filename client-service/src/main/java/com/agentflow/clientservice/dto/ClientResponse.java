package com.agentflow.clientservice.dto;

import com.agentflow.clientservice.entity.DealStatus;
import com.agentflow.clientservice.entity.ServiceType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ClientResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private Long managerId;
    private DealStatus dealStatus;
    private ServiceType serviceType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
