package com.toolshed.backend.integration;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.toolshed.backend.dto.CreateReviewRequest;
import com.toolshed.backend.dto.ReviewResponse;
import com.toolshed.backend.repository.BookingRepository;
import com.toolshed.backend.repository.ReviewRepository;
import com.toolshed.backend.repository.ToolRepository;
import com.toolshed.backend.repository.UserRepository;
import com.toolshed.backend.repository.entities.Booking;
import com.toolshed.backend.repository.entities.Tool;
import com.toolshed.backend.repository.entities.User;
import com.toolshed.backend.repository.enums.BookingStatus;
import com.toolshed.backend.repository.enums.UserRole;
import com.toolshed.backend.repository.enums.UserStatus;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ReviewIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ToolRepository toolRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    private User renter;
    private User owner;
    private Tool tool;
    private Booking booking;

    @BeforeEach
    void setUp() {
        reviewRepository.deleteAll();
        bookingRepository.deleteAll();
        toolRepository.deleteAll();
        userRepository.deleteAll();

        owner = new User();
        owner.setFirstName("Owner");
        owner.setLastName("User");
        owner.setEmail("owner@example.com");
        owner.setPassword("password");
        owner.setRole(UserRole.SUPPLIER);
        owner.setStatus(UserStatus.ACTIVE);
        owner.setReputationScore(5.0);
        owner = userRepository.save(owner);

        renter = new User();
        renter.setFirstName("Renter");
        renter.setLastName("User");
        renter.setEmail("renter@example.com");
        renter.setPassword("password");
        renter.setRole(UserRole.RENTER);
        renter.setStatus(UserStatus.ACTIVE);
        renter.setReputationScore(5.0);
        renter = userRepository.save(renter);

        tool = new Tool();
        tool.setTitle("Test Tool");
        tool.setDescription("Description");
        tool.setPricePerDay(10.0);
        tool.setDistrict("Aveiro");
        tool.setMunicipality("Aveiro");
        tool.setOwner(owner);
        tool.setActive(true);
        tool.setOverallRating(0.0);
        tool.setNumRatings(0);
        tool = toolRepository.save(tool);

        booking = new Booking();
        booking.setTool(tool);
        booking.setRenter(renter);
        booking.setOwner(owner);
        booking.setStartDate(LocalDate.now().minusDays(5));
        booking.setEndDate(LocalDate.now().minusDays(3));
        booking.setStatus(BookingStatus.COMPLETED);
        booking.setTotalPrice(30.0);
        booking = bookingRepository.save(booking);
    }

    @Test
    void createReview_validRequest_returnsCreatedReview() {
        CreateReviewRequest request = new CreateReviewRequest();
        request.setBookingId(booking.getId());
        request.setRating(5);
        request.setComment("Excellent experience!");

        ResponseEntity<ReviewResponse> response = restTemplate.postForEntity(
                "/api/reviews",
                request,
                ReviewResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ReviewResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getRating()).isEqualTo(5);
        assertThat(body.getComment()).isEqualTo("Excellent experience!");
        assertThat(body.getReviewerId()).isEqualTo(renter.getId());
        assertThat(body.getOwnerId()).isEqualTo(owner.getId());
    }

    @Test
    void createReview_bookingNotCompleted_returnsError() {
        booking.setStatus(BookingStatus.APPROVED);
        bookingRepository.save(booking);

        CreateReviewRequest request = new CreateReviewRequest();
        request.setBookingId(booking.getId());
        request.setRating(5);
        request.setComment("Premature review");

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/reviews",
                request,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
