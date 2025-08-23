// CartConfirmResponse.java
package com.promo.quoter.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartConfirmResponse {
    private String orderId;
    private BigDecimal finalTotal;
    private OrderStatus status;
    private List<ReservedItem> reservedItems;
    private List<AppliedPromotion> appliedPromotions;
    private LocalDateTime createdAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReservedItem {
        private String productId;
        private String productName;
        private Integer quantity;
        private BigDecimal unitPrice;
        private LocalDateTime reservedAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AppliedPromotion {
        private String promotionId;
        private String promotionType;
        private String description;
        private BigDecimal discountAmount;
    }

    public enum OrderStatus {
        CONFIRMED,
        PENDING,
        FAILED
    }
}


