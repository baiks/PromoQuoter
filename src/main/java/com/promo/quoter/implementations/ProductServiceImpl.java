package com.promo.quoter.implementations;

import com.promo.quoter.dtos.ProductDto;
import com.promo.quoter.entities.Product;
import com.promo.quoter.repos.ProductRepository;
import com.promo.quoter.services.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {
    private final ProductRepository productRepository;
    private final ModelMapper modelMapper;

    @Override
    public ResponseEntity<?> create(ProductDto.CreateProductDto createProductDto) {

        Product product = modelMapper.map(createProductDto, Product.class);
        productRepository.save(product);
        return ResponseEntity.status(HttpStatus.CREATED).body(product);
    }

    @Override
    public List<Product> findAll() {
        return productRepository.findAll();
    }
}