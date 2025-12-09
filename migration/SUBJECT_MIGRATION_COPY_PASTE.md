# 📋 MIGRATION BY ENDPOINT - SUBJECT MODULE

> **NGUYÊN TẮC**: 
> - Mỗi endpoint = 1 commit
> - **KHÔNG dùng interface**, viết thẳng vào Service class (giống AuthService.java)

## 🔄 MAPPING ĐỔI TÊN ENTITY

| Deprecated Backend | New Backend | Ghi chú |
|--------------------|-------------|---------|
| Subject | Curriculum | Entity gốc (IELTS, TOEIC...) |
| Level | Level | Giữ nguyên |
| Course | Subject | Phiên bản cụ thể của Level |
| Course_Phase | Subject_Phase | Giai đoạn của Subject |
| Course_Session | Subject_Session | Buổi học trong Phase |
| Course_Material | Subject_Material | Tài liệu học |
| Course_Assessment | Subject_Assessment | Đánh giá |

**Workflow mới**: `Curriculum → Level → Subject`

---

# 🚀 ENDPOINT 0: BASE SETUP (BẮT BUỘC TRƯỚC)

## A. REPOSITORY: `SubjectRepository.java`

**📖 Giải thích Annotations:**
| Annotation/Code | Ý nghĩa |
|-----------------|---------|
| `@Repository` | Đánh dấu class là Repository (tầng truy cập dữ liệu) |
| `extends JpaRepository<Subject, Long>` | Kế thừa các method CRUD sẵn có |
| `@Query` | Custom JPQL query khi cần logic phức tạp |
| `@Param` | Map parameter vào query |

```java
package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.Subject;
import org.fyp.tmssep490be.entities.enums.ApprovalStatus;
import org.fyp.tmssep490be.entities.enums.SubjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long> {
    
    // Find by Curriculum/Level
    List<Subject> findByCurriculumId(Long curriculumId);
    List<Subject> findByLevelId(Long levelId);
    List<Subject> findByCurriculumIdAndLevelId(Long curriculumId, Long levelId);
    
    // Ordered by updatedAt DESC
    List<Subject> findByCurriculumIdOrderByUpdatedAtDesc(Long curriculumId);
    List<Subject> findByLevelIdOrderByUpdatedAtDesc(Long levelId);
    List<Subject> findByCurriculumIdAndLevelIdOrderByUpdatedAtDesc(Long curriculumId, Long levelId);
    
    long countByLevelId(Long levelId);

    // Tìm môn học cần kích hoạt (for SubjectActivationJob)
    @Query("SELECT s FROM Subject s " +
            "WHERE s.effectiveDate <= :date " +
            "AND s.status = :status " +
            "AND s.approvalStatus = :approvalStatus " +
            "ORDER BY s.effectiveDate ASC")
    List<Subject> findByEffectiveDateBeforeOrEqualAndStatusAndApprovalStatus(
            @Param("date") LocalDate date,
            @Param("status") SubjectStatus status,
            @Param("approvalStatus") ApprovalStatus approvalStatus);

    // Đếm môn học có lớp trong các chi nhánh
    @Query("SELECT COUNT(DISTINCT s) FROM Subject s " +
            "INNER JOIN s.classes cl " +
            "WHERE cl.branch.id IN :branchIds")
    long countDistinctByClassesInBranches(@Param("branchIds") List<Long> branchIds);

    // Kiểm tra curriculum/level có môn học với status
    boolean existsByCurriculumIdAndStatus(Long curriculumId, SubjectStatus status);
    boolean existsByLevelIdAndStatus(Long levelId, SubjectStatus status);

    // Versioning
    @Query("SELECT MAX(s.version) FROM Subject s WHERE s.logicalSubjectCode = :logicalSubjectCode")
    Integer findMaxVersionByLogicalSubjectCode(@Param("logicalSubjectCode") String logicalSubjectCode);
    List<Subject> findByLogicalSubjectCodeOrderByVersionDesc(String logicalSubjectCode);
    boolean existsByCode(String code);
}
```

---

## B. DTOs CẦN TẠO:

**📖 Giải thích:**
| DTO | Mục đích |
|-----|----------|
| `SubjectPLODTO` | PLOs liên kết với môn học (từ Curriculum) |
| `SubjectProgressDTO` | Tiến độ học của sinh viên |
| `AssessmentProgressDTO` | Tiến độ từng bài đánh giá |
| `CLOProgressDTO` | Tiến độ đạt CLO |
| `MaterialHierarchyDTO` | Cấu trúc tài liệu phân cấp |
| `PhaseMaterialDTO` | Tài liệu theo giai đoạn |
| `SessionMaterialDTO` | Tài liệu theo buổi học |
| `StudentSubjectDTO` | Môn học đã đăng ký của sinh viên |

### B1. `SubjectPLODTO.java`

```java
package org.fyp.tmssep490be.dtos.subject;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SubjectPLODTO {
    private Long id;
    private String code;
    private String description;
    private String programName;
}
```

### B2. `SubjectProgressDTO.java`

```java
package org.fyp.tmssep490be.dtos.subject;

import lombok.*;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SubjectProgressDTO {
    private Long subjectId;
    private Long studentId;
    private Integer totalSessions;
    private Integer completedSessions;
    private Integer totalMaterials;
    private Integer accessibleMaterials;
    private Double progressPercentage;
    private Double attendanceRate;
    private List<CLOProgressDTO> cloProgress;
    private List<AssessmentProgressDTO> assessmentProgress;
    private String currentPhase;
    private String nextSession;
    private Long estimatedCompletionDate;
}
```

### B3. `AssessmentProgressDTO.java`

```java
package org.fyp.tmssep490be.dtos.subject;

import lombok.*;
import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AssessmentProgressDTO {
    private Long assessmentId;
    private String name;
    private String assessmentType;
    private BigDecimal weight;
    private BigDecimal maxScore;
    private BigDecimal achievedScore;
    private Boolean isCompleted;
    private String completedAt;
    private Double percentageScore;
}
```

### B4. `CLOProgressDTO.java`

```java
package org.fyp.tmssep490be.dtos.subject;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CLOProgressDTO {
    private Long cloId;
    private String cloCode;
    private String description;
    private Double achievementRate;
    private Boolean isAchieved;
    private Integer totalAssessments;
    private Integer completedAssessments;
    private Double averageScore;
}
```

### B5. `MaterialHierarchyDTO.java`

```java
package org.fyp.tmssep490be.dtos.subject;

import lombok.*;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class MaterialHierarchyDTO {
    private List<SubjectMaterialDTO> subjectLevel;
    private List<PhaseMaterialDTO> phases;
    private Integer totalMaterials;
    private Integer accessibleMaterials;
}
```

