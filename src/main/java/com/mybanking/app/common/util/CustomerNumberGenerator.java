package com.mybanking.app.common.util;

import com.mybanking.app.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Year;
import java.util.concurrent.ThreadLocalRandom;

@Component
@RequiredArgsConstructor
public class CustomerNumberGenerator {

    private final UserRepository userRepository;

    public String next() {
        String prefix = "RB-" + Year.now() + "-";
        while (true) {
            String seq = String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1_000_000));
            String candidate = prefix + seq;
            if (!userRepository.existsByCustomerNumber(candidate)) return candidate;
        }
    }
}
