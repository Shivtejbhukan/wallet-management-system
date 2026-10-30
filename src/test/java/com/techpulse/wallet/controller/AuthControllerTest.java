
package com.techpulse.wallet.controller;

import com.techpulse.wallet.dto.request.LoginRequest;
import com.techpulse.wallet.dto.request.RegisterRequest;
import com.techpulse.wallet.dto.response.AuthResponse;
import com.techpulse.wallet.service.AuthService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    // Test 1: Register user successfully
    @Test
    void registerUserSuccessfully() {

        RegisterRequest request =
                new RegisterRequest("test@gmail.com", "password123");

        AuthResponse expectedResponse =
                new AuthResponse(
                        null,
                        "Bearer",
                        1L,
                        "test@gmail.com",
                        "USER"
                );

        when(authService.register(request))
                .thenReturn(expectedResponse);

        ResponseEntity<AuthResponse> response =
                authController.register(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(expectedResponse, response.getBody());

        verify(authService).register(request);
    }

    // Test 2: Login user successfully
    @Test
    void loginUserSuccessfully() {

        LoginRequest request =
                new LoginRequest("test@gmail.com", "password123");

        AuthResponse expectedResponse =
                new AuthResponse(
                        "sample-jwt-token",
                        "Bearer",
                        1L,
                        "test@gmail.com",
                        "USER"
                );

        when(authService.login(request))
                .thenReturn(expectedResponse);

        ResponseEntity<AuthResponse> response =
                authController.login(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(expectedResponse, response.getBody());

        verify(authService).login(request);
    }
}
