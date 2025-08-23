package com.promo.quoter.repos;

import com.promo.quoter.entities.PercentOffCategoryPromotion;
import com.promo.quoter.enums.ProductCategory;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PercentOffCategoryPromotionRepository extends JpaRepository<PercentOffCategoryPromotion, UUID> {
    boolean existsByCategory(@NotNull(message = "Promotion type is required") ProductCategory category);
}