### B6. `PhaseMaterialDTO.java`

```java
package org.fyp.tmssep490be.dtos.subject;

import lombok.*;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PhaseMaterialDTO {
    private Long id;
    private Integer phaseNumber;
    private String name;
    private List<SubjectMaterialDTO> materials;
    private List<SessionMaterialDTO> sessions;
    private Integer totalMaterials;
}
```

### B7. `SessionMaterialDTO.java`

```java
package org.fyp.tmssep490be.dtos.subject;

import lombok.*;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SessionMaterialDTO {
    private Long id;
    private Integer sequenceNo;
    private String topic;
    private List<SubjectMaterialDTO> materials;
    private List<String> skills;
    private Integer totalMaterials;
}
```

### B8. `StudentSubjectDTO.java`

```java
package org.fyp.tmssep490be.dtos.subject;

import lombok.*;
import java.time.LocalDate;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class StudentSubjectDTO {
    private Long id;
    private String code;
    private String name;
    private String description;
    private String curriculumName;
    private String levelName;
    private String logicalSubjectCode;
    private Integer totalHours;
    private String targetAudience;
    private String teachingMethods;
    private LocalDate effectiveDate;
    private String status;
    private String approvalStatus;
    private Long classId;
    private String classCode;
    private String centerName;
    private String roomName;
    private String modality;
    private LocalDate classStartDate;
    private LocalDate classEndDate;
    private String teacherName;
    private String enrollmentStatus;
    private LocalDate enrolledAt;
    private Double progressPercentage;
    private Integer completedSessions;
    private Integer totalSessions;
    private String attendanceRate;
}
```

## ✅ Commit:
```bash
git add src/main/java/org/fyp/tmssep490be/repositories/SubjectRepository.java
git add src/main/java/org/fyp/tmssep490be/dtos/subject/
git commit -m "feat(base): add SubjectRepository and DTOs for Subject module"
```

---

# 🚀 ENDPOINT 1: GET /subjects (Lấy danh sách môn học)

## 1.1 Service: `SubjectService.java`

> **LƯU Ý**: KHÔNG có interface, viết thẳng class giống AuthService

**📖 Giải thích Annotations:**
| Annotation | Ý nghĩa |
|------------|---------|
| `@Service` | Đánh dấu class là Service (tầng business logic) |
| `@RequiredArgsConstructor` | Lombok tự tạo constructor cho các field `final` |
| `@Slf4j` | Tạo biến `log` để ghi log |
| `@Transactional(readOnly = true)` | Tối ưu cho query chỉ đọc |

```java
package org.fyp.tmssep490be.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.common.SubjectDTO;
import org.fyp.tmssep490be.dtos.subject.*;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.*;
import org.fyp.tmssep490be.exceptions.ResourceNotFoundException;
import org.fyp.tmssep490be.repositories.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SubjectService {

    private final SubjectRepository subjectRepository;
    private final CurriculumRepository curriculumRepository;
    private final LevelRepository levelRepository;
    private final UserAccountRepository userAccountRepository;
    private final NotificationService notificationService;
    // Add other repositories as needed

    public List<SubjectDTO> getAllSubjects(Long curriculumId, Long levelId) {
        log.debug("Fetching subjects with filters - curriculumId: {}, levelId: {}", curriculumId, levelId);
        
        List<Subject> subjects;
        if (curriculumId != null && levelId != null) {
            subjects = subjectRepository.findByCurriculumIdAndLevelIdOrderByUpdatedAtDesc(curriculumId, levelId);
        } else if (curriculumId != null) {
            subjects = subjectRepository.findByCurriculumIdOrderByUpdatedAtDesc(curriculumId);
        } else if (levelId != null) {
            subjects = subjectRepository.findByLevelIdOrderByUpdatedAtDesc(levelId);
        } else {
            subjects = subjectRepository.findAll();
        }
        
        return subjects.stream()
            .map(this::toSubjectDTO)
            .collect(Collectors.toList());
    }

    private SubjectDTO toSubjectDTO(Subject subject) {
        return SubjectDTO.builder()
            .id(subject.getId())
            .name(subject.getName())
            .code(subject.getCode())
            .status(subject.getStatus() != null ? subject.getStatus().name() : null)
            .build();
    }
}
```

## 1.2 Controller: `SubjectController.java`

**📖 Giải thích Annotations:**
| Annotation | Ý nghĩa |
|------------|---------|
| `@RestController` | Controller trả về JSON (không render view) |
| `@RequestMapping("/api/v1/subjects")` | Base URL cho tất cả endpoints |
| `@PreAuthorize` | Kiểm tra role trước khi vào method |
| `@Operation` | Swagger documentation cho endpoint |

```java
package org.fyp.tmssep490be.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.common.SubjectDTO;
import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.services.SubjectService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/subjects")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Subject Management", description = "Subject APIs")
@SecurityRequirement(name = "bearerAuth")
public class SubjectController {

    private final SubjectService subjectService;

    @GetMapping
    @Operation(summary = "Get all subjects", description = "Lấy danh sách môn học")
    public ResponseEntity<ResponseObject<List<SubjectDTO>>> getAllSubjects(
            @RequestParam(required = false) Long curriculumId,
            @RequestParam(required = false) Long levelId) {
        log.info("Getting all subjects - curriculumId: {}, levelId: {}", curriculumId, levelId);
        List<SubjectDTO> subjects = subjectService.getAllSubjects(curriculumId, levelId);
        return ResponseEntity.ok(ResponseObject.<List<SubjectDTO>>builder()
                .success(true)
                .message("Subjects retrieved successfully")
                .data(subjects)
                .build());
    }
}
```

## ✅ Commit:
```bash
git add .
git commit -m "feat(subject): GET /subjects - list all subjects"
```

---

# 🚀 ENDPOINT 2: GET /subjects/{id} (Chi tiết môn học - Admin view)

## 2.1 Thêm vào Service:

```java
    @PreAuthorize("hasRole('SUBJECT_LEADER') or hasRole('MANAGER') or hasRole('ADMIN')")
    public SubjectDetailDTO getSubjectDetails(Long id) {
        log.debug("Fetching subject details for id: {}", id);
        Subject subject = subjectRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy môn học với ID: " + id));
        return toSubjectDetailDTO(subject);
    }

    private SubjectDetailDTO toSubjectDetailDTO(Subject subject) {
        // Map all fields from Subject entity to SubjectDetailDTO
        // Include CLOs, Phases, Sessions, Assessments, Materials
        return SubjectDetailDTO.builder()
            .id(subject.getId())
            .code(subject.getCode())
            .name(subject.getName())
            // ... other fields
            .build();
    }
```

## 2.2 Thêm vào Controller:

