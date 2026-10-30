
package com.techpulse.wallet.controller;

import com.techpulse.wallet.dto.request.LoginRequest;
import com.techpulse.wallet.dto.request.RegisterRequest;
import com.techpulse.wallet.dto.response.AuthResponse;
import com.techpulse.wallet.service.AuthService;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@Tag(
        name = "Authentication APIs",
        description = "APIs for user registration and login"
)
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(
            summary = "Register a new user",
            description = "Creates a new user account and automatically creates a wallet for that user."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User registered successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid registration details"),
            @ApiResponse(responseCode = "409", description = "Email already exists")
    })
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request) {

        AuthResponse response = authService.register(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "User login",
            description = "Authenticates the user and returns a JWT access token."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "401", description = "Invalid email or password")
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {

        return ResponseEntity.ok(authService.login(request));
    }
}
