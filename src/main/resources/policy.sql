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

-- 9. Student Max Concurrent Enrollments (GLOBAL)
INSERT INTO system_policy (
    policy_key, policy_category, policy_name, description,
    value_type, default_value, current_value, min_value, max_value, unit,
    scope, branch_id, course_id, class_id,
    is_active, version, created_by, updated_by
) VALUES (
    'student.max_concurrent_enrollments',
    'ENROLLMENT',
    'Số lớp học tối đa đồng thời',
    'Sinh viên chỉ được ghi danh tối đa X lớp học cùng lúc (status = ENROLLED). Đây là giới hạn để đảm bảo chất lượng học tập.',
    'INTEGER',
    '3',
    '3',
    '1',
    '10',
    'classes',
    'GLOBAL',
    NULL, NULL, NULL,
    true, 1,
    (SELECT id FROM user_account WHERE email = 'admin@tms-edu.vn' LIMIT 1),
    (SELECT id FROM user_account WHERE email = 'admin@tms-edu.vn' LIMIT 1)
);

-- 10. Student Default Password (GLOBAL)
INSERT INTO system_policy (
    policy_key, policy_category, policy_name, description,
    value_type, default_value, current_value, min_value, max_value, unit,
    scope, branch_id, course_id, class_id,
    is_active, version, created_by, updated_by
) VALUES (
    'student.default_password',
    'SECURITY',
    'Mật khẩu mặc định cho sinh viên mới',
    'Mật khẩu được cấp cho tài khoản sinh viên mới tạo. Sinh viên nên đổi mật khẩu sau lần đăng nhập đầu tiên.',
    'STRING',
    '12345678',
    '12345678',
    NULL,
    NULL,
    NULL,
    'GLOBAL',
    NULL, NULL, NULL,
    true, 1,
    (SELECT id FROM user_account WHERE email = 'admin@tms-edu.vn' LIMIT 1),
    (SELECT id FROM user_account WHERE email = 'admin@tms-edu.vn' LIMIT 1)
);

-- 11. Student Makeup Request Weeks Limit (GLOBAL)
INSERT INTO system_policy (
    policy_key, policy_category, policy_name, description,
    value_type, default_value, current_value, min_value, max_value, unit,
    scope, branch_id, course_id, class_id,
    is_active, version, created_by, updated_by
) VALUES (
    'student.makeup.weeks_limit',
    'REQUEST',
    'Giới hạn thời gian yêu cầu học bù',
    'Sinh viên chỉ được yêu cầu học bù cho buổi vắng trong vòng X tuần gần nhất. Sau thời gian này, yêu cầu học bù sẽ không được chấp nhận.',
    'INTEGER',
    '4',
    '4',
    '1',
    '12',
    'weeks',
    'GLOBAL',
    NULL, NULL, NULL,
    true, 1,
    (SELECT id FROM user_account WHERE email = 'admin@tms-edu.vn' LIMIT 1),
    (SELECT id FROM user_account WHERE email = 'admin@tms-edu.vn' LIMIT 1)
);

-- 12. Student Makeup Lookback Weeks (GLOBAL)
INSERT INTO system_policy (
    policy_key, policy_category, policy_name, description,
    value_type, default_value, current_value, min_value, max_value, unit,
    scope, branch_id, course_id, class_id,
    is_active, version, created_by, updated_by
) VALUES (
    'student.makeup.lookback_weeks',
    'REQUEST',
    'Số tuần tìm kiếm buổi vắng',
    'Khi tạo yêu cầu học bù, hệ thống chỉ hiển thị các buổi vắng trong X tuần gần nhất. Giá trị này phải <= giới hạn yêu cầu học bù.',
    'INTEGER',
    '2',
    '2',
    '1',
    '8',
    'weeks',
    'GLOBAL',
    NULL, NULL, NULL,
    true, 1,
    (SELECT id FROM user_account WHERE email = 'admin@tms-edu.vn' LIMIT 1),
    (SELECT id FROM user_account WHERE email = 'admin@tms-edu.vn' LIMIT 1)
);