```java
    @GetMapping("/{id}")
    @Operation(summary = "Get subject details (Admin/Manager view)")
    @PreAuthorize("hasRole('SUBJECT_LEADER') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<ResponseObject<SubjectDetailDTO>> getSubjectDetails(@PathVariable Long id) {
        log.info("Getting subject details for id: {}", id);
        SubjectDetailDTO subject = subjectService.getSubjectDetails(id);
        return ResponseEntity.ok(ResponseObject.<SubjectDetailDTO>builder()
                .success(true)
                .message("Subject details retrieved successfully")
                .data(subject)
                .build());
    }
```

## ✅ Commit:
```bash
git add .
git commit -m "feat(subject): GET /subjects/{id} - get subject details"
```

---

# 🚀 ENDPOINT 3: GET /subjects/{id}/detail (Student/Teacher view)

## 3.1 Thêm vào Service:

```java
    public SubjectDetailDTO getSubjectDetail(Long subjectId) {
        log.debug("Fetching subject detail for Student/Teacher view: {}", subjectId);
        Subject subject = subjectRepository.findById(subjectId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy môn học"));
        return toSubjectDetailDTO(subject);
    }
```

## 3.2 Thêm vào Controller:

```java
    @GetMapping("/{subjectId}/detail")
    @Operation(summary = "Get subject detail (Student/Teacher view)")
    @PreAuthorize("hasRole('STUDENT') or hasRole('ROLE_ACADEMIC_AFFAIR') or hasRole('TEACHER')")
    public ResponseEntity<ResponseObject<SubjectDetailDTO>> getSubjectDetail(
            @PathVariable Long subjectId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        Long currentUserId = currentUser != null ? currentUser.getId() : 1L;
        log.info("User {} requesting details for subject {}", currentUserId, subjectId);
        SubjectDetailDTO subjectDetail = subjectService.getSubjectDetail(subjectId);
        return ResponseEntity.ok(ResponseObject.<SubjectDetailDTO>builder()
                .success(true)
                .message("Subject details retrieved successfully")
                .data(subjectDetail)
                .build());
    }
```

## ✅ Commit:
```bash
git add .
git commit -m "feat(subject): GET /subjects/{id}/detail - student/teacher view"
```

---

# 🚀 ENDPOINT 4: POST /subjects (Tạo môn học mới)

## 4.1 Thêm vào Service:

**📖 Giải thích logic tạo mới:**
1. Tạo entity Subject từ basicInfo
2. Link đến Curriculum và Level
3. Set status = DRAFT
4. Lưu createdBy từ userId
5. Tạo CLOs, Phases, Sessions, Assessments, Materials

```java
    @Transactional
    public SubjectDetailDTO createSubject(CreateSubjectRequestDTO request, Long userId) {
        log.info("Creating new subject: {}", request.getBasicInfo().getName());
        
        // 1. Create Subject entity
        Subject subject = new Subject();
        SubjectBasicInfoDTO basicInfo = request.getBasicInfo();
        
        subject.setName(basicInfo.getName());
        subject.setCode(basicInfo.getCode());
        subject.setDescription(basicInfo.getDescription());
        subject.setPrerequisites(basicInfo.getPrerequisites());
        subject.setTotalHours(basicInfo.getDurationHours());
        subject.setScoreScale(basicInfo.getScoreScale());
        subject.setTargetAudience(basicInfo.getTargetAudience());
        subject.setTeachingMethods(basicInfo.getTeachingMethods());
        subject.setEffectiveDate(basicInfo.getEffectiveDate());
        subject.setNumberOfSessions(basicInfo.getNumberOfSessions());
        subject.setHoursPerSession(basicInfo.getHoursPerSession());
        subject.setThumbnailUrl(basicInfo.getThumbnailUrl());
        
        // Link to Curriculum and Level
        if (basicInfo.getCurriculumId() != null) {
            Curriculum curriculum = curriculumRepository.findById(basicInfo.getCurriculumId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khung chương trình"));
            subject.setCurriculum(curriculum);
        }
        
        if (basicInfo.getLevelId() != null) {
            Level level = levelRepository.findById(basicInfo.getLevelId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy cấp độ"));
            subject.setLevel(level);
        }
        
        // Set status
        subject.setStatus(SubjectStatus.DRAFT);
        
        // Set createdBy
        if (userId != null) {
            UserAccount creator = userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));
            subject.setCreatedBy(creator);
        }
        
        subject = subjectRepository.save(subject);
        
        // 2. Create CLOs, Phases, Sessions, Assessments, Materials...
        // (Similar logic from deprecated CourseServiceImpl.createCourse)
        
        log.info("Subject created with ID: {}", subject.getId());
        return toSubjectDetailDTO(subject);
    }
```

## 4.2 Thêm vào Controller:

```java
    @PostMapping
    @Operation(summary = "Create a new subject (Syllabus)")
    @PreAuthorize("hasRole('SUBJECT_LEADER')")
    public ResponseEntity<ResponseObject<SubjectDetailDTO>> createSubject(
            @RequestBody CreateSubjectRequestDTO request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        log.info("Creating new subject: {}", request.getBasicInfo().getName());
        Long userId = currentUser != null ? currentUser.getId() : null;
        SubjectDetailDTO createdSubject = subjectService.createSubject(request, userId);
        return ResponseEntity.ok(ResponseObject.<SubjectDetailDTO>builder()
                .success(true)
                .message("Subject created successfully")
                .data(createdSubject)
                .build());
    }
```

## ✅ Commit:
```bash
git add .
git commit -m "feat(subject): POST /subjects - create new subject"
```

---

# 🚀 ENDPOINT 5: PUT /subjects/{id} (Cập nhật môn học)

## 5.1 Thêm vào Service:

```java
    @Transactional
    public SubjectDetailDTO updateSubject(Long id, CreateSubjectRequestDTO request, Long userId) {
        log.info("Updating subject with ID: {}", id);
        
        Subject subject = subjectRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy môn học"));
        
        // Update basic info
        SubjectBasicInfoDTO basicInfo = request.getBasicInfo();
        subject.setName(basicInfo.getName());
        subject.setCode(basicInfo.getCode());
        subject.setDescription(basicInfo.getDescription());
        // ... update other fields
        
        subject.setUpdatedAt(OffsetDateTime.now());
        subject = subjectRepository.save(subject);
        
        // Update CLOs, Phases, Sessions, Assessments, Materials...
        // (Similar logic from deprecated CourseServiceImpl.updateCourse)
        
        log.info("Subject updated with ID: {}", subject.getId());
        return toSubjectDetailDTO(subject);
    }
```

## 5.2 Thêm vào Controller:

