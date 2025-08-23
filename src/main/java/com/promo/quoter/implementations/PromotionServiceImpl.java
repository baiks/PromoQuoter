package com.promo.quoter.implementations;

import com.promo.quoter.dtos.ProductDto;
import com.promo.quoter.dtos.PromotionDto;
import com.promo.quoter.entities.BuyXGetYPromotion;
import com.promo.quoter.entities.PercentOffCategoryPromotion;
import com.promo.quoter.entities.Product;
import com.promo.quoter.entities.Promotion;
import com.promo.quoter.repos.PromotionRepository;
import com.promo.quoter.services.PromotionService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PromotionServiceImpl implements PromotionService {
    private final PromotionRepository promotionRepository;
    private final ModelMapper modelMapper;

    @Override
    public ResponseEntity<?> create(PromotionDto.CreatePromotionDto createPromotionDto) {
        Promotion promotion = switch (createPromotionDto.getPromotionType()) {
            case PERCENT_OFF_CATEGORY -> modelMapper.map(createPromotionDto, PercentOffCategoryPromotion.class);
            case BUY_X_GET_Y -> modelMapper.map(createPromotionDto, BuyXGetYPromotion.class);
            default -> throw new IllegalArgumentException("Unsupported promotion type");
        };
        promotionRepository.save(promotion);
        return ResponseEntity.status(HttpStatus.CREATED).body(promotion);
    }
}
