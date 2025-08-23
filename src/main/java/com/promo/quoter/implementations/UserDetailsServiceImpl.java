package com.promo.quoter.implementations;

import com.promo.quoter.entities.Users;
import com.promo.quoter.exception.CustomException;
import com.promo.quoter.repos.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UsersRepository usersRepository;

    @Override
    public UserDetails loadUserByUsername(String name) {
        Users user = usersRepository.findByUsername(name).orElseThrow(() -> new CustomException("Invalid Credentials", HttpStatus.NOT_FOUND));
        return UserDetailsImpl.build(user);
    }
}