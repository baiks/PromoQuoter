package com.promo.quoter.exception.advice;


import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.promo.quoter.exception.CustomException;
import com.promo.quoter.exception.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;


@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler extends MessageSourceAdviceCtrl {

    protected GlobalExceptionHandler(MessageSource messageSource) {
        super(messageSource);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        Map<String, String> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }
        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleExceptionDataIntegrityViolationException(
            DataIntegrityViolationException e) {
        if (e.getCause() instanceof ConstraintViolationException sqlEx) {
            String message = "Database integrity constraint violation: " + sqlEx.getMessage();
            if (sqlEx.getMessage().contains("Duplicate entry")) {
                message = "Duplicate entry detected";
            }
            log.error("SQLIntegrityConstraintViolationException: ", sqlEx);
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ErrorResponse(HttpStatus.CONFLICT.toString(), message));
        }

        // If it's not an SQLIntegrityConstraintViolationException, just handle the general case
        String message = e.getCause().getLocalizedMessage();
        log.error("Invalid input {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(HttpStatus.BAD_REQUEST.toString(), message));
    }

    @ExceptionHandler(MismatchedInputException.class)
    public ResponseEntity<ErrorResponse> handleExceptionMismatchedInputException(MismatchedInputException e) {
        String message = "Invalid Data types";
        log.error("Mismatched Input Exception...{}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(HttpStatus.BAD_REQUEST.toString(), message));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleApiRequestException(ResponseStatusException e) {
        String message = NestedExceptionUtils.getMostSpecificCause(e).getMessage();
        log.error("ResponseStatusException...{}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(HttpStatus.BAD_REQUEST.toString(), message));
    }

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleExceptionCustomException(CustomException e) {
        log.error("Custom Exception... {}", e.getMessage());
        return ResponseEntity.status(e.getStatus())
                .body(new ErrorResponse(e.getStatus().toString(), e.getMessage()));
    }
}
