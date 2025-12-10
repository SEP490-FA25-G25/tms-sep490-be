TRUNCATE TABLE student_feedback_response CASCADE;
TRUNCATE TABLE student_feedback CASCADE;
TRUNCATE TABLE qa_report CASCADE;
TRUNCATE TABLE policy_history CASCADE;
TRUNCATE TABLE system_policy CASCADE;
TRUNCATE TABLE score CASCADE;
TRUNCATE TABLE assessment CASCADE;
TRUNCATE TABLE subject_assessment_clo_mapping CASCADE;
TRUNCATE TABLE subject_assessment CASCADE;
TRUNCATE TABLE notification CASCADE;
TRUNCATE TABLE teacher_request CASCADE;
TRUNCATE TABLE student_request CASCADE;
TRUNCATE TABLE student_session CASCADE;
TRUNCATE TABLE enrollment CASCADE;
TRUNCATE TABLE teaching_slot CASCADE;
TRUNCATE TABLE teacher_availability CASCADE;
TRUNCATE TABLE session_resource CASCADE;
TRUNCATE TABLE session CASCADE;
TRUNCATE TABLE subject_session_clo_mapping CASCADE;
TRUNCATE TABLE plo_clo_mapping CASCADE;
TRUNCATE TABLE clo CASCADE;
TRUNCATE TABLE subject_material CASCADE;
TRUNCATE TABLE subject_session CASCADE;
TRUNCATE TABLE subject_phase CASCADE;
TRUNCATE TABLE "class" CASCADE;
TRUNCATE TABLE teacher_skill CASCADE;
TRUNCATE TABLE student CASCADE;
TRUNCATE TABLE teacher CASCADE;
TRUNCATE TABLE user_branches CASCADE;
TRUNCATE TABLE user_role CASCADE;
TRUNCATE TABLE resource CASCADE;
TRUNCATE TABLE time_slot_template CASCADE;
TRUNCATE TABLE subject CASCADE;
TRUNCATE TABLE plo CASCADE;
TRUNCATE TABLE level CASCADE;
TRUNCATE TABLE curriculum CASCADE;
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
SELECT setval('curriculum_id_seq', 1, false);
SELECT setval('level_id_seq', 1, false);
SELECT setval('plo_id_seq', 1, false);
SELECT setval('subject_id_seq', 10, false);
SELECT setval('subject_phase_id_seq', 23, false);
SELECT setval('clo_id_seq', 29, false);
SELECT setval('subject_session_id_seq', 1, false);
SELECT setval('subject_assessment_id_seq', 1, false);
SELECT setval('class_id_seq', 31, false);
SELECT setval('session_id_seq', 1, false);
SELECT setval('assessment_id_seq', 1, false);
SELECT setval('score_id_seq', 1, false);
SELECT setval('student_request_id_seq', 1, false);
SELECT setval('teacher_request_id_seq', 1, false);
SELECT setval('student_feedback_id_seq', 1, false);
SELECT setval('student_feedback_response_id_seq', 1, false);
SELECT setval('qa_report_id_seq', 1, false);
SELECT setval('subject_material_id_seq', 1, false);
SELECT setval('time_slot_template_id_seq', 26, false);
SELECT setval('resource_id_seq', 1, false);
SELECT setval('replacement_skill_assessment_id_seq', 1, false);
SELECT setval('feedback_question_id_seq', 1, false);

INSERT INTO center (id, code, name, description, phone, email, address, created_at, updated_at) VALUES
(1, 'TMS-EDU', 'TMS Education Group', 'Leading language education group in Vietnam', '+84-24-3999-8888', 'info@tms-edu.vn', '123 Nguyen Trai, Thanh Xuan, Ha Noi', '2024-01-01 00:00:00+07', '2024-01-01 00:00:00+07');

INSERT INTO role (id, code, name) VALUES
(1, 'ADMIN', 'System Administrator'),
(2, 'MANAGER', 'Manager'),
(3, 'CENTER_HEAD', 'Center Head'),
(4, 'SUBJECT_LEADER', 'Subject Leader'),
(5, 'ACADEMIC_AFFAIR', 'Academic Affair'),
(6, 'TEACHER', 'Teacher'),
(7, 'STUDENT', 'Student'),
(8, 'QA', 'Quality Assurance');

