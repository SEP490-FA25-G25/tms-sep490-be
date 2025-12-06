package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TeacherRepository extends JpaRepository<Teacher, Long> {

    //Find teacher by user account ID
    Optional<Teacher> findByUserAccountId(Long userAccountId);
}
