
package com.techpulse.wallet.controller;

import com.techpulse.wallet.dto.request.AddMoneyRequest;
import com.techpulse.wallet.dto.request.TransferRequest;
import com.techpulse.wallet.dto.response.TransactionResponse;
import com.techpulse.wallet.dto.response.WalletResponse;
import com.techpulse.wallet.service.WalletService;

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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletControllerTest {

    @Mock
    private WalletService walletService;

    @InjectMocks
    private WalletController walletController;

    private Authentication authentication() {
        return new UsernamePasswordAuthenticationToken(
                "test@gmail.com", null
        );
    }

    // Test 1: Get my wallet
    @Test
    void getMyWalletSuccessfully() {

        WalletResponse wallet = new WalletResponse(
                1L, new BigDecimal("1000.00"), 0L
        );

        when(walletService.getMyWallet("test@gmail.com"))
                .thenReturn(wallet);

        ResponseEntity<WalletResponse> response =
                walletController.getMyWallet(authentication());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(wallet, response.getBody());

        verify(walletService).getMyWallet("test@gmail.com");
    }

    // Test 2: Add money
    @Test
    void addMoneySuccessfully() {

        AddMoneyRequest request =
                new AddMoneyRequest(new BigDecimal("500.00"));

        TransactionResponse transaction = new TransactionResponse(
                1L, "ADD", "SUCCESS",
                new BigDecimal("500.00"),
                "add-key-123",
                LocalDateTime.now()
        );

        when(walletService.addMoney(
                "test@gmail.com",
                new BigDecimal("500.00"),
                "add-key-123"
        )).thenReturn(transaction);

        ResponseEntity<TransactionResponse> response =
                walletController.addMoney(
                        request, "add-key-123", authentication()
                );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(transaction, response.getBody());

        verify(walletService).addMoney(
                "test@gmail.com",
                new BigDecimal("500.00"),
                "add-key-123"
        );
    }

    // Test 3: Transfer money
    @Test
    void transferMoneySuccessfully() {

        TransferRequest request =
                new TransferRequest(2L, new BigDecimal("200.00"));

        TransactionResponse transaction = new TransactionResponse(
                2L, "TRANSFER", "SUCCESS",
                new BigDecimal("200.00"),
                "transfer-key-123",
                LocalDateTime.now()
        );

        when(walletService.transferMoney(
                "test@gmail.com",
                2L,
                new BigDecimal("200.00"),
                "transfer-key-123"
        )).thenReturn(transaction);

        ResponseEntity<TransactionResponse> response =
                walletController.transferMoney(
                        request, "transfer-key-123", authentication()
                );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(transaction, response.getBody());

        verify(walletService).transferMoney(
                "test@gmail.com",
                2L,
                new BigDecimal("200.00"),
                "transfer-key-123"
        );
    }

    // Test 4: Get my transactions
    @Test
    void getMyTransactionsSuccessfully() {

        TransactionResponse transaction = new TransactionResponse(
                1L, "ADD", "SUCCESS",
                new BigDecimal("500.00"),
                "key-123",
                LocalDateTime.now()
        );

        Page<TransactionResponse> page = new PageImpl<>(
                List.of(transaction),
                PageRequest.of(0, 10),
                1
        );

        when(walletService.getMyTransactions("test@gmail.com", 0, 10))
                .thenReturn(page);

        ResponseEntity<Page<TransactionResponse>> response =
                walletController.getMyTransactions(
                        authentication(), 0, 10
                );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().getTotalElements());

        verify(walletService).getMyTransactions("test@gmail.com", 0, 10);
    }

    // Test 5: Get transactions with custom pagination
    @Test
    void getMyTransactionsWithCustomPagination() {

        Page<TransactionResponse> page = new PageImpl<>(
                List.of(),
                PageRequest.of(1, 5),
                0
        );

        when(walletService.getMyTransactions("test@gmail.com", 1, 5))
                .thenReturn(page);

        ResponseEntity<Page<TransactionResponse>> response =
                walletController.getMyTransactions(
                        authentication(), 1, 5
                );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isEmpty());

        verify(walletService).getMyTransactions("test@gmail.com", 1, 5);
    }
}
