package com.promo.quoter.services;

import com.promo.quoter.dtos.ProductDto;
import com.promo.quoter.entities.Product;
import jakarta.transaction.Transactional;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface ProductService {
    @Transactional
    ResponseEntity<?> create(ProductDto.CreateProductDto createProductDto);
    List<Product> findAll();
}
