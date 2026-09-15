package com.agentflow.documentservice.mapper;

import com.agentflow.documentservice.dto.event.DocumentUploadedEvent;
import com.agentflow.documentservice.entity.Document;
import com.agentflow.documentservice.entity.DocumentVersion;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface DocumentEventMapper {

    @Mapping(target = "documentId", source = "document.id")
    @Mapping(target = "clientId", source = "document.clientId")
    @Mapping(target = "documentType", source = "document.documentType")
    @Mapping(target = "version", source = "documentVersion.version")
    DocumentUploadedEvent toEvent(Document document,
                                  DocumentVersion documentVersion);
}
