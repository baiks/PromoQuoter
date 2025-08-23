package com.promo.quoter.controllers;

import com.promo.quoter.dtos.ProductDto;
import com.promo.quoter.services.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping(value = "products")
public class ProductController {
    private final ProductService productService;

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody ProductDto.CreateProductDto createProductDto) {
        return productService.create(createProductDto);
    }
}
