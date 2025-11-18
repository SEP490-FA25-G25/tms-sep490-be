-- =========================================
-- EMS-SEP490-BE: Complete Database Schema (PostgreSQL 16)
-- =========================================
-- 1) Drop tables (cascade)
-- 2) Create enum types
-- 3) Create tables (đúng thứ tự phụ thuộc)
-- 4) Create indexes
-- 5) Comments + notice
-- =========================================

-- ========== SECTION 1: DROP EXISTING TABLES ==========
DROP TABLE IF EXISTS teacher_request CASCADE;
DROP TABLE IF EXISTS student_request CASCADE;
DROP TABLE IF EXISTS qa_report CASCADE;
DROP TABLE IF EXISTS student_feedback CASCADE;
DROP TABLE IF EXISTS score CASCADE;
DROP TABLE IF EXISTS assessment CASCADE;
DROP TABLE IF EXISTS course_assessment_clo_mapping CASCADE;
DROP TABLE IF EXISTS course_assessment CASCADE;
DROP TABLE IF EXISTS teacher_availability CASCADE;
DROP TABLE IF EXISTS student_session CASCADE;
DROP TABLE IF EXISTS enrollment CASCADE;
DROP TABLE IF EXISTS teaching_slot CASCADE;
DROP TABLE IF EXISTS session_resource CASCADE;
DROP TABLE IF EXISTS session CASCADE;
DROP TABLE IF EXISTS course_session_clo_mapping CASCADE;
DROP TABLE IF EXISTS plo_clo_mapping CASCADE;
DROP TABLE IF EXISTS clo CASCADE;
DROP TABLE IF EXISTS plo CASCADE;
DROP TABLE IF EXISTS course_material CASCADE;
DROP TABLE IF EXISTS course_session CASCADE;
DROP TABLE IF EXISTS course_phase CASCADE;
DROP TABLE IF EXISTS "class" CASCADE;
DROP TABLE IF EXISTS teacher_skill CASCADE;
DROP TABLE IF EXISTS student CASCADE;
DROP TABLE IF EXISTS teacher CASCADE;
DROP TABLE IF EXISTS user_branches CASCADE;
DROP TABLE IF EXISTS user_role CASCADE;
DROP TABLE IF EXISTS resource CASCADE;
DROP TABLE IF EXISTS time_slot_template CASCADE;
DROP TABLE IF EXISTS course CASCADE;
DROP TABLE IF EXISTS level CASCADE;
DROP TABLE IF EXISTS subject CASCADE;
DROP TABLE IF EXISTS branch CASCADE;
DROP TABLE IF EXISTS center CASCADE;
DROP TABLE IF EXISTS role CASCADE;
DROP TABLE IF EXISTS user_account CASCADE;
DROP TABLE IF EXISTS replacement_skill_assessment CASCADE;
DROP TABLE IF EXISTS feedback_question CASCADE;
DROP TABLE IF EXISTS student_feedback_response CASCADE;

-- Drop existing enum types (to ensure clean recreation)
DROP TYPE IF EXISTS session_status_enum CASCADE;
DROP TYPE IF EXISTS session_type_enum CASCADE;
DROP TYPE IF EXISTS attendance_status_enum CASCADE;
DROP TYPE IF EXISTS enrollment_status_enum CASCADE;
DROP TYPE IF EXISTS request_status_enum CASCADE;
DROP TYPE IF EXISTS teacher_request_type_enum CASCADE;
DROP TYPE IF EXISTS student_request_type_enum CASCADE;
DROP TYPE IF EXISTS resource_type_enum CASCADE;
DROP TYPE IF EXISTS modality_enum CASCADE;
DROP TYPE IF EXISTS skill_enum CASCADE;
DROP TYPE IF EXISTS branch_status_enum CASCADE;
DROP TYPE IF EXISTS class_status_enum CASCADE;
DROP TYPE IF EXISTS subject_status_enum CASCADE;
DROP TYPE IF EXISTS assessment_kind_enum CASCADE;
DROP TYPE IF EXISTS teaching_slot_status_enum CASCADE;
DROP TYPE IF EXISTS homework_status_enum CASCADE;
DROP TYPE IF EXISTS course_status_enum CASCADE;
DROP TYPE IF EXISTS approval_status_enum CASCADE;
DROP TYPE IF EXISTS material_type_enum CASCADE;
DROP TYPE IF EXISTS mapping_status_enum CASCADE;
DROP TYPE IF EXISTS gender_enum CASCADE;
DROP TYPE IF EXISTS user_status_enum CASCADE;

-- ========== SECTION 2: REFACTORED - NO ENUM TYPES ==========
-- BEST PRACTICE: Using VARCHAR with @Enumerated(EnumType.STRING)
-- - Better Hibernate compatibility
-- - Easier enum value management (no ALTER TYPE needed)
-- - Database independence
-- - Human-readable in SQL queries
-- 
-- All enum validation is handled by:
-- 1. CHECK constraints on columns (database level)
-- 2. @Enumerated(EnumType.STRING) in entities (application level)
-- ========== SECTION 3: TABLES (ĐÚNG THỨ TỰ) ==========

-- TIER 1: Independent
CREATE TABLE center (
  id BIGSERIAL PRIMARY KEY,
  code VARCHAR(255) NOT NULL UNIQUE,
  name VARCHAR(255) NOT NULL,
  description TEXT,
  phone VARCHAR(50),
  email VARCHAR(255),
  address TEXT,
  created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE role (
  id BIGSERIAL PRIMARY KEY,
  code VARCHAR(50) NOT NULL UNIQUE,
  name VARCHAR(255) NOT NULL
);

CREATE TABLE user_account (
  id BIGSERIAL PRIMARY KEY,
  email VARCHAR(255) UNIQUE,
  phone VARCHAR(50),
  facebook_url VARCHAR(500),
  full_name VARCHAR(255) NOT NULL,
  gender VARCHAR(20) NOT NULL DEFAULT 'MALE',
  dob DATE,
  address TEXT,
  password_hash VARCHAR(255) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  last_login_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT chk_user_gender CHECK (gender IN ('MALE', 'FEMALE', 'OTHER')),
  CONSTRAINT chk_user_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

-- TIER 2
CREATE TABLE branch (
  id BIGSERIAL PRIMARY KEY,
  center_id BIGINT NOT NULL,
  code VARCHAR(50) NOT NULL,
  name VARCHAR(255) NOT NULL,
  address TEXT,
  phone VARCHAR(50),
  email VARCHAR(255),
  district VARCHAR(255),
  city VARCHAR(255),
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  opening_date DATE,
  created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_branch_center FOREIGN KEY(center_id) REFERENCES center(id) ON DELETE CASCADE,
  CONSTRAINT uq_branch_center_code UNIQUE(center_id,code),
  CONSTRAINT chk_branch_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'CLOSED', 'PLANNED'))
);

