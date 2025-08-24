package com.promo.quoter.implementations;

import com.promo.quoter.dtos.ProductDto;
import com.promo.quoter.entities.Product;
import com.promo.quoter.enums.ProductCategory;
import com.promo.quoter.repos.ProductRepository;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductServiceImpl Tests")
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private ProductServiceImpl productService;

    private ProductDto.CreateProductDto createProductDto;
    private Product product;

    @BeforeEach
    void setUp() {
        createProductDto = ProductDto.CreateProductDto.builder()
                .name("Test Product")
                .category(ProductCategory.ELECTRONICS)
                .price(new BigDecimal("99.99"))
                .stock(10)
                .build();

        product = Product.builder()
                .id(UUID.randomUUID())
                .name("Test Product")
                .category(ProductCategory.ELECTRONICS)
                .price(new BigDecimal("99.99"))
                .stock(10)
                .build();
    }

    @Test
    @DisplayName("Should create product successfully and return CREATED status")
    void create_ShouldReturnCreatedProduct_WhenValidInput() {
        // Given
        when(modelMapper.map(createProductDto, Product.class)).thenReturn(product);
        when(productRepository.save(any(Product.class))).thenReturn(product);

        // When
        ResponseEntity<?> response = productService.create(createProductDto);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(product);
        verify(modelMapper, times(1)).map(createProductDto, Product.class);
        verify(productRepository, times(1)).save(product);
    }

    @Test
    @DisplayName("Should handle different product categories and stock levels")
    void create_ShouldHandleDifferentProductData() {
        // Given
        ProductDto.CreateProductDto clothingDto = ProductDto.CreateProductDto.builder()
                .name("T-Shirt")
                .category(ProductCategory.CLOTHING)
                .price(new BigDecimal("25.99"))
                .stock(0)
                .build();

        Product clothingProduct = Product.builder()
                .id(UUID.randomUUID())
                .name("T-Shirt")
                .category(ProductCategory.CLOTHING)
                .price(new BigDecimal("25.99"))
                .stock(0)
                .build();

        when(modelMapper.map(clothingDto, Product.class)).thenReturn(clothingProduct);
        when(productRepository.save(any(Product.class))).thenReturn(clothingProduct);

        // When
        ResponseEntity<?> response = productService.create(clothingDto);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Product responseProduct = (Product) response.getBody();
        assertThat(responseProduct.getCategory()).isEqualTo(ProductCategory.CLOTHING);
        assertThat(responseProduct.getStock()).isEqualTo(0);
        assertThat(responseProduct.getPrice()).isEqualByComparingTo(new BigDecimal("25.99"));
    }

    @Test
    @DisplayName("Should handle null input gracefully")
    void create_ShouldHandleNullInput() {
        // Given
        ProductDto.CreateProductDto nullDto = null;
        when(modelMapper.map(nullDto, Product.class)).thenReturn(null);
        when(productRepository.save(any())).thenReturn(null);

        // When
        ResponseEntity<?> response = productService.create(nullDto);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNull();
        verify(modelMapper, times(1)).map(nullDto, Product.class);
        verify(productRepository, times(1)).save(null);
    }

    @Test
    @DisplayName("Should return all products when they exist")
    void findAll_ShouldReturnAllProducts_WhenProductsExist() {
        // Given
        Product product1 = Product.builder()
                .id(UUID.randomUUID())
                .name("Product 1")
                .category(ProductCategory.ELECTRONICS)
                .price(new BigDecimal("199.99"))
                .stock(20)
                .build();

        Product product2 = Product.builder()
                .id(UUID.randomUUID())
                .name("Product 2")
                .category(ProductCategory.BOOKS)
                .price(new BigDecimal("19.99"))
                .stock(50)
                .build();

        List<Product> expectedProducts = Arrays.asList(product1, product2);
        when(productRepository.findAll()).thenReturn(expectedProducts);

        // When
        List<Product> actualProducts = productService.findAll();

        // Then
        assertThat(actualProducts).hasSize(2);
        assertThat(actualProducts).containsExactly(product1, product2);
        verify(productRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should return empty list when no products exist")
    void findAll_ShouldReturnEmptyList_WhenNoProductsExist() {
        // Given
        when(productRepository.findAll()).thenReturn(Collections.emptyList());

        // When
        List<Product> actualProducts = productService.findAll();

        // Then
        assertThat(actualProducts).isEmpty();
        verify(productRepository, times(1)).findAll();
    }
}