-- Password: '12345678' hashed with BCrypt (cost factor 10)
INSERT INTO user_account (id, email, phone, full_name, gender, dob, address, password_hash, status, created_at, updated_at) VALUES
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
(35, 'isabella.young@tms-edu.vn', '0912001016', 'Isabella Young',  'FEMALE', '1990-04-25', 'TP. HCM', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-02-01 00:00:00+07', '2024-02-01 00:00:00+07'),
(36, 'toeic.peter@tms-edu.vn', '0912001017', 'Peter Nguyen',  'MALE', '1985-05-15', 'Ha Noi', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-02-01 00:00:00+07', '2024-02-01 00:00:00+07'),
(37, 'toeic.mary@tms-edu.vn', '0912001018', 'Mary Tran',  'FEMALE', '1988-09-20', 'Ha Noi', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-02-01 00:00:00+07', '2024-02-01 00:00:00+07'),
-- Weekend-only Teachers (HN)
(38, 'weekend.tom@tms-edu.vn', '0912001019', 'Tom Weekend',  'MALE', '1990-03-10', 'Ha Noi', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-02-01 00:00:00+07', '2024-02-01 00:00:00+07'),
(39, 'weekend.jane@tms-edu.vn', '0912001020', 'Jane Saturday',  'FEMALE', '1992-07-25', 'Ha Noi', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-02-01 00:00:00+07', '2024-02-01 00:00:00+07'),
-- Evening-only Teachers (HN) - Part-time
(40, 'evening.kevin@tms-edu.vn', '0912001021', 'Kevin Night',  'MALE', '1987-11-08', 'Ha Noi', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-02-01 00:00:00+07', '2024-02-01 00:00:00+07'),
-- TOEIC Specialists (HCM)
(41, 'toeic.robert@tms-edu.vn', '0912001022', 'Robert Lee',  'MALE', '1986-02-14', 'TP. HCM', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-02-01 00:00:00+07', '2024-02-01 00:00:00+07'),
-- Weekend-only Teachers (HCM)
(42, 'weekend.david@tms-edu.vn', '0912001023', 'David Sunday',  'MALE', '1991-06-30', 'TP. HCM', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-02-01 00:00:00+07', '2024-02-01 00:00:00+07'),
-- Inactive Teacher (for testing filtering)
(43, 'inactive.teacher@tms-edu.vn', '0912001024', 'Sam Inactive',  'MALE', '1988-04-01', 'Ha Noi', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'INACTIVE', '2024-02-01 00:00:00+07', '2024-02-01 00:00:00+07');

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

-- Branches
INSERT INTO branch (id, center_id, code, name, address, phone, email, district, city, status, opening_date, created_at, updated_at) VALUES
(1, 1, 'HN01', 'TMS Ha Noi Branch', '456 Lang Ha, Dong Da, Ha Noi', '+84-24-3888-9999', 'hanoi01@tms-edu.vn', 'Dong Da', 'Ha Noi', 'ACTIVE', '2024-01-15', '2024-01-15 00:00:00+07', '2024-01-15 00:00:00+07'),
(2, 1, 'HCM01', 'TMS Ho Chi Minh Branch', '789 Le Loi, Quan 1, TP. HCM', '+84-28-3777-6666', 'hcm01@tms-edu.vn', 'Quan 1', 'TP. HCM', 'ACTIVE', '2024-03-01', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07');

-- Subjects
INSERT INTO curriculum (id, code, name, description, language, status, created_by, created_at, updated_at) VALUES
(1, 'IELTS', 'International English Language Testing System', 'Comprehensive IELTS preparation courses', 'English', 'ACTIVE', 5, '2024-06-01 00:00:00+07', '2024-06-01 00:00:00+07'),
(2, 'TOEIC', 'Test of English for International Communication', 'Business English certification courses', 'English', 'ACTIVE', 5, '2024-06-01 00:00:00+07', '2024-06-01 00:00:00+07'),
(3, 'JLPT', 'Japanese Language Proficiency Test', 'Japanese language certification preparation', 'Japanese', 'ACTIVE', 5, '2024-06-01 00:00:00+07', '2024-06-01 00:00:00+07');

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
(16, 2, 'HCM Weekend Afternoon 2.5h', '14:00:00', '16:30:00', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),

-- Additional 1.5h Time Slots for Ha Noi Branch
(17, 1, 'HN Morning Slot 1 1.5h', '08:40:00', '10:10:00', '2024-01-15 00:00:00+07', '2024-01-15 00:00:00+07'),
(18, 1, 'HN Morning Slot 2 1.5h', '10:20:00', '11:50:00', '2024-01-15 00:00:00+07', '2024-01-15 00:00:00+07'),
(19, 1, 'HN Afternoon Slot 1 1.5h', '12:40:00', '14:10:00', '2024-01-15 00:00:00+07', '2024-01-15 00:00:00+07'),
(20, 1, 'HN Afternoon Slot 2 1.5h', '14:20:00', '15:50:00', '2024-01-15 00:00:00+07', '2024-01-15 00:00:00+07'),
(21, 1, 'HN Late Afternoon 1.5h', '16:00:00', '17:30:00', '2024-01-15 00:00:00+07', '2024-01-15 00:00:00+07'),

-- Additional 1.5h Time Slots for Ho Chi Minh Branch
(22, 2, 'HCM Morning Slot 1 1.5h', '08:40:00', '10:10:00', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(23, 2, 'HCM Morning Slot 2 1.5h', '10:20:00', '11:50:00', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(24, 2, 'HCM Afternoon Slot 1 1.5h', '12:40:00', '14:10:00', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(25, 2, 'HCM Afternoon Slot 2 1.5h', '14:20:00', '15:50:00', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(26, 2, 'HCM Late Afternoon 1.5h', '16:00:00', '17:30:00', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07');

-- Resources (Rooms & Zoom) - Comprehensive for Testing
INSERT INTO resource (id, branch_id, resource_type, code, name, capacity, capacity_override, created_at, updated_at) VALUES
-- Ha Noi Branch - Physical Rooms (Various capacities)
(1, 1, 'ROOM', 'HN01-R101', 'Ha Noi Room 101', 20, 25, '2024-01-15 00:00:00+07', '2024-01-15 00:00:00+07'),
(2, 1, 'ROOM', 'HN01-R102', 'Ha Noi Room 102', 15, NULL, '2024-01-15 00:00:00+07', '2024-01-15 00:00:00+07'),
(3, 1, 'ROOM', 'HN01-R201', 'Ha Noi Room 201', 25, NULL, '2024-01-15 00:00:00+07', '2024-01-15 00:00:00+07'),
-- Ha Noi Branch - Small Rooms (for VIP/1-1 classes)
(9, 1, 'ROOM', 'HN01-R301', 'Ha Noi VIP Room 301', 5, NULL, '2024-01-15 00:00:00+07', '2024-01-15 00:00:00+07'),
(10, 1, 'ROOM', 'HN01-R302', 'Ha Noi Study Room 302', 8, 10, '2024-01-15 00:00:00+07', '2024-01-15 00:00:00+07'),
-- Ha Noi Branch - Large Rooms (for workshops/seminars)
(11, 1, 'ROOM', 'HN01-R401', 'Ha Noi Conference Hall', 50, 60, '2024-01-15 00:00:00+07', '2024-01-15 00:00:00+07'),
(12, 1, 'ROOM', 'HN01-R402', 'Ha Noi Seminar Room', 35, NULL, '2024-01-15 00:00:00+07', '2024-01-15 00:00:00+07'),
-- Ha Noi Branch - Virtual (Multiple Zoom accounts)
(4, 1, 'VIRTUAL', 'HN01-Z01', 'Ha Noi Zoom 01', 100, NULL, '2024-01-15 00:00:00+07', '2024-01-15 00:00:00+07'),
(13, 1, 'VIRTUAL', 'HN01-Z02', 'Ha Noi Zoom 02', 100, NULL, '2024-01-15 00:00:00+07', '2024-01-15 00:00:00+07'),
(14, 1, 'VIRTUAL', 'HN01-Z03', 'Ha Noi Zoom 03 (Premium)', 300, NULL, '2024-01-15 00:00:00+07', '2024-01-15 00:00:00+07'),
(15, 1, 'VIRTUAL', 'HN01-GM01', 'Ha Noi Google Meet 01', 100, NULL, '2024-01-15 00:00:00+07', '2024-01-15 00:00:00+07'),
-- Ho Chi Minh Branch - Physical Rooms
(5, 2, 'ROOM', 'HCM01-R101', 'HCM Room 101', 20, NULL, '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(6, 2, 'ROOM', 'HCM01-R102', 'HCM Room 102', 20, NULL, '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(7, 2, 'ROOM', 'HCM01-R201', 'HCM Room 201', 25, NULL, '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
-- HCM Branch - Small Rooms
(16, 2, 'ROOM', 'HCM01-R301', 'HCM VIP Room 301', 6, NULL, '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(17, 2, 'ROOM', 'HCM01-R302', 'HCM Private Room 302', 4, NULL, '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
-- HCM Branch - Large Rooms
(18, 2, 'ROOM', 'HCM01-R401', 'HCM Training Center', 40, 50, '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
-- Ho Chi Minh Branch - Virtual
(8, 2, 'VIRTUAL', 'HCM01-Z01', 'HCM Zoom 01', 100, NULL, '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(19, 2, 'VIRTUAL', 'HCM01-Z02', 'HCM Zoom 02', 100, NULL, '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(20, 2, 'VIRTUAL', 'HCM01-MS01', 'HCM MS Teams 01', 250, NULL, '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07');

-- User Role & Branch Assignments
INSERT INTO user_role (user_id, role_id) VALUES
(1,1), (2,2), (3,3), (4,3), (5,4), (6,5), (7,5), (8,5), (9,5), (10,8), (11,8);
-- Teachers (original + new)
INSERT INTO user_role (user_id, role_id) SELECT id, 6 FROM user_account WHERE id >= 20 AND id <= 43;
-- Students
INSERT INTO user_role (user_id, role_id) SELECT id, 7 FROM user_account WHERE id >= 101;

INSERT INTO user_branches (user_id, branch_id, assigned_by) VALUES
-- Staff assignments
(1,1,1), (1,2,1), (2,1,1), (2,2,1), (3,1,2), (3,2,2), (4,2,2), (5,1,2), (6,1,2), (6,2,2), (7,1,2), (8,2,4), (9,2,4), (10,1,2), (11,2,4);
-- Teachers - HN (original)
INSERT INTO user_branches (user_id, branch_id, assigned_by) SELECT id, 1, 6 FROM user_account WHERE id BETWEEN 20 AND 27;
-- Teachers - HCM (original)
INSERT INTO user_branches (user_id, branch_id, assigned_by) SELECT id, 2, 8 FROM user_account WHERE id BETWEEN 28 AND 35;
-- New Teachers - HN (TOEIC, Weekend, Evening)
INSERT INTO user_branches (user_id, branch_id, assigned_by) VALUES (36, 1, 6), (37, 1, 6), (38, 1, 6), (39, 1, 6), (40, 1, 6), (43, 1, 6);
-- New Teachers - HCM (TOEIC, Weekend)
INSERT INTO user_branches (user_id, branch_id, assigned_by) VALUES (41, 2, 8), (42, 2, 8);
-- Students - HN
INSERT INTO user_branches (user_id, branch_id, assigned_by) SELECT id, 1, 6 FROM user_account WHERE id BETWEEN 101 AND 150;
-- Students - HCM
INSERT INTO user_branches (user_id, branch_id, assigned_by) SELECT id, 2, 8 FROM user_account WHERE id > 150;

-- Teachers & Students
INSERT INTO teacher (id, user_account_id, employee_code, hire_date, contract_type, created_at, updated_at)
SELECT (id - 19), id, 'TCH-' || LPAD((id-19)::text, 3, '0'), '2024-02-01', CASE WHEN id % 2 = 0 THEN 'full-time' ELSE 'part-time' END, '2024-02-01 00:00:00+07', '2024-02-01 00:00:00+07'
FROM user_account WHERE id BETWEEN 20 AND 43;

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

-- Each teacher has all 5 skills: GENERAL, LISTENING, READING, WRITING, SPEAKING
-- Level represents IELTS band scores: 6.5-9.0 scale

INSERT INTO teacher_skill (teacher_id, skill, specialization, language, level) VALUES

-- ===== HANOI TEACHERS (1-8) =====

-- Teacher 1 (John Smith): GENERAL EXCELLENCE (Band 9.0 overall)
(1, 'GENERAL', 'IELTS', 'English', 9.0),
(1, 'LISTENING', 'IELTS', 'English', 9.0),
(1, 'READING', 'IELTS', 'English', 8.5),
(1, 'WRITING', 'IELTS', 'English', 8.5),
(1, 'SPEAKING', 'IELTS', 'English', 9.0),

-- Teacher 2 (Emma Wilson): WRITING & READING SPECIALIST (Band 8.5 overall)
(2, 'GENERAL', 'IELTS', 'English', 8.0),
(2, 'LISTENING', 'IELTS', 'English', 8.0),
(2, 'READING', 'IELTS', 'English', 9.0),
(2, 'WRITING', 'IELTS', 'English', 9.0),
(2, 'SPEAKING', 'IELTS', 'English', 7.5),

-- Teacher 3 (David Lee): SPEAKING SPECIALIST (Band 8.0 overall)
(3, 'GENERAL', 'IELTS', 'English', 8.0),
(3, 'LISTENING', 'IELTS', 'English', 8.0),
(3, 'READING', 'IELTS', 'English', 7.5),
(3, 'WRITING', 'IELTS', 'English', 7.5),
(3, 'SPEAKING', 'IELTS', 'English', 9.0),

-- Teacher 4 (Sarah Johnson): BALANCED PROFILE (Band 8.0 overall)
(4, 'GENERAL', 'IELTS', 'English', 8.0),
(4, 'LISTENING', 'IELTS', 'English', 8.0),
(4, 'READING', 'IELTS', 'English', 8.0),
(4, 'WRITING', 'IELTS', 'English', 8.0),
(4, 'SPEAKING', 'IELTS', 'English', 8.0),

-- Teacher 5 (Michael Brown): LISTENING & SPEAKING FOCUS (Band 7.5 overall)
(5, 'GENERAL', 'IELTS', 'English', 7.5),
(5, 'LISTENING', 'IELTS', 'English', 8.5),
(5, 'READING', 'IELTS', 'English', 7.0),
(5, 'WRITING', 'IELTS', 'English', 7.0),
(5, 'SPEAKING', 'IELTS', 'English', 8.5),

-- Teacher 6 (Lisa Chen): READING SPECIALIST (Band 8.5 overall)
(6, 'GENERAL', 'IELTS', 'English', 8.5),
(6, 'LISTENING', 'IELTS', 'English', 8.0),
(6, 'READING', 'IELTS', 'English', 9.0),
(6, 'WRITING', 'IELTS', 'English', 8.0),
(6, 'SPEAKING', 'IELTS', 'English', 8.5),

-- Teacher 7 (James Taylor): WRITING FOCUS (Band 7.5 overall)
(7, 'GENERAL', 'IELTS', 'English', 7.5),
(7, 'LISTENING', 'IELTS', 'English', 7.5),
(7, 'READING', 'IELTS', 'English', 7.5),
(7, 'WRITING', 'IELTS', 'English', 8.5),
(7, 'SPEAKING', 'IELTS', 'English', 7.0),

-- Teacher 8 (Anna Martinez): ALL-ROUNDER (Band 8.0 overall)
(8, 'GENERAL', 'IELTS', 'English', 8.0),
(8, 'LISTENING', 'IELTS', 'English', 8.0),
(8, 'READING', 'IELTS', 'English', 8.0),
(8, 'WRITING', 'IELTS', 'English', 8.0),
(8, 'SPEAKING', 'IELTS', 'English', 8.5),

-- ===== HO CHI MINH TEACHERS (9-16) =====

-- Teacher 9 (Chris Evans): SPEAKING & LISTENING EXPERT (Band 9.0 overall)
(9, 'GENERAL', 'IELTS', 'English', 9.0),
(9, 'LISTENING', 'IELTS', 'English', 9.0),
(9, 'READING', 'IELTS', 'English', 8.5),
(9, 'WRITING', 'IELTS', 'English', 8.5),
(9, 'SPEAKING', 'IELTS', 'English', 9.0),

-- Teacher 10 (Olivia White): WRITING SPECIALIST (Band 8.5 overall)
(10, 'GENERAL', 'IELTS', 'English', 8.5),
(10, 'LISTENING', 'IELTS', 'English', 8.0),
(10, 'READING', 'IELTS', 'English', 8.5),
(10, 'WRITING', 'IELTS', 'English', 9.0),
(10, 'SPEAKING', 'IELTS', 'English', 8.0),

-- Teacher 11 (Daniel Harris): READING FOCUS (Band 8.0 overall)
(11, 'GENERAL', 'IELTS', 'English', 8.0),
(11, 'LISTENING', 'IELTS', 'English', 7.5),
(11, 'READING', 'IELTS', 'English', 9.0),
(11, 'WRITING', 'IELTS', 'English', 7.5),
(11, 'SPEAKING', 'IELTS', 'English', 8.0),

-- Teacher 12 (Sophia Clark): BALANCED HIGH PERFORMER (Band 8.5 overall)
(12, 'GENERAL', 'IELTS', 'English', 8.5),
(12, 'LISTENING', 'IELTS', 'English', 8.5),
(12, 'READING', 'IELTS', 'English', 8.5),
(12, 'WRITING', 'IELTS', 'English', 8.5),
(12, 'SPEAKING', 'IELTS', 'English', 8.5),

-- Teacher 13 (Matthew Lewis): LISTENING SPECIALIST (Band 7.5 overall)
(13, 'GENERAL', 'IELTS', 'English', 7.5),
(13, 'LISTENING', 'IELTS', 'English', 9.0),
(13, 'READING', 'IELTS', 'English', 7.0),
(13, 'WRITING', 'IELTS', 'English', 7.0),
(13, 'SPEAKING', 'IELTS', 'English', 7.5),

-- Teacher 14 (Ava Robinson): SPEAKING EXPERT (Band 8.5 overall)
(14, 'GENERAL', 'IELTS', 'English', 8.5),
(14, 'LISTENING', 'IELTS', 'English', 8.5),
(14, 'READING', 'IELTS', 'English', 8.0),
(14, 'WRITING', 'IELTS', 'English', 8.0),
(14, 'SPEAKING', 'IELTS', 'English', 9.0),

-- Teacher 15 (Andrew Walker): WRITING & READING (Band 7.5 overall)
(15, 'GENERAL', 'IELTS', 'English', 7.5),
(15, 'LISTENING', 'IELTS', 'English', 7.0),
(15, 'READING', 'IELTS', 'English', 8.0),
(15, 'WRITING', 'IELTS', 'English', 8.5),
(15, 'SPEAKING', 'IELTS', 'English', 7.0),

-- Teacher 16 (Isabella Young): ALL-ROUNDER (Band 8.0 overall)
(16, 'GENERAL', 'IELTS', 'English', 8.0),
(16, 'LISTENING', 'IELTS', 'English', 8.0),
(16, 'READING', 'IELTS', 'English', 8.5),
(16, 'WRITING', 'IELTS', 'English', 8.0),
(16, 'SPEAKING', 'IELTS', 'English', 7.5),

-- ===== NEW TEACHERS: TOEIC SPECIALISTS =====

-- Teacher 17 (Peter Nguyen): TOEIC SPECIALIST - LISTENING FOCUS
(17, 'GENERAL', 'TOEIC', 'English', 9.0),
(17, 'LISTENING', 'TOEIC', 'English', 9.5),
(17, 'READING', 'TOEIC', 'English', 8.5),

-- Teacher 18 (Mary Tran): TOEIC SPECIALIST - READING FOCUS
(18, 'GENERAL', 'TOEIC', 'English', 8.5),
(18, 'LISTENING', 'TOEIC', 'English', 8.0),
(18, 'READING', 'TOEIC', 'English', 9.5),

-- Teacher 19 (Tom Weekend): IELTS + WEEKEND ONLY
(19, 'GENERAL', 'IELTS', 'English', 8.0),
(19, 'LISTENING', 'IELTS', 'English', 8.0),
(19, 'SPEAKING', 'IELTS', 'English', 8.5),

-- Teacher 20 (Jane Saturday): IELTS + WEEKEND ONLY
(20, 'GENERAL', 'IELTS', 'English', 7.5),
(20, 'WRITING', 'IELTS', 'English', 8.0),
(20, 'SPEAKING', 'IELTS', 'English', 8.0),

-- Teacher 21 (Kevin Night): EVENING PART-TIME - ALL SKILLS
(21, 'GENERAL', 'IELTS', 'English', 8.0),
(21, 'LISTENING', 'IELTS', 'English', 7.5),
(21, 'READING', 'IELTS', 'English', 8.0),
(21, 'WRITING', 'IELTS', 'English', 8.0),
(21, 'SPEAKING', 'IELTS', 'English', 7.5),

-- Teacher 22 (Robert Lee): TOEIC SPECIALIST HCM
(22, 'GENERAL', 'TOEIC', 'English', 9.0),
(22, 'LISTENING', 'TOEIC', 'English', 9.0),
(22, 'READING', 'TOEIC', 'English', 9.0),

-- Teacher 23 (David Sunday): IELTS WEEKEND HCM
(23, 'GENERAL', 'IELTS', 'English', 8.0),
(23, 'SPEAKING', 'IELTS', 'English', 9.0),
(23, 'LISTENING', 'IELTS', 'English', 8.5);

INSERT INTO teacher_availability (teacher_id, time_slot_template_id, day_of_week, effective_date, created_at, updated_at) VALUES

-- Teacher 1: 100% available Mon/Wed/Fri
(1, 1, 1, '2024-02-01', NOW(), NOW()),
(1, 1, 3, '2024-02-01', NOW(), NOW()),
(1, 1, 5, '2024-02-01', NOW(), NOW()),
-- Teacher 6: 100% available Mon/Wed/Fri
(6, 1, 1, '2024-02-01', NOW(), NOW()),
(6, 1, 3, '2024-02-01', NOW(), NOW()),
(6, 1, 5, '2024-02-01', NOW(), NOW()),
-- Teacher 8: 100% available all weekdays
(8, 1, 1, '2024-02-01', NOW(), NOW()),
(8, 1, 2, '2024-02-01', NOW(), NOW()),
(8, 1, 3, '2024-02-01', NOW(), NOW()),
(8, 1, 4, '2024-02-01', NOW(), NOW()),
(8, 1, 5, '2024-02-01', NOW(), NOW()),
-- Teacher 5: PARTIAL - only Mon/Wed (missing Fri)
(5, 1, 1, '2024-02-01', NOW(), NOW()),
(5, 1, 3, '2024-02-01', NOW(), NOW()),

-- Teacher 1: 100% available Mon/Wed/Fri
(1, 2, 1, '2024-02-01', NOW(), NOW()),
(1, 2, 3, '2024-02-01', NOW(), NOW()),
(1, 2, 5, '2024-02-01', NOW(), NOW()),
-- Teacher 4: 100% available Mon/Wed/Fri
(4, 2, 1, '2024-02-01', NOW(), NOW()),
(4, 2, 3, '2024-02-01', NOW(), NOW()),
(4, 2, 5, '2024-02-01', NOW(), NOW()),
-- Teacher 6: 100% available Mon/Wed/Fri
(6, 2, 1, '2024-02-01', NOW(), NOW()),
(6, 2, 3, '2024-02-01', NOW(), NOW()),
(6, 2, 5, '2024-02-01', NOW(), NOW()),
-- Teacher 8: 100% available all weekdays
(8, 2, 1, '2024-02-01', NOW(), NOW()),
(8, 2, 2, '2024-02-01', NOW(), NOW()),
(8, 2, 3, '2024-02-01', NOW(), NOW()),
(8, 2, 4, '2024-02-01', NOW(), NOW()),
(8, 2, 5, '2024-02-01', NOW(), NOW()),
-- Teacher 17: 100% available Mon/Wed/Fri (TOEIC specialist)
(17, 2, 1, '2024-02-01', NOW(), NOW()),
(17, 2, 3, '2024-02-01', NOW(), NOW()),
(17, 2, 5, '2024-02-01', NOW(), NOW()),
-- Teacher 5: PARTIAL - only Mon/Wed
(5, 2, 1, '2024-02-01', NOW(), NOW()),
(5, 2, 3, '2024-02-01', NOW(), NOW()),
-- Teacher 18: Tue/Thu only (MISMATCH for Mon/Wed/Fri classes)
(18, 2, 2, '2024-02-01', NOW(), NOW()),
(18, 2, 4, '2024-02-01', NOW(), NOW()),

-- Teacher 1: 100% available Mon/Wed/Fri
(1, 3, 1, '2024-02-01', NOW(), NOW()),
(1, 3, 3, '2024-02-01', NOW(), NOW()),
(1, 3, 5, '2024-02-01', NOW(), NOW()),
-- Teacher 5: 100% available all weekdays
(5, 3, 1, '2024-02-01', NOW(), NOW()),
(5, 3, 2, '2024-02-01', NOW(), NOW()),
(5, 3, 3, '2024-02-01', NOW(), NOW()),
(5, 3, 4, '2024-02-01', NOW(), NOW()),
(5, 3, 5, '2024-02-01', NOW(), NOW()),
-- Teacher 8: 100% available all weekdays
(8, 3, 1, '2024-02-01', NOW(), NOW()),
(8, 3, 2, '2024-02-01', NOW(), NOW()),
(8, 3, 3, '2024-02-01', NOW(), NOW()),
(8, 3, 4, '2024-02-01', NOW(), NOW()),
(8, 3, 5, '2024-02-01', NOW(), NOW()),
-- Teacher 2: Tue/Thu/Sat only (MISMATCH for common classes)
(2, 3, 2, '2024-02-01', NOW(), NOW()),
(2, 3, 4, '2024-02-01', NOW(), NOW()),
(2, 3, 6, '2024-02-01', NOW(), NOW()),
-- Teacher 18: Tue/Thu only
(18, 3, 2, '2024-02-01', NOW(), NOW()),
(18, 3, 4, '2024-02-01', NOW(), NOW()),

-- Teacher 4: 100% available Mon/Wed/Fri
(4, 4, 1, '2024-02-01', NOW(), NOW()),
(4, 4, 3, '2024-02-01', NOW(), NOW()),
(4, 4, 5, '2024-02-01', NOW(), NOW()),
-- Teacher 5: 100% available all weekdays
(5, 4, 1, '2024-02-01', NOW(), NOW()),
(5, 4, 2, '2024-02-01', NOW(), NOW()),
(5, 4, 3, '2024-02-01', NOW(), NOW()),
(5, 4, 4, '2024-02-01', NOW(), NOW()),
(5, 4, 5, '2024-02-01', NOW(), NOW()),
-- Teacher 21: 100% available all weekdays (evening specialist)
(21, 4, 1, '2024-02-01', NOW(), NOW()),
(21, 4, 2, '2024-02-01', NOW(), NOW()),
(21, 4, 3, '2024-02-01', NOW(), NOW()),
(21, 4, 4, '2024-02-01', NOW(), NOW()),
(21, 4, 5, '2024-02-01', NOW(), NOW()),

-- Teacher 5: 100% available Mon/Wed/Fri
(5, 5, 1, '2024-02-01', NOW(), NOW()),
(5, 5, 3, '2024-02-01', NOW(), NOW()),
(5, 5, 5, '2024-02-01', NOW(), NOW()),
-- Teacher 7: Tue/Thu only (MISMATCH)
(7, 5, 2, '2024-02-01', NOW(), NOW()),
(7, 5, 4, '2024-02-01', NOW(), NOW()),
-- Teacher 8: 100% available all weekdays
(8, 5, 1, '2024-02-01', NOW(), NOW()),
(8, 5, 2, '2024-02-01', NOW(), NOW()),
(8, 5, 3, '2024-02-01', NOW(), NOW()),
(8, 5, 4, '2024-02-01', NOW(), NOW()),
(8, 5, 5, '2024-02-01', NOW(), NOW()),
-- Teacher 21: 100% available all weekdays
(21, 5, 1, '2024-02-01', NOW(), NOW()),
(21, 5, 2, '2024-02-01', NOW(), NOW()),
(21, 5, 3, '2024-02-01', NOW(), NOW()),
(21, 5, 4, '2024-02-01', NOW(), NOW()),
(21, 5, 5, '2024-02-01', NOW(), NOW()),

-- Teacher 7: Tue/Thu only
(7, 6, 2, '2024-02-01', NOW(), NOW()),
(7, 6, 4, '2024-02-01', NOW(), NOW()),
-- Teacher 21: 100% available Mon/Wed/Fri
(21, 6, 1, '2024-02-01', NOW(), NOW()),
(21, 6, 3, '2024-02-01', NOW(), NOW()),
(21, 6, 5, '2024-02-01', NOW(), NOW()),

-- Teacher 1: 100% available Mon/Wed/Fri + Weekend
(1, 7, 1, '2024-02-01', NOW(), NOW()),
(1, 7, 3, '2024-02-01', NOW(), NOW()),
(1, 7, 5, '2024-02-01', NOW(), NOW()),
(1, 7, 6, '2024-02-01', NOW(), NOW()),  -- Saturday
(1, 7, 7, '2024-02-01', NOW(), NOW()),  -- Sunday
-- Teacher 4: 100% available Mon/Wed/Fri + Weekend
(4, 7, 1, '2024-02-01', NOW(), NOW()),
(4, 7, 3, '2024-02-01', NOW(), NOW()),
(4, 7, 5, '2024-02-01', NOW(), NOW()),
(4, 7, 6, '2024-02-01', NOW(), NOW()),
(4, 7, 7, '2024-02-01', NOW(), NOW()),
-- Teacher 6: 100% available Mon/Wed/Fri + Weekend
(6, 7, 1, '2024-02-01', NOW(), NOW()),
(6, 7, 3, '2024-02-01', NOW(), NOW()),
(6, 7, 5, '2024-02-01', NOW(), NOW()),
(6, 7, 6, '2024-02-01', NOW(), NOW()),
(6, 7, 7, '2024-02-01', NOW(), NOW()),
-- Teacher 8: 100% available all days
(8, 7, 1, '2024-02-01', NOW(), NOW()),
(8, 7, 2, '2024-02-01', NOW(), NOW()),
(8, 7, 3, '2024-02-01', NOW(), NOW()),
(8, 7, 4, '2024-02-01', NOW(), NOW()),
(8, 7, 5, '2024-02-01', NOW(), NOW()),
(8, 7, 6, '2024-02-01', NOW(), NOW()),
(8, 7, 7, '2024-02-01', NOW(), NOW()),
-- Teacher 17: 100% available Mon/Wed/Fri + Weekend
(17, 7, 1, '2024-02-01', NOW(), NOW()),
(17, 7, 3, '2024-02-01', NOW(), NOW()),
(17, 7, 5, '2024-02-01', NOW(), NOW()),
(17, 7, 6, '2024-02-01', NOW(), NOW()),
(17, 7, 7, '2024-02-01', NOW(), NOW()),
-- Teacher 19: Weekend ONLY (PARTIAL for weekday classes)
(19, 7, 6, '2024-02-01', NOW(), NOW()),
(19, 7, 7, '2024-02-01', NOW(), NOW()),
-- Teacher 20: Weekend ONLY
(20, 7, 6, '2024-02-01', NOW(), NOW()),
(20, 7, 7, '2024-02-01', NOW(), NOW()),
-- Teacher 18: Tue/Thu + Saturday (PARTIAL for Mon/Wed/Fri)
(18, 7, 2, '2024-02-01', NOW(), NOW()),
(18, 7, 4, '2024-02-01', NOW(), NOW()),
(18, 7, 6, '2024-02-01', NOW(), NOW()),
-- Teacher 5: PARTIAL - only Mon/Wed
(5, 7, 1, '2024-02-01', NOW(), NOW()),
(5, 7, 3, '2024-02-01', NOW(), NOW()),

-- Teacher 1: 100% available Mon/Wed/Fri + Weekend
(1, 8, 1, '2024-02-01', NOW(), NOW()),
(1, 8, 3, '2024-02-01', NOW(), NOW()),
(1, 8, 5, '2024-02-01', NOW(), NOW()),
(1, 8, 6, '2024-02-01', NOW(), NOW()),
(1, 8, 7, '2024-02-01', NOW(), NOW()),
-- Teacher 4: 100% available Mon/Wed/Fri + Weekend
(4, 8, 1, '2024-02-01', NOW(), NOW()),
(4, 8, 3, '2024-02-01', NOW(), NOW()),
(4, 8, 5, '2024-02-01', NOW(), NOW()),
(4, 8, 6, '2024-02-01', NOW(), NOW()),
(4, 8, 7, '2024-02-01', NOW(), NOW()),
-- Teacher 6: 100% available all days
(6, 8, 1, '2024-02-01', NOW(), NOW()),
(6, 8, 3, '2024-02-01', NOW(), NOW()),
(6, 8, 5, '2024-02-01', NOW(), NOW()),
(6, 8, 6, '2024-02-01', NOW(), NOW()),
(6, 8, 7, '2024-02-01', NOW(), NOW()),
-- Teacher 8: 100% available all days
(8, 8, 1, '2024-02-01', NOW(), NOW()),
(8, 8, 2, '2024-02-01', NOW(), NOW()),
(8, 8, 3, '2024-02-01', NOW(), NOW()),
(8, 8, 4, '2024-02-01', NOW(), NOW()),
(8, 8, 5, '2024-02-01', NOW(), NOW()),
(8, 8, 6, '2024-02-01', NOW(), NOW()),
(8, 8, 7, '2024-02-01', NOW(), NOW()),
-- Teacher 19: Weekend ONLY
(19, 8, 6, '2024-02-01', NOW(), NOW()),
(19, 8, 7, '2024-02-01', NOW(), NOW()),
-- Teacher 20: Weekend ONLY
(20, 8, 6, '2024-02-01', NOW(), NOW()),
(20, 8, 7, '2024-02-01', NOW(), NOW());

INSERT INTO teacher_availability (teacher_id, time_slot_template_id, day_of_week, effective_date, created_at, updated_at) VALUES

-- Teacher 28 (Chris Evans): 100% available Mon/Wed/Fri
(9, 9, 1, '2024-02-01', NOW(), NOW()),
(9, 9, 3, '2024-02-01', NOW(), NOW()),
(9, 9, 5, '2024-02-01', NOW(), NOW()),
-- Teacher 29 (Olivia White): 100% available Mon/Wed/Fri
(10, 9, 1, '2024-02-01', NOW(), NOW()),
(10, 9, 3, '2024-02-01', NOW(), NOW()),
(10, 9, 5, '2024-02-01', NOW(), NOW()),
-- Teacher 30 (Daniel Harris): 100% available all weekdays
(11, 9, 1, '2024-02-01', NOW(), NOW()),
(11, 9, 2, '2024-02-01', NOW(), NOW()),
(11, 9, 3, '2024-02-01', NOW(), NOW()),
(11, 9, 4, '2024-02-01', NOW(), NOW()),
(11, 9, 5, '2024-02-01', NOW(), NOW()),
-- Teacher 31 (Sophia Clark): PARTIAL - only Mon/Wed (missing Fri)
(12, 9, 1, '2024-02-01', NOW(), NOW()),
(12, 9, 3, '2024-02-01', NOW(), NOW()),

-- Teacher 28 (Chris Evans): 100% available Mon/Wed/Fri
(9, 10, 1, '2024-02-01', NOW(), NOW()),
(9, 10, 3, '2024-02-01', NOW(), NOW()),
(9, 10, 5, '2024-02-01', NOW(), NOW()),
-- Teacher 29 (Olivia White): 100% available Mon/Wed/Fri
(10, 10, 1, '2024-02-01', NOW(), NOW()),
(10, 10, 3, '2024-02-01', NOW(), NOW()),
(10, 10, 5, '2024-02-01', NOW(), NOW()),
-- Teacher 30 (Daniel Harris): 100% available all weekdays
(11, 10, 1, '2024-02-01', NOW(), NOW()),
(11, 10, 2, '2024-02-01', NOW(), NOW()),
(11, 10, 3, '2024-02-01', NOW(), NOW()),
(11, 10, 4, '2024-02-01', NOW(), NOW()),
(11, 10, 5, '2024-02-01', NOW(), NOW()),
-- Teacher 32 (Matthew Lewis): 100% available Mon/Wed/Fri
(13, 10, 1, '2024-02-01', NOW(), NOW()),
(13, 10, 3, '2024-02-01', NOW(), NOW()),
(13, 10, 5, '2024-02-01', NOW(), NOW()),
-- Teacher 41 (Robert Lee - TOEIC): 100% available Mon/Wed/Fri
(22, 10, 1, '2024-02-01', NOW(), NOW()),
(22, 10, 3, '2024-02-01', NOW(), NOW()),
(22, 10, 5, '2024-02-01', NOW(), NOW()),
-- Teacher 31 (Sophia Clark): PARTIAL - only Mon/Wed
(12, 10, 1, '2024-02-01', NOW(), NOW()),
(12, 10, 3, '2024-02-01', NOW(), NOW()),
-- Teacher 33 (Ava Robinson): Tue/Thu only (MISMATCH for Mon/Wed/Fri classes)
(14, 10, 2, '2024-02-01', NOW(), NOW()),
(14, 10, 4, '2024-02-01', NOW(), NOW()),

-- Teacher 28 (Chris Evans): 100% available Mon/Wed/Fri
(9, 11, 1, '2024-02-01', NOW(), NOW()),
(9, 11, 3, '2024-02-01', NOW(), NOW()),
(9, 11, 5, '2024-02-01', NOW(), NOW()),
-- Teacher 32 (Matthew Lewis): 100% available all weekdays
(13, 11, 1, '2024-02-01', NOW(), NOW()),
(13, 11, 2, '2024-02-01', NOW(), NOW()),
(13, 11, 3, '2024-02-01', NOW(), NOW()),
(13, 11, 4, '2024-02-01', NOW(), NOW()),
(13, 11, 5, '2024-02-01', NOW(), NOW()),
-- Teacher 34 (Andrew Walker): 100% available all weekdays
(15, 11, 1, '2024-02-01', NOW(), NOW()),
(15, 11, 2, '2024-02-01', NOW(), NOW()),
(15, 11, 3, '2024-02-01', NOW(), NOW()),
(15, 11, 4, '2024-02-01', NOW(), NOW()),
(15, 11, 5, '2024-02-01', NOW(), NOW()),
-- Teacher 33 (Ava Robinson): Tue/Thu/Sat only (MISMATCH for Mon/Wed/Fri)
(14, 11, 2, '2024-02-01', NOW(), NOW()),
(14, 11, 4, '2024-02-01', NOW(), NOW()),

-- Teacher 29 (Olivia White): 100% available Mon/Wed/Fri
(10, 12, 1, '2024-02-01', NOW(), NOW()),
(10, 12, 3, '2024-02-01', NOW(), NOW()),
(10, 12, 5, '2024-02-01', NOW(), NOW()),
-- Teacher 31 (Sophia Clark): 100% available Mon/Wed/Fri
(12, 12, 1, '2024-02-01', NOW(), NOW()),
(12, 12, 3, '2024-02-01', NOW(), NOW()),
(12, 12, 5, '2024-02-01', NOW(), NOW()),
-- Teacher 35 (Isabella Young): 100% available all weekdays
(16, 12, 1, '2024-02-01', NOW(), NOW()),
(16, 12, 2, '2024-02-01', NOW(), NOW()),
(16, 12, 3, '2024-02-01', NOW(), NOW()),
(16, 12, 4, '2024-02-01', NOW(), NOW()),
(16, 12, 5, '2024-02-01', NOW(), NOW()),
-- Teacher 34 (Andrew Walker): Tue/Thu only (MISMATCH)
(15, 12, 2, '2024-02-01', NOW(), NOW()),
(15, 12, 4, '2024-02-01', NOW(), NOW()),

-- Teacher 30 (Daniel Harris): 100% available Mon/Wed/Fri
(11, 13, 1, '2024-02-01', NOW(), NOW()),
(11, 13, 3, '2024-02-01', NOW(), NOW()),
(11, 13, 5, '2024-02-01', NOW(), NOW()),
-- Teacher 33 (Ava Robinson): 100% available Mon/Wed/Fri
(14, 13, 1, '2024-02-01', NOW(), NOW()),
(14, 13, 3, '2024-02-01', NOW(), NOW()),
(14, 13, 5, '2024-02-01', NOW(), NOW()),
-- Teacher 35 (Isabella Young): 100% available Mon/Wed/Fri
(16, 13, 1, '2024-02-01', NOW(), NOW()),
(16, 13, 3, '2024-02-01', NOW(), NOW()),
(16, 13, 5, '2024-02-01', NOW(), NOW()),
-- Teacher 41 (Robert Lee - TOEIC): Tue/Thu only
(22, 13, 2, '2024-02-01', NOW(), NOW()),
(22, 13, 4, '2024-02-01', NOW(), NOW()),

-- Teacher 32 (Matthew Lewis): Tue/Thu only
(13, 14, 2, '2024-02-01', NOW(), NOW()),
(13, 14, 4, '2024-02-01', NOW(), NOW()),
-- Teacher 34 (Andrew Walker): 100% available Mon/Wed/Fri
(15, 14, 1, '2024-02-01', NOW(), NOW()),
(15, 14, 3, '2024-02-01', NOW(), NOW()),
(15, 14, 5, '2024-02-01', NOW(), NOW()),

-- ═══════════════════════════════════════════════════════════════════════════
-- SLOT 15: HCM Weekend Morning 2.5h (08:30-11:00) - ALL DAYS
-- ═══════════════════════════════════════════════════════════════════════════
-- Teacher 28 (Chris Evans): 100% available Mon/Wed/Fri + Weekend
(9, 15, 1, '2024-02-01', NOW(), NOW()),
(9, 15, 3, '2024-02-01', NOW(), NOW()),
(9, 15, 5, '2024-02-01', NOW(), NOW()),
(9, 15, 6, '2024-02-01', NOW(), NOW()),  -- Saturday
(9, 15, 7, '2024-02-01', NOW(), NOW()),  -- Sunday
-- Teacher 29 (Olivia White): 100% available Mon/Wed/Fri + Weekend
(10, 15, 1, '2024-02-01', NOW(), NOW()),
(10, 15, 3, '2024-02-01', NOW(), NOW()),
(10, 15, 5, '2024-02-01', NOW(), NOW()),
(10, 15, 6, '2024-02-01', NOW(), NOW()),
(10, 15, 7, '2024-02-01', NOW(), NOW()),
-- Teacher 30 (Daniel Harris): 100% available all days
(11, 15, 1, '2024-02-01', NOW(), NOW()),
(11, 15, 2, '2024-02-01', NOW(), NOW()),
(11, 15, 3, '2024-02-01', NOW(), NOW()),
(11, 15, 4, '2024-02-01', NOW(), NOW()),
(11, 15, 5, '2024-02-01', NOW(), NOW()),
(11, 15, 6, '2024-02-01', NOW(), NOW()),
(11, 15, 7, '2024-02-01', NOW(), NOW()),
-- Teacher 41 (Robert Lee - TOEIC): 100% available Mon/Wed/Fri + Weekend
(22, 15, 1, '2024-02-01', NOW(), NOW()),
(22, 15, 3, '2024-02-01', NOW(), NOW()),
(22, 15, 5, '2024-02-01', NOW(), NOW()),
(22, 15, 6, '2024-02-01', NOW(), NOW()),
(22, 15, 7, '2024-02-01', NOW(), NOW()),
-- Teacher 42 (David Sunday - Weekend): Weekend ONLY
(23, 15, 6, '2024-02-01', NOW(), NOW()),
(23, 15, 7, '2024-02-01', NOW(), NOW()),
-- Teacher 31 (Sophia Clark): PARTIAL - only Mon/Wed
(12, 15, 1, '2024-02-01', NOW(), NOW()),
(12, 15, 3, '2024-02-01', NOW(), NOW()),
-- Teacher 28 (Chris Evans): 100% available Mon/Wed/Fri + Weekend
(9, 16, 1, '2024-02-01', NOW(), NOW()),
(9, 16, 3, '2024-02-01', NOW(), NOW()),
(9, 16, 5, '2024-02-01', NOW(), NOW()),
(9, 16, 6, '2024-02-01', NOW(), NOW()),
(9, 16, 7, '2024-02-01', NOW(), NOW()),
-- Teacher 32 (Matthew Lewis): 100% available all days
(13, 16, 1, '2024-02-01', NOW(), NOW()),
(13, 16, 3, '2024-02-01', NOW(), NOW()),
(13, 16, 5, '2024-02-01', NOW(), NOW()),
(13, 16, 6, '2024-02-01', NOW(), NOW()),
(13, 16, 7, '2024-02-01', NOW(), NOW()),
-- Teacher 34 (Andrew Walker): 100% available all days
(15, 16, 1, '2024-02-01', NOW(), NOW()),
(15, 16, 2, '2024-02-01', NOW(), NOW()),
(15, 16, 3, '2024-02-01', NOW(), NOW()),
(15, 16, 4, '2024-02-01', NOW(), NOW()),
(15, 16, 5, '2024-02-01', NOW(), NOW()),
(15, 16, 6, '2024-02-01', NOW(), NOW()),
(15, 16, 7, '2024-02-01', NOW(), NOW()),
-- Teacher 42 (David Sunday - Weekend): Weekend ONLY
(23, 16, 6, '2024-02-01', NOW(), NOW()),
(23, 16, 7, '2024-02-01', NOW(), NOW()),
-- Teacher 35 (Isabella Young): Weekend ONLY
(16, 16, 6, '2024-02-01', NOW(), NOW()),
(16, 16, 7, '2024-02-01', NOW(), NOW());
-- Teachers with NO AVAILABILITY (for testing "Chưa đăng ký lịch"):
-- Teacher 3, 11, 24: NO AVAILABILITY

-- ========== AVAILABILITY CAMPAIGNS ==========
INSERT INTO availability_campaign (id, name, deadline, target_audience, is_active, created_at, updated_at) VALUES
(1, 'Đợt đăng ký lịch dạy Tháng 12/2025', '2025-11-30 23:59:59+07', 'ALL', true, '2025-11-20 00:00:00+07', '2025-11-20 00:00:00+07'),
(2, 'Đợt bổ sung lịch dạy Full-time', '2025-12-05 17:00:00+07', 'FULL_TIME', true, '2025-12-01 00:00:00+07', '2025-12-01 00:00:00+07'),
(3, 'Đợt đăng ký lịch dạy Tháng 11/2025', '2025-10-31 23:59:59+07', 'ALL', false, '2025-10-20 00:00:00+07', '2025-10-20 00:00:00+07');

SELECT setval('availability_campaign_id_seq', 3, true);

-- Levels (CEFR-based A1-C2 for both IELTS and TOEIC)
INSERT INTO level (id, curriculum_id, code, name, sort_order, created_at, updated_at) VALUES
-- IELTS Levels (A1 -> C2)
(1, 1, 'A1', 'IELTS A1 (Beginner)', 1, '2024-06-01 00:00:00+07', '2024-06-01 00:00:00+07'),
(2, 1, 'A2', 'IELTS A2 (Elementary)', 2, '2024-06-01 00:00:00+07', '2024-06-01 00:00:00+07'),
(3, 1, 'B1', 'IELTS B1 (Intermediate)', 3, '2024-06-01 00:00:00+07', '2024-06-01 00:00:00+07'),
(4, 1, 'B2', 'IELTS B2 (Upper-Intermediate)', 4, '2024-06-01 00:00:00+07', '2024-06-01 00:00:00+07'),
(5, 1, 'C1', 'IELTS C1 (Advanced)', 5, '2024-06-01 00:00:00+07', '2024-06-01 00:00:00+07'),
(6, 1, 'C2', 'IELTS C2 (Proficiency)', 6, '2024-06-01 00:00:00+07', '2024-06-01 00:00:00+07'),
-- TOEIC Levels (A1 -> C2)
(7, 2, 'A1', 'TOEIC A1 (Beginner)', 1, '2024-06-01 00:00:00+07', '2024-06-01 00:00:00+07'),
(8, 2, 'A2', 'TOEIC A2 (Elementary)', 2, '2024-06-01 00:00:00+07', '2024-06-01 00:00:00+07'),
(9, 2, 'B1', 'TOEIC B1 (Intermediate)', 3, '2024-06-01 00:00:00+07', '2024-06-01 00:00:00+07'),
(10, 2, 'B2', 'TOEIC B2 (Upper-Intermediate)', 4, '2024-06-01 00:00:00+07', '2024-06-01 00:00:00+07'),
(11, 2, 'C1', 'TOEIC C1 (Advanced)', 5, '2024-06-01 00:00:00+07', '2024-06-01 00:00:00+07'),
(12, 2, 'C2', 'TOEIC C2 (Proficiency)', 6, '2024-06-01 00:00:00+07', '2024-06-01 00:00:00+07');

-- Replacement Skill Assessments for initial students
-- Simulates placement test results before their first enrollment.
-- IELTS Placement Tests (format n/n: score/maxScore)
INSERT INTO replacement_skill_assessment (student_id, skill, level_id, score, assessment_date, assessment_type, assessed_by, note, created_at, updated_at)
SELECT
    s.id,
    'GENERAL',
    1, -- Corresponds to 'IELTS Foundation (3.0-4.0)'
    (30 + floor(random() * 16)::int)::text || '/40', -- Score as n/n format (30-45/40)
    '2025-06-15',
    'placement_test',
    6, -- Assessed by 'staff.huong.hn@tms-edu.vn'
    'Initial IELTS placement test score.',
    '2025-06-15 00:00:00+07',
    '2025-06-15 00:00:00+07'
FROM generate_series(2, 60) AS s(id);

-- Student 1: IELTS Skills Assessment (detailed breakdown)
INSERT INTO replacement_skill_assessment (student_id, skill, level_id, score, assessment_date, assessment_type, assessed_by, note, created_at, updated_at) VALUES
(1, 'LISTENING', 1, '35/40', '2025-06-15', 'ielts_placement', 6, 'IELTS Listening Assessment', '2025-06-15 00:00:00+07', '2025-06-15 00:00:00+07'),
(1, 'READING', 1, '32/40', '2025-06-15', 'ielts_placement', 6, 'IELTS Reading Assessment', '2025-06-15 00:00:00+07', '2025-06-15 00:00:00+07'),
(1, 'SPEAKING', 1, '30/40', '2025-06-15', 'ielts_placement', 6, 'IELTS Speaking Assessment', '2025-06-15 00:00:00+07', '2025-06-15 00:00:00+07');

-- Student 2: TOEIC Placement Test
INSERT INTO replacement_skill_assessment (student_id, skill, level_id, score, assessment_date, assessment_type, assessed_by, note, created_at, updated_at) VALUES
(2, 'LISTENING', 9, '375/495', '2025-06-16', 'toeic_placement', 6, 'TOEIC Listening Placement Test', '2025-06-16 00:00:00+07', '2025-06-16 00:00:00+07'),
(2, 'READING', 9, '360/495', '2025-06-16', 'toeic_placement', 6, 'TOEIC Reading Placement Test', '2025-06-16 00:00:00+07', '2025-06-16 00:00:00+07');

-- PLOs for IELTS Subject
INSERT INTO plo (id, curriculum_id, code, description, created_at, updated_at) VALUES
(1, 1, 'PLO1', 'Demonstrate basic English communication skills in everyday contexts', '2024-06-01 00:00:00+07', '2024-06-01 00:00:00+07'),
(2, 1, 'PLO2', 'Comprehend and produce simple English texts for common situations', '2024-06-01 00:00:00+07', '2024-06-01 00:00:00+07'),
(3, 1, 'PLO3', 'Apply intermediate English grammar and vocabulary in professional contexts', '2024-06-01 00:00:00+07', '2024-06-01 00:00:00+07'),
(4, 1, 'PLO4', 'Analyze and evaluate complex English texts across various topics', '2024-06-01 00:00:00+07', '2024-06-01 00:00:00+07'),
(5, 1, 'PLO5', 'Produce coherent, well-structured academic essays and reports', '2024-06-01 00:00:00+07', '2024-06-01 00:00:00+07'),
-- PLOs for TOEIC Subject
(6, 2, 'PLO1', 'Understand workplace conversations and announcements', '2024-06-01 00:00:00+07', '2024-06-01 00:00:00+07'),
(7, 2, 'PLO2', 'Comprehend business emails, reports, and articles', '2024-06-01 00:00:00+07', '2024-06-01 00:00:00+07'),
(8, 2, 'PLO3', 'Use appropriate business vocabulary and grammar', '2024-06-01 00:00:00+07', '2024-06-01 00:00:00+07'),
(9, 2, 'PLO4', 'Communicate effectively in professional settings', '2024-06-01 00:00:00+07', '2024-06-01 00:00:00+07'),
(10, 2, 'PLO5', 'Apply business etiquette in written and spoken communication', '2024-06-01 00:00:00+07', '2024-06-01 00:00:00+07');

-- Courses: 3 IELTS + 3 TOEIC
INSERT INTO subject (id, curriculum_id, level_id, logical_subject_code, version, code, name, description, total_hours, number_of_sessions, hours_per_session, prerequisites, target_audience, teaching_methods, score_scale, status, approval_status, decided_by_manager, decided_at, rejection_reason, created_by, created_at, updated_at, effective_date) VALUES
-- IELTS Courses
(1, 1, 1, 'IELTS-FOUND-2025', 1, 'IELTS-FOUND-2025-V1', 'IELTS Foundation 2025', 'Khóa học nền tảng cho người mới bắt đầu, mục tiêu band 3.0-4.0', 60, 24, 2.5, 'Không yêu cầu kiến thức nền tảng.', 'Học viên mất gốc hoặc mới bắt đầu học tiếng Anh.', 'Communicative Language Teaching (CLT) kết hợp bài tập thực hành.', '0-9', 'ACTIVE', 'APPROVED', 2, '2024-08-20 14:00:00+07', NULL, 5, '2024-08-15 00:00:00+07', '2024-08-20 14:00:00+07', '2024-09-01'),
(2, 1, 3, 'IELTS-INT-2025', 1, 'IELTS-INT-2025-V1', 'IELTS Intermediate 2025', 'Khóa học trung cấp, mục tiêu band 5.0-5.5', 60, 24, 2.5, 'Hoàn thành khóa Foundation hoặc IELTS 4.0+', 'Học viên có nền tảng cơ bản, mục tiêu band 5.0-5.5.', 'Chiến thuật giải đề và nâng cao từ vựng học thuật.', '0-9', 'ACTIVE', 'APPROVED', 2, '2024-08-25 14:00:00+07', NULL, 5, '2024-08-15 00:00:00+07', '2024-08-25 14:00:00+07', '2024-09-01'),
(3, 1, 4, 'IELTS-ADV-2025', 1, 'IELTS-ADV-2025-V1', 'IELTS Advanced 2025', 'Khóa học nâng cao, mục tiêu band 6.5+', 100, 40, 2.5, 'Hoàn thành khóa Intermediate hoặc IELTS 5.5+', 'Học viên muốn đạt band 6.5+ để du học hoặc làm việc.', 'Luyện đề cường độ cao và phản hồi cá nhân hóa.', '0-9', 'ACTIVE', 'APPROVED', 2, '2024-08-25 14:00:00+07', NULL, 5, '2024-08-20 00:00:00+07', '2024-08-25 14:00:00+07', '2024-09-01'),
-- TOEIC Courses (subject_id = 2, level_id = 8,9,11 for TOEIC levels)
(4, 2, 8, 'TOEIC-FOUND-2025', 1, 'TOEIC-FOUND-2025-V1', 'TOEIC Foundation 2025', 'Nền tảng TOEIC cho người mới, mục tiêu 300-450', 50, 20, 2.5, 'Không yêu cầu kiến thức nền tảng.', 'Sinh viên, người đi làm mới bắt đầu học TOEIC.', 'Làm quen với format đề thi và từ vựng cơ bản.', '0-990', 'ACTIVE', 'APPROVED', 2, '2024-09-01 14:00:00+07', NULL, 5, '2024-08-20 00:00:00+07', '2024-09-01 14:00:00+07', '2024-09-15'),
(5, 2, 9, 'TOEIC-INT-2025', 1, 'TOEIC-INT-2025-V1', 'TOEIC Intermediate 2025', 'Chuẩn bị thi TOEIC mục tiêu 500-650', 60, 24, 2.5, 'Hoàn thành TOEIC Foundation hoặc 400+', 'Người đi làm muốn nâng cao kỹ năng tiếng Anh công sở.', 'Từ vựng kinh doanh và chiến thuật làm bài.', '0-990', 'ACTIVE', 'APPROVED', 2, '2024-09-05 14:00:00+07', NULL, 5, '2024-08-20 00:00:00+07', '2024-09-05 14:00:00+07', '2024-09-15'),
(6, 2, 11, 'TOEIC-ADV-2025', 1, 'TOEIC-ADV-2025-V1', 'TOEIC Advanced 2025', 'Luyện thi TOEIC mục tiêu 750+', 70, 28, 2.5, 'Hoàn thành TOEIC Intermediate hoặc 600+', 'Quản lý và chuyên viên cần chứng chỉ TOEIC cao.', 'Case study thực tế và business negotiation.', '0-990', 'ACTIVE', 'APPROVED', 2, '2024-09-10 14:00:00+07', NULL, 5, '2024-08-20 00:00:00+07', '2024-09-10 14:00:00+07', '2024-09-15');
-- Course Phases for Foundation
INSERT INTO subject_phase (id, subject_id, phase_number, name, duration_weeks, created_at, updated_at) VALUES
(1, 1, 1, 'Foundation Basics', 4, '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(2, 1, 2, 'Foundation Practice', 4, '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07');

-- Course Phases for IELTS Intermediate (Course 2)
INSERT INTO subject_phase (id, subject_id, phase_number, name, duration_weeks, created_at, updated_at) VALUES
(3, 2, 1, 'Skill Building', 5, '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(4, 2, 2, 'Test Strategies', 5, '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07');

-- Course Phases for IELTS Advanced (Course 3)
INSERT INTO subject_phase (id, subject_id, phase_number, name, duration_weeks, created_at, updated_at) VALUES
(6, 3, 1, 'Advanced Techniques', 6, '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(7, 3, 2, 'Intensive Practice', 6, '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(8, 3, 3, 'Test Mastery', 4, '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07');

-- Course Phases for TOEIC Foundation (Course 4)
INSERT INTO subject_phase (id, subject_id, phase_number, name, duration_weeks, created_at, updated_at) VALUES
(9, 4, 1, 'TOEIC Basics', 4, '2024-09-01 00:00:00+07', '2024-09-01 00:00:00+07'),
(10, 4, 2, 'Practice & Review', 4, '2024-09-01 00:00:00+07', '2024-09-01 00:00:00+07');

-- Course Phases for TOEIC Intermediate (Course 5)
INSERT INTO subject_phase (id, subject_id, phase_number, name, duration_weeks, created_at, updated_at) VALUES
(11, 5, 1, 'Business Vocabulary & Grammar', 4, '2024-09-05 00:00:00+07', '2024-09-05 00:00:00+07'),
(12, 5, 2, 'Test Techniques & Practice', 4, '2024-09-05 00:00:00+07', '2024-09-05 00:00:00+07');

-- Course Phases for TOEIC Advanced (Course 6)
INSERT INTO subject_phase (id, subject_id, phase_number, name, duration_weeks, created_at, updated_at) VALUES
(13, 6, 1, 'Advanced Business Communication', 5, '2024-09-10 00:00:00+07', '2024-09-10 00:00:00+07'),
(14, 6, 2, 'Executive Test Preparation', 5, '2024-09-10 00:00:00+07', '2024-09-10 00:00:00+07');

-- Course Sessions for Foundation (24 sessions = 8 weeks × 3 sessions/week)
INSERT INTO subject_session (id, phase_id, sequence_no, topic, student_task, skill, created_at, updated_at) VALUES
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
(13, 2, 13, 'Listening: Following Instructions', 'Complete tasks from audio', 'LISTENING', '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(14, 2, 14, 'Speaking: Asking Questions', 'Practice question forms', 'SPEAKING', '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(15, 2, 15, 'Reading: Short Stories', 'Read and summarize', 'READING', '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(16, 2, 16, 'Writing: Describing People and Places', 'Write descriptions', 'WRITING', '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(17, 2, 17, 'Listening: News and Announcements', 'Understand main points', 'LISTENING', '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(18, 2, 18, 'Speaking: Giving Opinions', 'Express simple opinions', 'SPEAKING', '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(19, 2, 19, 'Reading: Understanding Context', 'Use context clues', 'READING', '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(20, 2, 20, 'Writing: Personal Letters', 'Write informal letters', 'WRITING', '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(21, 2, 21, 'Practice Test: Listening & Reading', 'Complete practice test', 'GENERAL', '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(22, 2, 22, 'Practice Test: Writing & Speaking', 'Complete practice test', 'GENERAL', '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(23, 2, 23, 'Review and Feedback', 'Review all skills', 'GENERAL', '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(24, 2, 24, 'Final Assessment', 'Complete final test', 'GENERAL', '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07');

-- Course 2: IELTS Intermediate (30 sessions)
-- Phase 3: 12 sessions
INSERT INTO subject_session (id, phase_id, sequence_no, topic, student_task, skill, created_at, updated_at)
SELECT 24 + s.idx, 3, s.idx, 'IELTS Int Skill Building ' || s.idx, 'Practice skills', 'GENERAL', NOW(), NOW()
FROM generate_series(1, 12) AS s(idx);
-- Phase 4: 12 sessions
INSERT INTO subject_session (id, phase_id, sequence_no, topic, student_task, skill, created_at, updated_at)
SELECT 36 + s.idx, 4, 12 + s.idx, 'IELTS Int Strategies ' || s.idx, 'Learn strategies', 'GENERAL', NOW(), NOW()
FROM generate_series(1, 12) AS s(idx);


-- Course 3: IELTS Advanced (40 sessions)
-- Phase 6: 15 sessions
INSERT INTO subject_session (id, phase_id, sequence_no, topic, student_task, skill, created_at, updated_at)
SELECT 54 + s.idx, 6, s.idx, 'IELTS Adv Techniques ' || s.idx, 'Advanced techniques', 'GENERAL', NOW(), NOW()
FROM generate_series(1, 15) AS s(idx);
-- Phase 7: 15 sessions
INSERT INTO subject_session (id, phase_id, sequence_no, topic, student_task, skill, created_at, updated_at)
SELECT 69 + s.idx, 7, s.idx, 'IELTS Adv Practice ' || s.idx, 'Intensive practice', 'GENERAL', NOW(), NOW()
FROM generate_series(1, 15) AS s(idx);
-- Phase 8: 10 sessions
INSERT INTO subject_session (id, phase_id, sequence_no, topic, student_task, skill, created_at, updated_at)
SELECT 84 + s.idx, 8, s.idx, 'IELTS Adv Mastery ' || s.idx, 'Mastery test', 'GENERAL', NOW(), NOW()
FROM generate_series(1, 10) AS s(idx);

-- Course 4: TOEIC Foundation (20 sessions)
-- Phase 9: 10 sessions
INSERT INTO subject_session (id, phase_id, sequence_no, topic, student_task, skill, created_at, updated_at)
SELECT 94 + s.idx, 9, s.idx, 'TOEIC Found Basics ' || s.idx, 'Basic concepts', 'GENERAL', NOW(), NOW()
FROM generate_series(1, 10) AS s(idx);
-- Phase 10: 10 sessions
INSERT INTO subject_session (id, phase_id, sequence_no, topic, student_task, skill, created_at, updated_at)
SELECT 104 + s.idx, 10, s.idx, 'TOEIC Found Practice ' || s.idx, 'Practice questions', 'GENERAL', NOW(), NOW()
FROM generate_series(1, 10) AS s(idx);

-- Course 5: TOEIC Intermediate (24 sessions)
-- Phase 11: 12 sessions
INSERT INTO subject_session (id, phase_id, sequence_no, topic, student_task, skill, created_at, updated_at)
SELECT 114 + s.idx, 11, s.idx, 'TOEIC Int Business ' || s.idx, 'Business vocab', 'GENERAL', NOW(), NOW()
FROM generate_series(1, 12) AS s(idx);
-- Phase 12: 12 sessions
INSERT INTO subject_session (id, phase_id, sequence_no, topic, student_task, skill, created_at, updated_at)
SELECT 126 + s.idx, 12, s.idx, 'TOEIC Int Techniques ' || s.idx, 'Test techniques', 'GENERAL', NOW(), NOW()
FROM generate_series(1, 12) AS s(idx);

-- Course 6: TOEIC Advanced (28 sessions)
-- Phase 13: 14 sessions
INSERT INTO subject_session (id, phase_id, sequence_no, topic, student_task, skill, created_at, updated_at)
SELECT 138 + s.idx, 13, s.idx, 'TOEIC Adv Comm ' || s.idx, 'Advanced comm', 'GENERAL', NOW(), NOW()
FROM generate_series(1, 14) AS s(idx);
-- Phase 14: 14 sessions
INSERT INTO subject_session (id, phase_id, sequence_no, topic, student_task, skill, created_at, updated_at)
SELECT 152 + s.idx, 14, s.idx, 'TOEIC Adv Prep ' || s.idx, 'Exam prep', 'GENERAL', NOW(), NOW()
FROM generate_series(1, 14) AS s(idx);



-- CLOs for Foundation Course
INSERT INTO clo (id, subject_id, code, description, created_at, updated_at) VALUES
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

-- CLOs for Course 2 (IELTS Intermediate)
INSERT INTO clo (id, subject_id, code, description, created_at, updated_at) VALUES
(5, 2, 'CLO1', 'Apply intermediate IELTS strategies', NOW(), NOW()),
(6, 2, 'CLO2', 'Analyze complex reading passages', NOW(), NOW()),
(7, 2, 'CLO3', 'Write structured essays', NOW(), NOW()),
(8, 2, 'CLO4', 'Speak fluently on abstract topics', NOW(), NOW());

-- CLOs for Course 3 (IELTS Advanced)
INSERT INTO clo (id, subject_id, code, description, created_at, updated_at) VALUES
(9, 3, 'CLO1', 'Master advanced IELTS techniques', NOW(), NOW()),
(10, 3, 'CLO2', 'Comprehend academic lectures', NOW(), NOW()),
(11, 3, 'CLO3', 'Produce sophisticated writing', NOW(), NOW()),
(12, 3, 'CLO4', 'Demonstrate native-like speaking', NOW(), NOW());

-- CLOs for Course 4 (TOEIC Foundation)
INSERT INTO clo (id, subject_id, code, description, created_at, updated_at) VALUES
(13, 4, 'CLO1', 'Understand basic business English', NOW(), NOW()),
(14, 4, 'CLO2', 'Identify key information in announcements', NOW(), NOW()),
(15, 4, 'CLO3', 'Read simple business documents', NOW(), NOW()),
(16, 4, 'CLO4', 'Use basic business grammar', NOW(), NOW());

-- CLOs for Course 5 (TOEIC Intermediate)
INSERT INTO clo (id, subject_id, code, description, created_at, updated_at) VALUES
(17, 5, 'CLO1', 'Comprehend business meetings', NOW(), NOW()),
(18, 5, 'CLO2', 'Analyze business reports', NOW(), NOW()),
(19, 5, 'CLO3', 'Write professional emails', NOW(), NOW()),
(20, 5, 'CLO4', 'Participate in business discussions', NOW(), NOW());

-- CLOs for Course 6 (TOEIC Advanced)
INSERT INTO clo (id, subject_id, code, description, created_at, updated_at) VALUES
(21, 6, 'CLO1', 'Negotiate in English', NOW(), NOW()),
(22, 6, 'CLO2', 'Understand complex business scenarios', NOW(), NOW()),
(23, 6, 'CLO3', 'Draft executive summaries', NOW(), NOW()),
(24, 6, 'CLO4', 'Present business proposals', NOW(), NOW());

-- PLO-CLO Mappings for new CLOs
INSERT INTO plo_clo_mapping (plo_id, clo_id, status) VALUES
-- Course 2 (IELTS) -> PLO 3, 4
(3, 5, 'ACTIVE'), (4, 6, 'ACTIVE'), (5, 7, 'ACTIVE'), (3, 8, 'ACTIVE'),
-- Course 3 (IELTS) -> PLO 4, 5
(4, 9, 'ACTIVE'), (4, 10, 'ACTIVE'), (5, 11, 'ACTIVE'), (5, 12, 'ACTIVE'),
-- Course 4 (TOEIC) -> PLO 6, 7 (Subject 2)
(6, 13, 'ACTIVE'), (6, 14, 'ACTIVE'), (7, 15, 'ACTIVE'), (8, 16, 'ACTIVE'),
-- Course 5 (TOEIC) -> PLO 8, 9
(9, 17, 'ACTIVE'), (7, 18, 'ACTIVE'), (10, 19, 'ACTIVE'), (9, 20, 'ACTIVE'),
-- Course 6 (TOEIC) -> PLO 9, 10
(9, 21, 'ACTIVE'), (7, 22, 'ACTIVE'), (10, 23, 'ACTIVE'), (9, 24, 'ACTIVE');

-- Course Session-CLO Mappings for new sessions
-- Course 2 (Sessions 25-54) -> CLO 5-8
INSERT INTO subject_session_clo_mapping (subject_session_id, clo_id, status)
SELECT id, 5 + (id % 4), 'ACTIVE' FROM subject_session WHERE id BETWEEN 25 AND 54;

-- Course 3 (Sessions 55-94) -> CLO 9-12
INSERT INTO subject_session_clo_mapping (subject_session_id, clo_id, status)
SELECT id, 9 + (id % 4), 'ACTIVE' FROM subject_session WHERE id BETWEEN 55 AND 94;

-- Course 4 (Sessions 95-114) -> CLO 13-16
INSERT INTO subject_session_clo_mapping (subject_session_id, clo_id, status)
SELECT id, 13 + (id % 4), 'ACTIVE' FROM subject_session WHERE id BETWEEN 95 AND 114;

-- Course 5 (Sessions 115-138) -> CLO 17-20
INSERT INTO subject_session_clo_mapping (subject_session_id, clo_id, status)
SELECT id, 17 + (id % 4), 'ACTIVE' FROM subject_session WHERE id BETWEEN 115 AND 138;

-- Course 6 (Sessions 139-166) -> CLO 21-24
INSERT INTO subject_session_clo_mapping (subject_session_id, clo_id, status)
SELECT id, 21 + (id % 4), 'ACTIVE' FROM subject_session WHERE id BETWEEN 139 AND 166;



-- Course Session-CLO Mappings (Sample - map each CLO to relevant sessions)
INSERT INTO subject_session_clo_mapping (subject_session_id, clo_id, status) VALUES
-- CLO1 (Understand basic English) - Listening sessions
(1, 1, 'ACTIVE'), (5, 1, 'ACTIVE'), (9, 1, 'ACTIVE'), (13, 1, 'ACTIVE'), (17, 1, 'ACTIVE'),
-- CLO2 (Communicate simple info) - Speaking sessions
(2, 2, 'ACTIVE'), (6, 2, 'ACTIVE'), (10, 2, 'ACTIVE'), (14, 2, 'ACTIVE'), (18, 2, 'ACTIVE'),
-- CLO3 (Read simple texts) - Reading sessions
(3, 3, 'ACTIVE'), (7, 3, 'ACTIVE'), (11, 3, 'ACTIVE'), (15, 3, 'ACTIVE'), (19, 3, 'ACTIVE'),
-- CLO4 (Write simple paragraphs) - Writing sessions
(4, 4, 'ACTIVE'), (8, 4, 'ACTIVE'), (12, 4, 'ACTIVE'), (16, 4, 'ACTIVE'), (20, 4, 'ACTIVE');

-- Course Materials for Foundation Course
INSERT INTO subject_material (subject_id, phase_id, subject_session_id, title, description, material_type, url, uploaded_by) VALUES
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

-- Course Materials for IELTS Intermediate (Course 2)
INSERT INTO subject_material (subject_id, phase_id, subject_session_id, title, description, material_type, url, uploaded_by) VALUES
-- Course Level
(2, NULL, NULL, 'IELTS Intermediate Syllabus', 'Course syllabus and schedule.', 'DOCUMENT', '/materials/courses/2/syllabus.pdf', 5),
-- Phase 3: Skill Building
(2, 3, NULL, 'Intermediate Skill Building Guide', 'Overview of intermediate skills.', 'DOCUMENT', '/materials/phases/3/guide.pdf', 5),
(2, 3, 25, 'Complex Grammar Guide', 'Guide to complex sentences.', 'DOCUMENT', '/materials/sessions/25/grammar.pdf', 5),
(2, 3, 26, 'Advanced Listening Audio', 'Audio for advanced listening practice.', 'MEDIA', '/materials/sessions/26/audio.mp3', 5),
-- Phase 4: Test Strategies
(2, 4, NULL, 'IELTS Test Strategies Handbook', 'Strategies for all 4 skills.', 'DOCUMENT', '/materials/phases/4/strategies.pdf', 5),
(2, 4, 37, 'Speaking Part 2 Cue Cards', 'Practice cue cards for Speaking Part 2.', 'DOCUMENT', '/materials/sessions/37/cue-cards.pdf', 5);

-- Course Materials for IELTS Advanced (Course 3)
INSERT INTO subject_material (subject_id, phase_id, subject_session_id, title, description, material_type, url, uploaded_by) VALUES
-- Course Level
(3, NULL, NULL, 'IELTS Advanced Syllabus', 'Advanced course syllabus.', 'DOCUMENT', '/materials/courses/3/syllabus.pdf', 5),
-- Phase 6: Advanced Techniques
(3, 6, NULL, 'Advanced Techniques Manual', 'Deep dive into band 7+ techniques.', 'DOCUMENT', '/materials/phases/6/manual.pdf', 5),
(3, 6, 55, 'Academic Vocabulary List', 'List of high-level academic words.', 'DOCUMENT', '/materials/sessions/55/vocab.pdf', 5),
-- Phase 7: Intensive Practice
(3, 7, NULL, 'Intensive Practice Workbook', 'Workbook for daily practice.', 'DOCUMENT', '/materials/phases/7/workbook.pdf', 5),
(3, 7, 70, 'Writing Task 2 Samples', 'Band 8.0+ essay samples.', 'DOCUMENT', '/materials/sessions/70/samples.pdf', 5),
-- Phase 8: Test Mastery
(3, 8, NULL, 'Mastery Test Pack', 'Set of difficult practice tests.', 'DOCUMENT', '/materials/phases/8/test-pack.zip', 5);

-- Course Materials for TOEIC Foundation (Course 4)
INSERT INTO subject_material (subject_id, phase_id, subject_session_id, title, description, material_type, url, uploaded_by) VALUES
-- Course Level
(4, NULL, NULL, 'TOEIC Foundation Syllabus', 'Syllabus for TOEIC beginners.', 'DOCUMENT', '/materials/courses/4/syllabus.pdf', 5),
-- Phase 9: TOEIC Basics
(4, 9, NULL, 'TOEIC Basics Guide', 'Introduction to TOEIC format.', 'DOCUMENT', '/materials/phases/9/guide.pdf', 5),
(4, 9, 95, 'Business Vocab Starter', 'Essential business vocabulary.', 'DOCUMENT', '/materials/sessions/95/vocab.pdf', 5),
-- Phase 10: Practice & Review
(4, 10, NULL, 'Practice Questions Set', 'Set of practice questions.', 'DOCUMENT', '/materials/phases/10/questions.pdf', 5),
(4, 10, 105, 'Listening Part 1 Practice', 'Photos for listening practice.', 'DOCUMENT', '/materials/sessions/105/photos.pdf', 5);

-- Course Materials for TOEIC Intermediate (Course 5)
INSERT INTO subject_material (subject_id, phase_id, subject_session_id, title, description, material_type, url, uploaded_by) VALUES
-- Course Level
(5, NULL, NULL, 'TOEIC Intermediate Syllabus', 'Syllabus for intermediate learners.', 'DOCUMENT', '/materials/courses/5/syllabus.pdf', 5),
-- Phase 11: Business Vocab & Grammar
(5, 11, NULL, 'Business Grammar Handbook', 'Grammar for business contexts.', 'DOCUMENT', '/materials/phases/11/grammar.pdf', 5),
(5, 11, 115, 'Meeting Terminology', 'Vocabulary for business meetings.', 'DOCUMENT', '/materials/sessions/115/meetings.pdf', 5),
-- Phase 12: Test Techniques
(5, 12, NULL, 'TOEIC Techniques Guide', 'Tips and tricks for the test.', 'DOCUMENT', '/materials/phases/12/techniques.pdf', 5),
(5, 12, 127, 'Reading Part 7 Strategies', 'Strategies for reading passages.', 'DOCUMENT', '/materials/sessions/127/strategies.pdf', 5);

-- Course Materials for TOEIC Advanced (Course 6)
INSERT INTO subject_material (subject_id, phase_id, subject_session_id, title, description, material_type, url, uploaded_by) VALUES
-- Course Level
(6, NULL, NULL, 'TOEIC Advanced Syllabus', 'Syllabus for advanced learners.', 'DOCUMENT', '/materials/courses/6/syllabus.pdf', 5),
-- Phase 13: Adv Business Comm
(6, 13, NULL, 'Advanced Communication Guide', 'Guide to executive communication.', 'DOCUMENT', '/materials/phases/13/guide.pdf', 5),
(6, 13, 139, 'Negotiation Case Studies', 'Real-world negotiation scenarios.', 'DOCUMENT', '/materials/sessions/139/cases.pdf', 5),
-- Phase 14: Executive Prep
(6, 14, NULL, 'Executive Prep Pack', 'Preparation materials for high scores.', 'DOCUMENT', '/materials/phases/14/prep.pdf', 5),
(6, 14, 153, 'Full Simulation Test', 'Full-length simulation test.', 'DOCUMENT', '/materials/sessions/153/simulation.pdf', 5);

-- Course Assessments for Foundation
INSERT INTO subject_assessment (id, subject_id, name, kind, duration_minutes, max_score, skill, created_at, updated_at) VALUES
(1, 1, 'Listening Quiz 1', 'QUIZ', 30, 20, 'LISTENING', '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(2, 1, 'Speaking Quiz 1', 'QUIZ', 15, 20, 'SPEAKING', '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(3, 1, 'Reading Quiz 1', 'QUIZ', 30, 20, 'READING', '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(4, 1, 'Writing Assignment 1', 'HOMEWORK', 60, 20, 'WRITING', '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(5, 1, 'Midterm Exam', 'MIDTERM', 90, 100, 'GENERAL', '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(6, 1, 'Final Exam', 'FINAL', 120, 100, 'GENERAL', '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07');

-- Course Assessment-CLO Mappings
INSERT INTO subject_assessment_clo_mapping (subject_assessment_id, clo_id, status) VALUES
(1, 1, 'ACTIVE'),
(2, 2, 'ACTIVE'),
(3, 3, 'ACTIVE'),
(4, 4, 'ACTIVE'),
(5, 1, 'ACTIVE'), (5, 2, 'ACTIVE'), (5, 3, 'ACTIVE'), (5, 4, 'ACTIVE'),
(6, 1, 'ACTIVE'), (6, 2, 'ACTIVE'), (6, 3, 'ACTIVE'), (6, 4, 'ACTIVE');

-- Course Assessments for Intermediate (ID 2)
INSERT INTO subject_assessment (id, subject_id, name, kind, duration_minutes, max_score, skill, created_at, updated_at) VALUES
(7, 2, 'Writing Task 2 Assignment', 'HOMEWORK', 60, 100, 'WRITING', NOW(), NOW()),
(8, 2, 'Full Mock Test', 'FINAL', 180, 100, 'GENERAL', NOW(), NOW());

-- Course Assessments for IELTS Advanced (ID 3)
INSERT INTO subject_assessment (id, subject_id, name, kind, duration_minutes, max_score, skill, created_at, updated_at) VALUES
(9, 3, 'Advanced Writing Portfolio', 'HOMEWORK', 120, 100, 'WRITING', NOW(), NOW()),
(10, 3, 'Speaking Part 3 Mastery', 'QUIZ', 20, 100, 'SPEAKING', NOW(), NOW()),
(11, 3, 'Full Mock Test', 'FINAL', 180, 100, 'GENERAL', NOW(), NOW());

-- Course Assessments for TOEIC Foundation (ID 4)
INSERT INTO subject_assessment (id, subject_id, name, kind, duration_minutes, max_score, skill, created_at, updated_at) VALUES
(12, 4, 'Listening Mini-Test', 'QUIZ', 30, 100, 'LISTENING', NOW(), NOW()),
(13, 4, 'Reading Mini-Test', 'QUIZ', 30, 100, 'READING', NOW(), NOW()),
(14, 4, 'Final TOEIC Test', 'FINAL', 120, 990, 'GENERAL', NOW(), NOW());

-- Course Assessments for TOEIC Intermediate (ID 5)
INSERT INTO subject_assessment (id, subject_id, name, kind, duration_minutes, max_score, skill, created_at, updated_at) VALUES
(15, 5, 'Midterm TOEIC Test', 'MIDTERM', 120, 990, 'GENERAL', NOW(), NOW()),
(16, 5, 'Final TOEIC Test', 'FINAL', 120, 990, 'GENERAL', NOW(), NOW());

-- Course Assessments for TOEIC Advanced (ID 6)
INSERT INTO subject_assessment (id, subject_id, name, kind, duration_minutes, max_score, skill, created_at, updated_at) VALUES
(17, 6, 'Business Case Study', 'HOMEWORK', 120, 100, 'WRITING', NOW(), NOW()),
(18, 6, 'Final TOEIC Test', 'FINAL', 120, 990, 'GENERAL', NOW(), NOW());

-- -- Course Materials for Intermediate (ID 2)
-- INSERT INTO subject_material (subject_id, phase_id, subject_session_id, title, description, material_type, url, uploaded_by) VALUES
-- (2, NULL, NULL, 'IELTS Intermediate Syllabus', 'Course syllabus', 'DOCUMENT', '/materials/courses/2/syllabus.pdf', 5),
-- (2, 3, 25, 'Complex Grammar Guide', 'Guide to complex sentences', 'DOCUMENT', '/materials/sessions/25/grammar.pdf', 5);

-- ========== TIER 4: CLASSES & SESSIONS ==========

-- Classes: 18 total (3 per course x 6 courses), NO HYBRID
INSERT INTO "class" (id, branch_id, subject_id, code, name, modality, start_date, planned_end_date, actual_end_date, schedule_days, max_capacity, status, approval_status, rejection_reason, created_by, decided_by, submitted_at, decided_at, created_at, updated_at) VALUES
-- IELTS Foundation (Course 1) - 3 classes (Staggered: Fast, Standard, Slow)
(1, 1, 1, 'HN-IELTS-F1', 'HN IELTS Foundation 1 (Fast)', 'OFFLINE', '2025-10-27', '2025-12-19', NULL, ARRAY[1,3,5]::smallint[], 20, 'ONGOING', 'APPROVED', NULL, 6, 3, '2025-10-20 10:00:00+07', '2025-10-21 14:00:00+07', '2025-10-20 10:00:00+07', NOW()),
(2, 1, 1, 'HN-IELTS-F2', 'HN IELTS Foundation 2 (Standard)', 'OFFLINE', '2025-11-14', '2026-01-08', NULL, ARRAY[1,3,5]::smallint[], 20, 'ONGOING', 'APPROVED', NULL, 6, 3, '2025-11-01 10:00:00+07', '2025-11-02 14:00:00+07', '2025-11-01 10:00:00+07', NOW()),
(3, 1, 1, 'HN-IELTS-F3', 'HN IELTS Foundation 3 (Slow)', 'OFFLINE', '2025-12-03', '2026-01-26', NULL, ARRAY[1,3,5]::smallint[], 20, 'ONGOING', 'APPROVED', NULL, 6, 3, '2025-11-25 10:00:00+07', '2025-11-26 14:00:00+07', '2025-11-25 10:00:00+07', NOW()),

-- IELTS Intermediate (Course 2) - 3 classes (Staggered: Fast, Standard, Slow)
(4, 1, 2, 'HN-IELTS-I1', 'HN IELTS Intermediate 1 (Fast)', 'OFFLINE', '2025-10-27', '2025-12-19', NULL, ARRAY[1,3,5]::smallint[], 20, 'ONGOING', 'APPROVED', NULL, 6, 3, '2025-10-20 10:00:00+07', '2025-10-21 14:00:00+07', '2025-10-20 10:00:00+07', NOW()),
(5, 1, 2, 'HN-IELTS-I2', 'HN IELTS Intermediate 2 (Standard)', 'OFFLINE', '2025-11-14', '2026-01-08', NULL, ARRAY[1,3,5]::smallint[], 20, 'ONGOING', 'APPROVED', NULL, 6, 3, '2025-11-01 10:00:00+07', '2025-11-02 14:00:00+07', '2025-11-01 10:00:00+07', NOW()),
(6, 1, 2, 'HN-IELTS-I3', 'HN IELTS Intermediate 3 (Slow)', 'OFFLINE', '2025-12-03', '2026-01-26', NULL, ARRAY[1,3,5]::smallint[], 20, 'ONGOING', 'APPROVED', NULL, 6, 3, '2025-11-25 10:00:00+07', '2025-11-26 14:00:00+07', '2025-11-25 10:00:00+07', NOW()),

-- IELTS Advanced (Course 3) - 3 classes (40 sessions)
(7, 1, 3, 'HN-IELTS-A1', 'HN IELTS Advanced 1', 'ONLINE', '2025-09-22', '2026-01-09', '2026-01-09', ARRAY[1,3,5]::smallint[], 15, 'COMPLETED', 'APPROVED', NULL, 6, 3, '2025-09-15 10:00:00+07', '2025-09-16 14:00:00+07', '2025-09-15 10:00:00+07', '2026-01-09 18:00:00+07'),
(8, 1, 3, 'HN-IELTS-A2', 'HN IELTS Advanced 2', 'OFFLINE', '2025-12-08', '2026-03-27', NULL, ARRAY[2,4,6]::smallint[], 15, 'SCHEDULED', 'APPROVED', NULL, 6, 3, '2025-12-01 10:00:00+07', '2025-12-02 14:00:00+07', '2025-12-01 10:00:00+07', '2025-12-02 14:00:00+07'),
(9, 2, 3, 'HCM-IELTS-A1', 'HCM IELTS Advanced 1', 'OFFLINE', '2025-11-24', '2026-03-13', NULL, ARRAY[1,3,5]::smallint[], 15, 'ONGOING', 'APPROVED', NULL, 8, 4, '2025-11-09 10:00:00+07', '2025-11-10 14:00:00+07', '2025-11-09 10:00:00+07', '2025-11-24 08:00:00+07'),

-- TOEIC Foundation (Course 4) - 3 classes
(10, 1, 4, 'HN-TOEIC-F1', 'HN TOEIC Foundation 1', 'OFFLINE', '2025-10-01', '2025-11-20', '2025-11-20', ARRAY[1,3,5]::smallint[], 20, 'COMPLETED', 'APPROVED', NULL, 6, 3, '2025-09-20 10:00:00+07', '2025-09-21 14:00:00+07', '2025-09-20 10:00:00+07', '2025-11-20 18:00:00+07'),
(11, 1, 4, 'HN-TOEIC-F2', 'HN TOEIC Foundation 2', 'ONLINE', '2025-12-01', '2026-01-20', NULL, ARRAY[2,4,6]::smallint[], 25, 'ONGOING', 'APPROVED', NULL, 6, 3, '2025-11-20 10:00:00+07', '2025-11-21 14:00:00+07', '2025-11-20 10:00:00+07', '2025-12-01 08:00:00+07'),
(12, 2, 4, 'HCM-TOEIC-F1', 'HCM TOEIC Foundation 1', 'OFFLINE', '2025-11-25', '2026-01-14', NULL, ARRAY[1,3,5]::smallint[], 20, 'ONGOING', 'APPROVED', NULL, 8, 4, '2025-11-15 10:00:00+07', '2025-11-16 14:00:00+07', '2025-11-15 10:00:00+07', '2025-11-25 08:00:00+07'),

-- TOEIC Intermediate (Course 5) - 3 classes
(13, 1, 5, 'HN-TOEIC-I1', 'HN TOEIC Intermediate 1', 'OFFLINE', '2025-10-08', '2025-12-05', '2025-12-05', ARRAY[2,4,6]::smallint[], 18, 'COMPLETED', 'APPROVED', NULL, 6, 3, '2025-09-28 10:00:00+07', '2025-09-29 14:00:00+07', '2025-09-28 10:00:00+07', '2025-12-05 18:00:00+07'),
(14, 1, 5, 'HN-TOEIC-I2', 'HN TOEIC Intermediate 2', 'ONLINE', '2025-12-10', '2026-02-06', NULL, ARRAY[1,3,5]::smallint[], 20, 'SCHEDULED', 'APPROVED', NULL, 6, 3, '2025-12-01 10:00:00+07', '2025-12-02 14:00:00+07', '2025-12-01 10:00:00+07', '2025-12-02 14:00:00+07'),
(15, 2, 5, 'HCM-TOEIC-I1', 'HCM TOEIC Intermediate 1', 'OFFLINE', '2025-12-01', '2026-01-28', NULL, ARRAY[2,4,6]::smallint[], 18, 'ONGOING', 'APPROVED', NULL, 8, 4, '2025-11-20 10:00:00+07', '2025-11-21 14:00:00+07', '2025-11-20 10:00:00+07', '2025-12-01 08:00:00+07'),

-- TOEIC Advanced (Course 6) - 3 classes
(16, 1, 6, 'HN-TOEIC-A1', 'HN TOEIC Advanced 1', 'OFFLINE', '2025-10-15', '2025-12-20', '2025-12-05', ARRAY[1,3,5]::smallint[], 15, 'COMPLETED', 'APPROVED', NULL, 6, 3, '2025-10-05 10:00:00+07', '2025-10-06 14:00:00+07', '2025-10-05 10:00:00+07', '2025-12-05 18:00:00+07'),
(17, 1, 6, 'HN-TOEIC-A2', 'HN TOEIC Advanced 2', 'ONLINE', '2025-12-15', '2026-02-20', NULL, ARRAY[2,4,6]::smallint[], 15, 'SCHEDULED', 'APPROVED', NULL, 6, 3, '2025-12-05 10:00:00+07', '2025-12-06 14:00:00+07', '2025-12-05 10:00:00+07', '2025-12-06 14:00:00+07'),
(18, 2, 6, 'HCM-TOEIC-A1', 'HCM TOEIC Advanced 1', 'OFFLINE', '2025-12-08', '2026-02-12', NULL, ARRAY[1,3,5]::smallint[], 15, 'ONGOING', 'APPROVED', NULL, 8, 4, '2025-11-25 10:00:00+07', '2025-11-26 14:00:00+07', '2025-11-25 10:00:00+07', '2025-12-08 08:00:00+07');

-- Generate Sessions for Class 1 (HN-IELTS-F1 - Fast)
INSERT INTO session (id, class_id, subject_session_id, time_slot_template_id, date, type, status, teacher_note, created_at, updated_at)
SELECT s.idx, 1, s.idx, 1, ('2025-10-27'::date + ((s.idx - 1) / 3) * 7 + CASE (s.idx - 1) % 3 WHEN 0 THEN 0 WHEN 1 THEN 2 ELSE 4 END), 'CLASS',
CASE WHEN ('2025-10-27'::date + ((s.idx - 1) / 3) * 7 + CASE (s.idx - 1) % 3 WHEN 0 THEN 0 WHEN 1 THEN 2 ELSE 4 END) < '2025-12-10' THEN 'DONE' ELSE 'PLANNED' END,
'Session note.', '2025-10-20 10:00:00+07', NOW()
FROM generate_series(1, 24) AS s(idx);

INSERT INTO session_resource (session_id, resource_id) SELECT id, 1 FROM session WHERE class_id = 1;
INSERT INTO teaching_slot (session_id, teacher_id, status) SELECT id, 1, 'SCHEDULED' FROM session WHERE class_id = 1;

-- Generate Sessions for Class 2 (HN-IELTS-F2 - Standard)
INSERT INTO session (id, class_id, subject_session_id, time_slot_template_id, date, type, status, created_at, updated_at)
SELECT 100 + s.idx, 2, s.idx, 1, ('2025-11-14'::date + ((s.idx - 1) / 3) * 7 + CASE (s.idx - 1) % 3 WHEN 0 THEN 0 WHEN 1 THEN 2 ELSE 4 END), 'CLASS',
  CASE WHEN ('2025-11-14'::date + ((s.idx - 1) / 3) * 7 + CASE (s.idx - 1) % 3 WHEN 0 THEN 0 WHEN 1 THEN 2 ELSE 4 END) < '2025-12-10' THEN 'DONE' ELSE 'PLANNED' END, '2025-11-01 10:00:00+07', NOW()
FROM generate_series(1, 24) AS s(idx);

INSERT INTO session_resource (session_id, resource_id) SELECT id, 2 FROM session WHERE class_id = 2;
INSERT INTO teaching_slot (session_id, teacher_id, status) SELECT id, 2, 'SCHEDULED' FROM session WHERE class_id = 2;

-- Generate Sessions for Class 3 (HN-IELTS-F3 - Slow)
INSERT INTO session (id, class_id, subject_session_id, time_slot_template_id, date, type, status, created_at, updated_at)
SELECT 200 + s.idx, 3, s.idx, 1, ('2025-12-03'::date + ((s.idx - 1) / 3) * 7 + CASE (s.idx - 1) % 3 WHEN 0 THEN 0 WHEN 1 THEN 2 ELSE 4 END), 'CLASS',
  CASE WHEN ('2025-12-03'::date + ((s.idx - 1) / 3) * 7 + CASE (s.idx - 1) % 3 WHEN 0 THEN 0 WHEN 1 THEN 2 ELSE 4 END) < '2025-12-10' THEN 'DONE' ELSE 'PLANNED' END, '2025-11-25 10:00:00+07', NOW()
FROM generate_series(1, 24) AS s(idx);

INSERT INTO session_resource (session_id, resource_id) SELECT id, 3 FROM session WHERE class_id = 3;
INSERT INTO teaching_slot (session_id, teacher_id, status) SELECT id, 3, 'SCHEDULED' FROM session WHERE class_id = 3;

-- Generate Sessions for Class 4 (HN-IELTS-I1 - Fast)
INSERT INTO session (id, class_id, subject_session_id, time_slot_template_id, date, type, status, teacher_note, created_at, updated_at)
SELECT 300 + s.idx, 4, 24 + s.idx, 2, ('2025-10-27'::date + ((s.idx - 1) / 3) * 7 + CASE (s.idx - 1) % 3 WHEN 0 THEN 0 WHEN 1 THEN 2 ELSE 4 END), 'CLASS',
CASE WHEN ('2025-10-27'::date + ((s.idx - 1) / 3) * 7 + CASE (s.idx - 1) % 3 WHEN 0 THEN 0 WHEN 1 THEN 2 ELSE 4 END) < '2025-12-10' THEN 'DONE' ELSE 'PLANNED' END,
'Session note.', '2025-10-20 10:00:00+07', NOW()
FROM generate_series(1, 24) AS s(idx);

INSERT INTO session_resource (session_id, resource_id) SELECT id, 9 FROM session WHERE class_id = 4;
INSERT INTO teaching_slot (session_id, teacher_id, status) SELECT id, 4, 'SCHEDULED' FROM session WHERE class_id = 4;

-- Generate Sessions for Class 5 (HN-IELTS-I2 - Standard)
INSERT INTO session (id, class_id, subject_session_id, time_slot_template_id, date, type, status, created_at, updated_at)
SELECT 400 + s.idx, 5, 24 + s.idx, 2, ('2025-11-14'::date + ((s.idx - 1) / 3) * 7 + CASE (s.idx - 1) % 3 WHEN 0 THEN 0 WHEN 1 THEN 2 ELSE 4 END), 'CLASS',
  CASE WHEN ('2025-11-14'::date + ((s.idx - 1) / 3) * 7 + CASE (s.idx - 1) % 3 WHEN 0 THEN 0 WHEN 1 THEN 2 ELSE 4 END) < '2025-12-10' THEN 'DONE' ELSE 'PLANNED' END, '2025-11-01 10:00:00+07', NOW()
FROM generate_series(1, 24) AS s(idx);

INSERT INTO session_resource (session_id, resource_id) SELECT id, 10 FROM session WHERE class_id = 5;
INSERT INTO teaching_slot (session_id, teacher_id, status) SELECT id, 5, 'SCHEDULED' FROM session WHERE class_id = 5;

-- Generate Sessions for Class 6 (HN-IELTS-I3 - Slow)
INSERT INTO session (id, class_id, subject_session_id, time_slot_template_id, date, type, status, created_at, updated_at)
SELECT 500 + s.idx, 6, 24 + s.idx, 2, ('2025-12-03'::date + ((s.idx - 1) / 3) * 7 + CASE (s.idx - 1) % 3 WHEN 0 THEN 0 WHEN 1 THEN 2 ELSE 4 END), 'CLASS',
  CASE WHEN ('2025-12-03'::date + ((s.idx - 1) / 3) * 7 + CASE (s.idx - 1) % 3 WHEN 0 THEN 0 WHEN 1 THEN 2 ELSE 4 END) < '2025-12-10' THEN 'DONE' ELSE 'PLANNED' END, '2025-11-25 10:00:00+07', NOW()
FROM generate_series(1, 24) AS s(idx);

INSERT INTO session_resource (session_id, resource_id) SELECT id, 11 FROM session WHERE class_id = 6;
INSERT INTO teaching_slot (session_id, teacher_id, status) SELECT id, 6, 'SCHEDULED' FROM session WHERE class_id = 6;

-- Generate Sessions for Class 7 (HN-IELTS-A1) - COMPLETED
INSERT INTO session (id, class_id, subject_session_id, time_slot_template_id, date, type, status, teacher_note, created_at, updated_at)
SELECT 600 + s.idx, 7, 54 + s.idx, 5, ('2025-09-22'::date + ((s.idx - 1) / 3) * 7 + CASE (s.idx - 1) % 3 WHEN 0 THEN 0 WHEN 1 THEN 2 ELSE 4 END), 'CLASS', 'DONE', 'Completed.', '2025-09-15 10:00:00+07', NOW()
FROM generate_series(1, 40) AS s(idx);

INSERT INTO session_resource (session_id, resource_id) SELECT id, 4 FROM session WHERE class_id = 7;
INSERT INTO teaching_slot (session_id, teacher_id, status) SELECT id, 5, 'SCHEDULED' FROM session WHERE class_id = 7;

-- Generate Sessions for Class 8 (HN-IELTS-A2) - SCHEDULED
INSERT INTO session (id, class_id, subject_session_id, time_slot_template_id, date, type, status, created_at, updated_at)
SELECT 700 + s.idx, 8, 54 + s.idx, 6, ('2025-12-08'::date + ((s.idx - 1) / 3) * 7 + CASE (s.idx - 1) % 3 WHEN 0 THEN 1 WHEN 1 THEN 3 ELSE 5 END), 'CLASS', 'PLANNED', '2025-12-01 10:00:00+07', NOW()
FROM generate_series(1, 40) AS s(idx);

INSERT INTO session_resource (session_id, resource_id) SELECT id, 3 FROM session WHERE class_id = 8;
INSERT INTO teaching_slot (session_id, teacher_id, status) SELECT id, 6, 'SCHEDULED' FROM session WHERE class_id = 8;

-- Generate Sessions for Class 9 (HCM-IELTS-A1) - ONGOING
INSERT INTO session (id, class_id, subject_session_id, time_slot_template_id, date, type, status, created_at, updated_at)
SELECT 800 + s.idx, 9, 54 + s.idx, 11, ('2025-11-24'::date + ((s.idx - 1) / 3) * 7 + CASE (s.idx - 1) % 3 WHEN 0 THEN 0 WHEN 1 THEN 2 ELSE 4 END), 'CLASS',
  CASE WHEN ('2025-11-24'::date + ((s.idx - 1) / 3) * 7 + CASE (s.idx - 1) % 3 WHEN 0 THEN 0 WHEN 1 THEN 2 ELSE 4 END) < '2025-12-05' THEN 'DONE' ELSE 'PLANNED' END, '2025-11-09 10:00:00+07', NOW()
FROM generate_series(1, 40) AS s(idx);

INSERT INTO session_resource (session_id, resource_id) SELECT id, 6 FROM session WHERE class_id = 9;
INSERT INTO teaching_slot (session_id, teacher_id, status) SELECT id, 11, 'SCHEDULED' FROM session WHERE class_id = 9;

-- Generate Sessions for Class 10 (HN-TOEIC-F1) - COMPLETED
INSERT INTO session (id, class_id, subject_session_id, time_slot_template_id, date, type, status, teacher_note, created_at, updated_at)
SELECT 900 + s.idx, 10, 94 + s.idx, 2, ('2025-10-01'::date + ((s.idx - 1) / 3) * 7 + CASE (s.idx - 1) % 3 WHEN 0 THEN 0 WHEN 1 THEN 2 ELSE 4 END), 'CLASS', 'DONE', 'Completed.', '2025-09-20 10:00:00+07', NOW()
FROM generate_series(1, 20) AS s(idx);

INSERT INTO session_resource (session_id, resource_id) SELECT id, 1 FROM session WHERE class_id = 10;
INSERT INTO teaching_slot (session_id, teacher_id, status) SELECT id, 17, 'SCHEDULED' FROM session WHERE class_id = 10;

-- Generate Sessions for Class 11 (HN-TOEIC-F2) - ONGOING ONLINE
INSERT INTO session (id, class_id, subject_session_id, time_slot_template_id, date, type, status, created_at, updated_at)
SELECT 1000 + s.idx, 11, 94 + s.idx, 6, ('2025-12-01'::date + ((s.idx - 1) / 3) * 7 + CASE (s.idx - 1) % 3 WHEN 0 THEN 1 WHEN 1 THEN 3 ELSE 5 END), 'CLASS',
  CASE WHEN ('2025-12-01'::date + ((s.idx - 1) / 3) * 7 + CASE (s.idx - 1) % 3 WHEN 0 THEN 1 WHEN 1 THEN 3 ELSE 5 END) < '2025-12-05' THEN 'DONE' ELSE 'PLANNED' END, '2025-11-20 10:00:00+07', NOW()
FROM generate_series(1, 20) AS s(idx);

INSERT INTO session_resource (session_id, resource_id) SELECT id, 4 FROM session WHERE class_id = 11;
INSERT INTO teaching_slot (session_id, teacher_id, status) SELECT id, 18, 'SCHEDULED' FROM session WHERE class_id = 11;

-- Generate Sessions for Class 12 (HCM-TOEIC-F1) - ONGOING
INSERT INTO session (id, class_id, subject_session_id, time_slot_template_id, date, type, status, created_at, updated_at)
SELECT 1100 + s.idx, 12, 94 + s.idx, 10, ('2025-11-25'::date + ((s.idx - 1) / 3) * 7 + CASE (s.idx - 1) % 3 WHEN 0 THEN 0 WHEN 1 THEN 2 ELSE 4 END), 'CLASS',
  CASE WHEN ('2025-11-25'::date + ((s.idx - 1) / 3) * 7 + CASE (s.idx - 1) % 3 WHEN 0 THEN 0 WHEN 1 THEN 2 ELSE 4 END) < '2025-12-05' THEN 'DONE' ELSE 'PLANNED' END, '2025-11-15 10:00:00+07', NOW()
FROM generate_series(1, 20) AS s(idx);

INSERT INTO session_resource (session_id, resource_id) SELECT id, 5 FROM session WHERE class_id = 12;
INSERT INTO teaching_slot (session_id, teacher_id, status) SELECT id, 22, 'SCHEDULED' FROM session WHERE class_id = 12;

-- Generate Sessions for Class 13 (HN-TOEIC-I1) - COMPLETED
INSERT INTO session (id, class_id, subject_session_id, time_slot_template_id, date, type, status, teacher_note, created_at, updated_at)
SELECT 1200 + s.idx, 13, 114 + s.idx, 3, ('2025-10-08'::date + ((s.idx - 1) / 3) * 7 + CASE (s.idx - 1) % 3 WHEN 0 THEN 1 WHEN 1 THEN 3 ELSE 5 END), 'CLASS', 'DONE', 'Completed.', '2025-09-28 10:00:00+07', NOW()
FROM generate_series(1, 24) AS s(idx);

INSERT INTO session_resource (session_id, resource_id) SELECT id, 2 FROM session WHERE class_id = 13;
INSERT INTO teaching_slot (session_id, teacher_id, status) SELECT id, 17, 'SCHEDULED' FROM session WHERE class_id = 13;

-- Generate Sessions for Class 14 (HN-TOEIC-I2) - SCHEDULED ONLINE
INSERT INTO session (id, class_id, subject_session_id, time_slot_template_id, date, type, status, created_at, updated_at)
SELECT 1300 + s.idx, 14, s.idx, 5, ('2025-12-10'::date + ((s.idx - 1) / 3) * 7 + CASE (s.idx - 1) % 3 WHEN 0 THEN 0 WHEN 1 THEN 2 ELSE 4 END), 'CLASS', 'PLANNED', '2025-12-01 10:00:00+07', NOW()
FROM generate_series(1, 24) AS s(idx);

INSERT INTO session_resource (session_id, resource_id) SELECT id, 4 FROM session WHERE class_id = 14;
INSERT INTO teaching_slot (session_id, teacher_id, status) SELECT id, 18, 'SCHEDULED' FROM session WHERE class_id = 14;

-- Generate Sessions for Class 15 (HCM-TOEIC-I1) - ONGOING
INSERT INTO session (id, class_id, subject_session_id, time_slot_template_id, date, type, status, created_at, updated_at)
SELECT 1400 + s.idx, 15, s.idx, 14, ('2025-12-01'::date + ((s.idx - 1) / 3) * 7 + CASE (s.idx - 1) % 3 WHEN 0 THEN 1 WHEN 1 THEN 3 ELSE 5 END), 'CLASS',
  CASE WHEN ('2025-12-01'::date + ((s.idx - 1) / 3) * 7 + CASE (s.idx - 1) % 3 WHEN 0 THEN 1 WHEN 1 THEN 3 ELSE 5 END) < '2025-12-05' THEN 'DONE' ELSE 'PLANNED' END, '2025-11-20 10:00:00+07', NOW()
FROM generate_series(1, 24) AS s(idx);

INSERT INTO session_resource (session_id, resource_id) SELECT id, 7 FROM session WHERE class_id = 15;
INSERT INTO teaching_slot (session_id, teacher_id, status) SELECT id, 22, 'SCHEDULED' FROM session WHERE class_id = 15;

-- Generate Sessions for Class 16 (HN-TOEIC-A1) - COMPLETED
INSERT INTO session (id, class_id, subject_session_id, time_slot_template_id, date, type, status, teacher_note, created_at, updated_at)
SELECT 1500 + s.idx, 16, 138 + s.idx, 2, ('2025-10-15'::date + ((s.idx - 1) / 3) * 7 + CASE (s.idx - 1) % 3 WHEN 0 THEN 0 WHEN 1 THEN 2 ELSE 4 END), 'CLASS', 'DONE', 'Completed.', '2025-10-05 10:00:00+07', NOW()
FROM generate_series(1, 28) AS s(idx);

INSERT INTO session_resource (session_id, resource_id) SELECT id, 3 FROM session WHERE class_id = 16;
INSERT INTO teaching_slot (session_id, teacher_id, status) SELECT id, 17, 'SCHEDULED' FROM session WHERE class_id = 16;

-- Generate Sessions for Class 17 (HN-TOEIC-A2) - SCHEDULED ONLINE
INSERT INTO session (id, class_id, subject_session_id, time_slot_template_id, date, type, status, created_at, updated_at)
SELECT 1600 + s.idx, 17, 138 + s.idx, 6, ('2025-12-15'::date + ((s.idx - 1) / 3) * 7 + CASE (s.idx - 1) % 3 WHEN 0 THEN 1 WHEN 1 THEN 3 ELSE 5 END), 'CLASS', 'PLANNED', '2025-12-05 10:00:00+07', NOW()
FROM generate_series(1, 28) AS s(idx);

INSERT INTO session_resource (session_id, resource_id) SELECT id, 4 FROM session WHERE class_id = 17;
INSERT INTO teaching_slot (session_id, teacher_id, status) SELECT id, 18, 'SCHEDULED' FROM session WHERE class_id = 17;

-- Generate Sessions for Class 18 (HCM-TOEIC-A1) - ONGOING
INSERT INTO session (id, class_id, subject_session_id, time_slot_template_id, date, type, status, created_at, updated_at)
SELECT 1700 + s.idx, 18, 138 + s.idx, 11, ('2025-12-08'::date + ((s.idx - 1) / 3) * 7 + CASE (s.idx - 1) % 3 WHEN 0 THEN 0 WHEN 1 THEN 2 ELSE 4 END), 'CLASS', 'PLANNED', '2025-11-25 10:00:00+07', NOW()
FROM generate_series(1, 28) AS s(idx);

INSERT INTO session_resource (session_id, resource_id) SELECT id, 6 FROM session WHERE class_id = 18;
INSERT INTO teaching_slot (session_id, teacher_id, status) SELECT id, 22, 'SCHEDULED' FROM session WHERE class_id = 18;


-- ========== TIER 5: ENROLLMENTS & ATTENDANCE ==========

-- Enrollments for Class 1 (HN-IELTS-F1) - 15 students (IDs 2-16)
INSERT INTO enrollment (id, class_id, student_id, status, enrolled_at, enrolled_by, join_session_id, created_at, updated_at)
SELECT (100 + s.id), 1, s.id + 1, 'ENROLLED', '2025-10-20 09:00:00+07', 6, 1, '2025-10-20 09:00:00+07', NOW()
FROM generate_series(1, 15) AS s(id);

-- Enrollments for Class 2 (HN-IELTS-F2) - 20 students (Student 1 + IDs 17-35)
INSERT INTO enrollment (id, class_id, student_id, status, enrolled_at, enrolled_by, join_session_id, created_at, updated_at) VALUES
(200, 2, 1, 'ENROLLED', '2025-11-01 09:00:00+07', 6, 101, '2025-11-01 09:00:00+07', NOW());

INSERT INTO enrollment (id, class_id, student_id, status, enrolled_at, enrolled_by, join_session_id, created_at, updated_at)
SELECT (201 + s.id), 2, s.id + 16, 'ENROLLED', '2025-11-01 09:00:00+07', 6, 101, '2025-11-01 09:00:00+07', NOW()
FROM generate_series(1, 19) AS s(id);

-- Enrollments for Class 3 (HN-IELTS-F3) - 20 students
INSERT INTO enrollment (id, class_id, student_id, status, enrolled_at, enrolled_by, join_session_id, created_at, updated_at)
SELECT (300 + s.id), 3, s.id + 35, 'ENROLLED', '2025-11-25 09:00:00+07', 6, 201, '2025-11-25 09:00:00+07', NOW()
FROM generate_series(1, 20) AS s(id);

-- Enrollments for Class 4 (HN-IELTS-I1) - 18 students
INSERT INTO enrollment (id, class_id, student_id, status, enrolled_at, enrolled_by, join_session_id, created_at, updated_at)
SELECT (400 + s.id), 4, s.id + 55, 'ENROLLED', '2025-10-20 09:00:00+07', 6, 301, '2025-10-20 09:00:00+07', NOW()
FROM generate_series(1, 18) AS s(id);

-- Enrollments for Class 5 (HN-IELTS-I2) - 15 students
INSERT INTO enrollment (id, class_id, student_id, status, enrolled_at, enrolled_by, join_session_id, created_at, updated_at)
SELECT (500 + s.id), 5, s.id + 73, 'ENROLLED', '2025-11-07 09:00:00+07', 6, 401, '2025-11-07 09:00:00+07', NOW()
FROM generate_series(1, 15) AS s(id);

-- Enrollments for Class 6 (HN-IELTS-I3) - 12 students (Remaining)
INSERT INTO enrollment (id, class_id, student_id, status, enrolled_at, enrolled_by, join_session_id, created_at, updated_at)
SELECT (600 + s.id), 6, s.id + 88, 'ENROLLED', '2025-11-25 09:00:00+07', 6, 501, '2025-11-25 09:00:00+07', NOW()
FROM generate_series(1, 12) AS s(id);

-- Enrollments for Class 7 (HN-IELTS-A1) - 12 students, completed
INSERT INTO enrollment (id, class_id, student_id, status, enrolled_at, enrolled_by, join_session_id, left_at, left_session_id, created_at, updated_at)
SELECT (700 + s.id), 7, s.id + 15, 'COMPLETED', '2025-09-15 09:00:00+07', 6, 601, '2025-12-01 18:00:00+07', 624, '2025-09-15 09:00:00+07', '2025-12-01 18:00:00+07'
FROM generate_series(1, 12) AS s(id);

-- Enrollments for Class 9 (HCM-IELTS-A1) - 12 students, ongoing
INSERT INTO enrollment (id, class_id, student_id, status, enrolled_at, enrolled_by, join_session_id, created_at, updated_at)
SELECT (900 + s.id), 9, s.id + 88, 'ENROLLED', '2025-11-09 09:00:00+07', 8, 801, '2025-11-09 09:00:00+07', NOW()
FROM generate_series(1, 12) AS s(id);

-- Enrollments for Class 10 (HN-TOEIC-F1) - 18 students, completed
INSERT INTO enrollment (id, class_id, student_id, status, enrolled_at, enrolled_by, join_session_id, left_at, left_session_id, created_at, updated_at)
SELECT (1000 + s.id), 10, s.id + 27, 'COMPLETED', '2025-09-20 09:00:00+07', 6, 901, '2025-11-20 18:00:00+07', 920, '2025-09-20 09:00:00+07', '2025-11-20 18:00:00+07'
FROM generate_series(1, 18) AS s(id);

-- Enrollments for Class 11 (HN-TOEIC-F2) - 22 students, ongoing online
INSERT INTO enrollment (id, class_id, student_id, status, enrolled_at, enrolled_by, join_session_id, created_at, updated_at)
SELECT (1100 + s.id), 11, s.id + 45, 'ENROLLED', '2025-11-20 09:00:00+07', 6, 1001, '2025-11-20 09:00:00+07', NOW()
FROM generate_series(1, 22) AS s(id);

-- Enrollments for Class 12 (HCM-TOEIC-F1) - 18 students, ongoing
INSERT INTO enrollment (id, class_id, student_id, status, enrolled_at, enrolled_by, join_session_id, created_at, updated_at)
SELECT (1200 + s.id), 12, s.id + 67, 'ENROLLED', '2025-11-15 09:00:00+07', 8, 1101, '2025-11-15 09:00:00+07', NOW()
FROM generate_series(1, 18) AS s(id);

-- Enrollments for Class 13 (HN-TOEIC-I1) - 16 students, completed
INSERT INTO enrollment (id, class_id, student_id, status, enrolled_at, enrolled_by, join_session_id, left_at, left_session_id, created_at, updated_at)
SELECT (1300 + s.id), 13, s.id + 5, 'COMPLETED', '2025-09-28 09:00:00+07', 6, 1201, '2025-12-05 18:00:00+07', 1224, '2025-09-28 09:00:00+07', '2025-12-05 18:00:00+07'
FROM generate_series(1, 16) AS s(id);

-- Enrollments for Class 15 (HCM-TOEIC-I1) - 15 students, ongoing
INSERT INTO enrollment (id, class_id, student_id, status, enrolled_at, enrolled_by, join_session_id, created_at, updated_at)
SELECT (1500 + s.id), 15, s.id + 85, 'ENROLLED', '2025-11-20 09:00:00+07', 8, 1401, '2025-11-20 09:00:00+07', NOW()
FROM generate_series(1, 15) AS s(id);

-- Enrollments for Class 16 (HN-TOEIC-A1) - 13 students, completed
INSERT INTO enrollment (id, class_id, student_id, status, enrolled_at, enrolled_by, join_session_id, left_at, left_session_id, created_at, updated_at)
SELECT (1600 + s.id), 16, s.id + 21, 'COMPLETED', '2025-10-05 09:00:00+07', 6, 1501, '2025-12-05 18:00:00+07', 1528, '2025-10-05 09:00:00+07', '2025-12-05 18:00:00+07'
FROM generate_series(1, 13) AS s(id);

-- Enrollments for Class 18 (HCM-TOEIC-A1) - 12 students, ongoing
INSERT INTO enrollment (id, class_id, student_id, status, enrolled_at, enrolled_by, join_session_id, created_at, updated_at)
SELECT (1800 + s.id), 18, s.id + 75, 'ENROLLED', '2025-11-25 09:00:00+07', 8, 1701, '2025-11-25 09:00:00+07', NOW()
FROM generate_series(1, 12) AS s(id);

-- Student Sessions (Attendance) for ALL classes
-- Generates attendance records for ALL sessions (DONE, PLANNED, IN_PROGRESS)
-- This ensures weekly schedule and absence request features work correctly

-- Step 1: Insert for DONE sessions (with attendance data)
INSERT INTO student_session (student_id, session_id, is_makeup, attendance_status, homework_status, recorded_at, created_at, updated_at)
SELECT 
    e.student_id, 
    s.id, 
    false, 
    CASE WHEN random() < 0.9 THEN 'PRESENT' ELSE 'ABSENT' END,
    CASE WHEN random() < 0.85 THEN 'COMPLETED' ELSE 'INCOMPLETE' END,
    s.date, 
    s.date + interval '2 hours', -- Recorded after class
    s.date + interval '2 hours'
FROM enrollment e 
JOIN session s ON e.class_id = s.class_id
WHERE s.status = 'DONE'
  AND s.id >= e.join_session_id
  AND (e.left_session_id IS NULL OR s.id <= e.left_session_id);

-- Step 2: Insert for PLANNED/IN_PROGRESS sessions (future sessions - no attendance yet)
INSERT INTO student_session (student_id, session_id, is_makeup, attendance_status, created_at, updated_at)
SELECT 
    e.student_id, 
    s.id, 
    false, 
    'PLANNED',
    NOW(),
    NOW()
FROM enrollment e 
JOIN session s ON e.class_id = s.class_id
WHERE s.status IN ('PLANNED', 'IN_PROGRESS')
  AND s.id >= e.join_session_id
  AND (e.left_session_id IS NULL OR s.id <= e.left_session_id)
ON CONFLICT (student_id, session_id) DO NOTHING;

-- ========== PHASE 3: EDGE CASES & ENHANCEMENTS ==========

-- 1. High Absence Scenario (Test Attendance Warnings)
-- Student 17 (in Class 2) has been absent for most sessions
UPDATE student_session
SET attendance_status = 'ABSENT'
WHERE student_id = 17 -- Student ID 17 is enrolled in Class 2 (Enrollment ID 201)
  AND session_id IN (SELECT id FROM session WHERE class_id = 2 AND status = 'DONE');

-- 2. Student.0001 Absent on Session 105 (Class 2 HN-IELTS-F2)
-- student.0001@gmail.com (student_id = 1) absent on session 105
UPDATE student_session
SET attendance_status = 'ABSENT'
WHERE student_id = 1 AND session_id = 105;

-- 3. Teacher Forgot Attendance Scenario (Test Reminders)
-- Note: We no longer delete StudentSession records as they are needed for schedule display
-- Instead, session 105 will have attendance_status = 'PLANNED' to indicate teacher hasn't recorded yet
UPDATE student_session 
SET attendance_status = 'PLANNED', recorded_at = NULL, homework_status = NULL
WHERE session_id = 105;

-- ========== TIER 6: REQUESTS (Simplified for new class structure) ==========

-- Student Request Scenarios (using new class IDs 1-18)
-- Class 1: HN-IELTS-F1 (completed), Class 2: HN-IELTS-F2 (ongoing), Class 3: HCM-IELTS-F1 (ongoing online)

-- SCENARIO 1: Approved Absence Request (Class 2, student 16)
INSERT INTO student_request (id, student_id, current_class_id, request_type, target_session_id, status, request_reason, submitted_by, submitted_at, decided_by, decided_at, note) VALUES
(1, 16, 2, 'ABSENCE', 110, 'APPROVED', 'Family emergency - need to attend urgent family matter', 116, '2025-11-10 10:00:00+07', 6, '2025-11-10 14:00:00+07', 'Approved - valid reason');

-- SCENARIO 2: Pending Absence Request (Class 2, student 17)
INSERT INTO student_request (id, student_id, current_class_id, request_type, target_session_id, status, request_reason, submitted_by, submitted_at) VALUES
(2, 17, 2, 'ABSENCE', 115, 'PENDING', 'Medical appointment - doctor consultation scheduled', 117, '2025-11-15 09:00:00+07');

-- SCENARIO 3: Rejected Absence Request (Class 3, student 51)
INSERT INTO student_request (id, student_id, current_class_id, request_type, target_session_id, status, request_reason, submitted_by, submitted_at, decided_by, decided_at, note) VALUES
(3, 51, 3, 'ABSENCE', 210, 'REJECTED', 'Want to attend friend birthday party', 151, '2025-11-12 10:00:00+07', 8, '2025-11-12 15:00:00+07', 'Rejected - not a valid reason for academic absence');

-- SCENARIO 4: Approved Makeup Request (Class 2 -> Class 3)
INSERT INTO student_request (id, student_id, current_class_id, request_type, target_session_id, makeup_session_id, status, request_reason, submitted_by, submitted_at, decided_by, decided_at) VALUES
(4, 18, 2, 'MAKEUP', 105, 205, 'APPROVED', 'Missed session due to illness, want to makeup in online class', 118, '2025-11-08 10:00:00+07', 6, '2025-11-08 16:00:00+07');

-- SCENARIO 5: Pending Makeup Request (Class 2, student 19)
INSERT INTO student_request (id, student_id, current_class_id, request_type, target_session_id, makeup_session_id, status, request_reason, submitted_by, submitted_at) VALUES
(5, 19, 2, 'MAKEUP', 108, 208, 'PENDING', 'Missed session due to work commitment, requesting makeup', 119, '2025-11-18 11:00:00+07');

-- SCENARIO 6: Approved Transfer Request (Class 2 -> Class 3, student 20)
INSERT INTO student_request (id, student_id, current_class_id, target_class_id, request_type, effective_date, effective_session_id, status, request_reason, submitted_by, submitted_at, decided_by, decided_at) VALUES
(6, 20, 2, 3, 'TRANSFER', '2025-11-20', 215, 'APPROVED', 'Need to change to online class due to work schedule conflict', 6, '2025-11-15 10:00:00+07', 6, '2025-11-16 14:00:00+07');

-- SCENARIO 7: Pending Transfer Request (Class 10 -> Class 11, TOEIC)
INSERT INTO student_request (id, student_id, current_class_id, target_class_id, request_type, effective_date, effective_session_id, status, request_reason, submitted_by, submitted_at, note) VALUES
(7, 28, 10, 11, 'TRANSFER', '2025-12-01', 1010, 'PENDING', 'Need online class due to relocation', 128, '2025-11-25 10:00:00+07', 'Tier 1 self-service request');

-- SCENARIO 8: Rejected Transfer - same class
INSERT INTO student_request (id, student_id, current_class_id, target_class_id, request_type, effective_date, effective_session_id, status, request_reason, submitted_by, submitted_at, decided_by, decided_at, note) VALUES
(8, 21, 2, 2, 'TRANSFER', '2025-11-20', 115, 'REJECTED', 'Accidentally selected current class', 121, '2025-11-10 09:00:00+07', 6, '2025-11-10 15:00:00+07', 'Rejected - cannot transfer to the same class');

-- SCENARIO 9: Request created by Academic Affair on behalf (waiting confirmation)
INSERT INTO student_request (id, student_id, current_class_id, request_type, target_session_id, status, request_reason, submitted_by, submitted_at, note) VALUES
(9, 22, 2, 'ABSENCE', 118, 'WAITING_CONFIRM', 'Student called to report illness - created on behalf', 6, '2025-11-20 13:00:00+07', 'Created by Academic Affair via phone call');

-- Teacher Request Scenarios
-- SCENARIO 10: Teacher Replacement Request - Approved (Class 1, session 10)
INSERT INTO teacher_request (id, teacher_id, session_id, request_type, replacement_teacher_id, status, request_reason, submitted_by, submitted_at, decided_by, decided_at) VALUES
(1, 5, 10, 'REPLACEMENT', 6, 'APPROVED', 'Family emergency - cannot attend session', 24, '2025-09-01 08:00:00+07', 6, '2025-09-01 10:00:00+07');

-- SCENARIO 11: Teacher Reschedule Request - Pending (Class 2)
INSERT INTO teacher_request (id, teacher_id, session_id, request_type, new_date, new_time_slot_id, new_resource_id, status, request_reason, submitted_by, submitted_at) VALUES
(2, 6, 112, 'RESCHEDULE', '2025-11-25', 5, 2, 'PENDING', 'Conference attendance - propose rescheduling to evening slot', 25, '2025-11-15 09:00:00+07');

-- SCENARIO 12: Teacher Modality Change Request - Approved (Class 4)
INSERT INTO teacher_request (id, teacher_id, session_id, request_type, new_resource_id, status, request_reason, submitted_by, submitted_at, decided_by, decided_at) VALUES
(3, 11, 310, 'MODALITY_CHANGE', 4, 'APPROVED', 'Room air conditioning broken - need to switch to online', 30, '2025-10-01 07:00:00+07', 6, '2025-10-01 08:00:00+07');

-- SCENARIO 13: Rejected Teacher Request
INSERT INTO teacher_request (id, teacher_id, session_id, request_type, status, request_reason, submitted_by, submitted_at, decided_by, decided_at) VALUES
(4, 5, 15, 'REPLACEMENT', 'REJECTED', 'Personal reason - insufficient notice', 24, '2025-09-10 08:00:00+07', 6, '2025-09-10 10:00:00+07');

-- SCENARIO 14: Pending Replacement Request (Emma Wilson - teacher 2)
INSERT INTO teacher_request (id, teacher_id, session_id, request_type, status, request_reason, submitted_by, submitted_at) VALUES
(5, 2, 20, 'REPLACEMENT', 'PENDING', 'Medical appointment scheduled - need substitute teacher for this session', 21, '2025-12-01 14:30:00+07');

-- SCENARIO 15: Pending Reschedule Request (David Lee - teacher 3)
INSERT INTO teacher_request (id, teacher_id, session_id, request_type, new_date, new_time_slot_id, new_resource_id, status, request_reason, submitted_by, submitted_at) VALUES
(6, 3, 105, 'RESCHEDULE', '2025-12-20', 3, 1, 'PENDING', 'Personal commitment conflicts with original schedule - request to move to Friday evening', 22, '2025-12-02 09:15:00+07');

-- SCENARIO 16: Pending Modality Change Request (Sarah Johnson - teacher 4)
INSERT INTO teacher_request (id, teacher_id, session_id, request_type, new_resource_id, status, request_reason, submitted_by, submitted_at) VALUES
(7, 4, 210, 'MODALITY_CHANGE', 5, 'PENDING', 'Classroom equipment malfunction - request to switch to online format for this session', 23, '2025-12-03 11:00:00+07');

-- SCENARIO 17: Waiting Confirm Replacement Request (James Taylor - teacher 7)
INSERT INTO teacher_request (id, teacher_id, session_id, request_type, replacement_teacher_id, status, request_reason, submitted_by, submitted_at, decided_by, decided_at) VALUES
(8, 7, 115, 'REPLACEMENT', 8, 'WAITING_CONFIRM', 'Family event - cannot attend session, replacement teacher assigned', 26, '2025-11-28 10:00:00+07', 6, '2025-11-28 14:00:00+07');

-- SCENARIO 18: Pending Replacement Request (Anna Martinez - teacher 8)
INSERT INTO teacher_request (id, teacher_id, session_id, request_type, status, request_reason, submitted_by, submitted_at) VALUES
(9, 8, 120, 'REPLACEMENT', 'PENDING', 'Unexpected travel required - need someone to cover this session', 27, '2025-12-04 08:45:00+07');

-- SCENARIO 19: Approved Reschedule Request (John Smith - teacher 1)
INSERT INTO teacher_request (id, teacher_id, session_id, request_type, new_date, new_time_slot_id, new_resource_id, status, request_reason, submitted_by, submitted_at, decided_by, decided_at) VALUES
(10, 1, 5, 'RESCHEDULE', '2025-09-08', 2, 1, 'APPROVED', 'Conference attendance - reschedule approved', 20, '2025-09-02 09:00:00+07', 6, '2025-09-02 11:00:00+07');

-- ========== TIER 7: ASSESSMENTS & SCORES ==========

-- Assessments for Class 2 (scheduled and completed)
INSERT INTO assessment (id, class_id, subject_assessment_id, scheduled_date, actual_date, created_at, updated_at) VALUES
(1, 2, 1, '2025-11-21 08:00:00+07', '2025-11-21 08:00:00+07', '2025-11-14 08:00:00+07', '2025-11-21 08:00:00+07'), -- Listening Quiz 1 - completed
(2, 2, 2, '2025-11-26 08:00:00+07', '2025-11-26 08:00:00+07', '2025-11-14 08:00:00+07', '2025-11-26 08:00:00+07'), -- Speaking Quiz 1 - completed
(3, 2, 5, '2025-12-10 08:00:00+07', NULL, '2025-11-14 08:00:00+07', '2025-11-14 08:00:00+07'), -- Midterm - today
(4, 2, 6, '2026-01-07 08:00:00+07', NULL, '2025-11-14 08:00:00+07', '2025-11-14 08:00:00+07'); -- Final - scheduled

-- Assessments for Class 16 (HN-INT-O1) - Ongoing class for teacher grading demo
-- Uses course_assessment ids 7 and 8 (Intermediate course)
INSERT INTO assessment (id, class_id, subject_assessment_id, scheduled_date, actual_date, created_at, updated_at) VALUES
(20, 16, 7, '2025-11-30 08:00:00+07', '2025-11-30 08:00:00+07', '2025-11-25 08:00:00+07', '2025-11-30 08:00:00+07'), -- Writing Task 2 Assignment - completed
(21, 16, 8, '2025-12-15 08:00:00+07', NULL, '2025-11-25 08:00:00+07', '2025-11-25 08:00:00+07'); -- Full Mock Test - upcoming

-- =========================================
-- ADDITIONAL ASSESSMENTS FOR COMPLETED CLASSES
-- =========================================

-- Class 4 (IELTS Intermediate - Completed)
INSERT INTO assessment (id, class_id, subject_assessment_id, scheduled_date, actual_date, created_at, updated_at) VALUES
(30, 4, 7, '2025-11-24 08:00:00+07', '2025-11-24 08:00:00+07', '2025-10-27 08:00:00+07', '2025-11-24 08:00:00+07'),
(31, 4, 8, '2025-12-19 08:00:00+07', NULL, '2025-10-27 08:00:00+07', '2025-10-27 08:00:00+07');

-- Class 7 (IELTS Advanced - Completed)
INSERT INTO assessment (id, class_id, subject_assessment_id, scheduled_date, actual_date, created_at, updated_at) VALUES
(32, 7, 9, '2025-10-20 08:00:00+07', '2025-10-20 08:00:00+07', '2025-10-01 08:00:00+07', '2025-10-20 08:00:00+07'),
(33, 7, 10, '2025-11-05 08:00:00+07', '2025-11-05 08:00:00+07', '2025-10-01 08:00:00+07', '2025-11-05 08:00:00+07'),
(34, 7, 11, '2025-11-25 08:00:00+07', '2025-11-25 08:00:00+07', '2025-10-01 08:00:00+07', '2025-11-25 08:00:00+07');

-- Class 10 (TOEIC Foundation - Completed)
INSERT INTO assessment (id, class_id, subject_assessment_id, scheduled_date, actual_date, created_at, updated_at) VALUES
(35, 10, 12, '2025-10-10 08:00:00+07', '2025-10-10 08:00:00+07', '2025-09-20 08:00:00+07', '2025-10-10 08:00:00+07'),
(36, 10, 13, '2025-10-10 09:00:00+07', '2025-10-10 09:00:00+07', '2025-09-20 08:00:00+07', '2025-10-10 09:00:00+07'),
(37, 10, 14, '2025-11-15 08:00:00+07', '2025-11-15 08:00:00+07', '2025-09-20 08:00:00+07', '2025-11-15 08:00:00+07');

-- Class 13 (TOEIC Intermediate - Completed)
INSERT INTO assessment (id, class_id, subject_assessment_id, scheduled_date, actual_date, created_at, updated_at) VALUES
(38, 13, 15, '2025-11-01 08:00:00+07', '2025-11-01 08:00:00+07', '2025-10-01 08:00:00+07', '2025-11-01 08:00:00+07'),
(39, 13, 16, '2025-12-01 08:00:00+07', '2025-12-01 08:00:00+07', '2025-10-01 08:00:00+07', '2025-12-01 08:00:00+07');

-- Class 16 (TOEIC Advanced - Completed)
-- Note: Class 16 already has assessments 20, 21 defined above.
-- But 21 is 'upcoming'. We need to mark it as completed if the class is completed.
-- Updating Assessment 21 to be completed
UPDATE assessment SET actual_date = '2025-12-01 08:00:00+07', updated_at = NOW() WHERE id = 21;
-- Adding Assessment 17 (Case Study)
INSERT INTO assessment (id, class_id, subject_assessment_id, scheduled_date, actual_date, created_at, updated_at) VALUES
(40, 16, 17, '2025-11-10 08:00:00+07', '2025-11-10 08:00:00+07', '2025-10-01 08:00:00+07', '2025-11-10 08:00:00+07');

-- Scores for completed assessments (Listening Quiz 1)
INSERT INTO score (assessment_id, student_id, score, feedback, graded_by, graded_at, created_at, updated_at) VALUES
(1, 1, 18.0, 'Good listening skills', 1, '2025-11-22 10:00:00+07', '2025-11-22 10:00:00+07', '2025-11-22 10:00:00+07'),
(1, 2, 16.5, 'Need more practice on numbers', 1, '2025-11-22 10:00:00+07', '2025-11-22 10:00:00+07', '2025-11-22 10:00:00+07'),
(1, 3, 19.0, 'Excellent performance', 1, '2025-11-22 10:00:00+07', '2025-11-22 10:00:00+07', '2025-11-22 10:00:00+07'),
(1, 4, 15.0, 'Satisfactory', 1, '2025-11-22 10:00:00+07', '2025-11-22 10:00:00+07', '2025-11-22 10:00:00+07'),
(1, 5, 17.5, 'Good work', 1, '2025-11-22 10:00:00+07', '2025-11-22 10:00:00+07', '2025-11-22 10:00:00+07'),
(1, 6, 14.0, 'Need improvement', 1, '2025-11-22 10:00:00+07', '2025-11-22 10:00:00+07', '2025-11-22 10:00:00+07'),
(1, 7, 18.5, 'Very good', 1, '2025-11-22 10:00:00+07', '2025-11-22 10:00:00+07', '2025-11-22 10:00:00+07'),
(1, 8, 16.0, 'Good progress', 1, '2025-11-22 10:00:00+07', '2025-11-22 10:00:00+07', '2025-11-22 10:00:00+07'),
(1, 9, 17.0, 'Well done', 1, '2025-11-22 10:00:00+07', '2025-11-22 10:00:00+07', '2025-11-22 10:00:00+07'),
(1, 10, 15.5, 'Fair performance', 1, '2025-11-22 10:00:00+07', '2025-11-22 10:00:00+07', '2025-11-22 10:00:00+07');

-- Scores for Class 16, Writing Task 2 Assignment (assessment_id = 20)
-- Students 1-10 are enrolled in Class 16
INSERT INTO score (assessment_id, student_id, score, feedback, graded_by, graded_at, created_at, updated_at)
SELECT
    20,
    s.id,
    60 + floor(random() * 25)::int, -- Score between 60 and 84
    'Initial writing assignment score',
    5, -- Graded by Teacher 5 (Michael Brown)
    '2025-12-01 10:00:00+07',
    '2025-12-01 10:00:00+07',
    '2025-12-01 10:00:00+07'
FROM generate_series(1, 10) AS s(id);

-- Scores for Speaking Quiz 1
INSERT INTO score (assessment_id, student_id, score, feedback, graded_by, graded_at, created_at, updated_at) VALUES
(2, 1, 17.0, 'Good fluency, work on pronunciation', 1, '2025-10-22 10:00:00+07', '2025-10-22 10:00:00+07', '2025-10-22 10:00:00+07'),
(2, 2, 18.0, 'Confident speaker', 1, '2025-10-22 10:00:00+07', '2025-10-22 10:00:00+07', '2025-10-22 10:00:00+07'),
(2, 3, 16.5, 'Good effort', 1, '2025-10-22 10:00:00+07', '2025-10-22 10:00:00+07', '2025-10-22 10:00:00+07'),
(2, 4, 15.0, 'Need more practice', 1, '2025-10-22 10:00:00+07', '2025-10-22 10:00:00+07', '2025-10-22 10:00:00+07'),
(2, 5, 19.0, 'Excellent speaking skills', 1, '2025-10-22 10:00:00+07', '2025-10-22 10:00:00+07', '2025-10-22 10:00:00+07');

-- Assessments for Class 1 (COMPLETED)
INSERT INTO assessment (id, class_id, subject_assessment_id, scheduled_date, actual_date, created_at, updated_at) VALUES
(10, 1, 1, '2025-11-03 08:00:00+07', '2025-11-03 08:00:00+07', '2025-10-27 08:00:00+07', '2025-11-03 08:00:00+07'), -- Listening Quiz 1
(11, 1, 2, '2025-11-07 08:00:00+07', '2025-11-07 08:00:00+07', '2025-10-27 08:00:00+07', '2025-11-07 08:00:00+07'), -- Speaking Quiz 1
(12, 1, 3, '2025-11-17 08:00:00+07', '2025-11-17 08:00:00+07', '2025-10-27 08:00:00+07', '2025-11-17 08:00:00+07'), -- Reading Quiz 1
(13, 1, 4, '2025-11-21 08:00:00+07', '2025-11-21 08:00:00+07', '2025-10-27 08:00:00+07', '2025-11-21 08:00:00+07'), -- Writing Assignment 1
(14, 1, 5, '2025-11-26 08:00:00+07', '2025-11-26 08:00:00+07', '2025-10-27 08:00:00+07', '2025-11-26 08:00:00+07'), -- Midterm Exam
(15, 1, 6, '2025-12-19 08:00:00+07', NULL, '2025-10-27 08:00:00+07', '2025-10-27 08:00:00+07'); -- Final Exam

-- Scores for Class 1, Midterm Exam (assessment_id = 14)
INSERT INTO score (assessment_id, student_id, score, feedback, graded_by, graded_at)
SELECT
    14,
    s.id,
    60 + floor(random() * 35)::int, -- Score between 60 and 94
    'Good overall performance on the midterm.',
    3, -- Graded by Teacher 3
    '2025-11-28 10:00:00+07'
FROM generate_series(1, 15) AS s(id);



-- =========================================
-- ADDITIONAL SCORES FOR COMPLETED CLASSES
-- =========================================

-- Scores for Class 4 (IELTS Intermediate)
-- Assessment 30 (Writing)
INSERT INTO score (assessment_id, student_id, score, feedback, graded_by, graded_at, created_at, updated_at)
SELECT 30, e.student_id, 60 + floor(random() * 30)::int, 'Good effort on writing task.', 6, NOW(), NOW(), NOW()
FROM enrollment e WHERE e.class_id = 4;


-- Scores for Class 7 (IELTS Advanced)
-- Assessment 32 (Writing Portfolio)
INSERT INTO score (assessment_id, student_id, score, feedback, graded_by, graded_at, created_at, updated_at)
SELECT 32, e.student_id, 70 + floor(random() * 25)::int, 'Strong portfolio.', 6, NOW(), NOW(), NOW()
FROM enrollment e WHERE e.class_id = 7;
-- Assessment 33 (Speaking)
INSERT INTO score (assessment_id, student_id, score, feedback, graded_by, graded_at, created_at, updated_at)
SELECT 33, e.student_id, 75 + floor(random() * 20)::int, 'Fluent speaking.', 6, NOW(), NOW(), NOW()
FROM enrollment e WHERE e.class_id = 7;
-- Assessment 34 (Final)
INSERT INTO score (assessment_id, student_id, score, feedback, graded_by, graded_at, created_at, updated_at)
SELECT 34, e.student_id, 70 + floor(random() * 25)::int, 'Advanced level achieved.', 6, NOW(), NOW(), NOW()
FROM enrollment e WHERE e.class_id = 7;

-- Scores for Class 10 (TOEIC Foundation)
-- Assessment 35 (Listening)
INSERT INTO score (assessment_id, student_id, score, feedback, graded_by, graded_at, created_at, updated_at)
SELECT 35, e.student_id, 60 + floor(random() * 35)::int, 'Listening skills improving.', 6, NOW(), NOW(), NOW()
FROM enrollment e WHERE e.class_id = 10;
-- Assessment 36 (Reading)
INSERT INTO score (assessment_id, student_id, score, feedback, graded_by, graded_at, created_at, updated_at)
SELECT 36, e.student_id, 55 + floor(random() * 40)::int, 'Keep practicing reading.', 6, NOW(), NOW(), NOW()
FROM enrollment e WHERE e.class_id = 10;
-- Assessment 37 (Final)
INSERT INTO score (assessment_id, student_id, score, feedback, graded_by, graded_at, created_at, updated_at)
SELECT 37, e.student_id, 500 + floor(random() * 300)::int, 'TOEIC Foundation completed.', 6, NOW(), NOW(), NOW()
FROM enrollment e WHERE e.class_id = 10;

-- Scores for Class 13 (TOEIC Intermediate)
-- Assessment 38 (Midterm)
INSERT INTO score (assessment_id, student_id, score, feedback, graded_by, graded_at, created_at, updated_at)
SELECT 38, e.student_id, 600 + floor(random() * 200)::int, 'Midterm progress good.', 6, NOW(), NOW(), NOW()
FROM enrollment e WHERE e.class_id = 13;
-- Assessment 39 (Final)
INSERT INTO score (assessment_id, student_id, score, feedback, graded_by, graded_at, created_at, updated_at)
SELECT 39, e.student_id, 650 + floor(random() * 200)::int, 'Ready for Advanced level.', 6, NOW(), NOW(), NOW()
FROM enrollment e WHERE e.class_id = 13;

-- Scores for Class 16 (TOEIC Advanced)
-- Assessment 40 (Case Study)
INSERT INTO score (assessment_id, student_id, score, feedback, graded_by, graded_at, created_at, updated_at)
SELECT 40, e.student_id, 75 + floor(random() * 20)::int, 'Good business analysis.', 6, NOW(), NOW(), NOW()
FROM enrollment e WHERE e.class_id = 16;
-- Assessment 21 (Final - Updated to completed)
INSERT INTO score (assessment_id, student_id, score, feedback, graded_by, graded_at, created_at, updated_at)
SELECT 21, e.student_id, 800 + floor(random() * 150)::int, 'Excellent TOEIC score.', 6, NOW(), NOW(), NOW()
FROM enrollment e WHERE e.class_id = 16;

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

-- 1. Classroom Observation (Dự giờ) - Class 2 (Good)
INSERT INTO qa_report (id, class_id, session_id, reported_by, report_type, status, content, created_at, updated_at) VALUES
(1, 2, 105, 10, 'CLASSROOM_OBSERVATION', 'SUBMITTED', 'Giáo viên chuẩn bị bài kỹ lưỡng. Tương tác với học viên tốt. Không khí lớp học sôi nổi. Đề xuất giáo viên chia sẻ kinh nghiệm giảng dạy cho các giáo viên mới.', '2025-10-15 10:00:00+07', '2025-10-15 10:00:00+07'),
(2, 2, 110, 10, 'CLASSROOM_OBSERVATION', 'SUBMITTED', 'Học viên tham gia đầy đủ. Bài giảng đi đúng trọng tâm. Tiếp tục phát huy.', '2025-10-25 10:00:00+07', '2025-10-25 10:00:00+07');

-- 2. Classroom Observation - Class 15 (Issues)
INSERT INTO qa_report (id, class_id, session_id, reported_by, report_type, status, content, created_at, updated_at) VALUES
(3, 15, 1005, 11, 'CLASSROOM_OBSERVATION', 'SUBMITTED', 'Giáo viên vào lớp muộn 10 phút. Lớp học ồn ào, thiếu kiểm soát. Nhắc nhở giáo viên về quy định giờ giấc. Cần có biện pháp quản lý lớp học tốt hơn.', '2025-07-25 10:00:00+07', '2025-07-25 10:00:00+07'),
(4, 15, 1010, 11, 'CLASSROOM_OBSERVATION', 'DRAFT', 'Học viên ít tương tác. Giáo viên chỉ giảng bài một chiều. Cần tổ chức training về phương pháp giảng dạy tương tác.', '2025-08-05 10:00:00+07', '2025-08-05 10:00:00+07');

-- 3. Student Feedback Analysis (Phân tích phản hồi)
INSERT INTO qa_report (id, class_id, reported_by, report_type, status, content, created_at, updated_at) VALUES
(5, 13, 10, 'STUDENT_FEEDBACK_ANALYSIS', 'SUBMITTED', '100% học viên hài lòng với khóa học. Điểm đánh giá trung bình 4.8/5. Khen thưởng giáo viên.', '2025-08-25 10:00:00+07', '2025-08-25 10:00:00+07'),
(6, 14, 11, 'STUDENT_FEEDBACK_ANALYSIS', 'SUBMITTED', 'Phản hồi trái chiều. Một số học viên phàn nàn về tốc độ giảng dạy. Trao đổi với giáo viên để điều chỉnh tốc độ phù hợp với trình độ học viên.', '2025-08-30 10:00:00+07', '2025-08-30 10:00:00+07'),
(7, 15, 11, 'STUDENT_FEEDBACK_ANALYSIS', 'SUBMITTED', 'Nhiều phản hồi tiêu cực về thái độ giáo viên và chất lượng bài giảng. Cần họp khẩn với giáo viên và Academic Manager để xem xét vấn đề.', '2025-08-05 10:00:00+07', '2025-08-05 10:00:00+07');

-- 4. CLO Achievement Analysis (Đánh giá CLO)
INSERT INTO qa_report (id, class_id, phase_id, reported_by, report_type, status, content, created_at, updated_at) VALUES
(8, 13, 1, 10, 'CLO_ACHIEVEMENT_ANALYSIS', 'SUBMITTED', 'Học viên đạt 90% chuẩn đầu ra Phase 1. Cho phép chuyển sang Phase 2.', '2025-08-15 10:00:00+07', '2025-08-15 10:00:00+07'),
(9, 15, 1, 11, 'CLO_ACHIEVEMENT_ANALYSIS', 'SUBMITTED', 'Chỉ 60% học viên đạt chuẩn đầu ra. Kỹ năng Viết còn yếu. Tổ chức các buổi phụ đạo thêm về kỹ năng Viết.', '2025-08-01 10:00:00+07', '2025-08-01 10:00:00+07');

-- 5. Attendance & Engagement Review (Đánh giá chuyên cần)
INSERT INTO qa_report (id, class_id, reported_by, report_type, status, content, created_at, updated_at) VALUES
(10, 7, 11, 'ATTENDANCE_ENGAGEMENT_REVIEW', 'SUBMITTED', 'Tỷ lệ chuyên cần thấp (dưới 80%). Nhiều học viên nghỉ không phép. Liên hệ phụ huynh để thông báo tình hình. Cảnh báo học viên về nguy cơ cấm thi.', '2025-10-25 10:00:00+07', '2025-10-25 10:00:00+07'),
(11, 2, 10, 'ATTENDANCE_ENGAGEMENT_REVIEW', 'SUBMITTED', 'Tỷ lệ chuyên cần cao (>95%). Tiếp tục duy trì.', '2025-10-20 10:00:00+07', '2025-10-20 10:00:00+07');

-- 6. Teaching Quality Assessment (Đánh giá chất lượng giảng dạy)
INSERT INTO qa_report (id, class_id, reported_by, report_type, status, content, created_at, updated_at) VALUES
(12, 1, 10, 'TEACHING_QUALITY_ASSESSMENT', 'SUBMITTED', 'Giáo viên có chuyên môn vững, phương pháp sư phạm tốt. Đề xuất tăng lương hoặc thăng cấp bậc.', '2025-09-05 10:00:00+07', '2025-09-05 10:00:00+07'),
(13, 16, 10, 'TEACHING_QUALITY_ASSESSMENT', 'DRAFT', 'Giáo viên còn lúng túng khi xử lý tình huống sư phạm. Cần tham gia khóa đào tạo kỹ năng quản lý lớp học.', '2025-10-30 10:00:00+07', '2025-10-30 10:00:00+07');

-- 7. Phase Review (Đánh giá giai đoạn)
INSERT INTO qa_report (id, class_id, phase_id, reported_by, report_type, status, content, created_at, updated_at) VALUES
(14, 2, 1, 10, 'PHASE_REVIEW', 'SUBMITTED', 'Hoàn thành Phase 1 đúng tiến độ. Kết quả kiểm tra giữa kỳ khả quan. Chuẩn bị tài liệu cho Phase 2.', '2025-10-28 10:00:00+07', '2025-10-28 10:00:00+07'),
(15, 16, 1, 10, 'PHASE_REVIEW', 'DRAFT', 'Tiến độ chậm hơn dự kiến 2 buổi. Cần bố trí lịch học bù để đuổi kịp chương trình.', '2025-10-30 10:00:00+07', '2025-10-30 10:00:00+07');

-- 8. General Reports
INSERT INTO qa_report (id, class_id, reported_by, report_type, status, content, created_at, updated_at) VALUES
(16, 5, 11, 'CLASSROOM_OBSERVATION', 'SUBMITTED', 'Lớp học trực tuyến (Online) diễn ra suôn sẻ, đường truyền ổn định. Đảm bảo duy trì chất lượng kỹ thuật.', '2025-10-15 10:00:00+07', '2025-10-15 10:00:00+07'),
(17, 18, 11, 'STUDENT_FEEDBACK_ANALYSIS', 'SUBMITTED', 'Học viên đánh giá cao sự nhiệt tình của trợ giảng (TA). Khen thưởng đội ngũ TA.', '2025-10-28 10:00:00+07', '2025-10-28 10:00:00+07'),
(18, 3, 10, 'CLO_ACHIEVEMENT_ANALYSIS', 'SUBMITTED', 'Kỹ năng Nói của học viên lớp Online thấp hơn so với lớp Offline. Tăng cường các hoạt động Speaking trong giờ học Online.', '2025-10-20 10:00:00+07', '2025-10-20 10:00:00+07'),
(19, 6, 11, 'ATTENDANCE_ENGAGEMENT_REVIEW', 'SUBMITTED', 'Lớp buổi tối thường xuyên có học viên đến muộn do tắc đường. Xem xét lùi giờ học xuống 15 phút nếu khả thi.', '2025-10-22 10:00:00+07', '2025-10-22 10:00:00+07'),
(20, 4, 10, 'TEACHING_QUALITY_ASSESSMENT', 'DRAFT', 'Chưa có dữ liệu đánh giá (Lớp chưa bắt đầu). Lên kế hoạch dự giờ ngay tuần đầu tiên.', '2025-11-01 10:00:00+07', '2025-11-01 10:00:00+07');

-- ================================================================================================
-- TEST DATA FOR TRANSFER REQUEST (SMART FILTERING)
-- Context Date: 2025-12-10
-- Current Class: HN-IELTS-F2 (ID 2) - Progress: Session 11 Completed
-- ================================================================================================

-- 1. Create Target Classes (B, C, D, E, F)
INSERT INTO "class" (id, branch_id, subject_id, code, name, modality, start_date, planned_end_date, schedule_days, max_capacity, status, approval_status, created_by, decided_by, submitted_at, decided_at, created_at, updated_at) VALUES
-- Class B: Gap 0 (Same progress), 5 slots left (15 enrolled)
(101, 1, 1, 'HN-IELTS-TEST-B', 'IELTS Test Class B (Gap 0)', 'OFFLINE', '2025-11-14', '2026-01-08', ARRAY[2,4,6]::smallint[], 20, 'ONGOING', 'APPROVED', 6, 3, NOW(), NOW(), NOW(), NOW()),
-- Class C: Gap 0 (Same progress), 3 slots left (17 enrolled)
(102, 1, 1, 'HN-IELTS-TEST-C', 'IELTS Test Class C (Gap 0)', 'OFFLINE', '2025-11-14', '2026-01-08', ARRAY[1,3,5]::smallint[], 20, 'ONGOING', 'APPROVED', 6, 3, NOW(), NOW(), NOW(), NOW()),
-- Class D: Gap 1 (Faster by 1 session), 8 slots left (12 enrolled)
(103, 1, 1, 'HN-IELTS-TEST-D', 'IELTS Test Class D (Gap 1)', 'OFFLINE', '2025-11-12', '2026-01-06', ARRAY[1,3,5]::smallint[], 20, 'ONGOING', 'APPROVED', 6, 3, NOW(), NOW(), NOW(), NOW()),
-- Class E: Gap 2 (Slower by 2 sessions), 10 slots left (10 enrolled)
(104, 1, 1, 'HN-IELTS-TEST-E', 'IELTS Test Class E (Gap 2)', 'OFFLINE', '2025-11-19', '2026-01-13', ARRAY[2,4,6]::smallint[], 20, 'ONGOING', 'APPROVED', 6, 3, NOW(), NOW(), NOW(), NOW()),
-- Class F: Gap 5 (Faster by 5 sessions), 20 slots left (0 enrolled) - SHOULD BE FILTERED
(105, 1, 1, 'HN-IELTS-TEST-F', 'IELTS Test Class F (Gap 5)', 'OFFLINE', '2025-11-03', '2025-12-28', ARRAY[1,3,5]::smallint[], 20, 'ONGOING', 'APPROVED', 6, 3, NOW(), NOW(), NOW(), NOW());

-- 3. Create Enrollments to simulate capacity
-- We use a helper to insert bulk enrollments. 
-- Note: We reuse student IDs 10-50 for these dummy enrollments.
-- Class B: 15 students
INSERT INTO enrollment (student_id, class_id, status, enrolled_at, created_at, updated_at)
SELECT id, 101, 'ENROLLED', NOW(), NOW(), NOW() FROM student WHERE id BETWEEN 10 AND 24;

-- Class C: 17 students
INSERT INTO enrollment (student_id, class_id, status, enrolled_at, created_at, updated_at)
SELECT id, 102, 'ENROLLED', NOW(), NOW(), NOW() FROM student WHERE id BETWEEN 25 AND 41;

-- Class D: 12 students
INSERT INTO enrollment (student_id, class_id, status, enrolled_at, created_at, updated_at)
SELECT id, 103, 'ENROLLED', NOW(), NOW(), NOW() FROM student WHERE id BETWEEN 42 AND 53;

-- Class E: 10 students
INSERT INTO enrollment (student_id, class_id, status, enrolled_at, created_at, updated_at)
SELECT id, 104, 'ENROLLED', NOW(), NOW(), NOW() FROM student WHERE id BETWEEN 54 AND 63;

-- Class F: 0 students (No insert needed)

-- 4. Create Sessions & Progress (Crucial for Content Gap)
-- We generate sessions with specific dates relative to '2025-12-10' to establish progress.
-- We use EXPLICIT IDs (starting from 1800) to avoid sequence conflicts with previous data.

-- Class B (Gap 0): Sessions 1-11 DONE, 12-16 PLANNED
WITH ordered_sessions AS (
    SELECT id, ROW_NUMBER() OVER (ORDER BY id) as rn 
    FROM subject_session WHERE phase_id IN (1,2)
)
INSERT INTO session (id, class_id, subject_session_id, time_slot_template_id, date, status, created_at, updated_at)
SELECT 
    1800 + os.rn,
    101,
    os.id,
    2,
    '2025-11-14'::date + ((os.rn - 1) * 2 || ' days')::interval,
    CASE WHEN os.rn <= 11 THEN 'DONE' ELSE 'PLANNED' END,
    NOW(),
    NOW()
FROM ordered_sessions os WHERE os.rn <= 16;

-- Class C (Gap 0): Sessions 1-11 DONE, 12-16 PLANNED
WITH ordered_sessions AS (
    SELECT id, ROW_NUMBER() OVER (ORDER BY id) as rn 
    FROM subject_session WHERE phase_id IN (1,2)
)
INSERT INTO session (id, class_id, subject_session_id, time_slot_template_id, date, status, created_at, updated_at)
SELECT 
    1850 + os.rn,
    102,
    os.id,
    3,
    '2025-11-14'::date + ((os.rn - 1) * 2 || ' days')::interval,
    CASE WHEN os.rn <= 11 THEN 'DONE' ELSE 'PLANNED' END,
    NOW(),
    NOW()
FROM ordered_sessions os WHERE os.rn <= 16;

-- Class D (Gap 1): Sessions 1-12 DONE, 13-17 PLANNED (Faster by 1)
WITH ordered_sessions AS (
    SELECT id, ROW_NUMBER() OVER (ORDER BY id) as rn 
    FROM subject_session WHERE phase_id IN (1,2)
)
INSERT INTO session (id, class_id, subject_session_id, time_slot_template_id, date, status, created_at, updated_at)
SELECT 
    1900 + os.rn,
    103,
    os.id,
    4,
    '2025-11-12'::date + ((os.rn - 1) * 2 || ' days')::interval,
    CASE WHEN os.rn <= 12 THEN 'DONE' ELSE 'PLANNED' END,
    NOW(),
    NOW()
FROM ordered_sessions os WHERE os.rn <= 17;

-- Class E (Gap 2): Sessions 1-9 DONE, 10-14 PLANNED (Slower by 2)
WITH ordered_sessions AS (
    SELECT id, ROW_NUMBER() OVER (ORDER BY id) as rn 
    FROM subject_session WHERE phase_id IN (1,2)
)
INSERT INTO session (id, class_id, subject_session_id, time_slot_template_id, date, status, created_at, updated_at)
SELECT 
    1950 + os.rn,
    104,
    os.id,
    5,
    '2025-11-19'::date + ((os.rn - 1) * 2 || ' days')::interval,
    CASE WHEN os.rn <= 9 THEN 'DONE' ELSE 'PLANNED' END,
    NOW(),
    NOW()
FROM ordered_sessions os WHERE os.rn <= 14;

-- Class F (Gap 5): Sessions 1-16 DONE, 17-21 PLANNED (Much Faster - should be FILTERED)
WITH ordered_sessions AS (
    SELECT id, ROW_NUMBER() OVER (ORDER BY id) as rn 
    FROM subject_session WHERE phase_id IN (1,2)
)
INSERT INTO session (id, class_id, subject_session_id, time_slot_template_id, date, status, created_at, updated_at)
SELECT 
    2000 + os.rn,
    105,
    os.id,
    6,
    '2025-11-03'::date + ((os.rn - 1) * 2 || ' days')::interval,
    CASE WHEN os.rn <= 16 THEN 'DONE' ELSE 'PLANNED' END,
    NOW(),
    NOW()
FROM ordered_sessions os WHERE os.rn <= 21;

-- Assign Teachers to Sessions (Teaching Slots)
INSERT INTO teaching_slot (session_id, teacher_id, status)
SELECT id, 20, 'SCHEDULED' FROM session WHERE class_id = 101;

INSERT INTO teaching_slot (session_id, teacher_id, status)
SELECT id, 21, 'SCHEDULED' FROM session WHERE class_id = 102;

INSERT INTO teaching_slot (session_id, teacher_id, status)
SELECT id, 22, 'SCHEDULED' FROM session WHERE class_id = 103;

INSERT INTO teaching_slot (session_id, teacher_id, status)
SELECT id, 23, 'SCHEDULED' FROM session WHERE class_id = 104;

INSERT INTO teaching_slot (session_id, teacher_id, status)
SELECT id, 24, 'SCHEDULED' FROM session WHERE class_id = 105;

-- ========== FINAL SEQUENCE UPDATES ==========
SELECT setval('center_id_seq', (SELECT MAX(id) FROM center), true);
SELECT setval('branch_id_seq', (SELECT MAX(id) FROM branch), true);
SELECT setval('role_id_seq', (SELECT MAX(id) FROM role), true);
SELECT setval('user_account_id_seq', (SELECT MAX(id) FROM user_account), true);
SELECT setval('teacher_id_seq', (SELECT MAX(id) FROM teacher), true);
SELECT setval('student_id_seq', (SELECT MAX(id) FROM student), true);
SELECT setval('curriculum_id_seq', (SELECT MAX(id) FROM subject), true);
SELECT setval('level_id_seq', (SELECT MAX(id) FROM level), true);
SELECT setval('plo_id_seq', (SELECT MAX(id) FROM plo), true);
SELECT setval('subject_id_seq', (SELECT MAX(id) FROM subject), true);
SELECT setval('subject_phase_id_seq', (SELECT MAX(id) FROM subject_phase), true);
SELECT setval('clo_id_seq', (SELECT MAX(id) FROM clo), true);
SELECT setval('subject_session_id_seq', (SELECT MAX(id) FROM subject_session), true);
SELECT setval('subject_assessment_id_seq', (SELECT MAX(id) FROM subject_assessment), true);
SELECT setval('class_id_seq', (SELECT MAX(id) FROM "class"), true);
SELECT setval('session_id_seq', (SELECT MAX(id) FROM session), true); -- Should be at least 716 after adding new sessions
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
