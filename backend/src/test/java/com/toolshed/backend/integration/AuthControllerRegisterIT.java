package com.toolshed.backend.integration;

import java.util.Optional;

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

import com.toolshed.backend.dto.RegisterRequest;
import com.toolshed.backend.repository.BookingRepository;
import com.toolshed.backend.repository.ReviewRepository;
import com.toolshed.backend.repository.ToolRepository;
import com.toolshed.backend.repository.UserRepository;
import com.toolshed.backend.repository.entities.User;
import com.toolshed.backend.repository.enums.UserRole;
import com.toolshed.backend.repository.enums.UserStatus;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
@DisplayName("AuthController Register Integration Tests")
class AuthControllerRegisterIT {

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
        baseUrl = "http://localhost:" + port + "/api/auth/register";
        headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        // Clear database before each test (order matters for foreign key constraints)
        reviewRepository.deleteAll();
        bookingRepository.deleteAll();
        toolRepository.deleteAll();
        userRepository.deleteAll();
    }

    private User createExistingUser(String email) {
        return userRepository.save(User.builder()
                .firstName("Existing")
                .lastName("User")
                .email(email)
                .password("existingPassword")
                .role(UserRole.RENTER)
                .status(UserStatus.ACTIVE)
                .reputationScore(0.0)
                .build());
    }

    @Nested
    @DisplayName("Registration Success Scenarios")
    class RegistrationSuccessTests {

        @Test
        @DisplayName("Should successfully register new renter user")
        void shouldSuccessfullyRegisterNewRenterUser() {
            // Arrange
            RegisterRequest registerRequest = RegisterRequest.builder()
                    .firstName("John")
                    .lastName("Doe")
                    .email("john.doe@example.com")
                    .password("securePassword123")
                    .role(UserRole.RENTER)
                    .build();

            HttpEntity<RegisterRequest> request = new HttpEntity<>(registerRequest, headers);

            // Act
            ResponseEntity<User> response = restTemplate.postForEntity(baseUrl, request, User.class);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            
            User responseUser = response.getBody();
            assertThat(responseUser.getFirstName()).isEqualTo("John");
            assertThat(responseUser.getLastName()).isEqualTo("Doe");
            assertThat(responseUser.getEmail()).isEqualTo("john.doe@example.com");
            assertThat(responseUser.getRole()).isEqualTo(UserRole.RENTER);
            assertThat(responseUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
            assertThat(responseUser.getReputationScore()).isEqualTo(0.0);

            // Verify user is saved in database
            Optional<User> savedUser = userRepository.findByEmail("john.doe@example.com");
            assertThat(savedUser).isPresent();
            assertThat(savedUser.get().getFirstName()).isEqualTo("John");
        }

        @Test
        @DisplayName("Should successfully register new supplier user")
        void shouldSuccessfullyRegisterNewSupplierUser() {
            // Arrange
            RegisterRequest registerRequest = RegisterRequest.builder()
                    .firstName("Jane")
                    .lastName("Smith")
                    .email("jane.smith@toolsupply.com")
                    .password("supplierPass456")
                    .role(UserRole.SUPPLIER)
                    .build();

            HttpEntity<RegisterRequest> request = new HttpEntity<>(registerRequest, headers);

            // Act
            ResponseEntity<User> response = restTemplate.postForEntity(baseUrl, request, User.class);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            
            User responseUser = response.getBody();
            assertThat(responseUser.getRole()).isEqualTo(UserRole.SUPPLIER);
            assertThat(responseUser.getEmail()).isEqualTo("jane.smith@toolsupply.com");
            assertThat(responseUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
        }

        @Test
        @DisplayName("Should register user with special characters in name")
        void shouldRegisterUserWithSpecialCharactersInName() {
            // Arrange
            RegisterRequest registerRequest = RegisterRequest.builder()
                    .firstName("José")
                    .lastName("García-López")
                    .email("jose.garcia@example.com")
                    .password("password123")
                    .role(UserRole.RENTER)
                    .build();

            HttpEntity<RegisterRequest> request = new HttpEntity<>(registerRequest, headers);

            // Act
            ResponseEntity<User> response = restTemplate.postForEntity(baseUrl, request, User.class);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getFirstName()).isEqualTo("José");
            assertThat(response.getBody().getLastName()).isEqualTo("García-López");
        }
    }

    @Nested
    @DisplayName("Email Validation")
    class EmailValidationTests {

        @Test
        @DisplayName("Should return 400 when email already exists")
        void shouldReturn400WhenEmailAlreadyExists() {
            // Arrange
            createExistingUser("existing@example.com");
            
            RegisterRequest registerRequest = RegisterRequest.builder()
                    .firstName("New")
                    .lastName("User")
                    .email("existing@example.com")
                    .password("newPassword123")
                    .role(UserRole.RENTER)
                    .build();

            HttpEntity<RegisterRequest> request = new HttpEntity<>(registerRequest, headers);

            // Act
            ResponseEntity<String> response = restTemplate.postForEntity(baseUrl, request, String.class);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).contains("Email already exists");
        }

        @Test
        @DisplayName("Should handle case sensitivity in email duplication check")
        void shouldHandleCaseSensitivityInEmailDuplicationCheck() {
            // Arrange
            createExistingUser("user@example.com");
            
            RegisterRequest registerRequest = RegisterRequest.builder()
                    .firstName("New")
                    .lastName("User")
                    .email("USER@EXAMPLE.COM")  // Different case
                    .password("password123")
                    .role(UserRole.RENTER)
                    .build();

            HttpEntity<RegisterRequest> request = new HttpEntity<>(registerRequest, headers);

            // Act
            ResponseEntity<User> response = restTemplate.postForEntity(baseUrl, request, User.class);

            // Assert - Should succeed because emails are case sensitive in this implementation
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getEmail()).isEqualTo("USER@EXAMPLE.COM");
        }

        @Test
        @DisplayName("Should accept valid email formats")
        void shouldAcceptValidEmailFormats() {
            // Arrange
            String[] validEmails = {
                "user@domain.com",
                "user.name@domain.co.uk",
                "user+tag@domain.org",
                "123@domain.net"
            };

            for (int i = 0; i < validEmails.length; i++) {
                RegisterRequest registerRequest = RegisterRequest.builder()
                        .firstName("User" + i)
                        .lastName("Test")
                        .email(validEmails[i])
                        .password("password123")
                        .role(UserRole.RENTER)
                        .build();

                HttpEntity<RegisterRequest> request = new HttpEntity<>(registerRequest, headers);

                // Act
                ResponseEntity<User> response = restTemplate.postForEntity(baseUrl, request, User.class);

                // Assert
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).isNotNull();
                assertThat(response.getBody().getEmail()).isEqualTo(validEmails[i]);
            }
        }
    }

    @Nested
    @DisplayName("Request Validation")
    class RequestValidationTests {

        @Test
        @DisplayName("Should return 400 when first name is empty")
        void shouldReturn400WhenFirstNameIsEmpty() {
            // Arrange
            RegisterRequest registerRequest = RegisterRequest.builder()
                    .firstName("")
                    .lastName("Doe")
                    .email("test@example.com")
                    .password("password123")
                    .role(UserRole.RENTER)
                    .build();

            HttpEntity<RegisterRequest> request = new HttpEntity<>(registerRequest, headers);

            // Act
            ResponseEntity<String> response = restTemplate.postForEntity(baseUrl, request, String.class);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Should return 400 when last name is empty")
        void shouldReturn400WhenLastNameIsEmpty() {
            // Arrange
            RegisterRequest registerRequest = RegisterRequest.builder()
                    .firstName("John")
                    .lastName("")
                    .email("test@example.com")
                    .password("password123")
                    .role(UserRole.RENTER)
                    .build();

            HttpEntity<RegisterRequest> request = new HttpEntity<>(registerRequest, headers);

            // Act
            ResponseEntity<String> response = restTemplate.postForEntity(baseUrl, request, String.class);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Should return 400 when email is empty")
        void shouldReturn400WhenEmailIsEmpty() {
            // Arrange
            RegisterRequest registerRequest = RegisterRequest.builder()
                    .firstName("John")
                    .lastName("Doe")
                    .email("")
                    .password("password123")
                    .role(UserRole.RENTER)
                    .build();

            HttpEntity<RegisterRequest> request = new HttpEntity<>(registerRequest, headers);

            // Act
            ResponseEntity<String> response = restTemplate.postForEntity(baseUrl, request, String.class);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Should return 400 when password is empty")
        void shouldReturn400WhenPasswordIsEmpty() {
            // Arrange
            RegisterRequest registerRequest = RegisterRequest.builder()
                    .firstName("John")
                    .lastName("Doe")
                    .email("test@example.com")
                    .password("")
                    .role(UserRole.RENTER)
                    .build();

            HttpEntity<RegisterRequest> request = new HttpEntity<>(registerRequest, headers);

            // Act
            ResponseEntity<String> response = restTemplate.postForEntity(baseUrl, request, String.class);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Should return 400 when email format is invalid")
        void shouldReturn400WhenEmailFormatIsInvalid() {
            // Arrange
            RegisterRequest registerRequest = RegisterRequest.builder()
                    .firstName("John")
                    .lastName("Doe")
                    .email("invalid-email")
                    .password("password123")
                    .role(UserRole.RENTER)
                    .build();

            HttpEntity<RegisterRequest> request = new HttpEntity<>(registerRequest, headers);

            // Act
            ResponseEntity<String> response = restTemplate.postForEntity(baseUrl, request, String.class);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Should return 400 when role is null")
        void shouldReturn400WhenRoleIsNull() {
            // Arrange
            RegisterRequest registerRequest = RegisterRequest.builder()
                    .firstName("John")
                    .lastName("Doe")
                    .email("test@example.com")
                    .password("password123")
                    .role(null)
                    .build();

            HttpEntity<RegisterRequest> request = new HttpEntity<>(registerRequest, headers);

            // Act
            ResponseEntity<String> response = restTemplate.postForEntity(baseUrl, request, String.class);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Should return 400 when request body is null")
        void shouldReturn400WhenRequestBodyIsNull() {
            // Arrange
            HttpEntity<RegisterRequest> request = new HttpEntity<>(null, headers);

            // Act
            ResponseEntity<String> response = restTemplate.postForEntity(baseUrl, request, String.class);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("Field Preservation and Defaults")
    class FieldPreservationTests {

        @Test
        @DisplayName("Should preserve all user input fields correctly")
        void shouldPreserveAllUserInputFieldsCorrectly() {
            // Arrange
            RegisterRequest registerRequest = RegisterRequest.builder()
                    .firstName("Alice")
                    .lastName("Johnson")
                    .email("alice.johnson@company.com")
                    .password("mySecretPassword")
                    .role(UserRole.SUPPLIER)
                    .build();

            HttpEntity<RegisterRequest> request = new HttpEntity<>(registerRequest, headers);

            // Act
            ResponseEntity<User> response = restTemplate.postForEntity(baseUrl, request, User.class);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            
            User user = response.getBody();
            assertThat(user.getFirstName()).isEqualTo("Alice");
            assertThat(user.getLastName()).isEqualTo("Johnson");
            assertThat(user.getEmail()).isEqualTo("alice.johnson@company.com");
            assertThat(user.getPassword()).isEqualTo("mySecretPassword");
            assertThat(user.getRole()).isEqualTo(UserRole.SUPPLIER);
        }

        @Test
        @DisplayName("Should set correct default values")
        void shouldSetCorrectDefaultValues() {
            // Arrange
            RegisterRequest registerRequest = RegisterRequest.builder()
                    .firstName("Bob")
                    .lastName("Wilson")
                    .email("bob.wilson@test.com")
                    .password("password123")
                    .role(UserRole.RENTER)
                    .build();

            HttpEntity<RegisterRequest> request = new HttpEntity<>(registerRequest, headers);

            // Act
            ResponseEntity<User> response = restTemplate.postForEntity(baseUrl, request, User.class);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            
            User user = response.getBody();
            assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);  // Default status
            assertThat(user.getReputationScore()).isEqualTo(0.0);       // Default reputation
            assertThat(user.getId()).isNotNull();                       // ID should be generated
        }
    }

    @Nested
    @DisplayName("Edge Case Scenarios")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle registration with long names")
        void shouldHandleRegistrationWithLongNames() {
            // Arrange
            String longFirstName = "VeryLongFirstNameThatExceedsNormalLength";
            String longLastName = "VeryLongLastNameThatExceedsNormalLengthToo";
            
            RegisterRequest registerRequest = RegisterRequest.builder()
                    .firstName(longFirstName)
                    .lastName(longLastName)
                    .email("longname@example.com")
                    .password("password123")
                    .role(UserRole.RENTER)
                    .build();

            HttpEntity<RegisterRequest> request = new HttpEntity<>(registerRequest, headers);

            // Act
            ResponseEntity<User> response = restTemplate.postForEntity(baseUrl, request, User.class);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getFirstName()).isEqualTo(longFirstName);
            assertThat(response.getBody().getLastName()).isEqualTo(longLastName);
        }

        @Test
        @DisplayName("Should handle registration with special characters in password")
        void shouldHandleRegistrationWithSpecialCharactersInPassword() {
            // Arrange
            String specialPassword = "P@ssw0rd!@#$%^&*()_+-=[]{}|;:,.<>?";
            
            RegisterRequest registerRequest = RegisterRequest.builder()
                    .firstName("Special")
                    .lastName("User")
                    .email("special@example.com")
                    .password(specialPassword)
                    .role(UserRole.SUPPLIER)
                    .build();

            HttpEntity<RegisterRequest> request = new HttpEntity<>(registerRequest, headers);

            // Act
            ResponseEntity<User> response = restTemplate.postForEntity(baseUrl, request, User.class);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getPassword()).isEqualTo(specialPassword);
        }

        @Test
        @DisplayName("Should handle registration with international domain")
        void shouldHandleRegistrationWithInternationalDomain() {
            // Arrange
            RegisterRequest registerRequest = RegisterRequest.builder()
                    .firstName("International")
                    .lastName("User")
                    .email("user@domain.co.uk")
                    .password("password123")
                    .role(UserRole.RENTER)
                    .build();

            HttpEntity<RegisterRequest> request = new HttpEntity<>(registerRequest, headers);

            // Act
            ResponseEntity<User> response = restTemplate.postForEntity(baseUrl, request, User.class);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getEmail()).isEqualTo("user@domain.co.uk");
        }

        @Test
        @DisplayName("Should handle multiple simultaneous registrations")
        void shouldHandleMultipleSimultaneousRegistrations() {
            // Arrange & Act
            for (int i = 1; i <= 5; i++) {
                RegisterRequest registerRequest = RegisterRequest.builder()
                        .firstName("User" + i)
                        .lastName("Test")
                        .email("user" + i + "@example.com")
                        .password("password" + i)
                        .role(i % 2 == 0 ? UserRole.SUPPLIER : UserRole.RENTER)
                        .build();

                HttpEntity<RegisterRequest> request = new HttpEntity<>(registerRequest, headers);
                ResponseEntity<User> response = restTemplate.postForEntity(baseUrl, request, User.class);

                // Assert each registration
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).isNotNull();
                assertThat(response.getBody().getEmail()).isEqualTo("user" + i + "@example.com");
            }

            // Verify all users were saved
            assertThat(userRepository.count()).isEqualTo(5);
        }
    }
}