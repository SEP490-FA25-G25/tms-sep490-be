package org.fyp.tmssep490be.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.center.CenterRequestDTO;
import org.fyp.tmssep490be.dtos.center.CenterResponseDTO;
import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.services.CenterService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/centers")
@RequiredArgsConstructor
@Slf4j
public class CenterController {

    private final CenterService centerService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResponseObject<CenterResponseDTO>> createCenter(@Valid @RequestBody CenterRequestDTO request) {
        log.info("REST request to create Center: {}", request.getCode());
        CenterResponseDTO response = centerService.createCenter(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ResponseObject.success("Center created successfully", response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CENTER_HEAD')")
    public ResponseEntity<ResponseObject<CenterResponseDTO>> getCenterById(@PathVariable Long id) {
        log.info("REST request to get Center: {}", id);
        CenterResponseDTO response = centerService.getCenterById(id);
        return ResponseEntity.ok(ResponseObject.success(response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CENTER_HEAD')")
    public ResponseEntity<ResponseObject<Page<CenterResponseDTO>>> getAllCenters(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        log.info("REST request to get all Centers with pagination: {}", pageable);
        Page<CenterResponseDTO> response = centerService.getAllCenters(pageable);
        return ResponseEntity.ok(ResponseObject.success(response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResponseObject<CenterResponseDTO>> updateCenter(
            @PathVariable Long id,
            @Valid @RequestBody CenterRequestDTO request) {
        log.info("REST request to update Center: {}", id);
        CenterResponseDTO response = centerService.updateCenter(id, request);
        return ResponseEntity.ok(ResponseObject.success("Center updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResponseObject<Void>> deleteCenter(@PathVariable Long id) {
        log.info("REST request to delete Center: {}", id);
        centerService.deleteCenter(id);
        return ResponseEntity.ok(ResponseObject.success("Center deleted successfully", null));
    }
}
