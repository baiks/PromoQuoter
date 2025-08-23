package com.promo.quoter.repos;

import com.promo.quoter.entities.BuyXGetYPromotion;
import com.promo.quoter.entities.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BuyXGetYPromotionRepository extends JpaRepository<BuyXGetYPromotion, UUID> {
boolean existsByProductId(UUID productId);
}
