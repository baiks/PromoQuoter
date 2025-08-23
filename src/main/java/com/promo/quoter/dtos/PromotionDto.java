package com.promo.quoter.dtos;

import com.promo.quoter.enums.ProductCategory;
import com.promo.quoter.enums.PromotionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Builder
public class PromotionDto {
    @Getter
    @Setter
    @Builder
    public static class CreatePromotionDto {
        @NotNull(message = "Promotion type is required")
        private PromotionType promotionType;
        @NotBlank(message = "Description is required")
        private String description;
        private BigDecimal percentOff;
        private ProductCategory category;
        private UUID productId;
        private Integer buyX;
        private Integer getY;
    }
}
