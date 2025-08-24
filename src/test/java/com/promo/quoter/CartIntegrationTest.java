package com.promo.quoter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.promo.quoter.dtos.CartConfirmResponse;
import com.promo.quoter.dtos.CartQuoteRequest;
import com.promo.quoter.dtos.CartQuoteResponse;
import com.promo.quoter.enums.CustomerSegment;
import com.promo.quoter.repos.UsersRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("h2")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Transactional
public class CartIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsersRepository usersRepository;

    // Remove @Autowired annotation and create manually
    private AuthIntegrationTest authIntegrationTest;

    private MockMvc mockMvc;

    private static final String CART_BASE_URL = "/cart";
    private static final String QUOTE_URL = CART_BASE_URL + "/quote";
    private static final String CONFIRM_URL = CART_BASE_URL + "/confirm";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        // Clean up any existing test data
        usersRepository.deleteAll();

        // Create AuthIntegrationTest instance and set up its dependencies manually
        authIntegrationTest = new AuthIntegrationTest();
        // Set the required fields using Spring's test context
        authIntegrationTest.setWebApplicationContext(webApplicationContext);
        authIntegrationTest.setObjectMapper(objectMapper);
        authIntegrationTest.setUsersRepository(usersRepository);
        authIntegrationTest.setUp();
    }


    @Test
    @DisplayName("Cart Scenario 1: Successful Quote Request with Valid Token")
    void testSuccessfulQuoteWithValidToken() throws Exception {
        String validToken = authIntegrationTest.getJwtTokenForUser("cartuser1", "password123!");

        CartQuoteRequest quoteRequest = CartQuoteRequest.builder()
                .items(Arrays.asList(
                        CartQuoteRequest.CartItem.builder().productId("550e8400-e29b-41d4-a716-446655440000").qty(3).build(),
                        CartQuoteRequest.CartItem.builder().productId("6ba7b810-9dad-11d1-80b4-00c04fd430c8").qty(2).build()
                ))
                .customerSegment(CustomerSegment.REGULAR)
                .build();

        String requestBody = objectMapper.writeValueAsString(quoteRequest);

        MvcResult result = mockMvc.perform(post(QUOTE_URL)
                        .header(HttpHeaders.AUTHORIZATION, validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lineItems").isArray())
                .andExpect(jsonPath("$.lineItems").isNotEmpty())
                .andReturn();

        CartQuoteResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), CartQuoteResponse.class);

        assertNotNull(response);
        assertFalse(response.getLineItems().isEmpty());
    }

    @Test
    @DisplayName("Cart Scenario 2: Quote Request with Unauthorized Access (No Token)")
    void testQuoteRequestWithoutToken() throws Exception {
        // Given
        CartQuoteRequest quoteRequest = CartQuoteRequest.builder()
                .items(Arrays.asList(
                        CartQuoteRequest.CartItem.builder()
                                .productId("550e8400-e29b-41d4-a716-446655440000")
                                .qty(1)
                                .build()
                ))
                .customerSegment(CustomerSegment.REGULAR)
                .build();

        String requestBody = objectMapper.writeValueAsString(quoteRequest);

        // When & Then
        mockMvc.perform(post(QUOTE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Cart Scenario 3: Quote Request with Invalid Token")
    void testQuoteRequestWithInvalidToken() throws Exception {
        // Given
        String invalidToken = "Bearer invalid.jwt.token.here";

        CartQuoteRequest quoteRequest = CartQuoteRequest.builder()
                .items(Arrays.asList(
                        CartQuoteRequest.CartItem.builder()
                                .productId("550e8400-e29b-41d4-a716-446655440000")
                                .qty(1)
                                .build()
                ))
                .customerSegment(CustomerSegment.REGULAR)
                .build();

        String requestBody = objectMapper.writeValueAsString(quoteRequest);

        // When & Then
        mockMvc.perform(post(QUOTE_URL)
                        .header(HttpHeaders.AUTHORIZATION, invalidToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Cart Scenario 4: Quote Request with Invalid Request Data")
    void testQuoteRequestWithInvalidData() throws Exception {
        // Given - Get valid JWT token
        String validToken = authIntegrationTest.getJwtTokenForUser("cartuser4", "password123!");

        // Test Case 4a: Empty items list
        CartQuoteRequest emptyItemsRequest = CartQuoteRequest.builder()
                .items(Collections.emptyList())
                .customerSegment(CustomerSegment.REGULAR)
                .build();

        mockMvc.perform(post(QUOTE_URL)
                        .header(HttpHeaders.AUTHORIZATION, validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emptyItemsRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        // Test Case 4b: Null customer segment
        CartQuoteRequest nullSegmentRequest = CartQuoteRequest.builder()
                .items(Arrays.asList(
                        CartQuoteRequest.CartItem.builder()
                                .productId("550e8400-e29b-41d4-a716-446655440000")
                                .qty(1)
                                .build()
                ))
                .customerSegment(null)
                .build();

        mockMvc.perform(post(QUOTE_URL)
                        .header(HttpHeaders.AUTHORIZATION, validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nullSegmentRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        // Test Case 4c: Invalid product ID format
        CartQuoteRequest invalidProductIdRequest = CartQuoteRequest.builder()
                .items(Arrays.asList(
                        CartQuoteRequest.CartItem.builder()
                                .productId("invalid-product-id")
                                .qty(1)
                                .build()
                ))
                .customerSegment(CustomerSegment.REGULAR)
                .build();

        mockMvc.perform(post(QUOTE_URL)
                        .header(HttpHeaders.AUTHORIZATION, validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidProductIdRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());

        // Test Case 4d: Zero or negative quantity
        CartQuoteRequest invalidQuantityRequest = CartQuoteRequest.builder()
                .items(Arrays.asList(
                        CartQuoteRequest.CartItem.builder()
                                .productId("550e8400-e29b-41d4-a716-446655440000")
                                .qty(0)
                                .build()
                ))
                .customerSegment(CustomerSegment.REGULAR)
                .build();

        mockMvc.perform(post(QUOTE_URL)
                        .header(HttpHeaders.AUTHORIZATION, validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidQuantityRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Cart Scenario 5: Successful Cart Confirmation with Valid Token and Idempotency")
    void testSuccessfulCartConfirmationWithIdempotency() throws Exception {
        // Given - Get valid JWT token
        String validToken = authIntegrationTest.getJwtTokenForUser("cartuser5", "password123!");

        CartQuoteRequest confirmRequest = CartQuoteRequest.builder()
                .items(Arrays.asList(
                        CartQuoteRequest.CartItem.builder()
                                .productId("550e8400-e29b-41d4-a716-446655440000")
                                .qty(2)
                                .build(),
                        CartQuoteRequest.CartItem.builder()
                                .productId("6ba7b810-9dad-11d1-80b4-00c04fd430c8")
                                .qty(1)
                                .build()
                ))
                .customerSegment(CustomerSegment.PREMIUM)
                .build();

        String requestBody = objectMapper.writeValueAsString(confirmRequest);
        String idempotencyKey = "test-idempotency-" + UUID.randomUUID().toString();

        // When & Then - First request
        MvcResult firstResult = mockMvc.perform(post(CONFIRM_URL)
                        .header(HttpHeaders.AUTHORIZATION, validToken)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.orderId").exists())
                .andExpect(jsonPath("$.orderId").isString())
                .andExpect(jsonPath("$.finalTotal").isNumber())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.reservedItems").isArray())
                .andExpect(jsonPath("$.reservedItems").isNotEmpty())
                .andExpect(jsonPath("$.appliedPromotions").isArray())
                .andReturn();

        // Verify first response structure
        String firstResponseBody = firstResult.getResponse().getContentAsString();
        CartConfirmResponse firstResponse = objectMapper.readValue(firstResponseBody, CartConfirmResponse.class);

        assertNotNull(firstResponse);
        assertNotNull(firstResponse.getOrderId());
        assertTrue(firstResponse.getOrderId().startsWith("ORD-"));
        assertNotNull(firstResponse.getFinalTotal());
        assertEquals("CONFIRMED", firstResponse.getStatus());
        assertNotNull(firstResponse.getReservedItems());
        assertTrue(firstResponse.getReservedItems().size() > 0);

        // When & Then - Second request with same idempotency key (should return same result)
        MvcResult secondResult = mockMvc.perform(post(CONFIRM_URL)
                        .header(HttpHeaders.AUTHORIZATION, validToken)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(firstResponse.getOrderId()))
                .andExpect(jsonPath("$.finalTotal").value(firstResponse.getFinalTotal()))
                .andReturn();

        String secondResponseBody = secondResult.getResponse().getContentAsString();
        CartConfirmResponse secondResponse = objectMapper.readValue(secondResponseBody, CartConfirmResponse.class);

        // Verify idempotency - responses should be identical
        assertEquals(firstResponse.getOrderId(), secondResponse.getOrderId());
        assertEquals(firstResponse.getFinalTotal(), secondResponse.getFinalTotal());
        assertEquals(firstResponse.getStatus(), secondResponse.getStatus());
    }

    @Test
    @DisplayName("Cart Scenario 6: Cart Confirmation with Product Not Found")
    void testCartConfirmationWithProductNotFound() throws Exception {
        // Given - Get valid JWT token
        String validToken = authIntegrationTest.getJwtTokenForUser("cartuser6", "password123!");

        CartQuoteRequest confirmRequest = CartQuoteRequest.builder()
                .items(Arrays.asList(
                        CartQuoteRequest.CartItem.builder()
                                .productId("99999999-9999-9999-9999-999999999999") // Non-existent product
                                .qty(1)
                                .build()
                ))
                .customerSegment(CustomerSegment.REGULAR)
                .build();

        String requestBody = objectMapper.writeValueAsString(confirmRequest);

        // When & Then
        mockMvc.perform(post(CONFIRM_URL)
                        .header(HttpHeaders.AUTHORIZATION, validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("Cart Scenario 7: Cart Confirmation with Insufficient Stock")
    void testCartConfirmationWithInsufficientStock() throws Exception {
        // Given - Get valid JWT token
        String validToken = authIntegrationTest.getJwtTokenForUser("cartuser7", "password123!");

        CartQuoteRequest confirmRequest = CartQuoteRequest.builder()
                .items(Arrays.asList(
                        CartQuoteRequest.CartItem.builder()
                                .productId("550e8400-e29b-41d4-a716-446655440000")
                                .qty(999999) // Extremely high quantity to trigger insufficient stock
                                .build()
                ))
                .customerSegment(CustomerSegment.REGULAR)
                .build();

        String requestBody = objectMapper.writeValueAsString(confirmRequest);

        // When & Then
        mockMvc.perform(post(CONFIRM_URL)
                        .header(HttpHeaders.AUTHORIZATION, validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Insufficient stock")));
    }

    @Test
    @DisplayName("Cart Scenario 8: Cart Confirmation Without Authorization")
    void testCartConfirmationWithoutAuthorization() throws Exception {
        // Given
        CartQuoteRequest confirmRequest = CartQuoteRequest.builder()
                .items(Arrays.asList(
                        CartQuoteRequest.CartItem.builder()
                                .productId("550e8400-e29b-41d4-a716-446655440000")
                                .qty(1)
                                .build()
                ))
                .customerSegment(CustomerSegment.REGULAR)
                .build();

        String requestBody = objectMapper.writeValueAsString(confirmRequest);

        // When & Then
        mockMvc.perform(post(CONFIRM_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Cart Scenario 9: Premium Customer Segment Quote")
    void testPremiumCustomerQuote() throws Exception {
        // Given - Get valid JWT token
        String validToken = authIntegrationTest.getJwtTokenForUser("premiumuser", "password123!");

        CartQuoteRequest premiumQuoteRequest = CartQuoteRequest.builder()
                .items(Arrays.asList(
                        CartQuoteRequest.CartItem.builder()
                                .productId("550e8400-e29b-41d4-a716-446655440000")
                                .qty(5)
                                .build()
                ))
                .customerSegment(CustomerSegment.PREMIUM)
                .build();

        String requestBody = objectMapper.writeValueAsString(premiumQuoteRequest);

        // When & Then
        MvcResult result = mockMvc.perform(post(QUOTE_URL)
                        .header(HttpHeaders.AUTHORIZATION, validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lineItems").isArray())
                .andExpect(jsonPath("$.appliedPromotions").isArray())
                .andExpect(jsonPath("$.subtotal").isNumber())
                .andExpect(jsonPath("$.finalTotal").isNumber())
                .andReturn();

        // Verify premium customer gets appropriate discounts
        String responseBody = result.getResponse().getContentAsString();
        CartQuoteResponse response = objectMapper.readValue(responseBody, CartQuoteResponse.class);

        assertNotNull(response);
        // Premium customers should typically get better discounts
        assertTrue(response.getTotalDiscount().doubleValue() >= 0);
        assertTrue(response.getFinalTotal().doubleValue() <= response.getSubtotal().doubleValue());
    }

    @Test
    @DisplayName("Cart Scenario 10: Multiple Items Quote with Different Customer Segments")
    void testMultipleItemsQuoteWithDifferentSegments() throws Exception {
        // Given - Get valid JWT token
        String validToken = authIntegrationTest.getJwtTokenForUser("multiuser", "password123!");

        CartQuoteRequest multiItemRequest = CartQuoteRequest.builder()
                .items(Arrays.asList(
                        CartQuoteRequest.CartItem.builder()
                                .productId("550e8400-e29b-41d4-a716-446655440000")
                                .qty(2)
                                .build(),
                        CartQuoteRequest.CartItem.builder()
                                .productId("6ba7b810-9dad-11d1-80b4-00c04fd430c8")
                                .qty(3)
                                .build(),
                        CartQuoteRequest.CartItem.builder()
                                .productId("123e4567-e89b-12d3-a456-426614174000")
                                .qty(1)
                                .build()
                ))
                .customerSegment(CustomerSegment.VIP)
                .build();

        String requestBody = objectMapper.writeValueAsString(multiItemRequest);

        // When & Then
        MvcResult result = mockMvc.perform(post(QUOTE_URL)
                        .header(HttpHeaders.AUTHORIZATION, validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lineItems").isArray())
                .andExpect(jsonPath("$.lineItems.length()").value(3))
                .andExpect(jsonPath("$.appliedPromotions").isArray())
                .andReturn();

        // Verify all line items are present
        String responseBody = result.getResponse().getContentAsString();
        CartQuoteResponse response = objectMapper.readValue(responseBody, CartQuoteResponse.class);

        assertNotNull(response);
        assertEquals(3, response.getLineItems().size());

        // Verify each product ID is in the response
        response.getLineItems().forEach(lineItem -> {
            assertTrue(Arrays.asList(
                    "550e8400-e29b-41d4-a716-446655440000",
                    "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
                    "123e4567-e89b-12d3-a456-426614174000"
            ).contains(lineItem.getProductId()));
        });
    }

    @Test
    @DisplayName("Cart Scenario 11: Quote Request with Malformed JSON")
    void testQuoteRequestWithMalformedJson() throws Exception {
        // Given - Get valid JWT token
        String validToken = authIntegrationTest.getJwtTokenForUser("jsonuser", "password123!");

        String malformedJson = "{ \"items\": [ { \"productId\": \"550e8400-e29b-41d4-a716-446655440000\", \"qty\": }"; // Missing value and closing braces

        // When & Then
        mockMvc.perform(post(QUOTE_URL)
                        .header(HttpHeaders.AUTHORIZATION, validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Cart Scenario 12: End-to-End Quote to Confirmation Flow")
    void testEndToEndQuoteToConfirmationFlow() throws Exception {
        // Given - Get valid JWT token
        String validToken = authIntegrationTest.getJwtTokenForUser("e2euser", "password123!");

        CartQuoteRequest request = CartQuoteRequest.builder()
                .items(Arrays.asList(
                        CartQuoteRequest.CartItem.builder()
                                .productId("550e8400-e29b-41d4-a716-446655440000")
                                .qty(2)
                                .build()
                ))
                .customerSegment(CustomerSegment.REGULAR)
                .build();

        String requestBody = objectMapper.writeValueAsString(request);

        // Step 1: Get Quote
        MvcResult quoteResult = mockMvc.perform(post(QUOTE_URL)
                        .header(HttpHeaders.AUTHORIZATION, validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        String quoteResponseBody = quoteResult.getResponse().getContentAsString();
        CartQuoteResponse quoteResponse = objectMapper.readValue(quoteResponseBody, CartQuoteResponse.class);

        // Step 2: Confirm Cart with same data
        String idempotencyKey = "e2e-test-" + UUID.randomUUID().toString();

        MvcResult confirmResult = mockMvc.perform(post(CONFIRM_URL)
                        .header(HttpHeaders.AUTHORIZATION, validToken)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andDo(print())
                .andExpect(status().isCreated())
                .andReturn();

        String confirmResponseBody = confirmResult.getResponse().getContentAsString();
        CartConfirmResponse confirmResponse = objectMapper.readValue(confirmResponseBody, CartConfirmResponse.class);

        // Verify consistency between quote and confirmation
        assertEquals(quoteResponse.getFinalTotal(), confirmResponse.getFinalTotal());
        assertNotNull(confirmResponse.getOrderId());
        assertEquals("CONFIRMED", confirmResponse.getStatus());

        // Verify applied promotions are consistent
        if (quoteResponse.getAppliedPromotions() != null && confirmResponse.getAppliedPromotions() != null) {
            assertEquals(quoteResponse.getAppliedPromotions().size(), confirmResponse.getAppliedPromotions().size());
        }
    }

    // Helper methods for setting up test data if needed
    private CartQuoteRequest createBasicQuoteRequest() {
        return CartQuoteRequest.builder()
                .items(Arrays.asList(
                        CartQuoteRequest.CartItem.builder()
                                .productId("550e8400-e29b-41d4-a716-446655440000")
                                .qty(1)
                                .build()
                ))
                .customerSegment(CustomerSegment.REGULAR)
                .build();
    }

    private String createValidJwtTokenForTest(String username) throws Exception {
        return authIntegrationTest.getJwtTokenForUser(username, "password123!");
    }
}