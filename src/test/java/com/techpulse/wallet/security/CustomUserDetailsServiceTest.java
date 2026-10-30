package com.techpulse.wallet.security;



import com.techpulse.wallet.entity.User;
import com.techpulse.wallet.enums.Role;
import com.techpulse.wallet.repository.UserRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void loadUserByUsernameWhenUserExists() {

        User user = new User(
                "test@gmail.com",
                "encodedPassword",
                Role.USER
        );

        when(userRepository.findByEmail("test@gmail.com"))
                .thenReturn(Optional.of(user));

        UserDetails result =
                customUserDetailsService.loadUserByUsername("test@gmail.com");

        assertNotNull(result);
        assertEquals("test@gmail.com", result.getUsername());
        assertEquals("encodedPassword", result.getPassword());

        assertTrue(result.getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_USER")));

        verify(userRepository).findByEmail("test@gmail.com");
    }


    @Test
    void loadUserByUsernameWhenAdminExists() {

        User user = new User(
                "admin@gmail.com",
                "encodedPassword",
                Role.ADMIN
        );

        when(userRepository.findByEmail("admin@gmail.com"))
                .thenReturn(Optional.of(user));

        UserDetails result =
                customUserDetailsService.loadUserByUsername("admin@gmail.com");

        assertNotNull(result);
        assertEquals("admin@gmail.com", result.getUsername());

        assertTrue(result.getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));

        verify(userRepository).findByEmail("admin@gmail.com");
    }

    @Test
    void loadUserByUsernameWhenUserDoesNotExist() {

        when(userRepository.findByEmail("missing@gmail.com"))
                .thenReturn(Optional.empty());

        assertThrows(
                org.springframework.security.core.userdetails.UsernameNotFoundException.class,
                () -> customUserDetailsService.loadUserByUsername("missing@gmail.com")
        );

        verify(userRepository).findByEmail("missing@gmail.com");
    }

}
