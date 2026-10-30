package com.techpulse.wallet.service;

import com.techpulse.wallet.dto.request.RegisterRequest;
import com.techpulse.wallet.dto.response.AuthResponse;
import com.techpulse.wallet.entity.User;
import com.techpulse.wallet.entity.Wallet;
import com.techpulse.wallet.enums.Role;
import com.techpulse.wallet.exception.EmailAlreadyExistsException;
import com.techpulse.wallet.repository.UserRepository;
import com.techpulse.wallet.repository.WalletRepository;
import com.techpulse.wallet.security.JwtService;
import com.techpulse.wallet.service.Impl.AuthServiceImpl;
import com.techpulse.wallet.dto.request.LoginRequest;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void registerUserSuccessfully() {

        // Arrange
        RegisterRequest request =
                new RegisterRequest("test@example.com", "password123");

        when(userRepository.existsByEmail("test@example.com"))
                .thenReturn(false);

        when(passwordEncoder.encode("password123"))
                .thenReturn("encodedPassword");

        User savedUser =
                new User("test@example.com", "encodedPassword", Role.USER);

        when(userRepository.save(any(User.class)))
                .thenReturn(savedUser);

        // Act
        AuthResponse response = authService.register(request);

        // Assert
        assertNotNull(response);
        assertEquals("test@example.com", response.email());
        assertEquals("USER", response.role());
        assertNull(response.token());

        verify(userRepository).save(any(User.class));
        verify(walletRepository).save(any(Wallet.class));
    }

    @Test
    void registerUserWithExistingEmail() {

        RegisterRequest request =
                new RegisterRequest("test@example.com", "password123");

        when(userRepository.existsByEmail("test@example.com"))
                .thenReturn(true);

        assertThrows(
                EmailAlreadyExistsException.class,
                () -> authService.register(request)
        );

        verify(userRepository, never()).save(any(User.class));
        verify(walletRepository, never()).save(any(Wallet.class));
    }



    @Test
    void loginUserSuccessfully() {

        // Arrange
        LoginRequest request =
                new LoginRequest("test@example.com", "password123");

        User user =
                new User("test@example.com", "encodedPassword", Role.USER);

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches("password123", "encodedPassword"))
                .thenReturn(true);

        when(jwtService.generateToken(
                any(), any(), any()))
                .thenReturn("dummy-jwt-token");

        // Act
        AuthResponse response = authService.login(request);

        // Assert
        assertNotNull(response);
        assertEquals("dummy-jwt-token", response.token());
        assertEquals("test@example.com", response.email());
        assertEquals("USER", response.role());
    }

    @Test
    void loginWithWrongPassword() {

        // Arrange
        LoginRequest request =
                new LoginRequest("test@example.com", "wrongPassword");

        User user =
                new User("test@example.com", "encodedPassword", Role.USER);

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches("wrongPassword", "encodedPassword"))
                .thenReturn(false);

        // Act and Assert
        assertThrows(
                org.springframework.security.authentication.BadCredentialsException.class,
                () -> authService.login(request)
        );

        verify(jwtService, never()).generateToken(any(), any(), any());
    }
}