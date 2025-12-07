package org.fyp.tmssep490be.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.entities.SystemPolicy;
import org.fyp.tmssep490be.repositories.SystemPolicyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
@Slf4j
public class PolicyService {

    private final SystemPolicyRepository systemPolicyRepository;

    @Transactional(readOnly = true)
    public String getGlobalString(String policyKey, String defaultValue) {
        return systemPolicyRepository.findFirstByPolicyKeyOrderByIdAsc(policyKey)
                .map(SystemPolicy::getCurrentValue)
                .filter(value -> value != null && !value.trim().isEmpty())
                .orElse(defaultValue);
    }

}
