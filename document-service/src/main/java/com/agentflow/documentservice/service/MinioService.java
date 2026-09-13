package com.agentflow.documentservice.service;

import com.agentflow.documentservice.config.MinioProperties;
import com.agentflow.documentservice.dto.response.PresignedPostData;
import com.agentflow.documentservice.exception.MinioStorageException;
import com.agentflow.documentservice.exception.NotFoundException;
import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.MinioException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MinioService {
    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    public String generatePresignedDownloadUrl(String objectKey) {
        validateObjectKey(objectKey);
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Http.Method.GET)
                            .bucket(minioProperties.getBucket())
                            .object(objectKey)
                            .expiry(minioProperties.getPresignedUrlExpiry())
                            .build());
        } catch (Exception e) {
            throw new MinioStorageException("Couldn't generate URL for objectKey=" + objectKey, e);
        }
    }

    public PresignedPostData generatePresignedPostFormData(String objectKey) {
        validateObjectKey(objectKey);
        PostPolicy policy = new PostPolicy(
                minioProperties.getBucket(),
                ZonedDateTime.now().plusMinutes(10)
        );
        policy.addContentLengthRangeCondition(0, 10 * 1024 * 1024);
        policy.addEqualsCondition("key", objectKey);
        try {
           Map<String, String> formData = minioClient.getPresignedPostFormData(policy);
           String uploadUrl = minioProperties.getEndpoint() + "/" + minioProperties.getBucket();
           return PresignedPostData
                   .builder()
                   .formData(formData)
                   .uploadUrl(uploadUrl)
                   .build();
        } catch (MinioException e) {
            throw new MinioStorageException("Couldn't generate presigned post form data", e);
        }
    }

    public boolean bucketExists() {
        try {
            return minioClient.bucketExists(BucketExistsArgs.builder()
                    .bucket(minioProperties.getBucket())
                    .build());
        } catch (Exception e) {
            throw new MinioStorageException("Couldn't check if bucket exists", e);
        }
    }

    public void deleteObject(String objectKey) {
        validateObjectKey(objectKey);
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(minioProperties.getBucket())
                    .object(objectKey)
                    .build());
        } catch (Exception e) {
            throw new MinioStorageException("Couldn't remove the object by the objectKey=" + objectKey, e);
        }
    }

    public StatObjectResponse getObjectMetadata(String objectKey) {
        validateObjectKey(objectKey);
        try {
            return minioClient.statObject(StatObjectArgs.builder()
                    .bucket(minioProperties.getBucket())
                    .object(objectKey)
                    .build());
        } catch (ErrorResponseException e) {
            if ("NoSuchKey".equals(e.errorResponse().code())) {
                throw new NotFoundException("Object not found: " + objectKey);
            }
            throw new MinioStorageException("Couldn't get metadata by objectKey=" + objectKey, e);
        } catch (Exception e) {
            throw new MinioStorageException("Couldn't get metadata by objectKey=" + objectKey, e);
        }
    }

    private void validateObjectKey(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            throw new IllegalArgumentException("Object key must not be null or blank");
        }
    }

}

