package com.promo.quoter.services;

import com.promo.quoter.dtos.ProductDto;
import com.promo.quoter.dtos.PromotionDto;
import jakarta.transaction.Transactional;
import org.springframework.http.ResponseEntity;

public interface PromotionService {
    @Transactional
    ResponseEntity<?> create(PromotionDto.CreatePromotionDto createPromotionDto);
}
