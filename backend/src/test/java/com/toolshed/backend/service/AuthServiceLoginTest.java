package com.toolshed.backend.service;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.toolshed.backend.dto.LoginRequest;
import com.toolshed.backend.dto.LoginResponse;
import com.toolshed.backend.repository.UserRepository;
import com.toolshed.backend.repository.entities.User;
import com.toolshed.backend.repository.enums.UserRole;
import com.toolshed.backend.repository.enums.UserStatus;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Login Tests")
class AuthServiceLoginTest {

        @Mock
        private UserRepository userRepository;

        @InjectMocks
        private AuthService authService;

        private User testUser;
        private LoginRequest loginRequest;

        @BeforeEach
        void setUp() {
                // Setup test user
                testUser = User.builder()
                                .id(UUID.randomUUID())
                                .firstName("John")
                                .lastName("Doe")
                                .email("test@example.com")
                                .password("password123")
                                .role(UserRole.RENTER)
                                .status(UserStatus.ACTIVE)
                                .reputationScore(0.0)
                                .build();

                // Setup login request
                loginRequest = LoginRequest.builder()
                                .email("test@example.com")
                                .password("password123")
                                .build();
        }

        @Test
        @DisplayName("Should login successfully with valid credentials")
        void login_WithValidCredentials_ShouldReturnLoginResponse() {
                // Arrange
                when(userRepository.findByEmail(loginRequest.getEmail()))
                                .thenReturn(Optional.of(testUser));

                // Act
                LoginResponse response = authService.login(loginRequest);

                // Assert
                assertThat(response).isNotNull();
                assertThat(response.getToken()).isNotNull();
                assertThat(response.getUser()).isEqualTo(testUser);

                // Verify interactions
                verify(userRepository).findByEmail(loginRequest.getEmail());
        }

        @Test
        @DisplayName("Should throw exception when email is not found")
        void login_WithNonExistentEmail_ShouldThrowException() {
                // Arrange
                when(userRepository.findByEmail(loginRequest.getEmail()))
                                .thenReturn(Optional.empty());

                // Act & Assert
                assertThatThrownBy(() -> authService.login(loginRequest))
                                .isInstanceOf(ResponseStatusException.class)
                                .hasMessageContaining("Invalid credentials")
                                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                                .isEqualTo(HttpStatus.UNAUTHORIZED);

                // Verify
                verify(userRepository).findByEmail(loginRequest.getEmail());
        }

        @Test
        @DisplayName("Should throw exception when password is incorrect")
        void login_WithIncorrectPassword_ShouldThrowException() {
                // Arrange
                loginRequest.setPassword("wrongPassword");
                when(userRepository.findByEmail(loginRequest.getEmail()))
                                .thenReturn(Optional.of(testUser));

                // Act & Assert
                assertThatThrownBy(() -> authService.login(loginRequest))
                                .isInstanceOf(ResponseStatusException.class)
                                .hasMessageContaining("Invalid credentials")
                                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                                .isEqualTo(HttpStatus.UNAUTHORIZED);

                // Verify
                verify(userRepository).findByEmail(loginRequest.getEmail());
        }

        @Test
        @DisplayName("Should throw exception when user account is pending verification")
        void login_WithPendingVerificationAccount_ShouldThrowException() {
                // Arrange
                testUser.setStatus(UserStatus.PENDING_VERIFICATION);
                when(userRepository.findByEmail(loginRequest.getEmail()))
                                .thenReturn(Optional.of(testUser));

                // Act & Assert
                assertThatThrownBy(() -> authService.login(loginRequest))
                                .isInstanceOf(ResponseStatusException.class)
                                .hasMessageContaining("Account is not verified")
                                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                                .isEqualTo(HttpStatus.FORBIDDEN);

                // Verify
                verify(userRepository).findByEmail(loginRequest.getEmail());
        }

        @Test
        @DisplayName("Should throw exception when user account is suspended")
        void login_WithSuspendedAccount_ShouldThrowException() {
                // Arrange
                testUser.setStatus(UserStatus.SUSPENDED);
                when(userRepository.findByEmail(loginRequest.getEmail()))
                                .thenReturn(Optional.of(testUser));

                // Act & Assert
                assertThatThrownBy(() -> authService.login(loginRequest))
                                .isInstanceOf(ResponseStatusException.class)
                                .hasMessageContaining("Account is suspended")
                                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                                .isEqualTo(HttpStatus.FORBIDDEN);

                // Verify
                verify(userRepository).findByEmail(loginRequest.getEmail());
        }

        @Test
        @DisplayName("Should generate unique token for each login")
        void login_MultipleTimes_ShouldGenerateUniqueTokens() {
                // Arrange
                when(userRepository.findByEmail(loginRequest.getEmail()))
                                .thenReturn(Optional.of(testUser));

                // Act
                LoginResponse response1 = authService.login(loginRequest);
                LoginResponse response2 = authService.login(loginRequest);

                // Assert
                assertThat(response1.getToken()).isNotEqualTo(response2.getToken());
                assertThat(response1.getUser()).isEqualTo(testUser);
                assertThat(response2.getUser()).isEqualTo(testUser);

                // Verify
                verify(userRepository, times(2)).findByEmail(loginRequest.getEmail());
        }

        @Test
        @DisplayName("Should handle case-sensitive email correctly")
        void login_WithDifferentCaseEmail_ShouldUseExactCase() {
                // Arrange
                loginRequest.setEmail("Test@Example.COM");
                when(userRepository.findByEmail("Test@Example.COM"))
                                .thenReturn(Optional.empty());

                // Act & Assert
                assertThatThrownBy(() -> authService.login(loginRequest))
                                .isInstanceOf(ResponseStatusException.class);

                // Verify exact case was used
                verify(userRepository).findByEmail("Test@Example.COM");
        }

