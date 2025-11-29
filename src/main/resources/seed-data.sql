-- =========================================
-- Anh ngữ Pinnacle - SEP490-BE: COMPREHENSIVE SEED DATA FOR TESTING
-- =========================================
-- Author: QA Team
-- Date: 2025-11-28
-- Purpose: High-quality, logically consistent dataset covering all business flows and edge cases
-- Reference Date: 2025-11-28 (today's date for testing)
-- =========================================
-- COVERAGE:
-- - Happy paths: Course creation → Class → Enrollment → Attendance → Requests
-- - Edge cases: Mid-course enrollment, cross-class makeup, transfers, teacher replacements
-- - Boundary conditions: Capacity limits, date ranges, status transitions
-- =========================================

-- ========== SECTION 0: CLEANUP & RESET ==========
-- Clean existing data in reverse dependency order
TRUNCATE TABLE student_feedback_response CASCADE;
TRUNCATE TABLE student_feedback CASCADE;
TRUNCATE TABLE qa_report CASCADE;
TRUNCATE TABLE policy_history CASCADE;
TRUNCATE TABLE system_policy CASCADE;
TRUNCATE TABLE score CASCADE;
TRUNCATE TABLE assessment CASCADE;
TRUNCATE TABLE course_assessment_clo_mapping CASCADE;
TRUNCATE TABLE course_assessment CASCADE;
TRUNCATE TABLE notification CASCADE;
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
SELECT setval('course_id_seq', 10, false);
SELECT setval('course_phase_id_seq', 23, false);
SELECT setval('clo_id_seq', 29, false);
SELECT setval('course_session_id_seq', 1, false);
SELECT setval('course_assessment_id_seq', 1, false);
SELECT setval('class_id_seq', 31, false);
SELECT setval('session_id_seq', 1, false);
SELECT setval('assessment_id_seq', 1, false);
SELECT setval('score_id_seq', 1, false);
SELECT setval('student_request_id_seq', 1, false);
SELECT setval('teacher_request_id_seq', 1, false);
SELECT setval('student_feedback_id_seq', 1, false);
SELECT setval('student_feedback_response_id_seq', 1, false);
SELECT setval('qa_report_id_seq', 1, false);
SELECT setval('course_material_id_seq', 1, false);
SELECT setval('time_slot_template_id_seq', 16, false);
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
-- Test user for forgot password functionality (using Resend verified email)
(12, 'cccccc9712@gmail.com', '0912345678', 'Truong Manh Thang', 'MALE', '1997-12-31', 'Ha Noi', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-01-01 00:00:00+07', '2024-01-01 00:00:00+07'),

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

-- Students (100 students total: 50 per branch for realistic testing)
INSERT INTO user_account (id, email, phone, full_name, gender, dob, address, password_hash, status, created_at, updated_at) 
SELECT 
    100 + s.id, 
    'student.' || LPAD(s.id::text, 4, '0') || '@gmail.com', 
    '0900' || LPAD(s.id::text, 6, '0'),
    'Student ' || LPAD(s.id::text, 4, '0'),
    CASE WHEN s.id % 2 = 0 THEN  'FEMALE' ELSE  'MALE' END, 
    make_date(2000 + (s.id % 6), (s.id % 12) + 1, (s.id % 28) + 1),
    CASE WHEN s.id <= 50 THEN 'Ha Noi' ELSE 'TP. HCM' END,
    '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.',
    'ACTIVE',
    '2024-03-01 00:00:00+07',
    '2024-03-01 00:00:00+07'
FROM generate_series(1, 100) AS s(id);

-- Feedback Questions (for student feedback feature)
INSERT INTO feedback_question (id, question_text, question_type, options, display_order, created_at, updated_at) VALUES
(1, 'Mức hài lòng với chất lượng giảng dạy tổng thể', 'rating', NULL, 1, '2024-01-01 00:00:00+07', '2024-01-01 00:00:00+07'),
(2, 'Bài giảng có rõ ràng và mạch lạc không?', 'rating', NULL, 2, '2024-01-01 00:00:00+07', '2024-01-01 00:00:00+07'),
(3, 'Tài liệu và nguồn lực hỗ trợ học tập hữu ích ở mức nào?', 'rating', NULL, 3, '2024-01-01 00:00:00+07', '2024-01-01 00:00:00+07'),
(4, 'Lịch học và quản lý lớp có hiệu quả không?', 'rating', NULL, 4, '2024-01-01 00:00:00+07', '2024-01-01 00:00:00+07'),
(5, 'Bạn có sẵn sàng giới thiệu khóa học này cho người khác?', 'rating', NULL, 5, '2024-01-01 00:00:00+07', '2024-01-01 00:00:00+07'),
(6, 'Điều bạn hài lòng nhất ở phase này là gì?', 'text', NULL, 6, '2024-01-01 00:00:00+07', '2024-01-01 00:00:00+07'),
(7, 'Bạn muốn cải thiện điều gì cho phase tiếp theo?', 'text', NULL, 7, '2024-01-01 00:00:00+07', '2024-01-01 00:00:00+07');

-- ========== TIER 2: DEPENDENT ON TIER 1 ==========

-- Branches
INSERT INTO branch (id, center_id, code, name, address, phone, email, district, city, status, opening_date, created_at, updated_at) VALUES
(1, 1, 'HN01', 'TMS Ha Noi Branch', '456 Lang Ha, Dong Da, Ha Noi', '+84-24-3888-9999', 'hanoi01@tms-edu.vn', 'Dong Da', 'Ha Noi', 'ACTIVE', '2024-01-15', '2024-01-15 00:00:00+07', '2024-01-15 00:00:00+07'),
(2, 1, 'HCM01', 'TMS Ho Chi Minh Branch', '789 Le Loi, Quan 1, TP. HCM', '+84-28-3777-6666', 'hcm01@tms-edu.vn', 'Quan 1', 'TP. HCM', 'ACTIVE', '2024-03-01', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07');

-- Subject
INSERT INTO subject (id, code, name, description, status, created_by, created_at, updated_at) VALUES
(1, 'IELTS', 'International English Language Testing System', 'Comprehensive IELTS preparation courses', 'ACTIVE', 5, '2024-06-01 00:00:00+07', '2024-06-01 00:00:00+07');

-- Time Slot Templates - REALISTIC SCHEDULES FOR LANGUAGE CENTER
INSERT INTO time_slot_template (id, branch_id, name, start_time, end_time, created_at, updated_at) VALUES
-- Ha Noi Branch - Comprehensive Time Slots
-- Morning Slots
(1, 1, 'HN Early Morning 1.5h', '07:00:00', '08:30:00', '2024-01-15 00:00:00+07', '2024-01-15 00:00:00+07'),
(2, 1, 'HN Morning Standard 2.5h', '09:00:00', '11:30:00', '2024-01-15 00:00:00+07', '2024-01-15 00:00:00+07'),
-- Afternoon Slots
(3, 1, 'HN Afternoon Standard 2.5h', '14:00:00', '16:30:00', '2024-01-15 00:00:00+07', '2024-01-15 00:00:00+07'),
(4, 1, 'HN Late Afternoon 2h', '16:00:00', '18:00:00', '2024-01-15 00:00:00+07', '2024-01-15 00:00:00+07'),
-- Evening Slots
(5, 1, 'HN Evening Standard 2.5h', '18:30:00', '21:00:00', '2024-01-15 00:00:00+07', '2024-01-15 00:00:00+07'),
(6, 1, 'HN Evening Late 2.5h', '19:00:00', '21:30:00', '2024-01-15 00:00:00+07', '2024-01-15 00:00:00+07'),
-- Weekend Special Slots
(7, 1, 'HN Weekend Morning 2.5h', '08:30:00', '11:00:00', '2024-01-15 00:00:00+07', '2024-01-15 00:00:00+07'),
(8, 1, 'HN Weekend Afternoon 2.5h', '14:00:00', '16:30:00', '2024-01-15 00:00:00+07', '2024-01-15 00:00:00+07'),

-- Ho Chi Minh Branch - Same Structure
-- Morning Slots
(9, 2, 'HCM Early Morning 1.5h', '07:00:00', '08:30:00', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(10, 2, 'HCM Morning Standard 2.5h', '09:00:00', '11:30:00', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
-- Afternoon Slots
(11, 2, 'HCM Afternoon Standard 2.5h', '14:00:00', '16:30:00', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(12, 2, 'HCM Late Afternoon 2h', '16:00:00', '18:00:00', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
-- Evening Slots
(13, 2, 'HCM Evening Standard 2.5h', '18:30:00', '21:00:00', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(14, 2, 'HCM Evening Late 2.5h', '19:00:00', '21:30:00', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
-- Weekend Special Slots
(15, 2, 'HCM Weekend Morning 2.5h', '08:30:00', '11:00:00', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(16, 2, 'HCM Weekend Afternoon 2.5h', '14:00:00', '16:30:00', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07');

-- Resources (Rooms & Zoom)
INSERT INTO resource (id, branch_id, resource_type, code, name, capacity, capacity_override, created_at, updated_at) VALUES
-- Ha Noi Branch - Physical Rooms
(1, 1, 'ROOM', 'HN01-R101', 'Ha Noi Room 101', 20, 25, '2024-01-15 00:00:00+07', '2024-01-15 00:00:00+07'),
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
INSERT INTO user_branches (user_id, branch_id, assigned_by) SELECT id, 1, 6 FROM user_account WHERE id BETWEEN 101 AND 150;
-- Students - HCM
INSERT INTO user_branches (user_id, branch_id, assigned_by) SELECT id, 2, 8 FROM user_account WHERE id > 150;

-- Teachers & Students
INSERT INTO teacher (id, user_account_id, employee_code, hire_date, contract_type, created_at, updated_at)
SELECT (id - 19), id, 'TCH-' || LPAD((id-19)::text, 3, '0'), '2024-02-01', CASE WHEN id % 3 = 0 THEN 'part-time' ELSE 'full-time' END, '2024-02-01 00:00:00+07', '2024-02-01 00:00:00+07'
FROM user_account WHERE id BETWEEN 20 AND 35;

-- Test student for forgot password functionality
INSERT INTO student (id, user_id, student_code, created_at, updated_at) VALUES
(999, 12, 'TEST-0012', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07');

-- Add user role for test student (STUDENT role = 7)
INSERT INTO user_role (user_id, role_id) VALUES (12, 7);

-- Add user branch for test student (HN branch = 1)
INSERT INTO user_branches (user_id, branch_id, assigned_by) VALUES (12, 1, 1);

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
(3, 1, 'ADVANCED', 'IELTS Advanced (6.5-8.0)', 90, 3, '2024-06-01 00:00:00+07', '2024-06-01 00:00:00+07'),
(4, 1, 'DUMMY4', 'Dummy Level 4', 0, 4, '2024-06-01 00:00:00+07', '2024-06-01 00:00:00+07'),
(5, 1, 'DUMMY5', 'Dummy Level 5 (TOEIC/JLPT)', 0, 5, '2024-06-01 00:00:00+07', '2024-06-01 00:00:00+07'),
(6, 1, 'DUMMY6', 'Dummy Level 6', 0, 6, '2024-06-01 00:00:00+07', '2024-06-01 00:00:00+07'),
(7, 1, 'DUMMY7', 'Dummy Level 7', 0, 7, '2024-06-01 00:00:00+07', '2024-06-01 00:00:00+07'),
(8, 1, 'DUMMY8', 'Dummy Level 8', 0, 8, '2024-06-01 00:00:00+07', '2024-06-01 00:00:00+07'),
(9, 1, 'DUMMY9', 'Dummy Level 9', 0, 9, '2024-06-01 00:00:00+07', '2024-06-01 00:00:00+07'),
(10, 1, 'DUMMY10', 'Dummy Level 10 (JLPT N5)', 0, 10, '2024-06-01 00:00:00+07', '2024-06-01 00:00:00+07'),
(11, 1, 'TOEIC-500', 'TOEIC 500+', 60, 5, '2024-06-01 00:00:00+07', '2024-06-01 00:00:00+07'),
(12, 1, 'JLPT-N5', 'JLPT N5', 60, 10, '2024-06-01 00:00:00+07', '2024-06-01 00:00:00+07');

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
FROM generate_series(2, 60) AS s(id);

-- JLPT Sample Assessments (for students 1-5 as examples)
-- Student 1: JLPT N5 Placement Test
INSERT INTO replacement_skill_assessment (student_id, skill, level_id, raw_score, scaled_score, score_scale, assessment_category, assessment_date, assessment_type, assessed_by, note, created_at, updated_at) VALUES
(1, 'GENERAL', 10, 45, 75.0, '0-100', 'PLACEMENT', '2025-06-15', 'jlpt_n5_placement', 6, 'JLPT N5 Vocabulary Assessment', '2025-06-15 00:00:00+07', '2025-06-15 00:00:00+07'),
(1, 'GENERAL', 10, 38, 63.3, '0-100', 'PRACTICE', '2025-06-16', 'jlpt_n5_placement', 6, 'JLPT N5 Grammar Assessment', '2025-06-16 00:00:00+07', '2025-06-16 00:00:00+07'),
(1, 'GENERAL', 10, 50, 83.3, '0-100', 'MOCK', '2025-06-17', 'jlpt_n5_placement', 6, 'JLPT N5 Kanji Assessment', '2025-06-17 00:00:00+07', '2025-06-17 00:00:00+07'),
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

-- =========================================
-- COMPREHENSIVE COURSE OFFERINGS - REALISTIC LANGUAGE CENTER STRUCTURE
-- =========================================

-- IELTS Series - Complete Learning Path
INSERT INTO course (id, subject_id, level_id, logical_course_code, version, code, name, description, total_hours, hours_per_session, prerequisites, target_audience, teaching_methods, score_scale, status, approval_status, decided_by_manager, decided_at, rejection_reason, created_by, created_at, updated_at) VALUES
(1, 1, 1, 'IELTS-FOUND-2025', 1, 'IELTS-FOUND-2025-V1', 'IELTS Foundation 2025', 'Khóa học nền tảng cho người mới bắt đầu, mục tiêu band 3.0-4.0', 60, 2.5, 'Không yêu cầu kiến thức nền tảng. Phù hợp cho người mới bắt đầu.', 'Học viên mất gốc hoặc mới bắt đầu học tiếng Anh, mong muốn đạt band 3.0-4.0 IELTS.', 'Phương pháp Communicative Language Teaching (CLT) kết hợp bài tập thực hành tương tác.', '0-9', 'ACTIVE', 'APPROVED', 2, '2024-08-20 14:00:00+07', NULL, 5, '2024-08-15 00:00:00+07', '2024-08-20 14:00:00+07'),
(2, 1, 2, 'IELTS-INT-2025', 1, 'IELTS-INT-2025-V1', 'IELTS Intermediate 2025', 'Khóa học trung cấp, mục tiêu band 5.0-5.5', 75, 2.5, 'Đã hoàn thành khóa Foundation hoặc có trình độ tương đương IELTS 4.0.', 'Học viên đã có nền tảng cơ bản, mục tiêu đạt band 5.0-5.5.', 'Tập trung vào kỹ năng làm bài thi, chiến thuật giải đề và nâng cao từ vựng học thuật.', '0-9', 'ACTIVE', 'APPROVED', 2, '2024-08-25 14:00:00+07', NULL, 5, '2024-08-15 00:00:00+07', '2024-08-25 14:00:00+07'),
(3, 1, 3, 'IELTS-ADV-2025', 1, 'IELTS-ADV-2025-V1', 'IELTS Advanced 2025', 'Khóa học nâng cao, mục tiêu band 6.5 trở lên với luyện đề chuyên sâu.', 100, 2.5, 'Hoàn thành khóa Intermediate hoặc tương đương IELTS 5.5.', 'Học viên muốn đạt band 6.5+ để du học hoặc làm việc.', 'Luyện đề cường độ cao, phản hồi cá nhân hóa và workshop kỹ năng.', '0-9', 'ACTIVE', 'APPROVED', 2, '2024-08-25 14:00:00+07', NULL, 5, '2024-08-20 00:00:00+07', '2024-08-25 14:00:00+07'),

-- TOEIC Series - Business English Focus
(4, 1, 2, 'TOEIC-INT-2025', 1, 'TOEIC-INT-2025-V1', 'TOEIC Intermediate 2025', 'Chuẩn bị thi TOEIC mục tiêu 500-650+', 50, 2.5, 'Nền tảng tiếng Anh cơ bản, tương đương IELTS 4.0.', 'Người đi làm muốn nâng cao kỹ năng tiếng Anh công sở.', 'Tập trung vào từ vựng kinh doanh, ngữ pháp thực tế và chiến thuật làm bài.', '0-990', 'ACTIVE', 'APPROVED', 2, '2024-09-01 14:00:00+07', NULL, 5, '2024-08-20 00:00:00+07', '2024-09-01 14:00:00+07'),
(5, 1, 3, 'TOEIC-ADV-2025', 1, 'TOEIC-ADV-2025-V1', 'TOEIC Advanced 2025', 'Luyện thi TOEIC mục tiêu 750+ cho vị trí quản lý.', 60, 2.5, 'Đã hoàn thành TOEIC Intermediate hoặc đạt 500+', 'Quản lý cấp trung và cấp cao muốn chứng minh năng lực tiếng Anh.', 'Case study thực tế, presentation skills và business negotiation.', '0-990', 'ACTIVE', 'APPROVED', 2, '2024-09-05 14:00:00+07', NULL, 5, '2024-08-20 00:00:00+07', '2024-09-05 14:00:00+07'),

-- Business English - Professional Communication
(6, 1, 2, 'BUS-ENG-2025', 1, 'BUS-ENG-2025-V1', 'Business English Professional 2025', 'Kỹ năng giao tiếp chuyên nghiệp trong môi trường công sở quốc tế.', 45, 2.5, 'Tiếng Anh giao tiếp cơ bản (IELTS 4.0+).', 'Nhân viên văn phòng, chuyên viên muốn làm việc trong công ty đa quốc gia.', 'Role-playing, case study và simulated business meetings.', 'N/A', 'ACTIVE', 'APPROVED', 2, '2024-09-10 14:00:00+07', NULL, 5, '2024-08-20 00:00:00+07', '2024-09-10 14:00:00+07'),

-- Conversation & Speaking Skills
(7, 1, 2, 'CONV-FLUENT-2025', 1, 'CONV-FLUENT-2025-V1', 'English Conversation - Fluency Building 2025', 'Tăng cường lưu loát giao tiếp trong đời sống hàng ngày.', 30, 2.0, 'Phát âm cơ bản, có thể giao tiếp đơn giản.', 'Học viên muốn cải thiện sự tự tin và lưu loát khi nói.', 'Discussions, debates và real-life conversation practice.', 'N/A', 'ACTIVE', 'APPROVED', 2, '2024-09-15 14:00:00+07', NULL, 5, '2024-08-20 00:00:00+07', '2024-09-15 14:00:00+07'),

-- Skill-Specific Courses
(8, 1, 2, 'PRON-WORK-2025', 1, 'PRON-WORK-2025-V1', 'Pronunciation Workshop 2025', 'Khắc phục lỗi phát âm và luyện nói chuẩn giọng Anh-Mỹ.', 20, 2.0, 'Không yêu cầu điều kiện tiên quyết.', 'Học viên mọi trình độ muốn cải thiện phát âm.', 'Phonics practice, minimal pairs và shadow recording techniques.', 'N/A', 'ACTIVE', 'APPROVED', 2, '2024-09-20 14:00:00+07', NULL, 5, '2024-08-20 00:00:00+07', '2024-09-20 14:00:00+07'),
(9, 1, 2, 'WRITING-ACAD-2025', 1, 'WRITING-ACAD-2025-V1', 'Academic Writing Workshop 2025', 'Nâng cao kỹ năng viết học thuật và luận văn.', 25, 2.5, 'IELTS 5.0+ hoặc tương đương.', 'Sinh viên, học viên cao học muốn chuẩn bị du học.', 'Essay structure, academic vocabulary và citation styles.', 'N/A', 'SUBMITTED', 'PENDING', NULL, NULL, NULL, 5, '2024-09-25 00:00:00+07', '2024-09-25 00:00:00+07'),

-- Intensive Programs
(10, 1, 2, 'IELTS-INTENSIVE-2025', 1, 'IELTS-INTENSIVE-2025-V1', 'IELTS Intensive Preparation 2025', 'Luyện thi IELTS cường độ cao trong 8 tuần.', 80, 4.0, 'IELTS 5.0+ hoặc tương đương.', 'Học viên cần đạt band 6.5+ trong thời gian ngắn.', 'Daily practice tests, intensive feedback và crash course techniques.', '0-9', 'SUBMITTED', 'PENDING', NULL, NULL, NULL, 5, '2024-10-01 00:00:00+07', '2024-10-01 00:00:00+07');
-- Course Phases for Foundation
INSERT INTO course_phase (id, course_id, phase_number, name, duration_weeks, created_at, updated_at) VALUES
(1, 1, 1, 'Foundation Basics', 4, '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(2, 1, 2, 'Foundation Practice', 4, '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07');

-- Course Sessions for Foundation (24 sessions = 8 weeks × 3 sessions/week)
INSERT INTO course_session (id, phase_id, sequence_no, topic, student_task, skill, created_at, updated_at) VALUES
-- Phase 1: Foundation Basics (Sessions 1-12)
(1, 1, 1, 'Introduction to IELTS & Basic Listening', 'Listen to simple dialogues', 'GENERAL', '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(2, 1, 2, 'Basic Speaking: Greetings and Introductions', 'Practice self-introduction', 'SPEAKING', '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(3, 1, 3, 'Basic Reading: Short Passages', 'Read and answer simple questions', 'READING', '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(4, 1, 4, 'Basic Writing: Simple Sentences', 'Write about yourself', 'WRITING', '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(5, 1, 5, 'Listening: Numbers and Dates', 'Complete listening exercises', 'LISTENING', '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(6, 1, 6, 'Speaking: Daily Activities', 'Describe your daily routine', 'SPEAKING', '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(7, 1, 7, 'Reading: Understanding Main Ideas', 'Identify main ideas', 'READING', '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(8, 1, 8, 'Writing: Simple Paragraphs', 'Write a short paragraph', 'WRITING', '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(9, 1, 9, 'Listening: Conversations', 'Listen to basic conversations', 'LISTENING', '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(10, 1, 10, 'Speaking: Expressing Likes and Dislikes', 'Talk about preferences', 'SPEAKING', '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(11, 1, 11, 'Reading: Details and Facts', 'Find specific information', 'READING', '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(12, 1, 12, 'Writing: Connecting Ideas', 'Use simple connectors', 'WRITING', '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
-- Phase 2: Foundation Practice (Sessions 13-24)
(13, 2, 1, 'Listening: Following Instructions', 'Complete tasks from audio', 'LISTENING', '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(14, 2, 2, 'Speaking: Asking Questions', 'Practice question forms', 'SPEAKING', '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(15, 2, 3, 'Reading: Short Stories', 'Read and summarize', 'READING', '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(16, 2, 4, 'Writing: Describing People and Places', 'Write descriptions', 'WRITING', '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(17, 2, 5, 'Listening: News and Announcements', 'Understand main points', 'LISTENING', '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(18, 2, 6, 'Speaking: Giving Opinions', 'Express simple opinions', 'SPEAKING', '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(19, 2, 7, 'Reading: Understanding Context', 'Use context clues', 'READING', '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(20, 2, 8, 'Writing: Personal Letters', 'Write informal letters', 'WRITING', '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(21, 2, 9, 'Practice Test: Listening & Reading', 'Complete practice test', 'GENERAL', '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(22, 2, 10, 'Practice Test: Writing & Speaking', 'Complete practice test', 'GENERAL', '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(23, 2, 11, 'Review and Feedback', 'Review all skills', 'GENERAL', '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(24, 2, 12, 'Final Assessment', 'Complete final test', 'GENERAL', '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07');



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

-- =========================================
-- COMPREHENSIVE COURSE PHASES FOR ALL COURSES
-- =========================================

-- Course Phases for IELTS Intermediate (Course 2)
INSERT INTO course_phase (id, course_id, phase_number, name, duration_weeks, created_at, updated_at) VALUES
-- Extending from previous: IDs 3-6 for Intermediate
(3, 2, 1, 'Skill Building', 5, '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(4, 2, 2, 'Test Strategies', 5, '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(5, 2, 3, 'Mock Tests & Review', 3, '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07');

-- Course Phases for IELTS Advanced (Course 3)
INSERT INTO course_phase (id, course_id, phase_number, name, duration_weeks, created_at, updated_at) VALUES
-- IDs 7-10 for Advanced
(7, 3, 1, 'Advanced Techniques', 6, '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(8, 3, 2, 'Intensive Practice', 6, '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(9, 3, 3, 'Test Mastery', 4, '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(10, 3, 4, 'Final Preparation', 2, '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07');

-- Course Phases for TOEIC Intermediate (Course 4)
INSERT INTO course_phase (id, course_id, phase_number, name, duration_weeks, created_at, updated_at) VALUES
-- IDs 11-12 for TOEIC Intermediate
(11, 4, 1, 'Business Vocabulary & Grammar', 4, '2024-09-01 00:00:00+07', '2024-09-01 00:00:00+07'),
(12, 4, 2, 'Test Techniques & Practice', 4, '2024-09-01 00:00:00+07', '2024-09-01 00:00:00+07');

-- Course Phases for TOEIC Advanced (Course 5)
INSERT INTO course_phase (id, course_id, phase_number, name, duration_weeks, created_at, updated_at) VALUES
-- IDs 13-14 for TOEIC Advanced
(13, 5, 1, 'Advanced Business Communication', 4, '2024-09-05 00:00:00+07', '2024-09-05 00:00:00+07'),
(14, 5, 2, 'Executive Test Preparation', 4, '2024-09-05 00:00:00+07', '2024-09-05 00:00:00+07');

-- Course Phases for Business English (Course 6)
INSERT INTO course_phase (id, course_id, phase_number, name, duration_weeks, created_at, updated_at) VALUES
-- IDs 15-17 for Business English
(15, 6, 1, 'Workplace Communication', 3, '2024-09-10 00:00:00+07', '2024-09-10 00:00:00+07'),
(16, 6, 2, 'Business Meetings & Presentations', 3, '2024-09-10 00:00:00+07', '2024-09-10 00:00:00+07'),
(17, 6, 3, 'Professional Writing', 3, '2024-09-10 00:00:00+07', '2024-09-10 00:00:00+07');

-- Course Phases for Conversation (Course 7)
INSERT INTO course_phase (id, course_id, phase_number, name, duration_weeks, created_at, updated_at) VALUES
-- IDs 18-19 for Conversation
(18, 7, 1, 'Confidence Building', 4, '2024-09-15 00:00:00+07', '2024-09-15 00:00:00+07'),
(19, 7, 2, 'Fluency Practice', 4, '2024-09-15 00:00:00+07', '2024-09-15 00:00:00+07');

-- Course Phases for Pronunciation Workshop (Course 8)
INSERT INTO course_phase (id, course_id, phase_number, name, duration_weeks, created_at, updated_at) VALUES
-- ID 20 for Pronunciation
(20, 8, 1, 'Sound System & Practice', 5, '2024-09-20 00:00:00+07', '2024-09-20 00:00:00+07');

-- Course Phases for Academic Writing (Course 9)
INSERT INTO course_phase (id, course_id, phase_number, name, duration_weeks, created_at, updated_at) VALUES
-- ID 21 for Academic Writing
(21, 9, 1, 'Academic Writing Fundamentals', 5, '2024-09-25 00:00:00+07', '2024-09-25 00:00:00+07');

-- Course Phases for IELTS Intensive (Course 10)
INSERT INTO course_phase (id, course_id, phase_number, name, duration_weeks, created_at, updated_at) VALUES
-- IDs 22-23 for IELTS Intensive
(22, 10, 1, 'Crash Course Foundation', 4, '2024-10-01 00:00:00+07', '2024-10-01 00:00:00+07'),
(23, 10, 2, 'High-Intensity Practice', 4, '2024-10-01 00:00:00+07', '2024-10-01 00:00:00+07');

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
(1, NULL, NULL, 'IELTS Foundation - Course Syllabus', 'The complete syllabus for the course.', 'DOCUMENT', '/materials/courses/1/syllabus.pdf', 5),
(1, NULL, NULL, 'Introductory Video', 'A welcome video from the head teacher.', 'MEDIA', '/materials/courses/1/intro.mp4', 5),
-- Phase 1 materials
(1, 1, NULL, 'Phase 1 Vocabulary List', 'Key vocabulary for the first 4 weeks.', 'DOCUMENT', '/materials/phases/1/vocab.docx', 5),
-- Session-specific materials
-- Session 1
(1, 1, 1, 'Introduction to IELTS Slides', 'Overview of the IELTS exam structure.', 'DOCUMENT', '/materials/sessions/1/intro-slides.pptx', 5),
(1, 1, 1, 'Basic Listening Audio', 'Audio tracks for basic listening exercises.', 'MEDIA', '/materials/sessions/1/audio.mp3', 5),
-- Session 2
(1, 1, 2, 'Greetings & Introductions Vocabulary', 'List of common phrases for introductions.', 'DOCUMENT', '/materials/sessions/2/vocab.pdf', 5),
-- Session 3
(1, 1, 3, 'Reading Passage: Daily Life', 'Simple reading text about daily routines.', 'DOCUMENT', '/materials/sessions/3/reading.pdf', 5),
-- Session 4
(1, 1, 4, 'Sentence Structure Guide', 'Basics of English sentence structure.', 'DOCUMENT', '/materials/sessions/4/grammar.pdf', 5),
-- Session 5
(1, 1, 5, 'Numbers & Dates Audio', 'Listening practice for numbers and dates.', 'MEDIA', '/materials/sessions/5/numbers.mp3', 5),
-- Session 6
(1, 1, 6, 'Daily Activities Worksheet', 'Exercises for describing daily routines.', 'DOCUMENT', '/materials/sessions/6/worksheet.docx', 5),
-- Session 7
(1, 1, 7, 'Main Idea Identification', 'Strategies for finding the main idea.', 'DOCUMENT', '/materials/sessions/7/strategies.pptx', 5),
-- Session 8
(1, 1, 8, 'Paragraph Writing Template', 'Template for writing simple paragraphs.', 'DOCUMENT', '/materials/sessions/8/template.docx', 5),
-- Session 9
(1, 1, 9, 'Conversation Practice Audio', 'Dialogues for listening practice.', 'MEDIA', '/materials/sessions/9/conversations.mp3', 5),
-- Session 10
(1, 1, 10, 'Likes & Dislikes Phrases', 'Vocabulary for expressing preferences.', 'DOCUMENT', '/materials/sessions/10/phrases.pdf', 5),
-- Session 11
(1, 1, 11, 'Scanning Techniques', 'How to scan for details and facts.', 'DOCUMENT', '/materials/sessions/11/scanning.pptx', 5),
-- Session 12
(1, 1, 12, 'Linking Words Chart', 'Common connecting words and their usage.', 'DOCUMENT', '/materials/sessions/12/linking-words.pdf', 5),

-- Phase 2 materials
(1, 2, NULL, 'Phase 2 Grammar Guide', 'Advanced grammar rules for the last 4 weeks.', 'DOCUMENT', '/materials/phases/2/grammar.pdf', 5),
-- Session 13
(1, 2, 13, 'Map Labeling Audio', 'Audio for map labeling exercises.', 'MEDIA', '/materials/sessions/13/maps.mp3', 5),
-- Session 14
(1, 2, 14, 'Question Formation Rules', 'Grammar guide for asking questions.', 'DOCUMENT', '/materials/sessions/14/questions.pdf', 5),
-- Session 15
(1, 2, 15, 'Short Story: The Adventure', 'Reading material for the session.', 'DOCUMENT', '/materials/sessions/15/story.pdf', 5),
-- Session 16
(1, 2, 16, 'Descriptive Adjectives List', 'Vocabulary for describing people and places.', 'DOCUMENT', '/materials/sessions/16/adjectives.docx', 5),
-- Session 17
(1, 2, 17, 'News Report Audio', 'Listening practice with news reports.', 'MEDIA', '/materials/sessions/17/news.mp3', 5),
-- Session 18
(1, 2, 18, 'Opinion Phrases Cheat Sheet', 'Useful phrases for giving opinions.', 'DOCUMENT', '/materials/sessions/18/opinions.pdf', 5),
-- Session 19
(1, 2, 19, 'Context Clues Worksheet', 'Exercises on using context clues.', 'DOCUMENT', '/materials/sessions/19/context.docx', 5),
-- Session 20
(1, 2, 20, 'Informal Letter Sample', 'Example of a personal letter.', 'DOCUMENT', '/materials/sessions/20/letter.pdf', 5),
-- Session 21
(1, 2, 21, 'Mock Test 1: Listening & Reading', 'Practice test questions.', 'DOCUMENT', '/materials/sessions/21/test1.pdf', 5),
(1, 2, 21, 'Mock Test 1 Audio', 'Audio for the listening section.', 'MEDIA', '/materials/sessions/21/audio.mp3', 5),
-- Session 22
(1, 2, 22, 'Mock Test 1: Writing & Speaking', 'Prompts for writing and speaking.', 'DOCUMENT', '/materials/sessions/22/test2.pdf', 5),
-- Session 23
(1, 2, 23, 'Course Review Slides', 'Summary of key course concepts.', 'DOCUMENT', '/materials/sessions/23/review.pptx', 5),
-- Session 24
(1, 2, 24, 'Final Exam Instructions', 'Guidelines for the final assessment.', 'DOCUMENT', '/materials/sessions/24/instructions.docx', 5);

-- Course Assessments for Foundation
INSERT INTO course_assessment (id, course_id, name, kind, duration_minutes, max_score, skill, created_at, updated_at) VALUES
(1, 1, 'Listening Quiz 1', 'QUIZ', 30, 20, 'LISTENING', '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(2, 1, 'Speaking Quiz 1', 'QUIZ', 15, 20, 'SPEAKING', '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(3, 1, 'Reading Quiz 1', 'QUIZ', 30, 20, 'READING', '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(4, 1, 'Writing Assignment 1', 'HOMEWORK', 60, 20, 'WRITING', '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(5, 1, 'Midterm Exam', 'MIDTERM', 90, 100, 'GENERAL', '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(6, 1, 'Final Exam', 'FINAL', 120, 100, 'GENERAL', '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07');

-- Course Assessment-CLO Mappings
INSERT INTO course_assessment_clo_mapping (course_assessment_id, clo_id, status) VALUES
(1, 1, 'ACTIVE'),
(2, 2, 'ACTIVE'),
(3, 3, 'ACTIVE'),
(4, 4, 'ACTIVE'),
(5, 1, 'ACTIVE'), (5, 2, 'ACTIVE'), (5, 3, 'ACTIVE'), (5, 4, 'ACTIVE'),
(6, 1, 'ACTIVE'), (6, 2, 'ACTIVE'), (6, 3, 'ACTIVE'), (6, 4, 'ACTIVE');

-- Course Sessions for Intermediate (ID 2)
INSERT INTO course_session (id, phase_id, sequence_no, topic, student_task, skill, created_at, updated_at) VALUES
(25, 3, 1, 'Complex Sentence Structures', 'Analyze complex sentences', 'WRITING', NOW(), NOW()),
(26, 3, 2, 'Advanced Listening Strategies', 'Listen for specific details', 'LISTENING', NOW(), NOW()),
(27, 4, 1, 'Speaking Part 2 & 3', 'Long turn speaking practice', 'SPEAKING', NOW(), NOW()),
(28, 4, 2, 'Essay Writing Task 2', 'Write an argumentative essay', 'WRITING', NOW(), NOW());

-- CLOs for Intermediate (ID 2)
INSERT INTO clo (id, course_id, code, description, created_at, updated_at) VALUES
(5, 2, 'CLO1', 'Apply complex grammar structures in writing and speaking', NOW(), NOW()),
(6, 2, 'CLO2', 'Analyze and synthesize information from academic texts', NOW(), NOW()),
(7, 2, 'CLO3', 'Develop coherent speaking responses for IELTS topics', NOW(), NOW()),
(8, 2, 'CLO4', 'Master listening comprehension for academic contexts', NOW(), NOW());

-- CLOs for IELTS Advanced (ID 3)
INSERT INTO clo (id, course_id, code, description, created_at, updated_at) VALUES
(9, 3, 'CLO1', 'Demonstrate mastery of academic vocabulary and discourse', NOW(), NOW()),
(10, 3, 'CLO2', 'Produce well-structured academic essays with complex arguments', NOW(), NOW()),
(11, 3, 'CLO3', 'Analyze and critique complex academic texts', NOW(), NOW()),
(12, 3, 'CLO4', 'Deliver sophisticated oral presentations on academic topics', NOW(), NOW());

-- CLOs for TOEIC Intermediate (ID 4)
INSERT INTO clo (id, course_id, code, description, created_at, updated_at) VALUES
(13, 4, 'CLO1', 'Master business English vocabulary for workplace communication', NOW(), NOW()),
(14, 4, 'CLO2', 'Comprehend business correspondence and reports', NOW(), NOW()),
(15, 4, 'CLO3', 'Write effective business emails and memos', NOW(), NOW());

-- CLOs for TOEIC Advanced (ID 5)
INSERT INTO clo (id, course_id, code, description, created_at, updated_at) VALUES
(16, 5, 'CLO1', 'Demonstrate executive-level business communication skills', NOW(), NOW()),
(17, 5, 'CLO2', 'Analyze complex business cases and propose solutions', NOW(), NOW()),
(18, 5, 'CLO3', 'Lead business meetings and negotiations effectively', NOW(), NOW());

-- CLOs for Business English (ID 6)
INSERT INTO clo (id, course_id, code, description, created_at, updated_at) VALUES
(19, 6, 'CLO1', 'Communicate professionally in international business environments', NOW(), NOW()),
(20, 6, 'CLO2', 'Deliver effective business presentations', NOW(), NOW()),
(21, 6, 'CLO3', 'Write professional business documents', NOW(), NOW());

-- CLOs for Conversation (ID 7)
INSERT INTO clo (id, course_id, code, description, created_at, updated_at) VALUES
(22, 7, 'CLO1', 'Communicate fluently in everyday social situations', NOW(), NOW()),
(23, 7, 'CLO2', 'Express opinions and discuss abstract topics confidently', NOW(), NOW());

-- CLOs for Pronunciation Workshop (ID 8)
INSERT INTO clo (id, course_id, code, description, created_at, updated_at) VALUES
(24, 8, 'CLO1', 'Produce clear and accurate English pronunciation', NOW(), NOW()),
(25, 8, 'CLO2', 'Identify and correct common pronunciation errors', NOW(), NOW());

-- CLOs for Academic Writing (ID 9)
INSERT INTO clo (id, course_id, code, description, created_at, updated_at) VALUES
(26, 9, 'CLO1', 'Write well-structured academic essays with proper citation', NOW(), NOW()),
(27, 9, 'CLO2', 'Apply academic writing conventions and research skills', NOW(), NOW());

-- CLOs for IELTS Intensive (ID 10)
INSERT INTO clo (id, course_id, code, description, created_at, updated_at) VALUES
(28, 10, 'CLO1', 'Achieve target IELTS band score through intensive practice', NOW(), NOW()),
(29, 10, 'CLO2', 'Apply advanced test-taking strategies under time pressure', NOW(), NOW());

-- Course Assessments for Intermediate (ID 2)
INSERT INTO course_assessment (id, course_id, name, kind, duration_minutes, max_score, skill, created_at, updated_at) VALUES
(7, 2, 'Writing Task 2 Assignment', 'HOMEWORK', 60, 100, 'WRITING', NOW(), NOW()),
(8, 2, 'Full Mock Test', 'FINAL', 180, 100, 'GENERAL', NOW(), NOW());

-- -- Course Materials for Intermediate (ID 2)
-- INSERT INTO course_material (course_id, phase_id, course_session_id, title, description, material_type, url, uploaded_by) VALUES
-- (2, NULL, NULL, 'IELTS Intermediate Syllabus', 'Course syllabus', 'DOCUMENT', '/materials/courses/2/syllabus.pdf', 5),
-- (2, 3, 25, 'Complex Grammar Guide', 'Guide to complex sentences', 'DOCUMENT', '/materials/sessions/25/grammar.pdf', 5);

-- ========== TIER 4: CLASSES & SESSIONS ==========

-- Classes (Test scenarios: completed, ongoing, scheduled, cancelled, various performance levels)
INSERT INTO "class" (id, branch_id, course_id, code, name, modality, start_date, planned_end_date, actual_end_date, schedule_days, max_capacity, status, approval_status, rejection_reason, created_by, decided_by, submitted_at, decided_at, created_at, updated_at) VALUES
-- 1. COMPLETED CLASSES (Diverse Performance)
(1, 1, 1, 'HN-FOUND-C1', 'HN Foundation 1 (High Perf)', 'OFFLINE', '2025-09-01', '2025-10-24', '2025-10-24', ARRAY[1,3,5]::smallint[], 20, 'COMPLETED', 'APPROVED', NULL, 6, 3, '2025-08-24 10:00:00+07', '2025-08-25 14:00:00+07', '2025-08-24 10:00:00+07', '2025-10-24 18:00:00+07'),
(13, 1, 2, 'HN-INT-C1', 'HN Intermediate 1 (Good)', 'OFFLINE', '2025-09-08', '2025-11-01', '2025-11-01', ARRAY[2,4,6]::smallint[], 20, 'COMPLETED', 'APPROVED', NULL, 6, 3, '2025-08-31 10:00:00+07', '2025-09-01 14:00:00+07', '2025-08-31 10:00:00+07', '2025-11-01 18:00:00+07'),
(14, 2, 2, 'HCM-INT-C1', 'HCM Intermediate 1 (Average)', 'OFFLINE', '2025-09-15', '2025-11-08', '2025-11-08', ARRAY[1,3,5]::smallint[], 20, 'COMPLETED', 'APPROVED', NULL, 8, 4, '2025-09-07 10:00:00+07', '2025-09-08 14:00:00+07', '2025-09-07 10:00:00+07', '2025-11-08 18:00:00+07'),
(15, 1, 3, 'HN-ADV-C1', 'HN Advanced 1 (At Risk)', 'ONLINE', '2025-09-22', '2025-11-15', '2025-11-15', ARRAY[2,4,6]::smallint[], 15, 'COMPLETED', 'APPROVED', NULL, 6, 3, '2025-09-15 10:00:00+07', '2025-09-16 14:00:00+07', '2025-09-15 10:00:00+07', '2025-11-15 18:00:00+07'),

-- 2. ONGOING CLASSES (Diverse Performance)
(2, 1, 1, 'HN-FOUND-O1', 'HN Foundation 2 (High Perf)', 'OFFLINE', '2025-11-10', '2026-01-02', NULL, ARRAY[1,3,5]::smallint[], 20, 'ONGOING', 'APPROVED', NULL, 6, 3, '2025-11-01 10:00:00+07', '2025-11-02 14:00:00+07', '2025-11-01 10:00:00+07', '2025-11-10 08:00:00+07'),
(3, 1, 1, 'HN-FOUND-O2', 'HN Foundation 3 (Average)', 'ONLINE', '2025-11-11', '2026-01-03', NULL, ARRAY[2,4,6]::smallint[], 25, 'ONGOING', 'APPROVED', NULL, 6, 3, '2025-11-02 10:00:00+07', '2025-11-03 14:00:00+07', '2025-11-02 10:00:00+07', '2025-11-11 08:00:00+07'),
(5, 2, 1, 'HCM-FOUND-O1', 'HCM Foundation 1 (High Perf)', 'OFFLINE', '2025-11-17', '2026-01-09', NULL, ARRAY[1,3,5]::smallint[], 20, 'ONGOING', 'APPROVED', NULL, 8, 4, '2025-11-08 10:00:00+07', '2025-11-09 14:00:00+07', '2025-11-08 10:00:00+07', '2025-11-17 08:00:00+07'),
(6, 2, 1, 'HCM-FOUND-E1', 'HCM Foundation 2 (Average)', 'OFFLINE', '2025-11-18', '2026-01-10', NULL, ARRAY[2,4,6]::smallint[], 18, 'ONGOING', 'APPROVED', NULL, 8, 4, '2025-11-09 10:00:00+07', '2025-11-10 14:00:00+07', '2025-11-09 10:00:00+07', '2025-11-18 08:00:00+07'),
(7, 2, 1, 'HCM-FOUND-M1', 'HCM Foundation Micro (At Risk)', 'OFFLINE', '2025-11-24', '2026-01-16', NULL, ARRAY[1,3,5]::smallint[], 5, 'ONGOING', 'APPROVED', NULL, 8, 4, '2025-11-14 10:00:00+07', '2025-11-15 14:00:00+07', '2025-11-14 10:00:00+07', '2025-11-24 08:00:00+07'),
(16, 1, 2, 'HN-INT-O1', 'HN Intermediate 2 (At Risk)', 'OFFLINE', '2025-11-17', '2026-01-09', NULL, ARRAY[1,3,5]::smallint[], 15, 'ONGOING', 'APPROVED', NULL, 6, 3, '2025-11-07 10:00:00+07', '2025-11-08 14:00:00+07', '2025-11-07 10:00:00+07', '2025-11-17 08:00:00+07'),
(17, 2, 2, 'HCM-INT-O1', 'HCM Intermediate 2 (At Risk)', 'ONLINE', '2025-11-18', '2026-01-10', NULL, ARRAY[2,4,6]::smallint[], 20, 'ONGOING', 'APPROVED', NULL, 8, 4, '2025-11-08 10:00:00+07', '2025-11-09 14:00:00+07', '2025-11-08 10:00:00+07', '2025-11-18 08:00:00+07'),
(18, 2, 3, 'HCM-ADV-O1', 'HCM Advanced 1 (Good)', 'OFFLINE', '2025-11-24', '2026-01-16', NULL, ARRAY[1,3,5]::smallint[], 15, 'ONGOING', 'APPROVED', NULL, 8, 4, '2025-11-09 10:00:00+07', '2025-11-10 14:00:00+07', '2025-11-09 10:00:00+07', '2025-11-24 08:00:00+07'),

-- 3. FUTURE/SCHEDULED CLASSES
(4, 1, 1, 'HN-FOUND-S1', 'HN Foundation 4 (Scheduled)', 'HYBRID', '2025-12-15', '2026-02-06', NULL, ARRAY[1,3,5]::smallint[], 20, 'SCHEDULED', 'APPROVED', NULL, 7, 3, '2025-12-05 10:00:00+07', '2025-12-06 14:00:00+07', '2025-12-05 10:00:00+07', '2025-12-06 14:00:00+07'),

-- 4. MAKEUP CLASSES (Special Scenarios)
(8, 1, 1, 'HN-FOUND-MAKEUP-O', 'HN Foundation Makeup (Offline)', 'OFFLINE', '2025-12-03', '2025-12-31', NULL, ARRAY[3,5]::smallint[], 15, 'ONGOING', 'APPROVED', NULL, 6, 3, '2025-11-25 10:00:00+07', '2025-11-26 14:00:00+07', '2025-11-25 10:00:00+07', '2025-11-25 14:00:00+07'),
(9, 2, 1, 'HCM-FOUND-MAKEUP-ON', 'HCM Foundation Makeup (Online)', 'ONLINE', '2025-12-04', '2026-01-02', NULL, ARRAY[4,6]::smallint[], 25, 'ONGOING', 'APPROVED', NULL, 8, 4, '2025-11-25 10:00:00+07', '2025-11-26 14:00:00+07', '2025-11-25 10:00:00+07', '2025-11-25 14:00:00+07'),
(10, 2, 1, 'HCM-FOUND-MAKEUP-OFF', 'HCM Foundation Makeup (Offline)', 'OFFLINE', '2025-12-02', '2025-12-30', NULL, ARRAY[2,4]::smallint[], 15, 'ONGOING', 'APPROVED', NULL, 8, 4, '2025-11-25 10:00:00+07', '2025-11-26 14:00:00+07', '2025-11-25 10:00:00+07', '2025-11-25 14:00:00+07'),

-- 5. DRAFT/REJECTED/CANCELLED
(11, 1, 1, 'HN-FOUND-D1', 'HN Foundation Draft', 'OFFLINE', '2026-02-10', '2026-04-10', NULL, ARRAY[2,4,6]::smallint[], 20, 'DRAFT', 'PENDING', NULL, 6, NULL, NULL, NULL, NOW(), NOW()),
(12, 1, 1, 'HN-FOUND-R1', 'HN Foundation Rejected', 'ONLINE', '2026-03-01', '2026-05-01', NULL, ARRAY[1,3,5]::smallint[], 20, 'DRAFT', 'REJECTED', 'Schedule conflict with other classes', 6, 3, '2025-12-01 10:00:00+07', '2025-12-02 10:00:00+07', NOW(), NOW()),
(19, 2, 1, 'HCM-FOUND-R1', 'HCM Foundation Cancelled', 'OFFLINE', '2025-12-05', '2026-01-25', NULL, ARRAY[1,3,5]::smallint[], 20, 'DRAFT', 'REJECTED', 'Insufficient enrollment', 8, 4, '2025-11-25 10:00:00+07', '2025-11-28 10:00:00+07', NOW(), NOW()),
(20, 1, 2, 'HN-INT-R1', 'HN Intermediate Rejected', 'HYBRID', '2026-01-10', '2026-02-20', NULL, ARRAY[2,4,6]::smallint[], 20, 'DRAFT', 'REJECTED', 'Teacher unavailable', 6, 3, '2025-12-05 10:00:00+07', '2025-12-06 10:00:00+07', NOW(), NOW());

-- Generate Sessions for Class 1 (HN-FOUND-C1) - COMPLETED
-- Start: 2025-07-07 (Mon), Schedule: Mon/Wed/Fri, 24 sessions over 8 weeks
INSERT INTO session (id, class_id, course_session_id, time_slot_template_id, date, type, status, teacher_note, created_at, updated_at)
SELECT
    s.idx,
    1,
    s.idx,
    1,
    ('2025-07-07'::date + ((s.idx - 1) / 3) * 7 + CASE (s.idx - 1) % 3 WHEN 0 THEN 0 WHEN 1 THEN 2 ELSE 4 END),
    'CLASS',
    'DONE',
    'Session completed as planned.',
    '2025-07-01 10:00:00+07',
    ('2025-07-07'::date + ((s.idx - 1) / 3) * 7 + CASE (s.idx - 1) % 3 WHEN 0 THEN 0 WHEN 1 THEN 2 ELSE 4 END)
FROM generate_series(1, 24) AS s(idx);

-- Session Resources for Class 1
INSERT INTO session_resource (session_id, resource_id)
SELECT id, 2 FROM session WHERE class_id = 1;

-- Teaching Slots for Class 1 (assign Teacher 3)
INSERT INTO teaching_slot (session_id, teacher_id, status)
SELECT id, 3, 'SCHEDULED' FROM session WHERE class_id = 1;


-- Generate Sessions for Class 2 (HN-FOUND-O1) - Main testing class
-- Start: 2025-10-06 (Mon), Schedule: Mon/Wed/Fri, 24 sessions over 8 weeks
-- Today: 2025-11-02 (Sat) - Week 5 completed
INSERT INTO session (id, class_id, course_session_id, time_slot_template_id, date, type, status, created_at, updated_at)
SELECT
    100 + s.idx,
    2,
    s.idx,
    1,
    ('2025-10-06'::date + ((s.idx - 1) / 3) * 7 + CASE (s.idx - 1) % 3 WHEN 0 THEN 0 WHEN 1 THEN 2 ELSE 4 END),
    'CLASS',
    CASE WHEN ('2025-10-06'::date + ((s.idx - 1) / 3) * 7 + CASE (s.idx - 1) % 3 WHEN 0 THEN 0 WHEN 1 THEN 2 ELSE 4 END) < '2025-11-02' THEN 'DONE' ELSE 'PLANNED' END,
    '2025-09-30 10:00:00+07',
    CURRENT_TIMESTAMP
FROM generate_series(1, 24) AS s(idx);

-- Generate Sessions for Class 3 (HN-FOUND-O2) - For transfer scenario
-- Start: 2025-10-07 (Tue), Schedule: Tue/Thu/Sat
INSERT INTO session (id, class_id, course_session_id, time_slot_template_id, date, type, status, created_at, updated_at)
SELECT
    200 + s.idx,
    3,
    s.idx,
    4,
    ('2025-10-07'::date + ((s.idx - 1) / 3) * 7 + CASE (s.idx - 1) % 3 WHEN 0 THEN 0 WHEN 1 THEN 2 ELSE 4 END),
    'CLASS',
    CASE WHEN ('2025-10-07'::date + ((s.idx - 1) / 3) * 7 + CASE (s.idx - 1) % 3 WHEN 0 THEN 0 WHEN 1 THEN 2 ELSE 4 END) < '2025-11-02' THEN 'DONE' ELSE 'PLANNED' END,
    '2025-10-01 10:00:00+07',
    CURRENT_TIMESTAMP
FROM generate_series(1, 24) AS s(idx);

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
INSERT INTO session (id, class_id, course_session_id, time_slot_template_id, date, type, status, created_at, updated_at)
SELECT
    300 + s.idx,
    5,
    s.idx,
    6,
    ('2025-10-13'::date + ((s.idx - 1) / 3) * 7 + CASE (s.idx - 1) % 3 WHEN 0 THEN 0 WHEN 1 THEN 2 ELSE 4 END),
    'CLASS',
    CASE WHEN ('2025-10-13'::date + ((s.idx - 1) / 3) * 7 + CASE (s.idx - 1) % 3 WHEN 0 THEN 0 WHEN 1 THEN 2 ELSE 4 END) < '2025-11-02' THEN 'DONE' ELSE 'PLANNED' END,
    '2025-10-06 10:00:00+07',
    CURRENT_TIMESTAMP
FROM generate_series(1, 24) AS s(idx);

-- Session Resources for Class 5
INSERT INTO session_resource (session_id, resource_id)
SELECT id, 5 FROM session WHERE class_id = 5;

-- Teaching Slots for Class 5 (assign Teacher 9)
INSERT INTO teaching_slot (session_id, teacher_id, status)
SELECT id, 9, 'SCHEDULED' FROM session WHERE class_id = 5;

-- Generate Sessions for Class 4 (HN-FOUND-S1) - SCHEDULED (Future class)
-- Start: 2025-12-15 (Mon), Schedule: Mon/Wed/Fri, 24 sessions over 8 weeks
-- All sessions are PLANNED (in the future)
INSERT INTO session (id, class_id, course_session_id, time_slot_template_id, date, type, status, created_at, updated_at)
SELECT
    400 + s.idx,
    4,
    s.idx,
    3,
    ('2025-11-18'::date + ((s.idx - 1) / 3) * 7 + CASE (s.idx - 1) % 3 WHEN 0 THEN 0 WHEN 1 THEN 2 ELSE 4 END),
    'CLASS',
    'PLANNED',
    '2025-11-10 10:00:00+07',
    CURRENT_TIMESTAMP
FROM generate_series(1, 24) AS s(idx);

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
INSERT INTO session (id, class_id, course_session_id, time_slot_template_id, date, type, status, created_at, updated_at)
SELECT
    500 + s.idx,
    6,
    s.idx,
    8,
    ('2025-10-14'::date + ((s.idx - 1) / 3) * 7 + CASE (s.idx - 1) % 3 WHEN 0 THEN 0 WHEN 1 THEN 2 ELSE 4 END),
    'CLASS',
    CASE WHEN ('2025-10-14'::date + ((s.idx - 1) / 3) * 7 + CASE (s.idx - 1) % 3 WHEN 0 THEN 0 WHEN 1 THEN 2 ELSE 4 END) < '2025-11-02' THEN 'DONE' ELSE 'PLANNED' END,
    '2025-10-08 10:00:00+07',
    CURRENT_TIMESTAMP
FROM generate_series(1, 24) AS s(idx);

-- Session Resources for Class 6 (assign HCM Room 102)
INSERT INTO session_resource (session_id, resource_id)
SELECT id, 6 FROM session WHERE class_id = 6;

-- Teaching Slots for Class 6 (assign Teacher 10 - Olivia White)
INSERT INTO teaching_slot (session_id, teacher_id, status)
SELECT id, 10, 'SCHEDULED' FROM session WHERE class_id = 6;

-- Generate Sessions for Class 7 (HCM-FOUND-M1) - Micro cohort kept at capacity
-- Start: 2025-10-20 (Mon), Schedule: Mon/Wed/Fri, Afternoon slot
INSERT INTO session (id, class_id, course_session_id, time_slot_template_id, date, type, status, created_at, updated_at)
SELECT
    600 + s.idx,
    7,
    s.idx,
    7,
    ('2025-10-20'::date + ((s.idx - 1) / 3) * 7 + CASE (s.idx - 1) % 3 WHEN 0 THEN 0 WHEN 1 THEN 2 ELSE 4 END),
    'CLASS',
    CASE WHEN ('2025-10-20'::date + ((s.idx - 1) / 3) * 7 + CASE (s.idx - 1) % 3 WHEN 0 THEN 0 WHEN 1 THEN 2 ELSE 4 END) < '2025-11-02' THEN 'DONE' ELSE 'PLANNED' END,
    '2025-10-10 10:00:00+07',
    CURRENT_TIMESTAMP
FROM generate_series(1, 24) AS s(idx);

-- Session Resources for Class 7 (assign HCM Room 201)
INSERT INTO session_resource (session_id, resource_id)
SELECT id, 7 FROM session WHERE class_id = 7;

-- Teaching Slots for Class 7 (assign Teacher 11 - Daniel Harris)
INSERT INTO teaching_slot (session_id, teacher_id, status)
SELECT id, 11, 'SCHEDULED' FROM session WHERE class_id = 7;

-- ================================================================================================
-- GENERATE SESSIONS FOR NEW CLASSES (13-18)
-- ================================================================================================

-- Class 13 (HN-INT-C1) - COMPLETED (Good)
-- Start: 2025-09-08, Mon/Wed/Fri
INSERT INTO session (id, class_id, course_session_id, time_slot_template_id, date, type, status, teacher_note, created_at, updated_at)
SELECT
    800 + s.idx,
    13,
    s.idx,
    1,
    ('2025-09-08'::date + ((s.idx - 1) / 3) * 7 + CASE (s.idx - 1) % 3 WHEN 0 THEN 0 WHEN 1 THEN 2 ELSE 4 END),
    'CLASS',
    'DONE',
    'Good session.',
    NOW(),
    NOW()
FROM generate_series(1, 24) AS s(idx);
INSERT INTO session_resource (session_id, resource_id) SELECT id, 1 FROM session WHERE class_id = 13;
INSERT INTO teaching_slot (session_id, teacher_id, status) SELECT id, 4, 'SCHEDULED' FROM session WHERE class_id = 13;

-- Class 14 (HCM-INT-C1) - COMPLETED (Average)
-- Start: 2025-09-15, Mon/Wed/Fri
INSERT INTO session (id, class_id, course_session_id, time_slot_template_id, date, type, status, teacher_note, created_at, updated_at)
SELECT
    900 + s.idx,
    14,
    s.idx,
    6,
    ('2025-09-15'::date + ((s.idx - 1) / 3) * 7 + CASE (s.idx - 1) % 3 WHEN 0 THEN 0 WHEN 1 THEN 2 ELSE 4 END),
    'CLASS',
    'DONE',
    'Session ok.',
    NOW(),
    NOW()
FROM generate_series(1, 24) AS s(idx);
INSERT INTO session_resource (session_id, resource_id) SELECT id, 5 FROM session WHERE class_id = 14;
INSERT INTO teaching_slot (session_id, teacher_id, status) SELECT id, 12, 'SCHEDULED' FROM session WHERE class_id = 14;

-- Class 15 (HN-ADV-C1) - COMPLETED (At Risk)
-- Start: 2025-09-22, Tue/Thu/Sat
INSERT INTO session (id, class_id, course_session_id, time_slot_template_id, date, type, status, teacher_note, created_at, updated_at)
SELECT
    1000 + s.idx,
    15,
    s.idx,
    4,
    ('2025-09-22'::date + ((s.idx - 1) / 3) * 7 + CASE (s.idx - 1) % 3 WHEN 0 THEN 1 WHEN 1 THEN 3 ELSE 5 END),
    'CLASS',
    'DONE',
    'Issues noted.',
    NOW(),
    NOW()
FROM generate_series(1, 24) AS s(idx);
INSERT INTO session_resource (session_id, resource_id) SELECT id, 4 FROM session WHERE class_id = 15; -- Online
INSERT INTO teaching_slot (session_id, teacher_id, status) SELECT id, 2, 'SCHEDULED' FROM session WHERE class_id = 15;

-- Class 16 (HN-INT-O1) - ONGOING (At Risk)
-- Start: 2025-11-17, Mon/Wed/Fri
INSERT INTO session (id, class_id, course_session_id, time_slot_template_id, date, type, status, created_at, updated_at)
SELECT
    1100 + s.idx,
    16,
    s.idx,
    1,
    ('2025-11-17'::date + ((s.idx - 1) / 3) * 7 + CASE (s.idx - 1) % 3 WHEN 0 THEN 0 WHEN 1 THEN 2 ELSE 4 END),
    'CLASS',
    CASE WHEN ('2025-11-17'::date + ((s.idx - 1) / 3) * 7 + CASE (s.idx - 1) % 3 WHEN 0 THEN 0 WHEN 1 THEN 2 ELSE 4 END) < '2025-11-28' THEN 'DONE' ELSE 'PLANNED' END,
    NOW(),
    NOW()
FROM generate_series(1, 24) AS s(idx);
INSERT INTO session_resource (session_id, resource_id) SELECT id, 2 FROM session WHERE class_id = 16;
INSERT INTO teaching_slot (session_id, teacher_id, status) SELECT id, 5, 'SCHEDULED' FROM session WHERE class_id = 16;

-- Class 17 (HCM-INT-O1) - ONGOING (At Risk)
-- Start: 2025-11-18, Tue/Thu/Sat
INSERT INTO session (id, class_id, course_session_id, time_slot_template_id, date, type, status, created_at, updated_at)
SELECT
    1200 + s.idx,
    17,
    s.idx,
    8,
    ('2025-11-18'::date + ((s.idx - 1) / 3) * 7 + CASE (s.idx - 1) % 3 WHEN 0 THEN 0 WHEN 1 THEN 2 ELSE 4 END),
    'CLASS',
    CASE WHEN ('2025-11-18'::date + ((s.idx - 1) / 3) * 7 + CASE (s.idx - 1) % 3 WHEN 0 THEN 0 WHEN 1 THEN 2 ELSE 4 END) < '2025-11-28' THEN 'DONE' ELSE 'PLANNED' END,
    NOW(),
    NOW()
FROM generate_series(1, 24) AS s(idx);
INSERT INTO session_resource (session_id, resource_id) SELECT id, 8 FROM session WHERE class_id = 17; -- Online
INSERT INTO teaching_slot (session_id, teacher_id, status) SELECT id, 13, 'SCHEDULED' FROM session WHERE class_id = 17;

-- Class 18 (HCM-ADV-O1) - ONGOING (Good)
-- Start: 2025-11-24, Mon/Wed/Fri
INSERT INTO session (id, class_id, course_session_id, time_slot_template_id, date, type, status, created_at, updated_at)
SELECT
    1300 + s.idx,
    18,
    s.idx,
    6,
    ('2025-11-24'::date + ((s.idx - 1) / 3) * 7 + CASE (s.idx - 1) % 3 WHEN 0 THEN 0 WHEN 1 THEN 2 ELSE 4 END),
    'CLASS',
    CASE WHEN ('2025-11-24'::date + ((s.idx - 1) / 3) * 7 + CASE (s.idx - 1) % 3 WHEN 0 THEN 0 WHEN 1 THEN 2 ELSE 4 END) < '2025-11-28' THEN 'DONE' ELSE 'PLANNED' END,
    NOW(),
    NOW()
FROM generate_series(1, 24) AS s(idx);
INSERT INTO session_resource (session_id, resource_id) SELECT id, 6 FROM session WHERE class_id = 18;
INSERT INTO teaching_slot (session_id, teacher_id, status) SELECT id, 9, 'SCHEDULED' FROM session WHERE class_id = 18;


-- ========== MAKEUP OPTIONS TEST DATA ==========
-- Generate makeup sessions for student 1's absences (2025-11-03 to 2025-11-16)

-- =========================================
-- NEW CLASSES FOR COURSES 4-10 - REALISTIC SCHEDULING
-- =========================================

-- Classes for TOEIC Intermediate (Course 4)
INSERT INTO "class" (id, branch_id, course_id, code, name, modality, start_date, planned_end_date, actual_end_date, schedule_days, max_capacity, status, approval_status, rejection_reason, created_by, decided_by, submitted_at, decided_at, created_at, updated_at) VALUES
(21, 1, 4, 'HN-TOEIC-I1', 'HN TOEIC Intermediate 1 (Business)', 'OFFLINE', '2025-12-02', '2026-02-15', NULL, ARRAY[2,4]::smallint[], 18, 'ONGOING', 'APPROVED', NULL, 8, 3, '2025-11-20 10:00:00+07', '2025-11-21 14:00:00+07', '2025-11-20 10:00:00+07', '2025-12-02 08:00:00+07'),
(22, 2, 4, 'HCM-TOEIC-I1', 'HCM TOEIC Intermediate 1 (Online)', 'ONLINE', '2025-12-09', '2026-02-24', NULL, ARRAY[1,3,5]::smallint[], 20, 'ONGOING', 'APPROVED', NULL, 9, 4, '2025-11-25 10:00:00+07', '2025-11-26 14:00:00+07', '2025-11-25 10:00:00+07', '2025-12-09 08:00:00+07');

-- Classes for TOEIC Advanced (Course 5)
INSERT INTO "class" (id, branch_id, course_id, code, name, modality, start_date, planned_end_date, actual_end_date, schedule_days, max_capacity, status, approval_status, rejection_reason, created_by, decided_by, submitted_at, decided_at, created_at, updated_at) VALUES
(23, 1, 5, 'HN-TOEIC-A1', 'HN TOEIC Advanced 1 (Executive)', 'HYBRID', '2025-12-16', '2026-02-20', NULL, ARRAY[6,7]::smallint[], 15, 'ONGOING', 'APPROVED', NULL, 8, 3, '2025-12-05 10:00:00+07', '2025-12-06 14:00:00+07', '2025-12-05 10:00:00+07', '2025-12-16 08:00:00+07');

-- Classes for Business English (Course 6)
INSERT INTO "class" (id, branch_id, course_id, code, name, modality, start_date, planned_end_date, actual_end_date, schedule_days, max_capacity, status, approval_status, rejection_reason, created_by, decided_by, submitted_at, decided_at, created_at, updated_at) VALUES
(24, 1, 6, 'HN-BUS-1', 'HN Business English 1 (Weekend)', 'OFFLINE', '2025-12-07', '2026-02-01', NULL, ARRAY[6,7]::smallint[], 12, 'ONGOING', 'APPROVED', NULL, 7, 3, '2025-11-28 10:00:00+07', '2025-11-29 14:00:00+07', '2025-11-28 10:00:00+07', '2025-12-07 08:00:00+07'),
(25, 2, 6, 'HCM-BUS-1', 'HCM Business English 1 (Evening)', 'ONLINE', '2025-12-09', '2026-02-03', NULL, ARRAY[2,4]::smallint[], 16, 'ONGOING', 'APPROVED', NULL, 8, 4, '2025-11-29 10:00:00+07', '2025-11-30 14:00:00+07', '2025-11-29 10:00:00+07', '2025-12-09 08:00:00+07');

-- Classes for Conversation (Course 7)
INSERT INTO "class" (id, branch_id, course_id, code, name, modality, start_date, planned_end_date, actual_end_date, schedule_days, max_capacity, status, approval_status, rejection_reason, created_by, decided_by, submitted_at, decided_at, created_at, updated_at) VALUES
(26, 1, 7, 'HN-CONV-1', 'HN Conversation 1 (Evening)', 'OFFLINE', '2025-12-06', '2026-01-25', NULL, ARRAY[1,3,5]::smallint[], 10, 'ONGOING', 'APPROVED', NULL, 7, 3, '2025-11-27 10:00:00+07', '2025-11-28 14:00:00+07', '2025-11-27 10:00:00+07', '2025-12-06 08:00:00+07'),
(27, 2, 7, 'HCM-CONV-1', 'HCM Conversation 1 (Morning)', 'ONLINE', '2025-12-08', '2026-01-27', NULL, ARRAY[2,4,6]::smallint[], 12, 'ONGOING', 'APPROVED', NULL, 8, 4, '2025-11-28 10:00:00+07', '2025-11-29 14:00:00+07', '2025-11-28 10:00:00+07', '2025-12-08 08:00:00+07');

-- Classes for Pronunciation Workshop (Course 8)
INSERT INTO "class" (id, branch_id, course_id, code, name, modality, start_date, planned_end_date, actual_end_date, schedule_days, max_capacity, status, approval_status, rejection_reason, created_by, decided_by, submitted_at, decided_at, created_at, updated_at) VALUES
(28, 1, 8, 'HN-PRON-1', 'HN Pronunciation Workshop 1 (Weekend)', 'OFFLINE', '2025-12-14', '2026-01-18', NULL, ARRAY[6,7]::smallint[], 8, 'SCHEDULED', 'APPROVED', NULL, 7, 3, '2025-12-05 10:00:00+07', '2025-12-06 14:00:00+07', '2025-12-05 10:00:00+07', '2025-12-05 08:00:00+07');

-- Sessions for makeup classes
    -- Class 8 Sessions (HN, OFFLINE) - Wed, Fri
    INSERT INTO session (id, class_id, course_session_id, time_slot_template_id, date, type, status, created_at, updated_at) VALUES
    (701, 8, 10, 3, '2025-11-05', 'CLASS', 'PLANNED', NOW(), NOW()),
    (702, 8, 11, 3, '2025-11-07', 'CLASS', 'PLANNED', NOW(), NOW()),
    (703, 8, 12, 3, '2025-11-12', 'CLASS', 'PLANNED', NOW(), NOW()),
    (710, 8, 16, 3, '2025-11-14', 'CLASS', 'PLANNED', NOW(), NOW()); -- Makeup for session 116 (course_session_id 16)

    -- Class 9 Sessions (HCM, ONLINE) - Thu, Sat
    INSERT INTO session (id, class_id, course_session_id, time_slot_template_id, date, type, status, created_at, updated_at) VALUES
    (704, 9, 10, 7, '2025-11-06', 'CLASS', 'PLANNED', NOW(), NOW()),
    (705, 9, 11, 7, '2025-11-08', 'CLASS', 'PLANNED', NOW(), NOW()),
    (706, 9, 12, 7, '2025-11-13', 'CLASS', 'PLANNED', NOW(), NOW()),
    (711, 9, 16, 7, '2025-11-15', 'CLASS', 'PLANNED', NOW(), NOW()); -- Makeup for session 116 (course_session_id 16)

    -- Class 10 Sessions (HCM, OFFLINE) - Tue, Thu
    INSERT INTO session (id, class_id, course_session_id, time_slot_template_id, date, type, status, created_at, updated_at) VALUES
    (707, 10, 10, 7, '2025-11-04', 'CLASS', 'PLANNED', NOW(), NOW()),
    (708, 10, 11, 7, '2025-11-06', 'CLASS', 'PLANNED', NOW(), NOW()),
    (709, 10, 12, 7, '2025-11-11', 'CLASS', 'PLANNED', NOW(), NOW()),
    (712, 10, 16, 7, '2025-11-13', 'CLASS', 'PLANNED', NOW(), NOW()); -- Makeup for session 116 (course_session_id 16)

-- Classes for Academic Writing (Course 9)
INSERT INTO "class" (id, branch_id, course_id, code, name, modality, start_date, planned_end_date, actual_end_date, schedule_days, max_capacity, status, approval_status, rejection_reason, created_by, decided_by, submitted_at, decided_at, created_at, updated_at) VALUES
(29, 1, 9, 'HN-WRIT-1', 'HN Academic Writing 1 (Evening)', 'HYBRID', '2026-01-05', '2026-02-20', NULL, ARRAY[3,5]::smallint[], 15, 'SCHEDULED', 'PENDING', NULL, 7, NULL, '2025-12-20 10:00:00+07', NULL, '2025-12-20 10:00:00+07', '2025-12-20 10:00:00+07');

-- Classes for IELTS Intensive (Course 10)
INSERT INTO "class" (id, branch_id, course_id, code, name, modality, start_date, planned_end_date, actual_end_date, schedule_days, max_capacity, status, approval_status, rejection_reason, created_by, decided_by, submitted_at, decided_at, created_at, updated_at) VALUES
(30, 1, 10, 'HN-IELTS-INT-1', 'HN IELTS Intensive 1 (Daily)', 'OFFLINE', '2026-01-08', '2026-02-28', NULL, ARRAY[1,2,3,4,5]::smallint[], 18, 'SCHEDULED', 'APPROVED', NULL, 7, 3, '2025-12-15 10:00:00+07', '2025-12-16 14:00:00+07', '2025-12-15 10:00:00+07', '2025-12-15 08:00:00+07'),
(31, 2, 10, 'HCM-IELTS-INT-1', 'HCM IELTS Intensive 1 (Online)', 'ONLINE', '2026-01-10', '2026-03-05', NULL, ARRAY[2,3,4,5,6]::smallint[], 20, 'SCHEDULED', 'APPROVED', NULL, 8, 4, '2025-12-20 10:00:00+07', '2025-12-21 14:00:00+07', '2025-12-20 10:00:00+07', '2025-12-20 08:00:00+07');

-- Sessions for classes 8, 9, 10 are defined in Tier 4; no additional makeup batch here.

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
    CURRENT_TIMESTAMP,
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

-- Enrollment with Capacity Override
INSERT INTO enrollment (id, class_id, student_id, status, enrolled_at, enrolled_by, join_session_id, capacity_override, override_reason, created_at, updated_at) VALUES
(18, 2, 60, 'ENROLLED', '2025-10-05 09:00:00+07', 6, 101, true, 'Special request from Center Head', '2025-10-05 09:00:00+07', '2025-10-05 09:00:00+07');

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
-- Mark student 1 as ABSENT for 3 recent sessions (late Nov 2025 near ref date)
UPDATE student_session 
SET attendance_status = 'ABSENT', note = 'Missed session, eligible for makeup.'
WHERE student_id = 1 
  AND session_id IN (
    SELECT id FROM session 
    WHERE class_id = 2 
      AND status = 'DONE'
      AND date >= '2025-11-20'  -- Last week relative to ref date 2025-11-28
      AND date <= '2025-11-26'  -- Before reference date
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
            CASE WHEN random() < 0.70 THEN 'PRESENT' ELSE 'ABSENT' END
        ELSE 'PLANNED'
    END,
    CASE 
        WHEN s.status = 'DONE' AND cs.student_task IS NOT NULL THEN
            CASE WHEN random() < 0.60 THEN 'COMPLETED' ELSE 'INCOMPLETE' END
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

-- ================================================================================================
-- ENROLLMENTS & SESSIONS FOR NEW CLASSES (13-18)
-- ================================================================================================

-- Class 13 (HN-INT-C1) - Students 61-70 (Completed, High Perf)
INSERT INTO enrollment (id, class_id, student_id, status, enrolled_at, enrolled_by, join_session_id, created_at, updated_at)
SELECT 1300 + s.id, 13, 60 + s.id, 'COMPLETED', '2025-08-01', 6, 801, NOW(), NOW() FROM generate_series(1, 10) AS s(id);

INSERT INTO student_session (student_id, session_id, attendance_status, homework_status, recorded_at)
SELECT e.student_id, s.id, 
    CASE WHEN random() < 0.95 THEN 'PRESENT' ELSE 'ABSENT' END,
    CASE WHEN random() < 0.90 THEN 'COMPLETED' ELSE 'INCOMPLETE' END,
    s.date
FROM enrollment e JOIN session s ON s.class_id = e.class_id WHERE e.class_id = 13;

-- Class 14 (HCM-INT-C1) - Students 71-80 (Completed, Average)
INSERT INTO enrollment (id, class_id, student_id, status, enrolled_at, enrolled_by, join_session_id, created_at, updated_at)
SELECT 1400 + s.id, 14, 70 + s.id, 'COMPLETED', '2025-08-08', 8, 901, NOW(), NOW() FROM generate_series(1, 10) AS s(id);

INSERT INTO student_session (student_id, session_id, attendance_status, homework_status, recorded_at)
SELECT e.student_id, s.id, 
    CASE WHEN random() < 0.85 THEN 'PRESENT' ELSE 'ABSENT' END,
    CASE WHEN random() < 0.80 THEN 'COMPLETED' ELSE 'INCOMPLETE' END,
    s.date
FROM enrollment e JOIN session s ON s.class_id = e.class_id WHERE e.class_id = 14;

-- Class 15 (HN-ADV-C1) - Students 81-90 (Completed, At Risk)
INSERT INTO enrollment (id, class_id, student_id, status, enrolled_at, enrolled_by, join_session_id, created_at, updated_at)
SELECT 1500 + s.id, 15, 80 + s.id, 'COMPLETED', '2025-07-10', 6, 1001, NOW(), NOW() FROM generate_series(1, 10) AS s(id);

INSERT INTO student_session (student_id, session_id, attendance_status, homework_status, recorded_at)
SELECT e.student_id, s.id, 
    CASE WHEN random() < 0.70 THEN 'PRESENT' ELSE 'ABSENT' END,
    CASE WHEN random() < 0.60 THEN 'COMPLETED' ELSE 'INCOMPLETE' END,
    s.date
FROM enrollment e JOIN session s ON s.class_id = e.class_id WHERE e.class_id = 15;

-- Class 16 (HN-INT-O1) - Students 1-10 (Ongoing, At Risk)
INSERT INTO enrollment (id, class_id, student_id, status, enrolled_at, enrolled_by, join_session_id, created_at, updated_at)
SELECT 1600 + s.id, 16, s.id, 'ENROLLED', '2025-10-10', 6, 1101, NOW(), NOW() FROM generate_series(1, 10) AS s(id);

INSERT INTO student_session (student_id, session_id, attendance_status, homework_status, recorded_at)
SELECT e.student_id, s.id, 
    CASE WHEN s.status = 'DONE' THEN (CASE WHEN random() < 0.75 THEN 'PRESENT' ELSE 'ABSENT' END) ELSE 'PLANNED' END,
    CASE WHEN s.status = 'DONE' THEN (CASE WHEN random() < 0.65 THEN 'COMPLETED' ELSE 'INCOMPLETE' END) ELSE NULL END,
    CASE WHEN s.status = 'DONE' THEN s.date ELSE NULL END
FROM enrollment e JOIN session s ON s.class_id = e.class_id WHERE e.class_id = 16;

-- Class 17 (HCM-INT-O1) - Students 31-40 (Ongoing, At Risk)
INSERT INTO enrollment (id, class_id, student_id, status, enrolled_at, enrolled_by, join_session_id, created_at, updated_at)
SELECT 1700 + s.id, 17, 30 + s.id, 'ENROLLED', '2025-10-15', 8, 1201, NOW(), NOW() FROM generate_series(1, 10) AS s(id);

INSERT INTO student_session (student_id, session_id, attendance_status, homework_status, recorded_at)
SELECT e.student_id, s.id, 
    CASE WHEN s.status = 'DONE' THEN (CASE WHEN random() < 0.75 THEN 'PRESENT' ELSE 'ABSENT' END) ELSE 'PLANNED' END,
    CASE WHEN s.status = 'DONE' THEN (CASE WHEN random() < 0.65 THEN 'COMPLETED' ELSE 'INCOMPLETE' END) ELSE NULL END,
    CASE WHEN s.status = 'DONE' THEN s.date ELSE NULL END
FROM enrollment e JOIN session s ON s.class_id = e.class_id WHERE e.class_id = 17;

-- Class 18 (HCM-ADV-O1) - Students 91-100 (Ongoing, Good)
INSERT INTO enrollment (id, class_id, student_id, status, enrolled_at, enrolled_by, join_session_id, created_at, updated_at)
SELECT 1800 + s.id, 18, 90 + s.id, 'ENROLLED', '2025-10-15', 8, 1301, NOW(), NOW() FROM generate_series(1, 10) AS s(id);

INSERT INTO student_session (student_id, session_id, attendance_status, homework_status, recorded_at)
SELECT e.student_id, s.id, 
    CASE WHEN s.status = 'DONE' THEN (CASE WHEN random() < 0.92 THEN 'PRESENT' ELSE 'ABSENT' END) ELSE 'PLANNED' END,
    CASE WHEN s.status = 'DONE' THEN (CASE WHEN random() < 0.88 THEN 'COMPLETED' ELSE 'INCOMPLETE' END) ELSE NULL END,
    CASE WHEN s.status = 'DONE' THEN s.date ELSE NULL END
FROM enrollment e JOIN session s ON s.class_id = e.class_id WHERE e.class_id = 18;

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

-- SCENARIO 7: Teacher Replacement Request - Approved
INSERT INTO teacher_request (id, teacher_id, session_id, request_type, replacement_teacher_id, status, request_reason, submitted_by, submitted_at, decided_by, decided_at) VALUES
(1, 1, 115, 'REPLACEMENT', 3, 'APPROVED', 'Family emergency - cannot attend session', 20, '2025-10-28 08:00:00+07', 6, '2025-10-28 10:00:00+07');

-- Execute replacement: Update teaching_slot
UPDATE teaching_slot 
SET teacher_id = 3, status = 'SUBSTITUTED'
WHERE session_id = 115 AND teacher_id = 1;

-- SCENARIO 8: Teacher Reschedule Request - Pending
INSERT INTO teacher_request (id, teacher_id, session_id, request_type, new_date, new_time_slot_id, new_resource_id, status, request_reason, submitted_by, submitted_at) VALUES
(2, 2, 215, 'RESCHEDULE', '2025-11-05', 5, 4, 'PENDING', 'Conference attendance - propose rescheduling to evening slot', 21, '2025-11-01 09:00:00+07');

-- SCENARIO 9: Teacher Modality Change Request - Approved
INSERT INTO teacher_request (id, teacher_id, session_id, request_type, new_resource_id, status, request_reason, submitted_by, submitted_at, decided_by, decided_at) VALUES
(3, 1, 117, 'MODALITY_CHANGE', 4, 'APPROVED', 'Room air conditioning broken - need to switch to online', 20, '2025-11-01 07:00:00+07', 6, '2025-11-01 08:00:00+07');

-- Rejected Teacher Request
INSERT INTO teacher_request (id, teacher_id, session_id, request_type, status, request_reason, submitted_by, submitted_at, decided_by, decided_at) VALUES
(4, 1, 118, 'REPLACEMENT', 'REJECTED', 'Personal reason', 20, NOW(), 6, NOW());

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
INSERT INTO score (assessment_id, student_id, score, feedback, graded_by, graded_at, created_at, updated_at) VALUES
(1, 1, 18.0, 'Good listening skills', 1, '2025-10-19 10:00:00+07', '2025-10-19 10:00:00+07', '2025-10-19 10:00:00+07'),
(1, 2, 16.5, 'Need more practice on numbers', 1, '2025-10-19 10:00:00+07', '2025-10-19 10:00:00+07', '2025-10-19 10:00:00+07'),
(1, 3, 19.0, 'Excellent performance', 1, '2025-10-19 10:00:00+07', '2025-10-19 10:00:00+07', '2025-10-19 10:00:00+07'),
(1, 4, 15.0, 'Satisfactory', 1, '2025-10-19 10:00:00+07', '2025-10-19 10:00:00+07', '2025-10-19 10:00:00+07'),
(1, 5, 17.5, 'Good work', 1, '2025-10-19 10:00:00+07', '2025-10-19 10:00:00+07', '2025-10-19 10:00:00+07'),
(1, 6, 14.0, 'Need improvement', 1, '2025-10-19 10:00:00+07', '2025-10-19 10:00:00+07', '2025-10-19 10:00:00+07'),
(1, 7, 18.5, 'Very good', 1, '2025-10-19 10:00:00+07', '2025-10-19 10:00:00+07', '2025-10-19 10:00:00+07'),
(1, 8, 16.0, 'Good progress', 1, '2025-10-19 10:00:00+07', '2025-10-19 10:00:00+07', '2025-10-19 10:00:00+07'),
(1, 9, 17.0, 'Well done', 1, '2025-10-19 10:00:00+07', '2025-10-19 10:00:00+07', '2025-10-19 10:00:00+07'),
(1, 10, 15.5, 'Fair performance', 1, '2025-10-19 10:00:00+07', '2025-10-19 10:00:00+07', '2025-10-19 10:00:00+07');

-- Scores for Speaking Quiz 1
INSERT INTO score (assessment_id, student_id, score, feedback, graded_by, graded_at, created_at, updated_at) VALUES
(2, 1, 17.0, 'Good fluency, work on pronunciation', 1, '2025-10-22 10:00:00+07', '2025-10-22 10:00:00+07', '2025-10-22 10:00:00+07'),
(2, 2, 18.0, 'Confident speaker', 1, '2025-10-22 10:00:00+07', '2025-10-22 10:00:00+07', '2025-10-22 10:00:00+07'),
(2, 3, 16.5, 'Good effort', 1, '2025-10-22 10:00:00+07', '2025-10-22 10:00:00+07', '2025-10-22 10:00:00+07'),
(2, 4, 15.0, 'Need more practice', 1, '2025-10-22 10:00:00+07', '2025-10-22 10:00:00+07', '2025-10-22 10:00:00+07'),
(2, 5, 19.0, 'Excellent speaking skills', 1, '2025-10-22 10:00:00+07', '2025-10-22 10:00:00+07', '2025-10-22 10:00:00+07');

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

-- ========== TIER 8: FEEDBACK & QA (Vietnamese Content) ==========

-- Student Feedback (Sample 50+ entries with Vietnamese comments)
-- Class 13 (High Perf): Positive feedback
INSERT INTO student_feedback (id, student_id, class_id, phase_id, is_feedback, submitted_at, response, created_at, updated_at)
SELECT 100 + s.id, 60 + s.id, 13, 1, true, '2025-08-20 10:00:00+07', 'Học viên rất hài lòng.', NOW(), NOW() FROM generate_series(1, 8) AS s(id);

INSERT INTO student_feedback_response (feedback_id, question_id, rating, created_at, updated_at)
SELECT f.id, q.id, 
    CASE WHEN q.question_type = 'rating' THEN 5 ELSE NULL END,
    NOW(), NOW()
FROM student_feedback f CROSS JOIN feedback_question q WHERE f.class_id = 13;

-- Class 14 (Average): Mixed feedback
INSERT INTO student_feedback (id, student_id, class_id, phase_id, is_feedback, submitted_at, response, created_at, updated_at)
SELECT 200 + s.id, 70 + s.id, 14, 1, true, '2025-08-25 10:00:00+07', 'Cần cải thiện tốc độ giảng dạy.', NOW(), NOW() FROM generate_series(1, 8) AS s(id);

INSERT INTO student_feedback_response (feedback_id, question_id, rating, created_at, updated_at)
SELECT f.id, q.id, 
    CASE WHEN q.question_type = 'rating' THEN 3 + (f.id % 2) ELSE NULL END, -- Rating 3 or 4
    NOW(), NOW()
FROM student_feedback f CROSS JOIN feedback_question q WHERE f.class_id = 14;

-- Class 15 (At Risk): Negative feedback
INSERT INTO student_feedback (id, student_id, class_id, phase_id, is_feedback, submitted_at, response, created_at, updated_at)
SELECT 300 + s.id, 80 + s.id, 15, 1, true, '2025-07-30 10:00:00+07', 'Giáo viên thường xuyên đến muộn.', NOW(), NOW() FROM generate_series(1, 8) AS s(id);

INSERT INTO student_feedback_response (feedback_id, question_id, rating, created_at, updated_at)
SELECT f.id, q.id, 
    CASE WHEN q.question_type = 'rating' THEN 2 ELSE NULL END, -- Rating 2
    NOW(), NOW()
FROM student_feedback f CROSS JOIN feedback_question q WHERE f.class_id = 15;

-- Class 2 (Ongoing High Perf): Recent feedback
INSERT INTO student_feedback (id, student_id, class_id, phase_id, is_feedback, submitted_at, response, created_at, updated_at)
SELECT 400 + s.id, s.id, 2, 1, true, '2025-10-28 10:00:00+07', 'Rất thích cách cô giáo tổ chức trò chơi.', NOW(), NOW() FROM generate_series(1, 5) AS s(id);

INSERT INTO student_feedback_response (feedback_id, question_id, rating, created_at, updated_at)
SELECT f.id, q.id, 
    CASE WHEN q.question_type = 'rating' THEN 5 ELSE NULL END,
    NOW(), NOW()
FROM student_feedback f CROSS JOIN feedback_question q WHERE f.class_id = 2;

-- Reset feedback 401 (student 1, class 2, phase 1) to pending for demo
DELETE FROM student_feedback_response WHERE feedback_id = 401;
UPDATE student_feedback
SET is_feedback = false,
    submitted_at = NULL,
    response = NULL,
    updated_at = NOW()
WHERE id = 401;

-- QA Reports (20+ Samples covering various types)

-- 1. Classroom Observation (Dự giờ) - Class 2 (Good)
INSERT INTO qa_report (id, class_id, session_id, reported_by, report_type, status, findings, action_items, created_at, updated_at) VALUES
(1, 2, 105, 10, 'CLASSROOM_OBSERVATION', 'SUBMITTED', 'Giáo viên chuẩn bị bài kỹ lưỡng. Tương tác với học viên tốt. Không khí lớp học sôi nổi.', 'Đề xuất giáo viên chia sẻ kinh nghiệm giảng dạy cho các giáo viên mới.', '2025-10-15 10:00:00+07', '2025-10-15 10:00:00+07'),
(2, 2, 110, 10, 'CLASSROOM_OBSERVATION', 'SUBMITTED', 'Học viên tham gia đầy đủ. Bài giảng đi đúng trọng tâm.', 'Tiếp tục phát huy.', '2025-10-25 10:00:00+07', '2025-10-25 10:00:00+07');

-- 2. Classroom Observation - Class 15 (Issues)
INSERT INTO qa_report (id, class_id, session_id, reported_by, report_type, status, findings, action_items, created_at, updated_at) VALUES
(3, 15, 1005, 11, 'CLASSROOM_OBSERVATION', 'SUBMITTED', 'Giáo viên vào lớp muộn 10 phút. Lớp học ồn ào, thiếu kiểm soát.', 'Nhắc nhở giáo viên về quy định giờ giấc. Cần có biện pháp quản lý lớp học tốt hơn.', '2025-07-25 10:00:00+07', '2025-07-25 10:00:00+07'),
(4, 15, 1010, 11, 'CLASSROOM_OBSERVATION', 'DRAFT', 'Học viên ít tương tác. Giáo viên chỉ giảng bài một chiều.', 'Cần tổ chức training về phương pháp giảng dạy tương tác.', '2025-08-05 10:00:00+07', '2025-08-05 10:00:00+07');

-- 3. Student Feedback Analysis (Phân tích phản hồi)
INSERT INTO qa_report (id, class_id, reported_by, report_type, status, findings, action_items, created_at, updated_at) VALUES
(5, 13, 10, 'STUDENT_FEEDBACK_ANALYSIS', 'SUBMITTED', '100% học viên hài lòng với khóa học. Điểm đánh giá trung bình 4.8/5.', 'Khen thưởng giáo viên.', '2025-08-25 10:00:00+07', '2025-08-25 10:00:00+07'),
(6, 14, 11, 'STUDENT_FEEDBACK_ANALYSIS', 'SUBMITTED', 'Phản hồi trái chiều. Một số học viên phàn nàn về tốc độ giảng dạy.', 'Trao đổi với giáo viên để điều chỉnh tốc độ phù hợp với trình độ học viên.', '2025-08-30 10:00:00+07', '2025-08-30 10:00:00+07'),
(7, 15, 11, 'STUDENT_FEEDBACK_ANALYSIS', 'SUBMITTED', 'Nhiều phản hồi tiêu cực về thái độ giáo viên và chất lượng bài giảng.', 'Cần họp khẩn với giáo viên và Academic Manager để xem xét vấn đề.', '2025-08-05 10:00:00+07', '2025-08-05 10:00:00+07');

-- 4. CLO Achievement Analysis (Đánh giá CLO)
INSERT INTO qa_report (id, class_id, phase_id, reported_by, report_type, status, findings, action_items, created_at, updated_at) VALUES
(8, 13, 1, 10, 'CLO_ACHIEVEMENT_ANALYSIS', 'SUBMITTED', 'Học viên đạt 90% chuẩn đầu ra Phase 1.', 'Cho phép chuyển sang Phase 2.', '2025-08-15 10:00:00+07', '2025-08-15 10:00:00+07'),
(9, 15, 1, 11, 'CLO_ACHIEVEMENT_ANALYSIS', 'SUBMITTED', 'Chỉ 60% học viên đạt chuẩn đầu ra. Kỹ năng Viết còn yếu.', 'Tổ chức các buổi phụ đạo thêm về kỹ năng Viết.', '2025-08-01 10:00:00+07', '2025-08-01 10:00:00+07');

-- 5. Attendance & Engagement Review (Đánh giá chuyên cần)
INSERT INTO qa_report (id, class_id, reported_by, report_type, status, findings, action_items, created_at, updated_at) VALUES
(10, 7, 11, 'ATTENDANCE_ENGAGEMENT_REVIEW', 'SUBMITTED', 'Tỷ lệ chuyên cần thấp (dưới 80%). Nhiều học viên nghỉ không phép.', 'Liên hệ phụ huynh để thông báo tình hình. Cảnh báo học viên về nguy cơ cấm thi.', '2025-10-25 10:00:00+07', '2025-10-25 10:00:00+07'),
(11, 2, 10, 'ATTENDANCE_ENGAGEMENT_REVIEW', 'SUBMITTED', 'Tỷ lệ chuyên cần cao (>95%).', 'Tiếp tục duy trì.', '2025-10-20 10:00:00+07', '2025-10-20 10:00:00+07');

-- 6. Teaching Quality Assessment (Đánh giá chất lượng giảng dạy)
INSERT INTO qa_report (id, class_id, reported_by, report_type, status, findings, action_items, created_at, updated_at) VALUES
(12, 1, 10, 'TEACHING_QUALITY_ASSESSMENT', 'SUBMITTED', 'Giáo viên có chuyên môn vững, phương pháp sư phạm tốt.', 'Đề xuất tăng lương hoặc thăng cấp bậc.', '2025-09-05 10:00:00+07', '2025-09-05 10:00:00+07'),
(13, 16, 10, 'TEACHING_QUALITY_ASSESSMENT', 'DRAFT', 'Giáo viên còn lúng túng khi xử lý tình huống sư phạm.', 'Cần tham gia khóa đào tạo kỹ năng quản lý lớp học.', '2025-10-30 10:00:00+07', '2025-10-30 10:00:00+07');

-- 7. Phase Review (Đánh giá giai đoạn)
INSERT INTO qa_report (id, class_id, phase_id, reported_by, report_type, status, findings, action_items, created_at, updated_at) VALUES
(14, 2, 1, 10, 'PHASE_REVIEW', 'SUBMITTED', 'Hoàn thành Phase 1 đúng tiến độ. Kết quả kiểm tra giữa kỳ khả quan.', 'Chuẩn bị tài liệu cho Phase 2.', '2025-10-28 10:00:00+07', '2025-10-28 10:00:00+07'),
(15, 16, 1, 10, 'PHASE_REVIEW', 'DRAFT', 'Tiến độ chậm hơn dự kiến 2 buổi.', 'Cần bố trí lịch học bù để đuổi kịp chương trình.', '2025-10-30 10:00:00+07', '2025-10-30 10:00:00+07');

-- 8. General Reports
INSERT INTO qa_report (id, class_id, reported_by, report_type, status, findings, action_items, created_at, updated_at) VALUES
(16, 5, 11, 'CLASSROOM_OBSERVATION', 'SUBMITTED', 'Lớp học trực tuyến (Online) diễn ra suôn sẻ, đường truyền ổn định.', 'Đảm bảo duy trì chất lượng kỹ thuật.', '2025-10-15 10:00:00+07', '2025-10-15 10:00:00+07'),
(17, 18, 11, 'STUDENT_FEEDBACK_ANALYSIS', 'SUBMITTED', 'Học viên đánh giá cao sự nhiệt tình của trợ giảng (TA).', 'Khen thưởng đội ngũ TA.', '2025-10-28 10:00:00+07', '2025-10-28 10:00:00+07'),
(18, 3, 10, 'CLO_ACHIEVEMENT_ANALYSIS', 'SUBMITTED', 'Kỹ năng Nói của học viên lớp Online thấp hơn so với lớp Offline.', 'Tăng cường các hoạt động Speaking trong giờ học Online.', '2025-10-20 10:00:00+07', '2025-10-20 10:00:00+07'),
(19, 6, 11, 'ATTENDANCE_ENGAGEMENT_REVIEW', 'SUBMITTED', 'Lớp buổi tối thường xuyên có học viên đến muộn do tắc đường.', 'Xem xét lùi giờ học xuống 15 phút nếu khả thi.', '2025-10-22 10:00:00+07', '2025-10-22 10:00:00+07'),
(20, 4, 10, 'TEACHING_QUALITY_ASSESSMENT', 'DRAFT', 'Chưa có dữ liệu đánh giá (Lớp chưa bắt đầu).', 'Lên kế hoạch dự giờ ngay tuần đầu tiên.', '2025-11-01 10:00:00+07', '2025-11-01 10:00:00+07');

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

-- ========== SECTION 12: NOTIFICATIONS SAMPLE DATA ==========
-- Sample notifications for different users and scenarios
INSERT INTO notification (recipient_id, type, title, message, priority, status, reference_type, reference_id, metadata, created_at, expires_at, read_at) VALUES
-- Academic Affairs notifications (using staff.huong.hn as Academic Affairs staff)
((SELECT id FROM user_account WHERE email = 'staff.huong.hn@tms-edu.vn'), 'REQUEST_APPROVAL', 'Yêu cầu chuyển lớp chờ duyệt', 'Học sinh Nguyen Van A yêu cầu chuyển từ lớp HSK1-101 sang HSK2-201', 'MEDIUM', 'UNREAD', 'StudentRequest', 1, '{"studentName":"Nguyen Van A","fromClass":"HSK1-101","toClass":"HSK2-201"}', CURRENT_TIMESTAMP - INTERVAL '1 day', CURRENT_TIMESTAMP + INTERVAL '7 days', NULL),
((SELECT id FROM user_account WHERE email = 'staff.huong.hn@tms-edu.vn'), 'REQUEST_APPROVAL', 'Yêu cầu nghỉ học chờ duyệt', 'Học sinh Tran Thi B yêu cầu nghỉ buổi học ngày 2025-11-15', 'LOW', 'UNREAD', 'StudentRequest', 2, '{"studentName":"Tran Thi B","absenceDate":"2025-11-15","reason":"Bị ốm"}', CURRENT_TIMESTAMP - INTERVAL '2 hours', CURRENT_TIMESTAMP + INTERVAL '5 days', NULL),

-- Teacher notifications (using existing teacher emails)
((SELECT id FROM user_account WHERE email = 'john.smith@tms-edu.vn'), 'CLASS_REMINDER', 'Nhắc nhở buổi học', 'Lớp HSK1-101 sẽ bắt đầu vào lúc 09:00 ngày mai tại phòng Room-A', 'MEDIUM', 'UNREAD', NULL, NULL, '{"className":"HSK1-101","startTime":"2025-11-26 09:00:00","room":"Room-A"}', CURRENT_TIMESTAMP - INTERVAL '3 hours', CURRENT_TIMESTAMP + INTERVAL '1 day', NULL),
((SELECT id FROM user_account WHERE email = 'john.smith@tms-edu.vn'), 'FEEDBACK_REMINDER', 'Nhắc nhở cung cấp phản hồi', 'Vui lòng cung cấp phản hồi cho khóa học Foundation Level sau khi hoàn thành', 'LOW', 'READ', NULL, NULL, '{"courseName":"Foundation Level","feedbackDue":"2025-11-30"}', CURRENT_TIMESTAMP - INTERVAL '1 week', CURRENT_TIMESTAMP + INTERVAL '3 days', CURRENT_TIMESTAMP - INTERVAL '1 week'),

-- Student notifications (using generated student emails)
((SELECT id FROM user_account WHERE email = 'student.0001@gmail.com'), 'GRADE_NOTIFICATION', 'Thông báo điểm số', 'Điểm bài kiểm tra giữa kỳ của bạn đã có: 8.5/10', 'HIGH', 'UNREAD', 'Score', 1, '{"assessmentName":"Midterm Quiz","score":8.5,"maxScore":10.0}', CURRENT_TIMESTAMP - INTERVAL '6 hours', CURRENT_TIMESTAMP + INTERVAL '30 days', NULL),
((SELECT id FROM user_account WHERE email = 'student.0001@gmail.com'), 'ASSIGNMENT_DEADLINE', 'Hạn nộp bài tập', 'Bài tập cuối tuần sẽ hết hạn vào ngày 2025-11-27 23:59', 'MEDIUM', 'UNREAD', NULL, NULL, '{"assignmentName":"Weekly Homework 3","dueDate":"2025-11-27 23:59:59"}', CURRENT_TIMESTAMP - INTERVAL '12 hours', CURRENT_TIMESTAMP + INTERVAL '2 days', NULL),

-- Center Head notifications
((SELECT id FROM user_account WHERE email = 'head.hn01@tms-edu.vn'), 'LICENSE_WARNING', 'Cảnh báo giấy phép Zoom', 'Giấy phép Zoom cho phòng Room-B sẽ hết hạn trong 15 ngày', 'HIGH', 'UNREAD', NULL, NULL, '{"resourceName":"Room-B","licenseType":"Premium","expiryDate":"2025-12-10","daysRemaining":15}', CURRENT_TIMESTAMP - INTERVAL '4 hours', CURRENT_TIMESTAMP + INTERVAL '15 days', NULL),

-- System notifications for multiple users
((SELECT id FROM user_account WHERE email = 'john.smith@tms-edu.vn'), 'SYSTEM_ALERT', 'Bảo trì hệ thống', 'Hệ thống sẽ bảo trì từ 02:00-04:00 ngày 2025-11-28', 'URGENT', 'UNREAD', NULL, NULL, '{"maintenanceWindow":"2025-11-28 02:00-04:00","affectedServices":["Class Management","Attendance"]}', CURRENT_TIMESTAMP - INTERVAL '30 minutes', CURRENT_TIMESTAMP + INTERVAL '2 days', NULL),
((SELECT id FROM user_account WHERE email = 'staff.huong.hn@tms-edu.vn'), 'SYSTEM_ALERT', 'Bảo trì hệ thống', 'Hệ thống sẽ bảo trì từ 02:00-04:00 ngày 2025-11-28', 'URGENT', 'UNREAD', NULL, NULL, '{"maintenanceWindow":"2025-11-28 02:00-04:00","affectedServices":["Student Requests","Class Scheduling"]}', CURRENT_TIMESTAMP - INTERVAL '30 minutes', CURRENT_TIMESTAMP + INTERVAL '2 days', NULL),

-- Some read notifications for testing
((SELECT id FROM user_account WHERE email = 'student.0002@gmail.com'), 'CLASS_REMINDER', 'Buổi học đã di chuyển', 'Lớp HSK1-102 ngày 2025-11-20 đã được chuyển sang phòng Room-C', 'MEDIUM', 'READ', 'Session', 5, '{"originalRoom":"Room-A","newRoom":"Room-C","sessionDate":"2025-11-20"}', CURRENT_TIMESTAMP - INTERVAL '3 days', CURRENT_TIMESTAMP + INTERVAL '1 day', CURRENT_TIMESTAMP - INTERVAL '1 day'),
((SELECT id FROM user_account WHERE email = 'emma.wilson@tms-edu.vn'), 'REQUEST_APPROVAL', 'Yêu cầu thay thế đã duyệt', 'Yêu cầu dạy thay lớp HSK2-201 ngày 2025-11-15 đã được duyệt', 'MEDIUM', 'READ', 'TeacherRequest', 1, '{"replacementTeacher":"Teacher HSK1","sessionDate":"2025-11-15","status":"APPROVED"}', CURRENT_TIMESTAMP - INTERVAL '5 days', CURRENT_TIMESTAMP + INTERVAL '1 day', CURRENT_TIMESTAMP - INTERVAL '4 days');

-- =========================================
-- POLICY MANAGEMENT: System Policies
-- =========================================

-- 1. Absence Request Lead Time (GLOBAL)
INSERT INTO system_policy (
    policy_key, policy_category, policy_name, description,
    value_type, default_value, current_value, min_value, max_value, unit,
    scope, branch_id, course_id, class_id,
    is_active, version, created_by, updated_by
) VALUES (
    'request.absence.lead_time_days',
    'REQUEST',
    'Số ngày tối thiểu trước khi xin nghỉ',
    'Học viên phải xin nghỉ trước tối thiểu X ngày so với ngày session. Nếu xin nghỉ muộn hơn, hệ thống sẽ từ chối hoặc cảnh báo.',
    'INTEGER',
    '1',
    '1',
    '1',
    '7',
    'days',
    'GLOBAL',
    NULL, NULL, NULL,
    true, 1,
    (SELECT id FROM user_account WHERE email = 'admin@tms-edu.vn' LIMIT 1),
    (SELECT id FROM user_account WHERE email = 'admin@tms-edu.vn' LIMIT 1)
);

-- 2. Teacher Session Suggestion Max Days (GLOBAL)
INSERT INTO system_policy (
    policy_key, policy_category, policy_name, description,
    value_type, default_value, current_value, min_value, max_value, unit,
    scope, branch_id, course_id, class_id,
    is_active, version, created_by, updated_by
) VALUES (
    'teacher.session.suggestion.max_days',
    'TEACHER',
    'Số ngày tối đa gợi ý session cho giáo viên',
    'Số ngày tối đa mà hệ thống sẽ gợi ý sessions cho giáo viên khi họ xem lịch dạy. Giá trị này xác định phạm vi thời gian từ ngày hiện tại.',
    'INTEGER',
    '14',
    '14',
    '1',
    '30',
    'days',
    'GLOBAL',
    NULL, NULL, NULL,
    true, 1,
    (SELECT id FROM user_account WHERE email = 'admin@tms-edu.vn' LIMIT 1),
    (SELECT id FROM user_account WHERE email = 'admin@tms-edu.vn' LIMIT 1)
);

-- 2b. Teacher Modality Change - Require Resource at Create (GLOBAL)
INSERT INTO system_policy (
    policy_key, policy_category, policy_name, description,
    value_type, default_value, current_value, min_value, max_value, unit,
    scope, branch_id, course_id, class_id,
    is_active, version, created_by, updated_by
) VALUES (
    'teacher.modality_change.require_resource',
    'TEACHER',
    'Bắt buộc chọn phòng/link khi đổi hình thức dạy',
    'Nếu true, giáo viên bắt buộc phải chọn resource (phòng hoặc lớp online) khi tạo yêu cầu đổi modality. Nếu false, giáo vụ có thể chọn resource khi duyệt.',
    'BOOLEAN',
    'true',
    'true',
    NULL,
    NULL,
    NULL,
    'GLOBAL',
    NULL, NULL, NULL,
    true, 1,
    (SELECT id FROM user_account WHERE email = 'admin@tms-edu.vn' LIMIT 1),
    (SELECT id FROM user_account WHERE email = 'admin@tms-edu.vn' LIMIT 1)
);

-- 2c. Teacher Reschedule - Require Resource at Create (GLOBAL)
INSERT INTO system_policy (
    policy_key, policy_category, policy_name, description,
    value_type, default_value, current_value, min_value, max_value, unit,
    scope, branch_id, course_id, class_id,
    is_active, version, created_by, updated_by
) VALUES (
    'teacher.reschedule.require_resource_at_create',
    'TEACHER',
    'Bắt buộc chọn phòng/link khi xin dời buổi dạy',
    'Nếu true, giáo viên phải chọn đầy đủ ngày, khung giờ và resource khi tạo yêu cầu dời buổi. Nếu false, giáo viên chỉ cần chọn ngày + khung giờ, resource do giáo vụ chọn khi duyệt.',
    'BOOLEAN',
    'true',
    'true',
    NULL,
    NULL,
    NULL,
    'GLOBAL',
    NULL, NULL, NULL,
    true, 1,
    (SELECT id FROM user_account WHERE email = 'admin@tms-edu.vn' LIMIT 1),
    (SELECT id FROM user_account WHERE email = 'admin@tms-edu.vn' LIMIT 1)
);

-- 2d. Teacher Reschedule - Allow Same Day (GLOBAL)
INSERT INTO system_policy (
    policy_key, policy_category, policy_name, description,
    value_type, default_value, current_value, min_value, max_value, unit,
    scope, branch_id, course_id, class_id,
    is_active, version, created_by, updated_by
) VALUES (
    'teacher.reschedule.allow_same_day',
    'TEACHER',
    'Cho phép xin dời buổi trong cùng ngày',
    'Nếu true, giáo viên được phép tạo yêu cầu dời buổi trong ngày diễn ra session. Nếu false, bắt buộc xin trước ngày diễn ra session.',
    'BOOLEAN',
    'false',
    'false',
    NULL,
    NULL,
    NULL,
    'GLOBAL',
    NULL, NULL, NULL,
    true, 1,
    (SELECT id FROM user_account WHERE email = 'admin@tms-edu.vn' LIMIT 1),
    (SELECT id FROM user_account WHERE email = 'admin@tms-edu.vn' LIMIT 1)
);

-- 2d.2 Teacher Reschedule - Minimum Days Ahead for New Date (GLOBAL)
INSERT INTO system_policy (
    policy_key, policy_category, policy_name, description,
    value_type, default_value, current_value, min_value, max_value, unit,
    scope, branch_id, course_id, class_id,
    is_active, version, created_by, updated_by
) VALUES (
    'teacher.reschedule.min_days_ahead',
    'TEACHER',
    'Số ngày tối thiểu cho ngày dời mới',
    'Ngày được chọn để dời buổi phải cách ngày hiện tại ít nhất X ngày (trừ khi cho phép same-day).',
    'INTEGER',
    '1',
    '1',
    '0',
    '7',
    'days',
    'GLOBAL',
    NULL, NULL, NULL,
    true, 1,
    (SELECT id FROM user_account WHERE email = 'admin@tms-edu.vn' LIMIT 1),
    (SELECT id FROM user_account WHERE email = 'admin@tms-edu.vn' LIMIT 1)
);

-- 2d.3 Teacher Reschedule - Max Requests Per Course (GLOBAL)
INSERT INTO system_policy (
    policy_key, policy_category, policy_name, description,
    value_type, default_value, current_value, min_value, max_value, unit,
    scope, branch_id, course_id, class_id,
    is_active, version, created_by, updated_by
) VALUES (
    'teacher.reschedule.max_per_course',
    'TEACHER',
    'Số lần tối đa xin dời trong một khóa học',
    'Giới hạn số lượng yêu cầu dời buổi cho cùng một khóa học để tránh thay đổi lịch quá nhiều.',
    'INTEGER',
    '2',
    '2',
    '0',
    '20',
    'times',
    'GLOBAL',
    NULL, NULL, NULL,
    true, 1,
    (SELECT id FROM user_account WHERE email = 'admin@tms-edu.vn' LIMIT 1),
    (SELECT id FROM user_account WHERE email = 'admin@tms-edu.vn' LIMIT 1)
);

-- 2e. Teacher Request - Min Days Before Session (GLOBAL) - Unified policy for all request types
INSERT INTO system_policy (
    policy_key, policy_category, policy_name, description,
    value_type, default_value, current_value, min_value, max_value, unit,
    scope, branch_id, course_id, class_id,
    is_active, version, created_by, updated_by
) VALUES (
    'teacher.request.min_days_before_session',
    'TEACHER',
    'Số ngày tối thiểu trước khi tạo yêu cầu',
    'Giáo viên phải gửi yêu cầu (đổi lịch, dạy thay, đổi phương thức) trước ít nhất X ngày so với ngày diễn ra session hoặc ngày khai giảng.',
    'INTEGER',
    '1',
    '1',
    '0',
    '30',
    'days',
    'GLOBAL',
    NULL, NULL, NULL,
    true, 1,
    (SELECT id FROM user_account WHERE email = 'admin@tms-edu.vn' LIMIT 1),
    (SELECT id FROM user_account WHERE email = 'admin@tms-edu.vn' LIMIT 1)
);

-- 2f. Teacher Reschedule - Max Requests Per Month (GLOBAL)
INSERT INTO system_policy (
    policy_key, policy_category, policy_name, description,
    value_type, default_value, current_value, min_value, max_value, unit,
    scope, branch_id, course_id, class_id,
    is_active, version, created_by, updated_by
) VALUES (
    'teacher.reschedule.max_per_month',
    'TEACHER',
    'Số lần tối đa xin dời buổi mỗi tháng',
    'Giới hạn số lượng yêu cầu dời buổi mà giáo viên có thể gửi trong một tháng (tính cả yêu cầu đang chờ duyệt và đã duyệt).',
    'INTEGER',
    '2',
    '2',
    '0',
    '10',
    'times',
    'GLOBAL',
    NULL, NULL, NULL,
    true, 1,
    (SELECT id FROM user_account WHERE email = 'admin@tms-edu.vn' LIMIT 1),
    (SELECT id FROM user_account WHERE email = 'admin@tms-edu.vn' LIMIT 1)
);

-- 2g. Teacher Modality Change - Max Requests Per Course (GLOBAL)
INSERT INTO system_policy (
    policy_key, policy_category, policy_name, description,
    value_type, default_value, current_value, min_value, max_value, unit,
    scope, branch_id, course_id, class_id,
    is_active, version, created_by, updated_by
) VALUES (
    'teacher.modality_change.max_per_course',
    'TEACHER',
    'Số lần tối đa đổi hình thức dạy trên một khóa học',
    'Giới hạn số lượng yêu cầu đổi hình thức dạy mà giáo viên có thể gửi trong cùng một khóa học.',
    'INTEGER',
    '1',
    '1',
    '0',
    '5',
    'times',
    'GLOBAL',
    NULL, NULL, NULL,
    true, 1,
    (SELECT id FROM user_account WHERE email = 'admin@tms-edu.vn' LIMIT 1),
    (SELECT id FROM user_account WHERE email = 'admin@tms-edu.vn' LIMIT 1)
);

-- 2g. Teacher Modality Change - Allow After Start (GLOBAL)
INSERT INTO system_policy (
    policy_key, policy_category, policy_name, description,
    value_type, default_value, current_value, min_value, max_value, unit,
    scope, branch_id, course_id, class_id,
    is_active, version, created_by, updated_by
) VALUES (
    'teacher.modality_change.allow_after_start',
    'TEACHER',
    'Cho phép đổi hình thức sau khi khóa học bắt đầu',
    'Nếu bật, giáo viên vẫn có thể gửi yêu cầu đổi hình thức dạy sau khi khóa học đã khai giảng.',
    'BOOLEAN',
    'false',
    'false',
    NULL,
    NULL,
    NULL,
    'GLOBAL',
    NULL, NULL, NULL,
    true, 1,
    (SELECT id FROM user_account WHERE email = 'admin@tms-edu.vn' LIMIT 1),
    (SELECT id FROM user_account WHERE email = 'admin@tms-edu.vn' LIMIT 1)
);

-- 2h. Teacher Replacement - Max Requests Per Month (GLOBAL)
INSERT INTO system_policy (
    policy_key, policy_category, policy_name, description,
    value_type, default_value, current_value, min_value, max_value, unit,
    scope, branch_id, course_id, class_id,
    is_active, version, created_by, updated_by
) VALUES (
    'teacher.replacement.max_per_month',
    'TEACHER',
    'Số lần tối đa xin thay giáo viên mỗi tháng',
    'Giới hạn số lượng yêu cầu nhờ giáo viên khác dạy thay mà giáo viên có thể gửi trong một tháng.',
    'INTEGER',
    '3',
    '3',
    '0',
    '10',
    'times',
    'GLOBAL',
    NULL, NULL, NULL,
    true, 1,
    (SELECT id FROM user_account WHERE email = 'admin@tms-edu.vn' LIMIT 1),
    (SELECT id FROM user_account WHERE email = 'admin@tms-edu.vn' LIMIT 1)
);

-- 2i. Teacher Request - Require Reason (GLOBAL)
INSERT INTO system_policy (
    policy_key, policy_category, policy_name, description,
    value_type, default_value, current_value, min_value, max_value, unit,
    scope, branch_id, course_id, class_id,
    is_active, version, created_by, updated_by
) VALUES (
    'teacher.request.require_reason',
    'TEACHER',
    'Bắt buộc nhập lý do cho tất cả yêu cầu',
    'Nếu bật (true), giáo viên bắt buộc phải nhập lý do khi tạo bất kỳ yêu cầu nào (dời buổi, đổi hình thức, nhờ dạy thay). Nếu tắt (false), có thể để trống lý do.',
    'BOOLEAN',
    'true',
    'true',
    NULL,
    NULL,
    NULL,
    'GLOBAL',
    NULL, NULL, NULL,
    true, 1,
    (SELECT id FROM user_account WHERE email = 'admin@tms-edu.vn' LIMIT 1),
    (SELECT id FROM user_account WHERE email = 'admin@tms-edu.vn' LIMIT 1)
);

-- 2j. Teacher Request - Minimum Reason Length (GLOBAL)
INSERT INTO system_policy (
    policy_key, policy_category, policy_name, description,
    value_type, default_value, current_value, min_value, max_value, unit,
    scope, branch_id, course_id, class_id,
    is_active, version, created_by, updated_by
) VALUES (
    'teacher.request.reason_min_length',
    'TEACHER',
    'Độ dài tối thiểu lý do yêu cầu',
    'Giáo viên phải nhập lý do có độ dài tối thiểu X ký tự khi tạo yêu cầu để đảm bảo thông tin đủ chi tiết cho giáo vụ xem xét.',
    'INTEGER',
    '15',
    '15',
    '10',
    '500',
    'characters',
    'GLOBAL',
    NULL, NULL, NULL,
    true, 1,
    (SELECT id FROM user_account WHERE email = 'admin@tms-edu.vn' LIMIT 1),
    (SELECT id FROM user_account WHERE email = 'admin@tms-edu.vn' LIMIT 1)
);

-- 2k. Teacher Request - Maximum Requests Per Day (GLOBAL)
INSERT INTO system_policy (
    policy_key, policy_category, policy_name, description,
    value_type, default_value, current_value, min_value, max_value, unit,
    scope, branch_id, course_id, class_id,
    is_active, version, created_by, updated_by
) VALUES (
    'teacher.request.max_per_day',
    'TEACHER',
    'Số lượng yêu cầu tối đa mỗi ngày',
    'Giới hạn số yêu cầu giáo viên có thể tạo trong một ngày để tránh spam hoặc thao tác quá nhiều.',
    'INTEGER',
    '2',
    '2',
    '1',
    '20',
    'times',
    'GLOBAL',
    NULL, NULL, NULL,
    true, 1,
    (SELECT id FROM user_account WHERE email = 'admin@tms-edu.vn' LIMIT 1),
    (SELECT id FROM user_account WHERE email = 'admin@tms-edu.vn' LIMIT 1)
);

-- 3. Max Transfers Per Course (GLOBAL)
INSERT INTO system_policy (
    policy_key, policy_category, policy_name, description,
    value_type, default_value, current_value, min_value, max_value, unit,
    scope, branch_id, course_id, class_id,
    is_active, version, created_by, updated_by
) VALUES (
    'request.transfer.max_per_course',
    'REQUEST',
    'Số lần chuyển lớp tối đa mỗi khóa học',
    'Học viên chỉ được chuyển lớp tối đa X lần trong một khóa học. Sau đó phải hoàn thành khóa học hiện tại hoặc hủy đăng ký.',
    'INTEGER',
    '1',
    '1',
    '0',
    '5',
    'times',
    'GLOBAL',
    NULL, NULL, NULL,
    true, 1,
    (SELECT id FROM user_account WHERE email = 'admin@tms-edu.vn' LIMIT 1),
    (SELECT id FROM user_account WHERE email = 'admin@tms-edu.vn' LIMIT 1)
);

-- 4. Attendance Lock Time (GLOBAL)
INSERT INTO system_policy (
    policy_key, policy_category, policy_name, description,
    value_type, default_value, current_value, min_value, max_value, unit,
    scope, branch_id, course_id, class_id,
    is_active, version, created_by, updated_by
) VALUES (
    'attendance.lock.hours_after_session',
    'ATTENDANCE',
    'Thời gian khóa điểm danh sau khi session kết thúc',
    'Sau X giờ kể từ khi session kết thúc, hệ thống sẽ khóa việc chỉnh sửa điểm danh. Giáo viên không thể sửa điểm danh sau thời gian này.',
    'INTEGER',
    '24',
    '24',
    '1',
    '168',
    'hours',
    'GLOBAL',
    NULL, NULL, NULL,
    true, 1,
    (SELECT id FROM user_account WHERE email = 'admin@tms-edu.vn' LIMIT 1),
    (SELECT id FROM user_account WHERE email = 'admin@tms-edu.vn' LIMIT 1)
);

-- 5. Auto Mark Absent (GLOBAL)
INSERT INTO system_policy (
    policy_key, policy_category, policy_name, description,
    value_type, default_value, current_value, min_value, max_value, unit,
    scope, branch_id, course_id, class_id,
    is_active, version, created_by, updated_by
) VALUES (
    'attendance.auto_absent.enabled',
    'ATTENDANCE',
    'Tự động đánh vắng mặt khi chưa điểm danh',
    'Nếu bật (true), hệ thống sẽ tự động đánh học viên là vắng mặt (ABSENT) nếu giáo viên chưa điểm danh sau khi session kết thúc.',
    'BOOLEAN',
    'true',
    'true',
    NULL,
    NULL,
    NULL,
    'GLOBAL',
    NULL, NULL, NULL,
    true, 1,
    (SELECT id FROM user_account WHERE email = 'admin@tms-edu.vn' LIMIT 1),
    (SELECT id FROM user_account WHERE email = 'admin@tms-edu.vn' LIMIT 1)
);

-- 6. Absence Threshold Percent (GLOBAL)
INSERT INTO system_policy (
    policy_key, policy_category, policy_name, description,
    value_type, default_value, current_value, min_value, max_value, unit,
    scope, branch_id, course_id, class_id,
    is_active, version, created_by, updated_by
) VALUES (
    'request.absence.threshold_percent',
    'REQUEST',
    'Ngưỡng vắng mặt cảnh báo (%)',
    'Nếu học viên vắng mặt vượt quá X% tổng số sessions, hệ thống sẽ cảnh báo và có thể từ chối yêu cầu nghỉ tiếp.',
    'DOUBLE',
    '20.0',
    '20.0',
    '0.0',
    '100.0',
    'percent',
    'GLOBAL',
    NULL, NULL, NULL,
    true, 1,
    (SELECT id FROM user_account WHERE email = 'admin@tms-edu.vn' LIMIT 1),
    (SELECT id FROM user_account WHERE email = 'admin@tms-edu.vn' LIMIT 1)
);

-- 7. Request Reason Min Length (GLOBAL)
INSERT INTO system_policy (
    policy_key, policy_category, policy_name, description,
    value_type, default_value, current_value, min_value, max_value, unit,
    scope, branch_id, course_id, class_id,
    is_active, version, created_by, updated_by
) VALUES (
    'request.absence.reason_min_length',
    'REQUEST',
    'Độ dài tối thiểu lý do xin nghỉ',
    'Học viên phải nhập lý do xin nghỉ tối thiểu X ký tự.',
    'INTEGER',
    '10',
    '10',
    '5',
    '500',
    'characters',
    'GLOBAL',
    NULL, NULL, NULL,
    true, 1,
    (SELECT id FROM user_account WHERE email = 'admin@tms-edu.vn' LIMIT 1),
    (SELECT id FROM user_account WHERE email = 'admin@tms-edu.vn' LIMIT 1)
);

-- 8. Class Min Enrollment (GLOBAL)
INSERT INTO system_policy (
    policy_key, policy_category, policy_name, description,
    value_type, default_value, current_value, min_value, max_value, unit,
    scope, branch_id, course_id, class_id,
    is_active, version, created_by, updated_by
) VALUES (
    'class.min_enrollment',
    'CLASS',
    'Số học viên tối thiểu để mở lớp',
    'Lớp học phải có tối thiểu X học viên đăng ký mới được mở. Nếu dưới ngưỡng này, hệ thống sẽ cảnh báo hoặc tự động hủy lớp.',
    'INTEGER',
    '5',
    '5',
    '1',
    '50',
    'students',
    'GLOBAL',
    NULL, NULL, NULL,
    true, 1,
    (SELECT id FROM user_account WHERE email = 'admin@tms-edu.vn' LIMIT 1),
    (SELECT id FROM user_account WHERE email = 'admin@tms-edu.vn' LIMIT 1)
);

-- Insert initial CREATE history records for all policies
INSERT INTO policy_history (policy_id, old_value, new_value, changed_by, reason, version, change_type)
SELECT 
    id,
    NULL,
    current_value,
    created_by,
    'Initial policy creation',
    version,
    'CREATE'
FROM system_policy;

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
-- - 10 Sample notifications covering all types and priorities
-- - 17 System Policies (REQUEST, ATTENDANCE, CLASS, TEACHER categories)
-- - Edge cases: capacity limits, mid-course enrollment, transfers, perfect attendance
-- =========================================

-- ========== QA REPORT DOCUMENTATION ==========
--
-- QA Report Status Values (must match QAReportStatus enum):
-- - 'draft': Bản nháp (work in progress, not submitted)
-- - 'submitted': Đã nộp (finalized, submitted for review)
--
-- QA Report Type Values (must match QAReportType enum):
-- - 'classroom_observation': Classroom Observation
-- - 'phase_review': Phase Review
-- - 'clo_achievement_analysis': CLO Achievement Analysis
-- - 'student_feedback_analysis': Student Feedback Analysis
-- - 'attendance_engagement_review': Attendance & Engagement Review
-- - 'teaching_quality_assessment': Teaching Quality Assessment
--
-- Database Constraints:
-- - qa_report.status CHECK (status IN ('draft', 'submitted'))
-- - qa_report.report_type CHECK (report_type IN ('classroom_observation', 'phase_review', 'clo_achievement_analysis', 'student_feedback_analysis', 'attendance_engagement_review', 'teaching_quality_assessment'))
--
-- ========== DATABASE RESET INSTRUCTIONS ==========
--
-- To reset database with updated schema and seed data:
--
-- # Method 1: PowerShell (Recommended)
-- docker exec -it tms-postgres psql -U postgres -d tms -c "DROP DATABASE IF EXISTS tms;"
-- docker exec -it tms-postgres psql -U postgres -c "CREATE DATABASE tms;"
-- Get-Content "src/main/resources/schema.sql", "src/main/resources/seed-data.sql" | docker exec -i tms-postgres psql -U postgres -d tms
--
-- # Method 2: Git Bash / WSL
-- docker exec -it tms-postgres psql -U postgres -d tms -c "DROP DATABASE IF EXISTS tms;"
-- docker exec -it tms-postgres psql -U postgres -c "CREATE DATABASE tms;"
-- cat src/main/resources/schema.sql src/main/resources/seed-data.sql | docker exec -i tms-postgres psql -U postgres -d tms
--
-- # Method 3: Direct SQL (if database already exists)
-- docker exec -it tms-postgres psql -U postgres -d tms -c "\i schema.sql"
-- docker exec -it tms-postgres psql -U postgres -d tms -c "\i seed-data.sql"
--
-- =========================================
