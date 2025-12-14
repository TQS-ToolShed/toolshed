package com.toolshed.backend.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.toolshed.backend.dto.BookingResponse;
import com.toolshed.backend.dto.ConditionReportRequest;
import com.toolshed.backend.dto.CreateBookingRequest;
import com.toolshed.backend.dto.MonthlyEarnings;
import com.toolshed.backend.dto.OwnerBookingResponse;
import com.toolshed.backend.dto.OwnerEarningsResponse;
import com.toolshed.backend.dto.ReviewResponse;
import com.toolshed.backend.repository.BookingRepository;
import com.toolshed.backend.repository.ToolRepository;
import com.toolshed.backend.repository.UserRepository;
import com.toolshed.backend.repository.entities.Booking;
import com.toolshed.backend.repository.entities.Review;
import com.toolshed.backend.repository.entities.Tool;
import com.toolshed.backend.repository.entities.User;
import com.toolshed.backend.repository.enums.BookingStatus;
import com.toolshed.backend.repository.enums.ConditionStatus;
import com.toolshed.backend.repository.enums.DepositStatus;
import com.toolshed.backend.repository.enums.PaymentStatus;
import com.toolshed.backend.repository.enums.ReviewType;

@Service
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final ToolRepository toolRepository;
    private final UserRepository userRepository;

    public BookingServiceImpl(BookingRepository bookingRepository,
            ToolRepository toolRepository,
            UserRepository userRepository) {
        this.bookingRepository = bookingRepository;
        this.toolRepository = toolRepository;
        this.userRepository = userRepository;
    }

    @Override
    public BookingResponse createBooking(CreateBookingRequest request) {
        LocalDate today = LocalDate.now();
        if (request.getStartDate().isBefore(today) || request.getEndDate().isBefore(today)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dates cannot be in the past");
        }
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "End date must be on or after start date");
        }

        Tool tool = toolRepository.findById(request.getToolId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tool not found"));

        User renter = userRepository.findById(request.getRenterId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Renter not found"));

        User owner = tool.getOwner();
        if (owner == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tool owner is missing");
        }

        List<Booking> overlaps = bookingRepository.findOverlappingBookings(
                tool.getId(), request.getStartDate(), request.getEndDate());
        if (!overlaps.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tool is already booked for these dates");
        }

        long days = ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) + 1;
        double totalPrice = days * tool.getPricePerDay();

        Booking booking = new Booking();
        booking.setTool(tool);
        booking.setRenter(renter);
        booking.setOwner(owner);
        booking.setStartDate(request.getStartDate());
        booking.setEndDate(request.getEndDate());
        booking.setStatus(BookingStatus.PENDING);
        booking.setPaymentStatus(PaymentStatus.PENDING);
        booking.setTotalPrice(totalPrice);

        Booking saved = bookingRepository.save(booking);

        return toBookingResponse(saved);
    }

    /**
     * task to mark finished bookings as completed and free tools if they are no
     * longer rented.
     */
    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void completeExpiredBookings() {
        LocalDate today = LocalDate.now();
        List<Booking> expired = bookingRepository.findByStatusAndEndDateBefore(BookingStatus.APPROVED, today);

        for (Booking booking : expired) {
            booking.setStatus(BookingStatus.COMPLETED);
            bookingRepository.save(booking);

            Tool tool = booking.getTool();
            if (tool != null) {
                long activeCount = bookingRepository.countActiveApprovedBookingsForToolOnDate(tool.getId(), today);
                tool.setActive(activeCount == 0);
                toolRepository.save(tool);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<OwnerBookingResponse> getBookingsForOwner(UUID ownerId) {
        List<Booking> bookings = bookingRepository.findByOwnerId(ownerId);
        return bookings.stream()
                .map(this::toOwnerBookingResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> getBookingsForTool(UUID toolId) {
        List<Booking> bookings = bookingRepository.findByToolId(toolId);
        return bookings.stream()
                .map(this::toBookingResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> getBookingsForRenter(UUID renterId) {
        List<Booking> bookings = bookingRepository.findByRenterId(renterId);
        return bookings.stream()
                .map(this::toBookingResponse)
                .toList();
    }

    @Override
    @Transactional
    public BookingResponse updateBookingStatus(UUID bookingId, BookingStatus status) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));

        if (status != BookingStatus.APPROVED && status != BookingStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only approval or rejection is supported");
        }

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Booking decision is already final");
        }

        booking.setStatus(status);

        // If approved, only mark the tool unavailable during the booking window
        if (status == BookingStatus.APPROVED) {
            List<Booking> overlaps = bookingRepository.findOverlappingBookings(
                    booking.getTool().getId(),
                    booking.getStartDate(),
                    booking.getEndDate());

            boolean hasApprovedOverlap = overlaps.stream()
                    .anyMatch(b -> !b.getId().equals(bookingId) && b.getStatus() == BookingStatus.APPROVED);
            if (hasApprovedOverlap) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Booking window already approved for this tool");
            }

            Tool tool = booking.getTool();
            if (tool != null) {
                LocalDate today = LocalDate.now();
                boolean withinWindow = !today.isBefore(booking.getStartDate()) && !today.isAfter(booking.getEndDate());
                tool.setActive(!withinWindow);
                toolRepository.save(tool);
            }
        }

        Booking saved = bookingRepository.save(booking);

        return toBookingResponse(saved);
    }

    private static final Double DEPOSIT_AMOUNT = 50.0;

    @Override
    @Transactional
    public BookingResponse submitConditionReport(UUID bookingId, ConditionReportRequest request) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));

        // Validate renter
        if (!booking.getRenter().getId().equals(request.getRenterId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the renter can submit a condition report");
        }

        // Validate booking is completed
        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Condition reports can only be submitted for completed bookings");
        }

        // Validate no condition report already exists
        if (booking.getConditionStatus() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Condition report already submitted");
        }

        // Set condition report fields
        booking.setConditionStatus(request.getConditionStatus());
        booking.setConditionDescription(request.getDescription());
        booking.setConditionReportedAt(LocalDateTime.now());
        booking.setConditionReportedBy(booking.getRenter());

        // Determine deposit requirement
        boolean requiresDeposit = request.getConditionStatus() == ConditionStatus.MINOR_DAMAGE
                || request.getConditionStatus() == ConditionStatus.BROKEN
                || request.getConditionStatus() == ConditionStatus.MISSING_PARTS;

        if (requiresDeposit) {
            booking.setDepositStatus(DepositStatus.REQUIRED);
            booking.setDepositAmount(DEPOSIT_AMOUNT);
        } else {
            booking.setDepositStatus(DepositStatus.NOT_REQUIRED);
            booking.setDepositAmount(0.0);
        }

        Booking saved = bookingRepository.save(booking);
        return toBookingResponse(saved);
    }

    @Override
    @Transactional
    public BookingResponse payDeposit(UUID bookingId, UUID renterId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));

        // Validate renter
        if (!booking.getRenter().getId().equals(renterId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the renter can pay the deposit");
        }

        // Validate deposit is required
        if (booking.getDepositStatus() != DepositStatus.REQUIRED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No deposit required or already paid");
        }

        // Mark deposit as paid
        booking.setDepositStatus(DepositStatus.PAID);
        booking.setDepositPaidAt(LocalDateTime.now());

        Booking saved = bookingRepository.save(booking);
        return toBookingResponse(saved);
    }

    private ReviewResponse toReviewResponse(Review review) {
        if (review == null)
            return null;
        String reviewerName = review.getReviewer() != null
                ? (review.getReviewer().getFirstName() + " " + review.getReviewer().getLastName()).trim()
                : null;

        return ReviewResponse.builder()
                .id(review.getId())
                .bookingId(review.getBooking().getId())
                .reviewerId(review.getReviewer() != null ? review.getReviewer().getId() : null)
                .reviewerName(reviewerName)
                .ownerId(review.getOwner() != null ? review.getOwner().getId() : null)
                .toolId(review.getTool() != null ? review.getTool().getId() : null)
                .rating(review.getRating())
                .comment(review.getComment())
                .date(review.getDate())
                .build();
    }

    private Review getReviewByType(List<Review> reviews, ReviewType type) {
        if (reviews == null)
            return null;
        return reviews.stream()
                .filter(r -> r.getType() == type || (type == ReviewType.RENTER_TO_OWNER && r.getType() == null))
                .findFirst()
                .orElse(null);
    }

    private OwnerBookingResponse toOwnerBookingResponse(Booking booking) {
        Tool tool = booking.getTool();
        User renter = booking.getRenter();
        String renterName = renter != null
                ? (renter.getFirstName() + " " + renter.getLastName()).trim()
                : null;

        Review renterReview = getReviewByType(booking.getReviews(), ReviewType.RENTER_TO_OWNER);
        Review ownerReview = getReviewByType(booking.getReviews(), ReviewType.OWNER_TO_RENTER);

        return OwnerBookingResponse.builder()
                .id(booking.getId())
                .toolId(tool != null ? tool.getId() : null)
                .toolTitle(tool != null ? tool.getTitle() : null)
                .renterId(renter != null ? renter.getId() : null)
                .renterName(renterName)
                .startDate(booking.getStartDate())
                .endDate(booking.getEndDate())
                .status(booking.getStatus())
                .totalPrice(booking.getTotalPrice())
                .review(toReviewResponse(renterReview))
                .ownerReview(toReviewResponse(ownerReview))
                // Condition Report Fields
                .conditionStatus(booking.getConditionStatus())
                .conditionDescription(booking.getConditionDescription())
                .conditionReportedAt(booking.getConditionReportedAt())
                .conditionReportedByName(booking.getConditionReportedBy() != null
                        ? (booking.getConditionReportedBy().getFirstName() + " "
                                + booking.getConditionReportedBy().getLastName()).trim()
                        : null)
                // Deposit Fields
                .depositStatus(booking.getDepositStatus())
                .depositAmount(booking.getDepositAmount())
                .depositPaidAt(booking.getDepositPaidAt())
                .build();
    }

    private BookingResponse toBookingResponse(Booking booking) {
        String ownerName = booking.getOwner() != null
                ? (booking.getOwner().getFirstName() + " " + booking.getOwner().getLastName()).trim()
                : null;

        Review renterReview = getReviewByType(booking.getReviews(), ReviewType.RENTER_TO_OWNER);
        Review ownerReview = getReviewByType(booking.getReviews(), ReviewType.OWNER_TO_RENTER);
        Review toolReview = getReviewByType(booking.getReviews(), ReviewType.RENTER_TO_TOOL);

        return BookingResponse.builder()
                .id(booking.getId())
                .toolId(booking.getTool().getId())
                .renterId(booking.getRenter().getId())
                .ownerId(booking.getOwner().getId())
                .ownerName(ownerName)
                .toolTitle(booking.getTool().getTitle() != null ? booking.getTool().getTitle() : null)
                .startDate(booking.getStartDate())
                .endDate(booking.getEndDate())
                .status(booking.getStatus())
                .paymentStatus(booking.getPaymentStatus())
                .totalPrice(booking.getTotalPrice())
                .review(toReviewResponse(renterReview))
                .ownerReview(toReviewResponse(ownerReview))
                .toolReview(toReviewResponse(toolReview))
                // Condition Report Fields
                .conditionStatus(booking.getConditionStatus())
                .conditionDescription(booking.getConditionDescription())
                .conditionReportedAt(booking.getConditionReportedAt())
                .conditionReportedByName(booking.getConditionReportedBy() != null
                        ? (booking.getConditionReportedBy().getFirstName() + " "
                                + booking.getConditionReportedBy().getLastName()).trim()
                        : null)
                // Deposit Fields
                .depositStatus(booking.getDepositStatus())
                .depositAmount(booking.getDepositAmount())
                .depositPaidAt(booking.getDepositPaidAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public OwnerEarningsResponse getOwnerEarnings(UUID ownerId) {
        List<Booking> completedBookings = bookingRepository.findCompletedBookingsByOwnerId(ownerId);

        // Group bookings by year and month, calculate earnings
        Map<String, List<Booking>> bookingsByMonth = completedBookings.stream()
                .filter(b -> b.getTotalPrice() != null)
                .collect(Collectors.groupingBy(b -> {
                    LocalDate endDate = b.getEndDate();
                    return endDate.getYear() + "-" + endDate.getMonthValue();
                }));

        // Create MonthlyEarnings DTOs
        List<MonthlyEarnings> monthlyEarningsList = new ArrayList<>();
        
        for (Map.Entry<String, List<Booking>> entry : bookingsByMonth.entrySet()) {
            List<Booking> bookingsInMonth = entry.getValue();
            
            // Skip if empty (defensive programming)
            if (bookingsInMonth.isEmpty()) {
                continue;
            }
            
            // Use the first booking's endDate to get year and month
            LocalDate sampleDate = bookingsInMonth.get(0).getEndDate();
            int year = sampleDate.getYear();
            int month = sampleDate.getMonthValue();
            
            // Calculate total earnings (totalPrice is guaranteed non-null due to filter above)
            double totalEarnings = bookingsInMonth.stream()
                    .filter(b -> b.getTotalPrice() != null)
                    .mapToDouble(Booking::getTotalPrice)
                    .sum();
            
            monthlyEarningsList.add(MonthlyEarnings.builder()
                    .year(year)
                    .month(month)
                    .totalEarnings(totalEarnings)
                    .bookingCount((long) bookingsInMonth.size())
                    .build());
        }

        // Sort by year and month descending (most recent first)
        monthlyEarningsList.sort(Comparator
                .comparing(MonthlyEarnings::getYear)
                .thenComparing(MonthlyEarnings::getMonth)
                .reversed());

        // Calculate total earnings
        double totalEarnings = completedBookings.stream()
                .filter(b -> b.getTotalPrice() != null)
                .mapToDouble(Booking::getTotalPrice)
                .sum();

        return OwnerEarningsResponse.builder()
                .monthlyEarnings(monthlyEarningsList)
                .totalEarnings(totalEarnings)
                .build();
    }
}
