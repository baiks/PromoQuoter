package com.promo.quoter.entities;

import com.promo.quoter.enums.ProductCategory;
import jakarta.persistence.Entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {
    @Id
    @GeneratedValue
    private UUID id;
    private String name;
    @Enumerated(EnumType.STRING)
    private ProductCategory category;
    private BigDecimal price;
    private int stock;
}