CREATE TABLE subject (
  id BIGSERIAL PRIMARY KEY,
  code VARCHAR(50) NOT NULL UNIQUE,
  name VARCHAR(255) NOT NULL,
  description TEXT,
  status VARCHAR(20) NOT NULL DEFAULT 'DRAFT', 
  -- draft -> tạo course với subject đang draft -> submit -> manager duyệt -> active
  -- inactive khi không còn một class đang học với subject đó
  created_by BIGINT,
  created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_subject_created_by FOREIGN KEY(created_by) REFERENCES user_account(id) ON DELETE SET NULL,
  CONSTRAINT chk_subject_status CHECK (status IN ('DRAFT', 'ACTIVE', 'INACTIVE'))
);

CREATE TABLE time_slot_template ( -- center head điều chỉnh thời gian học của branch
  id BIGSERIAL PRIMARY KEY,
  branch_id BIGINT NOT NULL,
  name VARCHAR(255) NOT NULL,
  start_time TIME NOT NULL,
  end_time TIME NOT NULL,
  created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_timeslot_branch FOREIGN KEY(branch_id) REFERENCES branch(id) ON DELETE CASCADE
);

CREATE TABLE resource ( -- center head điều chỉnh tài sản của branch
  id BIGSERIAL PRIMARY KEY,
  branch_id BIGINT NOT NULL,
  resource_type VARCHAR(20) NOT NULL,
  code VARCHAR(50) NOT NULL UNIQUE, -- theo quy chuẩn code chi nhánh-loại resource-số phòng
  name VARCHAR(255) NOT NULL,
  description TEXT,
  capacity INTEGER, -- 20
  capacity_override INTEGER, -- override 23
  equipment TEXT,
  meeting_url VARCHAR(500), -- zoom url cho online class (personal meeting thì mới cố định)
  meeting_id VARCHAR(255), -- zoom id cho online class 
  meeting_passcode VARCHAR(255), -- zoom passcode cho online class
  account_email VARCHAR(255), -- email của zoom account cho online class
  account_password VARCHAR(255), -- password của zoom account cho online class
  license_type VARCHAR(100), -- premium/free cho online class
  expiry_date DATE, -- hết hạn của license cho online class (tính trước)
  renewal_date DATE, -- renewal date ví dụ hết hạn nửa tháng sau mới cần gia hạn lại
  created_by BIGINT,
  created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_resource_branch FOREIGN KEY(branch_id) REFERENCES branch(id) ON DELETE CASCADE,
  CONSTRAINT fk_resource_created_by FOREIGN KEY(created_by) REFERENCES user_account(id) ON DELETE SET NULL,
  CONSTRAINT chk_resource_type CHECK (resource_type IN ('ROOM', 'VIRTUAL'))
);

CREATE TABLE user_role (
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  PRIMARY KEY(user_id,role_id),
  CONSTRAINT fk_userrole_user FOREIGN KEY(user_id) REFERENCES user_account(id) ON DELETE CASCADE,
  CONSTRAINT fk_userrole_role FOREIGN KEY(role_id) REFERENCES role(id) ON DELETE CASCADE
);

CREATE TABLE user_branches (
  user_id BIGINT NOT NULL,
  branch_id BIGINT NOT NULL,
  assigned_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
  assigned_by BIGINT,
  PRIMARY KEY(user_id,branch_id),
  CONSTRAINT fk_userbranch_user FOREIGN KEY(user_id) REFERENCES user_account(id) ON DELETE CASCADE,
  CONSTRAINT fk_userbranch_branch FOREIGN KEY(branch_id) REFERENCES branch(id) ON DELETE CASCADE,
  CONSTRAINT fk_userbranch_assigned_by FOREIGN KEY(assigned_by) REFERENCES user_account(id) ON DELETE SET NULL
);

CREATE TABLE teacher (
  id BIGSERIAL PRIMARY KEY,
  user_account_id BIGINT NOT NULL UNIQUE,
  employee_code VARCHAR(50) UNIQUE,
  hire_date DATE,
  contract_type VARCHAR(100), -- full-time/part-time/internship
  note TEXT, -- ví dụ teacher có thể note lại có các loại chứng chỉ khác ngoài hệ thống
  created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
  CONSTRAINT fk_teacher_user_account FOREIGN KEY(user_account_id) REFERENCES user_account(id) ON DELETE CASCADE
);

CREATE TABLE student (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL UNIQUE,
  student_code VARCHAR(50) UNIQUE,
  created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
  CONSTRAINT fk_student_user FOREIGN KEY(user_id) REFERENCES user_account(id) ON DELETE CASCADE
);

-- TIER 3: Curriculum
CREATE TABLE level ( 
  id BIGSERIAL PRIMARY KEY,
  subject_id BIGINT NOT NULL,
  code VARCHAR(50) NOT NULL,
  name VARCHAR(255) NOT NULL,
  expected_duration_hours INTEGER, -- ví dụ: 80 giờ cho HSK1, 100 giờ cho HSK2, ...
  sort_order INTEGER, -- sắp xếp theo thứ tự để hiển thị trong UI (ví dụ: A1, A2, B1, B2, ...)
  description TEXT, -- ví dụ: "Beginner (A1)", "Intermediate (B1)", "Advanced (C1)", ...
  created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
  CONSTRAINT fk_level_subject FOREIGN KEY(subject_id) REFERENCES subject(id) ON DELETE CASCADE,
  CONSTRAINT uq_level_subject_code UNIQUE(subject_id,code)
);

CREATE TABLE replacement_skill_assessment (
  id BIGSERIAL PRIMARY KEY,
  student_id BIGINT NOT NULL,
  skill VARCHAR(20) NOT NULL,  -- Enum: GENERAL, READING, WRITING, SPEAKING, LISTENING, VOCABULARY, GRAMMAR, KANJI
  level_id BIGINT,  -- Link đến bảng level (ví dụ: A1, A2, B1, B2, N5, N4, N3, N2, N1...)
  raw_score DECIMAL(10,2),  -- Raw score from assessment (e.g., 32 out of 40 questions)
  scaled_score DECIMAL(10,2),  -- Converted score (e.g., 7.5 IELTS band, 750 TOEIC)
  score_scale VARCHAR(100),  -- Score scale description (e.g., "0-9", "0-990", "N1-N5")
  assessment_category VARCHAR(50),  -- Assessment category: PLACEMENT, MOCK, OFFICIAL, PRACTICE
  assessment_date DATE NOT NULL,  -- Ngày đánh giá
  assessment_type VARCHAR(100),  -- Loại đánh giá: 'placement_test', 'ielts', 'toeic', 'jlpt', 'internal_exam', 'self_assessment'
  note TEXT,  -- Ghi chú thêm
  assessed_by BIGINT,  -- Người đánh giá (teacher/academic staff)
  created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
  CONSTRAINT fk_student_skill_student FOREIGN KEY(student_id) REFERENCES student(id) ON DELETE CASCADE,
  CONSTRAINT fk_student_skill_level FOREIGN KEY(level_id) REFERENCES level(id) ON DELETE SET NULL,
  CONSTRAINT fk_student_skill_assessed_by FOREIGN KEY(assessed_by) REFERENCES user_account(id) ON DELETE SET NULL,
  CONSTRAINT uq_student_skill_assessment UNIQUE(student_id, skill, assessment_date, assessment_category),
  CONSTRAINT chk_replacement_skill CHECK (skill IN ('GENERAL', 'READING', 'WRITING', 'SPEAKING', 'LISTENING', 'VOCABULARY', 'GRAMMAR', 'KANJI'))
);

