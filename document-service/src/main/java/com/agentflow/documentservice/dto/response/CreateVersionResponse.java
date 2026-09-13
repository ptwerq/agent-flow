package com.agentflow.documentservice.dto.response;

import com.agentflow.documentservice.entity.DocumentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateVersionResponse {
    private Long id;
    private Integer version;
    private DocumentStatus status;
    private String fileName;
    private String contentType;
    private String uploadUrl;
    private Map<String, String> formData;
    private LocalDateTime createdAt;
}
