package org.fyp.tmssep490be.integration;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static io.restassured.RestAssured.given;

/**
 * Base class for Integration Tests using Testcontainers and REST Assured.
 * Provides PostgreSQL container, common setup, and authentication helpers.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseIntegrationTest {

    // Mock JavaMailSender to avoid email service configuration issues
    @MockBean
    @SuppressWarnings("unused")
    private JavaMailSender javaMailSender;

    // Static container shared across all test classes for faster tests
    static PostgreSQLContainer<?> postgres;

    static {
        postgres = new PostgreSQLContainer<>("postgres:15-alpine")
                .withDatabaseName("tms_test")
                .withUsername("test")
                .withPassword("test")
                .withInitScript("schema.sql");
        postgres.start();
    }

    @LocalServerPort
    protected int port;

    // Cache JWT tokens for test users
    protected String centerHeadToken;
    protected String academicAffairToken;
    protected String managerToken;
    protected String teacherToken;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.sql.init.mode", () -> "always");
        registry.add("spring.sql.init.data-locations", () -> "classpath:test-seed-data.sql");
    }

    @BeforeAll
    void setupAll() {
        RestAssured.baseURI = "http://localhost";
    }

    @BeforeEach
    void setupEach() {
        RestAssured.port = port;
        // Login and cache tokens if not already cached
        if (centerHeadToken == null) {
            centerHeadToken = login("admin@example.com", "password123");
        }
        if (academicAffairToken == null) {
            academicAffairToken = login("aa@example.com", "password123");
        }
    }

    /**
     * Login and return access token
     */
    protected String login(String email, String password) {
        Response response = given()
                .contentType(ContentType.JSON)
                .body(Map.of("email", email, "password", password))
                .when()
                .post("/api/v1/auth/login")
                .then()
                .statusCode(200)
                .extract()
                .response();

        return response.jsonPath().getString("data.accessToken");
    }

    /**
     * Helper to create authorized request with Bearer token
     */
    protected io.restassured.specification.RequestSpecification withAuth(String token) {
        return given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token);
    }

    /**
     * Helper for CENTER_HEAD authorized request
     */
    protected io.restassured.specification.RequestSpecification asCenterHead() {
        return withAuth(centerHeadToken);
    }

    /**
     * Helper for ACADEMIC_AFFAIR authorized request
     */
    protected io.restassured.specification.RequestSpecification asAcademicAffair() {
        return withAuth(academicAffairToken);
    }
}