        @Test
        @DisplayName("Should handle special characters in password")
        void login_WithSpecialCharactersInPassword_ShouldWork() {
                // Arrange
                String specialPassword = "P@ssw0rd!#$%^&*()";
                testUser.setPassword(specialPassword);
                loginRequest.setPassword(specialPassword);

                when(userRepository.findByEmail(loginRequest.getEmail()))
                                .thenReturn(Optional.of(testUser));

                // Act
                LoginResponse response = authService.login(loginRequest);

                // Assert
                assertThat(response).isNotNull();
                assertThat(response.getToken()).isNotNull();
        }

        @Test
        @DisplayName("Should handle very long password")
        void login_WithVeryLongPassword_ShouldWork() {
                // Arrange
                String longPassword = "a".repeat(256);
                testUser.setPassword(longPassword);
                loginRequest.setPassword(longPassword);

                when(userRepository.findByEmail(loginRequest.getEmail()))
                                .thenReturn(Optional.of(testUser));

                // Act
                LoginResponse response = authService.login(loginRequest);

                // Assert
                assertThat(response).isNotNull();
        }

        @Test
        @DisplayName("Should handle whitespace in email")
        void login_WithWhitespaceInEmail_ShouldUseAsIs() {
                // Arrange
                loginRequest.setEmail(" test@example.com ");
                when(userRepository.findByEmail(" test@example.com "))
                                .thenReturn(Optional.empty());

                // Act & Assert
                assertThatThrownBy(() -> authService.login(loginRequest))
                                .isInstanceOf(ResponseStatusException.class);

                // Verify email with whitespace was used as-is
                verify(userRepository).findByEmail(" test@example.com ");
        }

        @Test
        @DisplayName("Should return user with all properties intact")
        void login_ShouldReturnCompleteUser() {
                // Arrange
                when(userRepository.findByEmail(loginRequest.getEmail()))
                                .thenReturn(Optional.of(testUser));

                // Act
                LoginResponse response = authService.login(loginRequest);

                // Assert
                User returnedUser = response.getUser();
                assertThat(returnedUser.getId()).isEqualTo(testUser.getId());
                assertThat(returnedUser.getFirstName()).isEqualTo(testUser.getFirstName());
                assertThat(returnedUser.getLastName()).isEqualTo(testUser.getLastName());
                assertThat(returnedUser.getEmail()).isEqualTo(testUser.getEmail());
                assertThat(returnedUser.getRole()).isEqualTo(testUser.getRole());
                assertThat(returnedUser.getStatus()).isEqualTo(testUser.getStatus());
        }

        @Test
        @DisplayName("Should work with SUPPLIER role")
        void login_WithSupplierRole_ShouldWork() {
                // Arrange
                testUser.setRole(UserRole.SUPPLIER);
                when(userRepository.findByEmail(loginRequest.getEmail()))
                                .thenReturn(Optional.of(testUser));

                // Act
                LoginResponse response = authService.login(loginRequest);

                // Assert
                assertThat(response.getUser().getRole()).isEqualTo(UserRole.SUPPLIER);
        }

        @Test
        @DisplayName("Should work with different roles")
        void login_WithDifferentRoles_ShouldWork() {
                // Arrange - Test with SUPPLIER role
                testUser.setRole(UserRole.SUPPLIER);
                when(userRepository.findByEmail(loginRequest.getEmail()))
                                .thenReturn(Optional.of(testUser));

                // Act
                LoginResponse response = authService.login(loginRequest);

                // Assert
                assertThat(response.getUser().getRole()).isEqualTo(UserRole.SUPPLIER);
        }

        @Test
        @DisplayName("Should handle empty password")
        void login_WithEmptyPassword_ShouldFail() {
                // Arrange
                loginRequest.setPassword("");
                when(userRepository.findByEmail(loginRequest.getEmail()))
                                .thenReturn(Optional.of(testUser));

                // Act & Assert
                assertThatThrownBy(() -> authService.login(loginRequest))
                                .isInstanceOf(ResponseStatusException.class)
                                .hasMessageContaining("Invalid credentials");
        }

        @Test
        @DisplayName("Should handle null password gracefully")
        void login_WithNullPassword_ShouldFail() {
                // Arrange
                loginRequest.setPassword(null);
                when(userRepository.findByEmail(loginRequest.getEmail()))
                                .thenReturn(Optional.of(testUser));

                // Act & Assert
                assertThatThrownBy(() -> authService.login(loginRequest))
                                .isInstanceOf(ResponseStatusException.class)
                                .hasMessageContaining("Invalid credentials");
        }

        @Test
        @DisplayName("Should validate token is not null or empty")
        void login_ShouldGenerateValidToken() {
                // Arrange
                when(userRepository.findByEmail(loginRequest.getEmail()))
                                .thenReturn(Optional.of(testUser));

                // Act
                LoginResponse response = authService.login(loginRequest);

                // Assert
                assertThat(response.getToken()).isNotNull();
                assertThat(response.getToken()).isNotEmpty();
                assertThat(response.getToken().length()).isGreaterThan(10); // UUID should be longer than 10 chars
        }

        @Test
        @DisplayName("Should handle password with only whitespace")
        void login_WithWhitespaceOnlyPassword_ShouldFail() {
                // Arrange
                loginRequest.setPassword("   ");
                when(userRepository.findByEmail(loginRequest.getEmail()))
                                .thenReturn(Optional.of(testUser));

                // Act & Assert
                assertThatThrownBy(() -> authService.login(loginRequest))
                                .isInstanceOf(ResponseStatusException.class)
                                .hasMessageContaining("Invalid credentials");
        }
}