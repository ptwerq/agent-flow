package com.agentflow.documentservice.controller;

import com.agentflow.documentservice.dto.request.AddVersionRequest;
import com.agentflow.documentservice.dto.request.InitDocumentRequest;
import com.agentflow.documentservice.dto.request.VersionStatusUpdateRequest;
import com.agentflow.documentservice.dto.response.CreateVersionResponse;
import com.agentflow.documentservice.dto.response.InitDocumentResponse;
import com.agentflow.documentservice.dto.response.VersionResponse;
import com.agentflow.documentservice.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
@Tag(name = "Document service controller")
public class DocumentController {
    private final DocumentService documentService;

    @Operation(summary = "Initialize document upload")
    @PostMapping
    public ResponseEntity<InitDocumentResponse> createDocument(@Valid @RequestBody InitDocumentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(documentService.createDocument(request));
    }

    @Operation(summary = "Complete document upload")
    @PatchMapping("{documentId}/version")
    public ResponseEntity<Void> completeUpload(@PathVariable Long documentId,
                                               @RequestParam Integer version) {
        documentService.completeUpload(documentId, version);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Update version status")
    @PatchMapping("{documentId}/version/status")
    public ResponseEntity<VersionResponse> updateVersionStatus(@PathVariable Long documentId,
                                                               @RequestParam Integer version,
                                                               @Valid @RequestBody VersionStatusUpdateRequest request) {
        return ResponseEntity.ok(documentService.updateVersionStatus(documentId, version, request));
    }

    @Operation(summary = "Delete document and versions from MinIO")
    @DeleteMapping("{documentId}")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long documentId) {
        documentService.deleteDocument(documentId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get download url")
    @GetMapping("{documentId}/version/download")
    public ResponseEntity<String> getDocumentVersionDownloadUrl(@PathVariable Long documentId,
                                                                @RequestParam Integer version) {
        return ResponseEntity.ok(documentService.getDocumentDownloadUrl(documentId, version));
    }

    @Operation(summary = "Get document version")
    @GetMapping("{documentId}/version")
    public ResponseEntity<VersionResponse> getDocumentVersion(@PathVariable Long documentId,
                                                              @RequestParam Integer version) {
        return ResponseEntity.ok(documentService.getDocumentVersion(documentId, version));
    }

    @Operation(summary = "Create document version")
    @PostMapping("{documentId}")
    public ResponseEntity<CreateVersionResponse> createDocumentVersion(@PathVariable Long documentId,
                                                                       @Valid @RequestBody AddVersionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(documentService.createVersion(documentId, request));
    }
}
