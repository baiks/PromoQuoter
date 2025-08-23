package com.promo.quoter.entities;

import com.promo.quoter.enums.PromotionType;
import com.promo.quoter.enums.ProductCategory;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "promo_type")
@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class Promotion {
    @Id
    @GeneratedValue
    private UUID id;
    @Enumerated(EnumType.STRING)
    private PromotionType promotionType;
    @NotBlank
    private String description;
}
