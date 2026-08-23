package com.agentflow.documentservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddVersionRequest {
    @NotBlank(message = "File name is required")
    private String fileName;
    @NotBlank(message = "Content type is required")
    String contentType;
}
