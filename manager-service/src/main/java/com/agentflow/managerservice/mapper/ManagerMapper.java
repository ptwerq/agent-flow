package com.agentflow.managerservice.mapper;

import com.agentflow.managerservice.dto.request.ManagerRequest;
import com.agentflow.managerservice.dto.request.ManagerStatusUpdateRequest;
import com.agentflow.managerservice.dto.request.ManagerUpdateRequest;
import com.agentflow.managerservice.dto.response.ManagerResponse;
import com.agentflow.managerservice.entity.Manager;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface ManagerMapper {
    Manager toEntity(ManagerRequest managerRequest);
    ManagerResponse toResponse(Manager manager);
    void updateManagerFromUpdateRequest(ManagerUpdateRequest managerUpdateRequest,
                                        @MappingTarget Manager manager);
    void updateManagerFromStatusRequest(ManagerStatusUpdateRequest managerStatusUpdateRequest,
                                        @MappingTarget Manager manager);
}
