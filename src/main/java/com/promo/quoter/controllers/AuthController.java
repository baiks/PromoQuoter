package com.promo.quoter.controllers;


import com.promo.quoter.dtos.JwtResponse;
import com.promo.quoter.dtos.LoginDto;
import com.promo.quoter.dtos.SignupDto;
import com.promo.quoter.entities.Users;
import com.promo.quoter.services.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
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
            description = """
                    Registers a new user and returns user details.
                    
                    ### Example Request Body:
                    ```json
                    {
                      "username": "2M015D_FghDV1Jsv4lQR",
                      "password": "string"
                    }
                    ```
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "User successfully registered",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Users.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid input data"
            )
    })
    public ResponseEntity<Users> register(@Valid @RequestBody SignupDto signupDto) {
        return authService.signUp(signupDto);
    }


    @PostMapping("/login")
    @Operation(
            summary = "Login user",
            description = """
                    Authenticates a user and returns a JWT response with user details.
                    
                    ### Example Request Body:
                    ```json
                    {
                      "username": "user@example.com",
                      "password": "password123"
                    }
                    ```
                    
                    ### Example Response:
                    ```json
                    {
                      "status_code": 200,
                      "username": "user@example.com",
                      "role": "USER",
                      "type": "Bearer",
                      "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                      "issuedAt": "2025-08-22T10:00:00Z",
                      "expiry": "2025-08-22T12:00:00Z"
                    }
                    ```
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "User successfully authenticated",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = JwtResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Bad request - validation failed"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - invalid credentials")
    })
    public ResponseEntity<JwtResponse> login(@Valid @RequestBody LoginDto loginDto) {
        return authService.authUser(loginDto);
    }

}