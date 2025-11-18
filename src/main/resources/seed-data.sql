-- =========================================
-- TMS-SEP490-BE: COMPREHENSIVE SEED DATA FOR TESTING
-- =========================================
-- Author: QA Team
-- Date: 2025-11-02
-- Purpose: High-quality, logically consistent dataset covering all business flows and edge cases
-- Reference Date: 2025-11-02 (today's date for testing)
-- =========================================
-- COVERAGE:
-- - Happy paths: Course creation → Class → Enrollment → Attendance → Requests
-- - Edge cases: Mid-course enrollment, cross-class makeup, transfers, teacher swaps
-- - Boundary conditions: Capacity limits, date ranges, status transitions
-- =========================================

-- ========== SECTION 0: CLEANUP & RESET ==========
-- Clean existing data in reverse dependency order
TRUNCATE TABLE student_feedback_response CASCADE;
TRUNCATE TABLE student_feedback CASCADE;
TRUNCATE TABLE qa_report CASCADE;
TRUNCATE TABLE score CASCADE;
TRUNCATE TABLE assessment CASCADE;
TRUNCATE TABLE course_assessment_clo_mapping CASCADE;
TRUNCATE TABLE course_assessment CASCADE;
TRUNCATE TABLE teacher_request CASCADE;
TRUNCATE TABLE student_request CASCADE;
TRUNCATE TABLE student_session CASCADE;
TRUNCATE TABLE enrollment CASCADE;
TRUNCATE TABLE teaching_slot CASCADE;
TRUNCATE TABLE teacher_availability CASCADE;
TRUNCATE TABLE session_resource CASCADE;
TRUNCATE TABLE session CASCADE;
TRUNCATE TABLE course_session_clo_mapping CASCADE;
TRUNCATE TABLE plo_clo_mapping CASCADE;
TRUNCATE TABLE clo CASCADE;
TRUNCATE TABLE course_material CASCADE;
TRUNCATE TABLE course_session CASCADE;
TRUNCATE TABLE course_phase CASCADE;
TRUNCATE TABLE "class" CASCADE;
TRUNCATE TABLE teacher_skill CASCADE;
TRUNCATE TABLE student CASCADE;
TRUNCATE TABLE teacher CASCADE;
TRUNCATE TABLE user_branches CASCADE;
TRUNCATE TABLE user_role CASCADE;
TRUNCATE TABLE resource CASCADE;
TRUNCATE TABLE time_slot_template CASCADE;
TRUNCATE TABLE course CASCADE;
TRUNCATE TABLE plo CASCADE;
TRUNCATE TABLE level CASCADE;
TRUNCATE TABLE subject CASCADE;
TRUNCATE TABLE branch CASCADE;
TRUNCATE TABLE center CASCADE;
TRUNCATE TABLE role CASCADE;
TRUNCATE TABLE user_account CASCADE;
TRUNCATE TABLE replacement_skill_assessment CASCADE;
TRUNCATE TABLE feedback_question CASCADE;

-- Reset sequences
SELECT setval('center_id_seq', 1, false);
SELECT setval('branch_id_seq', 1, false);
SELECT setval('role_id_seq', 1, false);
SELECT setval('user_account_id_seq', 1, false);
SELECT setval('teacher_id_seq', 1, false);
SELECT setval('student_id_seq', 1, false);
SELECT setval('subject_id_seq', 1, false);
SELECT setval('level_id_seq', 1, false);
SELECT setval('plo_id_seq', 1, false);
SELECT setval('course_id_seq', 1, false);
SELECT setval('course_phase_id_seq', 1, false);
SELECT setval('clo_id_seq', 1, false);
SELECT setval('course_session_id_seq', 1, false);
SELECT setval('course_assessment_id_seq', 1, false);
SELECT setval('class_id_seq', 1, false);
SELECT setval('session_id_seq', 1, false);
SELECT setval('assessment_id_seq', 1, false);
SELECT setval('score_id_seq', 1, false);
SELECT setval('student_request_id_seq', 1, false);
SELECT setval('teacher_request_id_seq', 1, false);
SELECT setval('student_feedback_id_seq', 1, false);
SELECT setval('student_feedback_response_id_seq', 1, false);
SELECT setval('qa_report_id_seq', 1, false);
SELECT setval('course_material_id_seq', 1, false);
SELECT setval('time_slot_template_id_seq', 1, false);
SELECT setval('resource_id_seq', 1, false);
SELECT setval('replacement_skill_assessment_id_seq', 1, false);
SELECT setval('feedback_question_id_seq', 1, false);

-- ========== TIER 1: INDEPENDENT TABLES ==========

-- Center
INSERT INTO center (id, code, name, description, phone, email, address, created_at, updated_at) VALUES
(1, 'TMS-EDU', 'TMS Education Group', 'Leading language education group in Vietnam', '+84-24-3999-8888', 'info@tms-edu.vn', '123 Nguyen Trai, Thanh Xuan, Ha Noi', '2024-01-01 00:00:00+07', '2024-01-01 00:00:00+07');

-- Roles
INSERT INTO role (id, code, name) VALUES
(1, 'ADMIN', 'System Administrator'),
(2, 'MANAGER', 'Manager'),
(3, 'CENTER_HEAD', 'Center Head'),
(4, 'SUBJECT_LEADER', 'Subject Leader'),
(5, 'ACADEMIC_AFFAIR', 'Academic Affair'),
(6, 'TEACHER', 'Teacher'),
(7, 'STUDENT', 'Student'),
(8, 'QA', 'Quality Assurance');

-- User Accounts
-- Password: '12345678' hashed with BCrypt (cost factor 10)
-- Hash: $2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.
INSERT INTO user_account (id, email, phone, full_name, gender, dob, address, password_hash, status, created_at, updated_at) VALUES
-- Staff & Management (11 users)
(1, 'admin@tms-edu.vn', '0912000001', 'Nguyen Van Admin',  'MALE', '1980-01-15', 'Ha Noi', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-01-01 00:00:00+07', '2024-01-01 00:00:00+07'),
(2, 'manager.global@tms-edu.vn', '0912000002', 'Le Van Manager',  'MALE', '1982-07-10', 'Ha Noi', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-01-01 00:00:00+07', '2024-01-01 00:00:00+07'),
(3, 'head.hn01@tms-edu.vn', '0912000003', 'Tran Thi Lan',  'FEMALE', '1975-03-20', 'Ha Noi', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-01-01 00:00:00+07', '2024-01-01 00:00:00+07'),
(4, 'head.hcm01@tms-edu.vn', '0912000004', 'Nguyen Thi Mai',  'FEMALE', '1978-05-22', 'TP. HCM', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-01-01 00:00:00+07', '2024-01-01 00:00:00+07'),
(5, 'leader.ielts@tms-edu.vn', '0912000005', 'Bui Van Nam',  'MALE', '1985-12-30', 'Ha Noi', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-01-01 00:00:00+07', '2024-01-01 00:00:00+07'),
(6, 'staff.huong.hn@tms-edu.vn', '0912000006', 'Pham Thi Huong',  'FEMALE', '1990-11-05', 'Ha Noi', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-01-01 00:00:00+07', '2024-01-01 00:00:00+07'),
(7, 'staff.duc.hn@tms-edu.vn', '0912000007', 'Hoang Van Duc',  'MALE', '1992-05-18', 'Ha Noi', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-01-01 00:00:00+07', '2024-01-01 00:00:00+07'),
(8, 'staff.anh.hcm@tms-edu.vn', '0912000008', 'Le Thi Anh',  'FEMALE', '1991-02-15', 'TP. HCM', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-01-01 00:00:00+07', '2024-01-01 00:00:00+07'),
(9, 'staff.tuan.hcm@tms-edu.vn', '0912000009', 'Tran Minh Tuan',  'MALE', '1993-08-20', 'TP. HCM', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-01-01 00:00:00+07', '2024-01-01 00:00:00+07'),
(10, 'qa.linh@tms-edu.vn', '0912000010', 'Vu Thi Linh',  'FEMALE', '1988-09-25', 'Ha Noi', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-01-01 00:00:00+07', '2024-01-01 00:00:00+07'),
(11, 'qa.thanh@tms-edu.vn', '0912000011', 'Dang Ngoc Thanh',  'MALE', '1989-04-10', 'TP. HCM', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-01-01 00:00:00+07', '2024-01-01 00:00:00+07'),

-- Teachers (16 teachers: 8 per branch)
(20, 'john.smith@tms-edu.vn', '0912001001', 'John Smith',  'MALE', '1985-04-12', 'Ha Noi', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-02-01 00:00:00+07', '2024-02-01 00:00:00+07'),
(21, 'emma.wilson@tms-edu.vn', '0912001002', 'Emma Wilson',  'FEMALE', '1987-08-22', 'Ha Noi', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-02-01 00:00:00+07', '2024-02-01 00:00:00+07'),
(22, 'david.lee@tms-edu.vn', '0912001003', 'David Lee',  'MALE', '1983-12-05', 'Ha Noi', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-02-01 00:00:00+07', '2024-02-01 00:00:00+07'),
(23, 'sarah.johnson@tms-edu.vn', '0912001004', 'Sarah Johnson',  'FEMALE', '1990-06-14', 'Ha Noi', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-02-01 00:00:00+07', '2024-02-01 00:00:00+07'),
(24, 'michael.brown@tms-edu.vn', '0912001005', 'Michael Brown',  'MALE', '1986-02-28', 'Ha Noi', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-02-01 00:00:00+07', '2024-02-01 00:00:00+07'),
(25, 'lisa.chen@tms-edu.vn', '0912001006', 'Lisa Chen',  'FEMALE', '1988-10-17', 'Ha Noi', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-02-01 00:00:00+07', '2024-02-01 00:00:00+07'),
(26, 'james.taylor@tms-edu.vn', '0912001007', 'James Taylor',  'MALE', '1984-03-09', 'Ha Noi', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-02-01 00:00:00+07', '2024-02-01 00:00:00+07'),
(27, 'anna.martinez@tms-edu.vn', '0912001008', 'Anna Martinez',  'FEMALE', '1989-07-21', 'Ha Noi', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-02-01 00:00:00+07', '2024-02-01 00:00:00+07'),
(28, 'chris.evans@tms-edu.vn', '0912001009', 'Chris Evans',  'MALE', '1988-01-20', 'TP. HCM', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-02-01 00:00:00+07', '2024-02-01 00:00:00+07'),
(29, 'olivia.white@tms-edu.vn', '0912001010', 'Olivia White',  'FEMALE', '1991-03-15', 'TP. HCM', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-02-01 00:00:00+07', '2024-02-01 00:00:00+07'),
(30, 'daniel.harris@tms-edu.vn', '0912001011', 'Daniel Harris',  'MALE', '1987-11-30', 'TP. HCM', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-02-01 00:00:00+07', '2024-02-01 00:00:00+07'),
(31, 'sophia.clark@tms-edu.vn', '0912001012', 'Sophia Clark',  'FEMALE', '1992-09-05', 'TP. HCM', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-02-01 00:00:00+07', '2024-02-01 00:00:00+07'),
(32, 'matthew.lewis@tms-edu.vn', '0912001013', 'Matthew Lewis',  'MALE', '1989-06-27', 'TP. HCM', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-02-01 00:00:00+07', '2024-02-01 00:00:00+07'),
(33, 'ava.robinson@tms-edu.vn', '0912001014', 'Ava Robinson',  'FEMALE', '1993-01-10', 'TP. HCM', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-02-01 00:00:00+07', '2024-02-01 00:00:00+07'),
(34, 'andrew.walker@tms-edu.vn', '0912001015', 'Andrew Walker',  'MALE', '1986-08-18', 'TP. HCM', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-02-01 00:00:00+07', '2024-02-01 00:00:00+07'),
(35, 'isabella.young@tms-edu.vn', '0912001016', 'Isabella Young',  'FEMALE', '1990-04-25', 'TP. HCM', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-02-01 00:00:00+07', '2024-02-01 00:00:00+07');

-- Students (60 students total: 30 per branch for realistic testing)
INSERT INTO user_account (id, email, phone, full_name, gender, dob, address, password_hash, status, created_at, updated_at) 
SELECT 
    100 + s.id, 
    'student.' || LPAD(s.id::text, 4, '0') || '@gmail.com', 
    '0900' || LPAD(s.id::text, 6, '0'),
    'Student ' || LPAD(s.id::text, 4, '0'),
    CASE WHEN s.id % 2 = 0 THEN  'FEMALE' ELSE  'MALE' END, 
    make_date(2000 + (s.id % 6), (s.id % 12) + 1, (s.id % 28) + 1),
    CASE WHEN s.id <= 30 THEN 'Ha Noi' ELSE 'TP. HCM' END,
    '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.',
    'ACTIVE',
    '2024-03-01 00:00:00+07',
    '2024-03-01 00:00:00+07'
FROM generate_series(1, 60) AS s(id);

-- Feedback Questions (for student feedback feature)
INSERT INTO feedback_question (id, question_text, question_type, options, display_order, created_at, updated_at) VALUES
(1, 'How satisfied are you with the overall teaching quality?', 'rating', NULL, 1, '2024-01-01 00:00:00+07', '2024-01-01 00:00:00+07'),
(2, 'How clear and well-organized were the lessons?', 'rating', NULL, 2, '2024-01-01 00:00:00+07', '2024-01-01 00:00:00+07'),
(3, 'How helpful were the course materials and resources?', 'rating', NULL, 3, '2024-01-01 00:00:00+07', '2024-01-01 00:00:00+07'),
(4, 'How effective was the class management and scheduling?', 'rating', NULL, 4, '2024-01-01 00:00:00+07', '2024-01-01 00:00:00+07'),
(5, 'Would you recommend this course to others?', 'rating', NULL, 5, '2024-01-01 00:00:00+07', '2024-01-01 00:00:00+07'),
(6, 'What did you like most about the course?', 'text', NULL, 6, '2024-01-01 00:00:00+07', '2024-01-01 00:00:00+07'),
(7, 'What areas need improvement?', 'text', NULL, 7, '2024-01-01 00:00:00+07', '2024-01-01 00:00:00+07');

-- ========== TIER 2: DEPENDENT ON TIER 1 ==========

-- Branches
INSERT INTO branch (id, center_id, code, name, address, phone, email, district, city, status, opening_date, created_at, updated_at) VALUES
(1, 1, 'HN01', 'TMS Ha Noi Branch', '456 Lang Ha, Dong Da, Ha Noi', '+84-24-3888-9999', 'hanoi01@tms-edu.vn', 'Dong Da', 'Ha Noi', 'ACTIVE', '2024-01-15', '2024-01-15 00:00:00+07', '2024-01-15 00:00:00+07'),
(2, 1, 'HCM01', 'TMS Ho Chi Minh Branch', '789 Le Loi, Quan 1, TP. HCM', '+84-28-3777-6666', 'hcm01@tms-edu.vn', 'Quan 1', 'TP. HCM', 'ACTIVE', '2024-03-01', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07');

-- Subject
INSERT INTO subject (id, code, name, description, status, created_by, created_at, updated_at) VALUES
(1, 'IELTS', 'International English Language Testing System', 'Comprehensive IELTS preparation courses', 'ACTIVE', 5, '2024-06-01 00:00:00+07', '2024-06-01 00:00:00+07');

-- Time Slot Templates
INSERT INTO time_slot_template (id, branch_id, name, start_time, end_time, created_at, updated_at) VALUES
-- Ha Noi Branch
(1, 1, 'HN Morning 1', '08:00:00', '10:00:00', '2024-01-15 00:00:00+07', '2024-01-15 00:00:00+07'),
(2, 1, 'HN Morning 2', '10:00:00', '12:00:00', '2024-01-15 00:00:00+07', '2024-01-15 00:00:00+07'),
(3, 1, 'HN Afternoon 1', '13:30:00', '15:30:00', '2024-01-15 00:00:00+07', '2024-01-15 00:00:00+07'),
(4, 1, 'HN Afternoon 2', '15:30:00', '17:30:00', '2024-01-15 00:00:00+07', '2024-01-15 00:00:00+07'),
(5, 1, 'HN Evening', '18:00:00', '20:00:00', '2024-01-15 00:00:00+07', '2024-01-15 00:00:00+07'),
-- Ho Chi Minh Branch
(6, 2, 'HCM Morning', '08:30:00', '10:30:00', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(7, 2, 'HCM Afternoon', '14:00:00', '16:00:00', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(8, 2, 'HCM Evening', '18:30:00', '20:30:00', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07');

-- Resources (Rooms & Zoom)
INSERT INTO resource (id, branch_id, resource_type, code, name, capacity, capacity_override, created_at, updated_at) VALUES
-- Ha Noi Branch - Physical Rooms
(1, 1, 'ROOM', 'HN01-R101', 'Ha Noi Room 101', 20, NULL, '2024-01-15 00:00:00+07', '2024-01-15 00:00:00+07'),
(2, 1, 'ROOM', 'HN01-R102', 'Ha Noi Room 102', 15, NULL, '2024-01-15 00:00:00+07', '2024-01-15 00:00:00+07'),
(3, 1, 'ROOM', 'HN01-R201', 'Ha Noi Room 201', 25, NULL, '2024-01-15 00:00:00+07', '2024-01-15 00:00:00+07'),
-- Ha Noi Branch - Virtual
(4, 1, 'VIRTUAL', 'HN01-Z01', 'Ha Noi Zoom 01', 100, NULL, '2024-01-15 00:00:00+07', '2024-01-15 00:00:00+07'),
-- Ho Chi Minh Branch - Physical Rooms
(5, 2, 'ROOM', 'HCM01-R101', 'HCM Room 101', 20, NULL, '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(6, 2, 'ROOM', 'HCM01-R102', 'HCM Room 102', 20, NULL, '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(7, 2, 'ROOM', 'HCM01-R201', 'HCM Room 201', 25, NULL, '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
-- Ho Chi Minh Branch - Virtual
(8, 2, 'VIRTUAL', 'HCM01-Z01', 'HCM Zoom 01', 100, NULL, '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07');

-- User Role & Branch Assignments
INSERT INTO user_role (user_id, role_id) VALUES
(1,1), (2,2), (3,3), (4,3), (5,4), (6,5), (7,5), (8,5), (9,5), (10,8), (11,8);
-- Teachers
INSERT INTO user_role (user_id, role_id) SELECT id, 6 FROM user_account WHERE id >= 20 AND id <= 35;
-- Students
INSERT INTO user_role (user_id, role_id) SELECT id, 7 FROM user_account WHERE id >= 101;

INSERT INTO user_branches (user_id, branch_id, assigned_by) VALUES
-- Staff assignments
(1,1,1), (1,2,1), (2,1,1), (2,2,1), (3,1,2), (4,2,2), (5,1,2), (6,1,2), (7,1,2), (8,2,4), (9,2,4), (10,1,2), (11,2,4);
-- Teachers - HN
INSERT INTO user_branches (user_id, branch_id, assigned_by) SELECT id, 1, 6 FROM user_account WHERE id BETWEEN 20 AND 27;
-- Teachers - HCM
INSERT INTO user_branches (user_id, branch_id, assigned_by) SELECT id, 2, 8 FROM user_account WHERE id BETWEEN 28 AND 35;
-- Students - HN
INSERT INTO user_branches (user_id, branch_id, assigned_by) SELECT id, 1, 6 FROM user_account WHERE id BETWEEN 101 AND 130;
-- Students - HCM
INSERT INTO user_branches (user_id, branch_id, assigned_by) SELECT id, 2, 8 FROM user_account WHERE id > 130;

-- Teachers & Students
INSERT INTO teacher (id, user_account_id, employee_code, hire_date, contract_type, created_at, updated_at) 
SELECT (id - 19), id, 'TCH-' || LPAD((id-19)::text, 3, '0'), '2024-02-01', CASE WHEN id % 3 = 0 THEN 'part-time' ELSE 'full-time' END, '2024-02-01 00:00:00+07', '2024-02-01 00:00:00+07'
FROM user_account WHERE id BETWEEN 20 AND 35;

INSERT INTO student (id, user_id, student_code, created_at, updated_at)
SELECT (id - 100), id, 'STD-' || LPAD((id - 100)::text, 4, '0'),
'2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'
FROM user_account WHERE id >= 101;

-- ========== TEACHER SKILLS (Complete profiles with IELTS band scores) ==========
-- Each teacher has all 5 skills: GENERAL, LISTENING, READING, WRITING, SPEAKING
-- Level represents IELTS band scores: 6.5-9.0 scale

INSERT INTO teacher_skill (teacher_id, skill, specialization, language, level) VALUES

-- ===== HANOI TEACHERS (1-8) =====

-- Teacher 1 (John Smith): GENERAL EXCELLENCE (Band 9.0 overall)
(1, 'GENERAL', 'IELTS', 'English', 9.0),
(1, 'LISTENING', 'IELTS Listening', 'English', 9.0),
(1, 'READING', 'IELTS Reading', 'English', 8.5),
(1, 'WRITING', 'IELTS Writing', 'English', 8.5),
(1, 'SPEAKING', 'IELTS Speaking', 'English', 9.0),

-- Teacher 2 (Emma Wilson): WRITING & READING SPECIALIST (Band 8.5 overall)
(2, 'GENERAL', 'IELTS', 'English', 8.0),
(2, 'LISTENING', 'IELTS Listening', 'English', 8.0),
(2, 'READING', 'IELTS Reading', 'English', 9.0),
(2, 'WRITING', 'IELTS Writing', 'English', 9.0),
(2, 'SPEAKING', 'IELTS Speaking', 'English', 7.5),

-- Teacher 3 (David Lee): SPEAKING SPECIALIST (Band 8.0 overall)
(3, 'GENERAL', 'IELTS', 'English', 8.0),
(3, 'LISTENING', 'IELTS Listening', 'English', 8.0),
(3, 'READING', 'IELTS Reading', 'English', 7.5),
(3, 'WRITING', 'IELTS Writing', 'English', 7.5),
(3, 'SPEAKING', 'IELTS Speaking', 'English', 9.0),

-- Teacher 4 (Sarah Johnson): BALANCED PROFILE (Band 8.0 overall)
(4, 'GENERAL', 'IELTS', 'English', 8.0),
(4, 'LISTENING', 'IELTS Listening', 'English', 8.0),
(4, 'READING', 'IELTS Reading', 'English', 8.0),
(4, 'WRITING', 'IELTS Writing', 'English', 8.0),
(4, 'SPEAKING', 'IELTS Speaking', 'English', 8.0),

-- Teacher 5 (Michael Brown): LISTENING & SPEAKING FOCUS (Band 7.5 overall)
(5, 'GENERAL', 'IELTS', 'English', 7.5),
(5, 'LISTENING', 'IELTS Listening', 'English', 8.5),
(5, 'READING', 'IELTS Reading', 'English', 7.0),
(5, 'WRITING', 'IELTS Writing', 'English', 7.0),
(5, 'SPEAKING', 'IELTS Speaking', 'English', 8.5),

-- Teacher 6 (Lisa Chen): READING SPECIALIST (Band 8.5 overall)
(6, 'GENERAL', 'IELTS', 'English', 8.5),
(6, 'LISTENING', 'IELTS Listening', 'English', 8.0),
(6, 'READING', 'IELTS Reading', 'English', 9.0),
(6, 'WRITING', 'IELTS Writing', 'English', 8.0),
(6, 'SPEAKING', 'IELTS Speaking', 'English', 8.5),

-- Teacher 7 (James Taylor): WRITING FOCUS (Band 7.5 overall)
(7, 'GENERAL', 'IELTS', 'English', 7.5),
(7, 'LISTENING', 'IELTS Listening', 'English', 7.5),
(7, 'READING', 'IELTS Reading', 'English', 7.5),
(7, 'WRITING', 'IELTS Writing', 'English', 8.5),
(7, 'SPEAKING', 'IELTS Speaking', 'English', 7.0),

-- Teacher 8 (Anna Martinez): ALL-ROUNDER (Band 8.0 overall)
(8, 'GENERAL', 'IELTS', 'English', 8.0),
(8, 'LISTENING', 'IELTS Listening', 'English', 8.0),
(8, 'READING', 'IELTS Reading', 'English', 8.0),
(8, 'WRITING', 'IELTS Writing', 'English', 8.0),
(8, 'SPEAKING', 'IELTS Speaking', 'English', 8.5),

-- ===== HO CHI MINH TEACHERS (9-16) =====

-- Teacher 9 (Chris Evans): SPEAKING & LISTENING EXPERT (Band 9.0 overall)
(9, 'GENERAL', 'IELTS', 'English', 9.0),
(9, 'LISTENING', 'IELTS Listening', 'English', 9.0),
(9, 'READING', 'IELTS Reading', 'English', 8.5),
(9, 'WRITING', 'IELTS Writing', 'English', 8.5),
(9, 'SPEAKING', 'IELTS Speaking', 'English', 9.0),

-- Teacher 10 (Olivia White): WRITING SPECIALIST (Band 8.5 overall)
(10, 'GENERAL', 'IELTS', 'English', 8.5),
(10, 'LISTENING', 'IELTS Listening', 'English', 8.0),
(10, 'READING', 'IELTS Reading', 'English', 8.5),
(10, 'WRITING', 'IELTS Writing', 'English', 9.0),
(10, 'SPEAKING', 'IELTS Speaking', 'English', 8.0),

-- Teacher 11 (Daniel Harris): READING FOCUS (Band 8.0 overall)
(11, 'GENERAL', 'IELTS', 'English', 8.0),
(11, 'LISTENING', 'IELTS Listening', 'English', 7.5),
(11, 'READING', 'IELTS Reading', 'English', 9.0),
(11, 'WRITING', 'IELTS Writing', 'English', 7.5),
(11, 'SPEAKING', 'IELTS Speaking', 'English', 8.0),

-- Teacher 12 (Sophia Clark): BALANCED HIGH PERFORMER (Band 8.5 overall)
(12, 'GENERAL', 'IELTS', 'English', 8.5),
(12, 'LISTENING', 'IELTS Listening', 'English', 8.5),
(12, 'READING', 'IELTS Reading', 'English', 8.5),
(12, 'WRITING', 'IELTS Writing', 'English', 8.5),
(12, 'SPEAKING', 'IELTS Speaking', 'English', 8.5),

-- Teacher 13 (Matthew Lewis): LISTENING SPECIALIST (Band 7.5 overall)
(13, 'GENERAL', 'IELTS', 'English', 7.5),
(13, 'LISTENING', 'IELTS Listening', 'English', 9.0),
(13, 'READING', 'IELTS Reading', 'English', 7.0),
(13, 'WRITING', 'IELTS Writing', 'English', 7.0),
(13, 'SPEAKING', 'IELTS Speaking', 'English', 7.5),

-- Teacher 14 (Ava Robinson): SPEAKING EXPERT (Band 8.5 overall)
(14, 'GENERAL', 'IELTS', 'English', 8.5),
(14, 'LISTENING', 'IELTS Listening', 'English', 8.5),
(14, 'READING', 'IELTS Reading', 'English', 8.0),
(14, 'WRITING', 'IELTS Writing', 'English', 8.0),
(14, 'SPEAKING', 'IELTS Speaking', 'English', 9.0),

-- Teacher 15 (Andrew Walker): WRITING & READING (Band 7.5 overall)
(15, 'GENERAL', 'IELTS', 'English', 7.5),
(15, 'LISTENING', 'IELTS Listening', 'English', 7.0),
(15, 'READING', 'IELTS Reading', 'English', 8.0),
(15, 'WRITING', 'IELTS Writing', 'English', 8.5),
(15, 'SPEAKING', 'IELTS Speaking', 'English', 7.0),

-- Teacher 16 (Isabella Young): ALL-ROUNDER (Band 8.0 overall)
(16, 'GENERAL', 'IELTS', 'English', 8.0),
(16, 'LISTENING', 'IELTS Listening', 'English', 8.0),
(16, 'READING', 'IELTS Reading', 'English', 8.5),
(16, 'WRITING', 'IELTS Writing', 'English', 8.0),
(16, 'SPEAKING', 'IELTS Speaking', 'English', 7.5);

-- ========== TEACHER AVAILABILITY (Organized by Test Scenarios) ==========
-- Class Schedule Reference for Testing:
-- - Class 6 (HN): Mon/Wed/Fri (T2/T4/T6) - Morning 1 (08:00-10:00) - time_slot_id = 1
-- - Class 5 (HCM): Mon/Wed/Fri (T2/T4/T6) - Morning (08:30-10:30) - time_slot_id = 6
-- Day mapping: 1=Monday, 2=Tuesday, 3=Wednesday, 4=Thursday, 5=Friday, 6=Saturday, 0=Sunday

INSERT INTO teacher_availability (teacher_id, time_slot_template_id, day_of_week, effective_date, created_at, updated_at) VALUES

-- ===== HANOI TEACHERS (1-8) =====

-- Teacher 1 (John Smith): PERFECT MATCH for Class 6
-- Available: Mon/Wed/Fri Morning 1 → Matches Class 6 exactly
(1, 1, 1, '2024-02-01', '2024-02-01 00:00:00+07', '2024-02-01 00:00:00+07'), -- Monday Morning 1
(1, 1, 3, '2024-02-01', '2024-02-01 00:00:00+07', '2024-02-01 00:00:00+07'), -- Wednesday Morning 1
(1, 1, 5, '2024-02-01', '2024-02-01 00:00:00+07', '2024-02-01 00:00:00+07'), -- Friday Morning 1

-- Teacher 2 (Emma Wilson): SCHEDULE MISMATCH - Different days, different time
-- Available: Tue/Thu/Sat Afternoon 1 → Days don't match Class 6
(2, 3, 2, '2024-02-01', '2024-02-01 00:00:00+07', '2024-02-01 00:00:00+07'), -- Tuesday Afternoon 1
(2, 3, 4, '2024-02-01', '2024-02-01 00:00:00+07', '2024-02-01 00:00:00+07'), -- Thursday Afternoon 1
(2, 3, 6, '2024-02-01', '2024-02-01 00:00:00+07', '2024-02-01 00:00:00+07'), -- Saturday Afternoon 1

-- Teacher 3 (David Lee): NO AVAILABILITY REGISTERED
-- Use case: Test "Chưa đăng ký lịch làm việc" message

-- Teacher 4 (Sarah Johnson): PARTIAL MATCH - Same days, different time
-- Available: Mon/Wed/Fri Morning 2 → Days match but time doesn't
(4, 2, 1, '2024-02-01', '2024-02-01 00:00:00+07', '2024-02-01 00:00:00+07'), -- Monday Morning 2
(4, 2, 3, '2024-02-01', '2024-02-01 00:00:00+07', '2024-02-01 00:00:00+07'), -- Wednesday Morning 2
(4, 2, 5, '2024-02-01', '2024-02-01 00:00:00+07', '2024-02-01 00:00:00+07'), -- Friday Morning 2

-- Teacher 5 (Michael Brown): PARTIAL MATCH - Only 2 of 3 days available
-- Available: Mon/Wed Morning 1 → Missing Friday
(5, 1, 1, '2024-02-01', '2024-02-01 00:00:00+07', '2024-02-01 00:00:00+07'), -- Monday Morning 1
(5, 1, 3, '2024-02-01', '2024-02-01 00:00:00+07', '2024-02-01 00:00:00+07'), -- Wednesday Morning 1

-- Teacher 6 (Lisa Chen): PERFECT MATCH for Class 6
-- Available: Mon/Wed/Fri Morning 1
(6, 1, 1, '2024-02-01', '2024-02-01 00:00:00+07', '2024-02-01 00:00:00+07'), -- Monday Morning 1
(6, 1, 3, '2024-02-01', '2024-02-01 00:00:00+07', '2024-02-01 00:00:00+07'), -- Wednesday Morning 1
(6, 1, 5, '2024-02-01', '2024-02-01 00:00:00+07', '2024-02-01 00:00:00+07'), -- Friday Morning 1

-- Teacher 7 (James Taylor): SCHEDULE MISMATCH - Same time, different days
-- Available: Tue/Thu Evening → Neither days nor time match
(7, 5, 2, '2024-02-01', '2024-02-01 00:00:00+07', '2024-02-01 00:00:00+07'), -- Tuesday Evening
(7, 5, 4, '2024-02-01', '2024-02-01 00:00:00+07', '2024-02-01 00:00:00+07'), -- Thursday Evening

-- Teacher 8 (Anna Martinez): OVER-AVAILABLE - Available more days than needed
-- Available: Mon/Tue/Wed/Thu/Fri Morning 1 → Includes all Class 6 days + extras
(8, 1, 1, '2024-02-01', '2024-02-01 00:00:00+07', '2024-02-01 00:00:00+07'), -- Monday Morning 1
(8, 1, 2, '2024-02-01', '2024-02-01 00:00:00+07', '2024-02-01 00:00:00+07'), -- Tuesday Morning 1
(8, 1, 3, '2024-02-01', '2024-02-01 00:00:00+07', '2024-02-01 00:00:00+07'), -- Wednesday Morning 1
(8, 1, 4, '2024-02-01', '2024-02-01 00:00:00+07', '2024-02-01 00:00:00+07'), -- Thursday Morning 1
(8, 1, 5, '2024-02-01', '2024-02-01 00:00:00+07', '2024-02-01 00:00:00+07'), -- Friday Morning 1

-- ===== HO CHI MINH TEACHERS (9-16) =====

-- Teacher 9 (Chris Evans): PERFECT MATCH for Class 5
-- Available: Mon/Wed/Fri HCM Morning → Matches Class 5 exactly
(9, 6, 1, '2024-02-01', '2024-02-01 00:00:00+07', '2024-02-01 00:00:00+07'), -- Monday HCM Morning
(9, 6, 3, '2024-02-01', '2024-02-01 00:00:00+07', '2024-02-01 00:00:00+07'), -- Wednesday HCM Morning
(9, 6, 5, '2024-02-01', '2024-02-01 00:00:00+07', '2024-02-01 00:00:00+07'), -- Friday HCM Morning

-- Teacher 10 (Olivia White): SCHEDULE MISMATCH - Different time slot
-- Available: Mon/Wed/Fri HCM Afternoon → Days match but time doesn't
(10, 7, 1, '2024-02-01', '2024-02-01 00:00:00+07', '2024-02-01 00:00:00+07'), -- Monday HCM Afternoon
(10, 7, 3, '2024-02-01', '2024-02-01 00:00:00+07', '2024-02-01 00:00:00+07'), -- Wednesday HCM Afternoon
(10, 7, 5, '2024-02-01', '2024-02-01 00:00:00+07', '2024-02-01 00:00:00+07'), -- Friday HCM Afternoon

-- Teacher 11 (Daniel Harris): NO AVAILABILITY REGISTERED
-- Use case: Test "Chưa đăng ký lịch làm việc" message

-- Teacher 12 (Sophia Clark): PARTIAL MATCH - Only 2 of 3 days
-- Available: Mon/Fri HCM Morning → Missing Wednesday
(12, 6, 1, '2024-02-01', '2024-02-01 00:00:00+07', '2024-02-01 00:00:00+07'), -- Monday HCM Morning
(12, 6, 5, '2024-02-01', '2024-02-01 00:00:00+07', '2024-02-01 00:00:00+07'), -- Friday HCM Morning

-- Teacher 13 (Matthew Lewis): SCHEDULE MISMATCH - Different days
-- Available: Tue/Thu/Sat HCM Morning → Days don't match
(13, 6, 2, '2024-02-01', '2024-02-01 00:00:00+07', '2024-02-01 00:00:00+07'), -- Tuesday HCM Morning
(13, 6, 4, '2024-02-01', '2024-02-01 00:00:00+07', '2024-02-01 00:00:00+07'), -- Thursday HCM Morning
(13, 6, 6, '2024-02-01', '2024-02-01 00:00:00+07', '2024-02-01 00:00:00+07'), -- Saturday HCM Morning

-- Teacher 14 (Ava Robinson): PERFECT MATCH for Class 5
-- Available: Mon/Wed/Fri HCM Morning
(14, 6, 1, '2024-02-01', '2024-02-01 00:00:00+07', '2024-02-01 00:00:00+07'), -- Monday HCM Morning
(14, 6, 3, '2024-02-01', '2024-02-01 00:00:00+07', '2024-02-01 00:00:00+07'), -- Wednesday HCM Morning
(14, 6, 5, '2024-02-01', '2024-02-01 00:00:00+07', '2024-02-01 00:00:00+07'), -- Friday HCM Morning

-- Teacher 15 (Andrew Walker): SCHEDULE MISMATCH - Different time
-- Available: Mon/Wed/Fri HCM Evening → Days match but time doesn't
(15, 8, 1, '2024-02-01', '2024-02-01 00:00:00+07', '2024-02-01 00:00:00+07'), -- Monday HCM Evening
(15, 8, 3, '2024-02-01', '2024-02-01 00:00:00+07', '2024-02-01 00:00:00+07'), -- Wednesday HCM Evening
(15, 8, 5, '2024-02-01', '2024-02-01 00:00:00+07', '2024-02-01 00:00:00+07'), -- Friday HCM Evening

-- Teacher 16 (Isabella Young): OVER-AVAILABLE - Available all weekdays
-- Available: Mon-Fri HCM Morning → Includes all Class 5 days + extras
(16, 6, 1, '2024-02-01', '2024-02-01 00:00:00+07', '2024-02-01 00:00:00+07'), -- Monday HCM Morning
(16, 6, 2, '2024-02-01', '2024-02-01 00:00:00+07', '2024-02-01 00:00:00+07'), -- Tuesday HCM Morning
(16, 6, 3, '2024-02-01', '2024-02-01 00:00:00+07', '2024-02-01 00:00:00+07'), -- Wednesday HCM Morning
(16, 6, 4, '2024-02-01', '2024-02-01 00:00:00+07', '2024-02-01 00:00:00+07'), -- Thursday HCM Morning
(16, 6, 5, '2024-02-01', '2024-02-01 00:00:00+07', '2024-02-01 00:00:00+07'); -- Friday HCM Morning

-- ========== TIER 3: CURRICULUM (Complete Definition) ==========

-- Levels for IELTS
INSERT INTO level (id, subject_id, code, name, expected_duration_hours, sort_order, created_at, updated_at) VALUES
(1, 1, 'FOUNDATION', 'IELTS Foundation (3.0-4.0)', 60, 1, '2024-06-01 00:00:00+07', '2024-06-01 00:00:00+07'),
(2, 1, 'INTERMEDIATE', 'IELTS Intermediate (5.0-6.0)', 75, 2, '2024-06-01 00:00:00+07', '2024-06-01 00:00:00+07'),
(3, 1, 'ADVANCED', 'IELTS Advanced (6.5-8.0)', 90, 3, '2024-06-01 00:00:00+07', '2024-06-01 00:00:00+07');

-- Replacement Skill Assessments for initial students
-- Simulates placement test results before their first enrollment.
-- IELTS Placement Tests
INSERT INTO replacement_skill_assessment (student_id, skill, level_id, raw_score, scaled_score, score_scale, assessment_category, assessment_date, assessment_type, assessed_by, note, created_at, updated_at)
SELECT
    s.id,
    'GENERAL',
    1, -- Corresponds to 'IELTS Foundation (3.0-4.0)'
    30 + floor(random() * 16), -- Raw score (30-45 out of 60)
    3.0 + (floor(random() * 16) / 10.0), -- Scaled score (3.0-4.5 IELTS band)
    '0-9',
    'PLACEMENT',
    '2025-06-15',
    'placement_test',
    6, -- Assessed by 'staff.huong.hn@tms-edu.vn'
    'Initial IELTS placement test score.',
    '2025-06-15 00:00:00+07',
    '2025-06-15 00:00:00+07'
FROM generate_series(1, 30) AS s(id);

-- JLPT Sample Assessments (for students 1-5 as examples)
-- Student 1: JLPT N5 Placement Test
INSERT INTO replacement_skill_assessment (student_id, skill, level_id, raw_score, scaled_score, score_scale, assessment_category, assessment_date, assessment_type, assessed_by, note, created_at, updated_at) VALUES
(1, 'VOCABULARY', 10, 45, 75.0, '0-100', 'PLACEMENT', '2025-06-15', 'jlpt_n5_placement', 6, 'JLPT N5 Vocabulary Assessment', '2025-06-15 00:00:00+07', '2025-06-15 00:00:00+07'),
(1, 'GRAMMAR', 10, 38, 63.3, '0-100', 'PLACEMENT', '2025-06-15', 'jlpt_n5_placement', 6, 'JLPT N5 Grammar Assessment', '2025-06-15 00:00:00+07', '2025-06-15 00:00:00+07'),
(1, 'KANJI', 10, 50, 83.3, '0-100', 'PLACEMENT', '2025-06-15', 'jlpt_n5_placement', 6, 'JLPT N5 Kanji Assessment', '2025-06-15 00:00:00+07', '2025-06-15 00:00:00+07'),
(1, 'READING', 10, 42, 70.0, '0-100', 'PLACEMENT', '2025-06-15', 'jlpt_n5_placement', 6, 'JLPT N5 Reading Assessment', '2025-06-15 00:00:00+07', '2025-06-15 00:00:00+07'),
(1, 'LISTENING', 10, 48, 80.0, '0-100', 'PLACEMENT', '2025-06-15', 'jlpt_n5_placement', 6, 'JLPT N5 Listening Assessment', '2025-06-15 00:00:00+07', '2025-06-15 00:00:00+07');

-- Student 2: TOEIC Mock Test
INSERT INTO replacement_skill_assessment (student_id, skill, level_id, raw_score, scaled_score, score_scale, assessment_category, assessment_date, assessment_type, assessed_by, note, created_at, updated_at) VALUES
(2, 'LISTENING', 5, 75, 375, '0-495', 'MOCK', '2025-06-16', 'toeic_mock', 6, 'TOEIC Listening Mock Test', '2025-06-16 00:00:00+07', '2025-06-16 00:00:00+07'),
(2, 'READING', 5, 72, 360, '0-495', 'MOCK', '2025-06-16', 'toeic_mock', 6, 'TOEIC Reading Mock Test', '2025-06-16 00:00:00+07', '2025-06-16 00:00:00+07');

-- PLOs for IELTS Subject
INSERT INTO plo (id, subject_id, code, description, created_at, updated_at) VALUES
(1, 1, 'PLO1', 'Demonstrate basic English communication skills in everyday contexts', '2024-06-01 00:00:00+07', '2024-06-01 00:00:00+07'),
(2, 1, 'PLO2', 'Comprehend and produce simple English texts for common situations', '2024-06-01 00:00:00+07', '2024-06-01 00:00:00+07'),
(3, 1, 'PLO3', 'Apply intermediate English grammar and vocabulary in professional contexts', '2024-06-01 00:00:00+07', '2024-06-01 00:00:00+07'),
(4, 1, 'PLO4', 'Analyze and evaluate complex English texts across various topics', '2024-06-01 00:00:00+07', '2024-06-01 00:00:00+07'),
(5, 1, 'PLO5', 'Produce coherent, well-structured academic essays and reports', '2024-06-01 00:00:00+07', '2024-06-01 00:00:00+07');

-- Course: IELTS Foundation
INSERT INTO course (id, subject_id, level_id, logical_course_code, version, code, name, description, total_hours, duration_weeks, session_per_week, hours_per_session, status, approval_status, decided_by_manager, decided_at, created_by, created_at, updated_at) VALUES
(1, 1, 1, 'IELTS-FOUND-2025', 1, 'IELTS-FOUND-2025-V1', 'IELTS Foundation 2025', 'Foundation course for IELTS beginners targeting band 3.0-4.0', 60, 8, 3, 2.5, 'ACTIVE', 'APPROVED', 2, '2024-08-20 14:00:00+07', 5, '2024-08-15 00:00:00+07', '2024-08-20 14:00:00+07');

-- Course Phases for Foundation
INSERT INTO course_phase (id, course_id, phase_number, name, duration_weeks, created_at, updated_at) VALUES
(1, 1, 1, 'Foundation Basics', 4, '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(2, 1, 2, 'Foundation Practice', 4, '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07');

-- Course Sessions for Foundation (24 sessions = 8 weeks × 3 sessions/week)
INSERT INTO course_session (id, phase_id, sequence_no, topic, student_task, skill_set, created_at, updated_at) VALUES
-- Phase 1: Foundation Basics (Sessions 1-12)
(1, 1, 1, 'Introduction to IELTS & Basic Listening', 'Listen to simple dialogues', ARRAY['GENERAL','LISTENING'], '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(2, 1, 2, 'Basic Speaking: Greetings and Introductions', 'Practice self-introduction', ARRAY['SPEAKING'], '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(3, 1, 3, 'Basic Reading: Short Passages', 'Read and answer simple questions', ARRAY['READING'], '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(4, 1, 4, 'Basic Writing: Simple Sentences', 'Write about yourself', ARRAY['WRITING'], '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(5, 1, 5, 'Listening: Numbers and Dates', 'Complete listening exercises', ARRAY['LISTENING'], '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(6, 1, 6, 'Speaking: Daily Activities', 'Describe your daily routine', ARRAY['SPEAKING'], '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(7, 1, 7, 'Reading: Understanding Main Ideas', 'Identify main ideas', ARRAY['READING'], '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(8, 1, 8, 'Writing: Simple Paragraphs', 'Write a short paragraph', ARRAY['WRITING'], '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(9, 1, 9, 'Listening: Conversations', 'Listen to basic conversations', ARRAY['LISTENING'], '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(10, 1, 10, 'Speaking: Expressing Likes and Dislikes', 'Talk about preferences', ARRAY['SPEAKING'], '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(11, 1, 11, 'Reading: Details and Facts', 'Find specific information', ARRAY['READING'], '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(12, 1, 12, 'Writing: Connecting Ideas', 'Use simple connectors', ARRAY['WRITING'], '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
-- Phase 2: Foundation Practice (Sessions 13-24)
(13, 2, 1, 'Listening: Following Instructions', 'Complete tasks from audio', ARRAY['LISTENING'], '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(14, 2, 2, 'Speaking: Asking Questions', 'Practice question forms', ARRAY['SPEAKING'], '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(15, 2, 3, 'Reading: Short Stories', 'Read and summarize', ARRAY['READING'], '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(16, 2, 4, 'Writing: Describing People and Places', 'Write descriptions', ARRAY['WRITING'], '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(17, 2, 5, 'Listening: News and Announcements', 'Understand main points', ARRAY['LISTENING'], '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(18, 2, 6, 'Speaking: Giving Opinions', 'Express simple opinions', ARRAY['SPEAKING'], '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(19, 2, 7, 'Reading: Understanding Context', 'Use context clues', ARRAY['READING'], '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(20, 2, 8, 'Writing: Personal Letters', 'Write informal letters', ARRAY['WRITING'], '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(21, 2, 9, 'Practice Test: Listening & Reading', 'Complete practice test', ARRAY['LISTENING','READING'], '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(22, 2, 10, 'Practice Test: Writing & Speaking', 'Complete practice test', ARRAY['WRITING','SPEAKING'], '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(23, 2, 11, 'Review and Feedback', 'Review all skills', ARRAY['GENERAL'], '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(24, 2, 12, 'Final Assessment', 'Complete final test', ARRAY['GENERAL','READING','WRITING','SPEAKING','LISTENING'], '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07');

-- CLOs for Foundation Course
INSERT INTO clo (id, course_id, code, description, created_at, updated_at) VALUES
(1, 1, 'CLO1', 'Understand basic English in familiar everyday situations', '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(2, 1, 'CLO2', 'Communicate simple information about personal topics', '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(3, 1, 'CLO3', 'Read and understand simple texts about familiar topics', '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(4, 1, 'CLO4', 'Write simple sentences and short paragraphs about personal experiences', '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07');

-- PLO-CLO Mappings
INSERT INTO plo_clo_mapping (plo_id, clo_id, status) VALUES
(1, 1, 'ACTIVE'),
(1, 2, 'ACTIVE'),
(2, 3, 'ACTIVE'),
(2, 4, 'ACTIVE');

-- Course Session-CLO Mappings (Sample - map each CLO to relevant sessions)
INSERT INTO course_session_clo_mapping (course_session_id, clo_id, status) VALUES
-- CLO1 (Understand basic English) - Listening sessions
(1, 1, 'ACTIVE'), (5, 1, 'ACTIVE'), (9, 1, 'ACTIVE'), (13, 1, 'ACTIVE'), (17, 1, 'ACTIVE'),
-- CLO2 (Communicate simple info) - Speaking sessions
(2, 2, 'ACTIVE'), (6, 2, 'ACTIVE'), (10, 2, 'ACTIVE'), (14, 2, 'ACTIVE'), (18, 2, 'ACTIVE'),
-- CLO3 (Read simple texts) - Reading sessions
(3, 3, 'ACTIVE'), (7, 3, 'ACTIVE'), (11, 3, 'ACTIVE'), (15, 3, 'ACTIVE'), (19, 3, 'ACTIVE'),
-- CLO4 (Write simple paragraphs) - Writing sessions
(4, 4, 'ACTIVE'), (8, 4, 'ACTIVE'), (12, 4, 'ACTIVE'), (16, 4, 'ACTIVE'), (20, 4, 'ACTIVE');

-- Course Materials for Foundation Course
INSERT INTO course_material (course_id, phase_id, course_session_id, title, description, material_type, url, uploaded_by) VALUES
-- Course-level materials
(1, NULL, NULL, 'IELTS Foundation - Course Syllabus', 'The complete syllabus for the course.', 'PDF', '/materials/courses/1/syllabus.pdf', 5),
(1, NULL, NULL, 'Introductory Video', 'A welcome video from the head teacher.', 'VIDEO', '/materials/courses/1/intro.mp4', 5),
-- Phase 1 materials
(1, 1, NULL, 'Phase 1 Vocabulary List', 'Key vocabulary for the first 4 weeks.', 'DOCUMENT', '/materials/phases/1/vocab.docx', 5),
-- Session-specific materials
(1, 1, 1, 'Session 1 - Listening Slides', 'Presentation slides for the first session.', 'SLIDE', '/materials/sessions/1/slides.pptx', 5),
(1, 1, 2, 'Session 2 - Speaking Practice Audio', 'Audio files for speaking practice.', 'AUDIO', '/materials/sessions/2/practice.mp3', 5),
(1, 1, 3, 'Session 3 - Reading Passage PDF', 'Reading text for session 3.', 'PDF', '/materials/sessions/3/reading.pdf', 5),
(1, 1, 4, 'Session 4 - Writing Task 1 Sample', 'A sample for the first writing task.', 'DOCUMENT', '/materials/sessions/4/sample.docx', 5),
-- Phase 2 materials
(1, 2, NULL, 'Phase 2 Grammar Guide', 'Advanced grammar rules for the last 4 weeks.', 'PDF', '/materials/phases/2/grammar.pdf', 5),
-- More session-specific materials
(1, 2, 13, 'Session 13 - Listening Practice Test', 'A full practice test for listening.', 'AUDIO', '/materials/sessions/13/practice-test.mp3', 5),
(1, 2, 21, 'Session 21 - Full Practice Test', 'A complete mock test (all sections).', 'PDF', '/materials/sessions/21/mock-test.pdf', 5);

-- Course Assessments for Foundation
INSERT INTO course_assessment (id, course_id, name, kind, duration_minutes, max_score, skills, created_at, updated_at) VALUES
(1, 1, 'Listening Quiz 1', 'QUIZ', 30, 20, ARRAY['LISTENING'], '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(2, 1, 'Speaking Quiz 1', 'QUIZ', 15, 20, ARRAY['SPEAKING'], '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(3, 1, 'Reading Quiz 1', 'QUIZ', 30, 20, ARRAY['READING'], '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(4, 1, 'Writing Assignment 1', 'ASSIGNMENT', 60, 20, ARRAY['WRITING'], '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(5, 1, 'Midterm Exam', 'MIDTERM', 90, 100, ARRAY['LISTENING','READING','WRITING','SPEAKING'], '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(6, 1, 'Final Exam', 'FINAL', 120, 100, ARRAY['LISTENING','READING','WRITING','SPEAKING'], '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07');

-- Course Assessment-CLO Mappings
INSERT INTO course_assessment_clo_mapping (course_assessment_id, clo_id, status) VALUES
(1, 1, 'ACTIVE'),
(2, 2, 'ACTIVE'),
(3, 3, 'ACTIVE'),
(4, 4, 'ACTIVE'),
(5, 1, 'ACTIVE'), (5, 2, 'ACTIVE'), (5, 3, 'ACTIVE'), (5, 4, 'ACTIVE'),
(6, 1, 'ACTIVE'), (6, 2, 'ACTIVE'), (6, 3, 'ACTIVE'), (6, 4, 'ACTIVE');

-- ========== TIER 4: CLASSES & SESSIONS ==========

-- Classes (Test scenarios: completed, ongoing, scheduled)
INSERT INTO "class" (id, branch_id, course_id, code, name, modality, start_date, planned_end_date, actual_end_date, schedule_days, max_capacity, status, approval_status, created_by, decided_by, submitted_at, decided_at, created_at, updated_at) VALUES
-- HN Branch - Class 1: COMPLETED (to test historical data)
-- Pattern: IELTSFOUND-HN01-25-001 (Course: IELTS-FOUND, Branch: HN01, Year: 25, Seq: 001)
(1, 1, 1, 'IELTSFOUND-HN01-25-001', 'HN Foundation 1 (Completed)', 'OFFLINE', '2025-07-07', '2025-09-01', '2025-09-01', ARRAY[1,3,5]::smallint[], 20, 'COMPLETED', 'APPROVED', 6, 3, '2025-07-01 10:00:00+07', '2025-07-02 14:00:00+07', '2025-07-01 10:00:00+07', '2025-09-01 18:00:00+07'),

-- HN Branch - Class 2: ONGOING (main testing class - today is 2025-11-02, started Oct 6)
-- Pattern: IELTSFOUND-HN01-25-002 (Started after Class 1, so sequence 002)
(2, 1, 1, 'IELTSFOUND-HN01-25-002', 'HN Foundation 1 (Ongoing)', 'OFFLINE', '2025-10-06', '2025-11-28', NULL, ARRAY[1,3,5]::smallint[], 20, 'ONGOING', 'APPROVED', 6, 3, '2025-09-30 10:00:00+07', '2025-10-01 14:00:00+07', '2025-09-30 10:00:00+07', '2025-10-06 08:00:00+07'),

-- HN Branch - Class 3: ONGOING (for transfer/makeup scenarios)
-- Pattern: IELTSFOUND-HN01-25-003 (Started after Class 2, so sequence 003)
(3, 1, 1, 'IELTSFOUND-HN01-25-003', 'HN Foundation 2 (Ongoing)', 'ONLINE', '2025-10-07', '2025-11-29', NULL, ARRAY[2,4,6]::smallint[], 25, 'ONGOING', 'APPROVED', 6, 3, '2025-10-01 10:00:00+07', '2025-10-02 14:00:00+07', '2025-10-01 10:00:00+07', '2025-10-07 08:00:00+07'),

-- HN Branch - Class 4: SCHEDULED (for future enrollments)
-- Pattern: IELTSFOUND-HN01-25-004 (Started after Class 3, so sequence 004)
(4, 1, 1, 'IELTSFOUND-HN01-25-004', 'HN Foundation 3 (Scheduled)', 'HYBRID', '2025-11-18', '2026-01-10', NULL, ARRAY[1,3,5]::smallint[], 20, 'SCHEDULED', 'APPROVED', 7, 3, '2025-11-10 10:00:00+07', '2025-11-11 14:00:00+07', '2025-11-10 10:00:00+07', '2025-11-11 14:00:00+07'),

-- HCM Branch - Class 5: ONGOING
-- Pattern: IELTSFOUND-HCM01-25-001 (First class in HCM01 branch for this course/year)
(5, 2, 1, 'IELTSFOUND-HCM01-25-001', 'HCM Foundation 1 (Ongoing)', 'OFFLINE', '2025-10-13', '2025-12-05', NULL, ARRAY[1,3,5]::smallint[], 20, 'ONGOING', 'APPROVED', 8, 4, '2025-10-06 10:00:00+07', '2025-10-07 14:00:00+07', '2025-10-06 10:00:00+07', '2025-10-13 08:00:00+07'),

-- HCM Branch - Class 6: ONGOING (evening cohort for self-service transfers)
(6, 2, 1, 'HCM-FOUND-E1', 'HCM Foundation 2 (Evening)', 'OFFLINE', '2025-10-14', '2025-12-06', NULL, ARRAY[2,4,6]::smallint[], 18, 'ONGOING', 'APPROVED', 8, 4, '2025-10-08 10:00:00+07', '2025-10-09 14:00:00+07', '2025-10-08 10:00:00+07', '2025-10-14 08:00:00+07'),

-- HCM Branch - Class 7: ONGOING (micro cohort kept at capacity for negative cases)
(7, 2, 1, 'HCM-FOUND-M1', 'HCM Foundation Micro Cohort', 'OFFLINE', '2025-10-20', '2025-12-12', NULL, ARRAY[1,3,5]::smallint[], 5, 'ONGOING', 'APPROVED', 8, 4, '2025-10-10 10:00:00+07', '2025-10-11 14:00:00+07', '2025-10-10 10:00:00+07', '2025-10-20 08:00:00+07');

-- Generate Sessions for Class 1 (HN-FOUND-C1) - COMPLETED
-- Start: 2025-07-07 (Mon), Schedule: Mon/Wed/Fri, 24 sessions over 8 weeks
DO $$
DECLARE
    v_class_id BIGINT := 1;
    v_start_date DATE := '2025-07-07';
    v_session_count INT := 24;
    v_course_session_id INT;
    v_date DATE;
    v_week INT;
    v_day_idx INT;
    v_session_idx INT := 1;
BEGIN
    FOR v_week IN 0..7 LOOP -- 8 weeks
        FOR v_day_idx IN 1..3 LOOP -- 3 days per week
            EXIT WHEN v_session_idx > v_session_count;
            
            v_course_session_id := v_session_idx;
            -- Logic to calculate date for Mon/Wed/Fri
            v_date := v_start_date + (v_week * 7) + CASE v_day_idx WHEN 1 THEN 0 WHEN 2 THEN 2 WHEN 3 THEN 4 END;
            
            INSERT INTO session (id, class_id, course_session_id, time_slot_template_id, date, type, status, teacher_note, created_at, updated_at)
            VALUES (v_session_idx, v_class_id, v_course_session_id, 1, v_date, 'CLASS', 'DONE', 'Session completed as planned.', '2025-07-01 10:00:00+07', v_date);
            
            v_session_idx := v_session_idx + 1;
        END LOOP;
    END LOOP;
END $$;

-- Session Resources for Class 1
INSERT INTO session_resource (session_id, resource_id)
SELECT id, 2 FROM session WHERE class_id = 1;

-- Teaching Slots for Class 1 (assign Teacher 3)
INSERT INTO teaching_slot (session_id, teacher_id, status)
SELECT id, 3, 'SCHEDULED' FROM session WHERE class_id = 1;


-- Generate Sessions for Class 2 (HN-FOUND-O1) - Main testing class
-- Start: 2025-10-06 (Mon), Schedule: Mon/Wed/Fri, 24 sessions over 8 weeks
-- Today: 2025-11-02 (Sat) - Week 5 completed
DO $$
DECLARE
    v_class_id BIGINT := 2;
    v_start_date DATE := '2025-10-06';
    v_schedule_days INT[] := ARRAY[1,3,5]; -- Mon/Wed/Fri
    v_session_count INT := 24;
    v_course_session_id INT;
    v_date DATE;
    v_week INT;
    v_day_idx INT;
    v_session_idx INT := 1;
    v_status VARCHAR(20);
BEGIN
    FOR v_week IN 0..7 LOOP -- 8 weeks
        FOR v_day_idx IN 1..3 LOOP -- 3 days per week
            EXIT WHEN v_session_idx > v_session_count;
            
            v_course_session_id := v_session_idx;
            v_date := v_start_date + (v_week * 7 + (v_day_idx - 1) * 2); -- Mon, Wed, Fri spacing
            
            -- Set status based on reference date (2025-11-02)
            IF v_date < '2025-11-02' THEN
                v_status := 'DONE';
            ELSE
                v_status := 'PLANNED';
            END IF;
            
            INSERT INTO session (id, class_id, course_session_id, time_slot_template_id, date, type, status, created_at, updated_at)
            VALUES (100 + v_session_idx, v_class_id, v_course_session_id, 1, v_date, 'CLASS', v_status, '2025-09-30 10:00:00+07', CURRENT_TIMESTAMP);
            
            v_session_idx := v_session_idx + 1;
        END LOOP;
    END LOOP;
END $$;

-- Generate Sessions for Class 3 (HN-FOUND-O2) - For transfer scenario
-- Start: 2025-10-07 (Tue), Schedule: Tue/Thu/Sat
DO $$
DECLARE
    v_class_id BIGINT := 3;
    v_start_date DATE := '2025-10-07';
    v_session_count INT := 24;
    v_course_session_id INT;
    v_date DATE;
    v_week INT;
    v_day_idx INT;
    v_session_idx INT := 1;
    v_status VARCHAR(20);
BEGIN
    FOR v_week IN 0..7 LOOP
        FOR v_day_idx IN 1..3 LOOP
            EXIT WHEN v_session_idx > v_session_count;
            
            v_course_session_id := v_session_idx;
            v_date := v_start_date + (v_week * 7 + (v_day_idx - 1) * 2);
            
            IF v_date < '2025-11-02' THEN
                v_status := 'DONE';
            ELSE
                v_status := 'PLANNED';
            END IF;
            
            INSERT INTO session (id, class_id, course_session_id, time_slot_template_id, date, type, status, created_at, updated_at)
            VALUES (200 + v_session_idx, v_class_id, v_course_session_id, 4, v_date, 'CLASS', v_status, '2025-10-01 10:00:00+07', CURRENT_TIMESTAMP);
            
            v_session_idx := v_session_idx + 1;
        END LOOP;
    END LOOP;
END $$;

-- Session Resources for Class 2
INSERT INTO session_resource (session_id, resource_id)
SELECT id, 1 FROM session WHERE class_id = 2;

-- Session Resources for Class 3 (online - use Zoom)
INSERT INTO session_resource (session_id, resource_id)
SELECT id, 4 FROM session WHERE class_id = 3;

-- Teaching Slots for Class 2 (assign Teacher 1)
INSERT INTO teaching_slot (session_id, teacher_id, status)
SELECT id, 1, 'SCHEDULED' FROM session WHERE class_id = 2;

-- Teaching Slots for Class 3 (assign Teacher 2)
INSERT INTO teaching_slot (session_id, teacher_id, status)
SELECT id, 2, 'SCHEDULED' FROM session WHERE class_id = 3;

-- Generate Sessions for Class 5 (HCM-FOUND-O1) - ONGOING
-- Start: 2025-10-13 (Mon), Schedule: Mon/Wed/Fri
DO $$
DECLARE
    v_class_id BIGINT := 5;
    v_start_date DATE := '2025-10-13';
    v_session_count INT := 24;
    v_course_session_id INT;
    v_date DATE;
    v_week INT;
    v_day_idx INT;
    v_session_idx INT := 1;
    v_status VARCHAR(20);
BEGIN
    FOR v_week IN 0..7 LOOP
        FOR v_day_idx IN 1..3 LOOP
            EXIT WHEN v_session_idx > v_session_count;
            
            v_course_session_id := v_session_idx;
            v_date := v_start_date + (v_week * 7) + CASE v_day_idx WHEN 1 THEN 0 WHEN 2 THEN 2 WHEN 3 THEN 4 END;
            
            IF v_date < '2025-11-02' THEN
                v_status := 'DONE';
            ELSE
                v_status := 'PLANNED';
            END IF;
            
            INSERT INTO session (id, class_id, course_session_id, time_slot_template_id, date, type, status, created_at, updated_at)
            VALUES (300 + v_session_idx, v_class_id, v_course_session_id, 6, v_date, 'CLASS', v_status, '2025-10-06 10:00:00+07', CURRENT_TIMESTAMP);
            
            v_session_idx := v_session_idx + 1;
        END LOOP;
    END LOOP;
END $$;

-- Session Resources for Class 5
INSERT INTO session_resource (session_id, resource_id)
SELECT id, 5 FROM session WHERE class_id = 5;

-- Teaching Slots for Class 5 (assign Teacher 9)
INSERT INTO teaching_slot (session_id, teacher_id, status)
SELECT id, 9, 'SCHEDULED' FROM session WHERE class_id = 5;

-- Generate Sessions for Class 4 (HN-FOUND-S1) - SCHEDULED (Future class)
-- Start: 2025-11-18 (Mon), Schedule: Mon/Wed/Fri, 24 sessions over 8 weeks
-- All sessions are PLANNED (in the future)
DO $$
DECLARE
    v_class_id BIGINT := 4;
    v_start_date DATE := '2025-11-18'; -- 18/11/2025 (Monday)
    v_session_count INT := 24;
    v_course_session_id INT;
    v_date DATE;
    v_week INT;
    v_day_idx INT;
    v_session_idx INT := 1;
    v_status VARCHAR(20);
BEGIN
    FOR v_week IN 0..7 LOOP -- 8 weeks
        FOR v_day_idx IN 1..3 LOOP -- Mon/Wed/Fri
            EXIT WHEN v_session_idx > v_session_count;
            
            v_course_session_id := v_session_idx; -- Map to course_session 1-24
            -- Calculate date: Mon=0, Wed=2, Fri=4 days offset from Monday
            v_date := v_start_date + (v_week * 7) + CASE v_day_idx WHEN 1 THEN 0 WHEN 2 THEN 2 WHEN 3 THEN 4 END;
            
            -- All sessions are PLANNED (future class)
            v_status := 'PLANNED';
            
            -- Insert session with ID range 400-423 (to avoid conflict with other classes)
            INSERT INTO session (id, class_id, course_session_id, time_slot_template_id, date, type, status, created_at, updated_at)
            VALUES (400 + v_session_idx, v_class_id, v_course_session_id, 3, v_date, 'CLASS', v_status, '2025-11-10 10:00:00+07', CURRENT_TIMESTAMP);
            
            v_session_idx := v_session_idx + 1;
        END LOOP;
    END LOOP;
END $$;

-- Session Resources for Class 4 (Hybrid - assign Room 201)
-- For hybrid classes, we assign a primary physical room
INSERT INTO session_resource (session_id, resource_id)
SELECT id, 3 FROM session WHERE class_id = 4;

-- Teaching Slots for Class 4 (assign Teacher 3)
-- Teacher 3 (David Lee) is available and has GENERAL + SPEAKING skills
INSERT INTO teaching_slot (session_id, teacher_id, status)
SELECT id, 3, 'SCHEDULED' FROM session WHERE class_id = 4;

-- Generate Sessions for Class 6 (HCM-FOUND-E1) - ONGOING evening class
-- Start: 2025-10-14 (Tue), Schedule: Tue/Thu/Sat, Evening slot
DO $$
DECLARE
    v_class_id BIGINT := 6;
    v_start_date DATE := '2025-10-14';
    v_session_count INT := 24;
    v_course_session_id INT;
    v_date DATE;
    v_week INT;
    v_day_idx INT;
    v_session_idx INT := 1;
    v_status VARCHAR(20);
BEGIN
    FOR v_week IN 0..7 LOOP
        FOR v_day_idx IN 1..3 LOOP
            EXIT WHEN v_session_idx > v_session_count;

            v_course_session_id := v_session_idx;
            v_date := v_start_date + (v_week * 7) + CASE v_day_idx WHEN 1 THEN 0 WHEN 2 THEN 2 WHEN 3 THEN 4 END;

            IF v_date < '2025-11-02' THEN
                v_status := 'DONE';
            ELSE
                v_status := 'PLANNED';
            END IF;

            INSERT INTO session (id, class_id, course_session_id, time_slot_template_id, date, type, status, created_at, updated_at)
            VALUES (500 + v_session_idx, v_class_id, v_course_session_id, 8, v_date, 'CLASS', v_status, '2025-10-08 10:00:00+07', CURRENT_TIMESTAMP);

            v_session_idx := v_session_idx + 1;
        END LOOP;
    END LOOP;
END $$;

-- Session Resources for Class 6 (assign HCM Room 102)
INSERT INTO session_resource (session_id, resource_id)
SELECT id, 6 FROM session WHERE class_id = 6;

-- Teaching Slots for Class 6 (assign Teacher 10 - Olivia White)
INSERT INTO teaching_slot (session_id, teacher_id, status)
SELECT id, 10, 'SCHEDULED' FROM session WHERE class_id = 6;

-- Generate Sessions for Class 7 (HCM-FOUND-M1) - Micro cohort kept at capacity
-- Start: 2025-10-20 (Mon), Schedule: Mon/Wed/Fri, Afternoon slot
DO $$
DECLARE
    v_class_id BIGINT := 7;
    v_start_date DATE := '2025-10-20';
    v_session_count INT := 24;
    v_course_session_id INT;
    v_date DATE;
    v_week INT;
    v_day_idx INT;
    v_session_idx INT := 1;
    v_status VARCHAR(20);
BEGIN
    FOR v_week IN 0..7 LOOP
        FOR v_day_idx IN 1..3 LOOP
            EXIT WHEN v_session_idx > v_session_count;

            v_course_session_id := v_session_idx;
            v_date := v_start_date + (v_week * 7) + CASE v_day_idx WHEN 1 THEN 0 WHEN 2 THEN 2 WHEN 3 THEN 4 END;

            IF v_date < '2025-11-02' THEN
                v_status := 'DONE';
            ELSE
                v_status := 'PLANNED';
            END IF;

            INSERT INTO session (id, class_id, course_session_id, time_slot_template_id, date, type, status, created_at, updated_at)
            VALUES (600 + v_session_idx, v_class_id, v_course_session_id, 7, v_date, 'CLASS', v_status, '2025-10-10 10:00:00+07', CURRENT_TIMESTAMP);

            v_session_idx := v_session_idx + 1;
        END LOOP;
    END LOOP;
END $$;

-- Session Resources for Class 7 (assign HCM Room 201)
INSERT INTO session_resource (session_id, resource_id)
SELECT id, 7 FROM session WHERE class_id = 7;

-- Teaching Slots for Class 7 (assign Teacher 11 - Daniel Harris)
INSERT INTO teaching_slot (session_id, teacher_id, status)
SELECT id, 11, 'SCHEDULED' FROM session WHERE class_id = 7;


-- ========== MAKEUP OPTIONS TEST DATA ==========
-- Generate makeup sessions for student 1's absences (2025-11-03 to 2025-11-16)

-- Scenario A: OFFLINE class same branch (HN) - PERFECT MATCH
INSERT INTO "class" (id, branch_id, course_id, code, name, modality, start_date, planned_end_date, schedule_days, max_capacity, status, approval_status, created_by, decided_by, submitted_at, decided_at, created_at, updated_at) VALUES
(8, 1, 1, 'HN-FOUND-MAKEUP-O', 'HN Foundation Makeup (Offline)', 'OFFLINE', '2025-11-05', '2025-12-21', ARRAY[3,5]::smallint[], 15, 'ONGOING', 'APPROVED', 6, 3, '2025-11-01 10:00:00+07', '2025-11-01 14:00:00+07', '2025-11-01 10:00:00+07', '2025-11-01 14:00:00+07');

-- Scenario B: ONLINE class different branch (HCM) - CROSS-BRANCH MATCH
INSERT INTO "class" (id, branch_id, course_id, code, name, modality, start_date, planned_end_date, schedule_days, max_capacity, status, approval_status, created_by, decided_by, submitted_at, decided_at, created_at, updated_at) VALUES
(9, 2, 1, 'HCM-FOUND-MAKEUP-ON', 'HCM Foundation Makeup (Online)', 'ONLINE', '2025-11-06', '2025-12-22', ARRAY[4,6]::smallint[], 25, 'ONGOING', 'APPROVED', 8, 4, '2025-11-01 10:00:00+07', '2025-11-01 14:00:00+07', '2025-11-01 10:00:00+07', '2025-11-01 14:00:00+07');

-- Scenario C: OFFLINE class different branch (HCM) - SHOULD BE FILTERED
INSERT INTO "class" (id, branch_id, course_id, code, name, modality, start_date, planned_end_date, schedule_days, max_capacity, status, approval_status, created_by, decided_by, submitted_at, decided_at, created_at, updated_at) VALUES
(10, 2, 1, 'HCM-FOUND-MAKEUP-OFF', 'HCM Foundation Makeup (Offline)', 'OFFLINE', '2025-11-04', '2025-12-20', ARRAY[2,4]::smallint[], 15, 'ONGOING', 'APPROVED', 8, 4, '2025-11-01 10:00:00+07', '2025-11-01 14:00:00+07', '2025-11-01 10:00:00+07', '2025-11-01 14:00:00+07');

-- Sessions for makeup classes
DO $$
DECLARE
    -- Course session IDs corresponding to student 1's recent absences
    v_absent_course_session_ids INT[] := ARRAY[10, 11, 12, 16]; -- Added 16 for the user's test case
    v_session_id_counter INT := 700;
BEGIN
    -- Class 8 Sessions (HN, OFFLINE) - Wed, Fri
    INSERT INTO session (id, class_id, course_session_id, time_slot_template_id, date, type, status, created_at, updated_at) VALUES
    (v_session_id_counter + 1, 8, v_absent_course_session_ids[1], 3, '2025-11-05', 'CLASS', 'PLANNED', NOW(), NOW()),
    (v_session_id_counter + 2, 8, v_absent_course_session_ids[2], 3, '2025-11-07', 'CLASS', 'PLANNED', NOW(), NOW()),
    (v_session_id_counter + 3, 8, v_absent_course_session_ids[3], 3, '2025-11-12', 'CLASS', 'PLANNED', NOW(), NOW()),
    (v_session_id_counter + 10, 8, v_absent_course_session_ids[4], 3, '2025-11-14', 'CLASS', 'PLANNED', NOW(), NOW()); -- Makeup for session 116 (course_session_id 16)

    -- Class 9 Sessions (HCM, ONLINE) - Thu, Sat
    INSERT INTO session (id, class_id, course_session_id, time_slot_template_id, date, type, status, created_at, updated_at) VALUES
    (v_session_id_counter + 4, 9, v_absent_course_session_ids[1], 7, '2025-11-06', 'CLASS', 'PLANNED', NOW(), NOW()),
    (v_session_id_counter + 5, 9, v_absent_course_session_ids[2], 7, '2025-11-08', 'CLASS', 'PLANNED', NOW(), NOW()),
    (v_session_id_counter + 6, 9, v_absent_course_session_ids[3], 7, '2025-11-13', 'CLASS', 'PLANNED', NOW(), NOW()),
    (v_session_id_counter + 11, 9, v_absent_course_session_ids[4], 7, '2025-11-15', 'CLASS', 'PLANNED', NOW(), NOW()); -- Makeup for session 116 (course_session_id 16)

    -- Class 10 Sessions (HCM, OFFLINE) - Tue, Thu
    INSERT INTO session (id, class_id, course_session_id, time_slot_template_id, date, type, status, created_at, updated_at) VALUES
    (v_session_id_counter + 7, 10, v_absent_course_session_ids[1], 7, '2025-11-04', 'CLASS', 'PLANNED', NOW(), NOW()),
    (v_session_id_counter + 8, 10, v_absent_course_session_ids[2], 7, '2025-11-06', 'CLASS', 'PLANNED', NOW(), NOW()),
    (v_session_id_counter + 9, 10, v_absent_course_session_ids[3], 7, '2025-11-11', 'CLASS', 'PLANNED', NOW(), NOW()),
    (v_session_id_counter + 12, 10, v_absent_course_session_ids[4], 7, '2025-11-13', 'CLASS', 'PLANNED', NOW(), NOW()); -- Makeup for session 116 (course_session_id 16)
END $$;

-- Assign resources and teachers for makeup classes
-- Class 8 (HN Offline) -> Room 102, Teacher 4
INSERT INTO session_resource (session_id, resource_id) SELECT id, 2 FROM session WHERE class_id = 8;
INSERT INTO teaching_slot (session_id, teacher_id, status) SELECT id, 4, 'SCHEDULED' FROM session WHERE class_id = 8;

-- Class 9 (HCM Online) -> Zoom 01, Teacher 12
INSERT INTO session_resource (session_id, resource_id) SELECT id, 8 FROM session WHERE class_id = 9;
INSERT INTO teaching_slot (session_id, teacher_id, status) SELECT id, 12, 'SCHEDULED' FROM session WHERE class_id = 9;

-- Class 10 (HCM Offline) -> Room 101, Teacher 13
INSERT INTO session_resource (session_id, resource_id) SELECT id, 5 FROM session WHERE class_id = 10;
INSERT INTO teaching_slot (session_id, teacher_id, status) SELECT id, 13, 'SCHEDULED' FROM session WHERE class_id = 10;

-- ========== TIER 5: ENROLLMENTS & ATTENDANCE ==========

-- Enrollments for Class 1 (HN-FOUND-C1) - 15 students, all completed
-- This represents the learning history for students who are now in other classes.
INSERT INTO enrollment (id, class_id, student_id, status, enrolled_at, enrolled_by, join_session_id, left_at, left_session_id, created_at, updated_at)
SELECT 
    (100 + s.id), -- New enrollment IDs to avoid conflict
    1,
    s.id,
    'COMPLETED',
    '2025-07-01 09:00:00+07',
    6,
    1, -- join_session_id
    '2025-09-01 18:00:00+07', -- left_at
    24, -- left_session_id
    '2025-07-01 09:00:00+07',
    '2025-09-01 18:00:00+07'
FROM generate_series(1, 15) AS s(id);

-- Student Sessions for Class 1 (COMPLETED)
INSERT INTO student_session (student_id, session_id, is_makeup, attendance_status, homework_status, recorded_at, created_at, updated_at)
SELECT 
    e.student_id,
    s.id,
    false,
    CASE 
        WHEN random() < 0.9 THEN 'PRESENT'
        ELSE 'ABSENT'
    END,
    CASE 
        WHEN cs.student_task IS NOT NULL THEN
            CASE 
                WHEN random() < 0.85 THEN 'COMPLETED'
                ELSE 'INCOMPLETE'
            END
        ELSE NULL
    END,
    s.date,
    CURRENT_TIMESTAMP
FROM enrollment e
CROSS JOIN session s
LEFT JOIN course_session cs ON s.course_session_id = cs.id
WHERE e.class_id = 1 
  AND s.class_id = 1;

-- Enrollments for Class 2 (HN-FOUND-O1) - 17 students (including mid-course and transfer candidate)
INSERT INTO enrollment (id, class_id, student_id, status, enrolled_at, enrolled_by, join_session_id, created_at, updated_at) VALUES
(1, 2, 1, 'ENROLLED', '2025-10-01 09:00:00+07', 6, 101, '2025-10-01 09:00:00+07', '2025-10-01 09:00:00+07'),
(2, 2, 2, 'ENROLLED', '2025-10-01 09:00:00+07', 6, 101, '2025-10-01 09:00:00+07', '2025-10-01 09:00:00+07'),
(3, 2, 3, 'ENROLLED', '2025-10-01 09:00:00+07', 6, 101, '2025-10-01 09:00:00+07', '2025-10-01 09:00:00+07'),
(4, 2, 4, 'ENROLLED', '2025-10-01 09:00:00+07', 6, 101, '2025-10-01 09:00:00+07', '2025-10-01 09:00:00+07'),
(5, 2, 5, 'ENROLLED', '2025-10-01 09:00:00+07', 6, 101, '2025-10-01 09:00:00+07', '2025-10-01 09:00:00+07'),
(6, 2, 6, 'ENROLLED', '2025-10-01 09:00:00+07', 6, 101, '2025-10-01 09:00:00+07', '2025-10-01 09:00:00+07'),
(7, 2, 7, 'ENROLLED', '2025-10-01 09:00:00+07', 6, 101, '2025-10-01 09:00:00+07', '2025-10-01 09:00:00+07'),
(8, 2, 8, 'ENROLLED', '2025-10-01 09:00:00+07', 6, 101, '2025-10-01 09:00:00+07', '2025-10-01 09:00:00+07'),
(9, 2, 9, 'ENROLLED', '2025-10-01 09:00:00+07', 6, 101, '2025-10-01 09:00:00+07', '2025-10-01 09:00:00+07'),
(10, 2, 10, 'ENROLLED', '2025-10-01 09:00:00+07', 6, 101, '2025-10-01 09:00:00+07', '2025-10-01 09:00:00+07'),
(11, 2, 11, 'ENROLLED', '2025-10-01 09:00:00+07', 6, 101, '2025-10-01 09:00:00+07', '2025-10-01 09:00:00+07'),
(12, 2, 12, 'ENROLLED', '2025-10-01 09:00:00+07', 6, 101, '2025-10-01 09:00:00+07', '2025-10-01 09:00:00+07'),
(13, 2, 13, 'ENROLLED', '2025-10-01 09:00:00+07', 6, 101, '2025-10-01 09:00:00+07', '2025-10-01 09:00:00+07'),
(14, 2, 14, 'ENROLLED', '2025-10-01 09:00:00+07', 6, 101, '2025-10-01 09:00:00+07', '2025-10-01 09:00:00+07'),
(15, 2, 15, 'ENROLLED', '2025-10-01 09:00:00+07', 6, 101, '2025-10-01 09:00:00+07', '2025-10-01 09:00:00+07'),
-- Mid-course enrollment (enrolled after start) - for testing
(16, 2, 16, 'ENROLLED', '2025-10-20 14:00:00+07', 6, 109, '2025-10-20 14:00:00+07', '2025-10-20 14:00:00+07'),
-- Student 18 - will be transferred later (SCENARIO 6)
(17, 2, 18, 'ENROLLED', '2025-10-01 09:00:00+07', 6, 101, '2025-10-01 09:00:00+07', '2025-10-01 09:00:00+07');

-- Enrollments for Class 3 (HN-FOUND-O2) - 12 students
INSERT INTO enrollment (id, class_id, student_id, status, enrolled_at, enrolled_by, join_session_id, created_at, updated_at) VALUES
(20, 3, 20, 'ENROLLED', '2025-10-02 09:00:00+07', 6, 201, '2025-10-02 09:00:00+07', '2025-10-02 09:00:00+07'),
(21, 3, 21, 'ENROLLED', '2025-10-02 09:00:00+07', 6, 201, '2025-10-02 09:00:00+07', '2025-10-02 09:00:00+07'),
(22, 3, 22, 'ENROLLED', '2025-10-02 09:00:00+07', 6, 201, '2025-10-02 09:00:00+07', '2025-10-02 09:00:00+07'),
(23, 3, 23, 'ENROLLED', '2025-10-02 09:00:00+07', 6, 201, '2025-10-02 09:00:00+07', '2025-10-02 09:00:00+07'),
(24, 3, 24, 'ENROLLED', '2025-10-02 09:00:00+07', 6, 201, '2025-10-02 09:00:00+07', '2025-10-02 09:00:00+07'),
(25, 3, 25, 'ENROLLED', '2025-10-02 09:00:00+07', 6, 201, '2025-10-02 09:00:00+07', '2025-10-02 09:00:00+07'),
(26, 3, 26, 'ENROLLED', '2025-10-02 09:00:00+07', 6, 201, '2025-10-02 09:00:00+07', '2025-10-02 09:00:00+07'),
(27, 3, 27, 'ENROLLED', '2025-10-02 09:00:00+07', 6, 201, '2025-10-02 09:00:00+07', '2025-10-02 09:00:00+07'),
(28, 3, 28, 'ENROLLED', '2025-10-02 09:00:00+07', 6, 201, '2025-10-02 09:00:00+07', '2025-10-02 09:00:00+07'),
(29, 3, 29, 'ENROLLED', '2025-10-02 09:00:00+07', 6, 201, '2025-10-02 09:00:00+07', '2025-10-02 09:00:00+07'),
(30, 3, 30, 'ENROLLED', '2025-10-02 09:00:00+07', 6, 201, '2025-10-02 09:00:00+07', '2025-10-02 09:00:00+07'),
(31, 3, 17, 'ENROLLED', '2025-10-02 09:00:00+07', 6, 201, '2025-10-02 09:00:00+07', '2025-10-02 09:00:00+07');

-- Student Sessions for Class 2 enrollments
-- Generate for all students x all sessions (done + planned)
INSERT INTO student_session (student_id, session_id, is_makeup, attendance_status, homework_status, recorded_at, created_at, updated_at)
SELECT 
    e.student_id,
    s.id,
    false,
    CASE 
        WHEN s.status = 'DONE' THEN 
            CASE 
                -- Most students present
                WHEN random() < 0.85 THEN 'PRESENT'
                -- Some absences for testing
                ELSE 'ABSENT'
            END
        ELSE 'PLANNED'
    END,
    CASE 
        WHEN s.status = 'DONE' AND cs.student_task IS NOT NULL THEN
            CASE 
                WHEN random() < 0.8 THEN 'COMPLETED'
                ELSE 'INCOMPLETE'
            END
        ELSE NULL
    END,
    CASE WHEN s.status = 'DONE' THEN s.date ELSE NULL END,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM enrollment e
CROSS JOIN session s
LEFT JOIN course_session cs ON s.course_session_id = cs.id
WHERE e.class_id = 2 
  AND s.class_id = 2
  AND (e.join_session_id IS NULL OR s.id >= e.join_session_id);

-- MAKEUP FLOW TEST DATA: Create recent absences for student 1
-- Mark student 1 as ABSENT for 3 recent sessions (last week of Oct 2025)
UPDATE student_session 
SET attendance_status = 'ABSENT', note = 'Missed session, eligible for makeup.'
WHERE student_id = 1 
  AND session_id IN (
    SELECT id FROM session 
    WHERE class_id = 2 
      AND status = 'DONE'
      AND date >= '2025-10-27'  -- Last week
      AND date <= '2025-11-01'  -- Before reference date
    ORDER BY date DESC
    LIMIT 3
  );

-- Student Sessions for Class 3
INSERT INTO student_session (student_id, session_id, is_makeup, attendance_status, homework_status, recorded_at, created_at, updated_at)
SELECT 
    e.student_id,
    s.id,
    false,
    CASE 
        WHEN s.status = 'DONE' THEN 
            CASE WHEN random() < 0.9 THEN 'PRESENT' ELSE 'ABSENT' END
        ELSE 'PLANNED'
    END,
    CASE 
        WHEN s.status = 'DONE' AND cs.student_task IS NOT NULL THEN
            CASE WHEN random() < 0.85 THEN 'COMPLETED' ELSE 'INCOMPLETE' END
        ELSE NULL
    END,
    CASE WHEN s.status = 'DONE' THEN s.date ELSE NULL END,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM enrollment e
CROSS JOIN session s
LEFT JOIN course_session cs ON s.course_session_id = cs.id
WHERE e.class_id = 3 
  AND s.class_id = 3
  AND (e.join_session_id IS NULL OR s.id >= e.join_session_id);

-- Enrollments for Class 5 (HCM-FOUND-O1) - 15 students
INSERT INTO enrollment (id, class_id, student_id, status, enrolled_at, enrolled_by, join_session_id, created_at, updated_at)
SELECT 
    (200 + s.id), -- New enrollment IDs
    5,
    (30 + s.id), -- Students 31-45
    'ENROLLED',
    '2025-10-07 09:00:00+07',
    8, -- enrolled_by HCM staff
    301, -- join_session_id
    '2025-10-07 09:00:00+07',
    '2025-10-07 09:00:00+07'
FROM generate_series(1, 15) AS s(id);

-- Student Sessions for Class 5
INSERT INTO student_session (student_id, session_id, is_makeup, attendance_status, homework_status, recorded_at, created_at, updated_at)
SELECT 
    e.student_id,
    s.id,
    false,
    CASE 
        WHEN s.status = 'DONE' THEN 
            CASE WHEN random() < 0.9 THEN 'PRESENT' ELSE 'ABSENT' END
        ELSE 'PLANNED'
    END,
    CASE 
        WHEN s.status = 'DONE' AND cs.student_task IS NOT NULL THEN
            CASE WHEN random() < 0.85 THEN 'COMPLETED' ELSE 'INCOMPLETE' END
        ELSE NULL
    END,
    CASE WHEN s.status = 'DONE' THEN s.date ELSE NULL END,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM enrollment e
CROSS JOIN session s
LEFT JOIN course_session cs ON s.course_session_id = cs.id
WHERE e.class_id = 5 
  AND s.class_id = 5;

-- Enrollments for Class 6 (HCM-FOUND-E1) - Evening cohort (10 students, slots available)
INSERT INTO enrollment (id, class_id, student_id, status, enrolled_at, enrolled_by, join_session_id, created_at, updated_at)
SELECT 
    (400 + s.id),
    6,
    (45 + s.id), -- Students 46-55 (HCM cohort 2)
    'ENROLLED',
    '2025-10-12 19:00:00+07',
    8,
    501,
    '2025-10-12 19:00:00+07',
    '2025-10-12 19:00:00+07'
FROM generate_series(1, 10) AS s(id);

-- Student Sessions for Class 6
INSERT INTO student_session (student_id, session_id, is_makeup, attendance_status, homework_status, recorded_at, created_at, updated_at)
SELECT 
    e.student_id,
    s.id,
    false,
    CASE 
        WHEN s.status = 'DONE' THEN 
            CASE WHEN random() < 0.88 THEN 'PRESENT' ELSE 'ABSENT' END
        ELSE 'PLANNED'
    END,
    CASE 
        WHEN s.status = 'DONE' AND cs.student_task IS NOT NULL THEN
            CASE WHEN random() < 0.8 THEN 'COMPLETED' ELSE 'INCOMPLETE' END
        ELSE NULL
    END,
    CASE WHEN s.status = 'DONE' THEN s.date ELSE NULL END,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM enrollment e
CROSS JOIN session s
LEFT JOIN course_session cs ON s.course_session_id = cs.id
WHERE e.class_id = 6
  AND s.class_id = 6
  AND (e.join_session_id IS NULL OR s.id >= e.join_session_id);

-- Enrollments for Class 7 (HCM-FOUND-M1) - Micro cohort (hard cap at 5 to simulate full class)
INSERT INTO enrollment (id, class_id, student_id, status, enrolled_at, enrolled_by, join_session_id, created_at, updated_at)
SELECT 
    (420 + s.id),
    7,
    (55 + s.id), -- Students 56-60
    'ENROLLED',
    '2025-10-18 14:00:00+07',
    8,
    601,
    '2025-10-18 14:00:00+07',
    '2025-10-18 14:00:00+07'
FROM generate_series(1, 5) AS s(id);

-- Student Sessions for Class 7
INSERT INTO student_session (student_id, session_id, is_makeup, attendance_status, homework_status, recorded_at, created_at, updated_at)
SELECT 
    e.student_id,
    s.id,
    false,
    CASE 
        WHEN s.status = 'DONE' THEN 
            CASE WHEN random() < 0.92 THEN 'PRESENT' ELSE 'ABSENT' END
        ELSE 'PLANNED'
    END,
    CASE 
        WHEN s.status = 'DONE' AND cs.student_task IS NOT NULL THEN
            CASE WHEN random() < 0.82 THEN 'COMPLETED' ELSE 'INCOMPLETE' END
        ELSE NULL
    END,
    CASE WHEN s.status = 'DONE' THEN s.date ELSE NULL END,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM enrollment e
CROSS JOIN session s
LEFT JOIN course_session cs ON s.course_session_id = cs.id
WHERE e.class_id = 7
  AND s.class_id = 7;

-- ========== TIER 6: REQUESTS (Test all scenarios) ==========

-- SCENARIO 1: Approved Absence Request
INSERT INTO student_request (id, student_id, current_class_id, request_type, target_session_id, status, request_reason, submitted_by, submitted_at, decided_by, decided_at, note) VALUES
(1, 1, 2, 'ABSENCE', 116, 'APPROVED', 'Family emergency - need to attend urgent family matter', 101, '2025-10-25 10:00:00+07', 6, '2025-10-25 14:00:00+07', 'Approved - valid reason');

-- Update corresponding student_session for approved absence
UPDATE student_session 
SET attendance_status = 'ABSENT', note = 'Approved absence: Family emergency'
WHERE student_id = 1 AND session_id = 116;

-- SCENARIO 2: Pending Absence Request (for testing approval flow)
INSERT INTO student_request (id, student_id, current_class_id, request_type, target_session_id, status, request_reason, submitted_by, submitted_at) VALUES
(2, 2, 2, 'ABSENCE', 117, 'PENDING', 'Medical appointment - doctor consultation scheduled', 102, '2025-10-30 09:00:00+07');

-- SCENARIO 3: Rejected Absence Request
INSERT INTO student_request (id, student_id, current_class_id, request_type, target_session_id, status, request_reason, submitted_by, submitted_at, decided_by, decided_at, note) VALUES
(3, 3, 2, 'ABSENCE', 118, 'REJECTED', 'Want to attend friend birthday party', 103, '2025-10-28 10:00:00+07', 6, '2025-10-28 15:00:00+07', 'Rejected - not a valid reason for academic absence');

-- SCENARIO 4: Approved Makeup Request (cross-class)
INSERT INTO student_request (id, student_id, current_class_id, request_type, target_session_id, makeup_session_id, status, request_reason, submitted_by, submitted_at, decided_by, decided_at) VALUES
(4, 4, 2, 'MAKEUP', 107, 213, 'APPROVED', 'Missed session due to illness, want to makeup in online class', 104, '2025-10-22 10:00:00+07', 6, '2025-10-22 16:00:00+07');

-- Create makeup student_session for approved makeup
INSERT INTO student_session (student_id, session_id, is_makeup, makeup_session_id, original_session_id, attendance_status, note, created_at, updated_at)
VALUES (4, 213, true, 213, 107, 'PLANNED', 'Makeup for missed session #107', '2025-10-22 16:00:00+07', '2025-10-22 16:00:00+07');

-- Update original session note
UPDATE student_session 
SET note = 'Approved for makeup session #213'
WHERE student_id = 4 AND session_id = 107;

-- SCENARIO 5: Pending Makeup Request
INSERT INTO student_request (id, student_id, current_class_id, request_type, target_session_id, makeup_session_id, status, request_reason, submitted_by, submitted_at) VALUES
(5, 5, 2, 'MAKEUP', 108, 214, 'PENDING', 'Missed session due to work commitment, requesting makeup', 105, '2025-10-31 11:00:00+07');

-- SCENARIO 6: Approved Transfer Request
INSERT INTO student_request (id, student_id, current_class_id, target_class_id, request_type, effective_date, effective_session_id, status, request_reason, submitted_by, submitted_at, decided_by, decided_at) VALUES
(6, 18, 2, 3, 'TRANSFER', '2025-11-04', 214, 'APPROVED', 'Need to change to online class due to work schedule conflict', 6, '2025-10-27 10:00:00+07', 6, '2025-10-28 14:00:00+07');

-- Execute transfer: Update old enrollment
UPDATE enrollment 
SET status = 'TRANSFERRED', left_at = '2025-11-04 00:00:00+07', left_session_id = 113
WHERE student_id = 18 AND class_id = 2;

-- Execute transfer: Create new enrollment
INSERT INTO enrollment (id, class_id, student_id, status, enrolled_at, enrolled_by, join_session_id, created_at, updated_at)
VALUES (40, 3, 18, 'ENROLLED', '2025-11-04 00:00:00+07', 6, 214, '2025-11-04 00:00:00+07', '2025-11-04 00:00:00+07');

-- Execute transfer: Mark future sessions in old class as absent
UPDATE student_session 
SET attendance_status = 'ABSENT', note = 'Transferred to class #3'
WHERE student_id = 18 AND session_id IN (
    SELECT id FROM session WHERE class_id = 2 AND date >= '2025-11-04'
);

-- Execute transfer: Generate student_sessions in new class for future sessions
INSERT INTO student_session (student_id, session_id, is_makeup, attendance_status, note, created_at, updated_at)
SELECT 18, s.id, false, 'PLANNED', 'Transferred from class #2', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM session s
WHERE s.class_id = 3 AND s.date >= '2025-11-04';

-- Additional Transfer Scenarios to cover Tier 1, Tier 2, and error cases

-- SCENARIO 6A: Historical transfer (counts toward quota) - Student 1 moved from Class 1 -> Class 2
INSERT INTO student_request (id, student_id, current_class_id, target_class_id, request_type, effective_date, effective_session_id, status, request_reason, submitted_by, submitted_at, decided_by, decided_at, note) VALUES
(8, 1, 1, 2, 'TRANSFER', '2025-10-06', 101, 'APPROVED', 'Move to new weekday class after completing previous cohort', 101, '2025-09-28 09:00:00+07', 6, '2025-09-29 10:00:00+07', 'Historical transfer counted toward quota');

-- SCENARIO 6B: Tier 2 on-behalf transfer awaiting student confirmation (modality change)
INSERT INTO student_request (id, student_id, current_class_id, target_class_id, request_type, effective_date, effective_session_id, status, request_reason, submitted_by, submitted_at, note) VALUES
(9, 12, 2, 3, 'TRANSFER', '2025-11-06', 214, 'WAITING_CONFIRM', 'Needs online sessions while caring for family member', 6, '2025-11-01 08:30:00+07', 'Awaiting guardian confirmation of switch');

-- SCENARIO 6C: Tier 2 branch change (HN -> HCM) created by Academic Affairs, pending approval
INSERT INTO student_request (id, student_id, current_class_id, target_class_id, request_type, effective_date, effective_session_id, status, request_reason, submitted_by, submitted_at, note) VALUES
(10, 9, 2, 5, 'TRANSFER', '2025-11-10', 313, 'PENDING', 'Family relocation to Ho Chi Minh City - needs branch transfer', 6, '2025-11-02 10:00:00+07', 'Tier 2 branch change - requires center head approval');

-- SCENARIO 6D: Tier 1 self-service request (same branch + modality) - pending
INSERT INTO student_request (id, student_id, current_class_id, target_class_id, request_type, effective_date, effective_session_id, status, request_reason, submitted_by, submitted_at, note) VALUES
(11, 33, 5, 6, 'TRANSFER', '2025-11-06', 511, 'PENDING', 'Need later evening slot due to new work shift', 133, '2025-11-02 20:00:00+07', 'Tier 1 self-service request, awaiting AA review');

-- SCENARIO 6E: Tier 1 request cancelled by student before approval
INSERT INTO student_request (id, student_id, current_class_id, target_class_id, request_type, effective_date, effective_session_id, status, request_reason, submitted_by, submitted_at, note) VALUES
(12, 35, 5, 6, 'TRANSFER', '2025-11-08', 512, 'CANCELLED', 'Shift change reverted, will stay with current class', 135, '2025-10-30 07:30:00+07', 'Student cancelled before AA review was completed');

-- SCENARIO 6F: Rejected because target class is full (uses Class 7 micro cohort)
INSERT INTO student_request (id, student_id, current_class_id, target_class_id, request_type, effective_date, effective_session_id, status, request_reason, submitted_by, submitted_at, decided_by, decided_at, note) VALUES
(13, 34, 5, 7, 'TRANSFER', '2025-11-05', 608, 'REJECTED', 'Prefers smaller micro cohort for extra coaching', 134, '2025-11-01 19:00:00+07', 8, '2025-11-02 09:00:00+07', 'Rejected - target class is full (TRF_CLASS_FULL)');

-- SCENARIO 6G: Rejected because target class is the same as current (TRF_SAME_CLASS)
INSERT INTO student_request (id, student_id, current_class_id, target_class_id, request_type, effective_date, effective_session_id, status, request_reason, submitted_by, submitted_at, decided_by, decided_at, note) VALUES
(14, 14, 2, 2, 'TRANSFER', '2025-11-03', 113, 'REJECTED', 'Accidentally selected current class while testing feature', 114, '2025-10-31 09:00:00+07', 6, '2025-10-31 15:00:00+07', 'Rejected - cannot transfer to the same class (TRF_SAME_CLASS)');

-- SCENARIO 6H: Tier violation - student tried to change modality via Tier 1
INSERT INTO student_request (id, student_id, current_class_id, target_class_id, request_type, effective_date, effective_session_id, status, request_reason, submitted_by, submitted_at, decided_by, decided_at, note) VALUES
(15, 11, 2, 3, 'TRANSFER', '2025-11-06', 214, 'REJECTED', 'Wants to switch to online but used Tier 1 flow', 111, '2025-11-01 07:00:00+07', 6, '2025-11-01 12:00:00+07', 'Rejected - Tier 1 only allows schedule changes (TRF_TIER_VIOLATION)');

-- SCENARIO 6I: Rejected because effective date is in the past
INSERT INTO student_request (id, student_id, current_class_id, target_class_id, request_type, effective_date, effective_session_id, status, request_reason, submitted_by, submitted_at, decided_by, decided_at, note) VALUES
(16, 10, 2, 3, 'TRANSFER', '2025-10-25', 209, 'REJECTED', 'Attempted to backdate transfer after missing classes', 110, '2025-11-02 11:00:00+07', 6, '2025-11-02 16:00:00+07', 'Rejected - effective date must be a future session (TRF_PAST_DATE)');

-- SCENARIO 6J: Approved HCM transfer executed (Class 5 -> Class 6) for Student 40
INSERT INTO student_request (id, student_id, current_class_id, target_class_id, request_type, effective_date, effective_session_id, status, request_reason, submitted_by, submitted_at, decided_by, decided_at, note) VALUES
(17, 40, 5, 6, 'TRANSFER', '2025-11-08', 512, 'APPROVED', 'Switching to evening cohort due to job relocation downtown', 8, '2025-11-05 11:00:00+07', 4, '2025-11-05 15:00:00+07', 'AA executed branch-internal transfer for HCM');

-- Execute HCM transfer: Update old enrollment for Student 40
UPDATE enrollment
SET status = 'TRANSFERRED',
    left_at = '2025-11-08 00:00:00+07',
    left_session_id = 312
WHERE class_id = 5 AND student_id = 40;

-- Execute HCM transfer: Create new enrollment in Class 6
INSERT INTO enrollment (id, class_id, student_id, status, enrolled_at, enrolled_by, join_session_id, created_at, updated_at)
VALUES (450, 6, 40, 'ENROLLED', '2025-11-08 00:00:00+07', 8, 512, '2025-11-08 00:00:00+07', '2025-11-08 00:00:00+07');

-- Execute HCM transfer: Mark future sessions in Class 5 as absent
UPDATE student_session
SET attendance_status = 'ABSENT',
    note = 'Transferred to class #6'
WHERE student_id = 40
  AND session_id IN (
      SELECT id FROM session WHERE class_id = 5 AND date >= '2025-11-08'
  );

-- Execute HCM transfer: Generate student_sessions in Class 6 for Student 40
INSERT INTO student_session (student_id, session_id, is_makeup, attendance_status, note, created_at, updated_at)
SELECT 40, s.id, false, 'PLANNED', 'Transferred from class #5', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM session s
WHERE s.class_id = 6 AND s.date >= '2025-11-08';

-- SCENARIO 7: Teacher Swap Request - Approved
INSERT INTO teacher_request (id, teacher_id, session_id, request_type, replacement_teacher_id, status, request_reason, submitted_by, submitted_at, decided_by, decided_at) VALUES
(1, 1, 115, 'SWAP', 3, 'APPROVED', 'Family emergency - cannot attend session', 20, '2025-10-28 08:00:00+07', 6, '2025-10-28 10:00:00+07');

-- Execute swap: Update teaching_slot
UPDATE teaching_slot 
SET teacher_id = 3, status = 'SUBSTITUTED'
WHERE session_id = 115 AND teacher_id = 1;

-- SCENARIO 8: Teacher Reschedule Request - Pending
INSERT INTO teacher_request (id, teacher_id, session_id, request_type, new_date, new_time_slot_id, new_resource_id, status, request_reason, submitted_by, submitted_at) VALUES
(2, 2, 215, 'RESCHEDULE', '2025-11-05', 5, 4, 'PENDING', 'Conference attendance - propose rescheduling to evening slot', 21, '2025-11-01 09:00:00+07');

-- SCENARIO 9: Teacher Modality Change Request - Approved
INSERT INTO teacher_request (id, teacher_id, session_id, request_type, new_resource_id, status, request_reason, submitted_by, submitted_at, decided_by, decided_at) VALUES
(3, 1, 117, 'MODALITY_CHANGE', 4, 'APPROVED', 'Room air conditioning broken - need to switch to online', 20, '2025-11-01 07:00:00+07', 6, '2025-11-01 08:00:00+07');

-- Execute modality change: Update session_resource
DELETE FROM session_resource WHERE session_id = 117;
INSERT INTO session_resource (session_id, resource_id) VALUES (117, 4);

-- SCENARIO 10: Request created by Academic Affair on behalf (waiting confirmation)
INSERT INTO student_request (id, student_id, current_class_id, request_type, target_session_id, status, request_reason, submitted_by, submitted_at, note) VALUES
(7, 6, 2, 'ABSENCE', 119, 'WAITING_CONFIRM', 'Student called to report illness - created on behalf', 6, '2025-11-01 13:00:00+07', 'Created by Academic Affair via phone call');

-- ========== TIER 7: ASSESSMENTS & SCORES ==========

-- Assessments for Class 2 (scheduled and completed)
INSERT INTO assessment (id, class_id, course_assessment_id, scheduled_date, actual_date, created_at, updated_at) VALUES
(1, 2, 1, '2025-10-18 08:00:00+07', '2025-10-18 08:00:00+07', '2025-10-18 08:00:00+07', '2025-10-18 08:00:00+07'), -- Listening Quiz 1 - completed
(2, 2, 2, '2025-10-21 08:00:00+07', '2025-10-21 08:00:00+07', '2025-10-21 08:00:00+07', '2025-10-21 08:00:00+07'), -- Speaking Quiz 1 - completed
(3, 2, 5, '2025-11-08 08:00:00+07', NULL, '2025-10-15 08:00:00+07', '2025-10-15 08:00:00+07'), -- Midterm - scheduled
(4, 2, 6, '2025-11-27 08:00:00+07', NULL, '2025-10-15 08:00:00+07', '2025-10-15 08:00:00+07'); -- Final - scheduled

-- Scores for completed assessments (Listening Quiz 1)
INSERT INTO score (id, assessment_id, student_id, score, feedback, graded_by, graded_at, created_at, updated_at) VALUES
(1, 1, 1, 18.0, 'Good listening skills', 1, '2025-10-19 10:00:00+07', '2025-10-19 10:00:00+07', '2025-10-19 10:00:00+07'),
(2, 1, 2, 16.5, 'Need more practice on numbers', 1, '2025-10-19 10:00:00+07', '2025-10-19 10:00:00+07', '2025-10-19 10:00:00+07'),
(3, 1, 3, 19.0, 'Excellent performance', 1, '2025-10-19 10:00:00+07', '2025-10-19 10:00:00+07', '2025-10-19 10:00:00+07'),
(4, 1, 4, 15.0, 'Satisfactory', 1, '2025-10-19 10:00:00+07', '2025-10-19 10:00:00+07', '2025-10-19 10:00:00+07'),
(5, 1, 5, 17.5, 'Good work', 1, '2025-10-19 10:00:00+07', '2025-10-19 10:00:00+07', '2025-10-19 10:00:00+07'),
(6, 1, 6, 14.0, 'Need improvement', 1, '2025-10-19 10:00:00+07', '2025-10-19 10:00:00+07', '2025-10-19 10:00:00+07'),
(7, 1, 7, 18.5, 'Very good', 1, '2025-10-19 10:00:00+07', '2025-10-19 10:00:00+07', '2025-10-19 10:00:00+07'),
(8, 1, 8, 16.0, 'Good progress', 1, '2025-10-19 10:00:00+07', '2025-10-19 10:00:00+07', '2025-10-19 10:00:00+07'),
(9, 1, 9, 17.0, 'Well done', 1, '2025-10-19 10:00:00+07', '2025-10-19 10:00:00+07', '2025-10-19 10:00:00+07'),
(10, 1, 10, 15.5, 'Fair performance', 1, '2025-10-19 10:00:00+07', '2025-10-19 10:00:00+07', '2025-10-19 10:00:00+07');

-- Scores for Speaking Quiz 1
INSERT INTO score (id, assessment_id, student_id, score, feedback, graded_by, graded_at, created_at, updated_at) VALUES
(11, 2, 1, 17.0, 'Good fluency, work on pronunciation', 1, '2025-10-22 10:00:00+07', '2025-10-22 10:00:00+07', '2025-10-22 10:00:00+07'),
(12, 2, 2, 18.0, 'Confident speaker', 1, '2025-10-22 10:00:00+07', '2025-10-22 10:00:00+07', '2025-10-22 10:00:00+07'),
(13, 2, 3, 16.5, 'Good effort', 1, '2025-10-22 10:00:00+07', '2025-10-22 10:00:00+07', '2025-10-22 10:00:00+07'),
(14, 2, 4, 15.0, 'Need more practice', 1, '2025-10-22 10:00:00+07', '2025-10-22 10:00:00+07', '2025-10-22 10:00:00+07'),
(15, 2, 5, 19.0, 'Excellent speaking skills', 1, '2025-10-22 10:00:00+07', '2025-10-22 10:00:00+07', '2025-10-22 10:00:00+07');

-- Assessments for Class 1 (COMPLETED)
INSERT INTO assessment (id, class_id, course_assessment_id, scheduled_date, actual_date, created_at, updated_at) VALUES
(10, 1, 1, '2025-07-21 08:00:00+07', '2025-07-21 08:00:00+07', '2025-07-21 08:00:00+07', '2025-07-21 08:00:00+07'), -- Listening Quiz 1
(11, 1, 2, '2025-07-28 08:00:00+07', '2025-07-28 08:00:00+07', '2025-07-28 08:00:00+07', '2025-07-28 08:00:00+07'), -- Speaking Quiz 1
(12, 1, 3, '2025-08-04 08:00:00+07', '2025-08-04 08:00:00+07', '2025-08-04 08:00:00+07', '2025-08-04 08:00:00+07'), -- Reading Quiz 1
(13, 1, 4, '2025-08-11 08:00:00+07', '2025-08-11 08:00:00+07', '2025-08-11 08:00:00+07', '2025-08-11 08:00:00+07'), -- Writing Assignment 1
(14, 1, 5, '2025-08-18 08:00:00+07', '2025-08-18 08:00:00+07', '2025-08-18 08:00:00+07', '2025-08-18 08:00:00+07'), -- Midterm Exam
(15, 1, 6, '2025-09-01 08:00:00+07', '2025-09-01 08:00:00+07', '2025-09-01 08:00:00+07', '2025-09-01 08:00:00+07'); -- Final Exam

-- Scores for Class 1, Midterm Exam (assessment_id = 14)
INSERT INTO score (assessment_id, student_id, score, feedback, graded_by, graded_at)
SELECT
    14,
    s.id,
    60 + floor(random() * 35)::int, -- Score between 60 and 94
    'Good overall performance on the midterm.',
    3, -- Graded by Teacher 3
    '2025-08-20 10:00:00+07'
FROM generate_series(1, 15) AS s(id);

-- Scores for Class 1, Final Exam (assessment_id = 15)
INSERT INTO score (assessment_id, student_id, score, feedback, graded_by, graded_at)
SELECT
    15,
    s.id,
    65 + floor(random() * 30)::int, -- Score between 65 and 94, showing improvement
    'Solid performance on the final exam. Well done.',
    3, -- Graded by Teacher 3
    '2025-09-03 10:00:00+07'
FROM generate_series(1, 15) AS s(id);

-- ========== TIER 8: FEEDBACK & QA ==========

-- Student Feedback for completed phase
INSERT INTO student_feedback (id, student_id, class_id, phase_id, is_feedback, submitted_at, created_at, updated_at) VALUES
(1, 1, 2, 1, true, '2025-10-25 20:00:00+07', '2025-10-25 20:00:00+07', '2025-10-25 20:00:00+07'),
(2, 2, 2, 1, true, '2025-10-25 21:00:00+07', '2025-10-25 21:00:00+07', '2025-10-25 21:00:00+07'),
(3, 3, 2, 1, true, '2025-10-26 18:00:00+07', '2025-10-26 18:00:00+07', '2025-10-26 18:00:00+07');

-- Student Feedback Responses
INSERT INTO student_feedback_response (id, feedback_id, question_id, rating, created_at, updated_at) VALUES
-- Student 1 responses
(1, 1, 1, 5, '2025-10-25 20:00:00+07', '2025-10-25 20:00:00+07'), -- Teaching quality: 5/5
(2, 1, 2, 4, '2025-10-25 20:00:00+07', '2025-10-25 20:00:00+07'), -- Lesson organization: 4/5
(3, 1, 3, 5, '2025-10-25 20:00:00+07', '2025-10-25 20:00:00+07'), -- Materials: 5/5
(4, 1, 4, 4, '2025-10-25 20:00:00+07', '2025-10-25 20:00:00+07'), -- Class management: 4/5
(5, 1, 5, 5, '2025-10-25 20:00:00+07', '2025-10-25 20:00:00+07'), -- Recommendation: 5/5
-- Student 2 responses
(6, 2, 1, 4, '2025-10-25 21:00:00+07', '2025-10-25 21:00:00+07'),
(7, 2, 2, 4, '2025-10-25 21:00:00+07', '2025-10-25 21:00:00+07'),
(8, 2, 3, 3, '2025-10-25 21:00:00+07', '2025-10-25 21:00:00+07'),
(9, 2, 4, 4, '2025-10-25 21:00:00+07', '2025-10-25 21:00:00+07'),
(10, 2, 5, 4, '2025-10-25 21:00:00+07', '2025-10-25 21:00:00+07'),
-- Student 3 responses
(11, 3, 1, 5, '2025-10-26 18:00:00+07', '2025-10-26 18:00:00+07'),
(12, 3, 2, 5, '2025-10-26 18:00:00+07', '2025-10-26 18:00:00+07'),
(13, 3, 3, 4, '2025-10-26 18:00:00+07', '2025-10-26 18:00:00+07'),
(14, 3, 4, 5, '2025-10-26 18:00:00+07', '2025-10-26 18:00:00+07'),
(15, 3, 5, 5, '2025-10-26 18:00:00+07', '2025-10-26 18:00:00+07');

-- QA Reports
INSERT INTO qa_report (id, class_id, session_id, reported_by, report_type, status, findings, action_items, created_at, updated_at) VALUES
(1, 2, 105, 10, 'classroom_observation', 'closed', 'Teacher demonstrated excellent engagement techniques. Students actively participated.', 'Share teaching approach with other teachers in next training session.', '2025-10-19 15:00:00+07', '2025-10-20 10:00:00+07'),
(2, 2, 110, 10, 'classroom_observation', 'open', 'Noticed some students struggling with listening exercises. Recommend additional practice materials.', 'Teacher to provide supplementary listening resources. Follow-up in 2 weeks.', '2025-10-30 14:00:00+07', '2025-10-30 14:00:00+07');

-- ========== EDGE CASES & BOUNDARY CONDITIONS ==========

-- EDGE CASE 1: Student with no absences (perfect attendance)
-- Student 7 in Class 2 - already has all "present" from earlier logic

-- EDGE CASE 2: Student with high absence rate
UPDATE student_session 
SET attendance_status = 'ABSENT', note = 'Frequent absence - needs monitoring'
WHERE student_id = 13 AND session_id IN (101, 103, 105, 107, 109);

-- EDGE CASE 3: Class at maximum capacity
UPDATE "class" SET max_capacity = 15 WHERE id = 2; -- Already has 15 enrolled (at capacity)

-- EDGE CASE 4: Student with NULL optional fields
UPDATE user_account SET dob = NULL, address = NULL WHERE id = 160;

-- EDGE CASE 5: Session with NULL teacher_note (not yet submitted)
-- Already handled - planned sessions don't have notes

-- EDGE CASE 6: Enrollment on exact start date
INSERT INTO enrollment (id, class_id, student_id, status, enrolled_at, enrolled_by, join_session_id, created_at, updated_at)
VALUES (50, 2, 19, 'ENROLLED', '2025-10-06 00:00:00+07', 6, 101, '2025-10-06 00:00:00+07', '2025-10-06 00:00:00+07');

INSERT INTO student_session (student_id, session_id, is_makeup, attendance_status, created_at, updated_at)
SELECT 19, s.id, false,
    CASE WHEN s.status = 'DONE' THEN 'PRESENT' ELSE 'PLANNED' END,
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM session s
WHERE s.class_id = 2;

-- EDGE CASE 7: Future class with no enrollments yet
-- Class 4 - already defined as scheduled, no enrollments

-- ========== FINAL SEQUENCE UPDATES ==========
SELECT setval('center_id_seq', (SELECT MAX(id) FROM center), true);
SELECT setval('branch_id_seq', (SELECT MAX(id) FROM branch), true);
SELECT setval('role_id_seq', (SELECT MAX(id) FROM role), true);
SELECT setval('user_account_id_seq', (SELECT MAX(id) FROM user_account), true);
SELECT setval('teacher_id_seq', (SELECT MAX(id) FROM teacher), true);
SELECT setval('student_id_seq', (SELECT MAX(id) FROM student), true);
SELECT setval('subject_id_seq', (SELECT MAX(id) FROM subject), true);
SELECT setval('level_id_seq', (SELECT MAX(id) FROM level), true);
SELECT setval('plo_id_seq', (SELECT MAX(id) FROM plo), true);
SELECT setval('course_id_seq', (SELECT MAX(id) FROM course), true);
SELECT setval('course_phase_id_seq', (SELECT MAX(id) FROM course_phase), true);
SELECT setval('clo_id_seq', (SELECT MAX(id) FROM clo), true);
SELECT setval('course_session_id_seq', (SELECT MAX(id) FROM course_session), true);
SELECT setval('course_assessment_id_seq', (SELECT MAX(id) FROM course_assessment), true);
SELECT setval('class_id_seq', (SELECT MAX(id) FROM "class"), true);
SELECT setval('session_id_seq', (SELECT MAX(id) FROM session), true);
SELECT setval('assessment_id_seq', (SELECT MAX(id) FROM assessment), true);
SELECT setval('score_id_seq', (SELECT MAX(id) FROM score), true);
SELECT setval('student_request_id_seq', (SELECT MAX(id) FROM student_request), true);
SELECT setval('teacher_request_id_seq', (SELECT MAX(id) FROM teacher_request), true);
SELECT setval('student_feedback_id_seq', (SELECT MAX(id) FROM student_feedback), true);
SELECT setval('student_feedback_response_id_seq', (SELECT MAX(id) FROM student_feedback_response), true);
SELECT setval('qa_report_id_seq', (SELECT MAX(id) FROM qa_report), true);
SELECT setval('time_slot_template_id_seq', (SELECT MAX(id) FROM time_slot_template), true);
SELECT setval('resource_id_seq', (SELECT MAX(id) FROM resource), true);
SELECT setval('feedback_question_id_seq', (SELECT MAX(id) FROM feedback_question), true);
SELECT setval('enrollment_id_seq', (SELECT MAX(id) FROM enrollment), true);

-- ========== VERIFICATION QUERIES ==========
-- Uncomment to verify data integrity

-- SELECT 'Total Users' as metric, COUNT(*) as count FROM user_account
-- UNION ALL SELECT 'Total Teachers', COUNT(*) FROM teacher
-- UNION ALL SELECT 'Total Students', COUNT(*) FROM student
-- UNION ALL SELECT 'Total Classes', COUNT(*) FROM "class"
-- UNION ALL SELECT 'Total Sessions', COUNT(*) FROM session
-- UNION ALL SELECT 'Total Enrollments', COUNT(*) FROM enrollment
-- UNION ALL SELECT 'Total Student Sessions', COUNT(*) FROM student_session
-- UNION ALL SELECT 'Total Requests (Student)', COUNT(*) FROM student_request
-- UNION ALL SELECT 'Total Requests (Teacher)', COUNT(*) FROM teacher_request
-- UNION ALL SELECT 'Done Sessions', COUNT(*) FROM session WHERE status = 'DONE'
-- UNION ALL SELECT 'Planned Sessions', COUNT(*) FROM session WHERE status = 'PLANNED'
-- UNION ALL SELECT 'Attendance Records (Present)', COUNT(*) FROM student_session WHERE attendance_status = 'PRESENT'
-- UNION ALL SELECT 'Attendance Records (Absent)', COUNT(*) FROM student_session WHERE attendance_status = 'ABSENT';

-- =========================================
-- END OF SEED DATA
-- =========================================
-- Summary:
-- - 2 Branches (HN, HCM)
-- - 16 Teachers with skills
-- - 60 Students
-- - 1 Complete Course (Foundation) with 24 sessions, CLOs, assessments
-- - 5 Classes (completed, ongoing, scheduled)
-- - Full session generation for 2 main classes
-- - 16 Enrollments with complete student_sessions
-- - 10 Request scenarios (approved, pending, rejected, waiting_confirm)
-- - Assessments with scores
-- - Student feedback and QA reports
-- - Edge cases: capacity limits, mid-course enrollment, transfers, perfect attendance
-- =========================================
