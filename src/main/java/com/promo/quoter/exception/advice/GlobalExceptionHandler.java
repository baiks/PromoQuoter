package com.promo.quoter.exception.advice;

import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.promo.quoter.exception.CustomException;
import com.promo.quoter.exception.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
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

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                  HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String message = "Invalid request format";

        // Check for specific parsing errors
        if (ex.getMessage() != null) {
            if (ex.getMessage().contains("UUID has to be represented by standard 36-char representation")) {
                message = "Invalid UUID format. UUID must be in format: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx";
            } else if (ex.getMessage().contains("JSON parse error")) {
                message = "Invalid JSON format in request body";
            } else if (ex.getMessage().contains("Cannot deserialize")) {
                message = "Invalid data format in request";
            }
        }

        log.error("HttpMessageNotReadableException: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(HttpStatus.BAD_REQUEST.toString(), message));
    }

    @ExceptionHandler(InvalidDataAccessApiUsageException.class)
    public ResponseEntity<ErrorResponse> handleInvalidDataAccessApiUsageException(InvalidDataAccessApiUsageException e) {
        String message = "Invalid data access operation";

        // Check for specific error patterns
        if (e.getMessage() != null) {
            if (e.getMessage().contains("The given id must not be null")) {
                message = "Required ID parameter is missing or null";
            } else if (e.getMessage().contains("No entity found")) {
                message = "Requested resource not found";
            } else {
                message = "Invalid data access: " + e.getMessage();
            }
        }

        log.error("InvalidDataAccessApiUsageException: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(HttpStatus.BAD_REQUEST.toString(), message));
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

    // Add a general exception handler as fallback
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception e) {
        log.error("Unhandled exception: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.toString(),
                        "An unexpected error occurred"));
    }
}