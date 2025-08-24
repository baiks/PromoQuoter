package com.promo.quoter;

import com.promo.quoter.dtos.CartConfirmResponse;
import com.promo.quoter.dtos.CartQuoteRequest;
import com.promo.quoter.dtos.CartQuoteResponse;
import com.promo.quoter.entities.*;
import com.promo.quoter.enums.CustomerSegment;
import com.promo.quoter.enums.ProductCategory;
import com.promo.quoter.exception.CustomException;
import com.promo.quoter.implementations.CartServiceImpl;
import com.promo.quoter.repos.OrderRepository;
import com.promo.quoter.repos.ProductRepository;
import com.promo.quoter.repos.PromotionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private PromotionRepository promotionRepository;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private CartServiceImpl cartService;

    private UUID productId1;
    private UUID productId2;
    private UUID promotionId1;
    private Product product1;
    private Product product2;
    private CartQuoteRequest cartRequest;

    @BeforeEach
    void setUp() {
        productId1 = UUID.randomUUID();
        productId2 = UUID.randomUUID();
        promotionId1 = UUID.randomUUID();

        product1 = Product.builder()
                .id(productId1)
                .name("Product 1")
                .price(new BigDecimal("10.00"))
                .stock(100)
                .category(ProductCategory.ELECTRONICS)
                .build();

        product2 = Product.builder()
                .id(productId2)
                .name("Product 2")
                .price(new BigDecimal("20.00"))
                .stock(50)
                .category(ProductCategory.BOOKS)
                .build();

        CartQuoteRequest.CartItem item1 = new CartQuoteRequest.CartItem();
        item1.setProductId(productId1.toString());
        item1.setQty(2);

        CartQuoteRequest.CartItem item2 = new CartQuoteRequest.CartItem();
        item2.setProductId(productId2.toString());
        item2.setQty(1);

        cartRequest = new CartQuoteRequest();
        cartRequest.setItems(List.of(item1, item2));
        cartRequest.setCustomerSegment(CustomerSegment.REGULAR);
    }

    @Test
    void calculateQuote_BasicCalculation_Success() {
        // Arrange
        when(productRepository.findById(productId1)).thenReturn(Optional.of(product1));
        when(productRepository.findById(productId2)).thenReturn(Optional.of(product2));
        when(promotionRepository.findAll()).thenReturn(Collections.emptyList());

        // Act
        CartQuoteResponse response = cartService.calculateQuote(cartRequest);

        // Assert
        assertNotNull(response);
        assertEquals(2, response.getLineItems().size());
        assertEquals(new BigDecimal("40.00"), response.getSubtotal()); // (10*2) + (20*1)
        assertEquals(BigDecimal.ZERO, response.getTotalDiscount());
        assertEquals(new BigDecimal("40.00"), response.getFinalTotal());
        assertTrue(response.getAppliedPromotions().isEmpty());

        // Verify first line item
        CartQuoteResponse.LineItem line1 = response.getLineItems().get(0);
        assertEquals(productId1.toString(), line1.getProductId());
        assertEquals("Product 1", line1.getProductName());
        assertEquals(2, line1.getQuantity());
        assertEquals(new BigDecimal("10.00"), line1.getUnitPrice());
        assertEquals(new BigDecimal("20.00"), line1.getLineTotal());
    }

    @Test
    void calculateQuote_WithPromotionsAndEdgeCases_Success() {
        // Arrange - Test multiple promotion types and edge cases
        PercentOffCategoryPromotion categoryPromo = PercentOffCategoryPromotion.builder()
                .id(promotionId1)
                .description("20% off Electronics")
                .category(ProductCategory.ELECTRONICS)
                .percentOff(new BigDecimal("20"))
                .build();

        BuyXGetYPromotion buyGetPromo = BuyXGetYPromotion.builder()
                .id(UUID.randomUUID())
                .description("Buy 2 Get 1 Free")
                .productId(productId1)
                .buyX(2)
                .getY(1)
                .build();

        // Test with 3 items to trigger BuyXGetY
        CartQuoteRequest.CartItem item = new CartQuoteRequest.CartItem();
        item.setProductId(productId1.toString());
        item.setQty(3);

        CartQuoteRequest promoRequest = new CartQuoteRequest();
        promoRequest.setItems(List.of(item));
        promoRequest.setCustomerSegment(CustomerSegment.REGULAR);

        when(productRepository.findById(productId1)).thenReturn(Optional.of(product1));
        when(promotionRepository.findAll()).thenReturn(List.of(categoryPromo, buyGetPromo));

        // Act
        CartQuoteResponse response = cartService.calculateQuote(promoRequest);

        // Assert
        assertEquals(new BigDecimal("30.00"), response.getSubtotal()); // 10 * 3
        assertTrue(response.getTotalDiscount().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(response.getFinalTotal().compareTo(response.getSubtotal()) < 0);
        assertFalse(response.getAppliedPromotions().isEmpty());
    }

    @Test
    void calculateQuote_ErrorHandling_ThrowsExceptions() {
        // Arrange - Test product not found
        when(productRepository.findById(productId1)).thenReturn(Optional.empty());

        // Act & Assert - Product not found
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> cartService.calculateQuote(cartRequest));
        assertTrue(exception.getMessage().contains("Product not found"));

        // Test invalid product ID format
        CartQuoteRequest.CartItem invalidItem = new CartQuoteRequest.CartItem();
        invalidItem.setProductId("invalid-uuid-format");
        invalidItem.setQty(1);

        CartQuoteRequest invalidRequest = new CartQuoteRequest();
        invalidRequest.setItems(List.of(invalidItem));
        invalidRequest.setCustomerSegment(CustomerSegment.REGULAR);

        // Act & Assert - Invalid UUID format
        assertThrows(IllegalArgumentException.class,
                () -> cartService.calculateQuote(invalidRequest));
    }

    @Test
    void confirmCart_Success_CreatesOrderAndUpdatesStock() {
        // Arrange
        String idempotencyKey = "test-key-123";
        String expectedOrderId = "ORD-2024-123456";

        when(orderRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(productRepository.findById(productId1)).thenReturn(Optional.of(product1));
        when(productRepository.findById(productId2)).thenReturn(Optional.of(product2));
        when(productRepository.findByIdWithLock(productId1)).thenReturn(Optional.of(product1));
        when(productRepository.findByIdWithLock(productId2)).thenReturn(Optional.of(product2));
        when(promotionRepository.findAll()).thenReturn(Collections.emptyList());

        Order savedOrder = Order.builder()
                .orderId(expectedOrderId)
                .finalTotal(new BigDecimal("40.00"))
                .status(Order.OrderStatus.CONFIRMED)
                .createdAt(LocalDateTime.now())
                .orderItems(new ArrayList<>())
                .appliedPromotions(new ArrayList<>())
                .build();

        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        // Act
        CartConfirmResponse response = cartService.confirmCart(cartRequest, idempotencyKey);

        // Assert
        assertNotNull(response);
        assertEquals(expectedOrderId, response.getOrderId());
        assertEquals(new BigDecimal("40.00"), response.getFinalTotal());
        assertEquals(CartConfirmResponse.OrderStatus.CONFIRMED, response.getStatus());

        verify(productRepository, times(2)).save(any(Product.class)); // Stock updates
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void confirmCart_IdempotencyAndErrorHandling_HandlesCorrectly() {
        // Test duplicate idempotency key
        String duplicateKey = "duplicate-key";
        Order existingOrder = Order.builder()
                .orderId("EXISTING-ORDER")
                .finalTotal(new BigDecimal("100.00"))
                .status(Order.OrderStatus.CONFIRMED)
                .createdAt(LocalDateTime.now())
                .orderItems(new ArrayList<>())
                .appliedPromotions(new ArrayList<>())
                .build();

        when(orderRepository.findByIdempotencyKey(duplicateKey)).thenReturn(Optional.of(existingOrder));

        // Act - Test idempotency
        CartConfirmResponse response = cartService.confirmCart(cartRequest, duplicateKey);

        // Assert - Returns existing order
        assertEquals("EXISTING-ORDER", response.getOrderId());
        assertEquals(new BigDecimal("100.00"), response.getFinalTotal());
        verify(productRepository, never()).findByIdWithLock(any());
        verify(productRepository, never()).save(any(Product.class));

        // Test insufficient stock
        Product lowStockProduct = Product.builder()
                .id(productId1)
                .name("Low Stock Product")
                .price(new BigDecimal("10.00"))
                .stock(1) // Only 1 in stock, but we want 2
                .category(ProductCategory.ELECTRONICS)
                .build();

        when(orderRepository.findByIdempotencyKey("stock-test")).thenReturn(Optional.empty());
        when(productRepository.findById(productId1)).thenReturn(Optional.of(lowStockProduct));
        when(productRepository.findById(productId2)).thenReturn(Optional.of(product2));

        // Act & Assert - Insufficient stock
        CustomException stockException = assertThrows(CustomException.class,
                () -> cartService.confirmCart(cartRequest, "stock-test"));
        assertTrue(stockException.getMessage().contains("Insufficient stock"));
    }

    @Test
    void calculateQuote_SpecialScenariosAndBoundaryConditions_HandlesCorrectly() {
        // Test empty cart
        CartQuoteRequest emptyRequest = new CartQuoteRequest();
        emptyRequest.setItems(Collections.emptyList());
        emptyRequest.setCustomerSegment(CustomerSegment.REGULAR);
        when(promotionRepository.findAll()).thenReturn(Collections.emptyList());

        CartQuoteResponse emptyResponse = cartService.calculateQuote(emptyRequest);
        assertTrue(emptyResponse.getLineItems().isEmpty());
        assertEquals(BigDecimal.ZERO, emptyResponse.getSubtotal());
        assertEquals(BigDecimal.ZERO, emptyResponse.getFinalTotal());

        // Test zero quantity items (should handle gracefully)
        CartQuoteRequest.CartItem zeroQtyItem = new CartQuoteRequest.CartItem();
        zeroQtyItem.setProductId(productId1.toString());
        zeroQtyItem.setQty(0);

        CartQuoteRequest zeroQtyRequest = new CartQuoteRequest();
        zeroQtyRequest.setItems(List.of(zeroQtyItem));
        zeroQtyRequest.setCustomerSegment(CustomerSegment.REGULAR);

        when(productRepository.findById(productId1)).thenReturn(Optional.of(product1));

        CartQuoteResponse zeroResponse = cartService.calculateQuote(zeroQtyRequest);
        assertNotNull(zeroResponse);

        // Test null customer segment
        cartRequest.setCustomerSegment(null);
        when(productRepository.findById(productId1)).thenReturn(Optional.of(product1));
        when(productRepository.findById(productId2)).thenReturn(Optional.of(product2));
        when(promotionRepository.findAll()).thenReturn(Collections.emptyList());

        CartQuoteResponse nullSegmentResponse = cartService.calculateQuote(cartRequest);
        assertNotNull(nullSegmentResponse);
        assertEquals(new BigDecimal("40.00"), nullSegmentResponse.getSubtotal());
    }
}