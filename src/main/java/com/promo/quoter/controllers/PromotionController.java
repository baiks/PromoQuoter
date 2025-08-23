package com.promo.quoter.controllers;

import com.promo.quoter.dtos.ProductDto;
import com.promo.quoter.dtos.PromotionDto;
import com.promo.quoter.services.ProductService;
import com.promo.quoter.services.PromotionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "promotions")
@RequiredArgsConstructor
public class PromotionController {
    private final PromotionService promotionService;

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody PromotionDto.CreatePromotionDto createPromotionDto) {
        return promotionService.create(createPromotionDto);
    }
}
