package com.agentflow.clientservice.mapper;

import com.agentflow.clientservice.dto.event.ClientCreatedEvent;
import com.agentflow.clientservice.dto.request.ClientRequest;
import com.agentflow.clientservice.dto.response.ClientResponse;
import com.agentflow.clientservice.dto.request.ClientStatusUpdateRequest;
import com.agentflow.clientservice.dto.request.ClientUpdateRequest;
import com.agentflow.clientservice.entity.Client;
import org.mapstruct.*;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface ClientMapper {
    Client toEntity(ClientRequest clientRequest);
    ClientResponse toResponse(Client client);
    void updateEntityFromUpdateRequest(ClientUpdateRequest clientUpdateRequest, @MappingTarget Client client);
    void updateEntityFromStatusRequest(ClientStatusUpdateRequest clientStatusUpdateRequest, @MappingTarget Client client);

    @Mapping(target = "clientId", source = "id")
    ClientCreatedEvent toClientCreatedEvent(Client client);

}
