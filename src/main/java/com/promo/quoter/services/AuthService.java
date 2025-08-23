package com.promo.quoter.services;


import com.promo.quoter.dtos.JwtResponse;
import com.promo.quoter.dtos.LoginDto;
import com.promo.quoter.dtos.SignupDto;
import com.promo.quoter.entities.Users;
import org.springframework.http.ResponseEntity;


public interface AuthService {
    /**
     * @param signupDto
     * @return
     */
    ResponseEntity<Users> signUp(SignupDto signupDto);

    /**
     * @param loginDto
     * @return
     */
    ResponseEntity<JwtResponse> authUser(LoginDto loginDto);
}