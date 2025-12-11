package org.fyp.tmssep490be.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.dtos.user.UserImportData;
import org.fyp.tmssep490be.dtos.user.UserImportPreview;
import org.fyp.tmssep490be.services.ExcelParserService;
import org.fyp.tmssep490be.services.UserImportService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/users/import")
@RequiredArgsConstructor
@Slf4j
public class AdminUserImportController {

    private final UserImportService userImportService;
    private final ExcelParserService excelParserService;

    @GetMapping("/template")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Resource> downloadTemplate() {
        log.info("API: Tải mẫu import user");
        ByteArrayInputStream in = excelParserService.generateUserImportTemplate();
        
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=user_import_template.xlsx");
        
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(new InputStreamResource(in));
    }

    @PostMapping("/preview")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResponseObject<UserImportPreview>> previewImport(@RequestParam("file") MultipartFile file) {
        log.info("API: Xem trước import user");
        try {
            UserImportPreview preview = userImportService.previewUserImport(file);
            return ResponseEntity.ok(ResponseObject.<UserImportPreview>builder()
                    .success(true)
                    .message("Xem trước thành công")
                    .data(preview)
                    .build());
        } catch (Exception e) {
            log.error("Lỗi xem trước", e);
            return ResponseEntity.badRequest().body(ResponseObject.<UserImportPreview>builder()
                    .success(false)
                    .message("Lỗi xem trước: " + e.getMessage())
                    .build());
        }
    }

    @PostMapping("/execute")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResponseObject<Integer>> executeImport(@RequestBody List<UserImportData> users) {
        log.info("API: Thực hiện import user");
        try {
            int importedCount = userImportService.executeUserImport(users);
            return ResponseEntity.ok(ResponseObject.<Integer>builder()
                    .success(true)
                    .message("Import thành công " + importedCount + " người dùng")
                    .data(importedCount)
                    .build());
        } catch (Exception e) {
            log.error("Lỗi thực thi", e);
            return ResponseEntity.internalServerError().body(ResponseObject.<Integer>builder()
                    .success(false)
                    .message("Lỗi import: " + e.getMessage())
                    .build());
        }
    }
}
