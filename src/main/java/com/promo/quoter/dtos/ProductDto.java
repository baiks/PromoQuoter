package com.promo.quoter.dtos;

import com.promo.quoter.enums.ProductCategory;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Builder
public class ProductDto {
    @Getter
    @Setter
    @Builder
    public static class CreateProductDto {
        @NotBlank(message = "Product name must not be blank")
        @Size(max = 100, message = "Product name must be at most 100 characters")
        private String name;
        @NotNull(message = "Product category must not be null")
        private ProductCategory category;
        @NotNull(message = "Price must not be null")
        @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
        @Digits(integer = 10, fraction = 2, message = "Price must be a valid decimal (max 2 decimal places)")
        private BigDecimal price;
        @Min(value = 0, message = "Stock cannot be negative")
        private int stock;
    }

    @Getter
    @Setter
    @Builder
    public static class ResponseDto {
        private String status;
        private String description;
    }
}