CREATE TABLE course (
  id BIGSERIAL PRIMARY KEY,
  subject_id BIGINT NOT NULL,
  level_id BIGINT,
  logical_course_code VARCHAR(100), -- ví dụ: HSK3-2024, IELTS-Foundation-2024, TOEFL-Intermediate-2024, etc.
  version INTEGER, -- ví dụ: 1, 2, 3, ...
  code VARCHAR(100) NOT NULL UNIQUE, -- ví dụ: HSK3-2024-v1, IELTS-Foundation-2024-v2, TOEFL-Intermediate-2024-v1, etc.
  name VARCHAR(255) NOT NULL, -- ví dụ: HSK3 - 2024, IELTS Foundation - 2024, TOEFL Intermediate - 2024, etc.
  description TEXT,
  score_scale VARCHAR(100), -- ví dụ: IELTS: 0-9, TOEFL: 0-120, HSK: 0-600, etc.
  total_hours INTEGER, -- ví dụ: 120 hours cho HSK3
  duration_weeks INTEGER, -- ví dụ: 4 tuần cho HSK3
  session_per_week INTEGER, -- ví dụ: 3 session/week cho HSK3
  hours_per_session DECIMAL(5,2), -- ví dụ: 2.5 hours/session cho HSK3
  prerequisites TEXT,-- ví dụ: HSK2 or equivalent
  target_audience TEXT,-- ví dụ: learners targeting HSK3 certification
  teaching_methods TEXT, -- ví dụ: task-based learning, drills, mock tests, feedback
  effective_date DATE, -- ngày hiệu lực của course (ví dụ: ngày bắt đầu mở course) -> cronjob vào ngày cập nhật status là active
  status VARCHAR(20) NOT NULL DEFAULT 'DRAFT', 
  -- draft -> tạo course với subject đang draft -> submit -> manager duyệt -> approved -> không cập nhật status
  approval_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  -- pending -> subject leader submit -> manager duyệt -> approved -> status vẫn là draft
  -- rejected -> subject leader submit -> manager duyệt -> rejected -> status vẫn là draft
  decided_by_manager BIGINT,
  decided_at TIMESTAMPTZ,
  rejection_reason TEXT,
  hash_checksum VARCHAR(255), -- manager fetch ra bản cũ có checksum cũ -> đúng lúc subject leader update -> course đổi checksum -> manager duyêt -> check thấy khác nhau -> cảnh báo manager để refresh lại thông tin mới
  created_by BIGINT,
  created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_course_subject FOREIGN KEY(subject_id) REFERENCES subject(id) ON DELETE CASCADE,
  CONSTRAINT fk_course_level FOREIGN KEY(level_id) REFERENCES level(id) ON DELETE SET NULL,
  CONSTRAINT fk_course_decided_by_manager FOREIGN KEY(decided_by_manager) REFERENCES user_account(id) ON DELETE SET NULL,
  CONSTRAINT fk_course_created_by FOREIGN KEY(created_by) REFERENCES user_account(id) ON DELETE SET NULL,
  CONSTRAINT chk_course_status CHECK (status IN ('DRAFT', 'ACTIVE', 'INACTIVE')),
  CONSTRAINT chk_course_approval_status CHECK (approval_status IN ('PENDING', 'APPROVED', 'REJECTED'))
);

CREATE TABLE course_phase (
  id BIGSERIAL PRIMARY KEY,
  course_id BIGINT NOT NULL,
  phase_number INTEGER NOT NULL, -- ví dụ: Phase 1, Phase 2, Phase 3, ...
  name VARCHAR(255),
  duration_weeks INTEGER,
  learning_focus TEXT, -- kiểu description
  created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
  CONSTRAINT fk_course_phase_course FOREIGN KEY(course_id) REFERENCES course(id) ON DELETE CASCADE,
  CONSTRAINT uq_course_phase_course_number UNIQUE(course_id,phase_number)
);

CREATE TABLE course_session (
  id BIGSERIAL PRIMARY KEY,
  phase_id BIGINT NOT NULL,
  sequence_no INTEGER NOT NULL, -- ví dụ: Session 1, Session 2, Session 3, ...
  topic VARCHAR(500),
  student_task TEXT,
  skill_set VARCHAR(20)[],
  created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
  CONSTRAINT fk_course_session_phase FOREIGN KEY(phase_id) REFERENCES course_phase(id) ON DELETE CASCADE,
  CONSTRAINT uq_course_session_phase_sequence UNIQUE(phase_id,sequence_no)
);

CREATE TABLE course_material (
  id BIGSERIAL PRIMARY KEY,
  course_id BIGINT NOT NULL, -- liên kết đến course để biết tài liệu này thuộc course nào
  phase_id BIGINT, -- liên kết đến phase để biết tài liệu này thuộc phase nào (nếu có)
  course_session_id BIGINT, -- liên kết đến session để biết tài liệu này thuộc session nào (nếu có)
  title VARCHAR(500) NOT NULL, -- tiêu đề tài liệu
  description TEXT,
  material_type VARCHAR(20) NOT NULL, -- loại tài liệu: video, pdf, slide, audio, document, other
  url VARCHAR(1000) NOT NULL, -- link đến tài liệu (có thể là link nội bộ hoặc link bên ngoài)
  uploaded_by BIGINT, -- người upload tài liệu
  uploaded_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
  created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
  CONSTRAINT fk_course_material_course FOREIGN KEY(course_id) REFERENCES course(id) ON DELETE CASCADE,
  CONSTRAINT fk_course_material_phase FOREIGN KEY(phase_id) REFERENCES course_phase(id) ON DELETE CASCADE,
  CONSTRAINT fk_course_material_session FOREIGN KEY(course_session_id) REFERENCES course_session(id) ON DELETE CASCADE,
  CONSTRAINT fk_course_material_uploaded_by FOREIGN KEY(uploaded_by) REFERENCES user_account(id) ON DELETE SET NULL,
  CONSTRAINT chk_material_type CHECK (material_type IN ('VIDEO', 'PDF', 'SLIDE', 'AUDIO', 'DOCUMENT', 'OTHER'))
);

