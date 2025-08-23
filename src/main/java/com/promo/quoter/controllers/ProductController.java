package com.promo.quoter.controllers;

import com.promo.quoter.dtos.ProductDto;
import com.promo.quoter.entities.Product;
import com.promo.quoter.services.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Product management APIs")
public class ProductController {

    private final ProductService productService;

    @PostMapping
    @Operation(
            summary = "Create a new product",
            description = "Creates a new product and returns the product details",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ProductDto.CreateProductDto.class),
                            examples = @ExampleObject(
                                    value = "{\n" +
                                            "  \"name\": \"Electric Kettle\",\n" +
                                            "  \"category\": \"ELECTRONICS\",\n" +
                                            "  \"price\": 3000,\n" +
                                            "  \"stock\": 10\n" +
                                            "}"
                            )
                    )
            )
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Product successfully created",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ProductDto.ResponseDto.class),
                            examples = @ExampleObject(
                                    value = "{\n" +
                                            "  \"id\": \"9ba1b6ea-fc89-4cfc-8e67-6e9084ef4c69\",\n" +
                                            "  \"name\": \"Electric Kettle\",\n" +
                                            "  \"category\": \"ELECTRONICS\",\n" +
                                            "  \"price\": 3000,\n" +
                                            "  \"stock\": 10\n" +
                                            "}"
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    public ResponseEntity<?> create(@Valid @org.springframework.web.bind.annotation.RequestBody ProductDto.CreateProductDto createProductDto) {
        return productService.create(createProductDto);
    }
   @GetMapping
    public List<Product> create() {
        return productService.findAll();
    }
}
