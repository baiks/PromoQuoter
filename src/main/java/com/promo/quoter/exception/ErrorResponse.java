package com.promo.quoter.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ErrorResponse {
    String code;
    String message;

    public ErrorResponse(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
