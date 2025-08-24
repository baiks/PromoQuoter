package com.promo.quoter.dtos;

import com.promo.quoter.enums.CustomerSegment;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartQuoteRequest {
    @NotEmpty(message = "Items cannot be empty")
    @Valid
    private List<CartItem> items;
    
    @NotNull(message = "Customer segment is required")
    private CustomerSegment customerSegment;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CartItem {
        @NotNull(message = "Product ID is required")
        private String productId;
        
        @NotNull(message = "Quantity is required")
        private Integer qty;
    }
}