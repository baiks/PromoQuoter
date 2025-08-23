package com.promo.quoter.entities;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@DiscriminatorValue("BUY_X_GET_Y")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BuyXGetYPromotion extends Promotion {
    @NotNull
    private UUID productId;
    @NotNull
    private Integer buyX;
    @NotNull
    private Integer getY;
}
