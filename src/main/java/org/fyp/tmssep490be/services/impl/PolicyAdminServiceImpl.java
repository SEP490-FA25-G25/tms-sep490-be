package org.fyp.tmssep490be.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.policy.PolicyResponse;
import org.fyp.tmssep490be.entities.PolicyHistory;
import org.fyp.tmssep490be.entities.SystemPolicy;
import org.fyp.tmssep490be.exceptions.BusinessRuleException;
import org.fyp.tmssep490be.exceptions.ErrorCode;
import org.fyp.tmssep490be.repositories.PolicyHistoryRepository;
import org.fyp.tmssep490be.repositories.SystemPolicyRepository;
import org.fyp.tmssep490be.services.PolicyAdminService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PolicyAdminServiceImpl implements PolicyAdminService {

    private final SystemPolicyRepository systemPolicyRepository;
    private final PolicyHistoryRepository policyHistoryRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<PolicyResponse> getPolicies(String search, String category, String scope, Pageable pageable) {
        List<SystemPolicy> all = systemPolicyRepository.findAll();

        String normalizedSearch = normalize(search);
        String normalizedCategory = normalize(category);
        String normalizedScope = normalize(scope);

        List<SystemPolicy> filtered = all.stream()
                .filter(p -> {
                    if (normalizedSearch != null) {
                        String target = (p.getPolicyKey() + " " + p.getPolicyName() + " " + safe(p.getDescription()))
                                .toLowerCase(Locale.ROOT);
                        if (!target.contains(normalizedSearch)) {
                            return false;
                        }
                    }
                    if (normalizedCategory != null && (p.getPolicyCategory() == null
                            || !p.getPolicyCategory().equalsIgnoreCase(normalizedCategory))) {
                        return false;
                    }
                    if (normalizedScope != null && (p.getScope() == null
                            || !p.getScope().equalsIgnoreCase(normalizedScope))) {
                        return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());

        int pageSize = pageable.getPageSize();
        int pageNumber = pageable.getPageNumber();
        int fromIndex = pageNumber * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, filtered.size());

        List<PolicyResponse> content;
        if (fromIndex >= filtered.size()) {
            content = List.of();
        } else {
            content = filtered.subList(fromIndex, toIndex).stream()
                    .map(this::toResponse)
                    .collect(Collectors.toList());
        }

        return new PageImpl<>(content, pageable, filtered.size());
    }

    @Override
    @Transactional
    public PolicyResponse updatePolicy(Long id, String newValue, String reason, Long currentUserId) {
        SystemPolicy policy = systemPolicyRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException(
                        ErrorCode.RESOURCE_NOT_FOUND.name(),
                        "Không tìm thấy policy với id: " + id));

        validateValue(policy, newValue);

        String oldValue = policy.getCurrentValue();

        policy.setCurrentValue(newValue);
        policy.setUpdatedBy(currentUserId);
        policy.setVersion(policy.getVersion() == null ? 1 : policy.getVersion() + 1);

        systemPolicyRepository.save(policy);

        PolicyHistory history = PolicyHistory.builder()
                .policy(policy)
                .oldValue(oldValue)
                .newValue(newValue)
                .changedBy(currentUserId)
                .reason(reason)
                .version(policy.getVersion())
                .changeType("UPDATE")
                .build();

        policyHistoryRepository.save(history);

        log.info("Policy {} updated from '{}' to '{}' by user {}", policy.getPolicyKey(), oldValue, newValue, currentUserId);

        return toResponse(policy);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed.toLowerCase(Locale.ROOT);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private PolicyResponse toResponse(SystemPolicy p) {
        return PolicyResponse.builder()
                .id(p.getId())
                .policyKey(p.getPolicyKey())
                .policyCategory(p.getPolicyCategory())
                .policyName(p.getPolicyName())
                .description(p.getDescription())
                .valueType(p.getValueType())
                .defaultValue(p.getDefaultValue())
                .currentValue(p.getCurrentValue())
                .minValue(p.getMinValue())
                .maxValue(p.getMaxValue())
                .unit(p.getUnit())
                .scope(p.getScope())
                .branchId(p.getBranchId())
                .courseId(p.getCourseId())
                .classId(p.getClassId())
                .active(p.isActive())
                .version(p.getVersion())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }

    private void validateValue(SystemPolicy policy, String newValue) {
        if (newValue == null) {
            throw new BusinessRuleException(ErrorCode.INVALID_INPUT.name(), "Giá trị policy không được để trống");
        }

        String valueType = policy.getValueType() != null ? policy.getValueType().toUpperCase(Locale.ROOT) : "STRING";
        String min = policy.getMinValue();
        String max = policy.getMaxValue();

        try {
            switch (valueType) {
                case "INTEGER" -> {
                    int v = Integer.parseInt(newValue);
                    if (min != null && !min.isBlank() && v < Integer.parseInt(min)) {
                        throw new BusinessRuleException(ErrorCode.INVALID_INPUT.name(), "Giá trị nhỏ hơn min cho phép: " + min);
                    }
                    if (max != null && !max.isBlank() && v > Integer.parseInt(max)) {
                        throw new BusinessRuleException(ErrorCode.INVALID_INPUT.name(), "Giá trị lớn hơn max cho phép: " + max);
                    }
                }
                case "DOUBLE" -> {
                    double v = Double.parseDouble(newValue);
                    if (min != null && !min.isBlank() && v < Double.parseDouble(min)) {
                        throw new BusinessRuleException(ErrorCode.INVALID_INPUT.name(), "Giá trị nhỏ hơn min cho phép: " + min);
                    }
                    if (max != null && !max.isBlank() && v > Double.parseDouble(max)) {
                        throw new BusinessRuleException(ErrorCode.INVALID_INPUT.name(), "Giá trị lớn hơn max cho phép: " + max);
                    }
                }
                case "BOOLEAN" -> {
                    if (!"true".equalsIgnoreCase(newValue) && !"false".equalsIgnoreCase(newValue)) {
                        throw new BusinessRuleException(ErrorCode.INVALID_INPUT.name(), "Giá trị BOOLEAN chỉ chấp nhận true/false");
                    }
                }
                case "STRING", "JSON" -> {
                    // no-op, always valid from service perspective
                }
                default -> throw new BusinessRuleException(ErrorCode.INVALID_INPUT.name(),
                        "Kiểu giá trị policy không hợp lệ: " + policy.getValueType());
            }
        } catch (NumberFormatException ex) {
            throw new BusinessRuleException(ErrorCode.INVALID_INPUT.name(),
                    "Giá trị không đúng định dạng cho kiểu " + valueType);
        }
    }
}


