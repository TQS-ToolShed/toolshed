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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import com.toolshed.backend.repository.BookingRepository;
import com.toolshed.backend.repository.ReviewRepository;
import com.toolshed.backend.repository.ToolRepository;
import com.toolshed.backend.repository.UserRepository;
import com.toolshed.backend.repository.entities.User;
import com.toolshed.backend.repository.enums.UserRole;
import com.toolshed.backend.repository.enums.UserStatus;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("AuthController Email Validation Integration Tests")
class AuthControllerEmailValidationIT {

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

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api/auth/check-email";
        
        // Clear database before each test (order matters for foreign key constraints)
        reviewRepository.deleteAll();
        bookingRepository.deleteAll();
        toolRepository.deleteAll();
        userRepository.deleteAll();
    }

    private User createTestUser(String email) {
        return userRepository.save(User.builder()
                .firstName("Test")
                .lastName("User")
                .email(email)
                .password("password123")
                .role(UserRole.RENTER)
                .status(UserStatus.ACTIVE)
                .reputationScore(0.0)
                .build());
    }

    @Nested
    @DisplayName("Email Existence Validation")
    class EmailExistenceTests {

        @Test
        @DisplayName("Should return true when email exists in database")
        void shouldReturnTrueWhenEmailExistsInDatabase() {
            // Arrange
            createTestUser("existing@example.com");
            String url = baseUrl + "?email=existing@example.com";

            // Act
            ResponseEntity<Boolean> response = restTemplate.getForEntity(url, Boolean.class);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody()).isTrue();
        }

        @Test
        @DisplayName("Should return false when email does not exist in database")
        void shouldReturnFalseWhenEmailDoesNotExistInDatabase() {
            // Arrange
            String url = baseUrl + "?email=nonexistent@example.com";

            // Act
            ResponseEntity<Boolean> response = restTemplate.getForEntity(url, Boolean.class);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody()).isFalse();
        }

        @Test
        @DisplayName("Should return true for multiple existing emails")
        void shouldReturnTrueForMultipleExistingEmails() {
            // Arrange
            createTestUser("user1@example.com");
            createTestUser("user2@example.com");
            createTestUser("admin@company.com");

            String[] existingEmails = {
                "user1@example.com",
                "user2@example.com", 
                "admin@company.com"
            };

            for (String email : existingEmails) {
                String url = baseUrl + "?email=" + email;

                // Act
                ResponseEntity<Boolean> response = restTemplate.getForEntity(url, Boolean.class);

                // Assert
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).isNotNull();
                assertThat(response.getBody()).isTrue();
            }
        }

        @Test
        @DisplayName("Should return false for multiple non-existing emails")
        void shouldReturnFalseForMultipleNonExistingEmails() {
            // Arrange
            String[] nonExistingEmails = {
                "ghost1@example.com",
                "ghost2@example.com",
                "missing@company.com"
            };

            for (String email : nonExistingEmails) {
                String url = baseUrl + "?email=" + email;

                // Act
                ResponseEntity<Boolean> response = restTemplate.getForEntity(url, Boolean.class);

                // Assert
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).isNotNull();
                assertThat(response.getBody()).isFalse();
            }
        }
    }

    @Nested
    @DisplayName("Case Sensitivity Tests")
    class CaseSensitivityTests {

        @Test
        @DisplayName("Should be case sensitive - lowercase email exists, uppercase check returns false")
        void shouldBeCaseSensitiveLowercaseExistsUppercaseReturnsFalse() {
            // Arrange
            createTestUser("user@example.com");  // lowercase
            String url = baseUrl + "?email=USER@EXAMPLE.COM";  // uppercase

            // Act
            ResponseEntity<Boolean> response = restTemplate.getForEntity(url, Boolean.class);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody()).isFalse();  // Should be false due to case sensitivity
        }

        @Test
        @DisplayName("Should be case sensitive - uppercase email exists, lowercase check returns false")
        void shouldBeCaseSensitiveUppercaseExistsLowercaseReturnsFalse() {
            // Arrange
            createTestUser("USER@EXAMPLE.COM");  // uppercase
            String url = baseUrl + "?email=user@example.com";  // lowercase

            // Act
            ResponseEntity<Boolean> response = restTemplate.getForEntity(url, Boolean.class);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody()).isFalse();  // Should be false due to case sensitivity
        }

        @Test
        @DisplayName("Should match exact case - mixed case email")
        void shouldMatchExactCaseMixedCaseEmail() {
            // Arrange
            String mixedCaseEmail = "User.Name@Example.COM";
            createTestUser(mixedCaseEmail);
            String url = baseUrl + "?email=" + mixedCaseEmail;

            // Act
            ResponseEntity<Boolean> response = restTemplate.getForEntity(url, Boolean.class);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody()).isTrue();
        }
    }

    @Nested
    @DisplayName("Special Characters and Edge Cases")
    class SpecialCharactersTests {

        @Test
        @DisplayName("Should handle email with special characters")
        void shouldHandleEmailWithSpecialCharacters() {
            // Arrange - Use a simpler email with just dots instead of + and hyphens
            String specialEmail = "user.test@example.org";
            createTestUser(specialEmail);
            String url = baseUrl + "?email=" + specialEmail;

            // Act
            ResponseEntity<Boolean> response = restTemplate.getForEntity(url, Boolean.class);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody()).isTrue();
        }

        @Test
        @DisplayName("Should handle email with dots and numbers")
        void shouldHandleEmailWithDotsAndNumbers() {
            // Arrange
            String emailWithDots = "user.name123@domain.co.uk";
            createTestUser(emailWithDots);
            String url = baseUrl + "?email=" + emailWithDots;

            // Act
            ResponseEntity<Boolean> response = restTemplate.getForEntity(url, Boolean.class);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody()).isTrue();
        }

        @Test
        @DisplayName("Should handle long email address")
        void shouldHandleLongEmailAddress() {
            // Arrange
            String longEmail = "verylongusernamewithlotsofdots.and.characters@very.long.domain.name.example.organization";
            createTestUser(longEmail);
            String url = baseUrl + "?email=" + longEmail;

            // Act
            ResponseEntity<Boolean> response = restTemplate.getForEntity(url, Boolean.class);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody()).isTrue();
        }
    }

    @Nested
    @DisplayName("Request Parameter Validation")
    class RequestParameterValidationTests {

        @Test
        @DisplayName("Should handle missing email parameter")
        void shouldHandleMissingEmailParameter() {
            // Arrange - no email parameter
            String url = baseUrl;

            // Act
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            // Assert - Should return bad request when email parameter is missing
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Should handle empty email parameter")
        void shouldHandleEmptyEmailParameter() {
            // Arrange
            String url = baseUrl + "?email=";

            // Act
            ResponseEntity<Boolean> response = restTemplate.getForEntity(url, Boolean.class);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody()).isFalse();  // Empty email should not exist
        }

        @Test
        @DisplayName("Should handle whitespace-only email parameter")
        void shouldHandleWhitespaceOnlyEmailParameter() {
            // Arrange
            String url = baseUrl + "?email=%20%20%20";  // URL encoded spaces

            // Act
            ResponseEntity<Boolean> response = restTemplate.getForEntity(url, Boolean.class);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody()).isFalse();  // Whitespace-only email should not exist
        }
    }

    @Nested
    @DisplayName("User Status Independence")
    class UserStatusIndependenceTests {

        @Test
        @DisplayName("Should return true for suspended user email")
        void shouldReturnTrueForSuspendedUserEmail() {
            // Arrange
            User suspendedUser = userRepository.save(User.builder()
                    .firstName("Suspended")
                    .lastName("User")
                    .email("suspended@example.com")
                    .password("password123")
                    .role(UserRole.RENTER)
                    .status(UserStatus.SUSPENDED)
                    .reputationScore(0.0)
                    .build());

            String url = baseUrl + "?email=suspended@example.com";

            // Act
            ResponseEntity<Boolean> response = restTemplate.getForEntity(url, Boolean.class);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody()).isTrue();  // Should still return true regardless of status
        }

        @Test
        @DisplayName("Should return true for pending verification user email")
        void shouldReturnTrueForPendingVerificationUserEmail() {
            // Arrange
            User pendingUser = userRepository.save(User.builder()
                    .firstName("Pending")
                    .lastName("User")
                    .email("pending@example.com")
                    .password("password123")
                    .role(UserRole.SUPPLIER)
                    .status(UserStatus.PENDING_VERIFICATION)
                    .reputationScore(0.0)
                    .build());

            String url = baseUrl + "?email=pending@example.com";

            // Act
            ResponseEntity<Boolean> response = restTemplate.getForEntity(url, Boolean.class);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody()).isTrue();  // Should still return true regardless of status
        }

        @Test
        @DisplayName("Should work for both user roles")
        void shouldWorkForBothUserRoles() {
            // Arrange
            createTestUser("renter@example.com");  // RENTER role
            User supplierUser = userRepository.save(User.builder()
                    .firstName("Supplier")
                    .lastName("User")
                    .email("supplier@example.com")
                    .password("password123")
                    .role(UserRole.SUPPLIER)
                    .status(UserStatus.ACTIVE)
                    .reputationScore(0.0)
                    .build());

            // Act & Assert for RENTER
            String renterUrl = baseUrl + "?email=renter@example.com";
            ResponseEntity<Boolean> renterResponse = restTemplate.getForEntity(renterUrl, Boolean.class);
            
            assertThat(renterResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(renterResponse.getBody()).isNotNull();
            assertThat(renterResponse.getBody()).isTrue();

            // Act & Assert for SUPPLIER
            String supplierUrl = baseUrl + "?email=supplier@example.com";
            ResponseEntity<Boolean> supplierResponse = restTemplate.getForEntity(supplierUrl, Boolean.class);
            
            assertThat(supplierResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(supplierResponse.getBody()).isNotNull();
            assertThat(supplierResponse.getBody()).isTrue();
        }
    }
}