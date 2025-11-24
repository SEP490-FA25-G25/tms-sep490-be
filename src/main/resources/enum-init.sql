-- =========================================
-- EMS-SEP490-BE: Database Initialization Script
-- =========================================
-- REFACTORED: No longer using PostgreSQL enum types
-- 
-- Best Practice: Using VARCHAR with @Enumerated(EnumType.STRING)
-- - Better Hibernate compatibility
-- - Easier enum value management (no ALTER TYPE needed)
-- - Database independence
-- - Human-readable in SQL queries
-- 
-- Database validation is handled by:
-- 1. CHECK constraints in schema.sql (database level)
-- 2. @Enumerated(EnumType.STRING) in entities (application level)
-- =========================================

-- This file is now intentionally minimal.
-- All enum validation is done via VARCHAR columns with CHECK constraints.
-- See schema.sql for table definitions.

-- Enum for Teacher Request Type
CREATE TYPE teacher_request_type_enum AS ENUM ('REPLACEMENT', 'RESCHEDULE', 'MODALITY_CHANGE');

-- Enum for Student Request Type
CREATE TYPE student_request_type_enum AS ENUM ('ABSENCE', 'MAKEUP', 'TRANSFER');

-- Enum for Resource Type
CREATE TYPE resource_type_enum AS ENUM ('ROOM', 'VIRTUAL');

-- Enum for Modality
CREATE TYPE modality_enum AS ENUM ('OFFLINE', 'ONLINE', 'HYBRID');

-- Enum for Skill
CREATE TYPE skill_enum AS ENUM ('GENERAL', 'READING', 'WRITING', 'SPEAKING', 'LISTENING', 'VOCABULARY', 'GRAMMAR', 'KANJI');

-- Enum for Teaching Role
CREATE TYPE teaching_role_enum AS ENUM ('PRIMARY', 'ASSISTANT');

-- Enum for Branch Status
CREATE TYPE branch_status_enum AS ENUM ('ACTIVE', 'INACTIVE', 'CLOSED', 'PLANNED');

-- Enum for Class Status
CREATE TYPE class_status_enum AS ENUM ('DRAFT', 'SCHEDULED', 'ONGOING', 'COMPLETED', 'CANCELLED');

-- Enum for Subject Status
CREATE TYPE subject_status_enum AS ENUM ('DRAFT', 'ACTIVE', 'INACTIVE');

-- Enum for Assessment Kind
CREATE TYPE assessment_kind_enum AS ENUM ('QUIZ', 'MIDTERM', 'FINAL', 'ASSIGNMENT', 'PROJECT', 'ORAL', 'PRACTICE', 'OTHER');

-- Enum for Teaching Slot Status
CREATE TYPE teaching_slot_status_enum AS ENUM ('SCHEDULED', 'ON_LEAVE', 'SUBSTITUTED');

-- Enum for Homework Status
CREATE TYPE homework_status_enum AS ENUM ('COMPLETED', 'INCOMPLETE', 'NO_HOMEWORK');

-- Enum for Course Status
CREATE TYPE course_status_enum AS ENUM ('DRAFT', 'ACTIVE', 'INACTIVE');

-- Enum for Approval Status
CREATE TYPE approval_status_enum AS ENUM ('PENDING', 'APPROVED', 'REJECTED');

-- Enum for Material Type
CREATE TYPE material_type_enum AS ENUM ('VIDEO', 'PDF', 'SLIDE', 'AUDIO', 'DOCUMENT', 'OTHER');

-- Enum for Mapping Status
CREATE TYPE mapping_status_enum AS ENUM ('ACTIVE', 'INACTIVE');

-- Enum for Gender
CREATE TYPE gender_enum AS ENUM ('MALE', 'FEMALE', 'OTHER');

-- Enum for User Status
CREATE TYPE user_status_enum AS ENUM ('ACTIVE', 'INACTIVE', 'SUSPENDED');