```java
    @PutMapping("/{id}")
    @Operation(summary = "Update subject")
    @PreAuthorize("hasRole('SUBJECT_LEADER')")
    public ResponseEntity<ResponseObject<SubjectDetailDTO>> updateSubject(
            @PathVariable Long id,
            @RequestBody CreateSubjectRequestDTO request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        log.info("Updating subject with ID: {}", id);
        Long userId = currentUser != null ? currentUser.getId() : null;
        SubjectDetailDTO updatedSubject = subjectService.updateSubject(id, request, userId);
        return ResponseEntity.ok(ResponseObject.<SubjectDetailDTO>builder()
                .success(true)
                .message("Subject updated successfully")
                .data(updatedSubject)
                .build());
    }
```

## ✅ Commit:
```bash
git add .
git commit -m "feat(subject): PUT /subjects/{id} - update subject"
```

---

# 🚀 ENDPOINT 6: DELETE /subjects/{id} (Xóa môn học)

## 6.1 Thêm vào Service:

**📖 Giải thích logic xóa:**
1. Tìm môn học theo ID
2. Validation: Chỉ xóa được khi DRAFT và chưa submit
3. Xóa môn học

```java
    @Transactional
    public void deleteSubject(Long id) {
        log.info("Deleting subject with ID: {}", id);
        Subject subject = subjectRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy môn học với ID: " + id));
        
        // Validation: Chỉ xóa được khi status = DRAFT và approvalStatus = null
        if (subject.getStatus() != SubjectStatus.DRAFT || subject.getApprovalStatus() != null) {
            throw new IllegalStateException(
                    "Không thể xóa môn học. Môn học phải ở trạng thái NHÁP và chưa được gửi phê duyệt.");
        }
        
        subjectRepository.delete(subject);
        log.info("Subject deleted successfully: {}", id);
    }
```

## 6.2 Thêm vào Controller:

```java
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete subject")
    @PreAuthorize("hasRole('SUBJECT_LEADER')")
    public ResponseEntity<ResponseObject<Void>> deleteSubject(@PathVariable Long id) {
        log.info("Deleting subject with ID: {}", id);
        subjectService.deleteSubject(id);
        return ResponseEntity.ok(ResponseObject.<Void>builder()
                .success(true)
                .message("Subject deleted successfully")
                .build());
    }
```

## ✅ Commit:
```bash
git add .
git commit -m "feat(subject): DELETE /subjects/{id} - delete subject"
```

---

# 🚀 ENDPOINT 7: POST /subjects/{id}/submit (Gửi phê duyệt)

## 7.1 Thêm vào Service:

**📖 Giải thích logic submit:**
1. Kiểm tra status phải là DRAFT hoặc REJECTED
2. Cập nhật status → SUBMITTED, approvalStatus → PENDING
3. Gửi thông báo cho Managers

```java
    @Transactional
    public void submitSubject(Long id) {
        log.info("Submitting subject for approval: {}", id);
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy môn học"));

        // Chỉ cho phép submit khi status là DRAFT hoặc REJECTED
        if (subject.getStatus() != SubjectStatus.DRAFT && subject.getStatus() != SubjectStatus.REJECTED) {
            throw new IllegalStateException("Chỉ có thể gửi môn học ở trạng thái NHÁP hoặc BỊ TỪ CHỐI");
        }

        // Set updatedAt khi re-submit sau rejection
        if (subject.getApprovalStatus() == ApprovalStatus.REJECTED) {
            subject.setUpdatedAt(OffsetDateTime.now());
            log.info("Subject {} is being re-submitted after rejection", id);
        }

        subject.setStatus(SubjectStatus.SUBMITTED);
        subject.setApprovalStatus(ApprovalStatus.PENDING);
        subject.setSubmittedAt(OffsetDateTime.now());
        subjectRepository.save(subject);

        // Gửi thông báo cho Managers
        sendNotificationToManagers(subject);

        log.info("Subject {} submitted successfully", id);
    }
```

## 7.2 Thêm vào Controller:

```java
    @PostMapping("/{id}/submit")
    @Operation(summary = "Submit subject for approval")
    @PreAuthorize("hasRole('SUBJECT_LEADER')")
    public ResponseEntity<ResponseObject<Void>> submitSubject(@PathVariable Long id) {
        log.info("Submitting subject with ID: {}", id);
        subjectService.submitSubject(id);
        return ResponseEntity.ok(ResponseObject.<Void>builder()
                .success(true)
                .message("Subject submitted for approval successfully")
                .build());
    }
```

## ✅ Commit:
```bash
git add .
git commit -m "feat(subject): POST /subjects/{id}/submit - submit for approval"
```

---

# 🚀 ENDPOINT 8: POST /subjects/{id}/approve (Phê duyệt)

## 8.1 Thêm vào Service:

**📖 Giải thích logic phê duyệt:**
1. Kiểm tra status phải là SUBMITTED
2. Chuyển sang PENDING_ACTIVATION (chờ đến ngày hiệu lực)
3. Gửi thông báo cho Subject Leader

```java
    @Transactional
    public void approveSubject(Long id) {
        log.info("Approving subject with ID: {}", id);
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy môn học"));

        if (subject.getStatus() != SubjectStatus.SUBMITTED) {
            throw new IllegalStateException("Chỉ có thể phê duyệt môn học ở trạng thái ĐÃ GỬI");
        }

        // Chuyển sang PENDING_ACTIVATION (chờ đến ngày hiệu lực)
        subject.setStatus(SubjectStatus.PENDING_ACTIVATION);
        subject.setApprovalStatus(ApprovalStatus.APPROVED);
        subjectRepository.save(subject);

        // Gửi thông báo cho Subject Leader
        sendApprovalNotificationToSubjectLeader(subject, true, null);

        log.info("Subject {} approved. Will be activated on: {}", id, subject.getEffectiveDate());
    }
```

## 8.2 Thêm vào Controller:

```java
    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve subject")
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<ResponseObject<Void>> approveSubject(@PathVariable Long id) {
        log.info("Approving subject with ID: {}", id);
        subjectService.approveSubject(id);
        return ResponseEntity.ok(ResponseObject.<Void>builder()
                .success(true)
                .message("Subject approved successfully")
                .build());
    }
```

## ✅ Commit:
```bash
git add .
git commit -m "feat(subject): POST /subjects/{id}/approve - approve subject"
```

---

# 🚀 ENDPOINT 9: POST /subjects/{id}/reject (Từ chối)

## 9.1 Thêm vào Service:

**📖 Giải thích logic từ chối:**
1. Kiểm tra status phải là SUBMITTED
2. Chuyển status về DRAFT, lưu lý do từ chối
3. Gửi thông báo với lý do cho Subject Leader

