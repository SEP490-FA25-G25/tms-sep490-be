package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.SystemPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SystemPolicyRepository extends JpaRepository<SystemPolicy, Long> {

    Optional<SystemPolicy> findFirstByPolicyKeyOrderByIdAsc(String policyKey);
}

