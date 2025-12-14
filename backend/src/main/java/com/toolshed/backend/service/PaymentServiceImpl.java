package com.toolshed.backend.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import com.toolshed.backend.dto.CheckoutSessionResponse;
import com.toolshed.backend.dto.CreateCheckoutSessionRequest;
import com.toolshed.backend.dto.MonthlyEarningsResponse;
import com.toolshed.backend.dto.PayoutResponse;
import com.toolshed.backend.dto.WalletResponse;
import com.toolshed.backend.repository.BookingRepository;
import com.toolshed.backend.repository.PayoutRepository;
import com.toolshed.backend.repository.UserRepository;
import com.toolshed.backend.repository.entities.Booking;
import com.toolshed.backend.repository.entities.Payout;
import com.toolshed.backend.repository.entities.User;
import com.toolshed.backend.repository.enums.DepositStatus;
import com.toolshed.backend.repository.enums.PaymentStatus;
import com.toolshed.backend.repository.enums.PayoutStatus;

/**
 * Implementation of PaymentService for Stripe payment operations.
 */
@Service
public class PaymentServiceImpl implements PaymentService {

    private static final String BOOKING_ID_KEY = "bookingId";
    private static final String BOOKING_NOT_FOUND_MSG = "Booking not found: ";
    private static final String USER_NOT_FOUND_MSG = "User not found: ";

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final PayoutRepository payoutRepository;

    public PaymentServiceImpl(BookingRepository bookingRepository, UserRepository userRepository,
            PayoutRepository payoutRepository) {
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.payoutRepository = payoutRepository;
    }

    @Override
    public CheckoutSessionResponse createCheckoutSession(CreateCheckoutSessionRequest request, String successUrl,
            String cancelUrl) {
        validateBookingForPayment(request.getBookingId());
        Long amountInCents = request.getAmountInCents();

        try {
            SessionCreateParams params = buildSessionParams(request, successUrl, cancelUrl, amountInCents);
            Session session = Session.create(params);

            return CheckoutSessionResponse.builder()
                    .sessionId(session.getId())
                    .checkoutUrl(session.getUrl())
                    .build();
        } catch (StripeException e) {
            throw new PaymentProcessingException("Failed to create checkout session: " + e.getMessage(), e);
        }
    }

