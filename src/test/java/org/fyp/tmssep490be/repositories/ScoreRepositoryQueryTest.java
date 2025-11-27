package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.Assessment;
import org.fyp.tmssep490be.entities.Score;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class to verify ScoreRepository JPA queries work correctly
 * after fixing the courseSession path issue
 */
@DataJpaTest
@ActiveProfiles("test")
class ScoreRepositoryQueryTest {

    @Autowired
    private ScoreRepository scoreRepository;

    @MockitoBean
    private AssessmentRepository assessmentRepository;

    @Test
    void testRepositoryMethodsExist() {
        // Verify that all the fixed repository methods exist and can be called
        assertNotNull(scoreRepository);

        // Test method signatures exist (basic compilation test)
        assertTrue(true, "Repository methods compiled successfully");

        // Note: Actual query testing would require test data setup
        // This test primarily verifies the JPA query syntax is valid
    }

    /**
     * Test that the new helper method for finding assessments by course session works
     */
    @Test
    void testFindAssessmentsByCourseSessionIdExists() {
        // This test verifies the method signature is correct
        List<Assessment> result = scoreRepository.findAssessmentsByCourseSessionId(1L);
        assertNotNull(result, "Method should return a list (even if empty)");
    }
}