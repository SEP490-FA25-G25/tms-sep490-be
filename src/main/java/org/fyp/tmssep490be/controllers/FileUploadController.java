package org.fyp.tmssep490be.controllers;

import lombok.RequiredArgsConstructor;
import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.services.StorageService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/upload")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class FileUploadController {

    private final StorageService storageService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseObject<Map<String, String>>> uploadFile(@RequestParam("file") MultipartFile file) {
        String fileUrl = storageService.uploadFile(file);
        return ResponseEntity.ok(ResponseObject.<Map<String, String>>builder()
                .success(true)
                .message("File uploaded successfully")
                .data(Map.of("url", fileUrl))
                .build());
    }
}
