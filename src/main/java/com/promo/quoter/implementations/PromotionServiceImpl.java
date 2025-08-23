package com.promo.quoter.implementations;

import com.promo.quoter.dtos.ProductDto;
import com.promo.quoter.dtos.PromotionDto;
import com.promo.quoter.entities.BuyXGetYPromotion;
import com.promo.quoter.entities.PercentOffCategoryPromotion;
import com.promo.quoter.entities.Product;
import com.promo.quoter.entities.Promotion;
import com.promo.quoter.repos.BuyXGetYPromotionRepository;
import com.promo.quoter.repos.PercentOffCategoryPromotionRepository;
import com.promo.quoter.repos.ProductRepository;
import com.promo.quoter.repos.PromotionRepository;
import com.promo.quoter.services.PromotionService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import static com.promo.quoter.enums.PromotionType.PERCENT_OFF_CATEGORY;

@Service
@RequiredArgsConstructor
public class PromotionServiceImpl implements PromotionService {
    private final PromotionRepository promotionRepository;
    private final PercentOffCategoryPromotionRepository percentOffCategoryPromotionRepository;
    private final BuyXGetYPromotionRepository buyXGetYPromotionRepository;
    private final ProductRepository productRepository;
    private final ModelMapper modelMapper;

    @Override
    public ResponseEntity<?> create(PromotionDto.CreatePromotionDto createPromotionDto) {
        Promotion promotion = null;

        switch (createPromotionDto.getPromotionType()) {
            case PERCENT_OFF_CATEGORY:
                if (percentOffCategoryPromotionRepository.existsByCategory(createPromotionDto.getCategory())) {
                    return ResponseEntity.status(HttpStatus.CONFLICT).body(PromotionDto.ResponseDto.builder().
                            status(String.valueOf(HttpStatus.CONFLICT)).
                            description("Promotion category already exists").build());
                }
                promotion = modelMapper.map(createPromotionDto, PercentOffCategoryPromotion.class);
                break;
            case BUY_X_GET_Y:
                if (!productRepository.existsById(createPromotionDto.getProductId())) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                            PromotionDto.ResponseDto.builder()
                                    .status(String.valueOf(HttpStatus.NOT_FOUND.value()))
                                    .description("Product not found")
                                    .build()
                    );
                }

                if (buyXGetYPromotionRepository.existsByProductId(createPromotionDto.getProductId())) {
                    return ResponseEntity.status(HttpStatus.CONFLICT).body(
                            PromotionDto.ResponseDto.builder()
                                    .status(String.valueOf(HttpStatus.CONFLICT.value()))
                                    .description("Promotion for product already exists")
                                    .build()
                    );
                }
                promotion = modelMapper.map(createPromotionDto, BuyXGetYPromotion.class);
                break;
            default:
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                        PromotionDto.ResponseDto.builder()
                                .status(String.valueOf(HttpStatus.BAD_REQUEST.value()))
                                .description("Invalid promotion type")
                                .build()
                );
        }
        promotionRepository.save(promotion);
        return ResponseEntity.status(HttpStatus.CREATED).body(promotion);
    }
}
