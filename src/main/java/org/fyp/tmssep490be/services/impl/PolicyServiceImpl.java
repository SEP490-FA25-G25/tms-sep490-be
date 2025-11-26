package org.fyp.tmssep490be.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.entities.SystemPolicy;
import org.fyp.tmssep490be.repositories.SystemPolicyRepository;
import org.fyp.tmssep490be.services.PolicyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PolicyServiceImpl implements PolicyService {

    private final SystemPolicyRepository systemPolicyRepository;

    @Override
    @Transactional(readOnly = true)
    public int getGlobalInt(String policyKey, int defaultValue) {
        return systemPolicyRepository.findFirstByPolicyKeyAndScopeOrderByIdAsc(policyKey, "GLOBAL")
                .map(SystemPolicy::getCurrentValue)
                .map(value -> {
                    try {
                        return Integer.parseInt(value);
                    } catch (NumberFormatException ex) {
                        log.warn("Policy {} has non-integer value '{}', using default {}", policyKey, value, defaultValue);
                        return defaultValue;
                    }
                })
                .orElse(defaultValue);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean getGlobalBoolean(String policyKey, boolean defaultValue) {
        return systemPolicyRepository.findFirstByPolicyKeyAndScopeOrderByIdAsc(policyKey, "GLOBAL")
                .map(SystemPolicy::getCurrentValue)
                .map(value -> {
                    if (value == null) {
                        return defaultValue;
                    }
                    String normalized = value.trim().toLowerCase();
                    if ("true".equals(normalized) || "1".equals(normalized) || "yes".equals(normalized)) {
                        return true;
                    }
                    if ("false".equals(normalized) || "0".equals(normalized) || "no".equals(normalized)) {
                        return false;
                    }
                    log.warn("Policy {} has non-boolean value '{}', using default {}", policyKey, value, defaultValue);
                    return defaultValue;
                })
                .orElse(defaultValue);
    }
}


