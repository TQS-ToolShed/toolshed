package com.toolshed.backend.boundary;

import java.util.UUID;

import com.toolshed.backend.boundary.SubscriptionController;
import com.toolshed.backend.dto.CheckoutSessionResponse;
import com.toolshed.backend.dto.SubscriptionStatusResponse;
import com.toolshed.backend.repository.enums.SubscriptionTier;
import com.toolshed.backend.service.SubscriptionException;
import com.toolshed.backend.service.SubscriptionService;
import com.toolshed.backend.service.UserNotFoundException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SubscriptionController.class)
class SubscriptionControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private SubscriptionService subscriptionService;

        private UUID userId;
        private SubscriptionStatusResponse proStatus;
        private SubscriptionStatusResponse freeStatus;
        private CheckoutSessionResponse checkoutResponse;

        @BeforeEach
        void setUp() {
                userId = UUID.randomUUID();

                proStatus = SubscriptionStatusResponse.builder()
                                .tier(SubscriptionTier.PRO)
                                .active(true)
                                .discountPercentage(5.0)
                                .build();

                freeStatus = SubscriptionStatusResponse.builder()
                                .tier(SubscriptionTier.FREE)
                                .active(false)
                                .discountPercentage(0.0)
                                .build();

                checkoutResponse = CheckoutSessionResponse.builder()
                                .sessionId("cs_test_123")
                                .checkoutUrl("https://checkout.stripe.com/test")
                                .build();
        }

        @Nested
        @DisplayName("GET /api/subscriptions/status/{userId}")
        class GetSubscriptionStatusTests {

                @Test
                @DisplayName("Should return subscription status for existing user")
                void getSubscriptionStatus_success() throws Exception {
                        when(subscriptionService.getSubscriptionStatus(userId)).thenReturn(proStatus);

                        mockMvc.perform(get("/api/subscriptions/status/{userId}", userId))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.tier").value("PRO"))
                                        .andExpect(jsonPath("$.active").value(true))
                                        .andExpect(jsonPath("$.discountPercentage").value(5.0));
                }

                @Test
                @DisplayName("Should return 404 for non-existent user")
                void getSubscriptionStatus_userNotFound() throws Exception {
                        when(subscriptionService.getSubscriptionStatus(userId))
                                        .thenThrow(new UserNotFoundException("User not found"));

                        mockMvc.perform(get("/api/subscriptions/status/{userId}", userId))
                                        .andExpect(status().isNotFound());
                }

                @Test
                @DisplayName("Should return Free status for non-subscriber")
                void getSubscriptionStatus_freeTier() throws Exception {
                        when(subscriptionService.getSubscriptionStatus(userId)).thenReturn(freeStatus);

                        mockMvc.perform(get("/api/subscriptions/status/{userId}", userId))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.tier").value("FREE"))
                                        .andExpect(jsonPath("$.active").value(false))
                                        .andExpect(jsonPath("$.discountPercentage").value(0.0));
                }
        }

        @Nested
        @DisplayName("POST /api/subscriptions/pro/{userId}")
        class CreateProSubscriptionTests {

                @Test
                @DisplayName("Should create checkout session for Pro subscription")
                void createProSubscription_success() throws Exception {
                        when(subscriptionService.createProSubscription(eq(userId), anyString(), anyString()))
                                        .thenReturn(checkoutResponse);

                        mockMvc.perform(post("/api/subscriptions/pro/{userId}", userId))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.sessionId").value("cs_test_123"))
                                        .andExpect(jsonPath("$.checkoutUrl").value("https://checkout.stripe.com/test"));
                }

                @Test
                @DisplayName("Should return 404 for non-existent user")
                void createProSubscription_userNotFound() throws Exception {
                        when(subscriptionService.createProSubscription(eq(userId), anyString(), anyString()))
                                        .thenThrow(new UserNotFoundException("User not found"));

                        mockMvc.perform(post("/api/subscriptions/pro/{userId}", userId))
                                        .andExpect(status().isNotFound());
                }

                @Test
                @DisplayName("Should return 400 when user already has Pro subscription")
                void createProSubscription_alreadyPro() throws Exception {
                        when(subscriptionService.createProSubscription(eq(userId), anyString(), anyString()))
                                        .thenThrow(new SubscriptionException(
                                                        "User already has an active Pro subscription"));

                        mockMvc.perform(post("/api/subscriptions/pro/{userId}", userId))
                                        .andExpect(status().isBadRequest());
                }
        }

        @Nested
        @DisplayName("DELETE /api/subscriptions/{userId}")
        class CancelSubscriptionTests {

                @Test
                @DisplayName("Should cancel subscription successfully")
                void cancelSubscription_success() throws Exception {
                        doNothing().when(subscriptionService).cancelSubscription(userId);

                        mockMvc.perform(delete("/api/subscriptions/{userId}", userId))
                                        .andExpect(status().isOk());
                }

                @Test
                @DisplayName("Should return 404 for non-existent user")
                void cancelSubscription_userNotFound() throws Exception {
                        doThrow(new UserNotFoundException("User not found"))
                                        .when(subscriptionService).cancelSubscription(userId);

                        mockMvc.perform(delete("/api/subscriptions/{userId}", userId))
                                        .andExpect(status().isNotFound());
                }

                @Test
                @DisplayName("Should return 400 when user is not Pro member")
                void cancelSubscription_notPro() throws Exception {
                        doThrow(new SubscriptionException("User does not have an active Pro subscription"))
                                        .when(subscriptionService).cancelSubscription(userId);

                        mockMvc.perform(delete("/api/subscriptions/{userId}", userId))
                                        .andExpect(status().isBadRequest());
                }
        }

        @Nested
        @DisplayName("POST /api/subscriptions/activate/{userId}")
        class ActivateSubscriptionTests {

                @Test
                @DisplayName("Should activate subscription successfully")
                void activateSubscription_success() throws Exception {
                        doNothing().when(subscriptionService).activateProSubscription(eq(userId), anyString());
                        when(subscriptionService.getSubscriptionStatus(userId)).thenReturn(proStatus);

                        mockMvc.perform(post("/api/subscriptions/activate/{userId}", userId)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{\"stripeSubscriptionId\": \"sub_test123\"}"))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.tier").value("PRO"))
                                        .andExpect(jsonPath("$.active").value(true));
                }

                @Test
                @DisplayName("Should return 404 for non-existent user")
                void activateSubscription_userNotFound() throws Exception {
                        doThrow(new UserNotFoundException("User not found"))
                                        .when(subscriptionService).activateProSubscription(eq(userId), anyString());

                        mockMvc.perform(post("/api/subscriptions/activate/{userId}", userId)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{\"stripeSubscriptionId\": \"sub_test123\"}"))
                                        .andExpect(status().isNotFound());
                }
        }
}
