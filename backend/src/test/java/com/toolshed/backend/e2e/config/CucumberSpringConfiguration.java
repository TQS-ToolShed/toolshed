package com.toolshed.backend.e2e.config;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

/**
 * Spring configuration for Cucumber E2E tests.
 * 
 * This configuration uses the "local" profile which connects to PostgreSQL on
 * localhost:5432.
 * 
 * Before running these tests, make sure:
 * 1. Database is running (docker compose up db -d)
 * 2. Frontend is running (npm run dev in frontend directory)
 * 
 * Note: Some tests use MockMvc (testing the backend directly) while others use
 * Playwright (testing through the frontend). Both types require the database.
 */
@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, properties = "server.port=8081")
@AutoConfigureMockMvc
@ActiveProfiles("local")
public class CucumberSpringConfiguration {
    // Spring context configuration for Cucumber tests using local PostgreSQL
    // database
}