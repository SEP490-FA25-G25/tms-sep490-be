package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.TeacherSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

// Repository cho TeacherSkill entity
// Quản lý việc gán skill cho giáo viên với composite primary key (teacher_id, skill)
@Repository
public interface TeacherSkillRepository extends JpaRepository<TeacherSkill, TeacherSkill.TeacherSkillId> {

    // Tìm tất cả skills của một giáo viên
    List<TeacherSkill> findByTeacherId(Long teacherId);

    // Tìm các specialization riêng biệt của một giáo viên
    // Ví dụ: ["IELTS", "TOEIC"]
    @Query("SELECT DISTINCT ts.specialization FROM TeacherSkill ts WHERE ts.id.teacherId = :teacherId")
    List<String> findDistinctSpecializationsByTeacherId(@Param("teacherId") Long teacherId);

    // Tìm các specialization riêng biệt của nhiều giáo viên
    // Trả về danh sách cặp [teacherId, specialization]
    @Query("SELECT ts.id.teacherId, ts.specialization FROM TeacherSkill ts WHERE ts.id.teacherId IN :teacherIds")
    List<Object[]> findSpecializationsByTeacherIds(@Param("teacherIds") List<Long> teacherIds);

    // Tìm tất cả chi tiết skill (skill, specialization, level, language) của nhiều
    // giáo viên
    // Trả về danh sách tuple [teacherId, skill, specialization, level, language]
    @Query("SELECT ts.id.teacherId, ts.id.skill, ts.specialization, ts.level, ts.language FROM TeacherSkill ts WHERE ts.id.teacherId IN :teacherIds ORDER BY ts.id.teacherId, ts.specialization, ts.id.skill")
    List<Object[]> findSkillDetailsByTeacherIds(@Param("teacherIds") List<Long> teacherIds);
}
