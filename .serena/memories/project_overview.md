# TMS SEP490 Backend - Project Overview

## Purpose
Training Management System (TMS) backend for managing training centers, classes, students, teachers, enrollments, and academic workflows. Supports multiple user roles with JWT-based authentication and role-based authorization.

## Tech Stack
- **Java**: 21
- **Framework**: Spring Boot 3.5.7
- **Database**: PostgreSQL 16 with Hibernate JPA
- **Authentication**: JWT (JJWT 0.12.6) with Spring Security
- **Build Tool**: Maven
- **API Documentation**: SpringDoc OpenAPI (Swagger UI)
- **Testing**: JUnit 5, Testcontainers (PostgreSQL), REST Assured, JaCoCo
- **Code Reduction**: Lombok
- **Excel Parsing**: Apache POI 5.3.0

## Domain Model (Key Entities)
- **Core**: Center, Branch, ClassEntity, Course, Subject, Level
- **Users**: UserAccount, Student, Teacher, Role, UserRole
- **Academic**: Enrollment, Session, StudentSession, Score, Assessment
- **Requests**: StudentRequest, TeacherRequest
- **Curriculum**: CourseSession, CoursePhase, CLO, PLO, CourseMaterial

## User Roles (8)
ADMIN, MANAGER, CENTER_HEAD, SUBJECT_LEADER, ACADEMIC_AFFAIR, QA, TEACHER, STUDENT

## Architecture Pattern
Layered Architecture:
```
Controllers (REST API /api/v1/*) 
    → Services (Business Logic)
        → Repositories (JPA Data Access)
            → Entities (Domain Models)
```

## Key Features
- Student/Teacher request management (transfers, makeups)
- Class scheduling and enrollment management
- Curriculum and assessment tracking
- Multi-branch center operations
- Role-based access control
