package com.techpulse.wallet.service;

import com.techpulse.wallet.dto.response.WalletResponse;
import com.techpulse.wallet.dto.response.TransactionResponse;
import com.techpulse.wallet.entity.User;
import com.techpulse.wallet.entity.Wallet;
import com.techpulse.wallet.entity.WalletTransaction;
import com.techpulse.wallet.enums.Role;
import com.techpulse.wallet.enums.TransactionType;
import com.techpulse.wallet.enums.TransactionStatus;
import com.techpulse.wallet.repository.UserRepository;
import com.techpulse.wallet.repository.WalletRepository;
import com.techpulse.wallet.repository.WalletTransactionRepository;
import com.techpulse.wallet.service.Impl.WalletServiceImpl;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.test.util.ReflectionTestUtils;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private WalletTransactionRepository transactionRepository;

    @InjectMocks
    private WalletServiceImpl walletService;


    @Test
    void getMyWalletSuccessfully() {

        User user = mock(User.class);
        Wallet wallet = mock(Wallet.class);

        when(user.getId()).thenReturn(1L);
        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));

        when(walletRepository.findByUserId(1L))
                .thenReturn(Optional.of(wallet));

        when(wallet.getId()).thenReturn(10L);
        when(wallet.getBalance()).thenReturn(new BigDecimal("500.00"));
        when(wallet.getVersion()).thenReturn(0L);

        WalletResponse response =
                walletService.getMyWallet("test@example.com");

        assertNotNull(response);
        assertEquals(10L, response.walletId());
        assertEquals(0, response.balance().compareTo(new BigDecimal("500.00")));

        verify(userRepository).findByEmail("test@example.com");
        verify(walletRepository).findByUserId(1L);
    }

    @Test
    void getMyWalletWhenUserNotFound() {

        when(userRepository.findByEmail("wrong@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> walletService.getMyWallet("wrong@example.com")
        );

        verify(walletRepository, never()).findByUserId(anyLong());
    }



    @Test
    void addMoneySuccessfully() {

        User user = mock(User.class);
        Wallet wallet = mock(Wallet.class);

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));

        when(user.getId()).thenReturn(1L);

        when(walletRepository.findByUserIdForUpdate(1L))
                .thenReturn(Optional.of(wallet));

        when(wallet.getBalance())
                .thenReturn(new BigDecimal("500.00"));

        when(transactionRepository.findByIdempotencyKey("key-123"))
                .thenReturn(Optional.empty());

        when(transactionRepository.save(any(WalletTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TransactionResponse response = walletService.addMoney(
                "test@example.com",
                new BigDecimal("100.00"),
                "key-123"
        );

        assertNotNull(response);
        assertEquals("ADD", response.type());
        assertEquals("SUCCESS", response.status());
        assertEquals(0, response.amount().compareTo(new BigDecimal("100.00")));

        verify(wallet).setBalance(new BigDecimal("600.00"));
        verify(walletRepository).save(wallet);
        verify(transactionRepository).save(any(WalletTransaction.class));
    }
    @Test
    void addMoneyWithInvalidAmount() {

        assertThrows(
                IllegalArgumentException.class,
                () -> walletService.addMoney(
                        "test@example.com",
                        new BigDecimal("0.00"),
                        "key-456"
                )
        );

        verify(userRepository, never()).findByEmail(anyString());
        verify(transactionRepository, never()).save(any(WalletTransaction.class));
    }


    @Test
    void transferMoneySuccessfully() {

        // Create sender and receiver
        User sender = mock(User.class);
        User receiver = mock(User.class);

        Wallet senderWallet = mock(Wallet.class);
        Wallet receiverWallet = mock(Wallet.class);

        // Sender details
        when(sender.getId()).thenReturn(1L);
//        when(sender.getEmail()).thenReturn("sender@example.com");

        // Receiver details
        when(receiver.getId()).thenReturn(2L);

        // Find sender and receiver
        when(userRepository.findByEmail("sender@example.com"))
                .thenReturn(Optional.of(sender));

        when(userRepository.findById(2L))
                .thenReturn(Optional.of(receiver));

        // Lock wallets in ascending user ID order
        when(walletRepository.findByUserIdForUpdate(1L))
                .thenReturn(Optional.of(senderWallet));

        when(walletRepository.findByUserIdForUpdate(2L))
                .thenReturn(Optional.of(receiverWallet));

        // Initial balances
        when(senderWallet.getBalance())
                .thenReturn(new BigDecimal("1000.00"));

        when(receiverWallet.getBalance())
                .thenReturn(new BigDecimal("500.00"));

        // No existing transaction with this key
        when(transactionRepository.findByIdempotencyKey("transfer-123"))
                .thenReturn(Optional.empty());

        // Save transaction
        when(transactionRepository.save(any(WalletTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Call service method
        TransactionResponse response = walletService.transferMoney(
                "sender@example.com",
                2L,
                new BigDecimal("200.00"),
                "transfer-123"
        );

        // Check response
        assertNotNull(response);
        assertEquals("TRANSFER", response.type());
        assertEquals("SUCCESS", response.status());
        assertEquals(new BigDecimal("200.00"), response.amount());

        // Check wallet balance updates
        verify(senderWallet).setBalance(new BigDecimal("800.00"));
        verify(receiverWallet).setBalance(new BigDecimal("700.00"));

        // Check both wallets saved
        verify(walletRepository).save(senderWallet);
        verify(walletRepository).save(receiverWallet);

        // Check transaction saved
        verify(transactionRepository).save(any(WalletTransaction.class));
    }


    @Test
    void transferMoneyWithInsufficientBalance() {

        User sender = mock(User.class);
        User receiver = mock(User.class);

        Wallet senderWallet = mock(Wallet.class);
        Wallet receiverWallet = mock(Wallet.class);

        when(sender.getId()).thenReturn(1L);
        when(receiver.getId()).thenReturn(2L);

        when(userRepository.findByEmail("sender@example.com"))
                .thenReturn(Optional.of(sender));

        when(userRepository.findById(2L))
                .thenReturn(Optional.of(receiver));

        when(transactionRepository.findByIdempotencyKey("transfer-456"))
                .thenReturn(Optional.empty());

        when(walletRepository.findByUserIdForUpdate(1L))
                .thenReturn(Optional.of(senderWallet));

        when(walletRepository.findByUserIdForUpdate(2L))
                .thenReturn(Optional.of(receiverWallet));

        when(senderWallet.getBalance())
                .thenReturn(new BigDecimal("100.00"));

        assertThrows(IllegalArgumentException.class, () ->
                walletService.transferMoney(
                        "sender@example.com",
                        2L,
                        new BigDecimal("500.00"),
                        "transfer-456"
                )
        );

        verify(walletRepository, never()).save(any(Wallet.class));

        verify(transactionRepository, never())
                .save(any(WalletTransaction.class));
    }


    @Test
    void transferMoneyToSameUser() {

        User sender = mock(User.class);

        when(sender.getId()).thenReturn(1L);

        when(userRepository.findByEmail("sender@example.com"))
                .thenReturn(Optional.of(sender));

        assertThrows(IllegalArgumentException.class, () ->
                walletService.transferMoney(
                        "sender@example.com",
                        1L,
                        new BigDecimal("100.00"),
                        "transfer-789"
                )
        );

        verify(userRepository, never()).findById(anyLong());

        verify(walletRepository, never())
                .findByUserIdForUpdate(anyLong());
    }

    @Test
    void transferMoneyWithInvalidAmount() {

        assertThrows(IllegalArgumentException.class, () ->
                walletService.transferMoney(
                        "sender@example.com",
                        2L,
                        BigDecimal.ZERO,
                        "transfer-invalid"
                )
        );

        verify(transactionRepository, never())
                .findByIdempotencyKey(anyString());

        verify(userRepository, never())
                .findByEmail(anyString());
    }


    @Test
    void addMoneyWithSameIdempotencyKey() {

        User user = mock(User.class);
        Wallet wallet = mock(Wallet.class);

        when(user.getId()).thenReturn(1L);
        when(user.getEmail()).thenReturn("test@example.com");
        when(wallet.getUser()).thenReturn(user);

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));

        // Updated locking method
        when(walletRepository.findByUserIdForUpdate(1L))
                .thenReturn(Optional.of(wallet));

        when(wallet.getBalance())
                .thenReturn(new BigDecimal("500.00"));

        WalletTransaction existingTransaction =
                new WalletTransaction(
                        null,
                        wallet,
                        TransactionType.ADD,
                        TransactionStatus.SUCCESS,
                        new BigDecimal("100.00"),
                        "same-key-123"
                );

        when(transactionRepository.findByIdempotencyKey("same-key-123"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(existingTransaction));

        when(transactionRepository.save(any(WalletTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TransactionResponse firstResponse = walletService.addMoney(
                "test@example.com",
                new BigDecimal("100.00"),
                "same-key-123"
        );

        TransactionResponse secondResponse = walletService.addMoney(
                "test@example.com",
                new BigDecimal("100.00"),
                "same-key-123"
        );

        assertNotNull(firstResponse);
        assertNotNull(secondResponse);

        assertEquals(firstResponse.type(), secondResponse.type());
        assertEquals(firstResponse.status(), secondResponse.status());
        assertEquals(firstResponse.amount(), secondResponse.amount());
        assertEquals(firstResponse.idempotencyKey(),
                secondResponse.idempotencyKey());

        verify(wallet, times(1))
                .setBalance(new BigDecimal("600.00"));

        verify(transactionRepository, times(1))
                .save(any(WalletTransaction.class));
    }



    @Test
    void addMoneyWithDifferentIdempotencyKeys() {

        // Arrange
        User user = new User(
                "test@example.com",
                "password123",
                Role.USER
        );
        ReflectionTestUtils.setField(user, "id", 1L);

        // Create wallet
        Wallet wallet = new Wallet();
        wallet.setUser(user);
        wallet.setBalance(new BigDecimal("500.00"));

        // Mock user lookup
        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));

        // Mock wallet locking
        when(walletRepository.findByUserIdForUpdate(1L))
                .thenReturn(Optional.of(wallet));

        // Both idempotency keys are new
        when(transactionRepository.findByIdempotencyKey(anyString()))
                .thenReturn(Optional.empty());

        // Return saved transaction
        when(transactionRepository.save(any(WalletTransaction.class)))
                .thenAnswer(invocation -> {
                    WalletTransaction transaction = invocation.getArgument(0);
                    ReflectionTestUtils.setField(transaction, "id", 1L);
                    return transaction;
                });

        // First request: Add 100
        TransactionResponse firstResponse = walletService.addMoney(
                "test@example.com",
                new BigDecimal("100.00"),
                "key-001"
        );

        // Second request: Add 50 with a different key
        TransactionResponse secondResponse = walletService.addMoney(
                "test@example.com",
                new BigDecimal("50.00"),
                "key-002"
        );

        // Assert responses
        assertNotNull(firstResponse);
        assertNotNull(secondResponse);

        assertEquals("key-001", firstResponse.idempotencyKey());
        assertEquals("key-002", secondResponse.idempotencyKey());

        assertEquals(0, firstResponse.amount()
                .compareTo(new BigDecimal("100.00")));

        assertEquals(0, secondResponse.amount()
                .compareTo(new BigDecimal("50.00")));

        // Final balance should be 650
        assertEquals(
                0,
                wallet.getBalance().compareTo(new BigDecimal("650.00"))
        );

        // Verify two transactions were created
        verify(transactionRepository, times(2))
                .save(any(WalletTransaction.class));

        // Verify wallet was saved twice
        verify(walletRepository, times(2))
                .save(wallet);

        // Verify both keys were checked
        verify(transactionRepository, times(2))
                .findByIdempotencyKey(anyString());
    }


    @Test
    void addMoneyWithInvalidIdempotencyKey() {

        assertThrows(
                IllegalArgumentException.class,
                () -> walletService.addMoney(
                        "test@example.com",
                        new BigDecimal("100.00"),
                        " "
                )
        );

        verify(transactionRepository, never())
                .findByIdempotencyKey(anyString());

        verify(userRepository, never())
                .findByEmail(anyString());

        verify(walletRepository, never())
                .save(any(Wallet.class));
    }

    @Test
    void getMyTransactionsSuccessfully() {

        // Arrange
        WalletTransaction transaction1 = mock(WalletTransaction.class);
        WalletTransaction transaction2 = mock(WalletTransaction.class);

        LocalDateTime date1 = LocalDateTime.of(2026, 10, 20, 10, 0);
        LocalDateTime date2 = LocalDateTime.of(2026, 10, 19, 10, 0);

        when(transaction1.getId()).thenReturn(1L);
        when(transaction1.getType()).thenReturn(TransactionType.ADD);
        when(transaction1.getStatus()).thenReturn(TransactionStatus.SUCCESS);
        when(transaction1.getAmount()).thenReturn(new BigDecimal("100.00"));
        when(transaction1.getIdempotencyKey()).thenReturn("key-001");
        when(transaction1.getCreatedAt()).thenReturn(date1);

        when(transaction2.getId()).thenReturn(2L);
        when(transaction2.getType()).thenReturn(TransactionType.TRANSFER);
        when(transaction2.getStatus()).thenReturn(TransactionStatus.SUCCESS);
        when(transaction2.getAmount()).thenReturn(new BigDecimal("50.00"));
        when(transaction2.getIdempotencyKey()).thenReturn("key-002");
        when(transaction2.getCreatedAt()).thenReturn(date2);

        Page<WalletTransaction> transactionPage =
                new PageImpl<>(List.of(transaction1, transaction2));

        when(transactionRepository
                .findByFromWalletUserEmailOrToWalletUserEmail(
                        eq("test@example.com"),
                        eq("test@example.com"),
                        any(Pageable.class)
                ))
                .thenReturn(transactionPage);

        // Act
        Page<TransactionResponse> response =
                walletService.getMyTransactions("test@example.com", 0, 10);

        // Assert
        assertNotNull(response);
        assertEquals(2, response.getTotalElements());

        assertEquals(1L, response.getContent().get(0).transactionId());
        assertEquals("ADD", response.getContent().get(0).type());
        assertEquals(new BigDecimal("100.00"),
                response.getContent().get(0).amount());
        assertEquals("key-001",
                response.getContent().get(0).idempotencyKey());

        assertEquals(2L, response.getContent().get(1).transactionId());
        assertEquals("TRANSFER", response.getContent().get(1).type());
        assertEquals(new BigDecimal("50.00"),
                response.getContent().get(1).amount());
        assertEquals("key-002",
                response.getContent().get(1).idempotencyKey());

        verify(transactionRepository, times(1))
                .findByFromWalletUserEmailOrToWalletUserEmail(
                        eq("test@example.com"),
                        eq("test@example.com"),
                        any(Pageable.class)
                );
    }


    @Test
    void getMyTransactionsWithInvalidPage() {

        assertThrows(
                IllegalArgumentException.class,
                () -> walletService.getMyTransactions(
                        "test@example.com",
                        -1,
                        10
                )
        );

        verify(transactionRepository, never())
                .findByFromWalletUserEmailOrToWalletUserEmail(
                        anyString(),
                        anyString(),
                        any(Pageable.class)
                );
    }


    @Test
    void getMyTransactionsWithInvalidSize() {

        assertThrows(
                IllegalArgumentException.class,
                () -> walletService.getMyTransactions(
                        "test@example.com",
                        0,
                        101
                )
        );

        verify(transactionRepository, never())
                .findByFromWalletUserEmailOrToWalletUserEmail(
                        anyString(),
                        anyString(),
                        any(Pageable.class)
                );
    }



    @Test
    void transferMoneyWithSameIdempotencyKey() {

        // 1. Create sender
        User sender = mock(User.class);
        when(sender.getId()).thenReturn(1L);
        when(sender.getEmail()).thenReturn("sender@example.com");

        // 2. Create receiver
        User receiver = mock(User.class);
        when(receiver.getId()).thenReturn(2L);

        // 3. Create wallets
        Wallet senderWallet = mock(Wallet.class);
        Wallet receiverWallet = mock(Wallet.class);

        when(senderWallet.getUser()).thenReturn(sender);
        when(receiverWallet.getUser()).thenReturn(receiver);

        // 4. Mock user repository
        when(userRepository.findByEmail("sender@example.com"))
                .thenReturn(Optional.of(sender));

        when(userRepository.findById(2L))
                .thenReturn(Optional.of(receiver));

        // 5. Mock wallet repository
        when(walletRepository.findByUserIdForUpdate(1L))
                .thenReturn(Optional.of(senderWallet));

        when(walletRepository.findByUserIdForUpdate(2L))
                .thenReturn(Optional.of(receiverWallet));

        // 6. Mock existing transaction
        WalletTransaction existingTransaction =
                mock(WalletTransaction.class);

        when(existingTransaction.getId()).thenReturn(101L);

        when(existingTransaction.getType())
                .thenReturn(TransactionType.TRANSFER);

        when(existingTransaction.getStatus())
                .thenReturn(TransactionStatus.SUCCESS);

        when(existingTransaction.getFromWallet())
                .thenReturn(senderWallet);

        when(existingTransaction.getToWallet())
                .thenReturn(receiverWallet);

        when(existingTransaction.getAmount())
                .thenReturn(new BigDecimal("300.00"));

        when(existingTransaction.getIdempotencyKey())
                .thenReturn("transfer-key-001");

        when(existingTransaction.getCreatedAt())
                .thenReturn(LocalDateTime.now());

        when(transactionRepository.findByIdempotencyKey("transfer-key-001"))
                .thenReturn(Optional.of(existingTransaction));

        // 7. First transfer request
        TransactionResponse firstResponse = walletService.transferMoney(
                "sender@example.com",
                2L,
                new BigDecimal("300.00"),
                "transfer-key-001"
        );

        // 8. Duplicate transfer request
        TransactionResponse secondResponse = walletService.transferMoney(
                "sender@example.com",
                2L,
                new BigDecimal("300.00"),
                "transfer-key-001"
        );

        // 9. Assertions
        assertEquals(firstResponse, secondResponse);
        assertEquals(101L, firstResponse.transactionId());
        assertEquals("TRANSFER", firstResponse.type());
        assertEquals("SUCCESS", firstResponse.status());

        // 10. Verify no duplicate transaction or wallet update
        verify(transactionRepository, times(2))
                .findByIdempotencyKey("transfer-key-001");

        verify(walletRepository, never()).save(any(Wallet.class));

        verify(transactionRepository, never())
                .save(any(WalletTransaction.class));
    }

    @Test
    void transferMoneyWithDifferentRequestWithSameIdempotencyKey() {

        // Arrange
        User sender = new User();
        sender.setEmail("sender@example.com");
        ReflectionTestUtils.setField(sender, "id", 1L);

        User receiver = new User();
        receiver.setEmail("receiver@example.com");
        ReflectionTestUtils.setField(receiver, "id", 2L);

        Wallet senderWallet = mock(Wallet.class);
        Wallet receiverWallet = mock(Wallet.class);

        WalletTransaction existingTransaction =
                mock(WalletTransaction.class);

        // Sender and receiver exist
        when(userRepository.findByEmail("sender@example.com"))
                .thenReturn(Optional.of(sender));

        when(userRepository.findById(2L))
                .thenReturn(Optional.of(receiver));

        // Both wallets exist and are locked
        when(walletRepository.findByUserIdForUpdate(1L))
                .thenReturn(Optional.of(senderWallet));

        when(walletRepository.findByUserIdForUpdate(2L))
                .thenReturn(Optional.of(receiverWallet));

        // Existing transaction details
        when(senderWallet.getUser()).thenReturn(sender);
        when(receiverWallet.getUser()).thenReturn(receiver);

        when(existingTransaction.getType())
                .thenReturn(TransactionType.TRANSFER);

        when(existingTransaction.getFromWallet())
                .thenReturn(senderWallet);

        when(existingTransaction.getToWallet())
                .thenReturn(receiverWallet);

        when(existingTransaction.getAmount())
                .thenReturn(new BigDecimal("100.00"));

        when(transactionRepository.findByIdempotencyKey("transfer-key-001"))
                .thenReturn(Optional.of(existingTransaction));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> walletService.transferMoney(
                        "sender@example.com",
                        2L,
                        new BigDecimal("200.00"),
                        "transfer-key-001"
                )
        );

        assertEquals(
                "Idempotency key was already used for a different request",
                exception.getMessage()
        );

        // Verify both wallet locks
        verify(walletRepository).findByUserIdForUpdate(1L);
        verify(walletRepository).findByUserIdForUpdate(2L);

        // Verify idempotency lookup
        verify(transactionRepository)
                .findByIdempotencyKey("transfer-key-001");

        // No wallet or transaction should be saved
        verify(transactionRepository, never())
                .save(any(WalletTransaction.class));

        verify(walletRepository, never())
                .save(any(Wallet.class));
    }


    @Test
    void transferMoneyWithInvalidIdempotencyKey() {

        assertThrows(IllegalArgumentException.class, () ->
                walletService.transferMoney(
                        "sender@example.com",
                        2L,
                        new BigDecimal("100.00"),
                        " " // Invalid blank key
                )
        );

        // No database operation should happen
        verify(transactionRepository, never())
                .findByIdempotencyKey(anyString());

        verify(transactionRepository, never())
                .save(any(WalletTransaction.class));

        verify(walletRepository, never())
                .save(any(Wallet.class));
    }


    @Test
    void transferMoneyWithReceiverNotFound() {

        // Arrange
        User sender = new User();
        sender.setEmail("sender@example.com");
        ReflectionTestUtils.setField(sender, "id", 1L);

        when(userRepository.findByEmail("sender@example.com"))
                .thenReturn(Optional.of(sender));

        // Receiver does not exist
        when(userRepository.findById(2L))
                .thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> walletService.transferMoney(
                        "sender@example.com",
                        2L,
                        new BigDecimal("100.00"),
                        "transfer-key-002"
                )
        );

        assertEquals("Receiver not found", exception.getMessage());

        // Verify sender and receiver lookups
        verify(userRepository).findByEmail("sender@example.com");
        verify(userRepository).findById(2L);

        // Wallet locking should not happen
        verify(walletRepository, never())
                .findByUserIdForUpdate(anyLong());

        // Idempotency lookup should not happen
        verify(transactionRepository, never())
                .findByIdempotencyKey(anyString());

        // No wallet or transaction should be saved
        verify(walletRepository, never())
                .save(any(Wallet.class));

        verify(transactionRepository, never())
                .save(any(WalletTransaction.class));
    }


    @Test
    void transferMoneyWithSenderNotFound() {

        // Arrange
        when(userRepository.findByEmail("unknown@example.com"))
                .thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> walletService.transferMoney(
                        "unknown@example.com",
                        2L,
                        new BigDecimal("100.00"),
                        "transfer-key-003"
                )
        );

        assertEquals("Sender not found", exception.getMessage());

        // Verify sender lookup
        verify(userRepository).findByEmail("unknown@example.com");

        // Receiver lookup should not happen
        verify(userRepository, never())
                .findById(anyLong());

        // Wallet lookup should not happen
        verify(walletRepository, never())
                .findByUserIdForUpdate(anyLong());

        // Idempotency lookup should not happen
        verify(transactionRepository, never())
                .findByIdempotencyKey(anyString());

        // No transaction should be saved
        verify(transactionRepository, never())
                .save(any(WalletTransaction.class));

        verify(walletRepository, never())
                .save(any(Wallet.class));
    }

    @Test
    void transferMoneyWithWalletNotFound() {

        // Arrange
        User sender = new User();
        sender.setEmail("sender@example.com");
        ReflectionTestUtils.setField(sender, "id", 1L);

        User receiver = new User();
        receiver.setEmail("receiver@example.com");
        ReflectionTestUtils.setField(receiver, "id", 2L);

        // Sender exists
        when(userRepository.findByEmail("sender@example.com"))
                .thenReturn(Optional.of(sender));

        // Receiver exists
        when(userRepository.findById(2L))
                .thenReturn(Optional.of(receiver));

        // First wallet exists
        Wallet firstWallet = mock(Wallet.class);

        when(walletRepository.findByUserIdForUpdate(1L))
                .thenReturn(Optional.of(firstWallet));

        // Second wallet does not exist
        when(walletRepository.findByUserIdForUpdate(2L))
                .thenReturn(Optional.empty());

        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> walletService.transferMoney(
                        "sender@example.com",
                        2L,
                        new BigDecimal("100.00"),
                        "transfer-key-004"
                )
        );

        // Assert
        assertEquals("Wallet not found", exception.getMessage());

        // Verify both wallet lookups
        verify(walletRepository).findByUserIdForUpdate(1L);
        verify(walletRepository).findByUserIdForUpdate(2L);

        // Idempotency lookup is not reached
        verify(transactionRepository, never())
                .findByIdempotencyKey(anyString());

        // No wallet or transaction should be saved
        verify(walletRepository, never())
                .save(any(Wallet.class));

        verify(transactionRepository, never())
                .save(any(WalletTransaction.class));
    }
    @Test
    void transferMoneyWithAmountHavingMoreThanTwoDecimalPlaces() {

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> walletService.transferMoney(
                        "sender@example.com",
                        2L,
                        new BigDecimal("100.123"),
                        "transfer-key-005"
                )
        );

        assertEquals(
                "Amount must be positive with at most two decimal places",
                exception.getMessage()
        );

        // Verify no database operation happens
        verify(transactionRepository, never())
                .findByIdempotencyKey(anyString());

        verify(userRepository, never())
                .findByEmail(anyString());

        verify(walletRepository, never())
                .save(any(Wallet.class));
    }


    @Test
    void transferMoneyWithZeroAmount() {

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> walletService.transferMoney(
                        "sender@example.com",
                        2L,
                        BigDecimal.ZERO,
                        "transfer-key-006"
                )
        );

        assertEquals(
                "Amount must be positive with at most two decimal places",
                exception.getMessage()
        );

        // Verify no database calls happen
        verify(transactionRepository, never())
                .findByIdempotencyKey(anyString());

        verify(userRepository, never())
                .findByEmail(anyString());

        verify(walletRepository, never())
                .save(any(Wallet.class));
    }


    @Test
    void transferMoneyWithNegativeAmount() {

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> walletService.transferMoney(
                        "sender@example.com",
                        2L,
                        new BigDecimal("-100.00"),
                        "transfer-key-007"
                )
        );

        assertEquals(
                "Amount must be positive with at most two decimal places",
                exception.getMessage()
        );

        // Verify no database calls happen
        verify(transactionRepository, never())
                .findByIdempotencyKey(anyString());

        verify(userRepository, never())
                .findByEmail(anyString());

        verify(walletRepository, never())
                .save(any(Wallet.class));
    }

    @Test
    void transferMoneyWithNullAmount() {

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> walletService.transferMoney(
                        "sender@example.com",
                        2L,
                        null,
                        "transfer-key-008"
                )
        );

        assertEquals(
                "Amount must be positive with at most two decimal places",
                exception.getMessage()
        );

        // Verify no database calls happen
        verify(transactionRepository, never())
                .findByIdempotencyKey(anyString());

        verify(userRepository, never())
                .findByEmail(anyString());

        verify(walletRepository, never())
                .save(any(Wallet.class));
    }

    @Test
    void transferMoneyWithNullIdempotencyKey() {

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> walletService.transferMoney(
                        "sender@example.com",
                        2L,
                        new BigDecimal("100.00"),
                        null
                )
        );

        assertEquals(
                "A valid Idempotency-Key header is required",
                exception.getMessage()
        );

        verify(transactionRepository, never())
                .findByIdempotencyKey(anyString());

        verify(userRepository, never())
                .findByEmail(anyString());

        verify(walletRepository, never())
                .save(any(Wallet.class));
    }

    @Test
    void transferMoneyWithLongIdempotencyKey() {

        String longKey = "A".repeat(101);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> walletService.transferMoney(
                        "sender@example.com",
                        2L,
                        new BigDecimal("100.00"),
                        longKey
                )
        );

        assertEquals(
                "A valid Idempotency-Key header is required",
                exception.getMessage()
        );

        verify(transactionRepository, never())
                .findByIdempotencyKey(anyString());

        verify(userRepository, never())
                .findByEmail(anyString());

        verify(walletRepository, never())
                .save(any(Wallet.class));
    }


    @Test
    void transferMoneyWithDifferentReceiverSameKey() {

        User sender = mock(User.class);
        when(sender.getId()).thenReturn(1L);
        when(sender.getEmail()).thenReturn("sender@example.com");

        User requestedReceiver = mock(User.class);
        when(requestedReceiver.getId()).thenReturn(3L);

        User originalReceiver = mock(User.class);
        when(originalReceiver.getId()).thenReturn(2L);

        Wallet senderWallet = mock(Wallet.class);
        Wallet requestedReceiverWallet = mock(Wallet.class);
        Wallet originalReceiverWallet = mock(Wallet.class);

        when(senderWallet.getUser()).thenReturn(sender);
        when(originalReceiverWallet.getUser()).thenReturn(originalReceiver);

        when(userRepository.findByEmail("sender@example.com"))
                .thenReturn(Optional.of(sender));

        when(userRepository.findById(3L))
                .thenReturn(Optional.of(requestedReceiver));

        when(walletRepository.findByUserIdForUpdate(1L))
                .thenReturn(Optional.of(senderWallet));

        when(walletRepository.findByUserIdForUpdate(3L))
                .thenReturn(Optional.of(requestedReceiverWallet));

        WalletTransaction existingTransaction = mock(WalletTransaction.class);

        when(existingTransaction.getType())
                .thenReturn(TransactionType.TRANSFER);

        when(existingTransaction.getFromWallet())
                .thenReturn(senderWallet);

        when(existingTransaction.getToWallet())
                .thenReturn(originalReceiverWallet);

        when(transactionRepository.findByIdempotencyKey("transfer-key-004"))
                .thenReturn(Optional.of(existingTransaction));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> walletService.transferMoney(
                        "sender@example.com",
                        3L,
                        new BigDecimal("100.00"),
                        "transfer-key-004"
                )
        );

        assertEquals(
                "Idempotency key was already used for a different request",
                exception.getMessage()
        );

        verify(walletRepository, never()).save(any(Wallet.class));

        verify(transactionRepository, never())
                .save(any(WalletTransaction.class));
    }

    @Test
    void getMyTransactionsWithInvalidPageSize() {

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> walletService.getMyTransactions(
                        "sender@example.com",
                        0,
                        101
                )
        );

        assertEquals(
                "Page size must be between 1 and 100",
                exception.getMessage()
        );

        verify(userRepository, never()).findByEmail(anyString());

        verify(transactionRepository, never())
                .findByFromWalletUserEmailOrToWalletUserEmail(
                        anyString(),
                        anyString(),
                        any(Pageable.class)
                );
    }


    @Test
    void getMyWalletWhenWalletNotFound() {

        User user = new User();
        user.setEmail("sender@example.com");

        ReflectionTestUtils.setField(user, "id", 1L);

        when(userRepository.findByEmail("sender@example.com"))
                .thenReturn(Optional.of(user));

        when(walletRepository.findByUserId(1L))
                .thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> walletService.getMyWallet("sender@example.com")
        );

        assertEquals("Wallet not found", exception.getMessage());

        verify(userRepository).findByEmail("sender@example.com");
        verify(walletRepository).findByUserId(1L);
    }


    @Test
    void addMoneyWhenWalletNotFound() {

        User user = new User();
        user.setEmail("sender@example.com");
        ReflectionTestUtils.setField(user, "id", 1L);

        // User exists
        when(userRepository.findByEmail("sender@example.com"))
                .thenReturn(Optional.of(user));

        // Wallet does not exist
        when(walletRepository.findByUserIdForUpdate(1L))
                .thenReturn(Optional.empty());

        // Execute test
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> walletService.addMoney(
                        "sender@example.com",
                        new BigDecimal("100.00"),
                        "add-key-010"
                )
        );

        assertEquals("Wallet not found", exception.getMessage());

        // Verify wallet lookup happened
        verify(walletRepository).findByUserIdForUpdate(1L);

        // No transaction or wallet should be saved
        verify(transactionRepository, never())
                .save(any(WalletTransaction.class));

        verify(walletRepository, never())
                .save(any(Wallet.class));

        // Idempotency lookup is not reached
        verify(transactionRepository, never())
                .findByIdempotencyKey(anyString());
    }

    @Test
    void addMoneyWithNullIdempotencyKey() {

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> walletService.addMoney(
                        "sender@example.com",
                        new BigDecimal("100.00"),
                        null
                )
        );

        assertEquals(
                "A valid Idempotency-Key header is required",
                exception.getMessage()
        );

        verify(transactionRepository, never())
                .findByIdempotencyKey(anyString());

        verify(userRepository, never())
                .findByEmail(anyString());

        verify(walletRepository, never())
                .save(any(Wallet.class));
    }

    @Test
    void addMoneyWithLongIdempotencyKey() {

        String longKey = "A".repeat(101);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> walletService.addMoney(
                        "sender@example.com",
                        new BigDecimal("100.00"),
                        longKey
                )
        );

        assertEquals(
                "A valid Idempotency-Key header is required",
                exception.getMessage()
        );

        verify(transactionRepository, never())
                .findByIdempotencyKey(anyString());

        verify(userRepository, never())
                .findByEmail(anyString());

        verify(walletRepository, never())
                .save(any(Wallet.class));
    }

    @Test
    void addMoneyWithInvalidAmountScale() {

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> walletService.addMoney(
                        "sender@example.com",
                        new BigDecimal("100.123"),
                        "add-key-011"
                )
        );

        assertEquals(
                "Amount must be positive with at most two decimal places",
                exception.getMessage()
        );

        verify(transactionRepository, never())
                .findByIdempotencyKey(anyString());

        verify(userRepository, never())
                .findByEmail(anyString());

        verify(walletRepository, never())
                .save(any(Wallet.class));
    }

    @Test
    void addMoneyWithZeroAmount() {

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> walletService.addMoney(
                        "sender@example.com",
                        BigDecimal.ZERO,
                        "add-key-012"
                )
        );

        assertEquals(
                "Amount must be positive with at most two decimal places",
                exception.getMessage()
        );

        verify(transactionRepository, never())
                .findByIdempotencyKey(anyString());

        verify(userRepository, never())
                .findByEmail(anyString());

        verify(walletRepository, never())
                .save(any(Wallet.class));
    }

    @Test
    void addMoneyWithNegativeAmount() {

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> walletService.addMoney(
                        "sender@example.com",
                        new BigDecimal("-100.00"),
                        "add-key-013"
                )
        );

        assertEquals(
                "Amount must be positive with at most two decimal places",
                exception.getMessage()
        );

        verify(transactionRepository, never())
                .findByIdempotencyKey(anyString());

        verify(userRepository, never())
                .findByEmail(anyString());

        verify(walletRepository, never())
                .save(any(Wallet.class));
    }

    @Test
    void addMoneyWithNullAmount() {

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> walletService.addMoney(
                        "sender@example.com",
                        null,
                        "add-key-014"
                )
        );

        assertEquals(
                "Amount must be positive with at most two decimal places",
                exception.getMessage()
        );

        verify(transactionRepository, never())
                .findByIdempotencyKey(anyString());

        verify(userRepository, never())
                .findByEmail(anyString());

        verify(walletRepository, never())
                .save(any(Wallet.class));
    }


    @Test
    void addMoneyWithDifferentAmountSameKey() {

        // Requesting user
        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);
        when(user.getEmail()).thenReturn("sender@example.com");

        // Requesting user's wallet
        Wallet wallet = mock(Wallet.class);
        when(wallet.getUser()).thenReturn(user);

        when(userRepository.findByEmail("sender@example.com"))
                .thenReturn(Optional.of(user));

        when(walletRepository.findByUserIdForUpdate(1L))
                .thenReturn(Optional.of(wallet));

        // Existing transaction with the same key and amount 100
        WalletTransaction existingTransaction = new WalletTransaction(
                null,
                wallet,
                TransactionType.ADD,
                TransactionStatus.SUCCESS,
                new BigDecimal("100.00"),
                "add-key-015"
        );

        when(transactionRepository.findByIdempotencyKey("add-key-015"))
                .thenReturn(Optional.of(existingTransaction));

        // Same key, but requested amount is 200
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> walletService.addMoney(
                        "sender@example.com",
                        new BigDecimal("200.00"),
                        "add-key-015"
                )
        );

        assertEquals(
                "Idempotency key was already used for a different request",
                exception.getMessage()
        );

        verify(wallet, never()).setBalance(any(BigDecimal.class));
        verify(walletRepository, never()).save(any(Wallet.class));
        verify(transactionRepository, never()).save(any(WalletTransaction.class));
    }

    @Test
    void addMoneyWithDifferentUserSameKey() {

        // Original user
        User originalUser = mock(User.class);
        when(originalUser.getEmail()).thenReturn("original@example.com");

        Wallet originalWallet = mock(Wallet.class);
        when(originalWallet.getUser()).thenReturn(originalUser);

        WalletTransaction existingTransaction = new WalletTransaction(
                null,
                originalWallet,
                TransactionType.ADD,
                TransactionStatus.SUCCESS,
                new BigDecimal("100.00"),
                "add-key-016"
        );

        // Different requesting user
        User requestingUser = mock(User.class);
        when(requestingUser.getId()).thenReturn(2L);

        when(userRepository.findByEmail("another@example.com"))
                .thenReturn(Optional.of(requestingUser));

        Wallet requestingWallet = mock(Wallet.class);

        when(walletRepository.findByUserIdForUpdate(2L))
                .thenReturn(Optional.of(requestingWallet));

        when(transactionRepository.findByIdempotencyKey("add-key-016"))
                .thenReturn(Optional.of(existingTransaction));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> walletService.addMoney(
                        "another@example.com",
                        new BigDecimal("100.00"),
                        "add-key-016"
                )
        );

        assertEquals(
                "Idempotency key was already used for a different request",
                exception.getMessage()
        );

        verify(walletRepository, never()).save(any(Wallet.class));
        verify(transactionRepository, never()).save(any(WalletTransaction.class));
    }

    @Test
    void transferMoneyWithDifferentAmountSameKey() {

        User sender = mock(User.class);
        when(sender.getId()).thenReturn(1L);
        when(sender.getEmail()).thenReturn("sender@example.com");

        User receiver = mock(User.class);
        when(receiver.getId()).thenReturn(2L);

        Wallet senderWallet = mock(Wallet.class);
        Wallet receiverWallet = mock(Wallet.class);

        when(senderWallet.getUser()).thenReturn(sender);
        when(receiverWallet.getUser()).thenReturn(receiver);

        when(userRepository.findByEmail("sender@example.com"))
                .thenReturn(Optional.of(sender));

        when(userRepository.findById(2L))
                .thenReturn(Optional.of(receiver));

        when(walletRepository.findByUserIdForUpdate(1L))
                .thenReturn(Optional.of(senderWallet));

        when(walletRepository.findByUserIdForUpdate(2L))
                .thenReturn(Optional.of(receiverWallet));

        WalletTransaction existingTransaction = mock(WalletTransaction.class);

        when(existingTransaction.getType())
                .thenReturn(TransactionType.TRANSFER);
        when(existingTransaction.getFromWallet())
                .thenReturn(senderWallet);
        when(existingTransaction.getToWallet())
                .thenReturn(receiverWallet);
        when(existingTransaction.getAmount())
                .thenReturn(new BigDecimal("100.00"));

        when(transactionRepository.findByIdempotencyKey("transfer-key-002"))
                .thenReturn(Optional.of(existingTransaction));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> walletService.transferMoney(
                        "sender@example.com",
                        2L,
                        new BigDecimal("200.00"),
                        "transfer-key-002"
                )
        );

        assertEquals(
                "Idempotency key was already used for a different request",
                exception.getMessage()
        );

        verify(walletRepository, never()).save(any(Wallet.class));
        verify(transactionRepository, never()).save(any(WalletTransaction.class));
    }
    @Test
    void transferMoneyWithDifferentSenderSameKey() {

        User requestedSender = mock(User.class);
        when(requestedSender.getId()).thenReturn(3L);

        User receiver = mock(User.class);
        when(receiver.getId()).thenReturn(2L);

        User originalSender = mock(User.class);
        when(originalSender.getEmail()).thenReturn("original@example.com");

        Wallet firstWallet = mock(Wallet.class);
        Wallet secondWallet = mock(Wallet.class);
        Wallet originalSenderWallet = mock(Wallet.class);

        when(originalSenderWallet.getUser()).thenReturn(originalSender);

        when(userRepository.findByEmail("another@example.com"))
                .thenReturn(Optional.of(requestedSender));

        when(userRepository.findById(2L))
                .thenReturn(Optional.of(receiver));

        when(walletRepository.findByUserIdForUpdate(2L))
                .thenReturn(Optional.of(firstWallet));

        when(walletRepository.findByUserIdForUpdate(3L))
                .thenReturn(Optional.of(secondWallet));

        WalletTransaction existingTransaction = mock(WalletTransaction.class);

        when(existingTransaction.getType())
                .thenReturn(TransactionType.TRANSFER);

        when(existingTransaction.getFromWallet())
                .thenReturn(originalSenderWallet);

        when(transactionRepository.findByIdempotencyKey("transfer-key-003"))
                .thenReturn(Optional.of(existingTransaction));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> walletService.transferMoney(
                        "another@example.com",
                        2L,
                        new BigDecimal("100.00"),
                        "transfer-key-003"
                )
        );

        assertEquals(
                "Idempotency key was already used for a different request",
                exception.getMessage()
        );

        verify(walletRepository, never()).save(any(Wallet.class));
        verify(transactionRepository, never()).save(any(WalletTransaction.class));
    }


    @Test
    void transferMoneyWithDifferentRequestSameKey() {

        User sender = mock(User.class);
        when(sender.getId()).thenReturn(1L);

        User receiver = mock(User.class);
        when(receiver.getId()).thenReturn(2L);

        Wallet senderWallet = mock(Wallet.class);
        Wallet receiverWallet = mock(Wallet.class);

        when(userRepository.findByEmail("sender@example.com"))
                .thenReturn(Optional.of(sender));

        when(userRepository.findById(2L))
                .thenReturn(Optional.of(receiver));

        when(walletRepository.findByUserIdForUpdate(1L))
                .thenReturn(Optional.of(senderWallet));

        when(walletRepository.findByUserIdForUpdate(2L))
                .thenReturn(Optional.of(receiverWallet));

        WalletTransaction existingTransaction = mock(WalletTransaction.class);

        when(existingTransaction.getType())
                .thenReturn(TransactionType.ADD);

        when(transactionRepository.findByIdempotencyKey("transfer-key-005"))
                .thenReturn(Optional.of(existingTransaction));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> walletService.transferMoney(
                        "sender@example.com",
                        2L,
                        new BigDecimal("100.00"),
                        "transfer-key-005"
                )
        );

        assertEquals(
                "Idempotency key was already used for a different request",
                exception.getMessage()
        );

        verify(walletRepository, never()).save(any(Wallet.class));

        verify(transactionRepository, never())
                .save(any(WalletTransaction.class));
    }
}