    @Override
    public CheckoutSessionResponse createDepositCheckoutSession(UUID bookingId, String successUrl, String cancelUrl) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(BOOKING_NOT_FOUND_MSG + bookingId));

        if (booking.getDepositStatus() != DepositStatus.REQUIRED) {
            throw new DepositNotRequiredException("No deposit required or already paid for booking: " + bookingId);
        }

        Long amountInCents = Math.round(booking.getDepositAmount() * 100);

        try {
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(
                            successUrl + "?bookingId=" + bookingId + "&type=deposit&session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl(cancelUrl + "?bookingId=" + bookingId + "&type=deposit")
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setQuantity(1L)
                                    .setPriceData(
                                            SessionCreateParams.LineItem.PriceData.builder()
                                                    .setCurrency("eur")
                                                    .setUnitAmount(amountInCents)
                                                    .setProductData(
                                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                    .setName("Caução - " + (booking.getTool() != null
                                                                            ? booking.getTool().getTitle()
                                                                            : "Ferramenta"))
                                                                    .setDescription(
                                                                            "Depósito de segurança para Booking ID: "
                                                                                    + bookingId)
                                                                    .build())
                                                    .build())
                                    .build())
                    .putMetadata(BOOKING_ID_KEY, bookingId.toString())
                    .putMetadata("type", "deposit")
                    .build();

            Session session = Session.create(params);

            return CheckoutSessionResponse.builder()
                    .sessionId(session.getId())
                    .checkoutUrl(session.getUrl())
                    .build();
        } catch (StripeException e) {
            throw new PaymentProcessingException("Failed to create deposit checkout session: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public Booking markBookingAsPaid(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(BOOKING_NOT_FOUND_MSG + bookingId));

        booking.setPaymentStatus(PaymentStatus.COMPLETED);

        // Credit the owner's wallet
        User owner = booking.getOwner();
        if (owner != null && booking.getTotalPrice() != null) {
            Double currentBalance = owner.getWalletBalance() != null ? owner.getWalletBalance() : 0.0;
            owner.setWalletBalance(currentBalance + booking.getTotalPrice());
            userRepository.save(owner);
        }

        return bookingRepository.save(booking);
    }

    @Override
    public Booking markDepositAsPaid(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(BOOKING_NOT_FOUND_MSG + bookingId));

        if (booking.getDepositStatus() != DepositStatus.REQUIRED) {
            throw new DepositNotRequiredException("No deposit required or already paid for booking: " + bookingId);
        }

        booking.setDepositStatus(DepositStatus.PAID);
        booking.setDepositPaidAt(LocalDateTime.now());
        return bookingRepository.save(booking);
    }

    @Override
    public Booking getBookingPaymentStatus(UUID bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(BOOKING_NOT_FOUND_MSG + bookingId));
    }

    @Override
    public Booking validateBookingForPayment(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(BOOKING_NOT_FOUND_MSG + bookingId));

        if (booking.getPaymentStatus() == PaymentStatus.COMPLETED) {
            throw new PaymentAlreadyCompletedException("Booking is already paid: " + bookingId);
        }

        return booking;
    }

    @Override
    public Booking updatePaymentStatus(UUID bookingId, PaymentStatus status) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(BOOKING_NOT_FOUND_MSG + bookingId));

        booking.setPaymentStatus(status);
        return bookingRepository.save(booking);
    }

    // ============ Wallet & Payout Operations ============

    @Override
    public WalletResponse getOwnerWallet(UUID ownerId) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new UserNotFoundException(USER_NOT_FOUND_MSG + ownerId));

        List<PayoutResponse> recentPayouts = payoutRepository.findByOwnerIdOrderByRequestedAtDesc(ownerId)
                .stream()
                .limit(10)
                .map(this::mapToPayoutResponse)
                .toList();

        return WalletResponse.builder()
                .balance(owner.getWalletBalance() != null ? owner.getWalletBalance() : 0.0)
                .recentPayouts(recentPayouts)
                .build();
    }

    @Override
    public List<PayoutResponse> getPayoutHistory(UUID ownerId) {
        userRepository.findById(ownerId)
                .orElseThrow(() -> new UserNotFoundException(USER_NOT_FOUND_MSG + ownerId));

        return payoutRepository.findByOwnerIdOrderByRequestedAtDesc(ownerId)
                .stream()
                .map(this::mapToPayoutResponse)
                .toList();
    }

    @Override
    @Transactional
    public PayoutResponse requestPayout(UUID ownerId, Double amount) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new UserNotFoundException(USER_NOT_FOUND_MSG + ownerId));

        Double currentBalance = owner.getWalletBalance() != null ? owner.getWalletBalance() : 0.0;

        if (amount <= 0) {
            throw new InvalidPayoutException("Payout amount must be positive");
        }

        if (amount > currentBalance) {
            throw new InsufficientBalanceException(
                    "Insufficient balance. Available: €" + currentBalance + ", Requested: €" + amount);
        }

        // Simulate Stripe Transfer (generate fake transfer ID)
        String stripeTransferId = "tr_simulated_" + UUID.randomUUID().toString().substring(0, 8);

        // Create payout record
        Payout payout = Payout.builder()
                .owner(owner)
                .amount(amount)
                .status(PayoutStatus.COMPLETED)
                .stripeTransferId(stripeTransferId)
                .completedAt(LocalDateTime.now())
                .build();

        payout = payoutRepository.save(payout);

        // Deduct from wallet balance
        owner.setWalletBalance(currentBalance - amount);
        userRepository.save(owner);

        return mapToPayoutResponse(payout);
    }

    @Override
    public List<MonthlyEarningsResponse> getMonthlyEarnings(UUID ownerId) {
        userRepository.findById(ownerId)
                .orElseThrow(() -> new UserNotFoundException(USER_NOT_FOUND_MSG + ownerId));

        // Get all paid bookings for the owner
        List<Booking> bookings = bookingRepository.findAll().stream()
                .filter(b -> b.getOwner().getId().equals(ownerId))
                .filter(b -> b.getPaymentStatus() == PaymentStatus.COMPLETED)
                .toList();

        // Group by Month and Year
        Map<String, Double> earningsMap = new HashMap<>();

        for (Booking booking : bookings) {
            String monthKey = booking.getEndDate().getMonth().toString() + " " + booking.getEndDate().getYear();
            earningsMap.put(monthKey, earningsMap.getOrDefault(monthKey, 0.0) + booking.getTotalPrice());
        }

        // Convert to DTO list
        return earningsMap.entrySet().stream()
                .map(entry -> {
                    String[] parts = entry.getKey().split(" ");
                    return MonthlyEarningsResponse.builder()
                            .month(parts[0])
                            .year(Integer.parseInt(parts[1]))
                            .amount(entry.getValue())
                            .build();
                })
                .sorted((a, b) -> {
                    int yearCompare = Integer.compare(b.getYear(), a.getYear());
                    if (yearCompare != 0)
                        return yearCompare;
                    return b.getMonth().compareTo(a.getMonth()); // Simplification, better to parse Enum or use
                                                                 // LocalDate
                })
                .toList();
    }

    private PayoutResponse mapToPayoutResponse(Payout payout) {
        return PayoutResponse.builder()
                .id(payout.getId())
                .amount(payout.getAmount())
                .status(payout.getStatus())
                .stripeTransferId(payout.getStripeTransferId())
                .requestedAt(payout.getRequestedAt())
                .completedAt(payout.getCompletedAt())
                .build();
    }

    private SessionCreateParams buildSessionParams(CreateCheckoutSessionRequest request,
            String successUrl, String cancelUrl, Long amountInCents) {
        return SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(
                        successUrl + "?bookingId=" + request.getBookingId() + "&session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(cancelUrl + "?bookingId=" + request.getBookingId())
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency("eur")
                                                .setUnitAmount(amountInCents)
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName(request.getDescription())
                                                                .setDescription("Booking ID: " + request.getBookingId())
                                                                .build())
                                                .build())
                                .build())
                .putMetadata(BOOKING_ID_KEY, request.getBookingId().toString())
                .build();
    }

    // ============ Exception Classes ============

    public static class BookingNotFoundException extends RuntimeException {
        public BookingNotFoundException(String message) {
            super(message);
        }
    }

    public static class UserNotFoundException extends RuntimeException {
        public UserNotFoundException(String message) {
            super(message);
        }
    }

    public static class PaymentAlreadyCompletedException extends RuntimeException {
        public PaymentAlreadyCompletedException(String message) {
            super(message);
        }
    }

    public static class PaymentProcessingException extends RuntimeException {
        public PaymentProcessingException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class DepositNotRequiredException extends RuntimeException {
        public DepositNotRequiredException(String message) {
            super(message);
        }
    }

    public static class InsufficientBalanceException extends RuntimeException {
        public InsufficientBalanceException(String message) {
            super(message);
        }
    }

    public static class InvalidPayoutException extends RuntimeException {
        public InvalidPayoutException(String message) {
            super(message);
        }
    }
}
