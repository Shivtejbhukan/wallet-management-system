package com.techpulse.wallet.service.Impl;

import com.techpulse.wallet.dto.request.LoginRequest;
import com.techpulse.wallet.dto.request.RegisterRequest;
import com.techpulse.wallet.dto.response.AuthResponse;
import com.techpulse.wallet.entity.User;
import com.techpulse.wallet.entity.Wallet;
import com.techpulse.wallet.enums.Role;
import com.techpulse.wallet.exception.EmailAlreadyExistsException;
import com.techpulse.wallet.repository.UserRepository;
import com.techpulse.wallet.repository.WalletRepository;
import com.techpulse.wallet.security.JwtService;
import com.techpulse.wallet.service.AuthService;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthServiceImpl(
            UserRepository userRepository,
            WalletRepository walletRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(
                    "An account with this email already exists");
        }

        User user = new User(
                request.email(),
                passwordEncoder.encode(request.password()),
                Role.USER
        );

        User savedUser = userRepository.save(user);

        Wallet wallet = new Wallet(savedUser);
        walletRepository.save(wallet);

        // JWT will be added in the login step.
        return new AuthResponse(
                null,
                "Bearer",
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getRole().name()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() ->
                        new org.springframework.security.authentication
                                .BadCredentialsException(
                                "Invalid email or password"));

        if (!passwordEncoder.matches(
                request.password(), user.getPassword())) {
            throw new org.springframework.security.authentication
                    .BadCredentialsException(
                    "Invalid email or password");
        }

        String token = jwtService.generateToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        );

        return new AuthResponse(
                token,
                "Bearer",
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        );
    }



}