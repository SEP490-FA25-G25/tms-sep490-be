TRUNCATE TABLE student_feedback_response CASCADE;
TRUNCATE TABLE student_feedback CASCADE;
TRUNCATE TABLE qa_report CASCADE;
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
-- Vietnamese real names for students
INSERT INTO user_account (id, email, phone, full_name, gender, dob, address, password_hash, status, created_at, updated_at) VALUES
-- Ha Noi Students (IDs 101-150)
(101, 'nguyenvanan01@gmail.com', '0900000001', 'Nguyen Van An', 'MALE', '2000-01-15', 'Ha Noi', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(102, 'tranthimai02@gmail.com', '0900000002', 'Tran Thi Mai', 'FEMALE', '2001-02-20', 'Ha Noi', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(103, 'levanbinh03@gmail.com', '0900000003', 'Le Van Binh', 'MALE', '2002-03-10', 'Ha Noi', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(104, 'phamthihoa04@gmail.com', '0900000004', 'Pham Thi Hoa', 'FEMALE', '2000-04-25', 'Ha Noi', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(105, 'hoangvancuong05@gmail.com', '0900000005', 'Hoang Van Cuong', 'MALE', '2001-05-18', 'Ha Noi', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(106, 'nguyenthilan06@gmail.com', '0900000006', 'Nguyen Thi Lan', 'FEMALE', '2002-06-08', 'Ha Noi', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(107, 'vuvanduc07@gmail.com', '0900000007', 'Vu Van Duc', 'MALE', '2000-07-22', 'Ha Noi', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(108, 'dothihuong08@gmail.com', '0900000008', 'Do Thi Huong', 'FEMALE', '2001-08-14', 'Ha Noi', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(109, 'buivannam09@gmail.com', '0900000009', 'Bui Van Nam', 'MALE', '2002-09-05', 'Ha Noi', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(110, 'dangthiphuong10@gmail.com', '0900000010', 'Dang Thi Phuong', 'FEMALE', '2000-10-30', 'Ha Noi', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(111, 'tranvanquang11@gmail.com', '0900000011', 'Tran Van Quang', 'MALE', '2001-11-12', 'Ha Noi', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(112, 'nguyenthithu12@gmail.com', '0900000012', 'Nguyen Thi Thu', 'FEMALE', '2002-12-03', 'Ha Noi', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(113, 'levanson13@gmail.com', '0900000013', 'Le Van Son', 'MALE', '2000-01-28', 'Ha Noi', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(114, 'phamthithanh14@gmail.com', '0900000014', 'Pham Thi Thanh', 'FEMALE', '2001-02-17', 'Ha Noi', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(115, 'hovanung15@gmail.com', '0900000015', 'Ho Van Hung', 'MALE', '2002-03-22', 'Ha Noi', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(116, 'vuongthiloan16@gmail.com', '0900000016', 'Vuong Thi Loan', 'FEMALE', '2000-04-11', 'Ha Noi', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(117, 'ngovantuong17@gmail.com', '0900000017', 'Ngo Van Truong', 'MALE', '2001-05-09', 'Ha Noi', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(118, 'lethiyen18@gmail.com', '0900000018', 'Le Thi Yen', 'FEMALE', '2002-06-28', 'Ha Noi', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(119, 'tranvanhieu19@gmail.com', '0900000019', 'Tran Van Hieu', 'MALE', '2000-07-15', 'Ha Noi', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(120, 'nguyenthilinh20@gmail.com', '0900000020', 'Nguyen Thi Linh', 'FEMALE', '2001-08-04', 'Ha Noi', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(121, 'phamvanlong21@gmail.com', '0900000021', 'Pham Van Long', 'MALE', '2002-09-19', 'Ha Noi', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(122, 'vuthimyhanh22@gmail.com', '0900000022', 'Vu Thi My Hanh', 'FEMALE', '2000-10-08', 'Ha Noi', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(123, 'doanhvan23@gmail.com', '0900000023', 'Doan Van Khanh', 'MALE', '2001-11-27', 'Ha Noi', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(124, 'tranthinga24@gmail.com', '0900000024', 'Tran Thi Nga', 'FEMALE', '2002-12-16', 'Ha Noi', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(125, 'nguyenvanphuc25@gmail.com', '0900000025', 'Nguyen Van Phuc', 'MALE', '2000-01-03', 'Ha Noi', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(126, 'lethidiem26@gmail.com', '0900000026', 'Le Thi Diem', 'FEMALE', '2001-02-22', 'Ha Noi', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(127, 'hoangquoctuan27@gmail.com', '0900000027', 'Hoang Quoc Tuan', 'MALE', '2002-03-14', 'Ha Noi', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(128, 'vuthihue28@gmail.com', '0900000028', 'Vu Thi Hue', 'FEMALE', '2000-04-08', 'Ha Noi', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(129, 'nguyenvanminh29@gmail.com', '0900000029', 'Nguyen Van Minh', 'MALE', '2001-05-27', 'Ha Noi', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(130, 'dinhthitrang30@gmail.com', '0900000030', 'Dinh Thi Trang', 'FEMALE', '2002-06-19', 'Ha Noi', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(131, 'buixuanhoang31@gmail.com', '0900000031', 'Bui Xuan Hoang', 'MALE', '2000-07-11', 'Ha Noi', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(132, 'ngothikieu32@gmail.com', '0900000032', 'Ngo Thi Kieu', 'FEMALE', '2001-08-30', 'Ha Noi', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(133, 'tranminhdat33@gmail.com', '0900000033', 'Tran Minh Dat', 'MALE', '2002-09-23', 'Ha Noi', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(134, 'phamthithao34@gmail.com', '0900000034', 'Pham Thi Thao', 'FEMALE', '2000-10-15', 'Ha Noi', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(135, 'leducthanh35@gmail.com', '0900000035', 'Le Duc Thanh', 'MALE', '2001-11-07', 'Ha Noi', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(136, 'vuthithanhthuy36@gmail.com', '0900000036', 'Vu Thi Thanh Thuy', 'FEMALE', '2002-12-29', 'Ha Noi', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(137, 'nguyenhoanghuy37@gmail.com', '0900000037', 'Nguyen Hoang Huy', 'MALE', '2000-01-21', 'Ha Noi', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(138, 'dothiquyen38@gmail.com', '0900000038', 'Do Thi Quyen', 'FEMALE', '2001-02-14', 'Ha Noi', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(139, 'hoangvanhai39@gmail.com', '0900000039', 'Hoang Van Hai', 'MALE', '2002-03-08', 'Ha Noi', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(140, 'tranthianh40@gmail.com', '0900000040', 'Tran Thi Anh', 'FEMALE', '2000-04-02', 'Ha Noi', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(141, 'nguyenvanson41@gmail.com', '0900000041', 'Nguyen Van Son', 'MALE', '2001-05-25', 'Ha Noi', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(142, 'phanthithom42@gmail.com', '0900000042', 'Phan Thi Thom', 'FEMALE', '2002-06-17', 'Ha Noi', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(143, 'levantri43@gmail.com', '0900000043', 'Le Van Tri', 'MALE', '2000-07-09', 'Ha Noi', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(144, 'vuthihong44@gmail.com', '0900000044', 'Vu Thi Hong', 'FEMALE', '2001-08-01', 'Ha Noi', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(145, 'dangvanquyet45@gmail.com', '0900000045', 'Dang Van Quyet', 'MALE', '2002-09-24', 'Ha Noi', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(146, 'nguyenthitam46@gmail.com', '0900000046', 'Nguyen Thi Tam', 'FEMALE', '2000-10-18', 'Ha Noi', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(147, 'trananhtuan47@gmail.com', '0900000047', 'Tran Anh Tuan', 'MALE', '2001-11-10', 'Ha Noi', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(148, 'lethithao48@gmail.com', '0900000048', 'Le Thi Thao', 'FEMALE', '2002-12-02', 'Ha Noi', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(149, 'phamvanthang49@gmail.com', '0900000049', 'Pham Van Thang', 'MALE', '2000-01-26', 'Ha Noi', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(150, 'vuthingoc50@gmail.com', '0900000050', 'Vu Thi Ngoc', 'FEMALE', '2001-02-18', 'Ha Noi', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
-- Ho Chi Minh Students (IDs 151-200)
(151, 'nguyenhoanganh51@gmail.com', '0900000051', 'Nguyen Hoang Anh', 'MALE', '2002-03-12', 'TP. HCM', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(152, 'tranthimylam52@gmail.com', '0900000052', 'Tran Thi My Lam', 'FEMALE', '2000-04-06', 'TP. HCM', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(153, 'levankien53@gmail.com', '0900000053', 'Le Van Kien', 'MALE', '2001-05-29', 'TP. HCM', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(154, 'phamthikim54@gmail.com', '0900000054', 'Pham Thi Kim', 'FEMALE', '2002-06-21', 'TP. HCM', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(155, 'hoangminhkhoi55@gmail.com', '0900000055', 'Hoang Minh Khoi', 'MALE', '2000-07-13', 'TP. HCM', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(156, 'nguyenthidao56@gmail.com', '0900000056', 'Nguyen Thi Dao', 'FEMALE', '2001-08-05', 'TP. HCM', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(157, 'vuthanhphong57@gmail.com', '0900000057', 'Vu Thanh Phong', 'MALE', '2002-09-28', 'TP. HCM', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(158, 'dothithuy58@gmail.com', '0900000058', 'Do Thi Thuy', 'FEMALE', '2000-10-20', 'TP. HCM', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(159, 'buiquocviet59@gmail.com', '0900000059', 'Bui Quoc Viet', 'MALE', '2001-11-12', 'TP. HCM', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(160, 'dangthiloan60@gmail.com', '0900000060', 'Dang Thi Loan', 'FEMALE', '2002-12-04', 'TP. HCM', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(161, 'tranvanphuoc61@gmail.com', '0900000061', 'Tran Van Phuoc', 'MALE', '2000-01-27', 'TP. HCM', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(162, 'nguyenthitu62@gmail.com', '0900000062', 'Nguyen Thi Tu', 'FEMALE', '2001-02-19', 'TP. HCM', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(163, 'letrungkien63@gmail.com', '0900000063', 'Le Trung Kien', 'MALE', '2002-03-13', 'TP. HCM', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(164, 'phamthithanh64@gmail.com', '0900000064', 'Pham Thi Hanh', 'FEMALE', '2000-04-07', 'TP. HCM', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(165, 'hoangduclong65@gmail.com', '0900000065', 'Hoang Duc Long', 'MALE', '2001-05-30', 'TP. HCM', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(166, 'vuthixuan66@gmail.com', '0900000066', 'Vu Thi Xuan', 'FEMALE', '2002-06-22', 'TP. HCM', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(167, 'ngovanphu67@gmail.com', '0900000067', 'Ngo Van Phu', 'MALE', '2000-07-14', 'TP. HCM', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(168, 'lethimua68@gmail.com', '0900000068', 'Le Thi Mua', 'FEMALE', '2001-08-06', 'TP. HCM', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(169, 'tranquangdung69@gmail.com', '0900000069', 'Tran Quang Dung', 'MALE', '2002-09-29', 'TP. HCM', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(170, 'nguyenthiquỳnh70@gmail.com', '0900000070', 'Nguyen Thi Quynh', 'FEMALE', '2000-10-21', 'TP. HCM', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(171, 'phamvantien71@gmail.com', '0900000071', 'Pham Van Tien', 'MALE', '2001-11-13', 'TP. HCM', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(172, 'vuthiloi72@gmail.com', '0900000072', 'Vu Thi Loi', 'FEMALE', '2002-12-05', 'TP. HCM', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(173, 'doanvanhai73@gmail.com', '0900000073', 'Doan Van Hai', 'MALE', '2000-01-28', 'TP. HCM', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(174, 'tranthinhung74@gmail.com', '0900000074', 'Tran Thi Nhung', 'FEMALE', '2001-02-20', 'TP. HCM', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(175, 'nguyenvanphong75@gmail.com', '0900000075', 'Nguyen Van Phong', 'MALE', '2002-03-14', 'TP. HCM', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(176, 'lethicuc76@gmail.com', '0900000076', 'Le Thi Cuc', 'FEMALE', '2000-04-08', 'TP. HCM', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(177, 'hoangvandai77@gmail.com', '0900000077', 'Hoang Van Dai', 'MALE', '2001-05-31', 'TP. HCM', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(178, 'vuthicham78@gmail.com', '0900000078', 'Vu Thi Cham', 'FEMALE', '2002-06-23', 'TP. HCM', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(179, 'nguyenquochung79@gmail.com', '0900000079', 'Nguyen Quoc Hung', 'MALE', '2000-07-15', 'TP. HCM', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(180, 'dinhthithao80@gmail.com', '0900000080', 'Dinh Thi Thao', 'FEMALE', '2001-08-07', 'TP. HCM', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(181, 'buiminhtrung81@gmail.com', '0900000081', 'Bui Minh Trung', 'MALE', '2002-09-30', 'TP. HCM', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(182, 'ngothidan82@gmail.com', '0900000082', 'Ngo Thi Dan', 'FEMALE', '2000-10-22', 'TP. HCM', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(183, 'tranvanbach83@gmail.com', '0900000083', 'Tran Van Bach', 'MALE', '2001-11-14', 'TP. HCM', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(184, 'phamthinuong84@gmail.com', '0900000084', 'Pham Thi Nuong', 'FEMALE', '2002-12-06', 'TP. HCM', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(185, 'leducnhan85@gmail.com', '0900000085', 'Le Duc Nhan', 'MALE', '2000-01-29', 'TP. HCM', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(186, 'vuthiyeu86@gmail.com', '0900000086', 'Vu Thi Yeu', 'FEMALE', '2001-02-21', 'TP. HCM', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(187, 'nguyenvankhoa87@gmail.com', '0900000087', 'Nguyen Van Khoa', 'MALE', '2002-03-15', 'TP. HCM', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(188, 'dothikim88@gmail.com', '0900000088', 'Do Thi Kim', 'FEMALE', '2000-04-09', 'TP. HCM', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(189, 'hoangvanthanh89@gmail.com', '0900000089', 'Hoang Van Thanh', 'MALE', '2001-06-01', 'TP. HCM', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(190, 'tranthiphuc90@gmail.com', '0900000090', 'Tran Thi Phuc', 'FEMALE', '2002-06-24', 'TP. HCM', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(191, 'nguyentrunghai91@gmail.com', '0900000091', 'Nguyen Trung Hai', 'MALE', '2000-07-16', 'TP. HCM', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(192, 'phanthidung92@gmail.com', '0900000092', 'Phan Thi Dung', 'FEMALE', '2001-08-08', 'TP. HCM', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(193, 'levancan93@gmail.com', '0900000093', 'Le Van Can', 'MALE', '2002-10-01', 'TP. HCM', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(194, 'vuthichinh94@gmail.com', '0900000094', 'Vu Thi Chinh', 'FEMALE', '2000-10-23', 'TP. HCM', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(195, 'dangvanthuan95@gmail.com', '0900000095', 'Dang Van Thuan', 'MALE', '2001-11-15', 'TP. HCM', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(196, 'nguyenthimao96@gmail.com', '0900000096', 'Nguyen Thi Mao', 'FEMALE', '2002-12-07', 'TP. HCM', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(197, 'tranducphi97@gmail.com', '0900000097', 'Tran Duc Phi', 'MALE', '2000-01-30', 'TP. HCM', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(198, 'lethitran98@gmail.com', '0900000098', 'Le Thi Tran', 'FEMALE', '2001-02-22', 'TP. HCM', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(199, 'phamvanhai99@gmail.com', '0900000099', 'Pham Van Hai', 'MALE', '2002-03-16', 'TP. HCM', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(200, 'vuthithu100@gmail.com', '0900000100', 'Vu Thi Thu', 'FEMALE', '2000-04-10', 'TP. HCM', '$2a$12$YNA7sOfjJNXLzHPzolLvkuhVj8EkY85r9OgPUBtb1wpk2gT5g1IV.', 'ACTIVE', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07');

-- Feedback Questions (for student feedback feature)
INSERT INTO feedback_question (id, question_text, display_order, status, created_at, updated_at) VALUES
(1, 'Giáo viên giảng dạy rõ ràng, dễ hiểu', 1, 'ACTIVE', '2024-01-01 00:00:00+07', '2024-01-01 00:00:00+07'),
(2, 'Tài liệu học tập và bài tập phù hợp với nội dung giảng dạy', 2, 'ACTIVE', '2024-01-01 00:00:00+07', '2024-01-01 00:00:00+07'),
(3, 'Giáo viên nhiệt tình, tương tác tốt với học viên', 3, 'ACTIVE', '2024-01-01 00:00:00+07', '2024-01-01 00:00:00+07'),
(4, 'Lớp học có đủ thiết bị và điều kiện học tập tốt', 4, 'ACTIVE', '2024-01-01 00:00:00+07', '2024-01-01 00:00:00+07'),
(5, 'Thời gian và tốc độ học phù hợp với khả năng tiếp thu', 5, 'ACTIVE', '2024-01-01 00:00:00+07', '2024-01-01 00:00:00+07');

-- Branches
INSERT INTO branch (id, center_id, code, name, address, phone, email, district, city, status, opening_date, created_at, updated_at) VALUES
(1, 1, 'HN01', 'TMS Ha Noi Branch', '456 Lang Ha, Dong Da, Ha Noi', '+84-24-3888-9999', 'hanoi01@tms-edu.vn', 'Dong Da', 'Ha Noi', 'ACTIVE', '2024-01-15', '2024-01-15 00:00:00+07', '2024-01-15 00:00:00+07'),
(2, 1, 'HCM01', 'TMS Ho Chi Minh Branch', '789 Le Loi, Quan 1, TP. HCM', '+84-28-3777-6666', 'hcm01@tms-edu.vn', 'Quan 1', 'TP. HCM', 'ACTIVE', '2024-03-01', '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07');

-- Curriculum (chỉ giữ IELTS)
INSERT INTO curriculum (id, code, name, description, language, status, created_by, created_at, updated_at) VALUES
(1, 'IELTS', 'International English Language Testing System', 'Comprehensive IELTS preparation courses', 'English', 'ACTIVE', 5, '2024-06-01 00:00:00+07', '2024-06-01 00:00:00+07');

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
INSERT INTO resource (id, branch_id, resource_type, code, name, capacity, capacity_override, meeting_url, created_at, updated_at) VALUES
-- Ha Noi Branch - Physical Rooms (Various capacities)
(1, 1, 'ROOM', 'HN01-R101', 'Ha Noi Room 101', 20, 25, NULL, '2024-01-15 00:00:00+07', '2024-01-15 00:00:00+07'),
(2, 1, 'ROOM', 'HN01-R102', 'Ha Noi Room 102', 15, NULL, NULL, '2024-01-15 00:00:00+07', '2024-01-15 00:00:00+07'),
(3, 1, 'ROOM', 'HN01-R201', 'Ha Noi Room 201', 25, NULL, NULL, '2024-01-15 00:00:00+07', '2024-01-15 00:00:00+07'),
-- Ha Noi Branch - Small Rooms (for VIP/1-1 classes)
(9, 1, 'ROOM', 'HN01-R301', 'Ha Noi VIP Room 301', 5, NULL, NULL, '2024-01-15 00:00:00+07', '2024-01-15 00:00:00+07'),
(10, 1, 'ROOM', 'HN01-R302', 'Ha Noi Study Room 302', 8, 10, NULL, '2024-01-15 00:00:00+07', '2024-01-15 00:00:00+07'),
-- Ha Noi Branch - Large Rooms (for workshops/seminars)
(11, 1, 'ROOM', 'HN01-R401', 'Ha Noi Conference Hall', 50, 60, NULL, '2024-01-15 00:00:00+07', '2024-01-15 00:00:00+07'),
(12, 1, 'ROOM', 'HN01-R402', 'Ha Noi Seminar Room', 35, NULL, NULL, '2024-01-15 00:00:00+07', '2024-01-15 00:00:00+07'),
-- Ha Noi Branch - Virtual (Multiple Zoom accounts)
(4, 1, 'VIRTUAL', 'HN01-Z01', 'Ha Noi Zoom 01', 100, NULL, NULL, '2024-01-15 00:00:00+07', '2024-01-15 00:00:00+07'),
(13, 1, 'VIRTUAL', 'HN01-GM02', 'Ha Noi Google Meet 02', 100, NULL, 'https://meet.google.com/ugb-mcbz-eki', '2024-01-15 00:00:00+07', '2024-01-15 00:00:00+07'),
(14, 1, 'VIRTUAL', 'HN01-GM03', 'Ha Noi Google Meet 03', 100, NULL, 'https://meet.google.com/vyj-ngvm-vpm', '2024-01-15 00:00:00+07', '2024-01-15 00:00:00+07'),
(15, 1, 'VIRTUAL', 'HN01-GM01', 'Ha Noi Google Meet 01', 100, NULL, 'https://meet.google.com/izb-xhvh-cpb', '2024-01-15 00:00:00+07', '2024-01-15 00:00:00+07'),
-- Ho Chi Minh Branch - Physical Rooms
(5, 2, 'ROOM', 'HCM01-R101', 'HCM Room 101', 20, NULL, NULL, '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(6, 2, 'ROOM', 'HCM01-R102', 'HCM Room 102', 20, NULL, NULL, '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(7, 2, 'ROOM', 'HCM01-R201', 'HCM Room 201', 25, NULL, NULL, '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
-- HCM Branch - Small Rooms
(16, 2, 'ROOM', 'HCM01-R301', 'HCM VIP Room 301', 6, NULL, NULL, '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(17, 2, 'ROOM', 'HCM01-R302', 'HCM Private Room 302', 4, NULL, NULL, '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
-- HCM Branch - Large Rooms
(18, 2, 'ROOM', 'HCM01-R401', 'HCM Training Center', 40, 50, NULL, '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
-- Ho Chi Minh Branch - Virtual
(8, 2, 'VIRTUAL', 'HCM01-Z01', 'HCM Zoom 01', 100, NULL, NULL, '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(19, 2, 'VIRTUAL', 'HCM01-Z02', 'HCM Zoom 02', 100, NULL, NULL, '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07'),
(20, 2, 'VIRTUAL', 'HCM01-MS01', 'HCM MS Teams 01', 250, NULL, NULL, '2024-03-01 00:00:00+07', '2024-03-01 00:00:00+07');

-- User Role & Branch Assignments
INSERT INTO user_role (user_id, role_id) VALUES
(1,1), (2,2), (3,3), (4,3), (5,4), (6,5), (7,5), (8,5), (9,5), (10,8), (11,8);
-- Teachers (original + new)
INSERT INTO user_role (user_id, role_id) SELECT id, 6 FROM user_account WHERE id >= 20 AND id <= 43;
-- Students
INSERT INTO user_role (user_id, role_id) SELECT id, 7 FROM user_account WHERE id BETWEEN 101 AND 200;

INSERT INTO user_branches (user_id, branch_id, assigned_by) VALUES
-- Staff assignments
(1,1,1), (1,2,1), (2,1,1), (2,2,1), (3,1,2), (3,2,2), (4,2,2), (5,1,2), (6,1,2), (6,2,2), (7,1,2), (8,2,4), (9,2,4), (10,1,2), (11,2,4);
-- Teachers - HN (original)
INSERT INTO user_branches (user_id, branch_id, assigned_by) SELECT id, 1, 6 FROM user_account WHERE id BETWEEN 20 AND 27;
-- Teachers - HCM (original)
INSERT INTO user_branches (user_id, branch_id, assigned_by) SELECT id, 2, 8 FROM user_account WHERE id BETWEEN 28 AND 35;
-- Teacher 20 (John Smith) dạy cả HN và HCM
INSERT INTO user_branches (user_id, branch_id, assigned_by) VALUES (20, 2, 6);
-- New Teachers - HN (TOEIC, Weekend, Evening)
INSERT INTO user_branches (user_id, branch_id, assigned_by) VALUES (36, 1, 6), (37, 1, 6), (38, 1, 6), (39, 1, 6), (40, 1, 6), (43, 1, 6);
-- New Teachers - HCM (TOEIC, Weekend)
INSERT INTO user_branches (user_id, branch_id, assigned_by) VALUES (41, 2, 8), (42, 2, 8);
-- Students - HN (user_id 101-150)
INSERT INTO user_branches (user_id, branch_id, assigned_by) SELECT id, 1, 6 FROM user_account WHERE id BETWEEN 101 AND 150;
-- Students - HCM (user_id 151-200)
INSERT INTO user_branches (user_id, branch_id, assigned_by) SELECT id, 2, 8 FROM user_account WHERE id BETWEEN 151 AND 200;

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
FROM user_account WHERE id BETWEEN 101 AND 200;

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


-- Levels (CEFR-based A1-C2 for IELTS only)
INSERT INTO level (id, curriculum_id, code, name, sort_order, created_at, updated_at) VALUES
-- IELTS Levels (A1 -> C2)
(1, 1, 'A1', 'IELTS A1 (Beginner)', 1, '2024-06-01 00:00:00+07', '2024-06-01 00:00:00+07'),
(2, 1, 'A2', 'IELTS A2 (Elementary)', 2, '2024-06-01 00:00:00+07', '2024-06-01 00:00:00+07'),
(3, 1, 'B1', 'IELTS B1 (Intermediate)', 3, '2024-06-01 00:00:00+07', '2024-06-01 00:00:00+07'),
(4, 1, 'B2', 'IELTS B2 (Upper-Intermediate)', 4, '2024-06-01 00:00:00+07', '2024-06-01 00:00:00+07'),
(5, 1, 'C1', 'IELTS C1 (Advanced)', 5, '2024-06-01 00:00:00+07', '2024-06-01 00:00:00+07'),
(6, 1, 'C2', 'IELTS C2 (Proficiency)', 6, '2024-06-01 00:00:00+07', '2024-06-01 00:00:00+07');

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

-- Student 2: IELTS Placement Test
INSERT INTO replacement_skill_assessment (student_id, skill, level_id, score, assessment_date, assessment_type, assessed_by, note, created_at, updated_at) VALUES
(2, 'LISTENING', 3, '28/40', '2025-06-16', 'ielts_placement', 6, 'IELTS Listening Placement Test', '2025-06-16 00:00:00+07', '2025-06-16 00:00:00+07'),
(2, 'READING', 3, '26/40', '2025-06-16', 'ielts_placement', 6, 'IELTS Reading Placement Test', '2025-06-16 00:00:00+07', '2025-06-16 00:00:00+07');

-- PLOs for IELTS Subject
INSERT INTO plo (id, curriculum_id, code, description, created_at, updated_at) VALUES
(1, 1, 'PLO1', 'Demonstrate basic English communication skills in everyday contexts', '2024-06-01 00:00:00+07', '2024-06-01 00:00:00+07'),
(2, 1, 'PLO2', 'Comprehend and produce simple English texts for common situations', '2024-06-01 00:00:00+07', '2024-06-01 00:00:00+07'),
(3, 1, 'PLO3', 'Apply intermediate English grammar and vocabulary in professional contexts', '2024-06-01 00:00:00+07', '2024-06-01 00:00:00+07'),
(4, 1, 'PLO4', 'Analyze and evaluate complex English texts across various topics', '2024-06-01 00:00:00+07', '2024-06-01 00:00:00+07'),
(5, 1, 'PLO5', 'Produce coherent, well-structured academic essays and reports', '2024-06-01 00:00:00+07', '2024-06-01 00:00:00+07');

-- Courses: 2 IELTS subjects only (Foundation + Intermediate)
INSERT INTO subject (id, curriculum_id, level_id, logical_subject_code, version, code, name, description, total_hours, number_of_sessions, hours_per_session, prerequisites, target_audience, teaching_methods, score_scale, status, approval_status, decided_by_manager, decided_at, rejection_reason, created_by, created_at, updated_at, effective_date) VALUES
-- IELTS Courses
(1, 1, 1, 'IELTS-FOUND-2025', 1, 'IELTS-FOUND-2025-V1', 'IELTS Foundation 2025', 'Môn học nền tảng cho người mới bắt đầu, mục tiêu band 3.0-4.0', 60, 24, 2.5, 'Không yêu cầu kiến thức nền tảng.', 'Học viên mất gốc hoặc mới bắt đầu học tiếng Anh.', 'Communicative Language Teaching (CLT) kết hợp bài tập thực hành.', '0-9', 'ACTIVE', 'APPROVED', 2, '2024-08-20 14:00:00+07', NULL, 5, '2024-08-15 00:00:00+07', '2024-08-20 14:00:00+07', '2024-09-01'),
(2, 1, 3, 'IELTS-INT-2025', 1, 'IELTS-INT-2025-V1', 'IELTS Intermediate 2025', 'Môn học trung cấp, mục tiêu band 5.0-5.5', 60, 24, 2.5, 'Hoàn thành khóa Foundation hoặc IELTS 4.0+', 'Học viên có nền tảng cơ bản, mục tiêu band 5.0-5.5.', 'Chiến thuật giải đề và nâng cao từ vựng học thuật.', '0-9', 'ACTIVE', 'APPROVED', 2, '2024-08-25 14:00:00+07', NULL, 5, '2024-08-15 00:00:00+07', '2024-08-25 14:00:00+07', '2024-09-01');
-- Course Phases for Foundation
INSERT INTO subject_phase (id, subject_id, phase_number, name, duration_weeks, created_at, updated_at) VALUES
(1, 1, 1, 'Foundation Basics', 4, '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(2, 1, 2, 'Foundation Practice', 4, '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07');

-- Course Phases for IELTS Intermediate (Course 2)
INSERT INTO subject_phase (id, subject_id, phase_number, name, duration_weeks, created_at, updated_at) VALUES
(3, 2, 1, 'Skill Building', 5, '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(4, 2, 2, 'Test Strategies', 5, '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07');

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

-- ========== Course 2: IELTS Intermediate (24 sessions) - Band 5.0-5.5 Target ==========
-- Phase 3: Skill Building (Sessions 1-12) - Focus on developing core IELTS skills
INSERT INTO subject_session (id, phase_id, sequence_no, topic, student_task, skill, created_at, updated_at) VALUES
-- Week 1-2: Listening Skills Development
(25, 3, 1, 'Advanced Listening: Section 1 - Conversations', 'Complete Section 1 practice tests and identify key information types', 'LISTENING', '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(26, 3, 2, 'Listening: Section 2 - Monologues & Maps', 'Practice map labeling and note completion in monologue contexts', 'LISTENING', '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(27, 3, 3, 'Listening: Section 3 - Academic Discussions', 'Analyze academic discussions and identify speaker opinions', 'LISTENING', '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),

-- Week 3-4: Reading Skills Development
(28, 3, 4, 'Reading: Skimming & Scanning Techniques', 'Apply skimming for main ideas and scanning for specific details', 'READING', '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(29, 3, 5, 'Reading: True/False/Not Given Questions', 'Master T/F/NG question types with academic passages', 'READING', '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(30, 3, 6, 'Reading: Matching Headings & Features', 'Practice heading matching and feature matching questions', 'READING', '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),

-- Week 5-6: Writing Skills Development
(31, 3, 7, 'Writing Task 1: Line & Bar Charts', 'Describe trends and comparisons in line and bar graphs', 'WRITING', '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(32, 3, 8, 'Writing Task 1: Pie Charts & Tables', 'Analyze and describe data from pie charts and tables', 'WRITING', '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(33, 3, 9, 'Writing Task 2: Essay Structure & Planning', 'Learn essay structures and develop effective planning strategies', 'WRITING', '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),

-- Week 7-8: Speaking Skills Development
(34, 3, 10, 'Speaking Part 1: Personal Topics & Fluency', 'Practice fluent responses to common Part 1 topics', 'SPEAKING', '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(35, 3, 11, 'Speaking Part 2: Cue Card Techniques', 'Develop strategies for 2-minute talks using cue cards', 'SPEAKING', '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(36, 3, 12, 'Speaking Part 3: Abstract Discussions', 'Practice discussing abstract topics with extended answers', 'SPEAKING', '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07');

-- Phase 4: Test Strategies & Practice (Sessions 13-24) - Exam techniques and mock tests
INSERT INTO subject_session (id, phase_id, sequence_no, topic, student_task, skill, created_at, updated_at) VALUES
-- Week 9: Vocabulary & Grammar for Band 5.5
(37, 4, 13, 'Academic Vocabulary: Word Families & Collocations', 'Build academic word list and practice collocations', 'VOCABULARY', '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(38, 4, 14, 'Grammar for IELTS: Complex Sentences', 'Master complex sentence structures for Writing and Speaking', 'GRAMMAR', '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),

-- Week 10: Listening & Reading Test Strategies
(39, 4, 15, 'Listening: Section 4 - Academic Lectures', 'Practice note-taking and summary completion in lectures', 'LISTENING', '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(40, 4, 16, 'Reading: Summary Completion & Sentence Matching', 'Apply strategies for summary and sentence completion questions', 'READING', '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),

-- Week 11: Writing & Speaking Test Strategies
(41, 4, 17, 'Writing Task 2: Opinion Essays', 'Write and analyze opinion essay structures and arguments', 'WRITING', '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(42, 4, 18, 'Writing Task 2: Discussion Essays', 'Practice discussion essay format and balanced arguments', 'WRITING', '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(43, 4, 19, 'Speaking: Pronunciation & Intonation', 'Improve pronunciation, stress patterns and intonation', 'SPEAKING', '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),

-- Week 12: Mock Tests & Final Review
(44, 4, 20, 'Full Mock Test 1: Listening & Reading', 'Complete timed practice test under exam conditions', 'GENERAL', '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(45, 4, 21, 'Full Mock Test 1: Writing & Speaking', 'Complete writing tasks and speaking test simulation', 'GENERAL', '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(46, 4, 22, 'Mock Test Review & Error Analysis', 'Analyze mistakes and develop improvement strategies', 'GENERAL', '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(47, 4, 23, 'Final Mock Test: Full IELTS Simulation', 'Complete full IELTS test under timed conditions', 'GENERAL', '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07'),
(48, 4, 24, 'Course Review & Exam Tips', 'Review key strategies and receive personalized feedback', 'GENERAL', '2024-08-15 00:00:00+07', '2024-08-15 00:00:00+07');


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

-- PLO-CLO Mappings for Course 2
INSERT INTO plo_clo_mapping (plo_id, clo_id, status) VALUES
(3, 5, 'ACTIVE'), (4, 6, 'ACTIVE'), (5, 7, 'ACTIVE'), (3, 8, 'ACTIVE');

-- Course Session-CLO Mappings for Course 2 (Sessions 25-48) -> CLO 5-8
INSERT INTO subject_session_clo_mapping (subject_session_id, clo_id, status)
SELECT id, 5 + (id % 4), 'ACTIVE' FROM subject_session WHERE id BETWEEN 25 AND 48;


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

-- ========== Course Materials for IELTS Intermediate (Course 2) - Complete Set ==========
INSERT INTO subject_material (subject_id, phase_id, subject_session_id, title, description, material_type, url, uploaded_by) VALUES
-- Course Level Materials
(2, NULL, NULL, 'IELTS Intermediate Syllabus', 'Complete course syllabus with weekly breakdown and learning objectives.', 'DOCUMENT', '/materials/courses/2/syllabus.pdf', 5),
(2, NULL, NULL, 'IELTS Band 5.0-5.5 Target Guide', 'Comprehensive guide for achieving band 5.0-5.5 with scoring criteria.', 'DOCUMENT', '/materials/courses/2/band-guide.pdf', 5),
(2, NULL, NULL, 'Academic Word List (AWL)', 'Essential academic vocabulary for IELTS with definitions and examples.', 'DOCUMENT', '/materials/courses/2/awl-vocabulary.pdf', 5),

-- Phase 3: Skill Building Materials
(2, 3, NULL, 'Skill Building Overview', 'Introduction to intermediate IELTS skills and weekly objectives.', 'DOCUMENT', '/materials/phases/3/overview.pdf', 5),
(2, 3, NULL, 'Listening Answer Sheet Template', 'Official IELTS listening answer sheet for practice.', 'DOCUMENT', '/materials/phases/3/listening-answer-sheet.pdf', 5),

-- Session 25: Advanced Listening - Section 1
(2, 3, 25, 'Listening Section 1 Strategies', 'Comprehensive guide to Section 1 question types and techniques.', 'DOCUMENT', '/materials/sessions/25/section1-strategies.pdf', 5),
(2, 3, 25, 'Section 1 Practice Audio Set', 'Collection of 5 practice conversations with transcripts.', 'MEDIA', '/materials/sessions/25/section1-audio.mp3', 5),
(2, 3, 25, 'Note Completion Worksheet', 'Practice exercises for note completion questions.', 'DOCUMENT', '/materials/sessions/25/note-completion.docx', 5),

-- Session 26: Listening - Section 2
(2, 3, 26, 'Map Labeling Techniques', 'Step-by-step guide to map and diagram labeling questions.', 'DOCUMENT', '/materials/sessions/26/map-labeling.pdf', 5),
(2, 3, 26, 'Section 2 Practice Audio', 'Monologue recordings with map labeling exercises.', 'MEDIA', '/materials/sessions/26/section2-audio.mp3', 5),
(2, 3, 26, 'Directional Language Reference', 'Vocabulary guide for directions and locations.', 'DOCUMENT', '/materials/sessions/26/directions-vocab.pdf', 5),

-- Session 27: Listening - Section 3
(2, 3, 27, 'Section 3 Question Types', 'Analysis of academic discussion question formats.', 'DOCUMENT', '/materials/sessions/27/section3-guide.pdf', 5),
(2, 3, 27, 'Academic Discussion Audio', 'Practice recordings of university-style discussions.', 'MEDIA', '/materials/sessions/27/academic-audio.mp3', 5),
(2, 3, 27, 'Opinion Matching Exercises', 'Worksheet for identifying speaker opinions and attitudes.', 'DOCUMENT', '/materials/sessions/27/opinion-matching.docx', 5),

-- Session 28: Reading - Skimming & Scanning
(2, 3, 28, 'Skimming & Scanning Masterclass', 'Video tutorial on speed reading techniques.', 'MEDIA', '/materials/sessions/28/speed-reading.mp4', 5),
(2, 3, 28, 'Timed Reading Passages', 'Collection of passages with timed reading exercises.', 'DOCUMENT', '/materials/sessions/28/timed-passages.pdf', 5),
(2, 3, 28, 'Reading Speed Tracker', 'Worksheet to monitor and improve reading speed.', 'DOCUMENT', '/materials/sessions/28/speed-tracker.xlsx', 5),

-- Session 29: Reading - T/F/NG
(2, 3, 29, 'T/F/NG Question Analysis', 'Detailed guide to distinguishing True, False, and Not Given.', 'DOCUMENT', '/materials/sessions/29/tfng-guide.pdf', 5),
(2, 3, 29, 'T/F/NG Practice Set', '50 practice questions with detailed explanations.', 'DOCUMENT', '/materials/sessions/29/tfng-practice.pdf', 5),
(2, 3, 29, 'Common T/F/NG Traps', 'Guide to avoiding common mistakes in T/F/NG questions.', 'DOCUMENT', '/materials/sessions/29/tfng-traps.pdf', 5),

-- Session 30: Reading - Matching
(2, 3, 30, 'Matching Headings Strategy', 'Techniques for matching headings to paragraphs.', 'DOCUMENT', '/materials/sessions/30/matching-headings.pdf', 5),
(2, 3, 30, 'Feature Matching Practice', 'Exercises for matching features to categories.', 'DOCUMENT', '/materials/sessions/30/feature-matching.pdf', 5),
(2, 3, 30, 'Academic Reading Passages', 'Three full-length academic passages with questions.', 'DOCUMENT', '/materials/sessions/30/academic-passages.pdf', 5),

-- Session 31: Writing Task 1 - Line & Bar Charts
(2, 3, 31, 'Task 1 Line Chart Tutorial', 'Video guide to describing line graph trends.', 'MEDIA', '/materials/sessions/31/line-chart-tutorial.mp4', 5),
(2, 3, 31, 'Trend Description Vocabulary', 'Essential vocabulary for describing trends and changes.', 'DOCUMENT', '/materials/sessions/31/trend-vocabulary.pdf', 5),
(2, 3, 31, 'Sample Line & Bar Chart Essays', 'Model answers with examiner comments.', 'DOCUMENT', '/materials/sessions/31/model-answers.pdf', 5),

-- Session 32: Writing Task 1 - Pie Charts & Tables
(2, 3, 32, 'Pie Chart & Table Strategies', 'Guide to organizing and presenting data effectively.', 'DOCUMENT', '/materials/sessions/32/pie-table-guide.pdf', 5),
(2, 3, 32, 'Comparison Language Reference', 'Phrases for making comparisons and contrasts.', 'DOCUMENT', '/materials/sessions/32/comparison-phrases.pdf', 5),
(2, 3, 32, 'Task 1 Practice Exercises', 'Five pie chart and table exercises with model answers.', 'DOCUMENT', '/materials/sessions/32/task1-exercises.pdf', 5),

-- Session 33: Writing Task 2 - Essay Structure
(2, 3, 33, 'Essay Structure Templates', 'Templates for opinion, discussion, and problem-solution essays.', 'DOCUMENT', '/materials/sessions/33/essay-templates.pdf', 5),
(2, 3, 33, 'Planning & Brainstorming Guide', 'Techniques for effective essay planning.', 'DOCUMENT', '/materials/sessions/33/planning-guide.pdf', 5),
(2, 3, 33, 'Introduction Writing Workshop', 'Video tutorial on writing strong introductions.', 'MEDIA', '/materials/sessions/33/intro-workshop.mp4', 5),

-- Session 34: Speaking Part 1
(2, 3, 34, 'Part 1 Topic Bank', 'Collection of common Part 1 topics with sample answers.', 'DOCUMENT', '/materials/sessions/34/part1-topics.pdf', 5),
(2, 3, 34, 'Fluency Development Exercises', 'Practice activities to improve speaking fluency.', 'DOCUMENT', '/materials/sessions/34/fluency-exercises.pdf', 5),
(2, 3, 34, 'Part 1 Model Answers Audio', 'Native speaker model answers for common topics.', 'MEDIA', '/materials/sessions/34/part1-audio.mp3', 5),

-- Session 35: Speaking Part 2
(2, 3, 35, 'Cue Card Collection', '50 authentic Part 2 cue cards with preparation notes.', 'DOCUMENT', '/materials/sessions/35/cue-cards.pdf', 5),
(2, 3, 35, 'Part 2 Timing Strategies', 'How to structure a 2-minute response effectively.', 'DOCUMENT', '/materials/sessions/35/timing-guide.pdf', 5),
(2, 3, 35, 'Sample Part 2 Recordings', 'High-scoring Part 2 response examples.', 'MEDIA', '/materials/sessions/35/part2-samples.mp3', 5),

-- Session 36: Speaking Part 3
(2, 3, 36, 'Part 3 Discussion Topics', 'Abstract topics with discussion frameworks.', 'DOCUMENT', '/materials/sessions/36/part3-topics.pdf', 5),
(2, 3, 36, 'Extended Answer Techniques', 'How to extend and develop Part 3 answers.', 'DOCUMENT', '/materials/sessions/36/extension-techniques.pdf', 5),
(2, 3, 36, 'Opinion & Speculation Language', 'Phrases for expressing opinions and speculating.', 'DOCUMENT', '/materials/sessions/36/opinion-language.pdf', 5),

-- Phase 4: Test Strategies Materials
(2, 4, NULL, 'Test Strategies Overview', 'Complete guide to IELTS exam strategies and time management.', 'DOCUMENT', '/materials/phases/4/strategies-overview.pdf', 5),
(2, 4, NULL, 'Band Score Descriptors', 'Official IELTS band score descriptors for all skills.', 'DOCUMENT', '/materials/phases/4/band-descriptors.pdf', 5),

-- Session 37: Academic Vocabulary
(2, 4, 37, 'Word Families Workbook', 'Exercises on word forms and derivations.', 'DOCUMENT', '/materials/sessions/37/word-families.pdf', 5),
(2, 4, 37, 'Collocations Dictionary', 'Essential academic collocations for IELTS.', 'DOCUMENT', '/materials/sessions/37/collocations.pdf', 5),
(2, 4, 37, 'Vocabulary Quiz Set', 'Self-assessment quizzes for academic vocabulary.', 'DOCUMENT', '/materials/sessions/37/vocab-quizzes.pdf', 5),

-- Session 38: Grammar for IELTS
(2, 4, 38, 'Complex Sentence Workshop', 'Video tutorial on complex sentence structures.', 'MEDIA', '/materials/sessions/38/complex-sentences.mp4', 5),
(2, 4, 38, 'Grammar Error Analysis', 'Common grammar mistakes and corrections.', 'DOCUMENT', '/materials/sessions/38/error-analysis.pdf', 5),
(2, 4, 38, 'Sentence Combining Exercises', 'Practice combining simple sentences.', 'DOCUMENT', '/materials/sessions/38/sentence-combining.pdf', 5),

-- Session 39: Listening Section 4
(2, 4, 39, 'Section 4 Lecture Guide', 'Strategies for academic lecture comprehension.', 'DOCUMENT', '/materials/sessions/39/lecture-guide.pdf', 5),
(2, 4, 39, 'Academic Lecture Audio Set', 'Collection of university-style lectures.', 'MEDIA', '/materials/sessions/39/lectures.mp3', 5),
(2, 4, 39, 'Note-taking Templates', 'Templates for effective lecture note-taking.', 'DOCUMENT', '/materials/sessions/39/note-templates.pdf', 5),

-- Session 40: Reading Summary & Sentence
(2, 4, 40, 'Summary Completion Guide', 'Techniques for summary completion questions.', 'DOCUMENT', '/materials/sessions/40/summary-guide.pdf', 5),
(2, 4, 40, 'Sentence Matching Strategies', 'How to match sentence endings effectively.', 'DOCUMENT', '/materials/sessions/40/sentence-matching.pdf', 5),
(2, 4, 40, 'Practice Passage Collection', 'Four passages with mixed question types.', 'DOCUMENT', '/materials/sessions/40/practice-passages.pdf', 5),

-- Session 41: Writing - Opinion Essays
(2, 4, 41, 'Opinion Essay Masterclass', 'Complete guide to opinion essay structure.', 'DOCUMENT', '/materials/sessions/41/opinion-essay-guide.pdf', 5),
(2, 4, 41, 'Argument Development Workshop', 'Video on building strong arguments.', 'MEDIA', '/materials/sessions/41/argument-workshop.mp4', 5),
(2, 4, 41, 'Opinion Essay Band 6 Samples', 'Sample essays with examiner feedback.', 'DOCUMENT', '/materials/sessions/41/sample-essays.pdf', 5),

-- Session 42: Writing - Discussion Essays
(2, 4, 42, 'Discussion Essay Framework', 'Template for balanced discussion essays.', 'DOCUMENT', '/materials/sessions/42/discussion-framework.pdf', 5),
(2, 4, 42, 'Presenting Both Sides Guide', 'How to present balanced arguments.', 'DOCUMENT', '/materials/sessions/42/both-sides.pdf', 5),
(2, 4, 42, 'Practice Essay Topics', 'Ten discussion essay prompts with planning notes.', 'DOCUMENT', '/materials/sessions/42/essay-topics.pdf', 5),

-- Session 43: Speaking - Pronunciation
(2, 4, 43, 'Pronunciation Guide', 'Guide to common pronunciation issues for Vietnamese speakers.', 'DOCUMENT', '/materials/sessions/43/pronunciation-guide.pdf', 5),
(2, 4, 43, 'Stress & Intonation Audio', 'Audio exercises for stress and intonation practice.', 'MEDIA', '/materials/sessions/43/stress-intonation.mp3', 5),
(2, 4, 43, 'Minimal Pairs Practice', 'Exercises for distinguishing similar sounds.', 'DOCUMENT', '/materials/sessions/43/minimal-pairs.pdf', 5),

-- Session 44: Mock Test 1 - Listening & Reading
(2, 4, 44, 'Mock Test 1: Question Booklet', 'Full listening and reading test questions.', 'DOCUMENT', '/materials/sessions/44/mock1-questions.pdf', 5),
(2, 4, 44, 'Mock Test 1: Listening Audio', 'Complete 30-minute listening test recording.', 'MEDIA', '/materials/sessions/44/mock1-audio.mp3', 5),
(2, 4, 44, 'Mock Test 1: Answer Key', 'Answers with explanations for all questions.', 'DOCUMENT', '/materials/sessions/44/mock1-answers.pdf', 5),

-- Session 45: Mock Test 1 - Writing & Speaking
(2, 4, 45, 'Mock Test 1: Writing Tasks', 'Task 1 and Task 2 prompts with time limits.', 'DOCUMENT', '/materials/sessions/45/writing-tasks.pdf', 5),
(2, 4, 45, 'Mock Test 1: Speaking Cards', 'Part 1-3 questions for speaking simulation.', 'DOCUMENT', '/materials/sessions/45/speaking-cards.pdf', 5),
(2, 4, 45, 'Writing Scoring Checklist', 'Self-assessment checklist for writing tasks.', 'DOCUMENT', '/materials/sessions/45/writing-checklist.pdf', 5),

-- Session 46: Mock Test Review
(2, 4, 46, 'Error Analysis Worksheet', 'Template for analyzing mistakes and patterns.', 'DOCUMENT', '/materials/sessions/46/error-worksheet.pdf', 5),
(2, 4, 46, 'Improvement Action Plan', 'Guide to creating a personal improvement plan.', 'DOCUMENT', '/materials/sessions/46/action-plan.pdf', 5),
(2, 4, 46, 'Mock Test Review Video', 'Detailed walkthrough of test solutions.', 'MEDIA', '/materials/sessions/46/review-video.mp4', 5),

-- Session 47: Final Mock Test
(2, 4, 47, 'Final Mock Test: Complete Set', 'Full IELTS test with all four skills.', 'DOCUMENT', '/materials/sessions/47/final-test.pdf', 5),
(2, 4, 47, 'Final Mock: Listening Audio', 'Complete listening section recording.', 'MEDIA', '/materials/sessions/47/final-audio.mp3', 5),
(2, 4, 47, 'Final Mock: Answer Key', 'Complete answer key with score calculator.', 'DOCUMENT', '/materials/sessions/47/final-answers.pdf', 5),

-- Session 48: Course Review & Exam Tips
(2, 4, 48, 'Course Summary Review', 'Comprehensive review of all course content.', 'DOCUMENT', '/materials/sessions/48/course-summary.pdf', 5),
(2, 4, 48, 'Exam Day Checklist', 'What to bring and how to prepare for test day.', 'DOCUMENT', '/materials/sessions/48/exam-checklist.pdf', 5),
(2, 4, 48, 'Final Tips Video', 'Last-minute tips from experienced IELTS instructors.', 'MEDIA', '/materials/sessions/48/final-tips.mp4', 5);

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

-- ========== TIER 4: CLASSES & SESSIONS ==========

-- Classes: 10 total (5 per subject x 2 subjects)
-- Timing pattern per subject:
--   - Class 1: Early (2 weeks ahead)
--   - Classes 2-4: Parallel (standard timing, 1-2 sessions apart within same week)
--   - Class 5: Late (2 weeks behind)
INSERT INTO "class" (id, branch_id, subject_id, code, name, modality, start_date, planned_end_date, actual_end_date, schedule_days, max_capacity, status, approval_status, rejection_reason, created_by, decided_by, submitted_at, decided_at, created_at, updated_at) VALUES
-- IELTS Foundation (Subject 1) - 5 classes (24 sessions each)
(1, 1, 1, 'HN-IELTS-F1', 'HN IELTS Foundation 1 (Early)', 'OFFLINE', '2025-11-17', '2026-01-09', NULL, ARRAY[1,3,5]::smallint[], 20, 'ONGOING', 'APPROVED', NULL, 6, 3, '2025-11-10 10:00:00+07', '2025-11-11 14:00:00+07', '2025-11-10 10:00:00+07', NOW()),
(2, 1, 1, 'HN-IELTS-F2', 'HN IELTS Foundation 2 (Parallel A)', 'OFFLINE', '2025-12-01', '2026-01-23', NULL, ARRAY[1,3,5]::smallint[], 20, 'ONGOING', 'APPROVED', NULL, 6, 3, '2025-11-24 10:00:00+07', '2025-11-25 14:00:00+07', '2025-11-24 10:00:00+07', NOW()),
(3, 1, 1, 'HN-IELTS-F3', 'HN IELTS Foundation 3 (Parallel B)', 'OFFLINE', '2025-12-03', '2026-01-27', NULL, ARRAY[0,3,5]::smallint[], 20, 'ONGOING', 'APPROVED', NULL, 6, 3, '2025-11-26 10:00:00+07', '2025-11-27 14:00:00+07', '2025-11-26 10:00:00+07', NOW()),
(4, 1, 1, 'HN-IELTS-F4', 'HN IELTS Foundation 4 (Parallel C)', 'ONLINE', '2025-12-05', '2026-01-29', NULL, ARRAY[1,3,6]::smallint[], 25, 'ONGOING', 'APPROVED', NULL, 6, 3, '2025-11-28 10:00:00+07', '2025-11-29 14:00:00+07', '2025-11-28 10:00:00+07', NOW()),
(5, 1, 1, 'HN-IELTS-F5', 'HN IELTS Foundation 5 (Late)', 'OFFLINE', '2025-12-15', '2026-02-06', NULL, ARRAY[1,3,5]::smallint[], 20, 'SCHEDULED', 'APPROVED', NULL, 6, 3, '2025-12-08 10:00:00+07', '2025-12-09 14:00:00+07', '2025-12-08 10:00:00+07', NOW()),

-- IELTS Intermediate (Subject 2) - 5 classes (24 sessions each)
(6, 1, 2, 'HN-IELTS-I1', 'HN IELTS Intermediate 1 (Early)', 'OFFLINE', '2025-11-17', '2026-01-09', NULL, ARRAY[2,4,6]::smallint[], 20, 'ONGOING', 'APPROVED', NULL, 6, 3, '2025-11-10 10:00:00+07', '2025-11-11 14:00:00+07', '2025-11-10 10:00:00+07', NOW()),
(7, 1, 2, 'HN-IELTS-I2', 'HN IELTS Intermediate 2 (Parallel A)', 'OFFLINE', '2025-12-02', '2026-01-24', NULL, ARRAY[2,4,6]::smallint[], 20, 'ONGOING', 'APPROVED', NULL, 6, 3, '2025-11-25 10:00:00+07', '2025-11-26 14:00:00+07', '2025-11-25 10:00:00+07', NOW()),

(8, 1, 2, 'HN-IELTS-I3', 'HN IELTS Intermediate 3 (Parallel B)', 'OFFLINE', '2025-12-04', '2026-01-28', NULL, ARRAY[1,4,6]::smallint[], 20, 'ONGOING', 'APPROVED', NULL, 6, 3, '2025-11-27 10:00:00+07', '2025-11-28 14:00:00+07', '2025-11-27 10:00:00+07', NOW()),
(9, 1, 2, 'HN-IELTS-I4', 'HN IELTS Intermediate 4 (Parallel C)', 'ONLINE', '2025-12-06', '2026-01-30', NULL, ARRAY[0,2,4]::smallint[], 25, 'ONGOING', 'APPROVED', NULL, 6, 3, '2025-11-29 10:00:00+07', '2025-11-30 14:00:00+07', '2025-11-29 10:00:00+07', NOW()),
(10, 1, 2, 'HN-IELTS-I5', 'HN IELTS Intermediate 5 (Late)', 'OFFLINE', '2025-12-16', '2026-02-07', NULL, ARRAY[2,4,6]::smallint[], 20, 'SCHEDULED', 'APPROVED', NULL, 6, 3, '2025-12-09 10:00:00+07', '2025-12-10 14:00:00+07', '2025-12-09 10:00:00+07', NOW()),

-- Ho Chi Minh Branch Classes
-- IELTS Foundation (Subject 1) - 3 classes
(11, 2, 1, 'HCM-IELTS-F1', 'HCM IELTS Foundation 1', 'OFFLINE', '2025-12-18', '2026-02-10', NULL, ARRAY[1,3,5]::smallint[], 20, 'SCHEDULED', 'APPROVED', NULL, 8, 4, '2025-12-11 10:00:00+07', '2025-12-12 14:00:00+07', '2025-12-11 10:00:00+07', NOW()),
(12, 2, 1, 'HCM-IELTS-F2', 'HCM IELTS Foundation 2', 'OFFLINE', '2025-12-20', '2026-02-12', NULL, ARRAY[2,4,6]::smallint[], 20, 'SCHEDULED', 'APPROVED', NULL, 8, 4, '2025-12-13 10:00:00+07', '2025-12-14 14:00:00+07', '2025-12-13 10:00:00+07', NOW()),
(13, 2, 1, 'HCM-IELTS-F3', 'HCM IELTS Foundation 3 (Online)', 'ONLINE', '2025-12-22', '2026-02-14', NULL, ARRAY[1,3,5]::smallint[], 25, 'SCHEDULED', 'APPROVED', NULL, 8, 4, '2025-12-15 10:00:00+07', '2025-12-16 14:00:00+07', '2025-12-15 10:00:00+07', NOW()),

-- IELTS Intermediate (Subject 2) - 3 classes
(14, 2, 2, 'HCM-IELTS-I1', 'HCM IELTS Intermediate 1', 'OFFLINE', '2025-12-19', '2026-02-11', NULL, ARRAY[2,4,6]::smallint[], 20, 'SCHEDULED', 'APPROVED', NULL, 8, 4, '2025-12-12 10:00:00+07', '2025-12-13 14:00:00+07', '2025-12-12 10:00:00+07', NOW()),
(15, 2, 2, 'HCM-IELTS-I2', 'HCM IELTS Intermediate 2', 'OFFLINE', '2025-12-21', '2026-02-13', NULL, ARRAY[1,3,5]::smallint[], 20, 'SCHEDULED', 'APPROVED', NULL, 8, 4, '2025-12-14 10:00:00+07', '2025-12-15 14:00:00+07', '2025-12-14 10:00:00+07', NOW()),
(16, 2, 2, 'HCM-IELTS-I3', 'HCM IELTS Intermediate 3 (Online)', 'ONLINE', '2025-12-23', '2026-02-15', NULL, ARRAY[2,4,6]::smallint[], 25, 'SCHEDULED', 'APPROVED', NULL, 8, 4, '2025-12-16 10:00:00+07', '2025-12-17 14:00:00+07', '2025-12-16 10:00:00+07', NOW()),

-- New class for demo - Tue/Thu/Sat schedule starting 2025-12-20
(31, 1, 1, 'HN-IELTS-F6', 'HN IELTS Foundation 6 (Demo T3/T5/T7)', 'OFFLINE', '2025-12-20', '2026-02-12', NULL, ARRAY[2,4,6]::smallint[], 20, 'SCHEDULED', 'APPROVED', NULL, 6, 3, '2025-12-13 10:00:00+07', '2025-12-14 14:00:00+07', '2025-12-13 10:00:00+07', NOW());

-- Class 1: HN IELTS Foundation 1 (Early) - Mon/Wed/Fri, starts 2025-11-17 (2 weeks ahead)
INSERT INTO session (id, class_id, subject_session_id, time_slot_template_id, date, type, status, created_at, updated_at)
SELECT s.idx, 1, s.idx, 1, ('2025-11-17'::date + ((s.idx - 1) / 3) * 7 + CASE (s.idx - 1) % 3 WHEN 0 THEN 0 WHEN 1 THEN 2 ELSE 4 END), 'CLASS',
  CASE WHEN ('2025-11-17'::date + ((s.idx - 1) / 3) * 7 + CASE (s.idx - 1) % 3 WHEN 0 THEN 0 WHEN 1 THEN 2 ELSE 4 END) < '2025-12-10' THEN 'DONE' ELSE 'PLANNED' END, '2025-11-10 10:00:00+07', NOW()
FROM generate_series(1, 24) AS s(idx);
INSERT INTO session_resource (session_id, resource_id) SELECT id, 1 FROM session WHERE class_id = 1;
INSERT INTO teaching_slot (session_id, teacher_id, status) SELECT id, 1, 'SCHEDULED' FROM session WHERE class_id = 1;

-- Class 2: HN IELTS Foundation 2 (Parallel A) - Mon/Wed/Fri, starts 2025-12-01
INSERT INTO session (id, class_id, subject_session_id, time_slot_template_id, date, type, status, created_at, updated_at)
SELECT 100 + s.idx, 2, s.idx, 2, ('2025-12-01'::date + ((s.idx - 1) / 3) * 7 + CASE (s.idx - 1) % 3 WHEN 0 THEN 0 WHEN 1 THEN 2 ELSE 4 END), 'CLASS',
  CASE WHEN ('2025-12-01'::date + ((s.idx - 1) / 3) * 7 + CASE (s.idx - 1) % 3 WHEN 0 THEN 0 WHEN 1 THEN 2 ELSE 4 END) < '2025-12-10' THEN 'DONE' ELSE 'PLANNED' END, '2025-11-24 10:00:00+07', NOW()
FROM generate_series(1, 24) AS s(idx);
INSERT INTO session_resource (session_id, resource_id) SELECT id, 2 FROM session WHERE class_id = 2;
INSERT INTO teaching_slot (session_id, teacher_id, status) SELECT id, 2, 'SCHEDULED' FROM session WHERE class_id = 2;

-- Class 3: HN IELTS Foundation 3 (Parallel B) - Mon/Wed/Fri, starts 2025-12-03 (2 days behind Class 2)
INSERT INTO session (id, class_id, subject_session_id, time_slot_template_id, date, type, status, created_at, updated_at)
SELECT 200 + s.idx, 3, s.idx, 3, ('2025-12-03'::date + ((s.idx - 1) / 3) * 7 + CASE (s.idx - 1) % 3 WHEN 0 THEN 0 WHEN 1 THEN 2 ELSE 4 END), 'CLASS',
  CASE WHEN ('2025-12-03'::date + ((s.idx - 1) / 3) * 7 + CASE (s.idx - 1) % 3 WHEN 0 THEN 0 WHEN 1 THEN 2 ELSE 4 END) < '2025-12-10' THEN 'DONE' ELSE 'PLANNED' END, '2025-11-26 10:00:00+07', NOW()
FROM generate_series(1, 24) AS s(idx);
INSERT INTO session_resource (session_id, resource_id) SELECT id, 3 FROM session WHERE class_id = 3;
INSERT INTO teaching_slot (session_id, teacher_id, status) SELECT id, 3, 'SCHEDULED' FROM session WHERE class_id = 3;

-- Class 4: HN IELTS Foundation 4 (Parallel C - Online) - Tue/Thu/Sat, starts 2025-12-05 (4 days behind Class 2)
INSERT INTO session (id, class_id, subject_session_id, time_slot_template_id, date, type, status, created_at, updated_at)
SELECT 300 + s.idx, 4, s.idx, 5, ('2025-12-05'::date + ((s.idx - 1) / 3) * 7 + CASE (s.idx - 1) % 3 WHEN 0 THEN 1 WHEN 1 THEN 3 ELSE 5 END), 'CLASS',
  CASE WHEN ('2025-12-05'::date + ((s.idx - 1) / 3) * 7 + CASE (s.idx - 1) % 3 WHEN 0 THEN 1 WHEN 1 THEN 3 ELSE 5 END) < '2025-12-10' THEN 'DONE' ELSE 'PLANNED' END, '2025-11-28 10:00:00+07', NOW()
FROM generate_series(1, 24) AS s(idx);
INSERT INTO session_resource (session_id, resource_id) SELECT id, 4 FROM session WHERE class_id = 4;
INSERT INTO teaching_slot (session_id, teacher_id, status) SELECT id, 4, 'SCHEDULED' FROM session WHERE class_id = 4;

-- Class 5: HN IELTS Foundation 5 (Late) - Mon/Wed/Fri, starts 2025-12-15 (2 weeks behind)
INSERT INTO session (id, class_id, subject_session_id, time_slot_template_id, date, type, status, created_at, updated_at)
SELECT 400 + s.idx, 5, s.idx, 1, ('2025-12-15'::date + ((s.idx - 1) / 3) * 7 + CASE (s.idx - 1) % 3 WHEN 0 THEN 0 WHEN 1 THEN 2 ELSE 4 END), 'CLASS', 'PLANNED', '2025-12-08 10:00:00+07', NOW()
FROM generate_series(1, 24) AS s(idx);
INSERT INTO session_resource (session_id, resource_id) SELECT id, 1 FROM session WHERE class_id = 5;
INSERT INTO teaching_slot (session_id, teacher_id, status) SELECT id, 5, 'SCHEDULED' FROM session WHERE class_id = 5;

-- Class 6: HN IELTS Intermediate 1 (Early) - Tue/Thu/Sat, starts 2025-11-17 (2 weeks ahead)
INSERT INTO session (id, class_id, subject_session_id, time_slot_template_id, date, type, status, created_at, updated_at)
SELECT 500 + s.idx, 6, 24 + s.idx, 6, ('2025-11-17'::date + ((s.idx - 1) / 3) * 7 + CASE (s.idx - 1) % 3 WHEN 0 THEN 1 WHEN 1 THEN 3 ELSE 5 END), 'CLASS',
  CASE WHEN ('2025-11-17'::date + ((s.idx - 1) / 3) * 7 + CASE (s.idx - 1) % 3 WHEN 0 THEN 1 WHEN 1 THEN 3 ELSE 5 END) < '2025-12-10' THEN 'DONE' ELSE 'PLANNED' END, '2025-11-10 10:00:00+07', NOW()
FROM generate_series(1, 24) AS s(idx);
INSERT INTO session_resource (session_id, resource_id) SELECT id, 2 FROM session WHERE class_id = 6;
INSERT INTO teaching_slot (session_id, teacher_id, status) SELECT id, 6, 'SCHEDULED' FROM session WHERE class_id = 6;

-- Class 7: HN IELTS Intermediate 2 (Parallel A) - Tue/Thu/Sat, starts 2025-12-02
INSERT INTO session (id, class_id, subject_session_id, time_slot_template_id, date, type, status, created_at, updated_at)
SELECT 600 + s.idx, 7, 24 + s.idx, 6, ('2025-12-02'::date + ((s.idx - 1) / 3) * 7 + CASE (s.idx - 1) % 3 WHEN 0 THEN 0 WHEN 1 THEN 2 ELSE 4 END), 'CLASS',
  CASE WHEN ('2025-12-02'::date + ((s.idx - 1) / 3) * 7 + CASE (s.idx - 1) % 3 WHEN 0 THEN 0 WHEN 1 THEN 2 ELSE 4 END) < '2025-12-10' THEN 'DONE' ELSE 'PLANNED' END, '2025-11-25 10:00:00+07', NOW()
FROM generate_series(1, 24) AS s(idx);
INSERT INTO session_resource (session_id, resource_id) SELECT id, 3 FROM session WHERE class_id = 7;
INSERT INTO teaching_slot (session_id, teacher_id, status) SELECT id, 7, 'SCHEDULED' FROM session WHERE class_id = 7;

-- Class 8: HN IELTS Intermediate 3 (Parallel B) - Tue/Thu/Sat, starts 2025-12-04 (2 days behind Class 7)
INSERT INTO session (id, class_id, subject_session_id, time_slot_template_id, date, type, status, created_at, updated_at)
SELECT 700 + s.idx, 8, 24 + s.idx, 6, ('2025-12-04'::date + ((s.idx - 1) / 3) * 7 + CASE (s.idx - 1) % 3 WHEN 0 THEN 0 WHEN 1 THEN 2 ELSE 4 END), 'CLASS',
  CASE WHEN ('2025-12-04'::date + ((s.idx - 1) / 3) * 7 + CASE (s.idx - 1) % 3 WHEN 0 THEN 0 WHEN 1 THEN 2 ELSE 4 END) < '2025-12-10' THEN 'DONE' ELSE 'PLANNED' END, '2025-11-27 10:00:00+07', NOW()
FROM generate_series(1, 24) AS s(idx);
INSERT INTO session_resource (session_id, resource_id) SELECT id, 2 FROM session WHERE class_id = 8;
INSERT INTO teaching_slot (session_id, teacher_id, status) SELECT id, 8, 'SCHEDULED' FROM session WHERE class_id = 8;

-- Class 9: HN IELTS Intermediate 4 (Parallel C - Online) - Tue/Thu/Sat, starts 2025-12-06 (4 days behind Class 7)
INSERT INTO session (id, class_id, subject_session_id, time_slot_template_id, date, type, status, created_at, updated_at)
SELECT 800 + s.idx, 9, 24 + s.idx, 5, ('2025-12-06'::date + ((s.idx - 1) / 3) * 7 + CASE (s.idx - 1) % 3 WHEN 0 THEN 1 WHEN 1 THEN 3 ELSE 5 END), 'CLASS',
  CASE WHEN ('2025-12-06'::date + ((s.idx - 1) / 3) * 7 + CASE (s.idx - 1) % 3 WHEN 0 THEN 1 WHEN 1 THEN 3 ELSE 5 END) < '2025-12-10' THEN 'DONE' ELSE 'PLANNED' END, '2025-11-29 10:00:00+07', NOW()
FROM generate_series(1, 24) AS s(idx);
INSERT INTO session_resource (session_id, resource_id) SELECT id, 4 FROM session WHERE class_id = 9;
INSERT INTO teaching_slot (session_id, teacher_id, status) SELECT id, 9, 'SCHEDULED' FROM session WHERE class_id = 9;

-- Class 10: HN IELTS Intermediate 5 (Late) - Tue/Thu/Sat, starts 2025-12-16 (2 weeks behind)
INSERT INTO session (id, class_id, subject_session_id, time_slot_template_id, date, type, status, created_at, updated_at)
SELECT 900 + s.idx, 10, 24 + s.idx, 6, ('2025-12-16'::date + ((s.idx - 1) / 3) * 7 + CASE (s.idx - 1) % 3 WHEN 0 THEN 0 WHEN 1 THEN 2 ELSE 4 END), 'CLASS', 'PLANNED', '2025-12-09 10:00:00+07', NOW()
FROM generate_series(1, 24) AS s(idx);
INSERT INTO session_resource (session_id, resource_id) SELECT id, 3 FROM session WHERE class_id = 10;
INSERT INTO teaching_slot (session_id, teacher_id, status) SELECT id, 10, 'SCHEDULED' FROM session WHERE class_id = 10;

-- Class 11: HCM IELTS Foundation 1 - Mon/Wed/Fri, starts 2025-12-18
INSERT INTO session (id, class_id, subject_session_id, time_slot_template_id, date, type, status, created_at, updated_at)
SELECT 1000 + s.idx, 11, s.idx, 10, ('2025-12-18'::date + ((s.idx - 1) / 3) * 7 + CASE (s.idx - 1) % 3 WHEN 0 THEN 0 WHEN 1 THEN 2 ELSE 4 END), 'CLASS', 'PLANNED', '2025-12-11 10:00:00+07', NOW()
FROM generate_series(1, 24) AS s(idx);
INSERT INTO session_resource (session_id, resource_id) SELECT id, 5 FROM session WHERE class_id = 11;
INSERT INTO teaching_slot (session_id, teacher_id, status) SELECT id, 9, 'SCHEDULED' FROM session WHERE class_id = 11;

-- Class 12: HCM IELTS Foundation 2 - Tue/Thu/Sat, starts 2025-12-20
INSERT INTO session (id, class_id, subject_session_id, time_slot_template_id, date, type, status, created_at, updated_at)
SELECT 1100 + s.idx, 12, s.idx, 11, ('2025-12-20'::date + ((s.idx - 1) / 3) * 7 + CASE (s.idx - 1) % 3 WHEN 0 THEN 1 WHEN 1 THEN 3 ELSE 5 END), 'CLASS', 'PLANNED', '2025-12-13 10:00:00+07', NOW()
FROM generate_series(1, 24) AS s(idx);
INSERT INTO session_resource (session_id, resource_id) SELECT id, 6 FROM session WHERE class_id = 12;
INSERT INTO teaching_slot (session_id, teacher_id, status) SELECT id, 10, 'SCHEDULED' FROM session WHERE class_id = 12;

-- Class 13: HCM IELTS Foundation 3 (Online) - Mon/Wed/Fri, starts 2025-12-22
INSERT INTO session (id, class_id, subject_session_id, time_slot_template_id, date, type, status, created_at, updated_at)
SELECT 1200 + s.idx, 13, s.idx, 13, ('2025-12-22'::date + ((s.idx - 1) / 3) * 7 + CASE (s.idx - 1) % 3 WHEN 0 THEN 0 WHEN 1 THEN 2 ELSE 4 END), 'CLASS', 'PLANNED', '2025-12-15 10:00:00+07', NOW()
FROM generate_series(1, 24) AS s(idx);
INSERT INTO session_resource (session_id, resource_id) SELECT id, 8 FROM session WHERE class_id = 13;
INSERT INTO teaching_slot (session_id, teacher_id, status) SELECT id, 11, 'SCHEDULED' FROM session WHERE class_id = 13;

-- Class 14: HCM IELTS Intermediate 1 - Tue/Thu/Sat, starts 2025-12-19
INSERT INTO session (id, class_id, subject_session_id, time_slot_template_id, date, type, status, created_at, updated_at)
SELECT 1300 + s.idx, 14, 24 + s.idx, 11, ('2025-12-19'::date + ((s.idx - 1) / 3) * 7 + CASE (s.idx - 1) % 3 WHEN 0 THEN 1 WHEN 1 THEN 3 ELSE 5 END), 'CLASS', 'PLANNED', '2025-12-12 10:00:00+07', NOW()
FROM generate_series(1, 24) AS s(idx);
INSERT INTO session_resource (session_id, resource_id) SELECT id, 7 FROM session WHERE class_id = 14;
INSERT INTO teaching_slot (session_id, teacher_id, status) SELECT id, 12, 'SCHEDULED' FROM session WHERE class_id = 14;

-- Class 15: HCM IELTS Intermediate 2 - Mon/Wed/Fri, starts 2025-12-21
INSERT INTO session (id, class_id, subject_session_id, time_slot_template_id, date, type, status, created_at, updated_at)
SELECT 1400 + s.idx, 15, 24 + s.idx, 10, ('2025-12-21'::date + ((s.idx - 1) / 3) * 7 + CASE (s.idx - 1) % 3 WHEN 0 THEN 0 WHEN 1 THEN 2 ELSE 4 END), 'CLASS', 'PLANNED', '2025-12-14 10:00:00+07', NOW()
FROM generate_series(1, 24) AS s(idx);
INSERT INTO session_resource (session_id, resource_id) SELECT id, 5 FROM session WHERE class_id = 15;
INSERT INTO teaching_slot (session_id, teacher_id, status) SELECT id, 13, 'SCHEDULED' FROM session WHERE class_id = 15;

-- Class 16: HCM IELTS Intermediate 3 (Online) - Tue/Thu/Sat, starts 2025-12-23
INSERT INTO session (id, class_id, subject_session_id, time_slot_template_id, date, type, status, created_at, updated_at)
SELECT 1500 + s.idx, 16, 24 + s.idx, 13, ('2025-12-23'::date + ((s.idx - 1) / 3) * 7 + CASE (s.idx - 1) % 3 WHEN 0 THEN 1 WHEN 1 THEN 3 ELSE 5 END), 'CLASS', 'PLANNED', '2025-12-16 10:00:00+07', NOW()
FROM generate_series(1, 24) AS s(idx);
INSERT INTO session_resource (session_id, resource_id) SELECT id, 8 FROM session WHERE class_id = 16;
INSERT INTO teaching_slot (session_id, teacher_id, status) SELECT id, 14, 'SCHEDULED' FROM session WHERE class_id = 16;

-- Class 31: HN IELTS Foundation 6 (Demo T3/T5/T7) - Tue/Thu/Sat, starts 2025-12-20
-- 20/12/2025 is Saturday, schedule [2,4,6] = Tue/Thu/Sat
-- Session 1: 20/12 (Sat), Session 2: 23/12 (Tue), Session 3: 25/12 (Thu), Session 4: 27/12 (Sat)...
INSERT INTO session (id, class_id, subject_session_id, time_slot_template_id, date, type, status, created_at, updated_at)
SELECT 3100 + s.idx, 31, s.idx, 3, ('2025-12-20'::date + ((s.idx - 1) / 3) * 7 + CASE (s.idx - 1) % 3 WHEN 0 THEN 0 WHEN 1 THEN 3 ELSE 5 END), 'CLASS', 'PLANNED', '2025-12-13 10:00:00+07', NOW()
FROM generate_series(1, 24) AS s(idx);
INSERT INTO session_resource (session_id, resource_id) SELECT id, 1 FROM session WHERE class_id = 31;
INSERT INTO teaching_slot (session_id, teacher_id, status) SELECT id, 5, 'SCHEDULED' FROM session WHERE class_id = 31;

-- ========== UPDATE assigned_teacher_id for some classes (leave some for testing) ==========
-- Only assign teachers to ONGOING classes (1-4), leave SCHEDULED classes (5+) without teacher
UPDATE "class" SET assigned_teacher_id = 1, teacher_assigned_at = created_at WHERE id = 1;  -- John Smith → HN-IELTS-F1
UPDATE "class" SET assigned_teacher_id = 2, teacher_assigned_at = created_at WHERE id = 2;  -- Emma Wilson → HN-IELTS-F2
UPDATE "class" SET assigned_teacher_id = 3, teacher_assigned_at = created_at WHERE id = 3;  -- David Lee → HN-IELTS-F3
UPDATE "class" SET assigned_teacher_id = 4, teacher_assigned_at = created_at WHERE id = 4;  -- Sarah Johnson → HN-IELTS-F4
-- Classes 5-16 left without assigned_teacher for testing schedule conflict detection

-- Enrollments for Class 1 (HN-IELTS-F1 - Early) - 15 students (IDs 1-15)
INSERT INTO enrollment (id, class_id, student_id, status, enrolled_at, enrolled_by, join_session_id, created_at, updated_at)
SELECT (100 + s.id), 1, s.id, 'ENROLLED', '2025-11-10 09:00:00+07', 6, 1, '2025-11-10 09:00:00+07', NOW()
FROM generate_series(1, 15) AS s(id);

-- Enrollments for Class 2 (HN-IELTS-F2 - Parallel A) - 15 students (IDs 16-30)
INSERT INTO enrollment (id, class_id, student_id, status, enrolled_at, enrolled_by, join_session_id, created_at, updated_at)
SELECT (200 + s.id), 2, s.id + 15, 'ENROLLED', '2025-11-24 09:00:00+07', 6, 101, '2025-11-24 09:00:00+07', NOW()
FROM generate_series(1, 15) AS s(id);

-- Enrollments for Class 3 (HN-IELTS-F3 - Parallel B) - 15 students (IDs 31-45)
INSERT INTO enrollment (id, class_id, student_id, status, enrolled_at, enrolled_by, join_session_id, created_at, updated_at)
SELECT (300 + s.id), 3, s.id + 30, 'ENROLLED', '2025-11-26 09:00:00+07', 6, 201, '2025-11-26 09:00:00+07', NOW()
FROM generate_series(1, 15) AS s(id);

-- Enrollments for Class 4 (HN-IELTS-F4 - Parallel C Online) - 20 students (IDs 46-65)
INSERT INTO enrollment (id, class_id, student_id, status, enrolled_at, enrolled_by, join_session_id, created_at, updated_at)
SELECT (400 + s.id), 4, s.id + 45, 'ENROLLED', '2025-11-28 09:00:00+07', 6, 301, '2025-11-28 09:00:00+07', NOW()
FROM generate_series(1, 20) AS s(id);

-- Enrollments for Class 5 (HN-IELTS-F5 - Late) - 10 students (IDs 66-75) - SCHEDULED class
INSERT INTO enrollment (id, class_id, student_id, status, enrolled_at, enrolled_by, join_session_id, created_at, updated_at)
SELECT (500 + s.id), 5, s.id + 65, 'ENROLLED', '2025-12-08 09:00:00+07', 6, 401, '2025-12-08 09:00:00+07', NOW()
FROM generate_series(1, 10) AS s(id);

-- Enrollments for Class 6 (HN-IELTS-I1 - Early) - 12 students (IDs 76-87)
INSERT INTO enrollment (id, class_id, student_id, status, enrolled_at, enrolled_by, join_session_id, created_at, updated_at)
SELECT (600 + s.id), 6, s.id + 75, 'ENROLLED', '2025-11-10 09:00:00+07', 6, 501, '2025-11-10 09:00:00+07', NOW()
FROM generate_series(1, 12) AS s(id);

-- Enrollments for Class 7 (HN-IELTS-I2 - Parallel A) - 12 students (IDs 88-99)
INSERT INTO enrollment (id, class_id, student_id, status, enrolled_at, enrolled_by, join_session_id, created_at, updated_at)
SELECT (700 + s.id), 7, s.id + 87, 'ENROLLED', '2025-11-25 09:00:00+07', 6, 601, '2025-11-25 09:00:00+07', NOW()
FROM generate_series(1, 12) AS s(id);

-- Enrollments for Class 8 (HN-IELTS-I3 - Parallel B) - 1 student (ID 100)
INSERT INTO enrollment (id, class_id, student_id, status, enrolled_at, enrolled_by, join_session_id, created_at, updated_at) VALUES
(801, 8, 100, 'ENROLLED', '2025-11-27 09:00:00+07', 6, 701, '2025-11-27 09:00:00+07', NOW());

-- Enrollments for Class 9 (HN-IELTS-I4 - Parallel C Online) - reusing students 1-5 (they can take both Foundation and Intermediate)
INSERT INTO enrollment (id, class_id, student_id, status, enrolled_at, enrolled_by, join_session_id, created_at, updated_at)
SELECT (900 + s.id), 9, s.id, 'ENROLLED', '2025-11-29 09:00:00+07', 6, 801, '2025-11-29 09:00:00+07', NOW()
FROM generate_series(1, 5) AS s(id);

-- Enrollments for Class 10 (HN-IELTS-I5 - Late) - SCHEDULED class, reusing students 6-10
INSERT INTO enrollment (id, class_id, student_id, status, enrolled_at, enrolled_by, join_session_id, created_at, updated_at)
SELECT (1000 + s.id), 10, s.id + 5, 'ENROLLED', '2025-12-09 09:00:00+07', 6, 901, '2025-12-09 09:00:00+07', NOW()
FROM generate_series(1, 5) AS s(id);

-- Enrollments for HCM Classes
-- Enrollments for Class 11 (HCM-IELTS-F1) - 15 students (student_id 51-65, from user_id 151-165)
INSERT INTO enrollment (id, class_id, student_id, status, enrolled_at, enrolled_by, join_session_id, created_at, updated_at)
SELECT (1100 + s.id), 11, s.id + 50, 'ENROLLED', '2025-12-11 09:00:00+07', 8, 1001, '2025-12-11 09:00:00+07', NOW()
FROM generate_series(1, 15) AS s(id);

-- Enrollments for Class 12 (HCM-IELTS-F2) - 15 students (student_id 66-80, from user_id 166-180)
INSERT INTO enrollment (id, class_id, student_id, status, enrolled_at, enrolled_by, join_session_id, created_at, updated_at)
SELECT (1200 + s.id), 12, s.id + 65, 'ENROLLED', '2025-12-13 09:00:00+07', 8, 1101, '2025-12-13 09:00:00+07', NOW()
FROM generate_series(1, 15) AS s(id);

-- Enrollments for Class 13 (HCM-IELTS-F3 - Online) - 20 students (student_id 81-100, from user_id 181-200)
INSERT INTO enrollment (id, class_id, student_id, status, enrolled_at, enrolled_by, join_session_id, created_at, updated_at)
SELECT (1300 + s.id), 13, s.id + 80, 'ENROLLED', '2025-12-15 09:00:00+07', 8, 1201, '2025-12-15 09:00:00+07', NOW()
FROM generate_series(1, 20) AS s(id);

-- Enrollments for Class 14 (HCM-IELTS-I1) - 12 students (student_id 51-62, from user_id 151-162)
INSERT INTO enrollment (id, class_id, student_id, status, enrolled_at, enrolled_by, join_session_id, created_at, updated_at)
SELECT (1400 + s.id), 14, s.id + 50, 'ENROLLED', '2025-12-12 09:00:00+07', 8, 1301, '2025-12-12 09:00:00+07', NOW()
FROM generate_series(1, 12) AS s(id);

-- Enrollments for Class 15 (HCM-IELTS-I2) - 12 students (student_id 63-74, from user_id 163-174)
INSERT INTO enrollment (id, class_id, student_id, status, enrolled_at, enrolled_by, join_session_id, created_at, updated_at)
SELECT (1500 + s.id), 15, s.id + 62, 'ENROLLED', '2025-12-14 09:00:00+07', 8, 1401, '2025-12-14 09:00:00+07', NOW()
FROM generate_series(1, 12) AS s(id);

-- Enrollments for Class 16 (HCM-IELTS-I3 - Online) - 18 students (student_id 75-92, from user_id 175-192)
INSERT INTO enrollment (id, class_id, student_id, status, enrolled_at, enrolled_by, join_session_id, created_at, updated_at)
SELECT (1600 + s.id), 16, s.id + 74, 'ENROLLED', '2025-12-16 09:00:00+07', 8, 1501, '2025-12-16 09:00:00+07', NOW()
FROM generate_series(1, 18) AS s(id);

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
-- Student 20 (Nguyen Thi Linh - in Class 2) has been absent for most sessions
UPDATE student_session
SET attendance_status = 'ABSENT'
WHERE student_id = 20 -- Student ID 20 = Nguyen Thi Linh, enrolled in Class 2
  AND session_id IN (SELECT id FROM session WHERE class_id = 2 AND status = 'DONE');

-- 2. Test absence on specific session
-- Student 16 (Vuong Thi Loan - in Class 2) absent on session 105
UPDATE student_session
SET attendance_status = 'ABSENT'
WHERE student_id = 16 AND session_id = 105;

-- 3. Teacher Forgot Attendance Scenario (Test Reminders)
-- Session 105 (Class 2) will have attendance_status = 'PLANNED' to indicate teacher hasn't recorded yet
UPDATE student_session 
SET attendance_status = 'PLANNED', recorded_at = NULL, homework_status = NULL
WHERE session_id = 105;

-- SCENARIO 1: Approved Absence Request (Class 2 - Foundation Parallel A, student 20 - Nguyen Thi Linh)
INSERT INTO student_request (id, student_id, current_class_id, request_type, target_session_id, status, request_reason, submitted_by, submitted_at, decided_by, decided_at, note) VALUES
(1, 20, 2, 'ABSENCE', 110, 'APPROVED', 'Family emergency - need to attend urgent family matter', 120, '2025-12-01 10:00:00+07', 6, '2025-12-01 14:00:00+07', 'Approved - valid reason');

-- SCENARIO 2: Pending Absence Request (Class 2, student 21 - Pham Van Long)
INSERT INTO student_request (id, student_id, current_class_id, request_type, target_session_id, status, request_reason, submitted_by, submitted_at) VALUES
(2, 21, 2, 'ABSENCE', 115, 'PENDING', 'Medical appointment - doctor consultation scheduled', 121, '2025-12-03 09:00:00+07');

-- SCENARIO 3: Rejected Absence Request (Class 3 - Foundation Parallel B, student 35 - Le Duc Thanh)
INSERT INTO student_request (id, student_id, current_class_id, request_type, target_session_id, status, request_reason, submitted_by, submitted_at, decided_by, decided_at, note) VALUES
(3, 35, 3, 'ABSENCE', 210, 'REJECTED', 'Want to attend friend birthday party', 135, '2025-12-02 10:00:00+07', 6, '2025-12-02 15:00:00+07', 'Rejected - not a valid reason for academic absence');

-- SCENARIO 4: Approved Makeup Request (Class 2 -> Class 3 parallel Foundation classes, student 22 - Vu Thi My Hanh)
-- Student 22 vắng buổi 105 (Class 2, 10/12/2025) -> xin học bù tại buổi 205 (Class 3, 12/12/2025)
INSERT INTO student_request (id, student_id, current_class_id, request_type, target_session_id, makeup_session_id, status, request_reason, submitted_by, submitted_at, decided_by, decided_at) VALUES
(4, 22, 2, 'MAKEUP', 105, 205, 'APPROVED', 'Missed session due to illness, want to makeup in parallel class', 122, '2025-12-01 10:00:00+07', 6, '2025-12-01 16:00:00+07');

-- Khi APPROVED: Cần tạo StudentSession cho buổi học bù (session 205) và update buổi vắng (session 105) thành EXCUSED
-- 4a. Update buổi vắng gốc (session 105) thành EXCUSED + link tới makeup session
UPDATE student_session 
SET attendance_status = 'EXCUSED', 
    makeup_session_id = 205,
    note = 'Excused - học bù tại HN-IELTS-F3 ngày 12/12/2025 (14:00-16:30)'
WHERE student_id = 22 AND session_id = 105;

-- 4b. Tạo StudentSession cho buổi học bù (student 22 tham gia session 205 của Class 3)
INSERT INTO student_session (student_id, session_id, is_makeup, original_session_id, attendance_status, note, created_at, updated_at) VALUES
(22, 205, true, 105, 'PLANNED', 'Makeup for session 105', NOW(), NOW());

-- SCENARIO 5: Pending Makeup Request (Class 2, student 23 - Doan Van Khanh)
INSERT INTO student_request (id, student_id, current_class_id, request_type, target_session_id, makeup_session_id, status, request_reason, submitted_by, submitted_at) VALUES
(5, 23, 2, 'MAKEUP', 108, 208, 'PENDING', 'Missed session due to work commitment, requesting makeup', 123, '2025-12-04 11:00:00+07');

-- SCENARIO 6: Approved Transfer Request (Class 2 Offline -> Class 4 Online, student 24 - Tran Thi Nga)
-- target_session_id=310 means: join Class 4 starting from their session on 2025-12-10
INSERT INTO student_request (id, student_id, current_class_id, target_class_id, request_type, target_session_id, status, request_reason, submitted_by, submitted_at, decided_by, decided_at) VALUES
(6, 24, 2, 4, 'TRANSFER', 310, 'APPROVED', 'Need to change to online class due to work schedule conflict', 6, '2025-12-05 10:00:00+07', 6, '2025-12-06 14:00:00+07');

-- SCENARIO 7: Pending Transfer Request (Class 6 Intermediate -> Class 9 Online Intermediate, student 80 - Dinh Thi Thao)
-- target_session_id=810 means: join Class 9 starting from their session on 2025-12-15
INSERT INTO student_request (id, student_id, current_class_id, target_class_id, request_type, target_session_id, status, request_reason, submitted_by, submitted_at, note) VALUES
(7, 80, 6, 9, 'TRANSFER', 810, 'PENDING', 'Need online class due to relocation', 6, '2025-12-08 10:00:00+07', 'Created by AA on behalf of student');

-- SCENARIO 8: Rejected Transfer - same class (student 25 - Nguyen Van Phuc)
-- target_session_id=115 means: attempted to join Class 2 session on 2025-12-10 (invalid - same class)
INSERT INTO student_request (id, student_id, current_class_id, target_class_id, request_type, target_session_id, status, request_reason, submitted_by, submitted_at, decided_by, decided_at, note) VALUES
(8, 25, 2, 2, 'TRANSFER', 115, 'REJECTED', 'Accidentally selected current class', 6, '2025-12-02 09:00:00+07', 6, '2025-12-02 15:00:00+07', 'Rejected - cannot transfer to the same class. Created by AA.');

-- SCENARIO 9: Request created by Academic Affair on behalf (student 26 - Le Thi Diem, waiting confirmation)
INSERT INTO student_request (id, student_id, current_class_id, request_type, target_session_id, status, request_reason, submitted_by, submitted_at, note) VALUES
(9, 26, 2, 'ABSENCE', 118, 'WAITING_CONFIRM', 'Student called to report illness - created on behalf', 6, '2025-12-05 13:00:00+07', 'Created by Academic Affair via phone call');

-- Teacher Request Scenarios
-- SCENARIO 10: Teacher Replacement Request - Approved (Class 1, session 10)
INSERT INTO teacher_request (id, teacher_id, session_id, request_type, replacement_teacher_id, status, request_reason, submitted_by, submitted_at, decided_by, decided_at) VALUES
(1, 1, 10, 'REPLACEMENT', 2, 'APPROVED', 'Family emergency - cannot attend session', 20, '2025-11-15 08:00:00+07', 6, '2025-11-15 10:00:00+07');

-- SCENARIO 11: Teacher Reschedule Request - Pending (Class 2)
INSERT INTO teacher_request (id, teacher_id, session_id, request_type, new_date, new_time_slot_id, new_resource_id, status, request_reason, submitted_by, submitted_at) VALUES
(2, 2, 112, 'RESCHEDULE', '2025-12-20', 5, 2, 'PENDING', 'Conference attendance - propose rescheduling to evening slot', 21, '2025-12-05 09:00:00+07');

-- SCENARIO 13: Rejected Teacher Request
INSERT INTO teacher_request (id, teacher_id, session_id, request_type, status, request_reason, submitted_by, submitted_at, decided_by, decided_at) VALUES
(3, 1, 15, 'REPLACEMENT', 'REJECTED', 'Personal reason - insufficient notice', 20, '2025-11-20 08:00:00+07', 6, '2025-11-20 10:00:00+07');

-- SCENARIO 14: Pending Replacement Request (Teacher 3)
INSERT INTO teacher_request (id, teacher_id, session_id, request_type, status, request_reason, submitted_by, submitted_at) VALUES
(4, 3, 210, 'REPLACEMENT', 'PENDING', 'Medical appointment scheduled - need substitute teacher for this session', 22, '2025-12-08 14:30:00+07');

-- SCENARIO 15: Pending Reschedule Request (Teacher 4)
INSERT INTO teacher_request (id, teacher_id, session_id, request_type, new_date, new_time_slot_id, new_resource_id, status, request_reason, submitted_by, submitted_at) VALUES
(5, 4, 305, 'RESCHEDULE', '2025-12-20', 3, 1, 'PENDING', 'Personal commitment conflicts with original schedule - request to move to Friday evening', 23, '2025-12-09 09:15:00+07');

-- SCENARIO 16: Waiting Confirm Replacement Request (Teacher 7)
INSERT INTO teacher_request (id, teacher_id, session_id, request_type, replacement_teacher_id, status, request_reason, submitted_by, submitted_at, decided_by, decided_at) VALUES
(6, 7, 615, 'REPLACEMENT', 8, 'WAITING_CONFIRM', 'Family event - cannot attend session, replacement teacher assigned', 26, '2025-12-06 10:00:00+07', 6, '2025-12-06 14:00:00+07');

-- SCENARIO 17: Pending Replacement Request (Teacher 8)
INSERT INTO teacher_request (id, teacher_id, session_id, request_type, status, request_reason, submitted_by, submitted_at) VALUES
(7, 8, 720, 'REPLACEMENT', 'PENDING', 'Unexpected travel required - need someone to cover this session', 27, '2025-12-10 08:45:00+07');

-- Assessments for Class 1 (HN-IELTS-F1 - Early, ONGOING)
INSERT INTO assessment (id, class_id, subject_assessment_id, scheduled_date, actual_date, created_at, updated_at) VALUES
(1, 1, 1, '2025-11-24 08:00:00+07', '2025-11-24 08:00:00+07', '2025-11-17 08:00:00+07', '2025-11-24 08:00:00+07'), -- Listening Quiz - completed
(2, 1, 2, '2025-11-28 08:00:00+07', '2025-11-28 08:00:00+07', '2025-11-17 08:00:00+07', '2025-11-28 08:00:00+07'), -- Speaking Quiz - completed
(3, 1, 3, '2025-12-08 08:00:00+07', '2025-12-08 08:00:00+07', '2025-11-17 08:00:00+07', '2025-12-08 08:00:00+07'), -- Midterm - completed
(4, 1, 4, '2026-01-06 08:00:00+07', NULL, '2025-11-17 08:00:00+07', '2025-11-17 08:00:00+07'); -- Final - scheduled

-- Assessments for Class 2 (HN-IELTS-F2 - Parallel A, ONGOING)
INSERT INTO assessment (id, class_id, subject_assessment_id, scheduled_date, actual_date, created_at, updated_at) VALUES
(5, 2, 1, '2025-12-08 08:00:00+07', '2025-12-08 08:00:00+07', '2025-12-01 08:00:00+07', '2025-12-08 08:00:00+07'), -- Listening Quiz - today
(6, 2, 2, '2025-12-12 08:00:00+07', NULL, '2025-12-01 08:00:00+07', '2025-12-01 08:00:00+07'), -- Speaking Quiz - scheduled
(7, 2, 3, '2025-12-22 08:00:00+07', NULL, '2025-12-01 08:00:00+07', '2025-12-01 08:00:00+07'), -- Midterm - scheduled
(8, 2, 4, '2026-01-20 08:00:00+07', NULL, '2025-12-01 08:00:00+07', '2025-12-01 08:00:00+07'); -- Final - scheduled

-- Assessments for Class 6 (HN-IELTS-I1 - Intermediate Early, ONGOING)
INSERT INTO assessment (id, class_id, subject_assessment_id, scheduled_date, actual_date, created_at, updated_at) VALUES
(9, 6, 5, '2025-11-25 08:00:00+07', '2025-11-25 08:00:00+07', '2025-11-17 08:00:00+07', '2025-11-25 08:00:00+07'), -- Reading Quiz - completed
(10, 6, 6, '2025-11-29 08:00:00+07', '2025-11-29 08:00:00+07', '2025-11-17 08:00:00+07', '2025-11-29 08:00:00+07'), -- Writing Assignment - completed
(11, 6, 7, '2025-12-09 08:00:00+07', NULL, '2025-11-17 08:00:00+07', '2025-11-17 08:00:00+07'), -- Midterm - scheduled
(12, 6, 8, '2026-01-07 08:00:00+07', NULL, '2025-11-17 08:00:00+07', '2025-11-17 08:00:00+07'); -- Final - scheduled

-- Assessments for Class 7 (HN-IELTS-I2 - Intermediate Parallel A, ONGOING)
INSERT INTO assessment (id, class_id, subject_assessment_id, scheduled_date, actual_date, created_at, updated_at) VALUES
(13, 7, 5, '2025-12-09 08:00:00+07', NULL, '2025-12-02 08:00:00+07', '2025-12-02 08:00:00+07'), -- Reading Quiz - scheduled
(14, 7, 6, '2025-12-13 08:00:00+07', NULL, '2025-12-02 08:00:00+07', '2025-12-02 08:00:00+07'), -- Writing Assignment - scheduled
(15, 7, 7, '2025-12-23 08:00:00+07', NULL, '2025-12-02 08:00:00+07', '2025-12-02 08:00:00+07'), -- Midterm - scheduled
(16, 7, 8, '2026-01-21 08:00:00+07', NULL, '2025-12-02 08:00:00+07', '2025-12-02 08:00:00+07'); -- Final - scheduled

-- Scores for Class 1, Listening Quiz (assessment_id = 1)
INSERT INTO score (assessment_id, student_id, score, feedback, graded_by, graded_at, created_at, updated_at)
SELECT 1, e.student_id, 
    14 + floor(random() * 7)::int, -- Score between 14 and 20
    'Good listening comprehension.', 
    1, -- Graded by Teacher 1
    '2025-11-24 14:00:00+07',
    '2025-11-24 14:00:00+07',
    '2025-11-24 14:00:00+07'
FROM enrollment e WHERE e.class_id = 1;

-- Scores for Class 1, Speaking Quiz (assessment_id = 2)
INSERT INTO score (assessment_id, student_id, score, feedback, graded_by, graded_at, created_at, updated_at)
SELECT 2, e.student_id, 
    13 + floor(random() * 8)::int, -- Score between 13 and 20
    'Clear pronunciation, keep practicing.', 
    1, 
    '2025-11-28 14:00:00+07',
    '2025-11-28 14:00:00+07',
    '2025-11-28 14:00:00+07'
FROM enrollment e WHERE e.class_id = 1;

-- Scores for Class 1, Midterm (assessment_id = 3)
INSERT INTO score (assessment_id, student_id, score, feedback, graded_by, graded_at, created_at, updated_at)
SELECT 3, e.student_id, 
    60 + floor(random() * 35)::int, -- Score between 60 and 94
    'Good midterm performance.', 
    1, 
    '2025-12-08 16:00:00+07',
    '2025-12-08 16:00:00+07',
    '2025-12-08 16:00:00+07'
FROM enrollment e WHERE e.class_id = 1;

-- Scores for Class 2, Listening Quiz (assessment_id = 5)
INSERT INTO score (assessment_id, student_id, score, feedback, graded_by, graded_at, created_at, updated_at)
SELECT 5, e.student_id, 
    15 + floor(random() * 6)::int, -- Score between 15 and 20
    'Excellent listening skills.', 
    2, -- Graded by Teacher 2
    '2025-12-08 14:00:00+07',
    '2025-12-08 14:00:00+07',
    '2025-12-08 14:00:00+07'
FROM enrollment e WHERE e.class_id = 2;

-- Scores for Class 6, Reading Quiz (assessment_id = 9)
INSERT INTO score (assessment_id, student_id, score, feedback, graded_by, graded_at, created_at, updated_at)
SELECT 9, e.student_id, 
    14 + floor(random() * 7)::int, -- Score between 14 and 20
    'Good reading comprehension.', 
    6, -- Graded by Teacher 6
    '2025-11-25 14:00:00+07',
    '2025-11-25 14:00:00+07',
    '2025-11-25 14:00:00+07'
FROM enrollment e WHERE e.class_id = 6;

-- Scores for Class 6, Writing Assignment (assessment_id = 10)
INSERT INTO score (assessment_id, student_id, score, feedback, graded_by, graded_at, created_at, updated_at)
SELECT 10, e.student_id, 
    12 + floor(random() * 9)::int, -- Score between 12 and 20
    'Work on essay structure.', 
    6, 
    '2025-11-29 14:00:00+07',
    '2025-11-29 14:00:00+07',
    '2025-11-29 14:00:00+07'
FROM enrollment e WHERE e.class_id = 6;

-- Class 1 (Foundation Early): Positive feedback from students 1-8 (Nguyen Van An -> Do Thi Huong)
INSERT INTO student_feedback (id, student_id, class_id, phase_id, is_feedback, submitted_at, response, created_at, updated_at)
SELECT 100 + s.id, s.id, 1, 1, true, '2025-12-01 10:00:00+07', 'Học viên rất hài lòng với chất lượng giảng dạy.', NOW(), NOW() FROM generate_series(1, 8) AS s(id);

INSERT INTO student_feedback_response (feedback_id, question_id, rating, created_at, updated_at)
SELECT f.id, q.id, 5, NOW(), NOW()
FROM student_feedback f CROSS JOIN feedback_question q WHERE f.class_id = 1 AND q.status = 'ACTIVE';

-- Class 6 (Intermediate Early): Mixed feedback from students 76-83 (Le Thi Cuc -> Tran Van Bach)
INSERT INTO student_feedback (id, student_id, class_id, phase_id, is_feedback, submitted_at, response, created_at, updated_at)
SELECT 200 + s.id, 75 + s.id, 6, 3, true, '2025-12-02 10:00:00+07', 'Cần cải thiện tốc độ giảng dạy.', NOW(), NOW() FROM generate_series(1, 8) AS s(id);

INSERT INTO student_feedback_response (feedback_id, question_id, rating, created_at, updated_at)
SELECT f.id, q.id, 3 + (f.id % 2), NOW(), NOW() -- Rating 3 or 4
FROM student_feedback f CROSS JOIN feedback_question q WHERE f.class_id = 6 AND q.status = 'ACTIVE';

-- Class 3 (Foundation Parallel B): Average feedback from students 31-38 (Bui Xuan Hoang -> Do Thi Quyen)
INSERT INTO student_feedback (id, student_id, class_id, phase_id, is_feedback, submitted_at, response, created_at, updated_at)
SELECT 300 + s.id, 30 + s.id, 3, 1, true, '2025-12-03 10:00:00+07', 'Giáo viên thường xuyên giải đáp thắc mắc.', NOW(), NOW() FROM generate_series(1, 8) AS s(id);

INSERT INTO student_feedback_response (feedback_id, question_id, rating, created_at, updated_at)
SELECT f.id, q.id, 4, NOW(), NOW()
FROM student_feedback f CROSS JOIN feedback_question q WHERE f.class_id = 3 AND q.status = 'ACTIVE';

-- Class 2 (Foundation Parallel A): Recent feedback from students 16-20 (Vuong Thi Loan -> Nguyen Thi Linh)
INSERT INTO student_feedback (id, student_id, class_id, phase_id, is_feedback, submitted_at, response, created_at, updated_at)
SELECT 400 + s.id, 15 + s.id, 2, 1, true, '2025-12-08 10:00:00+07', 'Rất thích cách cô giáo tổ chức trò chơi.', NOW(), NOW() FROM generate_series(1, 5) AS s(id);

INSERT INTO student_feedback_response (feedback_id, question_id, rating, created_at, updated_at)
SELECT f.id, q.id, 5, NOW(), NOW()
FROM student_feedback f CROSS JOIN feedback_question q WHERE f.class_id = 2 AND q.status = 'ACTIVE';

-- Reset feedback 401 (student 16 - Vuong Thi Loan, class 2, phase 1) to pending for demo
DELETE FROM student_feedback_response WHERE feedback_id = 401;
UPDATE student_feedback
SET is_feedback = false,
    submitted_at = NULL,
    response = NULL,
    updated_at = NOW()
WHERE id = 401;

-- DEMO: Pending feedback for student 1 (Nguyen Van An) - Class 1, Phase 2
-- This simulates cronjob creating feedback waiting for student submission
INSERT INTO student_feedback (id, student_id, class_id, phase_id, is_feedback, submitted_at, response, created_at, updated_at) VALUES
(500, 1, 1, 2, false, NULL, NULL, NOW(), NOW());

-- 1. Classroom Observation (Dự giờ) - Class 2 (Good)
INSERT INTO qa_report (id, class_id, session_id, reported_by, report_type, status, content, created_at, updated_at) VALUES
(1, 2, 105, 10, 'CLASSROOM_OBSERVATION', 'SUBMITTED', 'Giáo viên chuẩn bị bài kỹ lưỡng. Tương tác với học viên tốt. Không khí lớp học sôi nổi. Đề xuất giáo viên chia sẻ kinh nghiệm giảng dạy cho các giáo viên mới.', '2025-12-05 10:00:00+07', '2025-12-05 10:00:00+07'),
(2, 2, 110, 10, 'CLASSROOM_OBSERVATION', 'DRAFT', 'Học viên tham gia đầy đủ. Bài giảng đi đúng trọng tâm. Tiếp tục phát huy.', '2025-12-08 10:00:00+07', '2025-12-08 10:00:00+07');

-- 2. Classroom Observation - Class 4 (Online Foundation)
INSERT INTO qa_report (id, class_id, session_id, reported_by, report_type, status, content, created_at, updated_at) VALUES
(3, 4, 305, 11, 'CLASSROOM_OBSERVATION', 'SUBMITTED', 'Lớp học trực tuyến diễn ra suôn sẻ. Đường truyền ổn định. Học viên tham gia đầy đủ camera và micro.', '2025-12-08 10:00:00+07', '2025-12-08 10:00:00+07'),
(4, 4, 310, 11, 'CLASSROOM_OBSERVATION', 'DRAFT', 'Một số học viên ít tương tác. Cần khuyến khích tham gia nhiều hơn.', '2025-12-09 10:00:00+07', '2025-12-09 10:00:00+07');

-- 3. Student Feedback Analysis (Phân tích phản hồi)
INSERT INTO qa_report (id, class_id, reported_by, report_type, status, content, created_at, updated_at) VALUES
(5, 1, 10, 'STUDENT_FEEDBACK_ANALYSIS', 'SUBMITTED', '100% học viên hài lòng với môn học. Điểm đánh giá trung bình 4.8/5. Khen thưởng giáo viên.', '2025-12-05 10:00:00+07', '2025-12-05 10:00:00+07'),
(6, 6, 11, 'STUDENT_FEEDBACK_ANALYSIS', 'SUBMITTED', 'Phản hồi trái chiều. Một số học viên phàn nàn về tốc độ giảng dạy. Trao đổi với giáo viên để điều chỉnh tốc độ phù hợp với trình độ học viên.', '2025-12-06 10:00:00+07', '2025-12-06 10:00:00+07'),
(7, 3, 11, 'STUDENT_FEEDBACK_ANALYSIS', 'SUBMITTED', 'Phản hồi tích cực về nội dung bài giảng. Đề xuất tiếp tục phương pháp giảng dạy hiện tại.', '2025-12-07 10:00:00+07', '2025-12-07 10:00:00+07');

-- 4. CLO Achievement Analysis (Đánh giá CLO)
INSERT INTO qa_report (id, class_id, phase_id, reported_by, report_type, status, content, created_at, updated_at) VALUES
(8, 1, 1, 10, 'CLO_ACHIEVEMENT_ANALYSIS', 'SUBMITTED', 'Học viên đạt 90% chuẩn đầu ra Phase 1. Cho phép chuyển sang Phase 2.', '2025-12-05 10:00:00+07', '2025-12-05 10:00:00+07'),
(9, 6, 3, 11, 'CLO_ACHIEVEMENT_ANALYSIS', 'SUBMITTED', '85% học viên đạt chuẩn đầu ra. Kỹ năng Viết cần được tăng cường thêm.', '2025-12-06 10:00:00+07', '2025-12-06 10:00:00+07');

-- 5. Attendance & Engagement Review (Đánh giá chuyên cần)
INSERT INTO qa_report (id, class_id, reported_by, report_type, status, content, created_at, updated_at) VALUES
(10, 7, 11, 'ATTENDANCE_ENGAGEMENT_REVIEW', 'SUBMITTED', 'Tỷ lệ chuyên cần cao (>90%). Học viên tham gia tích cực. Tiếp tục duy trì.', '2025-12-08 10:00:00+07', '2025-12-08 10:00:00+07'),
(11, 2, 10, 'ATTENDANCE_ENGAGEMENT_REVIEW', 'SUBMITTED', 'Tỷ lệ chuyên cần cao (>95%). Tiếp tục duy trì.', '2025-12-07 10:00:00+07', '2025-12-07 10:00:00+07');

-- 6. Teaching Quality Assessment (Đánh giá chất lượng giảng dạy)
INSERT INTO qa_report (id, class_id, reported_by, report_type, status, content, created_at, updated_at) VALUES
(12, 1, 10, 'TEACHING_QUALITY_ASSESSMENT', 'SUBMITTED', 'Giáo viên có chuyên môn vững, phương pháp sư phạm tốt. Đề xuất tăng lương hoặc thăng cấp bậc.', '2025-12-05 10:00:00+07', '2025-12-05 10:00:00+07'),
(13, 8, 10, 'TEACHING_QUALITY_ASSESSMENT', 'DRAFT', 'Giáo viên cần cải thiện kỹ năng quản lý lớp học online. Đề xuất tham gia khóa đào tạo.', '2025-12-09 10:00:00+07', '2025-12-09 10:00:00+07');

-- 7. Phase Review (Đánh giá giai đoạn)
INSERT INTO qa_report (id, class_id, phase_id, reported_by, report_type, status, content, created_at, updated_at) VALUES
(14, 2, 1, 10, 'PHASE_REVIEW', 'SUBMITTED', 'Hoàn thành Phase 1 đúng tiến độ. Kết quả kiểm tra giữa kỳ khả quan. Chuẩn bị tài liệu cho Phase 2.', '2025-12-08 10:00:00+07', '2025-12-08 10:00:00+07'),
(15, 9, 3, 10, 'PHASE_REVIEW', 'DRAFT', 'Lớp online cần tăng cường hoạt động tương tác. Đề xuất bổ sung thêm các bài tập nhóm.', '2025-12-09 10:00:00+07', '2025-12-09 10:00:00+07');

-- 8. Additional Reports
INSERT INTO qa_report (id, class_id, reported_by, report_type, status, content, created_at, updated_at) VALUES
(16, 5, 11, 'CLASSROOM_OBSERVATION', 'SUBMITTED', 'Lớp SCHEDULED chưa bắt đầu. Lên kế hoạch dự giờ tuần đầu tiên.', '2025-12-10 10:00:00+07', '2025-12-10 10:00:00+07'),
(17, 10, 11, 'ATTENDANCE_ENGAGEMENT_REVIEW', 'SUBMITTED', 'Lớp SCHEDULED. Chuẩn bị theo dõi chuyên cần ngay từ buổi đầu.', '2025-12-10 10:00:00+07', '2025-12-10 10:00:00+07'),
(18, 3, 10, 'CLO_ACHIEVEMENT_ANALYSIS', 'SUBMITTED', 'Kỹ năng Nói của học viên tốt. Tiếp tục duy trì phương pháp giảng dạy.', '2025-12-07 10:00:00+07', '2025-12-07 10:00:00+07'),
(19, 6, 11, 'TEACHING_QUALITY_ASSESSMENT', 'SUBMITTED', 'Giáo viên có kinh nghiệm. Bài giảng chất lượng cao.', '2025-12-08 10:00:00+07', '2025-12-08 10:00:00+07'),
(20, 4, 10, 'PHASE_REVIEW', 'DRAFT', 'Lớp online đang đi đúng tiến độ. Đề xuất duy trì chất lượng kỹ thuật.', '2025-12-09 10:00:00+07', '2025-12-09 10:00:00+07');

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
