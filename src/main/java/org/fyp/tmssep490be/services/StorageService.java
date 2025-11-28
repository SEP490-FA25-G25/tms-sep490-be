package org.fyp.tmssep490be.services;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {
    String uploadFile(MultipartFile file);

    void deleteFile(String fileUrl);

    String generatePresignedUrl(String fileUrl);

    String extractKeyFromUrl(String fileUrl);
}
