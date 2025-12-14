package com.toolshed.backend.service;

import java.io.IOException;

/**
 * Simple HTTP client interface for making GET requests.
 * Provides a basic abstraction for HTTP operations, allowing for easy
 * mocking and testing of components that depend on external HTTP calls.
 */
public interface ISimpleHttpClient {
    String doHttpGet(String url) throws IOException;
}