```java
    @Transactional
    public void rejectSubject(Long id, String reason) {
        log.info("Rejecting subject with ID: {}", id);
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy môn học"));

        if (subject.getStatus() != SubjectStatus.SUBMITTED) {
            throw new IllegalStateException("Chỉ có thể từ chối môn học ở trạng thái ĐÃ GỬI");
        }

        subject.setStatus(SubjectStatus.DRAFT);
        subject.setApprovalStatus(ApprovalStatus.REJECTED);
        subject.setRejectionReason(reason);
        subjectRepository.save(subject);

        // Gửi thông báo cho Subject Leader
        sendApprovalNotificationToSubjectLeader(subject, false, reason);

        log.info("Subject {} rejected. Reason: {}", id, reason);
    }
```

## 9.2 Thêm vào Controller:

```java
    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject subject")
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<ResponseObject<Void>> rejectSubject(
            @PathVariable Long id,
            @RequestBody(required = false) String reason) {
        log.info("Rejecting subject with ID: {}", id);
        subjectService.rejectSubject(id, reason);
        return ResponseEntity.ok(ResponseObject.<Void>builder()
                .success(true)
                .message("Subject rejected successfully")
                .build());
    }
```

## ✅ Commit:
```bash
git add .
git commit -m "feat(subject): POST /subjects/{id}/reject - reject subject"
```

---

# 🚀 ENDPOINT 10: PATCH /subjects/{id}/deactivate (Hủy kích hoạt)

## 10.1 Thêm vào Service:

**📖 Giải thích logic hủy kích hoạt:**
1. Kiểm tra status phải là ACTIVE
2. Kiểm tra không có lớp học còn session tương lai
3. Chuyển status → INACTIVE
4. Cập nhật Curriculum/Level nếu cần

```java
    @Transactional
    public void deactivateSubject(Long id) {
        log.info("Deactivating subject with ID: {}", id);
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy môn học"));

        if (subject.getStatus() != SubjectStatus.ACTIVE) {
            throw new IllegalStateException("Chỉ có thể hủy kích hoạt môn học đang HOẠT ĐỘNG");
        }

        // Kiểm tra có lớp học còn session tương lai
        boolean hasFutureSessions = subject.getClasses().stream()
                .flatMap(classEntity -> classEntity.getSessions().stream())
                .anyMatch(session -> !session.getDate().isBefore(java.time.LocalDate.now()));

        if (hasFutureSessions) {
            throw new IllegalStateException("Không thể hủy vì còn lớp học đang giảng dạy môn học này");
        }

        subject.setStatus(SubjectStatus.INACTIVE);
        subjectRepository.save(subject);

        // Chuyển Curriculum/Level về DRAFT nếu cần
        deactivateCurriculumAndLevelIfNeeded(subject);
    }
```

## 10.2 Thêm vào Controller:

```java
    @PatchMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate subject")
    @PreAuthorize("hasRole('SUBJECT_LEADER')")
    public ResponseEntity<ResponseObject<Void>> deactivateSubject(@PathVariable Long id) {
        log.info("Deactivating subject with ID: {}", id);
        subjectService.deactivateSubject(id);
        return ResponseEntity.ok(ResponseObject.<Void>builder()
                .success(true)
                .message("Subject deactivated successfully")
                .build());
    }
```

## ✅ Commit:
```bash
git add .
git commit -m "feat(subject): PATCH /subjects/{id}/deactivate"
```

---

# 🚀 ENDPOINT 11: PATCH /subjects/{id}/reactivate (Kích hoạt lại)

## 11.1 Thêm vào Service:

**📖 Giải thích logic kích hoạt lại:**
1. Kiểm tra môn học có đủ dữ liệu (CLOs, Phases)
2. Nếu đủ → ACTIVE, nếu không → DRAFT

```java
    @Transactional
    public void reactivateSubject(Long id) {
        log.info("Reactivating subject with ID: {}", id);
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy môn học"));

        // Kiểm tra môn học có đủ dữ liệu để active
        if (subject.getClos() != null && !subject.getClos().isEmpty() &&
                subject.getSubjectPhases() != null && !subject.getSubjectPhases().isEmpty()) {
            subject.setStatus(SubjectStatus.ACTIVE);
        } else {
            subject.setStatus(SubjectStatus.DRAFT);
        }
        subjectRepository.save(subject);
    }
```

## 11.2 Thêm vào Controller:

```java
    @PatchMapping("/{id}/reactivate")
    @Operation(summary = "Reactivate subject")
    @PreAuthorize("hasRole('SUBJECT_LEADER')")
    public ResponseEntity<ResponseObject<Void>> reactivateSubject(@PathVariable Long id) {
        log.info("Reactivating subject with ID: {}", id);
        subjectService.reactivateSubject(id);
        return ResponseEntity.ok(ResponseObject.<Void>builder()
                .success(true)
                .message("Subject reactivated successfully")
                .build());
    }
```

## ✅ Commit:
```bash
git add .
git commit -m "feat(subject): PATCH /subjects/{id}/reactivate"
```

---

# 🚀 ENDPOINT 12: GET /subjects/{id}/syllabus (Lấy syllabus)

## 12.1 Thêm vào Service:

```java
    public SubjectDetailDTO getSubjectSyllabus(Long subjectId) {
        log.debug("Fetching syllabus for subject: {}", subjectId);
        Subject subject = subjectRepository.findById(subjectId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy môn học"));
        return toSubjectDetailDTO(subject);
    }
```

## 12.2 Thêm vào Controller:

```java
    @GetMapping("/{subjectId}/syllabus")
    @Operation(summary = "Get subject syllabus")
    @PreAuthorize("hasRole('STUDENT') or hasRole('ROLE_ACADEMIC_AFFAIR') or hasRole('TEACHER')")
    public ResponseEntity<ResponseObject<SubjectDetailDTO>> getSubjectSyllabus(
            @PathVariable Long subjectId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        Long currentUserId = currentUser != null ? currentUser.getId() : 1L;
        log.info("User {} requesting syllabus for subject {}", currentUserId, subjectId);
        SubjectDetailDTO syllabus = subjectService.getSubjectSyllabus(subjectId);
        return ResponseEntity.ok(ResponseObject.<SubjectDetailDTO>builder()
                .success(true)
                .message("Subject syllabus retrieved successfully")
                .data(syllabus)
                .build());
    }
```

## ✅ Commit:
```bash
git add .
git commit -m "feat(subject): GET /subjects/{id}/syllabus"
```

---

# 🚀 ENDPOINT 13: GET /subjects/{id}/materials (Lấy tài liệu)

## 13.1 Thêm vào Service:

```java
    public MaterialHierarchyDTO getSubjectMaterials(Long subjectId, Long studentId) {
        log.debug("Fetching materials for subject: {}, student: {}", subjectId, studentId);
        Subject subject = subjectRepository.findById(subjectId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy môn học"));
        
        // Build material hierarchy
        return MaterialHierarchyDTO.builder()
            // ... populate from subject.getSubjectMaterials(), phases, sessions
            .build();
    }
```