-- 13. Teacher Availability Campaign Active (GLOBAL)
INSERT INTO system_policy (
    policy_key, policy_category, policy_name, description,
    value_type, default_value, current_value, min_value, max_value, unit,
    scope, branch_id, course_id, class_id,
    is_active, version, created_by, updated_by
) VALUES (
    'TEACHER_AVAILABILITY_CAMPAIGN_ACTIVE',
    'TEACHER_OPERATIONS',
    'Yêu cầu đợt đăng ký để cập nhật lịch',
    'Nếu true, giáo viên có thể cập nhật lịch rảnh bất cứ lúc nào. Nếu false, giáo viên chỉ có thể cập nhật khi có đợt đăng ký (campaign) đang mở.',
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

-- 14. Teacher Availability Lock Window Days (GLOBAL)
INSERT INTO system_policy (
    policy_key, policy_category, policy_name, description,
    value_type, default_value, current_value, min_value, max_value, unit,
    scope, branch_id, course_id, class_id,
    is_active, version, created_by, updated_by
) VALUES (
    'TEACHER_AVAILABILITY_LOCK_WINDOW_DAYS',
    'TEACHER_OPERATIONS',
    'Số ngày khóa lịch trước khi sửa',
    'Các slot trong vòng X ngày tới sẽ bị khóa và không thể sửa đổi. Giá trị này đảm bảo giáo viên không thể thay đổi lịch quá gần với thời điểm diễn ra lớp học.',
    'INTEGER',
    '7',
    '7',
    '0',
    '30',
    'days',
    'GLOBAL',
    NULL, NULL, NULL,
    true, 1,
    (SELECT id FROM user_account WHERE email = 'admin@tms-edu.vn' LIMIT 1),
    (SELECT id FROM user_account WHERE email = 'admin@tms-edu.vn' LIMIT 1)
);

-- 15. Min Weekly Slots for Full-time Teachers (GLOBAL)
INSERT INTO system_policy (
    policy_key, policy_category, policy_name, description,
    value_type, default_value, current_value, min_value, max_value, unit,
    scope, branch_id, course_id, class_id,
    is_active, version, created_by, updated_by
) VALUES (
    'MIN_WEEKLY_SLOTS_FULLTIME',
    'TEACHER_CONTRACT',
    'Số slot tối thiểu/tuần (Full-time)',
    'Giáo viên Full-time phải đăng ký tối thiểu X slot rảnh mỗi tuần (bao gồm cả slot đang dạy). Nếu dưới ngưỡng này, hệ thống sẽ cảnh báo nhưng không chặn việc lưu.',
    'INTEGER',
    '20',
    '20',
    '0',
    '50',
    'slots',
    'GLOBAL',
    NULL, NULL, NULL,
    true, 1,
    (SELECT id FROM user_account WHERE email = 'admin@tms-edu.vn' LIMIT 1),
    (SELECT id FROM user_account WHERE email = 'admin@tms-edu.vn' LIMIT 1)
);

-- 16. Min Weekly Slots for Part-time Teachers (GLOBAL)
INSERT INTO system_policy (
    policy_key, policy_category, policy_name, description,
    value_type, default_value, current_value, min_value, max_value, unit,
    scope, branch_id, course_id, class_id,
    is_active, version, created_by, updated_by
) VALUES (
    'MIN_WEEKLY_SLOTS_PARTTIME',
    'TEACHER_CONTRACT',
    'Số slot tối thiểu/tuần (Part-time)',
    'Giáo viên Part-time phải đăng ký tối thiểu X slot rảnh mỗi tuần (bao gồm cả slot đang dạy). Nếu dưới ngưỡng này, hệ thống sẽ cảnh báo nhưng không chặn việc lưu.',
    'INTEGER',
    '4',
    '4',
    '0',
    '20',
    'slots',
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

-- 3. Policy History (Test Audit Logs)
INSERT INTO policy_history (policy_id, changed_by, old_value, new_value, change_type, reason, changed_at, version)
SELECT 
    id, 
    (SELECT id FROM user_account WHERE email = 'admin@tms-edu.vn' LIMIT 1), -- Admin
    '2', 
    '1', 
    'UPDATE', 
    'Tightening absence request deadline', 
    NOW() - INTERVAL '1 month',
    1
FROM system_policy WHERE policy_key = 'request.absence.lead_time_days';

INSERT INTO policy_history (policy_id, changed_by, old_value, new_value, change_type, reason, changed_at, version)
SELECT 
    id, 
    (SELECT id FROM user_account WHERE email = 'admin@tms-edu.vn' LIMIT 1), -- Admin
    '1', 
    '2', 
    'UPDATE', 
    'Reverting absence request deadline', 
    NOW() - INTERVAL '2 weeks',
    2
FROM system_policy WHERE policy_key = 'request.absence.lead_time_days';
