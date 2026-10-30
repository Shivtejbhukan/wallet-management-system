package com.techpulse.wallet.exception;



import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler =
            new GlobalExceptionHandler();

    @Test
    void handleDuplicateEmail() {

        EmailAlreadyExistsException exception =
                new EmailAlreadyExistsException("Email already registered");

        ResponseEntity<Map<String, String>> response =
                handler.handleDuplicateEmail(exception);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());

        assertNotNull(response.getBody());

        assertEquals(
                "Email Already Exists",
                response.getBody().get("error")
        );

        assertEquals(
                "Email already registered",
                response.getBody().get("message")
        );
    }
}
