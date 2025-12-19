package org.fyp.tmssep490be.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.CompletedFileUpload;
import software.amazon.awssdk.transfer.s3.model.UploadFileRequest;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3StorageService {

    @Value("${aws.s3.bucket}")
    private String bucketName;

    @Value("${aws.s3.region}")
    private String region;

    @Value("${aws.s3.access-key}")
    private String accessKey;

    @Value("${aws.s3.secret-key}")
    private String secretKey;

    private S3TransferManager transferManager;
    private S3AsyncClient s3AsyncClient;

    @PostConstruct
    public void init() {
        // Create CRT-based S3 Async Client for high performance
        this.s3AsyncClient = S3AsyncClient.crtBuilder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .maxConcurrency(100)
                .build();

        // Create Transfer Manager using the CRT client
        this.transferManager = S3TransferManager.builder()
                .s3Client(s3AsyncClient)
                .build();
    }

    @PreDestroy
    public void cleanup() {
        if (this.transferManager != null) {
            this.transferManager.close();
        }
        if (this.s3AsyncClient != null) {
            this.s3AsyncClient.close();
        }
    }

    public String uploadFile(MultipartFile file) {
        String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        Path tempFile = null;

        try {
            // Create a temporary file
            tempFile = Files.createTempFile("s3-upload-", fileName);
            file.transferTo(tempFile.toFile());

            String contentType = file.getContentType();
            if (contentType == null || contentType.equals("application/octet-stream")) {
                String lowerCaseName = fileName.toLowerCase();
                if (lowerCaseName.endsWith(".mp4"))
                    contentType = "video/mp4";
                else if (lowerCaseName.endsWith(".mov"))
                    contentType = "video/quicktime";
                else if (lowerCaseName.endsWith(".webm"))
                    contentType = "video/webm";
                else if (lowerCaseName.endsWith(".avi"))
                    contentType = "video/x-msvideo";
                else if (lowerCaseName.endsWith(".mkv"))
                    contentType = "video/x-matroska";
                else if (lowerCaseName.endsWith(".pdf"))
                    contentType = "application/pdf";
                else if (lowerCaseName.endsWith(".jpg") || lowerCaseName.endsWith(".jpeg"))
                    contentType = "image/jpeg";
                else if (lowerCaseName.endsWith(".png"))
                    contentType = "image/png";
            }

            PutObjectRequest putOb = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .contentType(contentType)
                    .build();

            UploadFileRequest uploadFileRequest = UploadFileRequest.builder()
                    .putObjectRequest(putOb)
                    .source(tempFile.toFile())
                    .build();

            // Initiate the upload
            CompletedFileUpload completedUpload = transferManager.uploadFile(uploadFileRequest).completionFuture()
                    .join();

            log.info("File uploaded successfully. ETag: {}", completedUpload.response().eTag());

            // Construct public URL
            return String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, fileName);

        } catch (IOException e) {
            log.error("Error creating temp file or transferring content: {} - {}", e.getClass().getSimpleName(), e.getMessage(), e);
            throw new RuntimeException("Failed to prepare file for upload: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error uploading file to S3: {} - {}", e.getClass().getSimpleName(), e.getMessage(), e);
            throw new RuntimeException("Failed to upload file to S3: " + e.getMessage(), e);
        } finally {
            // Clean up temporary file
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException e) {
                    log.warn("Failed to delete temporary file: {}", tempFile, e);
                }
            }
        }
    }

    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return;
        }

        try {
            // Extract key from URL
            // URL format: https://bucket.s3.region.amazonaws.com/key
            String prefix = String.format("https://%s.s3.%s.amazonaws.com/", bucketName, region);
            if (!fileUrl.startsWith(prefix)) {
                log.warn("File URL does not match S3 bucket format: {}", fileUrl);
                return;
            }

            String key = fileUrl.substring(prefix.length());
            log.info("Deleting file from S3. Key: {}", key);

            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3AsyncClient.deleteObject(deleteObjectRequest).join();
            log.info("File deleted successfully from S3: {}", key);

        } catch (Exception e) {
            log.error("Error deleting file from S3: {}", fileUrl, e);
            // We don't throw exception here to avoid breaking the transaction of the caller
        }
    }

    public String generatePresignedUrl(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return null;
        }

        try {
            String key;
            String prefix = String.format("https://%s.s3.%s.amazonaws.com/", bucketName, region);

            // Check if input is already a full S3 URL or just a key
            if (fileUrl.startsWith(prefix)) {
                // It's a full URL, extract the key
                key = fileUrl.substring(prefix.length());
            } else if (fileUrl.startsWith("http://") || fileUrl.startsWith("https://")) {
                // It's an external URL, return as-is
                return fileUrl;
            } else {
                // It's just a key (e.g., "uuid_filename.ext"), use it directly
                key = fileUrl;
            }

            try (software.amazon.awssdk.services.s3.presigner.S3Presigner presigner = software.amazon.awssdk.services.s3.presigner.S3Presigner
                    .builder()
                    .region(Region.of(region))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(accessKey, secretKey)))
                    .build()) {

                software.amazon.awssdk.services.s3.model.GetObjectRequest getObjectRequest = software.amazon.awssdk.services.s3.model.GetObjectRequest
                        .builder()
                        .bucket(bucketName)
                        .key(key)
                        .build();

                software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest presignRequest = software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest
                        .builder()
                        .signatureDuration(java.time.Duration.ofMinutes(60))
                        .getObjectRequest(getObjectRequest)
                        .build();

                software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest presignedRequest = presigner
                        .presignGetObject(presignRequest);
                return presignedRequest.url().toString();
            }
        } catch (Exception e) {
            log.error("Error generating pre-signed URL for: {}", fileUrl, e);
            return fileUrl; // Fallback to original URL
        }
    }

    public String extractKeyFromUrl(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return null;
        }

        try {
            // Check if it's a standard S3 URL
            String prefix = String.format("https://%s.s3.%s.amazonaws.com/", bucketName, region);
            if (fileUrl.startsWith(prefix)) {
                String key = fileUrl.substring(prefix.length());

                // Recursively decode until no more encoding or max iterations reached
                // This handles doubly/triply encoded chars like %253F -> %3F -> ?
                String previousKey = "";
                int iterations = 0;
                while (!key.equals(previousKey) && iterations < 5) {
                    previousKey = key;
                    try {
                        key = java.net.URLDecoder.decode(key, java.nio.charset.StandardCharsets.UTF_8);
                    } catch (Exception e) {
                        // If decoding fails, stop and use current key
                        break;
                    }
                    iterations++;
                }

                // Now strip any query parameters
                if (key.contains("?")) {
                    key = key.substring(0, key.indexOf("?"));
                }

                return key;
            }
        } catch (Exception e) {
            log.error("Error extracting key from URL: {}", fileUrl, e);
        }

        return fileUrl;
    }
}
