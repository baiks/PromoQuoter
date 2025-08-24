package com.promo.quoter;

import com.promo.quoter.dtos.PromotionDto;
import com.promo.quoter.entities.BuyXGetYPromotion;
import com.promo.quoter.entities.PercentOffCategoryPromotion;
import com.promo.quoter.enums.ProductCategory;
import com.promo.quoter.enums.PromotionType;
import com.promo.quoter.implementations.PromotionServiceImpl;
import com.promo.quoter.repos.BuyXGetYPromotionRepository;
import com.promo.quoter.repos.PercentOffCategoryPromotionRepository;
import com.promo.quoter.repos.ProductRepository;
import com.promo.quoter.repos.PromotionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PromotionServiceImpl Tests")
class PromotionServiceImplTest {

    @Mock
    private PromotionRepository promotionRepository;

    @Mock
    private PercentOffCategoryPromotionRepository percentOffCategoryPromotionRepository;

    @Mock
    private BuyXGetYPromotionRepository buyXGetYPromotionRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private PromotionServiceImpl promotionService;

    private PromotionDto.CreatePromotionDto percentOffDto;
    private PromotionDto.CreatePromotionDto buyXGetYDto;
    private UUID productId;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();

        // Setup PercentOffCategory DTO
        percentOffDto = PromotionDto.CreatePromotionDto.builder()
                .promotionType(PromotionType.PERCENT_OFF_CATEGORY)
                .category(ProductCategory.ELECTRONICS)
                .percentOff(new BigDecimal("20"))
                .description("20% off Electronics")
                .build();

        // Setup BuyXGetY DTO
        buyXGetYDto = PromotionDto.CreatePromotionDto.builder()
                .promotionType(PromotionType.BUY_X_GET_Y)
                .productId(productId)
                .buyX(2)
                .getY(1)
                .description("Buy 2 Get 1 Free")
                .build();
    }

    @Test
    @DisplayName("Should create PercentOffCategory promotion successfully")
    void create_ShouldCreatePercentOffCategoryPromotion_WhenValidInput() {
        // Given
        PercentOffCategoryPromotion mappedPromotion = PercentOffCategoryPromotion.builder()
                .id(UUID.randomUUID())
                .description("20% off Electronics")
                .category(ProductCategory.ELECTRONICS)
                .percentOff(new BigDecimal("20"))
                .build();

        when(percentOffCategoryPromotionRepository.existsByCategory(ProductCategory.ELECTRONICS)).thenReturn(false);
        when(modelMapper.map(percentOffDto, PercentOffCategoryPromotion.class)).thenReturn(mappedPromotion);
        when(promotionRepository.save(any(PercentOffCategoryPromotion.class))).thenReturn(mappedPromotion);

        // When
        ResponseEntity<?> response = promotionService.create(percentOffDto);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(mappedPromotion);

        verify(percentOffCategoryPromotionRepository, times(1)).existsByCategory(ProductCategory.ELECTRONICS);
        verify(modelMapper, times(1)).map(percentOffDto, PercentOffCategoryPromotion.class);
        verify(promotionRepository, times(1)).save(mappedPromotion);
        verifyNoInteractions(buyXGetYPromotionRepository, productRepository);
    }

    @Test
    @DisplayName("Should create BuyXGetY promotion successfully")
    void create_ShouldCreateBuyXGetYPromotion_WhenValidInput() {
        // Given
        BuyXGetYPromotion mappedPromotion = BuyXGetYPromotion.builder()
                .id(UUID.randomUUID())
                .description("Buy 2 Get 1 Free")
                .productId(productId)
                .buyX(2)
                .getY(1)
                .build();

        when(productRepository.existsById(productId)).thenReturn(true);
        when(buyXGetYPromotionRepository.existsByProductId(productId)).thenReturn(false);
        when(modelMapper.map(buyXGetYDto, BuyXGetYPromotion.class)).thenReturn(mappedPromotion);
        when(promotionRepository.save(any(BuyXGetYPromotion.class))).thenReturn(mappedPromotion);

        // When
        ResponseEntity<?> response = promotionService.create(buyXGetYDto);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(mappedPromotion);

        verify(productRepository, times(1)).existsById(productId);
        verify(buyXGetYPromotionRepository, times(1)).existsByProductId(productId);
        verify(modelMapper, times(1)).map(buyXGetYDto, BuyXGetYPromotion.class);
        verify(promotionRepository, times(1)).save(mappedPromotion);
        verifyNoInteractions(percentOffCategoryPromotionRepository);
    }

    @Test
    @DisplayName("Should handle conflict scenarios for both promotion types")
    void create_ShouldReturnConflict_WhenPromotionAlreadyExists() {
        // Test PercentOffCategory conflict
        when(percentOffCategoryPromotionRepository.existsByCategory(ProductCategory.ELECTRONICS)).thenReturn(true);

        ResponseEntity<?> percentOffResponse = promotionService.create(percentOffDto);

        assertThat(percentOffResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        PromotionDto.ResponseDto responseDto = (PromotionDto.ResponseDto) percentOffResponse.getBody();
        assertThat(responseDto.getStatus()).isEqualTo(String.valueOf(HttpStatus.CONFLICT));
        assertThat(responseDto.getDescription()).isEqualTo("Promotion category already exists");

        verify(percentOffCategoryPromotionRepository, times(1)).existsByCategory(ProductCategory.ELECTRONICS);
        verify(promotionRepository, never()).save(any());

        // Test BuyXGetY conflict
        when(productRepository.existsById(productId)).thenReturn(true);
        when(buyXGetYPromotionRepository.existsByProductId(productId)).thenReturn(true);

        ResponseEntity<?> buyXGetYResponse = promotionService.create(buyXGetYDto);

        assertThat(buyXGetYResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        PromotionDto.ResponseDto buyXGetYResponseDto = (PromotionDto.ResponseDto) buyXGetYResponse.getBody();
        assertThat(buyXGetYResponseDto.getStatus()).isEqualTo(String.valueOf(HttpStatus.CONFLICT.value()));
        assertThat(buyXGetYResponseDto.getDescription()).isEqualTo("Promotion for product already exists");

        verify(buyXGetYPromotionRepository, times(1)).existsByProductId(productId);
    }

    @Test
    @DisplayName("Should handle BuyXGetY validation errors")
    void create_ShouldReturnNotFound_WhenProductNotExists() {
        // Given
        when(productRepository.existsById(productId)).thenReturn(false);

        // When
        ResponseEntity<?> response = promotionService.create(buyXGetYDto);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        PromotionDto.ResponseDto responseDto = (PromotionDto.ResponseDto) response.getBody();
        assertThat(responseDto.getStatus()).isEqualTo(String.valueOf(HttpStatus.NOT_FOUND.value()));
        assertThat(responseDto.getDescription()).isEqualTo("Product not found");

        verify(productRepository, times(1)).existsById(productId);
        verify(buyXGetYPromotionRepository, never()).existsByProductId(any());
        verify(promotionRepository, never()).save(any());
        verifyNoInteractions(modelMapper);
    }
}