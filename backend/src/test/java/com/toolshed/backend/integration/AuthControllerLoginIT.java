package com.toolshed.backend.integration;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import com.toolshed.backend.dto.LoginRequest;
import com.toolshed.backend.dto.LoginResponse;
import com.toolshed.backend.repository.BookingRepository;
import com.toolshed.backend.repository.ReviewRepository;
import com.toolshed.backend.repository.ToolRepository;
import com.toolshed.backend.repository.UserRepository;
import com.toolshed.backend.repository.entities.User;
import com.toolshed.backend.repository.enums.UserRole;
import com.toolshed.backend.repository.enums.UserStatus;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("AuthController Login Integration Tests")
class AuthControllerLoginIT {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private ToolRepository toolRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    private String baseUrl;
    private HttpHeaders headers;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api/auth/login";
        headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        // Clear database before each test (order matters for foreign key constraints)
        reviewRepository.deleteAll();
        bookingRepository.deleteAll();
        toolRepository.deleteAll();
        userRepository.deleteAll();
    }

    private User createTestUser(String email, String password, UserStatus status, UserRole role) {
        return userRepository.save(User.builder()
                .firstName("Test")
                .lastName("User")
                .email(email)
                .password(password)
                .role(role)
                .status(status)
                .reputationScore(0.0)
                .build());
    }

    @Nested
    @DisplayName("Login Success Scenarios")
    class LoginSuccessTests {

        @Test
        @DisplayName("Should successfully login with valid renter credentials")
        void shouldLoginSuccessfullyWithValidRenterCredentials() {
            // Arrange
            createTestUser("renter@test.com", "password123", UserStatus.ACTIVE, UserRole.RENTER);
            
            LoginRequest loginRequest = LoginRequest.builder()
                    .email("renter@test.com")
                    .password("password123")
                    .build();

            HttpEntity<LoginRequest> request = new HttpEntity<>(loginRequest, headers);

            // Act
            ResponseEntity<LoginResponse> response = restTemplate.postForEntity(baseUrl, request, LoginResponse.class);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getToken()).isNotNull().isNotBlank();
            assertThat(response.getBody().getUser().getEmail()).isEqualTo("renter@test.com");
            assertThat(response.getBody().getUser().getRole()).isEqualTo(UserRole.RENTER);
        }

        @Test
        @DisplayName("Should successfully login with valid supplier credentials")
        void shouldLoginSuccessfullyWithValidSupplierCredentials() {
            // Arrange
            createTestUser("supplier@test.com", "securePass456", UserStatus.ACTIVE, UserRole.SUPPLIER);
            
            LoginRequest loginRequest = LoginRequest.builder()
                    .email("supplier@test.com")
                    .password("securePass456")
                    .build();

            HttpEntity<LoginRequest> request = new HttpEntity<>(loginRequest, headers);

            // Act
            ResponseEntity<LoginResponse> response = restTemplate.postForEntity(baseUrl, request, LoginResponse.class);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getToken()).isNotNull().isNotBlank();
            assertThat(response.getBody().getUser().getEmail()).isEqualTo("supplier@test.com");
            assertThat(response.getBody().getUser().getRole()).isEqualTo(UserRole.SUPPLIER);
        }

        @Test
        @DisplayName("Should successfully login with case sensitive password")
        void shouldLoginSuccessfullyWithCaseSensitivePassword() {
            // Arrange
            createTestUser("user@test.com", "PassWord123", UserStatus.ACTIVE, UserRole.RENTER);
            
            LoginRequest loginRequest = LoginRequest.builder()
                    .email("user@test.com")
                    .password("PassWord123")
                    .build();

            HttpEntity<LoginRequest> request = new HttpEntity<>(loginRequest, headers);

            // Act
            ResponseEntity<LoginResponse> response = restTemplate.postForEntity(baseUrl, request, LoginResponse.class);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getToken()).isNotNull().isNotBlank();
        }
    }

    @Nested
    @DisplayName("Login Authentication Failures")
    class LoginAuthenticationFailureTests {

        @Test
        @DisplayName("Should return 400 when email not found")
        void shouldReturn400WhenEmailNotFound() {
            // Arrange
            LoginRequest loginRequest = LoginRequest.builder()
                    .email("nonexistent@test.com")
                    .password("password123")
                    .build();

            HttpEntity<LoginRequest> request = new HttpEntity<>(loginRequest, headers);

            // Act
            ResponseEntity<String> response = restTemplate.postForEntity(baseUrl, request, String.class);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).contains("Invalid email or password");
        }

        @Test
        @DisplayName("Should return 400 when password is incorrect")
        void shouldReturn400WhenPasswordIsIncorrect() {
            // Arrange
            createTestUser("user@test.com", "correctPassword", UserStatus.ACTIVE, UserRole.RENTER);
            
            LoginRequest loginRequest = LoginRequest.builder()
                    .email("user@test.com")
                    .password("wrongPassword")
                    .build();

            HttpEntity<LoginRequest> request = new HttpEntity<>(loginRequest, headers);

            // Act
            ResponseEntity<String> response = restTemplate.postForEntity(baseUrl, request, String.class);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).contains("Invalid email or password");
        }

        @Test
        @DisplayName("Should return 400 when password case is wrong")
        void shouldReturn400WhenPasswordCaseIsWrong() {
            // Arrange
            createTestUser("user@test.com", "PassWord123", UserStatus.ACTIVE, UserRole.RENTER);
            
            LoginRequest loginRequest = LoginRequest.builder()
                    .email("user@test.com")
                    .password("password123")  // lowercase instead of PassWord123
                    .build();

            HttpEntity<LoginRequest> request = new HttpEntity<>(loginRequest, headers);

            // Act
            ResponseEntity<String> response = restTemplate.postForEntity(baseUrl, request, String.class);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).contains("Invalid email or password");
        }
    }

    @Nested
    @DisplayName("Account Status Validation")
    class AccountStatusValidationTests {

        @Test
        @DisplayName("Should return 400 when account is suspended")
        void shouldReturn400WhenAccountIsSuspended() {
            // Arrange
            createTestUser("suspended@test.com", "password123", UserStatus.SUSPENDED, UserRole.RENTER);
            
            LoginRequest loginRequest = LoginRequest.builder()
                    .email("suspended@test.com")
                    .password("password123")
                    .build();

            HttpEntity<LoginRequest> request = new HttpEntity<>(loginRequest, headers);

            // Act
            ResponseEntity<String> response = restTemplate.postForEntity(baseUrl, request, String.class);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).contains("Account is not active");
        }

        @Test
        @DisplayName("Should return 400 when account is pending verification")
        void shouldReturn400WhenAccountIsPendingVerification() {
            // Arrange
            createTestUser("pending@test.com", "password123", UserStatus.PENDING_VERIFICATION, UserRole.RENTER);
            
            LoginRequest loginRequest = LoginRequest.builder()
                    .email("pending@test.com")
                    .password("password123")
                    .build();

            HttpEntity<LoginRequest> request = new HttpEntity<>(loginRequest, headers);

            // Act
            ResponseEntity<String> response = restTemplate.postForEntity(baseUrl, request, String.class);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).contains("Account is not active");
        }
    }

    @Nested
    @DisplayName("Request Validation")
    class RequestValidationTests {

        @Test
        @DisplayName("Should return 400 when email is empty")
        void shouldReturn400WhenEmailIsEmpty() {
            // Arrange
            LoginRequest loginRequest = LoginRequest.builder()
                    .email("")
                    .password("password123")
                    .build();

            HttpEntity<LoginRequest> request = new HttpEntity<>(loginRequest, headers);

            // Act
            ResponseEntity<String> response = restTemplate.postForEntity(baseUrl, request, String.class);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Should return 400 when password is empty")
        void shouldReturn400WhenPasswordIsEmpty() {
            // Arrange
            LoginRequest loginRequest = LoginRequest.builder()
                    .email("user@test.com")
                    .password("")
                    .build();

            HttpEntity<LoginRequest> request = new HttpEntity<>(loginRequest, headers);

            // Act
            ResponseEntity<String> response = restTemplate.postForEntity(baseUrl, request, String.class);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Should return 400 when email format is invalid")
        void shouldReturn400WhenEmailFormatIsInvalid() {
            // Arrange
            LoginRequest loginRequest = LoginRequest.builder()
                    .email("invalid-email")
                    .password("password123")
                    .build();

            HttpEntity<LoginRequest> request = new HttpEntity<>(loginRequest, headers);

            // Act
            ResponseEntity<String> response = restTemplate.postForEntity(baseUrl, request, String.class);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Should return 400 when request body is null")
        void shouldReturn400WhenRequestBodyIsNull() {
            // Arrange
            HttpEntity<LoginRequest> request = new HttpEntity<>(null, headers);

            // Act
            ResponseEntity<String> response = restTemplate.postForEntity(baseUrl, request, String.class);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("Edge Case Scenarios")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle login with special characters in password")
        void shouldHandleLoginWithSpecialCharactersInPassword() {
            // Arrange
            String specialPassword = "P@ssw0rd!@#$%^&*()";
            createTestUser("special@test.com", specialPassword, UserStatus.ACTIVE, UserRole.RENTER);
            
            LoginRequest loginRequest = LoginRequest.builder()
                    .email("special@test.com")
                    .password(specialPassword)
                    .build();

            HttpEntity<LoginRequest> request = new HttpEntity<>(loginRequest, headers);

            // Act
            ResponseEntity<LoginResponse> response = restTemplate.postForEntity(baseUrl, request, LoginResponse.class);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getToken()).isNotNull().isNotBlank();
        }

        @Test
        @DisplayName("Should handle login with long email")
        void shouldHandleLoginWithLongEmail() {
            // Arrange
            String longEmail = "verylongemailaddress.with.multiple.dots.and.subdomains@very.long.domain.name.example.com";
            createTestUser(longEmail, "password123", UserStatus.ACTIVE, UserRole.SUPPLIER);
            
            LoginRequest loginRequest = LoginRequest.builder()
                    .email(longEmail)
                    .password("password123")
                    .build();

            HttpEntity<LoginRequest> request = new HttpEntity<>(loginRequest, headers);

            // Act
            ResponseEntity<LoginResponse> response = restTemplate.postForEntity(baseUrl, request, LoginResponse.class);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getUser().getEmail()).isEqualTo(longEmail);
        }

        @Test
        @DisplayName("Should handle email case insensitivity correctly")
        void shouldHandleEmailCaseInsensitivityCorrectly() {
            // Arrange - create user with lowercase email
            createTestUser("user@test.com", "password123", UserStatus.ACTIVE, UserRole.RENTER);
            
            // Act - try to login with uppercase email
            LoginRequest loginRequest = LoginRequest.builder()
                    .email("USER@TEST.COM")
                    .password("password123")
                    .build();

            HttpEntity<LoginRequest> request = new HttpEntity<>(loginRequest, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(baseUrl, request, String.class);

            // Assert - should fail because emails are case sensitive in this implementation
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).contains("Invalid email or password");
        }
    }
}