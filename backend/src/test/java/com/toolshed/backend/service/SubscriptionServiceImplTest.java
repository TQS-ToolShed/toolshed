package com.toolshed.backend.service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import com.toolshed.backend.dto.CheckoutSessionResponse;
import com.toolshed.backend.dto.SubscriptionStatusResponse;
import com.toolshed.backend.repository.UserRepository;
import com.toolshed.backend.repository.entities.User;
import com.toolshed.backend.repository.enums.SubscriptionTier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SubscriptionServiceImpl subscriptionService;

    private User freeUser;
    private User proUser;
    private User expiredProUser;
    private UUID freeUserId;
    private UUID proUserId;
    private UUID expiredProUserId;

    @BeforeEach
    void setUp() {
        freeUserId = UUID.randomUUID();
        proUserId = UUID.randomUUID();
        expiredProUserId = UUID.randomUUID();

        freeUser = User.builder()
                .id(freeUserId)
                .firstName("Free")
                .lastName("User")
                .email("free@test.com")
                .password("password")
                .subscriptionTier(SubscriptionTier.FREE)
                .reputationScore(0.0)
                .walletBalance(0.0)
                .build();

        proUser = User.builder()
                .id(proUserId)
                .firstName("Pro")
                .lastName("User")
                .email("pro@test.com")
                .password("password")
                .subscriptionTier(SubscriptionTier.PRO)
                .subscriptionStart(LocalDateTime.now().minusDays(15))
                .subscriptionEnd(LocalDateTime.now().plusDays(15))
                .stripeSubscriptionId("sub_test123")
                .reputationScore(0.0)
                .walletBalance(0.0)
                .build();

        expiredProUser = User.builder()
                .id(expiredProUserId)
                .firstName("Expired")
                .lastName("Pro")
                .email("expired@test.com")
                .password("password")
                .subscriptionTier(SubscriptionTier.PRO)
                .subscriptionStart(LocalDateTime.now().minusDays(45))
                .subscriptionEnd(LocalDateTime.now().minusDays(15))
                .stripeSubscriptionId("sub_expired")
                .reputationScore(0.0)
                .walletBalance(0.0)
                .build();
    }

    @Nested
    @DisplayName("isProMember tests")
    class IsProMemberTests {

        @Test
        @DisplayName("Should return true for active Pro member")
        void isProMember_withActivePro_returnsTrue() {
            boolean result = subscriptionService.isProMember(proUser);
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false for Free tier user")
        void isProMember_withFreeTier_returnsFalse() {
            boolean result = subscriptionService.isProMember(freeUser);
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return false for expired Pro subscription")
        void isProMember_withExpiredPro_returnsFalse() {
            boolean result = subscriptionService.isProMember(expiredProUser);
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return true for Pro member with null end date")
        void isProMember_withNullEndDate_returnsTrue() {
            proUser.setSubscriptionEnd(null);
            boolean result = subscriptionService.isProMember(proUser);
            assertThat(result).isTrue();
        }
    }

    @Nested
    @DisplayName("getDiscountPercentage tests")
    class GetDiscountPercentageTests {

        @Test
        @DisplayName("Should return 5% for active Pro member")
        void getDiscountPercentage_forProMember_returns5() {
            double discount = subscriptionService.getDiscountPercentage(proUser);
            assertThat(discount).isEqualTo(5.0);
        }

        @Test
        @DisplayName("Should return 0% for Free tier user")
        void getDiscountPercentage_forFreeMember_returns0() {
            double discount = subscriptionService.getDiscountPercentage(freeUser);
            assertThat(discount).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should return 0% for expired Pro subscription")
        void getDiscountPercentage_forExpiredPro_returns0() {
            double discount = subscriptionService.getDiscountPercentage(expiredProUser);
            assertThat(discount).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("getSubscriptionStatus tests")
    class GetSubscriptionStatusTests {

        @Test
        @DisplayName("Should return correct status for Pro member")
        void getSubscriptionStatus_forProMember_returnsProStatus() {
            when(userRepository.findById(proUserId)).thenReturn(Optional.of(proUser));

            SubscriptionStatusResponse status = subscriptionService.getSubscriptionStatus(proUserId);

            assertThat(status.getTier()).isEqualTo(SubscriptionTier.PRO);
            assertThat(status.isActive()).isTrue();
            assertThat(status.getDiscountPercentage()).isEqualTo(5.0);
            assertThat(status.getSubscriptionStart()).isNotNull();
            // subscriptionEnd is null for lifetime Pro (one-time payment)
        }

        @Test
        @DisplayName("Should return correct status for Free member")
        void getSubscriptionStatus_forFreeMember_returnsFreeStatus() {
            when(userRepository.findById(freeUserId)).thenReturn(Optional.of(freeUser));

            SubscriptionStatusResponse status = subscriptionService.getSubscriptionStatus(freeUserId);

            assertThat(status.getTier()).isEqualTo(SubscriptionTier.FREE);
            assertThat(status.isActive()).isFalse();
            assertThat(status.getDiscountPercentage()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should throw exception for non-existent user")
        void getSubscriptionStatus_userNotFound_throwsException() {
            UUID unknownId = UUID.randomUUID();
            when(userRepository.findById(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> subscriptionService.getSubscriptionStatus(unknownId))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessageContaining("User not found");
        }
    }

    @Nested
    @DisplayName("createProSubscription tests")
    class CreateProSubscriptionTests {

        @Test
        @DisplayName("Should throw exception when user already has Pro subscription")
        void createProSubscription_alreadyPro_throwsException() {
            when(userRepository.findById(proUserId)).thenReturn(Optional.of(proUser));

            assertThatThrownBy(() -> subscriptionService.createProSubscription(
                    proUserId, "http://success", "http://cancel"))
                    .isInstanceOf(SubscriptionException.class)
                    .hasMessageContaining("already has an active Pro subscription");
        }

        @Test
        @DisplayName("Should throw exception for non-existent user")
        void createProSubscription_userNotFound_throwsException() {
            UUID unknownId = UUID.randomUUID();
            when(userRepository.findById(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> subscriptionService.createProSubscription(
                    unknownId, "http://success", "http://cancel"))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("cancelSubscription tests")
    class CancelSubscriptionTests {

        @Test
        @DisplayName("Should throw exception when user is not Pro member")
        void cancelSubscription_notPro_throwsException() {
            when(userRepository.findById(freeUserId)).thenReturn(Optional.of(freeUser));

            assertThatThrownBy(() -> subscriptionService.cancelSubscription(freeUserId))
                    .isInstanceOf(SubscriptionException.class)
                    .hasMessageContaining("does not have an active Pro subscription");
        }

        @Test
        @DisplayName("Should throw exception for non-existent user")
        void cancelSubscription_userNotFound_throwsException() {
            UUID unknownId = UUID.randomUUID();
            when(userRepository.findById(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> subscriptionService.cancelSubscription(unknownId))
                    .isInstanceOf(UserNotFoundException.class);
        }

        @Test
        @DisplayName("Should cancel Pro subscription and reset user to FREE tier")
        void cancelSubscription_proUser_resetsToFree() {
            when(userRepository.findById(proUserId)).thenReturn(Optional.of(proUser));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            subscriptionService.cancelSubscription(proUserId);

            verify(userRepository).save(any(User.class));
            assertThat(proUser.getSubscriptionTier()).isEqualTo(SubscriptionTier.FREE);
            assertThat(proUser.getSubscriptionEnd()).isNotNull();
            assertThat(proUser.getStripeSubscriptionId()).isNull();
        }
    }

    @Nested
    @DisplayName("activateProSubscription tests")
    class ActivateProSubscriptionTests {

        @Test
        @DisplayName("Should activate Pro subscription for user")
        void activateProSubscription_success() {
            when(userRepository.findById(freeUserId)).thenReturn(Optional.of(freeUser));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            subscriptionService.activateProSubscription(freeUserId, "sub_new123");

            verify(userRepository).save(any(User.class));
            assertThat(freeUser.getSubscriptionTier()).isEqualTo(SubscriptionTier.PRO);
            assertThat(freeUser.getStripeSubscriptionId()).isEqualTo("sub_new123");
            assertThat(freeUser.getSubscriptionStart()).isNotNull();
            // subscriptionEnd is null for lifetime Pro (one-time payment)
            assertThat(freeUser.getSubscriptionEnd()).isNull();
        }

        @Test
        @DisplayName("Should throw exception for non-existent user")
        void activateProSubscription_userNotFound_throwsException() {
            UUID unknownId = UUID.randomUUID();
            when(userRepository.findById(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> subscriptionService.activateProSubscription(unknownId, "sub_test"))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("handleSubscriptionWebhook tests")
    class HandleSubscriptionWebhookTests {

        @Test
        @DisplayName("Should handle webhook without throwing exception")
        void handleSubscriptionWebhook_doesNotThrow() {
            // Webhook handling is a no-op for now, just verify it doesn't throw
            subscriptionService.handleSubscriptionWebhook("{\"type\":\"test\"}", "sig_test");
            // If we reach here, the method executed successfully
        }
    }

    @Nested
    @DisplayName("createProSubscription with stripeEnabled=false tests")
    class CreateProSubscriptionDevModeTests {

        @Test
        @DisplayName("Should activate subscription directly when Stripe is disabled")
        void createProSubscription_devMode_activatesDirectly() {
            // Create a new instance with stripeEnabled=false (default)
            SubscriptionServiceImpl devService = new SubscriptionServiceImpl(userRepository);
            // stripeEnabled defaults to false in @Value annotation

            when(userRepository.findById(freeUserId)).thenReturn(Optional.of(freeUser));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            CheckoutSessionResponse response = devService.createProSubscription(
                    freeUserId, "http://success", "http://cancel");

            // Verify dev mode response
            assertThat(response.getSessionId()).isEqualTo("dev_session");
            assertThat(response.getCheckoutUrl()).contains("http://success");
            assertThat(response.getCheckoutUrl()).contains("dev_activated");

            // Verify user was activated directly
            verify(userRepository).save(any(User.class));
            assertThat(freeUser.getSubscriptionTier()).isEqualTo(SubscriptionTier.PRO);
        }
    }

    @Nested
    @DisplayName("createProSubscription with stripeEnabled=true tests")
    class CreateProSubscriptionStripeModeTests {

        @Test
        @DisplayName("Should throw SubscriptionException when Stripe API call fails")
        void createProSubscription_stripeMode_throwsExceptionOnStripeError() {
            // Create a new instance and enable Stripe mode via reflection
            SubscriptionServiceImpl stripeService = new SubscriptionServiceImpl(userRepository);
            org.springframework.test.util.ReflectionTestUtils.setField(stripeService, "stripeEnabled", true);

            when(userRepository.findById(freeUserId)).thenReturn(Optional.of(freeUser));

            // When Stripe is enabled but not properly configured, it will throw
            // StripeException
            // which gets wrapped in SubscriptionException
            assertThatThrownBy(() -> stripeService.createProSubscription(
                    freeUserId, "http://success", "http://cancel"))
                    .isInstanceOf(SubscriptionException.class)
                    .hasMessageContaining("Failed to create payment session");
        }
    }
}
