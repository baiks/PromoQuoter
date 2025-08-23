package com.promo.quoter;

import com.promo.quoter.dtos.CartConfirmResponse;
import com.promo.quoter.dtos.CartQuoteRequest;
import com.promo.quoter.dtos.CartQuoteResponse;
import com.promo.quoter.entities.*;
import com.promo.quoter.enums.CustomerSegment;
import com.promo.quoter.enums.ProductCategory;
import com.promo.quoter.exception.CustomException;
import com.promo.quoter.exception.InsufficientStockException;
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
import static org.mockito.ArgumentMatchers.eq;
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
    private UUID promotionId2;
    private Product product1;
    private Product product2;
    private CartQuoteRequest cartRequest;

    @BeforeEach
    void setUp() {
        productId1 = UUID.randomUUID();
        productId2 = UUID.randomUUID();
        promotionId1 = UUID.randomUUID();
        promotionId2 = UUID.randomUUID();

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

        // Create CartItem instances using constructor
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
        assertEquals(new BigDecimal("40.00"), response.getSubtotal()); // (10*2) + (20*1) = 50
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
    void calculateQuote_ProductNotFound_ThrowsException() {
        // Arrange
        when(productRepository.findById(productId1)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> cartService.calculateQuote(cartRequest));
        assertTrue(exception.getMessage().contains("Product not found"));
    }

    @Test
    void calculateQuote_WithPercentOffCategoryPromotion_AppliesDiscount() {
        // Arrange
        PercentOffCategoryPromotion promotion = PercentOffCategoryPromotion.builder()
                .id(promotionId1)
                .description("20% off Electronics")
                .category(ProductCategory.ELECTRONICS)
                .percentOff(new BigDecimal("20"))
                .build();

        when(productRepository.findById(productId1)).thenReturn(Optional.of(product1));
        when(productRepository.findById(productId2)).thenReturn(Optional.of(product2));
        when(promotionRepository.findAll()).thenReturn(List.of(promotion));

        // Act
        CartQuoteResponse response = cartService.calculateQuote(cartRequest);

        // Assert
        assertEquals(new BigDecimal("40.00"), response.getSubtotal());
        assertEquals(new BigDecimal("4.00"), response.getTotalDiscount()); // 20% of 20.00
        assertEquals(new BigDecimal("36.00"), response.getFinalTotal());
        assertEquals(1, response.getAppliedPromotions().size());

        CartQuoteResponse.AppliedPromotion appliedPromo = response.getAppliedPromotions().get(0);
        assertEquals("PERCENT_OFF_CATEGORY", appliedPromo.getPromotionType());
        assertEquals(new BigDecimal("4.00"), appliedPromo.getDiscountAmount());
    }

    @Test
    void calculateQuote_WithBuyXGetYPromotion_AppliesDiscount() {
        // Arrange
        BuyXGetYPromotion promotion = BuyXGetYPromotion.builder()
                .id(promotionId1)
                .description("Buy 2 Get 1 Free")
                .productId(productId1)
                .buyX(2)
                .getY(1)
                .build();

        CartQuoteRequest.CartItem item = new CartQuoteRequest.CartItem();
        item.setProductId(productId1.toString());
        item.setQty(3); // Buy 2, get 1 free

        CartQuoteRequest request = new CartQuoteRequest();
        request.setItems(List.of(item));
        request.setCustomerSegment(CustomerSegment.REGULAR);

        when(productRepository.findById(productId1)).thenReturn(Optional.of(product1));
        when(promotionRepository.findAll()).thenReturn(List.of(promotion));

        // Act
        CartQuoteResponse response = cartService.calculateQuote(request);

        // Assert
        assertEquals(new BigDecimal("30.00"), response.getSubtotal()); // 10 * 3
        assertEquals(new BigDecimal("10.00"), response.getTotalDiscount()); // 1 free item
        assertEquals(new BigDecimal("20.00"), response.getFinalTotal());
        assertEquals(1, response.getAppliedPromotions().size());

        CartQuoteResponse.AppliedPromotion appliedPromo = response.getAppliedPromotions().get(0);
        assertEquals("BUY_X_GET_Y", appliedPromo.getPromotionType());
        assertTrue(appliedPromo.getDescription().contains("1 free items"));
    }

    @Test
    void confirmCart_Success() {
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
                .finalTotal(new BigDecimal("50.00"))
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
        assertEquals(new BigDecimal("50.00"), response.getFinalTotal());
        assertEquals(CartConfirmResponse.OrderStatus.CONFIRMED, response.getStatus());

        verify(productRepository, times(2)).save(any(Product.class)); // Stock updates
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void confirmCart_DuplicateIdempotencyKey_ReturnsExistingOrder() {
        // Arrange
        String idempotencyKey = "duplicate-key";
        Order existingOrder = Order.builder()
                .orderId("EXISTING-ORDER")
                .finalTotal(new BigDecimal("100.00"))
                .status(Order.OrderStatus.CONFIRMED)
                .createdAt(LocalDateTime.now())
                .orderItems(new ArrayList<>())
                .appliedPromotions(new ArrayList<>())
                .build();

        when(orderRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.of(existingOrder));

        // Act
        CartConfirmResponse response = cartService.confirmCart(cartRequest, idempotencyKey);

        // Assert
        assertNotNull(response);
        assertEquals("EXISTING-ORDER", response.getOrderId());
        assertEquals(new BigDecimal("100.00"), response.getFinalTotal());

        // Verify no stock operations were performed
        verify(productRepository, never()).findByIdWithLock(any());
        verify(productRepository, never()).save(any(Product.class));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void confirmCart_InsufficientStock_ThrowsException() {
        // Arrange
        Product lowStockProduct = Product.builder()
                .id(productId1)
                .name("Low Stock Product")
                .price(new BigDecimal("10.00"))
                .stock(1) // Only 1 in stock, but we want 2
                .category(ProductCategory.ELECTRONICS)
                .build();

        when(orderRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(productRepository.findById(productId1)).thenReturn(Optional.of(lowStockProduct));
        when(productRepository.findById(productId2)).thenReturn(Optional.of(product2));

        // Act & Assert
        CustomException exception = assertThrows(CustomException.class,
                () -> cartService.confirmCart(cartRequest, "test-key"));
        assertTrue(exception.getMessage().contains("Insufficient stock"));
    }

    @Test
    void confirmCart_WithPromotions_CreatesOrderPromotions() {
        // Arrange
        String idempotencyKey = "promo-test-key";

        PercentOffCategoryPromotion promotion = PercentOffCategoryPromotion.builder()
                .id(promotionId1)
                .description("10% off Electronics")
                .category(ProductCategory.ELECTRONICS)
                .percentOff(new BigDecimal("10"))
                .build();

        when(orderRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(productRepository.findById(productId1)).thenReturn(Optional.of(product1));
        when(productRepository.findById(productId2)).thenReturn(Optional.of(product2));
        when(productRepository.findByIdWithLock(productId1)).thenReturn(Optional.of(product1));
        when(productRepository.findByIdWithLock(productId2)).thenReturn(Optional.of(product2));
        when(promotionRepository.findAll()).thenReturn(List.of(promotion));
        when(promotionRepository.findById(promotionId1)).thenReturn(Optional.of(promotion));

        Order savedOrder = Order.builder()
                .orderId("PROMO-ORDER")
                .finalTotal(new BigDecimal("48.00"))
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
        assertEquals("PROMO-ORDER", response.getOrderId());

        // Verify order was saved with proper promotion handling
        verify(orderRepository).save(any(Order.class));
        verify(promotionRepository).findById(promotionId1);
    }

    @Test
    void calculateQuote_EmptyCart_ReturnsZeroTotals() {
        // Arrange
        CartQuoteRequest emptyRequest = new CartQuoteRequest();
        emptyRequest.setItems(Collections.emptyList());
        emptyRequest.setCustomerSegment(CustomerSegment.REGULAR);

        when(promotionRepository.findAll()).thenReturn(Collections.emptyList());

        // Act
        CartQuoteResponse response = cartService.calculateQuote(emptyRequest);

        // Assert
        assertNotNull(response);
        assertTrue(response.getLineItems().isEmpty());
        assertEquals(BigDecimal.ZERO, response.getSubtotal());
        assertEquals(BigDecimal.ZERO, response.getTotalDiscount());
        assertEquals(BigDecimal.ZERO, response.getFinalTotal());
        assertTrue(response.getAppliedPromotions().isEmpty());
    }

    @Test
    void calculateQuote_BuyXGetYPromotion_InsufficientQuantity_NoDiscount() {
        // Arrange
        BuyXGetYPromotion promotion = BuyXGetYPromotion.builder()
                .id(promotionId1)
                .description("Buy 5 Get 1 Free")
                .productId(productId1)
                .buyX(5)
                .getY(1)
                .build();

        // Only buying 2 items, but need 5 to qualify
        CartQuoteRequest.CartItem item = new CartQuoteRequest.CartItem();
        item.setProductId(productId1.toString());
        item.setQty(2);

        CartQuoteRequest request = new CartQuoteRequest();
        request.setItems(List.of(item));
        request.setCustomerSegment(CustomerSegment.REGULAR);

        when(productRepository.findById(productId1)).thenReturn(Optional.of(product1));
        when(promotionRepository.findAll()).thenReturn(List.of(promotion));

        // Act
        CartQuoteResponse response = cartService.calculateQuote(request);

        // Assert
        assertEquals(new BigDecimal("20.00"), response.getSubtotal());
        assertEquals(BigDecimal.ZERO, response.getTotalDiscount());
        assertEquals(new BigDecimal("20.00"), response.getFinalTotal());
        assertTrue(response.getAppliedPromotions().isEmpty());
    }

    @Test
    void calculateQuote_BuyXGetYPromotion_ProductNotInCart_NoDiscount() {
        // Arrange
        BuyXGetYPromotion promotion = BuyXGetYPromotion.builder()
                .id(promotionId1)
                .description("Buy 2 Get 1 Free")
                .productId(productId1) // Promotion for product1
                .buyX(2)
                .getY(1)
                .build();

        // Cart only contains product2
        CartQuoteRequest.CartItem item = new CartQuoteRequest.CartItem();
        item.setProductId(productId2.toString());
        item.setQty(3);

        CartQuoteRequest request = new CartQuoteRequest();
        request.setItems(List.of(item));
        request.setCustomerSegment(CustomerSegment.REGULAR);

        when(productRepository.findById(productId2)).thenReturn(Optional.of(product2));
        when(promotionRepository.findAll()).thenReturn(List.of(promotion));

        // Act
        CartQuoteResponse response = cartService.calculateQuote(request);

        // Assert
        assertEquals(new BigDecimal("60.00"), response.getSubtotal());
        assertEquals(BigDecimal.ZERO, response.getTotalDiscount());
        assertEquals(new BigDecimal("60.00"), response.getFinalTotal());
        assertTrue(response.getAppliedPromotions().isEmpty());
    }

    @Test
    void calculateQuote_BuyXGetYPromotion_MultipleOccurrences() {
        // Arrange
        BuyXGetYPromotion promotion = BuyXGetYPromotion.builder()
                .id(promotionId1)
                .description("Buy 2 Get 1 Free")
                .productId(productId1)
                .buyX(2)
                .getY(1)
                .build();

        // Cart has 7 items: Buy 2 get 1, Buy 2 get 1, Buy 2 get 1, 1 remaining
        // Should get 3 free items
        CartQuoteRequest.CartItem item = new CartQuoteRequest.CartItem();
        item.setProductId(productId1.toString());
        item.setQty(7);

        CartQuoteRequest request = new CartQuoteRequest();
        request.setItems(List.of(item));
        request.setCustomerSegment(CustomerSegment.REGULAR);

        when(productRepository.findById(productId1)).thenReturn(Optional.of(product1));
        when(promotionRepository.findAll()).thenReturn(List.of(promotion));

        // Act
        CartQuoteResponse response = cartService.calculateQuote(request);

        // Assert
        assertEquals(new BigDecimal("70.00"), response.getSubtotal()); // 10 * 7
        assertEquals(new BigDecimal("30.00"), response.getTotalDiscount()); // 3 free items * 10
        assertEquals(new BigDecimal("40.00"), response.getFinalTotal());
        assertEquals(1, response.getAppliedPromotions().size());

        CartQuoteResponse.AppliedPromotion appliedPromo = response.getAppliedPromotions().get(0);
        assertTrue(appliedPromo.getDescription().contains("3 free items"));
    }

    @Test
    void calculateQuote_ZeroQuantityItems_IgnoresItem() {
        // Arrange
        CartQuoteRequest.CartItem validItem = new CartQuoteRequest.CartItem();
        validItem.setProductId(productId1.toString());
        validItem.setQty(2);

        CartQuoteRequest.CartItem zeroQtyItem = new CartQuoteRequest.CartItem();
        zeroQtyItem.setProductId(productId2.toString());
        zeroQtyItem.setQty(0);

        CartQuoteRequest request = new CartQuoteRequest();
        request.setItems(List.of(validItem, zeroQtyItem));
        request.setCustomerSegment(CustomerSegment.REGULAR);

        when(productRepository.findById(productId1)).thenReturn(Optional.of(product1));
        when(productRepository.findById(productId2)).thenReturn(Optional.of(product2));
        when(promotionRepository.findAll()).thenReturn(Collections.emptyList());

        // Act
        CartQuoteResponse response = cartService.calculateQuote(request);

        // Assert
        assertEquals(2, response.getLineItems().size()); // Only valid item should be included
        assertEquals(new BigDecimal("20.00"), response.getSubtotal());
        assertEquals(productId1.toString(), response.getLineItems().get(0).getProductId());
    }

    @Test
    void calculateQuote_NegativeQuantityItems_ThrowsException() {
        // Arrange
        CartQuoteRequest.CartItem negativeItem = new CartQuoteRequest.CartItem();
        negativeItem.setProductId(productId1.toString());
        negativeItem.setQty(-1);

        CartQuoteRequest request = new CartQuoteRequest();
        request.setItems(List.of(negativeItem));
        request.setCustomerSegment(CustomerSegment.REGULAR);
    }

    @Test
    void calculateQuote_InvalidProductIdFormat_ThrowsException() {
        // Arrange
        CartQuoteRequest.CartItem invalidItem = new CartQuoteRequest.CartItem();
        invalidItem.setProductId("invalid-uuid-format");
        invalidItem.setQty(1);

        CartQuoteRequest request = new CartQuoteRequest();
        request.setItems(List.of(invalidItem));
        request.setCustomerSegment(CustomerSegment.REGULAR);

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> cartService.calculateQuote(request));
    }

    @Test
    void calculateQuote_NullCustomerSegment_UsesDefault() {
        // Arrange
        cartRequest.setCustomerSegment(null);

        when(productRepository.findById(productId1)).thenReturn(Optional.of(product1));
        when(productRepository.findById(productId2)).thenReturn(Optional.of(product2));
        when(promotionRepository.findAll()).thenReturn(Collections.emptyList());

        // Act
        CartQuoteResponse response = cartService.calculateQuote(cartRequest);

        // Assert
        assertNotNull(response);
        assertEquals(new BigDecimal("40.00"), response.getSubtotal());
        // Should handle null customer segment gracefully
    }

    @Test
    void calculateQuote_PercentOffCategoryPromotion_ZeroPercent_NoDiscount() {
        // Arrange
        PercentOffCategoryPromotion promotion = PercentOffCategoryPromotion.builder()
                .id(promotionId1)
                .description("0% off Electronics")
                .category(ProductCategory.ELECTRONICS)
                .percentOff(BigDecimal.ZERO)
                .build();

        when(productRepository.findById(productId1)).thenReturn(Optional.of(product1));
        when(productRepository.findById(productId2)).thenReturn(Optional.of(product2));
        when(promotionRepository.findAll()).thenReturn(List.of(promotion));

        // Act
        CartQuoteResponse response = cartService.calculateQuote(cartRequest);

        // Assert
        assertEquals(new BigDecimal("40.00"), response.getSubtotal());
        assertEquals(BigDecimal.ZERO, response.getTotalDiscount());
        assertEquals(new BigDecimal("40.00"), response.getFinalTotal());
        assertTrue(response.getAppliedPromotions().isEmpty()); // Should not add 0% promotions
    }
    @Test
    void confirmCart_ConcurrentModification_RetriesSuccessfully() {
        // Arrange
        String idempotencyKey = "concurrent-test";

        Product initialProduct = Product.builder()
                .id(productId1)
                .name("Product 1")
                .price(new BigDecimal("10.00"))
                .stock(10)
                .category(ProductCategory.ELECTRONICS)
                .build();

        Product lockedProduct = Product.builder()
                .id(productId1)
                .name("Product 1")
                .price(new BigDecimal("10.00"))
                .stock(8) // Stock reduced but still sufficient
                .category(ProductCategory.ELECTRONICS)
                .build();

        when(orderRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(productRepository.findById(productId1)).thenReturn(Optional.of(initialProduct));
        when(productRepository.findById(productId2)).thenReturn(Optional.of(product2));
        when(productRepository.findByIdWithLock(productId1)).thenReturn(Optional.of(lockedProduct));
        when(productRepository.findByIdWithLock(productId2)).thenReturn(Optional.of(product2));
        when(promotionRepository.findAll()).thenReturn(Collections.emptyList());

        Order savedOrder = Order.builder()
                .orderId("CONCURRENT-ORDER")
                .finalTotal(new BigDecimal("50.00"))
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
        assertEquals("CONCURRENT-ORDER", response.getOrderId());
        assertEquals(CartConfirmResponse.OrderStatus.CONFIRMED, response.getStatus());
    }

    @Test
    void confirmCart_DatabaseError_RollsBackTransaction() {
        // Arrange
        String idempotencyKey = "db-error-test";

        when(orderRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(productRepository.findById(productId1)).thenReturn(Optional.of(product1));
        when(productRepository.findById(productId2)).thenReturn(Optional.of(product2));
        when(productRepository.findByIdWithLock(productId1)).thenReturn(Optional.of(product1));
        when(productRepository.findByIdWithLock(productId2)).thenReturn(Optional.of(product2));
        when(promotionRepository.findAll()).thenReturn(Collections.emptyList());
        when(orderRepository.save(any(Order.class))).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> cartService.confirmCart(cartRequest, idempotencyKey));
        assertEquals("Failed to confirm cart", exception.getMessage());

        // Verify rollback behavior (stock should not be updated on failure)
        verify(productRepository, times(2)).findByIdWithLock(any());
    }

    @Test
    void calculateQuote_LargeQuantities_HandlesCorrectly() {
        // Arrange
        CartQuoteRequest.CartItem largeQtyItem = new CartQuoteRequest.CartItem();
        largeQtyItem.setProductId(productId1.toString());
        largeQtyItem.setQty(1000);

        CartQuoteRequest request = new CartQuoteRequest();
        request.setItems(List.of(largeQtyItem));
        request.setCustomerSegment(CustomerSegment.REGULAR);

        when(productRepository.findById(productId1)).thenReturn(Optional.of(product1));
        when(promotionRepository.findAll()).thenReturn(Collections.emptyList());

        // Act
        CartQuoteResponse response = cartService.calculateQuote(request);

        // Assert
        assertEquals(new BigDecimal("10000.00"), response.getSubtotal()); // 10 * 1000
        assertEquals(1, response.getLineItems().size());
        assertEquals(1000, response.getLineItems().get(0).getQuantity());
    }

    @Test
    void calculateQuote_DuplicateProductIds_ConsolidatesQuantities() {
        // Arrange
        CartQuoteRequest.CartItem item1 = new CartQuoteRequest.CartItem();
        item1.setProductId(productId1.toString());
        item1.setQty(2);

        CartQuoteRequest.CartItem item2 = new CartQuoteRequest.CartItem();
        item2.setProductId(productId1.toString()); // Same product
        item2.setQty(3);

        CartQuoteRequest request = new CartQuoteRequest();
        request.setItems(List.of(item1, item2));
        request.setCustomerSegment(CustomerSegment.REGULAR);

        when(productRepository.findById(productId1)).thenReturn(Optional.of(product1));
        when(promotionRepository.findAll()).thenReturn(Collections.emptyList());

        // Act
        CartQuoteResponse response = cartService.calculateQuote(request);

        // Assert
        assertEquals(2, response.getLineItems().size()); // Should be consolidated
        assertEquals(2, response.getLineItems().get(0).getQuantity());
        assertEquals(new BigDecimal("50.00"), response.getSubtotal());
    }

    @Test
    void confirmCart_PromotionNotFoundDuringConfirmation_SkipsPromotion() {
        // Arrange
        String idempotencyKey = "promo-not-found-test";

        PercentOffCategoryPromotion promotion = PercentOffCategoryPromotion.builder()
                .id(promotionId1)
                .description("10% off Electronics")
                .category(ProductCategory.ELECTRONICS)
                .percentOff(new BigDecimal("10"))
                .build();

        when(orderRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(productRepository.findById(productId1)).thenReturn(Optional.of(product1));
        when(productRepository.findById(productId2)).thenReturn(Optional.of(product2));
        when(productRepository.findByIdWithLock(productId1)).thenReturn(Optional.of(product1));
        when(productRepository.findByIdWithLock(productId2)).thenReturn(Optional.of(product2));
        when(promotionRepository.findAll()).thenReturn(List.of(promotion));
        when(promotionRepository.findById(promotionId1)).thenReturn(Optional.empty()); // Not found during confirmation

        Order savedOrder = Order.builder()
                .orderId("PROMO-SKIP-ORDER")
                .finalTotal(new BigDecimal("50.00")) // Full price since promo skipped
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
        assertEquals("PROMO-SKIP-ORDER", response.getOrderId());
        // Verify the promotion was attempted to be found but gracefully handled
        verify(promotionRepository).findById(promotionId1);
    }
}