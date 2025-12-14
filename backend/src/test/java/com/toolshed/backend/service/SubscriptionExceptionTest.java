package com.toolshed.backend.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SubscriptionException tests")
class SubscriptionExceptionTest {

    @Test
    @DisplayName("Should create exception with message only")
    void constructor_withMessageOnly_createsException() {
        String message = "Test error message";

        SubscriptionException exception = new SubscriptionException(message);

        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isNull();
    }

    @Test
    @DisplayName("Should create exception with message and cause")
    void constructor_withMessageAndCause_createsException() {
        String message = "Test error message";
        Throwable cause = new RuntimeException("Root cause");

        SubscriptionException exception = new SubscriptionException(message, cause);

        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isEqualTo(cause);
    }
}
