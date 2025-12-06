package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.UserAccount;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    @EntityGraph(attributePaths = { "userRoles", "userRoles.role" })
    Optional<UserAccount> findByEmail(String email);

    @EntityGraph(attributePaths = { "userRoles", "userRoles.role" })
    Optional<UserAccount> findById(Long id);

    @Override
    @EntityGraph(attributePaths = { "userRoles", "userRoles.role", "userBranches", "userBranches.branch" })
    org.springframework.data.domain.Page<UserAccount> findAll(org.springframework.data.domain.Pageable pageable);

    boolean existsByPhone(String phone);

}
