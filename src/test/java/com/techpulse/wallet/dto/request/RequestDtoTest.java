
package com.techpulse.wallet.dto.request;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class RequestDtoTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    // RegisterRequest tests

    @Test
    void registerRequestWithValidData() {
        RegisterRequest request =
                new RegisterRequest("test@gmail.com", "password123");

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void registerRequestWithInvalidEmail() {
        RegisterRequest request =
                new RegisterRequest("invalid-email", "password123");

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void registerRequestWithShortPassword() {
        RegisterRequest request =
                new RegisterRequest("test@gmail.com", "123");

        assertFalse(validator.validate(request).isEmpty());
    }

    // LoginRequest tests

    @Test
    void loginRequestWithValidData() {
        LoginRequest request =
                new LoginRequest("test@gmail.com", "password123");

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void loginRequestWithBlankPassword() {
        LoginRequest request =
                new LoginRequest("test@gmail.com", "");

        assertFalse(validator.validate(request).isEmpty());
    }

    // AddMoneyRequest tests

    @Test
    void addMoneyRequestWithValidAmount() {
        AddMoneyRequest request =
                new AddMoneyRequest(new BigDecimal("500.00"));

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void addMoneyRequestWithZeroAmount() {
        AddMoneyRequest request =
                new AddMoneyRequest(BigDecimal.ZERO);

        assertFalse(validator.validate(request).isEmpty());
    }

    // TransferRequest tests

    @Test
    void transferRequestWithValidData() {
        TransferRequest request =
                new TransferRequest(2L, new BigDecimal("200.00"));

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void transferRequestWithInvalidReceiverId() {
        TransferRequest request =
                new TransferRequest(0L, new BigDecimal("200.00"));

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void transferRequestWithZeroAmount() {
        TransferRequest request =
                new TransferRequest(2L, BigDecimal.ZERO);

        assertFalse(validator.validate(request).isEmpty());
    }
}
