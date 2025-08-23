package com.promo.quoter.controllers;

import com.promo.quoter.dtos.CartQuoteRequest;
import com.promo.quoter.dtos.CartQuoteResponse;
import com.promo.quoter.dtos.CartConfirmResponse;
import com.promo.quoter.services.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Cart", description = "Cart quote and confirmation operations")
public class CartController {

    private final CartService cartService;

    @PostMapping("/quote")
    @Operation(
            summary = "Get cart quote",
            description = "Calculate itemized price breakdown with applied promotions in order",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Cart quote request with items and customer segment",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CartQuoteRequest.class),
                            examples = @ExampleObject(
                                    name = "Sample Cart Request",
                                    value = """
                    {
                        "items": [
                            {
                                "productId": "550e8400-e29b-41d4-a716-446655440000",
                                "qty": 3
                            },
                            {
                                "productId": "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
                                "qty": 2
                            }
                        ],
                        "customerSegment": "REGULAR"
                    }
                    """
                            )
                    )
            )
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Quote calculated successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CartQuoteResponse.class),
                            examples = @ExampleObject(
                                    name = "Sample Quote Response",
                                    value = """
                        {
                            "lineItems": [
                                {
                                    "productId": "550e8400-e29b-41d4-a716-446655440000",
                                    "productName": "Gaming Laptop",
                                    "quantity": 3,
                                    "unitPrice": 1000.00,
                                    "lineTotal": 3000.00,
                                    "discountAmount": 300.00,
                                    "finalLineTotal": 2700.00
                                }
                            ],
                            "appliedPromotions": [
                                {
                                    "promotionId": "promo-123",
                                    "promotionType": "PERCENT_OFF_CATEGORY",
                                    "description": "10% off Electronics",
                                    "discountAmount": 300.00,
                                    "affectedProductIds": ["550e8400-e29b-41d4-a716-446655440000"]
                                }
                            ],
                            "subtotal": 3000.00,
                            "totalDiscount": 300.00,
                            "finalTotal": 2700.00
                        }
                        """
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<CartQuoteResponse> getQuote(@Valid @RequestBody CartQuoteRequest request) {
        log.info("Processing cart quote request for {} items, customer segment: {}",
                request.getItems().size(), request.getCustomerSegment());

        CartQuoteResponse quote = cartService.calculateQuote(request);

        log.info("Quote calculated: subtotal={}, totalDiscount={}, finalTotal={}",
                quote.getSubtotal(), quote.getTotalDiscount(), quote.getFinalTotal());

        return ResponseEntity.ok(quote);
    }

    @PostMapping("/confirm")
    @Operation(
            summary = "Confirm cart and create order",
            description = "Validates stock availability, reserves inventory atomically, and creates an order with final pricing",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Same cart request payload as quote endpoint",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CartQuoteRequest.class),
                            examples = @ExampleObject(
                                    name = "Sample Cart Confirmation",
                                    value = """
                    {
                        "items": [
                            {
                                "productId": "550e8400-e29b-41d4-a716-446655440000",
                                "qty": 3
                            },
                            {
                                "productId": "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
                                "qty": 2
                            }
                        ],
                        "customerSegment": "REGULAR"
                    }
                    """
                            )
                    )
            )
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Order created successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CartConfirmResponse.class),
                            examples = @ExampleObject(
                                    name = "Order Confirmation Response",
                                    value = """
                        {
                            "orderId": "ORD-2024-001234",
                            "finalTotal": 2700.00,
                            "status": "CONFIRMED",
                            "reservedItems": [
                                {
                                    "productId": "550e8400-e29b-41d4-a716-446655440000",
                                    "quantity": 3,
                                    "reservedAt": "2024-01-15T10:30:00Z"
                                }
                            ],
                            "appliedPromotions": [
                                {
                                    "promotionId": "promo-123",
                                    "promotionType": "PERCENT_OFF_CATEGORY",
                                    "description": "10% off Electronics",
                                    "discountAmount": 300.00
                                }
                            ]
                        }
                        """
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "409", description = "Insufficient stock or duplicate order"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<CartConfirmResponse> confirmCart(
            @Valid @RequestBody CartQuoteRequest request,
            @Parameter(
                    description = "Optional idempotency key to prevent duplicate orders. If provided, subsequent requests with the same key will return the same result.",
                    example = "cart-confirm-12345-67890"
            )
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        log.info("Processing cart confirmation for {} items, customer segment: {}, idempotencyKey: {}",
                request.getItems().size(), request.getCustomerSegment(), idempotencyKey);

        CartConfirmResponse confirmation = cartService.confirmCart(request, idempotencyKey);

        log.info("Order confirmed: orderId={}, finalTotal={}, status={}",
                confirmation.getOrderId(), confirmation.getFinalTotal(), confirmation.getStatus());

        return ResponseEntity.status(HttpStatus.CREATED).body(confirmation);
    }
}