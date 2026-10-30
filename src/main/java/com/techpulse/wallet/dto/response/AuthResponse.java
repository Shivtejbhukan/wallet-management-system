
package com.techpulse.wallet.dto.response;

public record AuthResponse(
        String token,
        String tokenType,
        Long userId,
        String email,
        String role
) {}
