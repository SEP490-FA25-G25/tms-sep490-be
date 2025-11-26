package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.PolicyHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PolicyHistoryRepository extends JpaRepository<PolicyHistory, Long> {

    Page<PolicyHistory> findByPolicyIdOrderByChangedAtDesc(Long policyId, Pageable pageable);
}