## 13.2 Thêm vào Controller:

```java
    @GetMapping("/{subjectId}/materials")
    @Operation(summary = "Get subject materials hierarchy")
    @PreAuthorize("hasRole('STUDENT') or hasRole('ROLE_ACADEMIC_AFFAIR') or hasRole('TEACHER')")
    public ResponseEntity<ResponseObject<MaterialHierarchyDTO>> getSubjectMaterials(
            @PathVariable Long subjectId,
            @RequestParam(required = false) Long studentId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        Long currentUserId = currentUser != null ? currentUser.getId() : 1L;
        boolean isStudent = currentUser != null &&
                currentUser.getAuthorities().stream()
                        .anyMatch(auth -> auth.getAuthority().equals("ROLE_STUDENT"));

        if (isStudent && (studentId == null || !currentUserId.equals(studentId))) {
            return ResponseEntity.badRequest()
                    .body(ResponseObject.<MaterialHierarchyDTO>builder()
                            .success(false)
                            .message("Student ID required for material access")
                            .build());
        }

        log.info("User {} requesting materials for subject {}", currentUserId, subjectId);
        MaterialHierarchyDTO materials = subjectService.getSubjectMaterials(subjectId,
                isStudent ? studentId : null);
        return ResponseEntity.ok(ResponseObject.<MaterialHierarchyDTO>builder()
                .success(true)
                .message("Subject materials retrieved successfully")
                .data(materials)
                .build());
    }
```

## ✅ Commit:
```bash
git add .
git commit -m "feat(subject): GET /subjects/{id}/materials"
```

---

# 🚀 ENDPOINT 14: GET /subjects/{id}/plos (Lấy PLOs)

## 14.1 Thêm vào Service:

```java
    public List<SubjectPLODTO> getSubjectPLOs(Long subjectId) {
        log.debug("Fetching PLOs for subject: {}", subjectId);
        Subject subject = subjectRepository.findById(subjectId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy môn học"));
        
        if (subject.getCurriculum() == null) {
            return Collections.emptyList();
        }
        
        return subject.getCurriculum().getPlos().stream()
            .map(plo -> SubjectPLODTO.builder()
                .id(plo.getId())
                .code(plo.getCode())
                .description(plo.getDescription())
                .programName(subject.getCurriculum().getName())
                .build())
            .collect(Collectors.toList());
    }
```

## 14.2 Thêm vào Controller:

```java
    @GetMapping("/{subjectId}/plos")
    @Operation(summary = "Get subject PLOs")
    @PreAuthorize("hasAnyRole('STUDENT', 'ROLE_ACADEMIC_AFFAIR', 'TEACHER', 'SUBJECT_LEADER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ResponseObject<List<SubjectPLODTO>>> getSubjectPLOs(
            @PathVariable Long subjectId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        Long currentUserId = currentUser != null ? currentUser.getId() : 1L;
        log.info("User {} requesting PLOs for subject {}", currentUserId, subjectId);
        List<SubjectPLODTO> plos = subjectService.getSubjectPLOs(subjectId);
        return ResponseEntity.ok(ResponseObject.<List<SubjectPLODTO>>builder()
                .success(true)
                .message("Subject PLOs retrieved successfully")
                .data(plos)
                .build());
    }
```

## ✅ Commit:
```bash
git add .
git commit -m "feat(subject): GET /subjects/{id}/plos"
```

---

# 🚀 ENDPOINT 15: GET /subjects/{id}/clos (Lấy CLOs)

## 15.1 Thêm vào Service:

```java
    public List<SubjectCLODTO> getSubjectCLOs(Long subjectId) {
        log.debug("Fetching CLOs for subject: {}", subjectId);
        Subject subject = subjectRepository.findById(subjectId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy môn học"));
        
        return subject.getClos().stream()
            .map(clo -> SubjectCLODTO.builder()
                .id(clo.getId())
                .code(clo.getCode())
                .description(clo.getDescription())
                .build())
            .collect(Collectors.toList());
    }
```

## 15.2 Thêm vào Controller:

```java
    @GetMapping("/{subjectId}/clos")
    @Operation(summary = "Get subject CLOs")
    @PreAuthorize("hasRole('STUDENT') or hasRole('ROLE_ACADEMIC_AFFAIR') or hasRole('TEACHER')")
    public ResponseEntity<ResponseObject<List<SubjectCLODTO>>> getSubjectCLOs(
            @PathVariable Long subjectId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        Long currentUserId = currentUser != null ? currentUser.getId() : 1L;
        log.info("User {} requesting CLOs for subject {}", currentUserId, subjectId);
        List<SubjectCLODTO> clos = subjectService.getSubjectCLOs(subjectId);
        return ResponseEntity.ok(ResponseObject.<List<SubjectCLODTO>>builder()
                .success(true)
                .message("Subject CLOs retrieved successfully")
                .data(clos)
                .build());
    }
```

## ✅ Commit:
```bash
git add .
git commit -m "feat(subject): GET /subjects/{id}/clos"
```

---

# 🚀 ENDPOINT 16: POST /subjects/{id}/clone (Clone môn học)

## 16.1 Thêm vào Service:

**📖 Giải thích logic clone:**
1. Lấy môn học gốc theo ID
2. Tính logicalSubjectCode và next version
3. Tạo môn học mới với status = DRAFT
4. Clone CLOs, Phases, Sessions, Assessments, Materials

