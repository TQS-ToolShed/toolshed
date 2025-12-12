package com.toolshed.backend.boundary;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.toolshed.backend.dto.LoginRequest;
import com.toolshed.backend.dto.LoginResponse;
import com.toolshed.backend.dto.RegisterRequest;
import com.toolshed.backend.repository.entities.User;
import com.toolshed.backend.repository.enums.UserRole;
import com.toolshed.backend.repository.enums.UserStatus;
import com.toolshed.backend.service.AuthService;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private AuthService authService;

        @Autowired
        private ObjectMapper objectMapper;

        private User testUser;
        private RegisterRequest registerRequest;
        private LoginRequest loginRequest;

        @BeforeEach
        void setUp() {
                testUser = User.builder()
                                .firstName("John")
                                .lastName("Doe")
                                .email("john.doe@example.com")
                                .password("password123")
                                .role(UserRole.RENTER)
                                .status(UserStatus.ACTIVE)
                                .reputationScore(0.0)
                                .build();

                registerRequest = RegisterRequest.builder()
                                .firstName("John")
                                .lastName("Doe")
                                .email("john.doe@example.com")
                                .password("password123")
                                .role(UserRole.RENTER)
                                .build();

                loginRequest = LoginRequest.builder()
                                .email("john.doe@example.com")
                                .password("password123")
                                .build();
        }

        @Test
        void testRegisterSuccess() throws Exception {
                when(authService.register(any(RegisterRequest.class))).thenReturn(testUser);

                mockMvc.perform(post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(registerRequest)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.firstName").value("John"))
                                .andExpect(jsonPath("$.lastName").value("Doe"))
                                .andExpect(jsonPath("$.email").value("john.doe@example.com"));
        }

        @Test
        void testRegisterError() throws Exception {
                when(authService.register(any(RegisterRequest.class)))
                                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use"));

                mockMvc.perform(post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(registerRequest)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void testRegisterValidationErrorMissingFirstName() throws Exception {
                RegisterRequest invalidRequest = RegisterRequest.builder()
                                .lastName("Doe")
                                .email("john.doe@example.com")
                                .password("password123")
                                .role(UserRole.RENTER)
                                .build();

                mockMvc.perform(post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalidRequest)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void testRegisterValidationErrorInvalidEmail() throws Exception {
                RegisterRequest invalidRequest = RegisterRequest.builder()
                                .firstName("John")
                                .lastName("Doe")
                                .email("invalid-email")
                                .password("password123")
                                .role(UserRole.RENTER)
                                .build();

                mockMvc.perform(post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalidRequest)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void testLoginSuccess() throws Exception {
                LoginResponse loginResponse = LoginResponse.builder()
                                .token("mock-token-12345")
                                .user(testUser)
                                .build();

                when(authService.login(any(LoginRequest.class))).thenReturn(loginResponse);

                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginRequest)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.token").value("mock-token-12345"))
                                .andExpect(jsonPath("$.user.email").value("john.doe@example.com"));
        }

        @Test
        void testLoginError() throws Exception {
                when(authService.login(any(LoginRequest.class)))
                                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                                                "Invalid email or password"));

                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginRequest)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void testLoginValidationErrorMissingEmail() throws Exception {
                LoginRequest invalidRequest = LoginRequest.builder()
                                .password("password123")
                                .build();

                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalidRequest)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void testCheckEmailAvailabilityExists() throws Exception {
                when(authService.isEmailTaken("taken@example.com")).thenReturn(true);

                mockMvc.perform(get("/api/auth/check-email")
                                .param("email", "taken@example.com"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").value(true));
        }

        @Test
        void testCheckEmailAvailabilityNotExists() throws Exception {
                when(authService.isEmailTaken("available@example.com")).thenReturn(false);

                mockMvc.perform(get("/api/auth/check-email")
                                .param("email", "available@example.com"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").value(false));
        }
}
