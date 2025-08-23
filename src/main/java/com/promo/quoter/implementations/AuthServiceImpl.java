package com.promo.quoter.implementations;


import com.promo.quoter.dtos.JwtResponse;
import com.promo.quoter.dtos.LoginDto;
import com.promo.quoter.dtos.SignupDto;
import com.promo.quoter.entities.Users;
import com.promo.quoter.exception.CustomException;
import com.promo.quoter.jwt.JwtUtils;
import com.promo.quoter.repos.UsersRepository;
import com.promo.quoter.services.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final PasswordEncoder passwordEncoder;
    private final UsersRepository usersRepository;

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final ModelMapper modelMapper;


    /**
     * @param signupDto
     * @return
     */
    @Override
    public ResponseEntity<Users> signUp(SignupDto signupDto) {
        log.info("Request: {}", signupDto);
        try {
            Optional<Users> res = usersRepository.findByUsername(signupDto.getUsername().toLowerCase());
            if (res.isPresent()) {
                throw new CustomException("A user with the given username already exists.Try a different one.");
            }
            Users users = modelMapper.map(signupDto, Users.class);
            users.setRole("API_USER");
            users.setActive(true);
            users.setPassword(passwordEncoder.encode(signupDto.getPassword()));
            usersRepository.save(users);
            return new ResponseEntity<>(users, HttpStatus.CREATED);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * @param loginDto
     * @return
     */
    @Override
    public ResponseEntity<JwtResponse> authUser(LoginDto loginDto) {
        Authentication authentication = null;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginDto.getUsername().toLowerCase(Locale.ROOT).trim(),
                            loginDto.getPassword().trim()));


        } catch (BadCredentialsException e) {
            throw new CustomException("Invalid username or password.");
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        LocalDateTime presentDateTime = LocalDateTime.now();
        JwtResponse result = JwtResponse.builder()
                .username(userDetails.getUsername())
                .status_code(0)
                .role(null)
                .type("BEARER")
                .token(jwt)
                .issuedAt(presentDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss a")))
                .expiry(presentDateTime.plusSeconds(jwtUtils.expiryInMins * 60L).format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss a")))
                .build();
        return ResponseEntity.ok(result);
    }
}