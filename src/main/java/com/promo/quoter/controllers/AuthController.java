package com.promo.quoter.controllers;

import com.promo.quoter.dtos.JwtResponse;
import com.promo.quoter.dtos.LoginDto;
import com.promo.quoter.dtos.SignupDto;
import com.promo.quoter.entities.Users;
import com.promo.quoter.services.AuthService;
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

@RestController
@RequestMapping("user")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User authentication")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    @Operation(
            summary = "Create a new user account",
            description = "Registers a new user and returns user details",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SignupDto.class),
                            examples = @ExampleObject(
                                    value = "{\n" +
                                            "  \"username\": \"2M015D_FghDV1Jsv4lQR\",\n" +
                                            "  \"password\": \"string\"\n" +
                                            "}"
                            )
                    )
            )
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "User successfully registered",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Users.class),
                            examples = @ExampleObject(
                                    value = "{\n" +
                                            "  \"id\": 1,\n" +
                                            "  \"username\": \"baiks\",\n" +
                                            "  \"password\": \"$2a$10$T5rxlgHkMniLD551ZLiOv.rZ1e0NpFAxD2mlKfebypNUnjeuCO04O\",\n" +
                                            "  \"active\": true,\n" +
                                            "  \"role\": \"API_USER\"\n" +
                                            "}"
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    public ResponseEntity<Users> register(@Valid @org.springframework.web.bind.annotation.RequestBody SignupDto signupDto) {
        return authService.signUp(signupDto);
    }

    @PostMapping("/login")
    @Operation(
            summary = "Login user",
            description = "Authenticates a user and returns a JWT response with user details",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = LoginDto.class),
                            examples = @ExampleObject(
                                    value = "{\n" +
                                            "  \"username\": \"user@example.com\",\n" +
                                            "  \"password\": \"password123\"\n" +
                                            "}"
                            )
                    )
            )
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "User successfully authenticated",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = JwtResponse.class),
                            examples = @ExampleObject(
                                    value = "{\n" +
                                            "  \"status_code\": 200,\n" +
                                            "  \"username\": \"user@example.com\",\n" +
                                            "  \"role\": \"USER\",\n" +
                                            "  \"type\": \"Bearer\",\n" +
                                            "  \"token\": \"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...\",\n" +
                                            "  \"issuedAt\": \"2025-08-22T10:00:00Z\",\n" +
                                            "  \"expiry\": \"2025-08-22T12:00:00Z\"\n" +
                                            "}"
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Bad request - validation failed"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - invalid credentials")
    })
    public ResponseEntity<JwtResponse> login(@Valid @org.springframework.web.bind.annotation.RequestBody LoginDto loginDto) {
        return authService.authUser(loginDto);
    }
}
