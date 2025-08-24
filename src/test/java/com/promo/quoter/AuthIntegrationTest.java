package com.promo.quoter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.promo.quoter.dtos.JwtResponse;
import com.promo.quoter.dtos.LoginDto;
import com.promo.quoter.dtos.SignupDto;
import com.promo.quoter.entities.Users;
import com.promo.quoter.repos.UsersRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("h2")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Transactional
@Component
public class AuthIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsersRepository usersRepository;

    private MockMvc mockMvc;

    private static final String AUTH_BASE_URL = "/user";
    private static final String SIGNUP_URL = AUTH_BASE_URL + "/signup";
    private static final String LOGIN_URL = AUTH_BASE_URL + "/login";

    // Setter methods for manual dependency injection
    public void setWebApplicationContext(WebApplicationContext webApplicationContext) {
        this.webApplicationContext = webApplicationContext;
    }

    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void setUsersRepository(UsersRepository usersRepository) {
        this.usersRepository = usersRepository;
    }

    @BeforeEach
    public void setUp() {
        if (webApplicationContext != null) {
            mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        }
        // Clean up any existing test data
        if (usersRepository != null) {
            usersRepository.deleteAll();
        }
    }

    @Test
    @DisplayName("1. Success User Creation")
    void testSuccessfulUserCreation() throws Exception {
        SignupDto signupDto = SignupDto.builder()
                .username("testuser123")
                .password("securePassword123!")
                .build();

        String requestBody = objectMapper.writeValueAsString(signupDto);

        // When & Then
        // Given
        MvcResult result = mockMvc.perform(post(SIGNUP_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.username").value("testuser123"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.role").exists())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.password").exists()) // Password should be hashed
                .andReturn();

        // Verify user was actually created in database
        String responseBody = result.getResponse().getContentAsString();
        Users createdUser = objectMapper.readValue(responseBody, Users.class);

        assertTrue(usersRepository.existsById(createdUser.getId()));
        Users dbUser = usersRepository.findById(createdUser.getId()).orElse(null);
        assertNotNull(dbUser);
        assertEquals("testuser123", dbUser.getUsername());
        assertTrue(dbUser.isActive());
        // Ensure password is hashed (not plain text)
        assertNotEquals("securePassword123!", dbUser.getPassword());
        assertTrue(dbUser.getPassword().startsWith("$2a$")); // BCrypt hash prefix
    }

    @Test
    @DisplayName("2. Failed User Creation - Invalid Input")
    void testFailedUserCreation_InvalidInput() throws Exception {
        // Test Case 2a: Empty username
        SignupDto invalidSignupDto1 = SignupDto.builder()
                .username("")
                .password("validPassword123!")
                .build();

        mockMvc.perform(post(SIGNUP_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidSignupDto1)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        // Test Case 2b: Null username
        SignupDto invalidSignupDto2 = SignupDto.builder()
                .username(null)
                .password("validPassword123!")
                .build();

        mockMvc.perform(post(SIGNUP_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidSignupDto2)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        // Test Case 2c: Short password
        SignupDto invalidSignupDto3 = SignupDto.builder()
                .username("validuser")
                .password("123")
                .build();

        mockMvc.perform(post(SIGNUP_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidSignupDto3)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        // Test Case 2d: Duplicate username
        // First, create a user
        SignupDto firstUser = SignupDto.builder()
                .username("duplicatetest")
                .password("password123!")
                .build();

        mockMvc.perform(post(SIGNUP_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstUser)))
                .andExpect(status().isCreated());

        // Then try to create another user with same username
        SignupDto duplicateUser = SignupDto.builder()
                .username("duplicatetest")
                .password("differentPassword123!")
                .build();

        mockMvc.perform(post(SIGNUP_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateUser)))
                .andDo(print())
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("3. Success User Login")
    void testSuccessfulUserLogin() throws Exception {
        // Given - Create a user first
        SignupDto signupDto = SignupDto.builder()
                .username("logintest")
                .password("loginPassword123!")
                .build();

        mockMvc.perform(post(SIGNUP_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupDto)))
                .andExpect(status().isCreated());

        // When - Login with created user
        LoginDto loginDto = LoginDto.builder()
                .username("logintest")
                .password("loginPassword123!")
                .build();

        String loginRequestBody = objectMapper.writeValueAsString(loginDto);

        // Then
        MvcResult result = mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequestBody))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.username").value("logintest"))
                .andExpect(jsonPath("$.type").value("BEARER"))
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.issuedAt").exists())
                .andExpect(jsonPath("$.expiry").exists())
                .andReturn();

        // Verify JWT token structure
        String responseBody = result.getResponse().getContentAsString();
        JwtResponse jwtResponse = objectMapper.readValue(responseBody, JwtResponse.class);

        assertNotNull(jwtResponse.getToken());
        assertTrue(jwtResponse.getToken().length() > 50); // JWT tokens are typically long
        assertEquals("BEARER", jwtResponse.getType());
        assertNotNull(jwtResponse.getIssuedAt());
        assertNotNull(jwtResponse.getExpiry());
    }

    @Test
    @DisplayName("4. Failed User Login - Invalid Credentials")
    void testFailedUserLogin_InvalidCredentials() throws Exception {
        // Given - Create a user first
        SignupDto signupDto = SignupDto.builder()
                .username("logintest2")
                .password("correctPassword123!")
                .build();

        mockMvc.perform(post(SIGNUP_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupDto)))
                .andExpect(status().isCreated());

        // Test Case 4a: Wrong password
        LoginDto wrongPasswordDto = LoginDto.builder()
                .username("logintest2")
                .password("wrongPassword123!")
                .build();

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrongPasswordDto)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        // Test Case 4b: Non-existent user
        LoginDto nonExistentUserDto = LoginDto.builder()
                .username("nonexistentuser")
                .password("anyPassword123!")
                .build();

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nonExistentUserDto)))
                .andDo(print())
                .andExpect(status().isInternalServerError());

        // Test Case 4c: Empty credentials
        LoginDto emptyCredentialsDto = LoginDto.builder()
                .username("")
                .password("")
                .build();

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emptyCredentialsDto)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        // Test Case 4d: Null credentials
        LoginDto nullCredentialsDto = LoginDto.builder()
                .username(null)
                .password(null)
                .build();

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nullCredentialsDto)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }


    // Helper method for other integration tests to get valid JWT token
    public String getValidJwtToken() throws Exception {
        // Create a user
        SignupDto signupDto = SignupDto.builder()
                .username("integrationtestuser")
                .password("integrationPassword123!")
                .build();

        mockMvc.perform(post(SIGNUP_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupDto)))
                .andExpect(status().isCreated());

        // Login and get token
        LoginDto loginDto = LoginDto.builder()
                .username("integrationtestuser")
                .password("integrationPassword123!")
                .build();

        MvcResult result = mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JwtResponse jwtResponse = objectMapper.readValue(responseBody, JwtResponse.class);

        return "Bearer " + jwtResponse.getToken();
    }

    // Helper method to get JWT token for specific user credentials
    public String getJwtTokenForUser(String username, String password) throws Exception {
        // Ensure unique usernames across tests
        String uniqueUsername = username + UUID.randomUUID();

        // Create user
        SignupDto signupDto = SignupDto.builder()
                .username(uniqueUsername)
                .password(password)
                .build();

        mockMvc.perform(post(SIGNUP_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupDto)))
                .andExpect(status().isCreated())
                .andDo(print());

        // Login
        LoginDto loginDto = LoginDto.builder()
                .username(uniqueUsername)
                .password(password)
                .build();

        MvcResult result = mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        System.out.println("Login response body: " + responseBody);

        // Adjust JwtResponse to match your actual JSON structure
        JwtResponse jwtResponse = objectMapper.readValue(responseBody, JwtResponse.class);

        if (jwtResponse.getToken() == null) {
            throw new IllegalStateException("JWT token not found in response: " + responseBody);
        }

        return "Bearer " + jwtResponse.getToken();
    }

}