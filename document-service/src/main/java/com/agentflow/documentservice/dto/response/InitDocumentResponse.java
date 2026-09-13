package com.agentflow.documentservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InitDocumentResponse {
    private Long documentId;
    private Integer version;
    private String objectKey;
    private String uploadUrl;
    private Map<String, String> uploadFormData;
}