```java
    @Transactional
    public SubjectDTO cloneSubject(Long id, Long userId) {
        log.info("Cloning subject with ID: {} by user: {}", id, userId);

        Subject originalSubject = subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy môn học với ID: " + id));

        // Lấy curriculum và level codes
        String curriculumCode = originalSubject.getCurriculum().getCode();
        String levelCode = originalSubject.getLevel().getCode();
        int year = originalSubject.getEffectiveDate() != null
                ? originalSubject.getEffectiveDate().getYear()
                : java.time.LocalDate.now().getYear();

        // Tính logical subject code và next version
        String logicalSubjectCode = String.format("%s-%s-%d", curriculumCode, levelCode, year);
        Integer nextVersion = getNextVersionNumber(curriculumCode, levelCode, year);
        String newCode = String.format("%s-V%d", logicalSubjectCode, nextVersion);

        // Kiểm tra code đã tồn tại
        while (subjectRepository.existsByCode(newCode)) {
            nextVersion++;
            newCode = String.format("%s-V%d", logicalSubjectCode, nextVersion);
        }

        // Tạo môn học mới
        Subject newSubject = new Subject();
        newSubject.setCurriculum(originalSubject.getCurriculum());
        newSubject.setLevel(originalSubject.getLevel());
        newSubject.setLogicalSubjectCode(logicalSubjectCode);
        newSubject.setVersion(nextVersion);
        newSubject.setCode(newCode);
        newSubject.setName(originalSubject.getName() + " (V" + nextVersion + ")");
        newSubject.setDescription(originalSubject.getDescription());
        newSubject.setScoreScale(originalSubject.getScoreScale());
        newSubject.setTotalHours(originalSubject.getTotalHours());
        newSubject.setNumberOfSessions(originalSubject.getNumberOfSessions());
        newSubject.setHoursPerSession(originalSubject.getHoursPerSession());
        newSubject.setPrerequisites(originalSubject.getPrerequisites());
        newSubject.setTargetAudience(originalSubject.getTargetAudience());
        newSubject.setTeachingMethods(originalSubject.getTeachingMethods());
        newSubject.setEffectiveDate(originalSubject.getEffectiveDate());
        newSubject.setStatus(SubjectStatus.DRAFT);
        newSubject.setApprovalStatus(null);

        // Set created by
        UserAccount creator = userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));
        newSubject.setCreatedBy(creator);

        newSubject = subjectRepository.save(newSubject);
        final Subject savedNewSubject = newSubject;

        // Clone CLOs, Phases, Sessions, Assessments, Materials...
        // (See full implementation in deprecated CourseServiceImpl.cloneCourse)

        log.info("Successfully cloned subject {} to new subject {}", id, savedNewSubject.getId());

        return SubjectDTO.builder()
                .id(savedNewSubject.getId())
                .name(savedNewSubject.getName())
                .code(savedNewSubject.getCode())
                .status(savedNewSubject.getStatus().name())
                .build();
    }
```

## 16.2 Thêm vào Controller:

```java
    @PostMapping("/{id}/clone")
    @Operation(summary = "Clone subject", description = "Tạo version mới từ môn học hiện có")
    @PreAuthorize("hasRole('SUBJECT_LEADER')")
    public ResponseEntity<ResponseObject<SubjectDTO>> cloneSubject(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        log.info("User {} cloning subject with ID: {}", currentUser.getId(), id);
        SubjectDTO clonedSubject = subjectService.cloneSubject(id, currentUser.getId());
        return ResponseEntity.ok(ResponseObject.<SubjectDTO>builder()
                .success(true)
                .message("Subject cloned successfully. New version created.")
                .data(clonedSubject)
                .build());
    }
```

## ✅ Commit:
```bash
git add .
git commit -m "feat(subject): POST /subjects/{id}/clone"
```

---

# 🚀 ENDPOINT 17: GET /subjects/next-version (Lấy version tiếp theo)

## 17.1 Thêm vào Service:

```java
    public Integer getNextVersionNumber(String curriculumCode, String levelCode, Integer year) {
        String logicalSubjectCode = String.format("%s-%s-%d", curriculumCode, levelCode, year);
        Integer maxVersion = subjectRepository.findMaxVersionByLogicalSubjectCode(logicalSubjectCode);
        return (maxVersion == null) ? 1 : maxVersion + 1;
    }
```

## 17.2 Thêm vào Controller:

```java
    @GetMapping("/next-version")
    @Operation(summary = "Get next version number")
    @PreAuthorize("hasRole('SUBJECT_LEADER')")
    public ResponseEntity<ResponseObject<Integer>> getNextVersion(
            @RequestParam String curriculumCode,
            @RequestParam String levelCode,
            @RequestParam Integer year) {
        log.info("Getting next version for {}-{}-{}", curriculumCode, levelCode, year);
        Integer nextVersion = subjectService.getNextVersionNumber(curriculumCode, levelCode, year);
        return ResponseEntity.ok(ResponseObject.<Integer>builder()
                .success(true)
                .message("Next version number retrieved")
                .data(nextVersion)
                .build());
    }
```

## ✅ Commit:
```bash
git add .
git commit -m "feat(subject): GET /subjects/next-version"
```

---

# 🚀 HELPER METHODS (Thêm vào SubjectService)

## Notification Methods

**📖 Giải thích:**
- `sendNotificationToManagers`: Gửi thông báo cho tất cả Managers khi môn học được submit
- `sendApprovalNotificationToSubjectLeader`: Gửi kết quả phê duyệt cho người tạo môn học

```java
    // Gửi thông báo cho Managers khi submit
    private void sendNotificationToManagers(Subject subject) {
        try {
            List<UserAccount> managers = userAccountRepository.findUsersByRole("MANAGER");

            if (!managers.isEmpty()) {
                String title = "Môn học mới cần phê duyệt";
                String message = String.format(
                        "Môn học \"%s\" (Mã: %s) đã được gửi để phê duyệt. Khung chương trình: %s, Cấp độ: %s.",
                        subject.getName(),
                        subject.getCode(),
                        subject.getCurriculum() != null ? subject.getCurriculum().getName() : "N/A",
                        subject.getLevel() != null ? subject.getLevel().getName() : "N/A");

                List<Long> recipientIds = managers.stream()
                        .map(UserAccount::getId)
                        .collect(Collectors.toList());

                notificationService.sendBulkNotificationsWithReference(
                        recipientIds,
                        NotificationType.REQUEST_APPROVAL,
                        title,
                        message,
                        "Subject",
                        subject.getId());

                log.info("Sent notification to {} managers", managers.size());
            }
        } catch (Exception e) {
            log.error("Error sending notification: {}", e.getMessage(), e);
        }
    }

    // Gửi thông báo cho Subject Leader khi approve/reject
    private void sendApprovalNotificationToSubjectLeader(Subject subject, boolean isApproved, String rejectionReason) {
        try {
            UserAccount subjectLeader = subject.getCreatedBy();
            if (subjectLeader == null) {
                log.warn("No creator found for subject {}", subject.getId());
                return;
            }

            String title;
            String message;

            if (isApproved) {
                title = "Môn học đã được phê duyệt";
                message = String.format("Môn học \"%s\" (Mã: %s) đã được phê duyệt và sẽ kích hoạt vào ngày %s.",
                        subject.getName(), subject.getCode(),
                        subject.getEffectiveDate() != null ? subject.getEffectiveDate().toString() : "N/A");
            } else {
                title = "Môn học bị từ chối";
                message = String.format("Môn học \"%s\" (Mã: %s) đã bị từ chối. Lý do: %s",
                        subject.getName(), subject.getCode(),
                        rejectionReason != null ? rejectionReason : "Không có lý do cụ thể");
            }

            notificationService.sendBulkNotificationsWithReference(
                    List.of(subjectLeader.getId()),
                    NotificationType.REQUEST_APPROVAL,
                    title, message, "Subject", subject.getId());

            log.info("Sent {} notification to Subject Leader", isApproved ? "approval" : "rejection");
        } catch (Exception e) {
            log.error("Error sending approval notification: {}", e.getMessage(), e);
        }
    }
```

