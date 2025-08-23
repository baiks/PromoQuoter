package com.promo.quoter.services;

import com.promo.quoter.dtos.ProductDto;
import jakarta.transaction.Transactional;
import org.springframework.http.ResponseEntity;

public interface ProductService {
    @Transactional
    ResponseEntity<?> create(ProductDto.CreateProductDto createProductDto);
}
