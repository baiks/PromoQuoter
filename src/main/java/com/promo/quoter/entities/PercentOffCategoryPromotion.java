package com.promo.quoter.entities;

import com.promo.quoter.enums.ProductCategory;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@DiscriminatorValue("PERCENT_OFF_CATEGORY")
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class PercentOffCategoryPromotion extends Promotion {
    @NotNull
    private BigDecimal percentOff;
    @NotNull
    @Enumerated(EnumType.STRING)
    private ProductCategory category;
}

