
package com.techpulse.wallet.controller;

import com.techpulse.wallet.dto.response.AdminWalletResponse;
import com.techpulse.wallet.dto.response.TransactionResponse;
import com.techpulse.wallet.service.AdminService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private AdminService adminService;

    @InjectMocks
    private AdminController adminController;

    // Test 1: Get all wallets
    @Test
    void getAllWalletsSuccessfully() {

        List<AdminWalletResponse> wallets = List.of(
                new AdminWalletResponse(
                        1L, 101L, "user1@gmail.com",
                        new BigDecimal("1000.00"), 0L
                ),
                new AdminWalletResponse(
                        2L, 102L, "user2@gmail.com",
                        new BigDecimal("2000.00"), 0L
                )
        );

        when(adminService.getAllWallets()).thenReturn(wallets);

        ResponseEntity<List<AdminWalletResponse>> response =
                adminController.getAllWallets();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        assertEquals(wallets, response.getBody());

        verify(adminService).getAllWallets();
    }

    // Test 2: Get all transactions
    @Test
    void getAllTransactionsSuccessfully() {

        TransactionResponse transaction =
                new TransactionResponse(
                        1L,
                        "ADD",
                        "SUCCESS",
                        new BigDecimal("500.00"),
                        "key-123",
                        java.time.LocalDateTime.now()
                );

        Page<TransactionResponse> transactionPage =
                new PageImpl<>(
                        List.of(transaction),
                        PageRequest.of(0, 10),
                        1
                );

        when(adminService.getAllTransactions(0, 10))
                .thenReturn(transactionPage);

        ResponseEntity<Page<TransactionResponse>> response =
                adminController.getAllTransactions(0, 10);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getTotalElements());

        verify(adminService).getAllTransactions(0, 10);
    }

    // Test 3: Get transactions with custom pagination
    @Test
    void getAllTransactionsWithCustomPagination() {

        Page<TransactionResponse> transactionPage =
                new PageImpl<>(
                        List.of(),
                        PageRequest.of(1, 5),
                        0
                );

        when(adminService.getAllTransactions(1, 5))
                .thenReturn(transactionPage);

        ResponseEntity<Page<TransactionResponse>> response =
                adminController.getAllTransactions(1, 5);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isEmpty());

        verify(adminService).getAllTransactions(1, 5);
    }
}
