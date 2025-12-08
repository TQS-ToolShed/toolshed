package com.toolshed.backend.functional.config;

import org.springframework.test.context.ContextConfiguration;

import io.cucumber.spring.CucumberContextConfiguration;

@CucumberContextConfiguration
@ContextConfiguration(classes = CucumberSpringConfiguration.class)
public class CucumberSpringConfiguration {
    // This class provides a minimal Spring context configuration for Cucumber tests
    // We don't need @SpringBootTest here since we're testing against the running application
}