package com.agentflow.managerservice.mapper;

import com.agentflow.managerservice.entity.ClientAssignment;
import com.agentflow.managerservice.entity.Manager;
import com.agentflow.managerservice.entity.AssignmentStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, imports = {AssignmentStatus.class})
public interface ClientAssignmentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "clientId", source = "clientId")
    @Mapping(target = "manager", source = "manager")
    @Mapping(target = "status", expression = "java(AssignmentStatus.ACTIVE)")
    @Mapping(target = "assignedAt", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "releasedAt", ignore = true)
    ClientAssignment toEntity(Long clientId, Manager manager);
}

