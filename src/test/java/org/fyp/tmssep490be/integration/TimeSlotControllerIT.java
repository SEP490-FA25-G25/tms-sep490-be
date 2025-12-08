package org.fyp.tmssep490be.integration;

import io.restassured.response.Response;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for TimeSlot Management API endpoints.
 * Tests full flow from REST API through database.
 */
@DisplayName("TimeSlot Management Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TimeSlotControllerIT extends BaseIntegrationTest {

        private static Long createdTimeSlotId;

        // ==================== GET TIME SLOTS ====================
        @Nested
        @DisplayName("GET /api/v1/time-slots")
        @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
        class GetTimeSlotsTests {

                @Test
                @Order(1)
                @DisplayName("Trả về danh sách time slots với CENTER_HEAD role")
                void shouldReturnTimeSlotsListWithCenterHeadRole() {
                        asCenterHead()
                                        .when()
                                        .get("/api/v1/time-slots")
                                        .then()
                                        .statusCode(200)
                                        .body("$", hasSize(greaterThanOrEqualTo(0)));
                }

                @Test
                @Order(2)
                @DisplayName("Lọc theo branchId")
                void shouldFilterByBranchId() {
                        asCenterHead()
                                        .queryParam("branchId", 1)
                                        .when()
                                        .get("/api/v1/time-slots")
                                        .then()
                                        .statusCode(200)
                                        .body("$", everyItem(hasEntry("branchId", 1)));
                }

                @Test
                @Order(3)
                @DisplayName("Tìm kiếm theo keyword")
                void shouldSearchByKeyword() {
                        asCenterHead()
                                        .queryParam("branchId", 1)
                                        .queryParam("search", "Morning")
                                        .when()
                                        .get("/api/v1/time-slots")
                                        .then()
                                        .statusCode(200);
                }

                @Test
                @Order(4)
                @DisplayName("Trả về 401/403 khi không có token")
                void shouldReturn401WhenNoToken() {
                        int statusCode = given()
                                        .when()
                                        .get("/api/v1/time-slots")
                                        .then()
                                        .extract().statusCode();
                        org.junit.jupiter.api.Assertions.assertTrue(
                                        statusCode == 401 || statusCode == 403,
                                        "Expected 401 or 403 but was " + statusCode);
                }
        }

        // ==================== CREATE TIME SLOT ====================
        @Nested
        @DisplayName("POST /api/v1/time-slots")
        @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
        class CreateTimeSlotTests {

                @Test
                @Order(1)
                @DisplayName("Tạo time slot thành công")
                void shouldCreateTimeSlotSuccessfully() {
                        Map<String, Object> request = Map.of(
                                        "branchId", 1,
                                        "name", "Integration Test Slot",
                                        "startTime", "18:00",
                                        "endTime", "20:00");

                        Response response = asCenterHead()
                                        .body(request)
                                        .when()
                                        .post("/api/v1/time-slots")
                                        .then()
                                        .statusCode(200)
                                        .body("id", notNullValue())
                                        .body("name", equalTo("Integration Test Slot"))
                                        .body("startTime", equalTo("18:00"))
                                        .body("endTime", equalTo("20:00"))
                                        .body("status", equalTo("ACTIVE"))
                                        .extract()
                                        .response();

                        // Store for later tests
                        createdTimeSlotId = response.jsonPath().getLong("id");
                }

                @Test
                @Order(2)
                @DisplayName("Trả về lỗi khi tên trống")
                void shouldReturnErrorWhenNameIsEmpty() {
                        Map<String, Object> request = Map.of(
                                        "branchId", 1,
                                        "name", "",
                                        "startTime", "08:00",
                                        "endTime", "10:00");

                        asCenterHead()
                                        .body(request)
                                        .when()
                                        .post("/api/v1/time-slots")
                                        .then()
                                        .statusCode(400)
                                        .body("message", containsString("tên"));
                }

                @Test
                @Order(3)
                @DisplayName("Trả về lỗi khi endTime <= startTime")
                void shouldReturnErrorWhenEndTimeNotAfterStartTime() {
                        Map<String, Object> request = Map.of(
                                        "branchId", 1,
                                        "name", "Invalid Time Slot",
                                        "startTime", "14:00",
                                        "endTime", "12:00");

                        asCenterHead()
                                        .body(request)
                                        .when()
                                        .post("/api/v1/time-slots")
                                        .then()
                                        .statusCode(400)
                                        .body("message", containsString("Giờ kết thúc"));
                }

                @Test
                @Order(4)
                @DisplayName("Trả về lỗi khi tên đã tồn tại")
                void shouldReturnErrorWhenNameAlreadyExists() {
                        // First create a time slot
                        Map<String, Object> request1 = Map.of(
                                        "branchId", 1,
                                        "name", "Duplicate Name Test",
                                        "startTime", "06:00",
                                        "endTime", "08:00");

                        asCenterHead()
                                        .body(request1)
                                        .when()
                                        .post("/api/v1/time-slots")
                                        .then()
                                        .statusCode(200);

                        // Try to create another with same name
                        Map<String, Object> request2 = Map.of(
                                        "branchId", 1,
                                        "name", "Duplicate Name Test",
                                        "startTime", "20:00",
                                        "endTime", "22:00");

                        asCenterHead()
                                        .body(request2)
                                        .when()
                                        .post("/api/v1/time-slots")
                                        .then()
                                        .statusCode(400)
                                        .body("message", containsString("đã tồn tại"));
                }

                @Test
                @Order(5)
                @DisplayName("Trả về lỗi khi khung giờ đã tồn tại")
                void shouldReturnErrorWhenTimeRangeAlreadyExists() {
                        // First create a time slot
                        Map<String, Object> request1 = Map.of(
                                        "branchId", 1,
                                        "name", "Time Range Test 1",
                                        "startTime", "05:00",
                                        "endTime", "07:00");

                        asCenterHead()
                                        .body(request1)
                                        .when()
                                        .post("/api/v1/time-slots")
                                        .then()
                                        .statusCode(200);

                        // Try to create another with same time range
                        Map<String, Object> request2 = Map.of(
                                        "branchId", 1,
                                        "name", "Time Range Test 2",
                                        "startTime", "05:00",
                                        "endTime", "07:00");

                        asCenterHead()
                                        .body(request2)
                                        .when()
                                        .post("/api/v1/time-slots")
                                        .then()
                                        .statusCode(400)
                                        .body("message", containsString("đã tồn tại"));
                }

                @Test
                @Order(6)
                @DisplayName("Trả về 403 khi không có quyền CENTER_HEAD")
                void shouldReturn403WhenNotCenterHead() {
                        Map<String, Object> request = Map.of(
                                        "branchId", 1,
                                        "name", "No Permission Slot",
                                        "startTime", "21:00",
                                        "endTime", "23:00");

                        asAcademicAffair()
                                        .body(request)
                                        .when()
                                        .post("/api/v1/time-slots")
                                        .then()
                                        .statusCode(403);
                }
        }

        // ==================== GET TIME SLOT BY ID ====================
        @Nested
        @DisplayName("GET /api/v1/time-slots/{id}")
        class GetTimeSlotByIdTests {

                @Test
                @Order(1)
                @DisplayName("Trả về time slot khi tìm thấy")
                void shouldReturnTimeSlotWhenFound() {
                        // ID 1 should exist from seed data
                        asCenterHead()
                                        .when()
                                        .get("/api/v1/time-slots/1")
                                        .then()
                                        .statusCode(200)
                                        .body("id", equalTo(1));
                }

                @Test
                @Order(2)
                @DisplayName("Trả về lỗi khi không tìm thấy")
                void shouldReturn404WhenNotFound() {
                        int statusCode = asCenterHead()
                                        .when()
                                        .get("/api/v1/time-slots/999999")
                                        .then()
                                        .extract().statusCode();
                        org.junit.jupiter.api.Assertions.assertTrue(
                                        statusCode == 400 || statusCode == 404,
                                        "Expected 400 or 404 but was " + statusCode);
                }
        }

        // ==================== UPDATE TIME SLOT ====================
        @Nested
        @DisplayName("PUT /api/v1/time-slots/{id}")
        @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
        class UpdateTimeSlotTests {

                @Test
                @Order(1)
                @DisplayName("Cập nhật tên thành công")
                void shouldUpdateNameSuccessfully() {
                        // First create a new time slot
                        Map<String, Object> createRequest = Map.of(
                                        "branchId", 1,
                                        "name", "Update Test Slot",
                                        "startTime", "04:00",
                                        "endTime", "06:00");

                        Response createResponse = asCenterHead()
                                        .body(createRequest)
                                        .when()
                                        .post("/api/v1/time-slots")
                                        .then()
                                        .statusCode(200)
                                        .extract()
                                        .response();

                        Long timeSlotId = createResponse.jsonPath().getLong("id");

                        // Update the name
                        Map<String, Object> updateRequest = Map.of(
                                        "name", "Updated Slot Name");

                        asCenterHead()
                                        .body(updateRequest)
                                        .when()
                                        .put("/api/v1/time-slots/" + timeSlotId)
                                        .then()
                                        .statusCode(200)
                                        .body("name", equalTo("Updated Slot Name"));
                }
        }

        // ==================== UPDATE TIME SLOT STATUS ====================
        @Nested
        @DisplayName("PATCH /api/v1/time-slots/{id}/status")
        @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
        class UpdateTimeSlotStatusTests {

                @Test
                @Order(1)
                @DisplayName("Ngưng hoạt động time slot thành công")
                void shouldDeactivateTimeSlotSuccessfully() {
                        // Create a new time slot
                        Map<String, Object> createRequest = Map.of(
                                        "branchId", 1,
                                        "name", "Deactivate Test Slot",
                                        "startTime", "03:00",
                                        "endTime", "05:00");

                        Response createResponse = asCenterHead()
                                        .body(createRequest)
                                        .when()
                                        .post("/api/v1/time-slots")
                                        .then()
                                        .statusCode(200)
                                        .extract()
                                        .response();

                        Long timeSlotId = createResponse.jsonPath().getLong("id");

                        // Deactivate
                        asCenterHead()
                                        .body(Map.of("status", "INACTIVE"))
                                        .when()
                                        .patch("/api/v1/time-slots/" + timeSlotId + "/status")
                                        .then()
                                        .statusCode(200)
                                        .body("status", equalTo("INACTIVE"));
                }

                @Test
                @Order(2)
                @DisplayName("Kích hoạt lại time slot thành công")
                void shouldReactivateTimeSlotSuccessfully() {
                        // Create a new time slot
                        Map<String, Object> createRequest = Map.of(
                                        "branchId", 1,
                                        "name", "Reactivate Test Slot",
                                        "startTime", "02:00",
                                        "endTime", "04:00");

                        Response createResponse = asCenterHead()
                                        .body(createRequest)
                                        .when()
                                        .post("/api/v1/time-slots")
                                        .then()
                                        .statusCode(200)
                                        .extract()
                                        .response();

                        Long timeSlotId = createResponse.jsonPath().getLong("id");

                        // Deactivate
                        asCenterHead()
                                        .body(Map.of("status", "INACTIVE"))
                                        .when()
                                        .patch("/api/v1/time-slots/" + timeSlotId + "/status")
                                        .then()
                                        .statusCode(200);

                        // Reactivate
                        asCenterHead()
                                        .body(Map.of("status", "ACTIVE"))
                                        .when()
                                        .patch("/api/v1/time-slots/" + timeSlotId + "/status")
                                        .then()
                                        .statusCode(200)
                                        .body("status", equalTo("ACTIVE"));
                }
        }

        // ==================== DELETE TIME SLOT ====================
        @Nested
        @DisplayName("DELETE /api/v1/time-slots/{id}")
        @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
        class DeleteTimeSlotTests {

                @Test
                @Order(1)
                @DisplayName("Xóa time slot thành công khi INACTIVE và không có dependencies")
                void shouldDeleteTimeSlotSuccessfully() {
                        // Create a new time slot
                        Map<String, Object> createRequest = Map.of(
                                        "branchId", 1,
                                        "name", "Delete Test Slot",
                                        "startTime", "01:00",
                                        "endTime", "03:00");

                        Response createResponse = asCenterHead()
                                        .body(createRequest)
                                        .when()
                                        .post("/api/v1/time-slots")
                                        .then()
                                        .statusCode(200)
                                        .extract()
                                        .response();

                        Long timeSlotId = createResponse.jsonPath().getLong("id");

                        // Deactivate first
                        asCenterHead()
                                        .body(Map.of("status", "INACTIVE"))
                                        .when()
                                        .patch("/api/v1/time-slots/" + timeSlotId + "/status")
                                        .then()
                                        .statusCode(200);

                        // Delete
                        asCenterHead()
                                        .when()
                                        .delete("/api/v1/time-slots/" + timeSlotId)
                                        .then()
                                        .statusCode(204);

                        // Verify deletion - API returns 400 for not found
                        int statusCode = asCenterHead()
                                        .when()
                                        .get("/api/v1/time-slots/" + timeSlotId)
                                        .then()
                                        .extract().statusCode();
                        org.junit.jupiter.api.Assertions.assertTrue(
                                        statusCode == 400 || statusCode == 404,
                                        "Expected 400 or 404 but was " + statusCode);
                }

                @Test
                @Order(2)
                @DisplayName("Trả về lỗi khi xóa time slot đang ACTIVE")
                void shouldReturnErrorWhenDeletingActiveTimeSlot() {
                        // Create a new active time slot
                        Map<String, Object> createRequest = Map.of(
                                        "branchId", 1,
                                        "name", "No Delete Slot",
                                        "startTime", "00:00",
                                        "endTime", "02:00");

                        Response createResponse = asCenterHead()
                                        .body(createRequest)
                                        .when()
                                        .post("/api/v1/time-slots")
                                        .then()
                                        .statusCode(200)
                                        .extract()
                                        .response();

                        Long timeSlotId = createResponse.jsonPath().getLong("id");

                        // Try to delete without deactivating
                        asCenterHead()
                                        .when()
                                        .delete("/api/v1/time-slots/" + timeSlotId)
                                        .then()
                                        .statusCode(400)
                                        .body("message", containsString("ngưng hoạt động"));
                }
        }

        // ==================== GET SESSIONS BY TIME SLOT ====================
        @Nested
        @DisplayName("GET /api/v1/time-slots/{id}/sessions")
        class GetSessionsByTimeSlotTests {

                @Test
                @Order(1)
                @DisplayName("Trả về danh sách sessions")
                void shouldReturnSessionsList() {
                        // Time slot ID 1 should exist from seed data
                        asCenterHead()
                                        .when()
                                        .get("/api/v1/time-slots/1/sessions")
                                        .then()
                                        .statusCode(200)
                                        .body("$", instanceOf(java.util.List.class));
                }

                @Test
                @Order(2)
                @DisplayName("Trả về lỗi khi time slot không tồn tại")
                void shouldReturn404WhenTimeSlotNotFound() {
                        int statusCode = asCenterHead()
                                        .when()
                                        .get("/api/v1/time-slots/999999/sessions")
                                        .then()
                                        .extract().statusCode();
                        org.junit.jupiter.api.Assertions.assertTrue(
                                        statusCode == 400 || statusCode == 404,
                                        "Expected 400 or 404 but was " + statusCode);
                }
        }

        // ==================== GET BRANCH TIME SLOT TEMPLATES ====================
        @Nested
        @DisplayName("GET /api/v1/branches/{branchId}/time-slot-templates")
        class GetBranchTimeSlotTemplatesTests {

                @Test
                @Order(1)
                @DisplayName("Trả về danh sách time slot templates cho dropdown")
                void shouldReturnTimeSlotTemplatesForDropdown() {
                        asAcademicAffair()
                                        .when()
                                        .get("/api/v1/branches/1/time-slot-templates")
                                        .then()
                                        .statusCode(200)
                                        .body("success", equalTo(true))
                                        .body("data", instanceOf(java.util.List.class));
                }
        }
}
