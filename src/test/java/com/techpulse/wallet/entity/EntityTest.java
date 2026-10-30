
package com.techpulse.wallet.entity;

import com.techpulse.wallet.enums.Role;
import com.techpulse.wallet.enums.TransactionStatus;
import com.techpulse.wallet.enums.TransactionType;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class EntityTest {

    // ================= USER TESTS =================

    @Test
    void userDefaultConstructor() {

        User user = new User();

        assertNull(user.getId());
        assertNull(user.getEmail());
        assertNull(user.getPassword());
        assertEquals(Role.USER, user.getRole());
    }

    @Test
    void userParameterizedConstructor() {

        User user = new User(
                "test@gmail.com",
                "password123",
                Role.USER
        );

        assertEquals("test@gmail.com", user.getEmail());
        assertEquals("password123", user.getPassword());
        assertEquals(Role.USER, user.getRole());
    }

    @Test
    void userSettersAndGetters() {

        User user = new User();

        user.setEmail("admin@gmail.com");
        user.setPassword("admin123");
        user.setRole(Role.ADMIN);

        assertEquals("admin@gmail.com", user.getEmail());
        assertEquals("admin123", user.getPassword());
        assertEquals(Role.ADMIN, user.getRole());
    }

    // ================= WALLET TESTS =================

    @Test
    void walletDefaultConstructor() {

        Wallet wallet = new Wallet();

        assertNull(wallet.getId());
        assertNull(wallet.getUser());
        assertEquals(BigDecimal.ZERO, wallet.getBalance());
        assertNull(wallet.getVersion());
    }

    @Test
    void walletParameterizedConstructor() {

        User user = new User(
                "test@gmail.com",
                "password123",
                Role.USER
        );

        Wallet wallet = new Wallet(user);

        assertEquals(user, wallet.getUser());
        assertEquals(BigDecimal.ZERO, wallet.getBalance());
    }

    @Test
    void walletSetUser() {

        Wallet wallet = new Wallet();

        User user = new User(
                "test@gmail.com",
                "password123",
                Role.USER
        );

        wallet.setUser(user);

        assertEquals(user, wallet.getUser());
    }

    @Test
    void walletSetBalance() {

        Wallet wallet = new Wallet();

        wallet.setBalance(new BigDecimal("1500.50"));

        assertEquals(
                new BigDecimal("1500.50"),
                wallet.getBalance()
        );
    }

    // ================= WALLET TRANSACTION TESTS =================

    @Test
    void walletTransactionDefaultConstructor() {

        WalletTransaction transaction = new WalletTransaction();

        assertNull(transaction.getId());
        assertNull(transaction.getFromWallet());
        assertNull(transaction.getToWallet());
        assertNull(transaction.getType());
        assertNull(transaction.getStatus());
        assertNull(transaction.getAmount());
        assertNull(transaction.getIdempotencyKey());
        assertNull(transaction.getCreatedAt());
    }

    @Test
    void walletTransactionParameterizedConstructor() {

        Wallet sender = new Wallet(
                new User("sender@gmail.com", "password123", Role.USER)
        );

        Wallet receiver = new Wallet(
                new User("receiver@gmail.com", "password123", Role.USER)
        );

        WalletTransaction transaction = new WalletTransaction(
                sender,
                receiver,
                TransactionType.TRANSFER,
                TransactionStatus.SUCCESS,
                new BigDecimal("500.00"),
                "transfer-key-123"
        );

        assertEquals(sender, transaction.getFromWallet());
        assertEquals(receiver, transaction.getToWallet());
        assertEquals(TransactionType.TRANSFER, transaction.getType());
        assertEquals(TransactionStatus.SUCCESS, transaction.getStatus());
        assertEquals(new BigDecimal("500.00"), transaction.getAmount());
        assertEquals("transfer-key-123", transaction.getIdempotencyKey());
        assertNotNull(transaction.getCreatedAt());
    }

    @Test
    void walletTransactionSettersAndGetters() {

        Wallet sender = new Wallet();
        Wallet receiver = new Wallet();

        WalletTransaction transaction = new WalletTransaction();

        transaction.setFromWallet(sender);
        transaction.setToWallet(receiver);
        transaction.setType(TransactionType.ADD);
        transaction.setStatus(TransactionStatus.FAILED);
        transaction.setAmount(new BigDecimal("100.00"));
        transaction.setIdempotencyKey("add-key-123");

        assertEquals(sender, transaction.getFromWallet());
        assertEquals(receiver, transaction.getToWallet());
        assertEquals(TransactionType.ADD, transaction.getType());
        assertEquals(TransactionStatus.FAILED, transaction.getStatus());
        assertEquals(new BigDecimal("100.00"), transaction.getAmount());
        assertEquals("add-key-123", transaction.getIdempotencyKey());
    }

    @Test
    void onCreateSetsCreatedAtWhenNull() {

        WalletTransaction transaction = new WalletTransaction();

        ReflectionTestUtils.setField(transaction, "createdAt", null);

        transaction.onCreate();

        assertNotNull(transaction.getCreatedAt());
    }

    @Test
    void onCreateDoesNotOverwriteExistingCreatedAt() {

        WalletTransaction transaction = new WalletTransaction();

        LocalDateTime existingTime =
                LocalDateTime.of(2026, 1, 1, 10, 30);

        ReflectionTestUtils.setField(
                transaction, "createdAt", existingTime
        );

        transaction.onCreate();

        assertEquals(existingTime, transaction.getCreatedAt());
    }
}
