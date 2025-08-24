package com.promo.quoter.dtos;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class JwtResponse {
    private int statusCode;
    private String username;
    private String role;
    private String type;
    private String token;
    private String issuedAt;
    private String expiry;
}