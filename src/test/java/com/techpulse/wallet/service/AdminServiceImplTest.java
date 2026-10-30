
package com.techpulse.wallet.service;

import com.techpulse.wallet.dto.response.AdminWalletResponse;
import com.techpulse.wallet.dto.response.TransactionResponse;
import com.techpulse.wallet.entity.User;
import com.techpulse.wallet.entity.Wallet;
import com.techpulse.wallet.entity.WalletTransaction;
import com.techpulse.wallet.enums.Role;
import com.techpulse.wallet.enums.TransactionStatus;
import com.techpulse.wallet.enums.TransactionType;
import com.techpulse.wallet.repository.WalletRepository;
import com.techpulse.wallet.repository.WalletTransactionRepository;
import com.techpulse.wallet.service.Impl.AdminServiceImpl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceImplTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private WalletTransactionRepository walletTransactionRepository;

    @InjectMocks
    private AdminServiceImpl adminService;


    // Test 1: Get all wallets successfully
    @Test
    void getAllWalletsSuccessfully() {

        User user = new User(
                "adminuser@example.com",
                "password123",
                Role.USER
        );

        Wallet wallet = new Wallet();
        wallet.setUser(user);
        wallet.setBalance(new BigDecimal("500.00"));


        when(walletRepository.findAll())
                .thenReturn(List.of(wallet));

        List<AdminWalletResponse> result =
                adminService.getAllWallets();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("adminuser@example.com",
                result.get(0).email());
        assertEquals(new BigDecimal("500.00"),
                result.get(0).balance());

        verify(walletRepository).findAll();
    }


    // Test 2: Get all wallets when database is empty
    @Test
    void getAllWalletsWhenNoWalletsAvailable() {

        when(walletRepository.findAll())
                .thenReturn(List.of());

        List<AdminWalletResponse> result =
                adminService.getAllWallets();

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(walletRepository).findAll();
    }


    // Test 3: Get all transactions successfully
    @Test
    void getAllTransactionsSuccessfully() {

        LocalDateTime now = LocalDateTime.now();

        WalletTransaction transaction = new WalletTransaction(
                null,
                null,
                TransactionType.ADD,
                TransactionStatus.SUCCESS,
                new BigDecimal("200.00"),
                "admin-test-key"
        );

        when(walletTransactionRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(
                        List.of(transaction),
                        PageRequest.of(0, 10),
                        1
                ));

        Page<TransactionResponse> result =
                adminService.getAllTransactions(0, 10);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("ADD", result.getContent().get(0).type());
        assertEquals("SUCCESS", result.getContent().get(0).status());
        assertEquals(new BigDecimal("200.00"),
                result.getContent().get(0).amount());
        assertEquals("admin-test-key",
                result.getContent().get(0).idempotencyKey());

        verify(walletTransactionRepository).findAll(any(Pageable.class));
    }


    // Test 4: Get transactions when no records exist
    @Test
    void getAllTransactionsWhenNoTransactionsAvailable() {

        when(walletTransactionRepository.findAll(any(Pageable.class)))
                .thenReturn(Page.empty());

        Page<TransactionResponse> result =
                adminService.getAllTransactions(0, 10);

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(walletTransactionRepository)
                .findAll(any(Pageable.class));
    }


    // Test 5: Negative page number
    @Test
    void getAllTransactionsWithNegativePage() {

        assertThrows(
                IllegalArgumentException.class,
                () -> adminService.getAllTransactions(-1, 10)
        );

        verifyNoInteractions(walletTransactionRepository);
    }


    // Test 6: Page size is zero
    @Test
    void getAllTransactionsWithZeroSize() {

        assertThrows(
                IllegalArgumentException.class,
                () -> adminService.getAllTransactions(0, 0)
        );

        verifyNoInteractions(walletTransactionRepository);
    }


    // Test 7: Page size greater than 100
    @Test
    void getAllTransactionsWithSizeGreaterThan100() {

        assertThrows(
                IllegalArgumentException.class,
                () -> adminService.getAllTransactions(0, 101)
        );

        verifyNoInteractions(walletTransactionRepository);
    }
}
