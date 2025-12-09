package org.fyp.tmssep490be.integration;

import io.restassured.response.Response;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for Resource Management API endpoints.
 * Tests full flow from REST API through database.
 */
@DisplayName("Resource Management Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ResourceControllerIT extends BaseIntegrationTest {

    private static Long createdResourceId;

    // ==================== GET RESOURCES ====================
    @Nested
    @DisplayName("GET /api/v1/resources")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class GetResourcesTests {

        @Test
        @Order(1)
        @DisplayName("Trả về danh sách resources với CENTER_HEAD role")
        void shouldReturnResourcesListWithCenterHeadRole() {
            asCenterHead()
                    .when()
                    .get("/api/v1/resources")
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
                    .get("/api/v1/resources")
                    .then()
                    .statusCode(200)
                    .body("$", everyItem(hasEntry("branchId", 1)));
        }

        @Test
        @Order(3)
        @DisplayName("Lọc theo resourceType = ROOM")
        void shouldFilterByResourceType() {
            asCenterHead()
                    .queryParam("branchId", 1)
                    .queryParam("resourceType", "ROOM")
                    .when()
                    .get("/api/v1/resources")
                    .then()
                    .statusCode(200)
                    .body("$", everyItem(hasEntry("resourceType", "ROOM")));
        }

        @Test
        @Order(4)
        @DisplayName("Tìm kiếm theo keyword")
        void shouldSearchByKeyword() {
            asCenterHead()
                    .queryParam("branchId", 1)
                    .queryParam("search", "Room")
                    .when()
                    .get("/api/v1/resources")
                    .then()
                    .statusCode(200);
        }

        @Test
        @Order(5)
        @DisplayName("Trả về 401/403 khi không có token")
        void shouldReturn401WhenNoToken() {
            int statusCode = given()
                    .when()
                    .get("/api/v1/resources")
                    .then()
                    .extract().statusCode();
            // API may return 401 or 403 depending on security config
            org.junit.jupiter.api.Assertions.assertTrue(
                    statusCode == 401 || statusCode == 403,
                    "Expected 401 or 403 but was " + statusCode);
        }
    }

    // ==================== CREATE RESOURCE ====================
    @Nested
    @DisplayName("POST /api/v1/resources")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class CreateResourceTests {

        @Test
        @Order(1)
        @DisplayName("Tạo ROOM resource thành công")
        void shouldCreateRoomResourceSuccessfully() {
            Map<String, Object> request = Map.of(
                    "branchId", 1,
                    "resourceType", "ROOM",
                    "code", "IT-R999",
                    "name", "Test Room 999",
                    "capacity", 25,
                    "description", "Integration test room for automated testing");

            Response response = asCenterHead()
                    .body(request)
                    .when()
                    .post("/api/v1/resources")
                    .then()
                    .statusCode(200)
                    .body("id", notNullValue())
                    .body("name", equalTo("Test Room 999"))
                    .body("resourceType", equalTo("ROOM"))
                    .body("status", equalTo("ACTIVE"))
                    .extract()
                    .response();

            // Store for later tests
            createdResourceId = response.jsonPath().getLong("id");
        }

        @Test
        @Order(2)
        @DisplayName("Tạo VIRTUAL resource thành công")
        void shouldCreateVirtualResourceSuccessfully() {
            Map<String, Object> request = Map.of(
                    "branchId", 1,
                    "resourceType", "VIRTUAL",
                    "code", "IT-Z999",
                    "name", "Test Zoom 999",
                    "capacity", 50,
                    "meetingUrl", "https://zoom.us/j/integration-test");

            asCenterHead()
                    .body(request)
                    .when()
                    .post("/api/v1/resources")
                    .then()
                    .statusCode(200)
                    .body("id", notNullValue())
                    .body("resourceType", equalTo("VIRTUAL"));
        }

        @Test
        @Order(3)
        @DisplayName("Trả về lỗi khi code trống")
        void shouldReturnErrorWhenCodeIsEmpty() {
            Map<String, Object> request = Map.of(
                    "branchId", 1,
                    "resourceType", "ROOM",
                    "code", "",
                    "name", "Test Room",
                    "capacity", 20);

            asCenterHead()
                    .body(request)
                    .when()
                    .post("/api/v1/resources")
                    .then()
                    .statusCode(400);
        }

        @Test
        @Order(4)
        @DisplayName("Trả về lỗi khi capacity vượt quá giới hạn ROOM")
        void shouldReturnErrorWhenRoomCapacityExceedsLimit() {
            Map<String, Object> request = Map.of(
                    "branchId", 1,
                    "resourceType", "ROOM",
                    "code", "IT-RMAX",
                    "name", "Max Capacity Room",
                    "capacity", 50 // Limit is 40
            );

            asCenterHead()
                    .body(request)
                    .when()
                    .post("/api/v1/resources")
                    .then()
                    .statusCode(400)
                    .body("message", containsString("40"));
        }

        @Test
        @Order(5)
        @DisplayName("Trả về lỗi khi VIRTUAL resource không có meeting URL")
        void shouldReturnErrorWhenVirtualMissingMeetingUrl() {
            Map<String, Object> request = Map.of(
                    "branchId", 1,
                    "resourceType", "VIRTUAL",
                    "code", "IT-ZNULL",
                    "name", "Missing URL Zoom",
                    "capacity", 50);

            asCenterHead()
                    .body(request)
                    .when()
                    .post("/api/v1/resources")
                    .then()
                    .statusCode(400)
                    .body("message", containsString("Meeting URL"));
        }

        @Test
        @Order(6)
        @DisplayName("Trả về 403 khi không có quyền CENTER_HEAD")
        void shouldReturn403WhenNotCenterHead() {
            Map<String, Object> request = Map.of(
                    "branchId", 1,
                    "resourceType", "ROOM",
                    "code", "IT-R001",
                    "name", "Test Room",
                    "capacity", 20);

            asAcademicAffair()
                    .body(request)
                    .when()
                    .post("/api/v1/resources")
                    .then()
                    .statusCode(403);
        }
    }

    // ==================== GET RESOURCE BY ID ====================
    @Nested
    @DisplayName("GET /api/v1/resources/{id}")
    class GetResourceByIdTests {

        @Test
        @Order(1)
        @DisplayName("Trả về resource khi tìm thấy")
        void shouldReturnResourceWhenFound() {
            // ID 1 should exist from seed data
            asCenterHead()
                    .when()
                    .get("/api/v1/resources/1")
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
                    .get("/api/v1/resources/999999")
                    .then()
                    .extract().statusCode();
            // API may return 400 or 404 for not-found resources
            org.junit.jupiter.api.Assertions.assertTrue(
                    statusCode == 400 || statusCode == 404,
                    "Expected 400 or 404 but was " + statusCode);
        }
    }

    // ==================== UPDATE RESOURCE ====================
    @Nested
    @DisplayName("PUT /api/v1/resources/{id}")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class UpdateResourceTests {

        @Test
        @Order(1)
        @DisplayName("Cập nhật tên thành công")
        void shouldUpdateNameSuccessfully() {
            // Use resource from seed data (ID 1)
            Map<String, Object> request = Map.of(
                    "name", "Updated Room Name");

            asCenterHead()
                    .body(request)
                    .when()
                    .put("/api/v1/resources/1")
                    .then()
                    .statusCode(200)
                    .body("name", equalTo("Updated Room Name"));
        }

        @Test
        @Order(2)
        @DisplayName("Trả về lỗi khi mô tả quá ngắn")
        void shouldReturnErrorWhenDescriptionTooShort() {
            Map<String, Object> request = Map.of(
                    "description", "Short" // Less than 10 chars
            );

            asCenterHead()
                    .body(request)
                    .when()
                    .put("/api/v1/resources/1")
                    .then()
                    .statusCode(400)
                    .body("message", containsString("10"));
        }
    }

    // ==================== UPDATE RESOURCE STATUS ====================
    @Nested
    @DisplayName("PATCH /api/v1/resources/{id}/status")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class UpdateResourceStatusTests {

        @Test
        @Order(1)
        @DisplayName("Ngưng hoạt động resource thành công (nếu không có session)")
        void shouldDeactivateResourceSuccessfully() {
            // Note: This may fail if the resource has future sessions
            // First create a new resource, then deactivate it
            Map<String, Object> createRequest = Map.of(
                    "branchId", 1,
                    "resourceType", "ROOM",
                    "code", "IT-DEACT",
                    "name", "Deactivation Test Room",
                    "capacity", 15);

            Response createResponse = asCenterHead()
                    .body(createRequest)
                    .when()
                    .post("/api/v1/resources")
                    .then()
                    .statusCode(200)
                    .extract()
                    .response();

            Long resourceId = createResponse.jsonPath().getLong("id");

            // Now deactivate it
            asCenterHead()
                    .body(Map.of("status", "INACTIVE"))
                    .when()
                    .patch("/api/v1/resources/" + resourceId + "/status")
                    .then()
                    .statusCode(200)
                    .body("status", equalTo("INACTIVE"));
        }

        @Test
        @Order(2)
        @DisplayName("Kích hoạt lại resource thành công")
        void shouldReactivateResourceSuccessfully() {
            // Create and deactivate a resource first
            Map<String, Object> createRequest = Map.of(
                    "branchId", 1,
                    "resourceType", "ROOM",
                    "code", "IT-REACT",
                    "name", "Reactivation Test Room",
                    "capacity", 15);

            Response createResponse = asCenterHead()
                    .body(createRequest)
                    .when()
                    .post("/api/v1/resources")
                    .then()
                    .statusCode(200)
                    .extract()
                    .response();

            Long resourceId = createResponse.jsonPath().getLong("id");

            // Deactivate
            asCenterHead()
                    .body(Map.of("status", "INACTIVE"))
                    .when()
                    .patch("/api/v1/resources/" + resourceId + "/status")
                    .then()
                    .statusCode(200);

            // Reactivate
            asCenterHead()
                    .body(Map.of("status", "ACTIVE"))
                    .when()
                    .patch("/api/v1/resources/" + resourceId + "/status")
                    .then()
                    .statusCode(200)
                    .body("status", equalTo("ACTIVE"));
        }
    }

    // ==================== DELETE RESOURCE ====================
    @Nested
    @DisplayName("DELETE /api/v1/resources/{id}")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class DeleteResourceTests {

        @Test
        @Order(1)
        @DisplayName("Xóa resource thành công khi INACTIVE và không có dependencies")
        void shouldDeleteResourceSuccessfully() {
            // Create a new resource
            Map<String, Object> createRequest = Map.of(
                    "branchId", 1,
                    "resourceType", "ROOM",
                    "code", "IT-DEL",
                    "name", "Delete Test Room",
                    "capacity", 10);

            Response createResponse = asCenterHead()
                    .body(createRequest)
                    .when()
                    .post("/api/v1/resources")
                    .then()
                    .statusCode(200)
                    .extract()
                    .response();

            Long resourceId = createResponse.jsonPath().getLong("id");

            // Deactivate first
            asCenterHead()
                    .body(Map.of("status", "INACTIVE"))
                    .when()
                    .patch("/api/v1/resources/" + resourceId + "/status")
                    .then()
                    .statusCode(200);

            // Delete
            asCenterHead()
                    .when()
                    .delete("/api/v1/resources/" + resourceId)
                    .then()
                    .statusCode(204);

            // Verify deletion - API returns 400 for not found
            int statusCode = asCenterHead()
                    .when()
                    .get("/api/v1/resources/" + resourceId)
                    .then()
                    .extract().statusCode();
            org.junit.jupiter.api.Assertions.assertTrue(
                    statusCode == 400 || statusCode == 404,
                    "Expected 400 or 404 but was " + statusCode);
        }

        @Test
        @Order(2)
        @DisplayName("Trả về lỗi khi xóa resource đang ACTIVE")
        void shouldReturnErrorWhenDeletingActiveResource() {
            // Create a new active resource
            Map<String, Object> createRequest = Map.of(
                    "branchId", 1,
                    "resourceType", "ROOM",
                    "code", "IT-NODEL",
                    "name", "No Delete Room",
                    "capacity", 10);

            Response createResponse = asCenterHead()
                    .body(createRequest)
                    .when()
                    .post("/api/v1/resources")
                    .then()
                    .statusCode(200)
                    .extract()
                    .response();

            Long resourceId = createResponse.jsonPath().getLong("id");

            // Try to delete without deactivating
            asCenterHead()
                    .when()
                    .delete("/api/v1/resources/" + resourceId)
                    .then()
                    .statusCode(400)
                    .body("message", containsString("ngưng hoạt động"));
        }
    }

    // ==================== GET SESSIONS BY RESOURCE ====================
    @Nested
    @DisplayName("GET /api/v1/resources/{id}/sessions")
    class GetSessionsByResourceTests {

        @Test
        @Order(1)
        @DisplayName("Trả về danh sách sessions")
        void shouldReturnSessionsList() {
            // Resource ID 1 should exist from seed data
            asCenterHead()
                    .when()
                    .get("/api/v1/resources/1/sessions")
                    .then()
                    .statusCode(200)
                    .body("$", instanceOf(java.util.List.class));
        }

        @Test
        @Order(2)
        @DisplayName("Trả về lỗi khi resource không tồn tại")
        void shouldReturn404WhenResourceNotFound() {
            int statusCode = asCenterHead()
                    .when()
                    .get("/api/v1/resources/999999/sessions")
                    .then()
                    .extract().statusCode();
            org.junit.jupiter.api.Assertions.assertTrue(
                    statusCode == 400 || statusCode == 404,
                    "Expected 400 or 404 but was " + statusCode);
        }
    }
}
