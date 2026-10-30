
package com.techpulse.wallet.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        System.out.println("\n========== JWT FILTER START ==========");
        System.out.println("Request: " + request.getMethod()
                + " " + request.getRequestURI());

        String authHeader = request.getHeader("Authorization");

        // Never print the actual token or Authorization header.
        System.out.println("Authorization header present: "
                + (authHeader != null));

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            System.out.println("No Bearer token found. Continuing filter chain.");

            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            String email = jwtService.extractUsername(token);
            System.out.println("JWT email extracted: " + email);

            if (email != null &&
                    SecurityContextHolder.getContext()
                            .getAuthentication() == null) {

                UserDetails userDetails =
                        userDetailsService.loadUserByUsername(email);

                System.out.println("User found in database: "
                        + userDetails.getUsername());

                boolean valid = jwtService.isTokenValid(token, email);
                System.out.println("JWT token valid: " + valid);
                System.out.println("User authorities: "
                        + userDetails.getAuthorities());

                if (valid) {
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );

                    authentication.setDetails(
                            new WebAuthenticationDetailsSource()
                                    .buildDetails(request)
                    );

                    SecurityContextHolder.getContext()
                            .setAuthentication(authentication);

                    System.out.println("Authentication set successfully.");
                    System.out.println("Authenticated: "
                            + SecurityContextHolder.getContext()
                            .getAuthentication().isAuthenticated());
                } else {
                    System.out.println("ERROR: JWT validation failed.");
                }
            } else if (email != null) {
                System.out.println(
                        "Authentication already exists in SecurityContext.");
            }

        } catch (UsernameNotFoundException ex) {
            SecurityContextHolder.clearContext();
            System.out.println("ERROR: User not found: " + ex.getMessage());

        } catch (io.jsonwebtoken.JwtException |
                 IllegalArgumentException ex) {
            SecurityContextHolder.clearContext();
            System.out.println("ERROR: JWT processing failed: "
                    + ex.getMessage());

        } catch (Exception ex) {
            SecurityContextHolder.clearContext();
            System.out.println("ERROR: Unexpected authentication error: "
                    + ex.getClass().getSimpleName() + ": "
                    + ex.getMessage());
            ex.printStackTrace();
        }

        filterChain.doFilter(request, response);

        System.out.println("Response status: " + response.getStatus());
        System.out.println("========== JWT FILTER END ==========\n");
    }
}
