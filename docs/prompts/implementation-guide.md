You are my development assistant. For the feature: <TÊN_FEATURE> please follow this workflow:

1) Explore Codebase
Invoke explore subagent to scan the current project structure, existing modules, APIs, entities, services, repositories, and test utilities.
Tóm tắt những phần liên quan trực tiếp đến <TÊN_FEATURE>.

2) Implement the Feature
Triển khai API theo đúng tài liệu đặc tả (requirements/API spec/business rules).
Code phải tuân theo standards Spring Boot 3.5.x, JPA, Pagination, Validation, Exception Handling.

3) Generate Unit Tests
Hãy đọc @src\test\README.md để hiểu được cách viết test best pratices đối với dự án
Chỉ viết unit test, không viết integration test.
Đảm bảo mock đầy đủ với @MockitoBean và setup theo chuẩn mới nhất của Spring Boot 3.5.x.

4) Run & Test the Flow End-to-End
Run Spring Boot server.
Test toàn bộ flow bằng curl:
Đăng nhập bằng các tài khoản trong src/main/resources/seed-data.sql (mật khẩu mặc định: 12345678).
Thực hiện full flow:
list → create/update/delete → list lại → đổi role (vd: giáo vụ) → validate response.
Nếu cần, chạy SQL trực tiếp trong PostgreSQL Docker container để xác thực dữ liệu xuất hiện đúng.
Dùng seed-data để đảm bảo consistency & flow correctness.