CREATE TABLE plo (
  id BIGSERIAL PRIMARY KEY,
  subject_id BIGINT NOT NULL,
  code VARCHAR(50) NOT NULL,
  description TEXT NOT NULL,
  created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
  CONSTRAINT fk_plo_subject FOREIGN KEY(subject_id) REFERENCES subject(id) ON DELETE CASCADE,
  CONSTRAINT uq_plo_subject_code UNIQUE(subject_id,code)
);

CREATE TABLE clo (
  id BIGSERIAL PRIMARY KEY,
  course_id BIGINT NOT NULL,
  code VARCHAR(50) NOT NULL,
  description TEXT NOT NULL,
  created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
  CONSTRAINT fk_clo_course FOREIGN KEY(course_id) REFERENCES course(id) ON DELETE CASCADE,
  CONSTRAINT uq_clo_course_code UNIQUE(course_id,code)
);

CREATE TABLE plo_clo_mapping (
  plo_id BIGINT NOT NULL,
  clo_id BIGINT NOT NULL,
  status VARCHAR(20) NOT NULL,
  PRIMARY KEY (plo_id,clo_id),
  CONSTRAINT fk_plo_clo_plo FOREIGN KEY(plo_id) REFERENCES plo(id) ON DELETE CASCADE,
  CONSTRAINT fk_plo_clo_clo FOREIGN KEY(clo_id) REFERENCES clo(id) ON DELETE CASCADE,
  CONSTRAINT chk_plo_clo_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE TABLE course_session_clo_mapping (
  course_session_id BIGINT NOT NULL,
  clo_id BIGINT NOT NULL,
  status VARCHAR(20) NOT NULL,
  PRIMARY KEY (course_session_id,clo_id),
  CONSTRAINT fk_course_session_clo_session FOREIGN KEY(course_session_id) REFERENCES course_session(id) ON DELETE CASCADE,
  CONSTRAINT fk_course_session_clo_clo FOREIGN KEY(clo_id) REFERENCES clo(id) ON DELETE CASCADE,
  CONSTRAINT chk_session_clo_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE TABLE course_assessment (
  id BIGSERIAL PRIMARY KEY,
  course_id BIGINT NOT NULL,
  name VARCHAR(255) NOT NULL, -- ví dụ: Quiz 1, Midterm Exam, Final Project, etc.
  kind VARCHAR(20) NOT NULL, -- ví dụ: quiz, midterm, final, assignment, project, oral, practice, other
  duration_minutes INTEGER, -- thời lượng làm bài (nếu có)
  description TEXT,
  skills VARCHAR(20)[] NOT NULL,
  max_score DECIMAL(5,2) NOT NULL,
  note TEXT,
  created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
  CONSTRAINT fk_course_assessment_course FOREIGN KEY(course_id) REFERENCES course(id) ON DELETE CASCADE,
  CONSTRAINT chk_assessment_kind CHECK (kind IN ('QUIZ', 'MIDTERM', 'FINAL', 'ASSIGNMENT', 'PROJECT', 'ORAL', 'PRACTICE', 'OTHER'))
);

CREATE TABLE course_assessment_clo_mapping (
  course_assessment_id BIGINT NOT NULL,
  clo_id BIGINT NOT NULL,
  status VARCHAR(20) NOT NULL,
  PRIMARY KEY(course_assessment_id,clo_id),
  CONSTRAINT fk_course_assessment_clo_assessment FOREIGN KEY(course_assessment_id) REFERENCES course_assessment(id) ON DELETE CASCADE,
  CONSTRAINT fk_course_assessment_clo_clo FOREIGN KEY(clo_id) REFERENCES clo(id) ON DELETE CASCADE,
  CONSTRAINT chk_assessment_clo_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

-- TIER 4: Operations
CREATE TABLE "class" (
  id BIGSERIAL PRIMARY KEY,
  branch_id BIGINT NOT NULL,
  course_id BIGINT NOT NULL,
  code VARCHAR(50) NOT NULL,
  name VARCHAR(255),
  modality VARCHAR(20) NOT NULL, -- offline/online/hybrid
  start_date DATE NOT NULL, -- ngày bắt đầu lớp
  planned_end_date DATE, -- ngày kết thúc dự kiến
  actual_end_date DATE, -- ngày kết thúc thực tế
  schedule_days SMALLINT[], -- mảng các ngày trong tuần (1-7) lớp học (ví dụ: [2,4,6] cho thứ 3,5,7)
  max_capacity integer, -- policy về sức chứa tối đa của lớp
  status VARCHAR(20) NOT NULL DEFAULT 'DRAFT', -- draft -> scheduled -> ongoing -> completed
  approval_status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- pending -> academic affair submit -> centerhead duyệt -> approved/rejected
  created_by BIGINT, -- tạo bởi giáo vụ nào
  submitted_at TIMESTAMPTZ, -- thời gian giáo vụ submit để duyệt
  decided_by BIGINT, -- duyệt bởi centerhead nào
  decided_at TIMESTAMPTZ, -- duyệt lúc nào
  rejection_reason TEXT, -- lý do bị từ chối (nếu có)
  created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_class_branch FOREIGN KEY(branch_id) REFERENCES branch(id) ON DELETE CASCADE,
  CONSTRAINT fk_class_course FOREIGN KEY(course_id) REFERENCES course(id) ON DELETE CASCADE,
  CONSTRAINT fk_class_created_by FOREIGN KEY(created_by) REFERENCES user_account(id) ON DELETE SET NULL,
  CONSTRAINT fk_class_decided_by FOREIGN KEY(decided_by) REFERENCES user_account(id) ON DELETE SET NULL,
  CONSTRAINT uq_class_branch_code UNIQUE(branch_id,code),
  CONSTRAINT chk_class_modality CHECK (modality IN ('OFFLINE', 'ONLINE', 'HYBRID')),
  CONSTRAINT chk_class_status CHECK (status IN ('DRAFT', 'SCHEDULED', 'ONGOING', 'COMPLETED', 'CANCELLED')),
  CONSTRAINT chk_class_approval_status CHECK (approval_status IN ('PENDING', 'APPROVED', 'REJECTED'))
);

CREATE TABLE session (
  id BIGSERIAL PRIMARY KEY,
  class_id BIGINT,
  course_session_id BIGINT,
  time_slot_template_id BIGINT,
  date DATE NOT NULL,
  type VARCHAR(20) NOT NULL DEFAULT 'CLASS', -- class là theo lịch bình thường, other là buổi học đặc biệt khác, teacher_reschedule là lịch dạy của giáo viên (không liên quan đến lớp học)
  status VARCHAR(20) NOT NULL DEFAULT 'PLANNED',
  teacher_note TEXT, -- teacher báo cáo sau buổi học
  created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
  CONSTRAINT fk_session_class FOREIGN KEY(class_id) REFERENCES "class"(id) ON DELETE CASCADE,
  CONSTRAINT fk_session_course_session FOREIGN KEY(course_session_id) REFERENCES course_session(id) ON DELETE SET NULL,
  CONSTRAINT fk_session_time_slot_template FOREIGN KEY(time_slot_template_id) REFERENCES time_slot_template(id) ON DELETE SET NULL,
  CONSTRAINT chk_session_type CHECK (type IN ('CLASS', 'TEACHER_RESCHEDULE')),
  CONSTRAINT chk_session_status CHECK (status IN ('PLANNED', 'CANCELLED', 'DONE'))
);

CREATE TABLE teacher_skill (
  teacher_id BIGINT NOT NULL,
  skill VARCHAR(20) NOT NULL,
  specialization VARCHAR(255), -- ví dụ: TOEFL, IELTS,...
  language VARCHAR(255), -- ví dụ: English, Vietnamese, etc.
  level SMALLINT,
  PRIMARY KEY(teacher_id, skill, specialization),
  CONSTRAINT fk_teacher_skill_teacher FOREIGN KEY(teacher_id) REFERENCES teacher(id) ON DELETE CASCADE,
  CONSTRAINT chk_teacher_skill CHECK (skill IN ('GENERAL', 'READING', 'WRITING', 'SPEAKING', 'LISTENING'))
);

CREATE TABLE teacher_availability (
  teacher_id BIGINT NOT NULL,
  time_slot_template_id BIGINT NOT NULL, -- time slot trong tuần mà teacher có thể dạy
  day_of_week SMALLINT NOT NULL CHECK (day_of_week BETWEEN 1 AND 7),
  effective_date DATE,
  note TEXT,
  created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
  PRIMARY KEY(teacher_id,time_slot_template_id,day_of_week),
  CONSTRAINT fk_teacher_availability_teacher FOREIGN KEY(teacher_id) REFERENCES teacher(id) ON DELETE CASCADE,
  CONSTRAINT fk_teacher_availability_timeslot FOREIGN KEY(time_slot_template_id) REFERENCES time_slot_template(id) ON DELETE CASCADE
);

CREATE TABLE session_resource (
  session_id BIGINT NOT NULL,
  resource_id BIGINT NOT NULL,
  PRIMARY KEY(session_id,resource_id),
  CONSTRAINT fk_session_resource_session FOREIGN KEY(session_id) REFERENCES session(id) ON DELETE CASCADE,
  CONSTRAINT fk_session_resource_resource FOREIGN KEY(resource_id) REFERENCES resource(id) ON DELETE CASCADE
);

CREATE TABLE teaching_slot (
  session_id BIGINT NOT NULL,
  teacher_id BIGINT NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED', -- on_leave - session đó giáo viên nghỉ/substituted - session đó giáo viên khác dạy thay/scheduled - session giáo viên dạy đúng lịch
  PRIMARY KEY(session_id,teacher_id),
  CONSTRAINT fk_teaching_slot_session FOREIGN KEY(session_id) REFERENCES session(id) ON DELETE CASCADE,
  CONSTRAINT fk_teaching_slot_teacher FOREIGN KEY(teacher_id) REFERENCES teacher(id) ON DELETE CASCADE,
  CONSTRAINT chk_teaching_slot_status CHECK (status IN ('SCHEDULED', 'ON_LEAVE', 'SUBSTITUTED'))
);

CREATE TABLE enrollment (
  id BIGSERIAL PRIMARY KEY,
  class_id BIGINT NOT NULL,
  student_id BIGINT NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'ENROLLED',
  enrolled_at TIMESTAMPTZ,
  left_at TIMESTAMPTZ,
  join_session_id BIGINT, -- session_id mà student bắt đầu tham gia lớp
  left_session_id BIGINT, -- session_id mà student rời khỏi lớp
  enrolled_by BIGINT, -- user_account_id của người thực hiện việc ghi danh
  capacity_override BOOLEAN NOT NULL DEFAULT false, -- true nếu enrollment này vượt quá max_capacity
  override_reason TEXT, -- lý do override capacity (required khi capacity_override = true, min 20 chars)
  created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
  CONSTRAINT fk_enrollment_class FOREIGN KEY(class_id) REFERENCES "class"(id) ON DELETE CASCADE,
  CONSTRAINT fk_enrollment_enrolled_by FOREIGN KEY(enrolled_by) REFERENCES user_account(id) ON DELETE SET NULL,
  CONSTRAINT fk_enrollment_student FOREIGN KEY(student_id) REFERENCES student(id) ON DELETE CASCADE,
  CONSTRAINT fk_enrollment_join_session FOREIGN KEY(join_session_id) REFERENCES session(id) ON DELETE SET NULL,
  CONSTRAINT fk_enrollment_left_session FOREIGN KEY(left_session_id) REFERENCES session(id) ON DELETE SET NULL,
  CONSTRAINT chk_enrollment_status CHECK (status IN ('ENROLLED', 'TRANSFERRED', 'DROPPED', 'COMPLETED'))
);

CREATE TABLE student_session (
  student_id BIGINT NOT NULL,
  session_id BIGINT NOT NULL,
  is_makeup BOOLEAN DEFAULT false,
  makeup_session_id BIGINT, -- nếu là buổi học bù thì lưu session bù
  original_session_id BIGINT, -- nếu là buổi học bù thì lưu session gốc
  is_transferred_out BOOLEAN DEFAULT false, -- true nếu học sinh chuyển lớp và không còn tham gia session này
  attendance_status VARCHAR(20) NOT NULL DEFAULT 'PLANNED',
  homework_status VARCHAR(20),
  note TEXT,
  recorded_at TIMESTAMPTZ, -- thời gian ghi nhận trạng thái điểm danh/homework
  created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
  PRIMARY KEY(student_id,session_id),
  CONSTRAINT fk_student_session_makeup FOREIGN KEY(makeup_session_id) REFERENCES session(id) ON DELETE SET NULL,
  CONSTRAINT fk_student_session_original FOREIGN KEY(original_session_id) REFERENCES session(id) ON DELETE SET NULL,
  CONSTRAINT fk_student_session_student FOREIGN KEY(student_id) REFERENCES student(id) ON DELETE CASCADE,
  CONSTRAINT fk_student_session_session FOREIGN KEY(session_id) REFERENCES session(id) ON DELETE CASCADE,
  CONSTRAINT chk_attendance_status CHECK (attendance_status IN ('PLANNED', 'PRESENT', 'ABSENT')),
  CONSTRAINT chk_homework_status CHECK (homework_status IN ('COMPLETED', 'INCOMPLETE', 'NO_HOMEWORK'))
);

-- TIER 5: Assessment & Feedback
CREATE TABLE assessment (
  id BIGSERIAL PRIMARY KEY,
  class_id BIGINT NOT NULL,
  course_assessment_id BIGINT,
  scheduled_date TIMESTAMPTZ NOT NULL,
  actual_date TIMESTAMPTZ,
  created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
  CONSTRAINT fk_assessment_class FOREIGN KEY(class_id) REFERENCES "class"(id) ON DELETE CASCADE,
  CONSTRAINT fk_assessment_course_assessment FOREIGN KEY(course_assessment_id) REFERENCES course_assessment(id) ON DELETE SET NULL
);

CREATE TABLE score (
  id BIGSERIAL PRIMARY KEY,
  assessment_id BIGINT NOT NULL,
  student_id BIGINT NOT NULL,
  score DECIMAL(5,2) NOT NULL,
  feedback TEXT,
  graded_by BIGINT,
  graded_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
  CONSTRAINT fk_score_assessment FOREIGN KEY(assessment_id) REFERENCES assessment(id) ON DELETE CASCADE,
  CONSTRAINT fk_score_student FOREIGN KEY(student_id) REFERENCES student(id) ON DELETE CASCADE,
  CONSTRAINT fk_score_graded_by FOREIGN KEY(graded_by) REFERENCES teacher(id) ON DELETE SET NULL,
  CONSTRAINT uq_score_assessment_student UNIQUE(assessment_id,student_id)
);

CREATE TABLE feedback_question (
  id BIGSERIAL PRIMARY KEY,
  question_text TEXT NOT NULL,
  question_type VARCHAR(100), -- ví dụ: rating, text, multiple_choice, etc.
  options TEXT[], -- nếu là multiple_choice thì lưu các lựa chọn
  display_order INT,
  created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE student_feedback (
  id BIGSERIAL PRIMARY KEY,
  student_id BIGINT NOT NULL,
  class_id BIGINT NOT NULL,
  phase_id BIGINT,
  is_feedback BOOLEAN DEFAULT false,
  submitted_at TIMESTAMPTZ,
  response TEXT,
  created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
  CONSTRAINT fk_student_feedback_student FOREIGN KEY(student_id) REFERENCES student(id) ON DELETE CASCADE,
  CONSTRAINT fk_student_feedback_class FOREIGN KEY(class_id) REFERENCES "class"(id) ON DELETE CASCADE,
  CONSTRAINT fk_student_feedback_phase FOREIGN KEY(phase_id) REFERENCES course_phase(id) ON DELETE SET NULL
);

CREATE TABLE student_feedback_response (
  id BIGSERIAL PRIMARY KEY,
  feedback_id BIGINT NOT NULL,
  question_id BIGINT NOT NULL,
  rating SMALLINT CHECK (rating BETWEEN 1 AND 5),
  created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
  CONSTRAINT fk_feedback_response_feedback FOREIGN KEY(feedback_id) REFERENCES student_feedback(id) ON DELETE CASCADE,
  CONSTRAINT fk_feedback_response_question FOREIGN KEY(question_id) REFERENCES feedback_question(id) ON DELETE CASCADE
);

CREATE TABLE qa_report (
  id BIGSERIAL PRIMARY KEY,
  class_id BIGINT,
  session_id BIGINT,
  phase_id BIGINT,
  reported_by BIGINT,
  report_type VARCHAR(100),
  status VARCHAR(50),
  findings TEXT,
  action_items TEXT,
  created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
  CONSTRAINT fk_qa_report_class FOREIGN KEY(class_id) REFERENCES "class"(id) ON DELETE CASCADE,
  CONSTRAINT fk_qa_report_session FOREIGN KEY(session_id) REFERENCES session(id) ON DELETE CASCADE,
  CONSTRAINT fk_qa_report_phase FOREIGN KEY(phase_id) REFERENCES course_phase(id) ON DELETE SET NULL,
  CONSTRAINT fk_qa_report_reported_by FOREIGN KEY(reported_by) REFERENCES user_account(id) ON DELETE SET NULL
);

-- TIER 6: Requests
CREATE TABLE student_request (
  id BIGSERIAL PRIMARY KEY,
  student_id BIGINT NOT NULL,
  current_class_id BIGINT, -- lớp hiện tại của student
  request_type VARCHAR(20) NOT NULL,
  target_class_id BIGINT, -- lớp muốn chuyển đến (dành cho transfer request)
  target_session_id BIGINT, -- dành buổi gốc mình chọn để học bù hoặc nghỉ
  makeup_session_id BIGINT, -- dành buổi học bù mình muốn học bù
  effective_date DATE, -- ngày có hiệu lực của request (dành cho transfer request)
  effective_session_id BIGINT, -- buổi học có hiệu lực (dành cho transfer request)
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  submitted_at TIMESTAMPTZ,
  submitted_by BIGINT,
  decided_by BIGINT,
  decided_at TIMESTAMPTZ,
  request_reason TEXT, -- của student
  note TEXT, -- note giáo vụ xử lý
  CONSTRAINT fk_student_request_student FOREIGN KEY(student_id) REFERENCES student(id) ON DELETE CASCADE,
  CONSTRAINT fk_student_request_current_class FOREIGN KEY(current_class_id) REFERENCES "class"(id) ON DELETE SET NULL,
  CONSTRAINT fk_student_request_target_class FOREIGN KEY(target_class_id) REFERENCES "class"(id) ON DELETE SET NULL,
  CONSTRAINT fk_student_request_target_session FOREIGN KEY(target_session_id) REFERENCES session(id) ON DELETE SET NULL,
  CONSTRAINT fk_student_request_makeup_session FOREIGN KEY(makeup_session_id) REFERENCES session(id) ON DELETE SET NULL,
  CONSTRAINT fk_student_request_effective_session FOREIGN KEY(effective_session_id) REFERENCES session(id) ON DELETE SET NULL,
  CONSTRAINT fk_student_request_submitted_by FOREIGN KEY(submitted_by) REFERENCES user_account(id) ON DELETE SET NULL,
  CONSTRAINT fk_student_request_decided_by FOREIGN KEY(decided_by) REFERENCES user_account(id) ON DELETE SET NULL,
  CONSTRAINT chk_student_request_type CHECK (request_type IN ('ABSENCE', 'MAKEUP', 'TRANSFER')),
  CONSTRAINT chk_student_request_status CHECK (status IN ('PENDING', 'WAITING_CONFIRM', 'APPROVED', 'REJECTED', 'CANCELLED')),
  CONSTRAINT chk_makeup_valid CHECK (
    request_type != 'MAKEUP' OR (
      target_session_id IS NOT NULL AND
      makeup_session_id IS NOT NULL
    )
  )
);

CREATE TABLE teacher_request (
  id BIGSERIAL PRIMARY KEY,
  teacher_id BIGINT NOT NULL,
  session_id BIGINT, -- session cần thay đổi
  new_date DATE, -- teacher muốn đổi sang ngày này
  new_time_slot_id BIGINT, -- teacher muốn đổi sang khung giờ này
  new_resource_id BIGINT, -- teacher muốn đổi sang phòng này
  request_type VARCHAR(20) NOT NULL,
  replacement_teacher_id BIGINT, -- giáo viên thay thế (nếu có)
  new_session_id BIGINT, -- teacher đổi buổi sang buổi này
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  submitted_at TIMESTAMPTZ,
  submitted_by BIGINT,
  decided_by BIGINT,
  decided_at TIMESTAMPTZ,
  request_reason TEXT, -- của teacher
  note TEXT, -- note giáo vụ xử lý
  CONSTRAINT fk_teacher_request_teacher FOREIGN KEY(teacher_id) REFERENCES teacher(id) ON DELETE CASCADE,
  CONSTRAINT fk_teacher_request_replacement_teacher FOREIGN KEY(replacement_teacher_id) REFERENCES teacher(id) ON DELETE SET NULL,
  CONSTRAINT fk_teacher_request_session FOREIGN KEY(session_id) REFERENCES session(id) ON DELETE SET NULL,
  CONSTRAINT fk_teacher_request_submitted_by FOREIGN KEY(submitted_by) REFERENCES user_account(id) ON DELETE SET NULL,
  CONSTRAINT fk_teacher_request_decided_by FOREIGN KEY(decided_by) REFERENCES user_account(id) ON DELETE SET NULL,
  CONSTRAINT fk_teacher_request_new_time_slot FOREIGN KEY(new_time_slot_id) REFERENCES time_slot_template(id) ON DELETE SET NULL,
  CONSTRAINT fk_teacher_request_new_resource FOREIGN KEY(new_resource_id) REFERENCES resource(id) ON DELETE SET NULL,
  CONSTRAINT fk_teacher_request_new_session FOREIGN KEY(new_session_id) REFERENCES session(id) ON DELETE SET NULL,
  CONSTRAINT chk_teacher_request_type CHECK (request_type IN ('SWAP', 'RESCHEDULE', 'MODALITY_CHANGE')),
  CONSTRAINT chk_teacher_request_status CHECK (status IN ('PENDING', 'WAITING_CONFIRM', 'APPROVED', 'REJECTED'))
);

-- ========== SECTION 4: INDEXES ==========

-- ==================== FOREIGN KEY INDEXES ====================
-- PostgreSQL không tự động tạo index cho FK, cần tạo thủ công để tối ưu JOIN và ON DELETE CASCADE

-- Branch & Organization
CREATE INDEX idx_branch_center ON branch(center_id);
CREATE INDEX idx_time_slot_template_branch ON time_slot_template(branch_id);
CREATE INDEX idx_resource_branch ON resource(branch_id);

-- User & RBAC
CREATE INDEX idx_user_role_role ON user_role(role_id);
CREATE INDEX idx_user_branches_branch ON user_branches(branch_id);
CREATE INDEX idx_teacher_user_account ON teacher(user_account_id);
CREATE INDEX idx_student_user ON student(user_id);

-- Curriculum
CREATE INDEX idx_level_subject ON level(subject_id);
CREATE INDEX idx_plo_subject ON plo(subject_id);
CREATE INDEX idx_course_subject ON course(subject_id);
CREATE INDEX idx_course_level ON course(level_id);
CREATE INDEX idx_course_decided_by ON course(decided_by_manager);
CREATE INDEX idx_course_phase_course ON course_phase(course_id);
CREATE INDEX idx_course_session_phase ON course_session(phase_id);
CREATE INDEX idx_course_material_course ON course_material(course_id);
CREATE INDEX idx_course_material_phase ON course_material(phase_id);
CREATE INDEX idx_course_material_session ON course_material(course_session_id);
CREATE INDEX idx_clo_course ON clo(course_id);
CREATE INDEX idx_plo_clo_mapping_clo ON plo_clo_mapping(clo_id);
CREATE INDEX idx_course_session_clo_mapping_clo ON course_session_clo_mapping(clo_id);
CREATE INDEX idx_course_assessment_course ON course_assessment(course_id);
CREATE INDEX idx_course_assessment_clo_mapping_clo ON course_assessment_clo_mapping(clo_id);
CREATE INDEX idx_replacement_skill_student ON replacement_skill_assessment(student_id);
CREATE INDEX idx_replacement_skill_level ON replacement_skill_assessment(level_id);

-- Operations
CREATE INDEX idx_class_branch ON "class"(branch_id);
CREATE INDEX idx_class_course ON "class"(course_id);
CREATE INDEX idx_class_created_by ON "class"(created_by);
CREATE INDEX idx_class_decided_by ON "class"(decided_by);
CREATE INDEX idx_session_class ON session(class_id);
CREATE INDEX idx_session_course_session ON session(course_session_id);
CREATE INDEX idx_session_time_slot ON session(time_slot_template_id);
CREATE INDEX idx_session_resource_resource ON session_resource(resource_id);
CREATE INDEX idx_teaching_slot_teacher ON teaching_slot(teacher_id);
CREATE INDEX idx_teacher_availability_timeslot ON teacher_availability(time_slot_template_id);
CREATE INDEX idx_enrollment_student ON enrollment(student_id);
CREATE INDEX idx_enrollment_class ON enrollment(class_id);
CREATE INDEX idx_enrollment_join_session ON enrollment(join_session_id);
CREATE INDEX idx_enrollment_left_session ON enrollment(left_session_id);
CREATE INDEX idx_student_session_session ON student_session(session_id);
CREATE INDEX idx_student_session_makeup ON student_session(makeup_session_id);
CREATE INDEX idx_student_session_original ON student_session(original_session_id);

-- Assessment & Feedback
CREATE INDEX idx_assessment_class ON assessment(class_id);
CREATE INDEX idx_assessment_course_assessment ON assessment(course_assessment_id);
CREATE INDEX idx_score_assessment ON score(assessment_id);
CREATE INDEX idx_score_student ON score(student_id);
CREATE INDEX idx_score_graded_by ON score(graded_by);
CREATE INDEX idx_student_feedback_student ON student_feedback(student_id);
CREATE INDEX idx_student_feedback_class ON student_feedback(class_id);
CREATE INDEX idx_student_feedback_phase ON student_feedback(phase_id);
CREATE INDEX idx_student_feedback_response_feedback ON student_feedback_response(feedback_id);
CREATE INDEX idx_student_feedback_response_question ON student_feedback_response(question_id);
CREATE INDEX idx_qa_report_class ON qa_report(class_id);
CREATE INDEX idx_qa_report_session ON qa_report(session_id);
CREATE INDEX idx_qa_report_phase ON qa_report(phase_id);
CREATE INDEX idx_qa_report_reported_by ON qa_report(reported_by);

-- Requests
CREATE INDEX idx_student_request_student ON student_request(student_id);
CREATE INDEX idx_student_request_current_class ON student_request(current_class_id);
CREATE INDEX idx_student_request_target_class ON student_request(target_class_id);
CREATE INDEX idx_student_request_target_session ON student_request(target_session_id);
CREATE INDEX idx_student_request_makeup_session ON student_request(makeup_session_id);
CREATE INDEX idx_student_request_effective_session ON student_request(effective_session_id);
CREATE INDEX idx_student_request_decided_by ON student_request(decided_by);
CREATE INDEX idx_teacher_request_teacher ON teacher_request(teacher_id);
CREATE INDEX idx_teacher_request_session ON teacher_request(session_id);
CREATE INDEX idx_teacher_request_replacement_teacher ON teacher_request(replacement_teacher_id);
CREATE INDEX idx_teacher_request_new_time_slot ON teacher_request(new_time_slot_id);
CREATE INDEX idx_teacher_request_new_resource ON teacher_request(new_resource_id);
CREATE INDEX idx_teacher_request_new_session ON teacher_request(new_session_id);
CREATE INDEX idx_teacher_request_decided_by ON teacher_request(decided_by);

-- ==================== STATUS & FILTER INDEXES ====================
-- Indexes cho các query filter thường dùng (status, date ranges)

-- Status filters (frequent WHERE clauses)
CREATE INDEX idx_branch_status ON branch(status) WHERE status = 'ACTIVE';
CREATE INDEX idx_subject_status ON subject(status);
CREATE INDEX idx_course_status ON course(status);
CREATE INDEX idx_course_approval_status ON course(approval_status) WHERE approval_status = 'PENDING';
CREATE INDEX idx_class_status ON "class"(status);
CREATE INDEX idx_class_approval_status ON "class"(approval_status) WHERE approval_status = 'PENDING';
CREATE INDEX idx_session_status ON session(status);
CREATE INDEX idx_session_type ON session(type);
CREATE INDEX idx_enrollment_status ON enrollment(status) WHERE status = 'ENROLLED';
CREATE INDEX idx_student_session_attendance ON student_session(attendance_status);
CREATE INDEX idx_teaching_slot_status ON teaching_slot(status);
CREATE INDEX idx_student_request_status ON student_request(status) WHERE status IN ('PENDING', 'WAITING_CONFIRM');
CREATE INDEX idx_teacher_request_status ON teacher_request(status) WHERE status IN ('PENDING', 'WAITING_CONFIRM');

-- Date range queries (frequent in reports and scheduling)
CREATE INDEX idx_session_date ON session(date);
CREATE INDEX idx_class_start_date ON "class"(start_date);
CREATE INDEX idx_teacher_availability_effective_date ON teacher_availability(effective_date);
CREATE INDEX idx_course_effective_date ON course(effective_date);

-- ==================== SEARCH & LOOKUP INDEXES ====================
-- Indexes cho các query tìm kiếm theo code, email, phone

-- Code lookups (unique but frequently searched)
CREATE INDEX idx_center_code ON center(code);
CREATE INDEX idx_branch_code ON branch(code);
CREATE INDEX idx_subject_code ON subject(code);
CREATE INDEX idx_level_code ON level(code);
CREATE INDEX idx_course_code ON course(code);
CREATE INDEX idx_course_logical_code ON course(logical_course_code);
CREATE INDEX idx_resource_code ON resource(code);
CREATE INDEX idx_class_code ON "class"(code);
CREATE INDEX idx_teacher_employee_code ON teacher(employee_code);
CREATE INDEX idx_student_code ON student(student_code);

-- User account lookups
CREATE INDEX idx_user_account_email ON user_account(email);
CREATE INDEX idx_user_account_phone ON user_account(phone);

-- ==================== COMPOSITE INDEXES ====================
-- Indexes cho các query phức tạp thường dùng

-- Session scheduling queries: "Tìm sessions của class X vào ngày Y với status Z"
CREATE INDEX idx_session_class_date_status ON session(class_id, date, status);

-- Teacher schedule queries: "Lịch dạy của teacher X trong khoảng thời gian Y"
CREATE INDEX idx_teaching_slot_teacher_session ON teaching_slot(teacher_id, session_id);

-- Student attendance queries: "Điểm danh của student X trong class Y"
CREATE INDEX idx_student_session_student_attendance ON student_session(student_id, attendance_status);

-- Resource booking queries: "Phòng X có bị book vào ngày Y không?"
CREATE INDEX idx_session_resource_date ON session_resource(resource_id, session_id);

-- Enrollment active students: "Danh sách học sinh đang học ở class X"
CREATE INDEX idx_enrollment_class_status ON enrollment(class_id, status) WHERE status = 'ENROLLED';

-- Request processing queries: "Pending requests của student X"
CREATE INDEX idx_student_request_student_status ON student_request(student_id, status);
CREATE INDEX idx_teacher_request_teacher_status ON teacher_request(teacher_id, status);

-- Class scheduling queries: "Lớp học của branch X có status Y"
CREATE INDEX idx_class_branch_status ON "class"(branch_id, status);

-- Course approval workflow: "Courses đang chờ duyệt của subject X"
CREATE INDEX idx_course_subject_approval ON course(subject_id, approval_status) WHERE approval_status = 'PENDING';

-- Assessment grading: "Điểm của học sinh X trong assessment Y"
CREATE INDEX idx_score_student_assessment ON score(student_id, assessment_id);

-- Teacher availability lookup: "Teacher có rảnh vào thứ X slot Y không?"
CREATE INDEX idx_teacher_availability_day_slot ON teacher_availability(day_of_week, time_slot_template_id);

-- ==================== TIMESTAMP INDEXES ====================
-- Indexes cho sorting và filtering theo thời gian

CREATE INDEX idx_user_account_created_at ON user_account(created_at);
CREATE INDEX idx_class_created_at ON "class"(created_at);
CREATE INDEX idx_enrollment_enrolled_at ON enrollment(enrolled_at);
CREATE INDEX idx_student_request_submitted_at ON student_request(submitted_at);
CREATE INDEX idx_teacher_request_submitted_at ON teacher_request(submitted_at);
CREATE INDEX idx_score_graded_at ON score(graded_at);

-- ==================== TEXT SEARCH INDEXES ====================
-- GIN indexes cho full-text search (nếu cần)

-- Course and class name search
CREATE INDEX idx_course_name_gin ON course USING gin(to_tsvector('english', name));
CREATE INDEX idx_class_name_gin ON "class" USING gin(to_tsvector('english', name));

-- User account name search
CREATE INDEX idx_user_account_fullname_gin ON user_account USING gin(to_tsvector('english', full_name));

-- ==================== PARTIAL UNIQUE INDEX ====================
-- Thay thế unique constraint để cho phép multiple enrollment records với status khác nhau

-- Nếu muốn cho phép student re-enroll sau khi transferred/dropped,
-- comment out constraint uq_enrollment_class_student và uncomment index này:
-- CREATE UNIQUE INDEX uq_enrollment_active_class_student
--   ON enrollment(class_id, student_id)
--   WHERE status = 'ENROLLED';