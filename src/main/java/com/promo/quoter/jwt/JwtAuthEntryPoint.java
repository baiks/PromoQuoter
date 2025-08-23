package com.promo.quoter.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Component
@Slf4j
public class JwtAuthEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {
        log.error("Authorization error: {}", authException.getMessage());
        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        AuthErrorResponse authErrorResponse = new AuthErrorResponse();
        authErrorResponse.setCode(Integer.toString(UNAUTHORIZED.value()));
        authErrorResponse.setStatus(UNAUTHORIZED.name());
        authErrorResponse.setMessage(authException.getMessage().isEmpty() ? authException.getLocalizedMessage() : authException.getMessage());
        response.getOutputStream().println(new ObjectMapper().writeValueAsString(authErrorResponse));
    }

    @Getter
    @Setter
    private static class AuthErrorResponse {
        private String code;
        private String status;
        private String message;
        private String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("YYYY-MM-dd hh:MM:ss a"));
    }
}
