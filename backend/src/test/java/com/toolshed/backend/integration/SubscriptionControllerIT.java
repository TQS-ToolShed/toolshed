package com.toolshed.backend.integration;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.toolshed.backend.dto.SubscriptionStatusResponse;
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
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SubscriptionControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private User freeUser;
    private User proUser;

    @BeforeEach
    void setUp() {
        // Clean up any existing test data
        userRepository.deleteAll();

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
        userRepository.deleteAll();
    }

    @Nested
    @DisplayName("GET /api/subscriptions/status/{userId} Integration Tests")
    class GetSubscriptionStatusIntegrationTests {

        @Test
        @DisplayName("Should return Pro status for user with active subscription")
        void getStatus_proUser_returnsProStatus() throws Exception {
            mockMvc.perform(get("/api/subscriptions/status/{userId}", proUser.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tier").value("PRO"))
                    .andExpect(jsonPath("$.active").value(true))
                    .andExpect(jsonPath("$.discountPercentage").value(5.0));
        }

        @Test
        @DisplayName("Should return Free status for user without subscription")
        void getStatus_freeUser_returnsFreeStatus() throws Exception {
            mockMvc.perform(get("/api/subscriptions/status/{userId}", freeUser.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tier").value("FREE"))
                    .andExpect(jsonPath("$.active").value(false))
                    .andExpect(jsonPath("$.discountPercentage").value(0.0));
        }

        @Test
        @DisplayName("Should return 404 for non-existent user")
        void getStatus_unknownUser_returns404() throws Exception {
            UUID unknownId = UUID.randomUUID();

            mockMvc.perform(get("/api/subscriptions/status/{userId}", unknownId))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Subscription Activation Integration Tests")
    class ActivationIntegrationTests {

        @Test
        @DisplayName("Should activate Pro subscription and persist in database")
        void activateSubscription_persistsInDatabase() throws Exception {
            // Activate subscription
            mockMvc.perform(post("/api/subscriptions/activate/{userId}", freeUser.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"stripeSubscriptionId\": \"sub_new_integration\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tier").value("PRO"))
                    .andExpect(jsonPath("$.active").value(true));

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
        void cancelSubscription_freeUser_returns400() throws Exception {
            mockMvc.perform(delete("/api/subscriptions/{userId}", freeUser.getId()))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 404 for non-existent user")
        void cancelSubscription_unknownUser_returns404() throws Exception {
            UUID unknownId = UUID.randomUUID();

            mockMvc.perform(delete("/api/subscriptions/{userId}", unknownId))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Pro Subscription Creation Integration Tests")
    class CreateSubscriptionIntegrationTests {

        @Test
        @DisplayName("Should return 400 when user already has Pro subscription")
        void createProSubscription_alreadyPro_returns400() throws Exception {
            mockMvc.perform(post("/api/subscriptions/pro/{userId}", proUser.getId()))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 404 for non-existent user")
        void createProSubscription_unknownUser_returns404() throws Exception {
            UUID unknownId = UUID.randomUUID();

            mockMvc.perform(post("/api/subscriptions/pro/{userId}", unknownId))
                    .andExpect(status().isNotFound());
        }
    }
}
