package com.mybanking.app.user.service;

import com.mybanking.app.user.entity.User;
import com.mybanking.app.user.repository.UserRepository;
import com.mybanking.app.common.util.CustomerNumberGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CustomerNumberGenerator customerNumberGenerator;

    public User register(String name, String surname, String email, String rawPassword) {
        var user = User.builder()
                .customerNumber(customerNumberGenerator.next())
                .name(name)
                .surname(surname)
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .build();
        return userRepository.save(user);
    }

    public User getByCustomerNumber(String customerNumber) {
        return userRepository.findByCustomerNumber(customerNumber)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    public User getById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }
}
