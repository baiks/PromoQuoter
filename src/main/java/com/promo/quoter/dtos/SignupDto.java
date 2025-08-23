package com.promo.quoter.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class SignupDto {
    @Pattern(regexp = "\\w+", message = "Username should not contain any special character")
    @NotBlank(message = "Username cannot be blank")
    @Size(min = 3, max = 20, message = "Minimum of 3 characters expected")
    private String username;
    @NotBlank(message = "Please enter a password")
    @Size(min = 6, max = 40, message = "Minimum of 6 characters expected")
    private String password;
}