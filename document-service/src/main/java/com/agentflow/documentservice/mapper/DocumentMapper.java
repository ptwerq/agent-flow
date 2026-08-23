package com.agentflow.documentservice.mapper;


import com.agentflow.documentservice.dto.request.InitDocumentRequest;
import com.agentflow.documentservice.dto.response.DocumentResponse;
import com.agentflow.documentservice.dto.response.VersionResponse;
import com.agentflow.documentservice.entity.Document;
import com.agentflow.documentservice.entity.DocumentVersion;
import org.mapstruct.*;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface DocumentMapper {
    Document toEntity(InitDocumentRequest initDocumentRequest);
    DocumentResponse toResponse(Document document);
    VersionResponse toResponse(DocumentVersion documentVersion);
}
