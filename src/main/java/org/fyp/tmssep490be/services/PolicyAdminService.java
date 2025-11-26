package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.dtos.policy.PolicyResponse;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PolicyAdminService {

    Page<PolicyResponse> getPolicies(String search, String category, String scope, Pageable pageable);

    PolicyResponse updatePolicy(Long id, String newValue, String reason, Long currentUserId);
}


