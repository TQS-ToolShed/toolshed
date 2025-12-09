package com.toolshed.backend.boundary;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.toolshed.backend.dto.CreateReviewRequest;
import com.toolshed.backend.dto.ReviewResponse;
import com.toolshed.backend.service.ReviewService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReviewController.class)
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ReviewService reviewService;

    @Test
    @DisplayName("Should create review and return response body")
    void createReview() throws Exception {
        UUID bookingId = UUID.randomUUID();
        CreateReviewRequest request = CreateReviewRequest.builder()
                .bookingId(bookingId)
                .rating(5)
                .comment("Excellent!")
                .build();

        ReviewResponse response = ReviewResponse.builder()
                .id(UUID.randomUUID())
                .bookingId(bookingId)
                .rating(5)
                .comment("Excellent!")
                .build();

        when(reviewService.createReview(any(CreateReviewRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/reviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.comment").value("Excellent!"));
    }

    @Test
    @DisplayName("Should return 400 when input is invalid")
    void createReview_invalidInput() throws Exception {
        CreateReviewRequest request = CreateReviewRequest.builder()
                // missing bookingId
                .rating(6) // invalid rating
                .comment("") // blank comment
                .build();

        mockMvc.perform(post("/api/reviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
