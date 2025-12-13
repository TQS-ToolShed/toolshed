package com.toolshed.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.toolshed.backend.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Email Validation Tests")
class AuthServiceEmailValidationTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("Should return true when email is taken")
    void isEmailTaken_WithExistingEmail_ShouldReturnTrue() {
        // Arrange
        String email = "existing@example.com";
        when(userRepository.existsByEmail(email))
                .thenReturn(true);

        // Act
        boolean result = authService.isEmailTaken(email);

        // Assert
        assertThat(result).isTrue();
        verify(userRepository).existsByEmail(email);
    }

    @Test
    @DisplayName("Should return false when email is not taken")
    void isEmailTaken_WithNewEmail_ShouldReturnFalse() {
        // Arrange
        String email = "new@example.com";
        when(userRepository.existsByEmail(email))
                .thenReturn(false);

        // Act
        boolean result = authService.isEmailTaken(email);

        // Assert
        assertThat(result).isFalse();
        verify(userRepository).existsByEmail(email);
    }

    @Test
    @DisplayName("Should handle case-sensitive email check")
    void isEmailTaken_WithDifferentCase_ShouldUseExactCase() {
        // Arrange
        String email = "Test@Example.COM";
        when(userRepository.existsByEmail(email))
                .thenReturn(false);

        // Act
        boolean result = authService.isEmailTaken(email);

        // Assert
        assertThat(result).isFalse();
        verify(userRepository).existsByEmail(email);
    }

    @Test
    @DisplayName("Should handle email with special characters")
    void isEmailTaken_WithSpecialCharacters_ShouldWork() {
        // Arrange
        String email = "user+test@example-domain.co.uk";
        when(userRepository.existsByEmail(email))
                .thenReturn(true);

        // Act
        boolean result = authService.isEmailTaken(email);

        // Assert
        assertThat(result).isTrue();
        verify(userRepository).existsByEmail(email);
    }

    @Test
    @DisplayName("Should handle empty email")
    void isEmailTaken_WithEmptyEmail_ShouldWork() {
        // Arrange
        String email = "";
        when(userRepository.existsByEmail(email))
                .thenReturn(false);

        // Act
        boolean result = authService.isEmailTaken(email);

        // Assert
        assertThat(result).isFalse();
        verify(userRepository).existsByEmail(email);
    }

    @Test
    @DisplayName("Should handle email with whitespace")
    void isEmailTaken_WithWhitespace_ShouldPreserveWhitespace() {
        // Arrange
        String email = " user@example.com ";
        when(userRepository.existsByEmail(email))
                .thenReturn(false);

        // Act
        boolean result = authService.isEmailTaken(email);

        // Assert
        assertThat(result).isFalse();
        verify(userRepository).existsByEmail(email);
    }
}