## Curriculum/Level Activation Methods

**📖 Giải thích:**
- `activateCurriculumAndLevelIfNeeded`: Tự động kích hoạt Curriculum/Level khi Subject thành ACTIVE
- `deactivateCurriculumAndLevelIfNeeded`: Chuyển Curriculum/Level về DRAFT khi không còn Subject ACTIVE

```java
    // Auto-activate Curriculum và Level khi Subject ACTIVE
    public void activateCurriculumAndLevelIfNeeded(Subject subject) {
        Curriculum curriculum = subject.getCurriculum();
        if (curriculum != null && curriculum.getStatus() == CurriculumStatus.DRAFT) {
            curriculum.setStatus(CurriculumStatus.ACTIVE);
            curriculumRepository.save(curriculum);
            log.info("Curriculum {} auto-activated", curriculum.getId());
        }

        Level level = subject.getLevel();
        if (level != null && level.getStatus() == LevelStatus.DRAFT) {
            level.setStatus(LevelStatus.ACTIVE);
            levelRepository.save(level);
            log.info("Level {} auto-activated", level.getId());
        }
    }

    // Chuyển Curriculum/Level về DRAFT khi không còn Subject ACTIVE
    private void deactivateCurriculumAndLevelIfNeeded(Subject subject) {
        Curriculum curriculum = subject.getCurriculum();
        if (curriculum != null && curriculum.getStatus() == CurriculumStatus.ACTIVE) {
            boolean hasOtherActive = subjectRepository.existsByCurriculumIdAndStatus(
                    curriculum.getId(), SubjectStatus.ACTIVE);
            if (!hasOtherActive) {
                curriculum.setStatus(CurriculumStatus.DRAFT);
                curriculumRepository.save(curriculum);
                log.info("Curriculum {} reverted to DRAFT", curriculum.getId());
            }
        }

        Level level = subject.getLevel();
        if (level != null && level.getStatus() == LevelStatus.ACTIVE) {
            boolean hasOtherActive = subjectRepository.existsByLevelIdAndStatus(
                    level.getId(), SubjectStatus.ACTIVE);
            if (!hasOtherActive) {
                level.setStatus(LevelStatus.DRAFT);
                levelRepository.save(level);
                log.info("Level {} reverted to DRAFT", level.getId());
            }
        }
    }
```

---

# 🚀 SCHEDULER: SubjectActivationJob

**📖 Giải thích:**
| Annotation | Ý nghĩa |
|------------|--------|
| `@Scheduled(cron = "...")` | Chạy theo lịch (mặc định 1:00 AM hàng ngày) |
| `@ConditionalOnProperty` | Chỉ kích hoạt nếu config enabled=true |
| `@Transactional` | Đảm bảo transaction khi cập nhật |

```java
// File: src/main/java/org/fyp/tmssep490be/services/scheduler/SubjectActivationJob.java
package org.fyp.tmssep490be.services.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.entities.Subject;
import org.fyp.tmssep490be.entities.enums.ApprovalStatus;
import org.fyp.tmssep490be.entities.enums.SubjectStatus;
import org.fyp.tmssep490be.repositories.SubjectRepository;
import org.fyp.tmssep490be.services.SubjectService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

// Kích hoạt môn học vào ngày hiệu lực (runs daily 1:00 AM)
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
    prefix = "tms.scheduler.jobs.subject-activation",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class SubjectActivationJob extends BaseScheduledJob {

    private final SubjectRepository subjectRepository;
    private final SubjectService subjectService;

    @Scheduled(cron = "${tms.scheduler.jobs.subject-activation.cron:0 0 1 * * ?}")
    @Transactional
    public void activateSubjects() {
        try {
            logJobStart("SubjectActivation");

            LocalDate today = LocalDate.now();
            logJobInfo(String.format("Checking subjects with effectiveDate <= %s", today));

            List<Subject> subjectsToActivate = subjectRepository
                .findByEffectiveDateBeforeOrEqualAndStatusAndApprovalStatus(
                    today,
                    SubjectStatus.PENDING_ACTIVATION,
                    ApprovalStatus.APPROVED
                );

            if (subjectsToActivate.isEmpty()) {
                logJobEnd("SubjectActivation", "No subjects to activate");
                return;
            }

            int activatedCount = 0;
            for (Subject subject : subjectsToActivate) {
                logJobInfo(String.format("Activating subject '%s' (ID: %d)", 
                    subject.getName(), subject.getId()));

                subject.setStatus(SubjectStatus.ACTIVE);
                subjectRepository.save(subject);
                
                // Tự động kích hoạt Curriculum và Level
                subjectService.activateCurriculumAndLevelIfNeeded(subject);
                
                activatedCount++;
            }

            logJobEnd("SubjectActivation", activatedCount);

        } catch (Exception e) {
            logJobError("SubjectActivation", e);
            throw e;
        }
    }
}
```

## Config `application.yml`:
```yaml
tms:
  scheduler:
    jobs:
      subject-activation:
        enabled: true
        cron: "0 0 1 * * ?"
```

---

# 📋 CHECKLIST MIGRATION

- [ ] ENDPOINT 0: Base setup (Repository + DTOs)
- [ ] ENDPOINT 1: GET /subjects
- [ ] ENDPOINT 2: GET /subjects/{id}
- [ ] ENDPOINT 3: GET /subjects/{id}/detail
- [ ] ENDPOINT 4: POST /subjects
- [ ] ENDPOINT 5: PUT /subjects/{id}
- [ ] ENDPOINT 6: DELETE /subjects/{id}
- [ ] ENDPOINT 7: POST /subjects/{id}/submit
- [ ] ENDPOINT 8: POST /subjects/{id}/approve
- [ ] ENDPOINT 9: POST /subjects/{id}/reject
- [ ] ENDPOINT 10: PATCH /subjects/{id}/deactivate
- [ ] ENDPOINT 11: PATCH /subjects/{id}/reactivate
- [ ] ENDPOINT 12: GET /subjects/{id}/syllabus
- [ ] ENDPOINT 13: GET /subjects/{id}/materials
- [ ] ENDPOINT 14: GET /subjects/{id}/plos
- [ ] ENDPOINT 15: GET /subjects/{id}/clos
- [ ] ENDPOINT 16: POST /subjects/{id}/clone
- [ ] ENDPOINT 17: GET /subjects/next-version
- [ ] Helper methods (Notifications, Curriculum/Level activation)
- [ ] SubjectActivationJob scheduler
- [ ] Config scheduler trong application.yml
- [ ] Kiểm tra imports
- [ ] Test qua Swagger
