package com.hoang.crypto.service;


import com.hoang.crypto.entity.User;
import com.hoang.crypto.exception.ResourceNotFoundException;
import com.hoang.crypto.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {
    final private UserRepository userRepository;

    public User checkAndGetUser() {
        UserDetails userDetails =
                (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Optional<User> userOptional = userRepository.findByUsername(userDetails.getUsername());
        if (userOptional.isEmpty()) {
            throw new ResourceNotFoundException("User is empty");
        }
        return userOptional.get();
    }
}
