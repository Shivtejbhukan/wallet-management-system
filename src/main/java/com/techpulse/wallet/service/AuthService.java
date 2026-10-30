package com.techpulse.wallet.service;

import com.techpulse.wallet.dto.request.LoginRequest;
import com.techpulse.wallet.dto.request.RegisterRequest;
import com.techpulse.wallet.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);

}
