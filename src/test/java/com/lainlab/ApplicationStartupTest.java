package com.lainlab;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Smoke test to verify the application starts successfully.
 * This is a quick check for CI/CD pipelines to ensure basic functionality.
 */
@MicronautTest
class ApplicationStartupTest {

    @Test
    void applicationStarts() {
        // If we reach here, the application context was successfully created
        assertNotNull(ApplicationStartupTest.class, "Application context loaded successfully");
    }
}

