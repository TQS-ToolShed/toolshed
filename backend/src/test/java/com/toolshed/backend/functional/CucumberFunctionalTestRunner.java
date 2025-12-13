package com.toolshed.backend.functional;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

import io.cucumber.junit.platform.engine.Constants;

@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = Constants.GLUE_PROPERTY_NAME, value = "com.toolshed.backend.functional")
@ConfigurationParameter(key = Constants.PLUGIN_PROPERTY_NAME, value = "pretty,html:target/cucumber-reports/html,json:target/cucumber-reports/cucumber.json")
@ConfigurationParameter(key = Constants.FEATURES_PROPERTY_NAME, value = "classpath:features")
public class CucumberFunctionalTestRunner {
    // This class serves as the entry point for running Cucumber functional tests
}