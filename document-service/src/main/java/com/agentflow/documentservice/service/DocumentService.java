package com.agentflow.documentservice.service;

import com.agentflow.documentservice.dto.request.AddVersionRequest;
import com.agentflow.documentservice.dto.request.InitDocumentRequest;
import com.agentflow.documentservice.dto.request.VersionStatusUpdateRequest;
import com.agentflow.documentservice.dto.response.CreateVersionResponse;
import com.agentflow.documentservice.dto.response.InitDocumentResponse;
import com.agentflow.documentservice.dto.response.PresignedPostData;
import com.agentflow.documentservice.dto.response.VersionResponse;
import com.agentflow.documentservice.entity.Document;
import com.agentflow.documentservice.entity.DocumentVersion;
import com.agentflow.documentservice.exception.NotFoundException;
import com.agentflow.documentservice.mapper.DocumentMapper;
import com.agentflow.documentservice.mapper.DocumentVersionMapper;
import com.agentflow.documentservice.repository.DocumentRepository;
import com.agentflow.documentservice.repository.DocumentVersionRepository;
import io.minio.StatObjectResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {
    private final DocumentRepository documentRepository;
    private final DocumentVersionRepository documentVersionRepository;
    private final MinioService minioService;
    private final DocumentMapper documentMapper;
    private final DocumentVersionMapper documentVersionMapper;

    @Transactional
    public InitDocumentResponse createDocument(InitDocumentRequest request) {
        Document document = documentMapper.toEntity(request);
        Document savedDocument = documentRepository.save(document);

        String objectKey = generateObjectKey(
                savedDocument.getClientId(),
                savedDocument.getId(),
                1);

        DocumentVersion documentVersion = DocumentVersion.builder()
                .document(savedDocument)
                .version(1)
                .objectKey(objectKey)
                .fileName(request.getFileName())
                .contentType(request.getContentType())
                .build();

        DocumentVersion savedDocumentVersion = documentVersionRepository.save(documentVersion);
        PresignedPostData presignedPostData = minioService.generatePresignedPostFormData(objectKey);

        return InitDocumentResponse.builder()
                .documentId(savedDocument.getId())
                .version(savedDocumentVersion.getVersion())
                .objectKey(objectKey)
                .uploadUrl(presignedPostData.getUploadUrl())
                .uploadFormData(presignedPostData.getFormData())
                .build();
    }

    @Transactional
    public void completeUpload(Long documentId, Integer version) {
        DocumentVersion documentVersion = getDocumentVersionByDocumentIdAndVersion(documentId, version);
        String objectKey = documentVersion.getObjectKey();
        StatObjectResponse metadata = minioService.getObjectMetadata(objectKey);
        documentVersion.setFileSize(metadata.size());
    }

    @Transactional
    public void deleteDocument(Long documentId) {
        Document document = getDocumentEntityById(documentId);
        List<DocumentVersion> documentVersions = documentVersionRepository.findAllVersionsByDocumentId(documentId);
        documentVersions.stream()
                .map(DocumentVersion::getObjectKey)
                .forEach(minioService::deleteObject);
        document.setIsDeleted(true);
    }

    @Transactional
    public VersionResponse updateVersionStatus(Long documentId, Integer version, VersionStatusUpdateRequest request) {
        DocumentVersion documentVersion = getDocumentVersionByDocumentIdAndVersion(documentId, version);
        documentVersionMapper.updateVersionFromStatusUpdateRequest(request, documentVersion);
        return documentVersionMapper.toResponse(documentVersion);
    }

    @Transactional
    public CreateVersionResponse createVersion(Long documentId, AddVersionRequest request) {
        Document document = getDocumentEntityById(documentId);

        Integer version = getMaxVersionByDocumentId(documentId) + 1;
        String objectKey = generateObjectKey(document.getClientId(), documentId, version);

        DocumentVersion documentVersion = DocumentVersion.builder()
                .document(document)
                .version(version)
                .objectKey(objectKey)
                .fileName(request.getFileName())
                .contentType(request.getContentType())
                .build();

        DocumentVersion savedVersion = documentVersionRepository.save(documentVersion);
        PresignedPostData presignedPostData = minioService.generatePresignedPostFormData(objectKey);

        CreateVersionResponse createVersionResponse = documentVersionMapper.toCreateResponse(savedVersion);
        createVersionResponse.setUploadUrl(presignedPostData.getUploadUrl());
        createVersionResponse.setFormData(presignedPostData.getFormData());

        return createVersionResponse;
    }


    public String getDocumentDownloadUrl(Long documentId, Integer version) {
        DocumentVersion documentVersion = getDocumentVersionByDocumentIdAndVersion(documentId, version);
        String objectKey = documentVersion.getObjectKey();
        return minioService.generatePresignedDownloadUrl(objectKey);
    }

    public VersionResponse getDocumentVersion(Long documentId, Integer version) {
        return documentVersionMapper.toResponse(getDocumentVersionByDocumentIdAndVersion(documentId, version));
    }

    private Integer getMaxVersionByDocumentId(Long documentId) {
        return documentVersionRepository.findMaxVersionByDocumentId(documentId)
                .orElseThrow(() -> new NotFoundException("Max version not found: " + documentId));
    }

    private DocumentVersion getDocumentVersionByDocumentIdAndVersion(Long documentId, Integer version) {
        return documentVersionRepository.findDocumentVersionByDocumentIdAndVersion(documentId, version)
                .orElseThrow(() -> new NotFoundException("Document version not found: documentId=" + documentId +
                        ", version=" + version));
    }

    private Document getDocumentEntityById(Long documentId) {
        return documentRepository.findByIdAndIsDeletedFalse(documentId)
                .orElseThrow(() -> new NotFoundException("Document not found: " + documentId));
    }

    private String generateObjectKey(Long clientId, Long documentId, Integer version) {
        String uuid = UUID.randomUUID().toString();
        return String.format("%d-%d-%d-%s", clientId, documentId, version, uuid);
    }
}
