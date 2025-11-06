package com.mybanking.app.user.service;

import com.mybanking.app.user.entity.User;
import com.mybanking.app.user.repository.UserRepository;
import com.mybanking.app.common.util.CustomerNumberGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock CustomerNumberGenerator customerNumberGenerator;

    @InjectMocks UserService userService;

    @Test
    void register_ok_generates_customer_number_and_hashes_password() {
        when(customerNumberGenerator.next()).thenReturn("CUST-123");
        when(passwordEncoder.encode("S3cret!")).thenReturn("{bcrypt}hash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
            return u;
        });

        var created = userService.register("Jonathan", "Jackson", "jonathanjackson@mybanking.nl", "S3cret!");

        ArgumentCaptor<User> cap = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(cap.capture());
        var toSave = cap.getValue();

        assertThat(toSave.getCustomerNumber()).isEqualTo("CUST-123");
        assertThat(toSave.getName()).isEqualTo("Jonathan");
        assertThat(toSave.getSurname()).isEqualTo("Jackson");
        assertThat(toSave.getEmail()).isEqualTo("jonathanjackson@mybanking.nl");
        assertThat(toSave.getPassword()).isEqualTo("{bcrypt}hash");
        assertThat(created.getId()).isNotNull();

        verify(customerNumberGenerator).next();
        verify(passwordEncoder).encode("S3cret!");
    }

    @Test
    void getByCustomerNumber_found() {
        var u = new User();
        u.setId(UUID.randomUUID());
        u.setCustomerNumber("CUST-9");
        when(userRepository.findByCustomerNumber("CUST-9")).thenReturn(Optional.of(u));

        var res = userService.getByCustomerNumber("CUST-9");

        assertThat(res.getCustomerNumber()).isEqualTo("CUST-9");
        verify(userRepository).findByCustomerNumber("CUST-9");
    }

    @Test
    void getByCustomerNumber_not_found_throws() {
        when(userRepository.findByCustomerNumber("X")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getByCustomerNumber("X"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void getById_found() {
        var id = UUID.randomUUID();
        var u = new User();
        u.setId(id);
        when(userRepository.findById(id)).thenReturn(Optional.of(u));

        var res = userService.getById(id);

        assertThat(res.getId()).isEqualTo(id);
        verify(userRepository).findById(id);
    }

    @Test
    void getById_not_found_throws() {
        var id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getById(id))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found");
    }
}