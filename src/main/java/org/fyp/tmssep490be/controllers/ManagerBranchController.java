package org.fyp.tmssep490be.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.branch.CreateBranchRequest;
import org.fyp.tmssep490be.dtos.branch.ManagerBranchOverviewDTO;
import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.services.ManagerBranchService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/manager/branches")
@RequiredArgsConstructor
public class ManagerBranchController {

    private final ManagerBranchService managerBranchService;

    // Lấy danh sách tất cả chi nhánh
    @GetMapping
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ResponseObject<List<ManagerBranchOverviewDTO>>> getAllBranches() {
        log.info("API: Manager lấy danh sách tất cả chi nhánh");
        List<ManagerBranchOverviewDTO> branches = managerBranchService.getAllBranches();
        return ResponseEntity.ok(new ResponseObject<>(true, "Lấy danh sách chi nhánh thành công", branches));
    }

    // Lấy chi tiết một chi nhánh theo ID
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ResponseObject<ManagerBranchOverviewDTO>> getBranchById(@PathVariable Long id) {
        log.info("API: Manager lấy chi tiết chi nhánh ID: {}", id);
        ManagerBranchOverviewDTO branch = managerBranchService.getBranchById(id);
        return ResponseEntity.ok(new ResponseObject<>(true, "Lấy chi tiết chi nhánh thành công", branch));
    }

    // Tạo chi nhánh mới
    @PostMapping
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ResponseObject<ManagerBranchOverviewDTO>> createBranch(
            @Valid @RequestBody CreateBranchRequest request) {
        log.info("API: Manager tạo chi nhánh mới: {}", request.getName());
        ManagerBranchOverviewDTO createdBranch = managerBranchService.createBranch(request);
        log.info("=== BRANCH ĐÃ TẠO THÀNH CÔNG: {} ({}) ===", createdBranch.getName(), createdBranch.getCode());
        
        // Gửi thông báo đến Admin
        managerBranchService.sendNewBranchNotificationToAdmins(createdBranch.getName(), createdBranch.getCode());
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ResponseObject<>(true, "Tạo chi nhánh thành công", createdBranch));
    }

    // Kiểm tra email đã tồn tại chưa
    @GetMapping("/check-email")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ResponseObject<Boolean>> checkEmailExists(
            @RequestParam String email,
            @RequestParam(required = false) Long excludeId) {
        log.info("API: Kiểm tra email chi nhánh: {}", email);
        boolean exists = managerBranchService.checkEmailExists(email, excludeId);
        return ResponseEntity.ok(new ResponseObject<>(true, 
                exists ? "Email đã tồn tại" : "Email chưa tồn tại", exists));
    }

    // Cập nhật chi nhánh
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ResponseObject<ManagerBranchOverviewDTO>> updateBranch(
            @PathVariable Long id,
            @Valid @RequestBody org.fyp.tmssep490be.dtos.branch.UpdateBranchRequest request) {
        log.info("API: Manager cập nhật chi nhánh ID: {}", id);
        ManagerBranchOverviewDTO updatedBranch = managerBranchService.updateBranch(id, request);
        return ResponseEntity.ok(new ResponseObject<>(true, "Cập nhật chi nhánh thành công", updatedBranch));
    }

    // Ngưng hoạt động chi nhánh
    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ResponseObject<ManagerBranchOverviewDTO>> deactivateBranch(@PathVariable Long id) {
        log.info("API: Manager ngưng hoạt động chi nhánh ID: {}", id);
        ManagerBranchOverviewDTO deactivatedBranch = managerBranchService.deactivateBranch(id);
        return ResponseEntity.ok(new ResponseObject<>(true, "Đã ngưng hoạt động chi nhánh", deactivatedBranch));
    }

    // Kích hoạt lại chi nhánh
    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ResponseObject<ManagerBranchOverviewDTO>> activateBranch(@PathVariable Long id) {
        log.info("API: Manager kích hoạt lại chi nhánh ID: {}", id);
        ManagerBranchOverviewDTO activatedBranch = managerBranchService.activateBranch(id);
        return ResponseEntity.ok(new ResponseObject<>(true, "Đã kích hoạt lại chi nhánh", activatedBranch));
    }
}
