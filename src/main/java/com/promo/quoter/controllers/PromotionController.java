package com.promo.quoter.controllers;

import com.promo.quoter.dtos.PromotionDto;
import com.promo.quoter.services.PromotionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("promotions")
@RequiredArgsConstructor
@Tag(name = "Promotions", description = "Promotions management APIs")
public class PromotionController {

    private final PromotionService promotionService;

    @Operation(
            summary = "Create a new promotion",
            description = "Creates a new promotion with type, category, and optional product info",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody( // fully qualified
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PromotionDto.CreatePromotionDto.class),
                            examples = @ExampleObject(
                                    value = "{\n" +
                                            "  \"promotionType\": \"PERCENT_OFF_CATEGORY\",\n" +
                                            "  \"description\": \"Summer sale for electronics\",\n" +
                                            "  \"percentOff\": 10,\n" +
                                            "  \"category\": \"ELECTRONICS\",\n" +
                                            "  \"productId\": \"3fa85f64-5717-4562-b3fc-2c963f66afa6\",\n" +
                                            "  \"buyX\": 0,\n" +
                                            "  \"getY\": 0\n" +
                                            "}"
                            )
                    )
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Promotion created successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = PromotionDto.ResponseDto.class),
                                    examples = @ExampleObject(
                                            value = "{\n" +
                                                    "  \"id\": \"1a534ebf-ef09-4ea6-bde2-8a5a0e9ebbd8\",\n" +
                                                    "  \"promotionType\": \"PERCENT_OFF_CATEGORY\",\n" +
                                                    "  \"description\": \"Summer sale for electronics\",\n" +
                                                    "  \"percentOff\": 10,\n" +
                                                    "  \"category\": \"ELECTRONICS\"\n" +
                                                    "}"
                                    )
                            )
                    )
            }
    )
    @PostMapping
    public ResponseEntity<?> create(@Valid @org.springframework.web.bind.annotation.RequestBody PromotionDto.CreatePromotionDto createPromotionDto) {
        return promotionService.create(createPromotionDto);
    }
}
