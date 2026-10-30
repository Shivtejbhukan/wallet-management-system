package com.techpulse.wallet.security;



import io.jsonwebtoken.JwtException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockFilterChain;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserDetailsService userDetailsService;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain filterChain;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();

        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = new MockFilterChain();
    }

    // Test 1: Authorization header is missing
    @Test
    void doFilterWithoutAuthorizationHeader() throws Exception {

        jwtAuthenticationFilter.doFilter(
                request, response, filterChain
        );

        assertNull(
                SecurityContextHolder.getContext().getAuthentication()
        );

        verifyNoInteractions(jwtService, userDetailsService);
    }

    // Test 2: Authorization header has wrong prefix
    @Test
    void doFilterWithWrongAuthorizationPrefix() throws Exception {

        request.addHeader("Authorization", "Basic abc123");

        jwtAuthenticationFilter.doFilter(
                request, response, filterChain
        );

        assertNull(
                SecurityContextHolder.getContext().getAuthentication()
        );

        verifyNoInteractions(jwtService, userDetailsService);
    }

    // Test 3: Valid JWT token
    @Test
    void doFilterWithValidToken() throws Exception {

        request.addHeader("Authorization", "Bearer valid-token");

        UserDetails userDetails = User.withUsername("test@gmail.com")
                .password("encodedPassword")
                .authorities("ROLE_USER")
                .build();

        when(jwtService.extractUsername("valid-token"))
                .thenReturn("test@gmail.com");

        when(userDetailsService.loadUserByUsername("test@gmail.com"))
                .thenReturn(userDetails);

        when(jwtService.isTokenValid("valid-token", "test@gmail.com"))
                .thenReturn(true);

        jwtAuthenticationFilter.doFilter(
                request, response, filterChain
        );
        assertNotNull(
                SecurityContextHolder.getContext().getAuthentication()
        );

        assertTrue(
                SecurityContextHolder.getContext()
                        .getAuthentication().isAuthenticated()
        );

        assertEquals(
                "test@gmail.com",
                SecurityContextHolder.getContext()
                        .getAuthentication().getName()
        );

        verify(userDetailsService).loadUserByUsername("test@gmail.com");
        verify(jwtService).isTokenValid("valid-token", "test@gmail.com");
    }

    // Test 4: Invalid JWT token
    @Test
    void doFilterWithInvalidToken() throws Exception {

        request.addHeader("Authorization", "Bearer invalid-token");

        when(jwtService.extractUsername("invalid-token"))
                .thenThrow(new JwtException("Invalid JWT"));

        jwtAuthenticationFilter.doFilter(
                request, response, filterChain
        );

        assertNull(
                SecurityContextHolder.getContext().getAuthentication()
        );

        verifyNoInteractions(userDetailsService);
    }

    // Test 5: User does not exist
    @Test
    void doFilterWhenUserDoesNotExist() throws Exception {

        request.addHeader("Authorization", "Bearer valid-token");

        when(jwtService.extractUsername("valid-token"))
                .thenReturn("missing@gmail.com");

        when(userDetailsService.loadUserByUsername("missing@gmail.com"))
                .thenThrow(new UsernameNotFoundException("User not found"));

        jwtAuthenticationFilter.doFilter(
                request, response, filterChain
        );

        assertNull(
                SecurityContextHolder.getContext().getAuthentication()
        );

        verify(userDetailsService).loadUserByUsername("missing@gmail.com");
        verify(jwtService, never()).isTokenValid(anyString(), anyString());
    }

    // Test 6: Authentication already exists
    @Test
    void doFilterWhenAuthenticationAlreadyExists() throws Exception {

        request.addHeader("Authorization", "Bearer valid-token");

        var existingAuthentication =
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        "existing@gmail.com",
                        null,
                        java.util.List.of(
                                new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                        "ROLE_USER"
                                )
                        )
                );

        SecurityContextHolder.getContext()
                .setAuthentication(existingAuthentication);

        when(jwtService.extractUsername("valid-token"))
                .thenReturn("test@gmail.com");

        jwtAuthenticationFilter.doFilter(
                request, response, filterChain
        );

        assertSame(
                existingAuthentication,
                SecurityContextHolder.getContext().getAuthentication()
        );

        verifyNoInteractions(userDetailsService);
        verify(jwtService, never()).isTokenValid(anyString(), anyString());
    }
}
