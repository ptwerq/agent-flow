package com.agentflow.documentservice.mapper;

import com.agentflow.documentservice.dto.request.VersionStatusUpdateRequest;
import com.agentflow.documentservice.dto.response.CreateVersionResponse;
import com.agentflow.documentservice.dto.response.VersionResponse;
import com.agentflow.documentservice.entity.DocumentVersion;
import org.mapstruct.*;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface DocumentVersionMapper {
    VersionResponse toResponse(DocumentVersion documentVersion);
    @Mapping(target = "uploadUrl", ignore = true)
    CreateVersionResponse toCreateResponse(DocumentVersion documentVersion);
    void updateVersionFromStatusUpdateRequest(VersionStatusUpdateRequest request, @MappingTarget DocumentVersion documentVersion);
}
