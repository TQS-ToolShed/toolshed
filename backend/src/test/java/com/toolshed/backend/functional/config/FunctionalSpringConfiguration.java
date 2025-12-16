package com.toolshed.backend.functional.config;

import org.springframework.boot.test.context.SpringBootTest;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

/**
 * Spring configuration for Cucumber Functional tests.
 * 
 * These tests do not rely on the frontend being up, but they do require
 * the database integration and Spring Context.
 */
@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class FunctionalSpringConfiguration {
    // Spring context configuration for functional tests
}
