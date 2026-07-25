package com.agentflow.clientservice.mapper;

import com.agentflow.clientservice.dto.ClientRequest;
import com.agentflow.clientservice.dto.ClientResponse;
import com.agentflow.clientservice.dto.ClientStatusUpdateRequest;
import com.agentflow.clientservice.dto.ClientUpdateRequest;
import com.agentflow.clientservice.entity.Client;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface ClientMapper {
    Client toEntity(ClientRequest clientRequest);
    ClientResponse toResponse(Client client);
    void updateEntityFromUpdateRequest(ClientUpdateRequest clientUpdateRequest, @MappingTarget Client client);
    void updateEntityFromStatusRequest(ClientStatusUpdateRequest clientStatusUpdateRequest, @MappingTarget Client client);
}
