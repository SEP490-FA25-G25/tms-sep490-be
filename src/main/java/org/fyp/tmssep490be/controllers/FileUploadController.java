package org.fyp.tmssep490be.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.exceptions.CustomException;
import org.fyp.tmssep490be.exceptions.ErrorCode;
import org.fyp.tmssep490be.services.S3StorageService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/upload")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "File Upload", description = "File upload APIs for S3")
@SecurityRequirement(name = "bearerAuth")
public class FileUploadController {

    private final S3StorageService storageService;

    // Allowed file types: Images, PDF, Word, Excel, PowerPoint
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            // Images
            "image/jpeg",
            "image/png",
            "image/webp",
            // PDF
            "application/pdf",
            // Word
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            // Excel
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            // PowerPoint
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation");

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            // Images
            ".jpg", ".jpeg", ".png", ".webp",
            // Documents
            ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx");

    // Max file size: 20MB for all files
    private static final long MAX_FILE_SIZE = 20 * 1024 * 1024; // 20MB

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload file", description = "Upload images (JPG, PNG, WebP) or documents (PDF, Word, Excel, PowerPoint) to S3. Max 20MB.")
    public ResponseEntity<ResponseObject<Map<String, String>>> uploadFile(
            @RequestParam("file") MultipartFile file) {

        // Validate file
        validateFile(file);

        log.info("Uploading file: {} ({})", file.getOriginalFilename(), file.getSize());
        String fileUrl = storageService.uploadFile(file);
        return ResponseEntity.ok(ResponseObject.<Map<String, String>>builder()
                .success(true)
                .message("File uploaded successfully")
                .data(Map.of("url", fileUrl))
                .build());
    }

    @DeleteMapping
    @Operation(summary = "Delete file", description = "Delete a file from S3")
    public ResponseEntity<ResponseObject<Void>> deleteFile(@RequestParam("url") String fileUrl) {
        log.info("Deleting file: {}", fileUrl);
        storageService.deleteFile(fileUrl);
        return ResponseEntity.ok(ResponseObject.<Void>builder()
                .success(true)
                .message("File deleted successfully")
                .build());
    }

    @GetMapping("/presigned")
    @Operation(summary = "Get presigned URL", description = "Generate a presigned URL for secure file access")
    public ResponseEntity<ResponseObject<Map<String, String>>> getPresignedUrl(
            @RequestParam("url") String fileUrl) {
        log.info("Generating presigned URL for: {}", fileUrl);
        String presignedUrl = storageService.generatePresignedUrl(fileUrl);
        return ResponseEntity.ok(ResponseObject.<Map<String, String>>builder()
                .success(true)
                .message("Presigned URL generated")
                .data(Map.of("url", presignedUrl))
                .build());
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "File không được để trống");
        }

        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "Tên file không hợp lệ");
        }

        // Check file extension
        String lowerFilename = filename.toLowerCase();
        boolean validExtension = ALLOWED_EXTENSIONS.stream().anyMatch(lowerFilename::endsWith);
        if (!validExtension) {
            throw new CustomException(ErrorCode.INVALID_INPUT,
                    "Định dạng file không được hỗ trợ. Chỉ cho phép: JPG, PNG, WebP, PDF, Word, Excel, PowerPoint");
        }

        // Check content type (allow null for some browsers)
        String contentType = file.getContentType();
        if (contentType != null && !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new CustomException(ErrorCode.INVALID_INPUT,
                    "Loại file không được hỗ trợ. Chỉ cho phép: Hình ảnh, PDF, Word, Excel, PowerPoint");
        }

        // Check file size
        long fileSize = file.getSize();
        if (fileSize > MAX_FILE_SIZE) {
            throw new CustomException(ErrorCode.INVALID_INPUT,
                    "Kích thước file vượt quá giới hạn cho phép (20MB)");
        }

        log.debug("File validation passed: {} ({} bytes, {})", filename, fileSize, contentType);
    }
}
