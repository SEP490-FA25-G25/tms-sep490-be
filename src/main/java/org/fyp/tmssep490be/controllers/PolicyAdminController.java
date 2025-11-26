package org.fyp.tmssep490be.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.dtos.policy.PolicyResponse;
import org.fyp.tmssep490be.dtos.policy.UpdatePolicyRequest;
import org.fyp.tmssep490be.security.UserPrincipal;
import org.fyp.tmssep490be.services.PolicyAdminService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/policies")
@Tag(name = "Policy Admin", description = "Quản lý system policies (ADMIN)")
@RequiredArgsConstructor
@Validated
@Slf4j
public class PolicyAdminController {

    private final PolicyAdminService policyAdminService;

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal userPrincipal) {
            return userPrincipal.getId();
        }
        throw new RuntimeException("Không xác định được người dùng hiện tại");
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Danh sách policies", description = "Lấy danh sách system policies với filter và pagination")
    public ResponseEntity<ResponseObject<Page<PolicyResponse>>> getPolicies(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String scope
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<PolicyResponse> result = policyAdminService.getPolicies(search, category, scope, pageable);
        return ResponseEntity.ok(ResponseObject.success("Lấy danh sách policy thành công", result));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cập nhật giá trị policy", description = "Admin cập nhật business rule thông qua system policy")
    public ResponseEntity<ResponseObject<PolicyResponse>> updatePolicy(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePolicyRequest request
    ) {
        Long userId = getCurrentUserId();
        PolicyResponse updated = policyAdminService.updatePolicy(id, request.getNewValue(), request.getReason(), userId);
        return ResponseEntity.ok(ResponseObject.success("Cập nhật policy thành công", updated));
    }
}


