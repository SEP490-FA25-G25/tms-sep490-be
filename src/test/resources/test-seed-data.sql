-- ========== SIMPLIFIED TEST SEED DATA ==========
-- This file contains minimal data required for integration tests
-- It avoids complex FK dependencies present in production seed-data.sql

-- ========== TIER 1: CORE LOOKUP DATA ==========

-- Center
INSERT INTO center (id, code, name, description, phone, email, address, created_at, updated_at) VALUES
(1, 'CENTER-01', 'Main Center', 'Main Education Center', '0901234567', 'center@example.com', 'Hanoi, Vietnam', NOW(), NOW());

-- Branches
INSERT INTO branch (id, center_id, code, name, address, phone, email, status, created_at, updated_at) VALUES
(1, 1, 'HN01', 'Ha Noi Main Branch', '123 Test Street, Hanoi', '0901234567', 'hn01@example.com', 'ACTIVE', NOW(), NOW()),
(2, 1, 'HCM01', 'Ho Chi Minh Branch', '456 Test Road, HCMC', '0909876543', 'hcm01@example.com', 'ACTIVE', NOW(), NOW());

-- Roles
INSERT INTO role (id, code, name) VALUES
(1, 'CENTER_HEAD', 'Center Head'),
(2, 'ACADEMIC_AFFAIR', 'Academic Affairs'),
(3, 'MANAGER', 'Branch Manager'),
(4, 'TEACHER', 'Teacher'),
(5, 'STUDENT', 'Student');

-- ========== TIER 2: USER ACCOUNTS ==========

-- Admin user (CENTER_HEAD) - password: 'password123'
INSERT INTO user_account (id, email, password_hash, full_name, gender, phone, status, created_at, updated_at) VALUES
(1, 'admin@example.com', '$2a$12$UY1q/qoqZdD8Av38QlRgSuti1mPbSbSSpfpHZdfc589hdNOi.LFOq', 'Admin User', 'MALE', '0901111111', 'ACTIVE', NOW(), NOW());

-- Academic Affairs user - password: 'password123'
INSERT INTO user_account (id, email, password_hash, full_name, gender, phone, status, created_at, updated_at) VALUES
(2, 'aa@example.com', '$2a$12$UY1q/qoqZdD8Av38QlRgSuti1mPbSbSSpfpHZdfc589hdNOi.LFOq', 'Academic Affairs', 'FEMALE', '0902222222', 'ACTIVE', NOW(), NOW());

-- Teacher user - password: 'password123'
INSERT INTO user_account (id, email, password_hash, full_name, gender, phone, status, created_at, updated_at) VALUES
(3, 'teacher@example.com', '$2a$12$UY1q/qoqZdD8Av38QlRgSuti1mPbSbSSpfpHZdfc589hdNOi.LFOq', 'Teacher One', 'MALE', '0903333333', 'ACTIVE', NOW(), NOW());

-- User-Role mappings
INSERT INTO user_role (user_id, role_id) VALUES
(1, 1),  -- Admin is CENTER_HEAD
(2, 2),  -- AA is ACADEMIC_AFFAIR
(3, 4);  -- Teacher is TEACHER

-- User-Branch mappings
INSERT INTO user_branches (user_id, branch_id) VALUES
(1, 1),  -- Admin belongs to HN01
(2, 1),  -- AA belongs to HN01
(3, 1);  -- Teacher belongs to HN01

-- ========== TIER 3: RESOURCES ==========

-- Time Slot Templates
INSERT INTO time_slot_template (id, branch_id, name, start_time, end_time, status, created_at, updated_at) VALUES
(1, 1, 'Morning Slot 1', '08:00', '10:00', 'ACTIVE', NOW(), NOW()),
(2, 1, 'Morning Slot 2', '10:15', '12:15', 'ACTIVE', NOW(), NOW()),
(3, 1, 'Afternoon Slot', '13:30', '15:30', 'ACTIVE', NOW(), NOW()),
(4, 1, 'Evening Slot', '17:30', '19:30', 'ACTIVE', NOW(), NOW());

-- Resources (Rooms and Virtual)
INSERT INTO resource (id, branch_id, resource_type, code, name, capacity, meeting_url, account_email, expiry_date, description, status, created_at, updated_at) VALUES
(1, 1, 'ROOM', 'HN01-R101', 'Room 101', 30, NULL, NULL, NULL, 'Standard classroom', 'ACTIVE', NOW(), NOW()),
(2, 1, 'ROOM', 'HN01-R102', 'Room 102', 25, NULL, NULL, NULL, 'Medium classroom', 'ACTIVE', NOW(), NOW()),
(3, 1, 'ROOM', 'HN01-R103', 'Room 103', 20, NULL, NULL, NULL, 'Small classroom', 'ACTIVE', NOW(), NOW()),
(4, 1, 'VIRTUAL', 'HN01-Z01', 'Zoom Room 1', 100, 'https://zoom.us/j/123456789', 'zoom@example.com', '2025-12-31', 'Virtual classroom', 'ACTIVE', NOW(), NOW()),
(5, 1, 'VIRTUAL', 'HN01-Z02', 'Zoom Room 2', 50, 'https://zoom.us/j/987654321', 'zoom2@example.com', '2025-12-31', 'Small virtual room', 'ACTIVE', NOW(), NOW());

-- ========== TIER 4: CURRICULUM ==========

-- Curriculum
INSERT INTO curriculum (id, code, name, description, language, status, created_by, created_at, updated_at) VALUES
(1, 'IELTS', 'IELTS Preparation', 'International English Language Testing System preparation course', 'English', 'ACTIVE', 1, NOW(), NOW()),
(2, 'TOEIC', 'TOEIC Preparation', 'Test of English for International Communication preparation', 'English', 'ACTIVE', 1, NOW(), NOW());

-- Levels
INSERT INTO level (id, curriculum_id, code, name, sort_order, description, status, created_at, updated_at) VALUES
(1, 1, 'IELTS-5.0', 'IELTS 5.0', 1, 'Target band 5.0', 'ACTIVE', NOW(), NOW()),
(2, 1, 'IELTS-6.0', 'IELTS 6.0', 2, 'Target band 6.0', 'ACTIVE', NOW(), NOW()),
(3, 2, 'TOEIC-500', 'TOEIC 500', 1, 'Target score 500', 'ACTIVE', NOW(), NOW());

-- Reset sequences
SELECT setval('branch_id_seq', (SELECT MAX(id) FROM branch));
SELECT setval('role_id_seq', (SELECT MAX(id) FROM role));
SELECT setval('user_account_id_seq', (SELECT MAX(id) FROM user_account));
SELECT setval('time_slot_template_id_seq', (SELECT MAX(id) FROM time_slot_template));
SELECT setval('resource_id_seq', (SELECT MAX(id) FROM resource));
SELECT setval('curriculum_id_seq', (SELECT MAX(id) FROM curriculum));
SELECT setval('level_id_seq', (SELECT MAX(id) FROM level));
