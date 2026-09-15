package com.agentflow.documentservice.mapper;


import com.agentflow.documentservice.dto.request.InitDocumentRequest;
import com.agentflow.documentservice.entity.Document;
import org.mapstruct.*;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface DocumentMapper {
    Document toEntity(InitDocumentRequest initDocumentRequest);
}

