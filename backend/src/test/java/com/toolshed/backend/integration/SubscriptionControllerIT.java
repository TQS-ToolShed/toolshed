package com.toolshed.backend.integration;

import java.time.LocalDateTime;
import java.util.UUID;
import com.toolshed.backend.repository.BookingRepository;
import com.toolshed.backend.repository.PayoutRepository;
import com.toolshed.backend.repository.ReviewRepository;
import com.toolshed.backend.repository.ToolRepository;
import com.toolshed.backend.repository.UserRepository;
import com.toolshed.backend.repository.entities.User;
import com.toolshed.backend.repository.enums.SubscriptionTier;
import com.toolshed.backend.repository.enums.UserRole;
import com.toolshed.backend.repository.enums.UserStatus;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import io.restassured.RestAssured;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SubscriptionControllerIT {

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private ToolRepository toolRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private PayoutRepository payoutRepository;

    private User freeUser;
    private User proUser;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;

        // Clean up in correct order to avoid FK constraint violations
        cleanupDatabase();

        freeUser = User.builder()
                .firstName("Free")
                .lastName("User")
                .email("free@integration-test.com")
                .password("password123")
                .role(UserRole.RENTER)
                .status(UserStatus.ACTIVE)
                .reputationScore(4.5)
                .walletBalance(0.0)
                .subscriptionTier(SubscriptionTier.FREE)
                .build();
        freeUser = userRepository.save(freeUser);

        proUser = User.builder()
                .firstName("Pro")
                .lastName("User")
                .email("pro@integration-test.com")
                .password("password123")
                .role(UserRole.RENTER)
                .status(UserStatus.ACTIVE)
                .reputationScore(5.0)
                .walletBalance(100.0)
                .subscriptionTier(SubscriptionTier.PRO)
                .subscriptionStart(LocalDateTime.now().minusDays(15))
                .subscriptionEnd(LocalDateTime.now().plusDays(15))
                .stripeSubscriptionId("sub_test_integration")
                .build();
        proUser = userRepository.save(proUser);
    }

    @AfterEach
    void tearDown() {
        cleanupDatabase();
    }

    private void cleanupDatabase() {
        // Delete in correct order: dependent entities first
        payoutRepository.deleteAll();
        reviewRepository.deleteAll();
        bookingRepository.deleteAll();
        toolRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Nested
    @DisplayName("GET /api/subscriptions/status/{userId} Integration Tests")
    class GetSubscriptionStatusIntegrationTests {

        @Test
        @DisplayName("Should return Pro status for user with active subscription")
        void getStatus_proUser_returnsProStatus() {
            given()
                    .pathParam("userId", proUser.getId())
                    .when()
                    .get("/api/subscriptions/status/{userId}")
                    .then()
                    .statusCode(HttpStatus.OK.value())
                    .body("tier", equalTo("PRO"))
                    .body("active", equalTo(true))
                    .body("discountPercentage", equalTo(5.0f));
        }

        @Test
        @DisplayName("Should return Free status for user without subscription")
        void getStatus_freeUser_returnsFreeStatus() {
            given()
                    .pathParam("userId", freeUser.getId())
                    .when()
                    .get("/api/subscriptions/status/{userId}")
                    .then()
                    .statusCode(HttpStatus.OK.value())
                    .body("tier", equalTo("FREE"))
                    .body("active", equalTo(false))
                    .body("discountPercentage", equalTo(0.0f));
        }

        @Test
        @DisplayName("Should return 404 for non-existent user")
        void getStatus_unknownUser_returns404() {
            UUID unknownId = UUID.randomUUID();

            given()
                    .pathParam("userId", unknownId)
                    .when()
                    .get("/api/subscriptions/status/{userId}")
                    .then()
                    .statusCode(HttpStatus.NOT_FOUND.value());
        }
    }

    @Nested
    @DisplayName("Subscription Activation Integration Tests")
    class ActivationIntegrationTests {

        @Test
        @DisplayName("Should activate Pro subscription and persist in database")
        void activateSubscription_persistsInDatabase() {
            // Activate subscription
            given()
                    .pathParam("userId", freeUser.getId())
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body("{\"stripeSubscriptionId\": \"sub_new_integration\"}")
                    .when()
                    .post("/api/subscriptions/activate/{userId}")
                    .then()
                    .statusCode(HttpStatus.OK.value())
                    .body("tier", equalTo("PRO"))
                    .body("active", equalTo(true));

            // Verify persistence
            User updatedUser = userRepository.findById(freeUser.getId()).orElseThrow();
            assertThat(updatedUser.getSubscriptionTier()).isEqualTo(SubscriptionTier.PRO);
            assertThat(updatedUser.getStripeSubscriptionId()).isEqualTo("sub_new_integration");
            assertThat(updatedUser.getSubscriptionStart()).isNotNull();
            // subscriptionEnd is null for lifetime Pro (one-time payment)
            assertThat(updatedUser.getSubscriptionEnd()).isNull();
        }
    }

    @Nested
    @DisplayName("Subscription Cancellation Integration Tests")
    class CancellationIntegrationTests {

        @Test
        @DisplayName("Should return 400 when cancelling non-Pro user subscription")
        void cancelSubscription_freeUser_returns400() {
            given()
                    .pathParam("userId", freeUser.getId())
                    .when()
                    .delete("/api/subscriptions/{userId}")
                    .then()
                    .statusCode(HttpStatus.BAD_REQUEST.value());
        }

        @Test
        @DisplayName("Should return 404 for non-existent user")
        void cancelSubscription_unknownUser_returns404() {
            UUID unknownId = UUID.randomUUID();

            given()
                    .pathParam("userId", unknownId)
                    .when()
                    .delete("/api/subscriptions/{userId}")
                    .then()
                    .statusCode(HttpStatus.NOT_FOUND.value());
        }
    }

    @Nested
    @DisplayName("Pro Subscription Creation Integration Tests")
    class CreateSubscriptionIntegrationTests {

        @Test
        @DisplayName("Should return 400 when user already has Pro subscription")
        void createProSubscription_alreadyPro_returns400() {
            given()
                    .pathParam("userId", proUser.getId())
                    .when()
                    .post("/api/subscriptions/pro/{userId}")
                    .then()
                    .statusCode(HttpStatus.BAD_REQUEST.value());
        }

        @Test
        @DisplayName("Should return 404 for non-existent user")
        void createProSubscription_unknownUser_returns404() {
            UUID unknownId = UUID.randomUUID();

            given()
                    .pathParam("userId", unknownId)
                    .when()
                    .post("/api/subscriptions/pro/{userId}")
                    .then()
                    .statusCode(HttpStatus.NOT_FOUND.value());
        }
    }
}
