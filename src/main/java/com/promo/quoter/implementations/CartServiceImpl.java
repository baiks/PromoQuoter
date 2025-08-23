package com.promo.quoter.implementations;

import com.promo.quoter.dtos.CartConfirmResponse;
import com.promo.quoter.dtos.CartQuoteRequest;
import com.promo.quoter.dtos.CartQuoteResponse;
import com.promo.quoter.entities.*;
import com.promo.quoter.exception.CustomException;
import com.promo.quoter.exception.InsufficientStockException;
import com.promo.quoter.repos.OrderRepository;
import com.promo.quoter.repos.ProductRepository;
import com.promo.quoter.repos.PromotionRepository;
import com.promo.quoter.services.CartService;
import jakarta.persistence.LockModeType;
// CHANGE: Use Spring's @Transactional instead of Jakarta's
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartServiceImpl implements CartService {

    private final ProductRepository productRepository;
    private final PromotionRepository promotionRepository;
    private final OrderRepository orderRepository;

    @Override
    @Transactional(readOnly = true) // Quotes should be read-only
    public CartQuoteResponse calculateQuote(CartQuoteRequest request) {
        //Validate and fetch products
        List<CartQuoteResponse.LineItem> lineItems = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (CartQuoteRequest.CartItem item : request.getItems()) {
            UUID productId = UUID.fromString(item.getProductId());
            // CHANGE: Use regular findById for quotes, not locking
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("Product not found: " + productId));

            BigDecimal lineTotal = product.getPrice().multiply(BigDecimal.valueOf(item.getQty()));
            subtotal = subtotal.add(lineTotal);

            CartQuoteResponse.LineItem lineItem = CartQuoteResponse.LineItem.builder()
                    .productId(item.getProductId())
                    .productName(product.getName())
                    .quantity(item.getQty())
                    .unitPrice(product.getPrice())
                    .lineTotal(lineTotal)
                    .discountAmount(BigDecimal.ZERO)
                    .finalLineTotal(lineTotal)
                    .build();

            lineItems.add(lineItem);
        }

        // Apply promotions
        List<CartQuoteResponse.AppliedPromotion> appliedPromotions = applyPromotions(lineItems, request);

        //Calculate totals
        BigDecimal totalDiscount = appliedPromotions.stream()
                .map(CartQuoteResponse.AppliedPromotion::getDiscountAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal finalTotal = subtotal.subtract(totalDiscount);

        return CartQuoteResponse.builder()
                .lineItems(lineItems)
                .appliedPromotions(appliedPromotions)
                .subtotal(subtotal)
                .totalDiscount(totalDiscount)
                .finalTotal(finalTotal)
                .build();
    }

    private List<CartQuoteResponse.AppliedPromotion> applyPromotions(
            List<CartQuoteResponse.LineItem> lineItems,
            CartQuoteRequest request) {

        List<CartQuoteResponse.AppliedPromotion> appliedPromotions = new ArrayList<>();

        //Get all active promotions
        List<Promotion> activePromotions = promotionRepository.findAll();

        // Create a map of productId to Product for quick lookup
        Map<UUID, Product> productMap = new HashMap<>();
        for (CartQuoteResponse.LineItem lineItem : lineItems) {
            UUID productId = UUID.fromString(lineItem.getProductId());
            Product product = productRepository.findById(productId).orElse(null);
            if (product != null) {
                productMap.put(productId, product);
            }
        }

        //Apply PercentOffCategoryPromotion first
        for (Promotion promotion : activePromotions) {
            if (promotion instanceof PercentOffCategoryPromotion percentPromo) {
                CartQuoteResponse.AppliedPromotion appliedPromo = applyPercentOffCategoryPromotion(
                        percentPromo, lineItems, productMap);
                if (appliedPromo != null) {
                    appliedPromotions.add(appliedPromo);
                }
            }
        }

        //Apply BuyXGetYPromotion after percentage discounts
        for (Promotion promotion : activePromotions) {
            if (promotion instanceof BuyXGetYPromotion buyXGetYPromo) {
                CartQuoteResponse.AppliedPromotion appliedPromo = applyBuyXGetYPromotion(
                        buyXGetYPromo, lineItems, productMap);
                if (appliedPromo != null) {
                    appliedPromotions.add(appliedPromo);
                }
            }
        }

        return appliedPromotions;
    }

    private CartQuoteResponse.AppliedPromotion applyPercentOffCategoryPromotion(
            PercentOffCategoryPromotion promotion,
            List<CartQuoteResponse.LineItem> lineItems,
            Map<UUID, Product> productMap) {

        BigDecimal totalDiscount = BigDecimal.ZERO;
        List<String> affectedProductIds = new ArrayList<>();

        for (CartQuoteResponse.LineItem lineItem : lineItems) {
            UUID productId = UUID.fromString(lineItem.getProductId());
            Product product = productMap.get(productId);

            if (product != null && product.getCategory() == promotion.getCategory()) {
                // Calculate discount on current line total (after any previous discounts)
                BigDecimal discountAmount = lineItem.getFinalLineTotal()
                        .multiply(promotion.getPercentOff())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

                // Update line item
                lineItem.setDiscountAmount(lineItem.getDiscountAmount().add(discountAmount));
                lineItem.setFinalLineTotal(lineItem.getFinalLineTotal().subtract(discountAmount));

                totalDiscount = totalDiscount.add(discountAmount);
                affectedProductIds.add(lineItem.getProductId());
            }
        }

        if (totalDiscount.compareTo(BigDecimal.ZERO) > 0) {
            return CartQuoteResponse.AppliedPromotion.builder()
                    .promotionId(promotion.getId().toString())
                    .promotionType("PERCENT_OFF_CATEGORY")
                    .description(promotion.getDescription())
                    .discountAmount(totalDiscount)
                    .affectedProductIds(affectedProductIds)
                    .build();
        }

        return null;
    }

    private CartQuoteResponse.AppliedPromotion applyBuyXGetYPromotion(
            BuyXGetYPromotion promotion,
            List<CartQuoteResponse.LineItem> lineItems,
            Map<UUID, Product> productMap) {

        // Find the line item for the promotion product
        CartQuoteResponse.LineItem targetLineItem = null;
        for (CartQuoteResponse.LineItem lineItem : lineItems) {
            if (promotion.getProductId().toString().equals(lineItem.getProductId())) {
                targetLineItem = lineItem;
                break;
            }
        }

        if (targetLineItem == null) {
            return null; // Product not in cart
        }

        // Calculate how many free items customer gets
        int qualifyingSets = targetLineItem.getQuantity() / promotion.getBuyX();
        int freeItems = qualifyingSets * promotion.getGetY();

        if (freeItems <= 0) {
            return null; // Not enough quantity to qualify
        }

        // Calculate discount (price of free items)
        Product product = productMap.get(promotion.getProductId());
        if (product == null) {
            return null;
        }

        BigDecimal discountAmount = product.getPrice().multiply(BigDecimal.valueOf(freeItems));

        // Update line item
        targetLineItem.setDiscountAmount(targetLineItem.getDiscountAmount().add(discountAmount));
        targetLineItem.setFinalLineTotal(targetLineItem.getFinalLineTotal().subtract(discountAmount));

        return CartQuoteResponse.AppliedPromotion.builder()
                .promotionId(promotion.getId().toString())
                .promotionType("BUY_X_GET_Y")
                .description(String.format("%s (Buy %d Get %d Free - %d free items)",
                        promotion.getDescription(), promotion.getBuyX(), promotion.getGetY(), freeItems))
                .discountAmount(discountAmount)
                .affectedProductIds(List.of(targetLineItem.getProductId()))
                .build();
    }

    // CHANGE: Use Spring's @Transactional with explicit readOnly = false
    @Override
    @Transactional(readOnly = false, timeout = 30)
    public CartConfirmResponse confirmCart(CartQuoteRequest request, String idempotencyKey) {
        try {
            // 1. Check for duplicate request using idempotency key
            if (idempotencyKey != null) {
                Optional<Order> existingOrder = orderRepository.findByIdempotencyKey(idempotencyKey);
                if (existingOrder.isPresent()) {
                    log.info("Duplicate request detected for idempotency key: {}", idempotencyKey);
                    return mapOrderToConfirmResponse(existingOrder.get());
                }
            }

            // 2. Validate stock availability first (read-only check)
            List<Product> products = validateStockAvailability(request);

            // 3. Calculate quote to get pricing (this will use a separate read-only transaction)
            CartQuoteResponse quote = calculateQuoteForConfirm(request);

            // 4. Reserve stock atomically (with database locking)
            List<OrderItem> reservedItems = reserveStock(request, products, quote.getLineItems());

            // 5. Generate unique order ID
            String orderId = generateOrderId();

            // 6. Create and save order
            Order order = createOrder(orderId, idempotencyKey, request, quote, reservedItems);
            order = orderRepository.save(order);

            log.info("Order created successfully: orderId={}, finalTotal={}",
                    orderId, order.getFinalTotal());

            return mapOrderToConfirmResponse(order);

        } catch (InsufficientStockException e) {
            log.error("Insufficient stock for cart confirmation: {}", e.getMessage());
            throw new CustomException("Insufficient stock: " + e.getMessage(), HttpStatus.CONFLICT);
        } catch (Exception e) {
            log.error("Error confirming cart: {}", e.getMessage(), e);
            throw new CustomException("Failed to confirm cart", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // CHANGE: Separate method for quote calculation during confirm (without separate transaction)
    private CartQuoteResponse calculateQuoteForConfirm(CartQuoteRequest request) {
        //Validate and fetch products
        List<CartQuoteResponse.LineItem> lineItems = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (CartQuoteRequest.CartItem item : request.getItems()) {
            UUID productId = UUID.fromString(item.getProductId());
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("Product not found: " + productId));

            BigDecimal lineTotal = product.getPrice().multiply(BigDecimal.valueOf(item.getQty()));
            subtotal = subtotal.add(lineTotal);

            CartQuoteResponse.LineItem lineItem = CartQuoteResponse.LineItem.builder()
                    .productId(item.getProductId())
                    .productName(product.getName())
                    .quantity(item.getQty())
                    .unitPrice(product.getPrice())
                    .lineTotal(lineTotal)
                    .discountAmount(BigDecimal.ZERO)
                    .finalLineTotal(lineTotal)
                    .build();

            lineItems.add(lineItem);
        }

        // Apply promotions
        List<CartQuoteResponse.AppliedPromotion> appliedPromotions = applyPromotions(lineItems, request);

        //Calculate totals
        BigDecimal totalDiscount = appliedPromotions.stream()
                .map(CartQuoteResponse.AppliedPromotion::getDiscountAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal finalTotal = subtotal.subtract(totalDiscount);

        return CartQuoteResponse.builder()
                .lineItems(lineItems)
                .appliedPromotions(appliedPromotions)
                .subtotal(subtotal)
                .totalDiscount(totalDiscount)
                .finalTotal(finalTotal)
                .build();
    }

    private List<Product> validateStockAvailability(CartQuoteRequest request) {
        List<Product> products = new ArrayList<>();
        List<String> outOfStockItems = new ArrayList<>();

        for (CartQuoteRequest.CartItem item : request.getItems()) {
            UUID productId = UUID.fromString(item.getProductId());
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new CustomException("Product not found: " + item.getProductId(),
                            HttpStatus.NOT_FOUND));

            if (product.getStock() < item.getQty()) {
                outOfStockItems.add(String.format("%s (requested: %d, available: %d)",
                        product.getName(), item.getQty(), product.getStock()));
            }

            products.add(product);
        }

        if (!outOfStockItems.isEmpty()) {
            throw new InsufficientStockException("Insufficient stock for items: " +
                    String.join(", ", outOfStockItems));
        }
        return products;
    }

    private List<OrderItem> reserveStock(CartQuoteRequest request,
                                         List<Product> products,
                                         List<CartQuoteResponse.LineItem> lineItems) {
        List<OrderItem> orderItems = new ArrayList<>();
        Map<String, CartQuoteResponse.LineItem> lineItemMap = lineItems.stream()
                .collect(Collectors.toMap(
                        CartQuoteResponse.LineItem::getProductId,
                        item -> item
                ));

        for (int i = 0; i < request.getItems().size(); i++) {
            CartQuoteRequest.CartItem requestItem = request.getItems().get(i);
            Product product = products.get(i);
            CartQuoteResponse.LineItem lineItem = lineItemMap.get(requestItem.getProductId());

            // Use pessimistic locking to prevent concurrent stock updates
            Product lockedProduct = productRepository.findByIdWithLock(product.getId())
                    .orElseThrow(() -> new CustomException(
                            "Product not found during reservation: " + product.getId(),
                            HttpStatus.NOT_FOUND));

            // Double-check stock after locking
            if (lockedProduct.getStock() < requestItem.getQty()) {
                throw new InsufficientStockException(
                        String.format("Insufficient stock for %s during reservation",
                                lockedProduct.getName()));
            }

            // Reserve stock (decrement)
            lockedProduct.setStock(lockedProduct.getStock() - requestItem.getQty());
            productRepository.save(lockedProduct);

            // Create order item
            OrderItem orderItem = OrderItem.builder()
                    .product(lockedProduct)
                    .quantity(requestItem.getQty())
                    .unitPrice(lineItem.getUnitPrice())
                    .lineTotal(lineItem.getLineTotal())
                    .discountAmount(lineItem.getDiscountAmount())
                    .finalLineTotal(lineItem.getFinalLineTotal())
                    .build();

            orderItems.add(orderItem);

            log.info("Reserved {} units of product {}, remaining stock: {}",
                    requestItem.getQty(), lockedProduct.getName(), lockedProduct.getStock());
        }

        return orderItems;
    }

    private String generateOrderId() {
        return "ORD-" + LocalDate.now().getYear() + "-" +
                String.format("%06d", System.nanoTime() % 1000000);
    }

    private Order createOrder(String orderId,
                              String idempotencyKey,
                              CartQuoteRequest request,
                              CartQuoteResponse quote,
                              List<OrderItem> orderItems) {
        Order order = Order.builder()
                .orderId(orderId)
                .idempotencyKey(idempotencyKey)
                .customerSegment(request.getCustomerSegment())
                .subtotal(quote.getSubtotal())
                .totalDiscount(quote.getTotalDiscount())
                .finalTotal(quote.getFinalTotal())
                .status(Order.OrderStatus.CONFIRMED)
                .build();

        // Set order reference in order items
        orderItems.forEach(item -> item.setOrder(order));
        order.setOrderItems(orderItems);

        // Create order promotions
        List<OrderPromotion> orderPromotions = quote.getAppliedPromotions().stream()
                .map(promo -> {
                    Promotion promotion = promotionRepository.findById(UUID.fromString(promo.getPromotionId()))
                            .orElse(null);

                    return OrderPromotion.builder()
                            .order(order)
                            .promotion(promotion)
                            .promotionType(promo.getPromotionType())
                            .description(promo.getDescription())
                            .discountAmount(promo.getDiscountAmount())
                            .build();
                })
                .collect(Collectors.toList());

        order.setAppliedPromotions(orderPromotions);

        return order;
    }

    private CartConfirmResponse mapOrderToConfirmResponse(Order order) {
        List<CartConfirmResponse.ReservedItem> reservedItems = order.getOrderItems().stream()
                .map(item -> CartConfirmResponse.ReservedItem.builder()
                        .productId(item.getProduct().getId().toString())
                        .productName(item.getProduct().getName())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .reservedAt(item.getReservedAt())
                        .build())
                .collect(Collectors.toList());

        List<CartConfirmResponse.AppliedPromotion> appliedPromotions = order.getAppliedPromotions().stream()
                .map(promo -> CartConfirmResponse.AppliedPromotion.builder()
                        .promotionId(promo.getPromotion() != null ?
                                promo.getPromotion().getId().toString() : null)
                        .promotionType(promo.getPromotionType())
                        .description(promo.getDescription())
                        .discountAmount(promo.getDiscountAmount())
                        .build())
                .collect(Collectors.toList());

        return CartConfirmResponse.builder()
                .orderId(order.getOrderId())
                .finalTotal(order.getFinalTotal())
                .status(CartConfirmResponse.OrderStatus.valueOf(order.getStatus().name()))
                .reservedItems(reservedItems)
                .appliedPromotions(appliedPromotions)
                .createdAt(order.getCreatedAt())
                .build();
    }
}