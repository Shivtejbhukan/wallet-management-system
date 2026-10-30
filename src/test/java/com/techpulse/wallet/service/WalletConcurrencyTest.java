
package com.techpulse.wallet.service;

import com.techpulse.wallet.dto.response.TransactionResponse;
import com.techpulse.wallet.entity.User;
import com.techpulse.wallet.entity.Wallet;
import com.techpulse.wallet.enums.Role;
import com.techpulse.wallet.repository.UserRepository;
import com.techpulse.wallet.repository.WalletRepository;
import com.techpulse.wallet.repository.WalletTransactionRepository;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(properties = {
        "jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
class WalletConcurrencyTest {

    @Autowired
    private WalletService walletService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private WalletTransactionRepository transactionRepository;

    @Test
    void shouldAllowOnlyOneTransferWhenTwoTransfersRunAtSameTime()
            throws Exception {

        String uniqueId = UUID.randomUUID().toString();

        // 1. Create three users
        User sender = userRepository.saveAndFlush(
                new User("sender" + uniqueId + "@test.com",
                        "password123", Role.USER));

        User receiver1 = userRepository.saveAndFlush(
                new User("receiver1" + uniqueId + "@test.com",
                        "password123", Role.USER));

        User receiver2 = userRepository.saveAndFlush(
                new User("receiver2" + uniqueId + "@test.com",
                        "password123", Role.USER));

        // 2. Create wallets
        Wallet senderWallet = new Wallet(sender);
        senderWallet.setBalance(new BigDecimal("1000.00"));
        walletRepository.saveAndFlush(senderWallet);

        walletRepository.saveAndFlush(new Wallet(receiver1));
        walletRepository.saveAndFlush(new Wallet(receiver2));

        // 3. Synchronize both requests
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<Throwable> transfer1 = executor.submit(() -> {
                ready.countDown();
                start.await();

                try {
                    walletService.transferMoney(
                            sender.getEmail(),
                            receiver1.getId(),
                            new BigDecimal("700.00"),
                            "key-700-" + uniqueId
                    );
                    return null;
                } catch (Throwable ex) {
                    return ex;
                }
            });

            Future<Throwable> transfer2 = executor.submit(() -> {
                ready.countDown();
                start.await();

                try {
                    walletService.transferMoney(
                            sender.getEmail(),
                            receiver2.getId(),
                            new BigDecimal("600.00"),
                            "key-600-" + uniqueId
                    );
                    return null;
                } catch (Throwable ex) {
                    return ex;
                }
            });

            // Wait until both threads are ready
            assertTrue(ready.await(10, TimeUnit.SECONDS));

            // Start both transfers together
            start.countDown();

            Throwable result1 = transfer1.get(20, TimeUnit.SECONDS);
            Throwable result2 = transfer2.get(20, TimeUnit.SECONDS);

            // 4. Exactly one transfer should succeed
            int successCount = 0;

            if (result1 == null) successCount++;
            if (result2 == null) successCount++;

            assertEquals(1, successCount,
                    "Exactly one transfer should succeed");

            // 5. One transfer must fail
            assertTrue(
                    (result1 == null && result2 != null)
                            || (result1 != null && result2 == null)
            );

            // 6. Check final sender balance
            Wallet updatedSenderWallet =
                    walletRepository.findByUserId(sender.getId())
                            .orElseThrow();

            BigDecimal finalBalance = updatedSenderWallet.getBalance();

            assertTrue(
                    finalBalance.compareTo(BigDecimal.ZERO) >= 0,
                    "Wallet balance must never become negative"
            );

            assertTrue(
                    finalBalance.compareTo(new BigDecimal("300.00")) == 0
                            || finalBalance.compareTo(new BigDecimal("400.00")) == 0,
                    "Final balance should be 300 or 400"
            );

            // 7. Check that only one transfer was recorded
            long transactionCount = transactionRepository.findAll()
                    .stream()
                    .filter(tx -> tx.getIdempotencyKey()
                            .equals("key-700-" + uniqueId)
                            || tx.getIdempotencyKey()
                            .equals("key-600-" + uniqueId))
                    .count();

            assertEquals(1, transactionCount,
                    "Only one transfer transaction should be saved");

        } finally {
            executor.shutdownNow();
        }
    }


    @Test
    void shouldProcessSameAddMoneyRequestOnlyOnce() throws Exception {

        String email = "idempotency-" + UUID.randomUUID() + "@test.com";
        String key = "same-key-" + UUID.randomUUID();

        User user = userRepository.save(
                new User(email, "password", Role.USER)
        );

        Wallet wallet = walletRepository.save(new Wallet(user));

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startSignal = new CountDownLatch(1);

        try {
            Callable<TransactionResponse> request = () -> {
                startSignal.await();

                return walletService.addMoney(
                        email,
                        new BigDecimal("500.00"),
                        key
                );
            };

            Future<TransactionResponse> future1 = executor.submit(request);
            Future<TransactionResponse> future2 = executor.submit(request);

            startSignal.countDown();

            TransactionResponse response1 = future1.get();
            TransactionResponse response2 = future2.get();

            Wallet updatedWallet = walletRepository
                    .findByUserId(user.getId())
                    .orElseThrow();

            assertEquals(
                    new BigDecimal("500.00"),
                    updatedWallet.getBalance()
            );

            assertEquals(response1.transactionId(), response2.transactionId());

            long transactionCount = transactionRepository.findAll()
                    .stream()
                    .filter(t -> key.equals(t.getIdempotencyKey()))
                    .count();

            assertEquals(1, transactionCount);

        } finally {
            executor.shutdownNow();
        }
    }



    @Test
    void shouldProcessSameTransferRequestOnlyOnce() throws Exception {

        String suffix = UUID.randomUUID().toString();

        User sender = userRepository.save(
                new User("sender-" + suffix + "@test.com",
                        "password", Role.USER));

        User receiver = userRepository.save(
                new User("receiver-" + suffix + "@test.com",
                        "password", Role.USER));

        Wallet senderWallet = new Wallet(sender);
        senderWallet.setBalance(new BigDecimal("1000.00"));
        walletRepository.save(senderWallet);

        walletRepository.save(new Wallet(receiver));

        String key = "transfer-key-" + suffix;

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startSignal = new CountDownLatch(1);

        try {
            Callable<TransactionResponse> request = () -> {
                startSignal.await();

                return walletService.transferMoney(
                        sender.getEmail(),
                        receiver.getId(),
                        new BigDecimal("300.00"),
                        key
                );
            };

            Future<TransactionResponse> future1 = executor.submit(request);
            Future<TransactionResponse> future2 = executor.submit(request);

            startSignal.countDown();

            TransactionResponse response1 = future1.get();
            TransactionResponse response2 = future2.get();

            Wallet updatedSender = walletRepository
                    .findByUserId(sender.getId()).orElseThrow();

            Wallet updatedReceiver = walletRepository
                    .findByUserId(receiver.getId()).orElseThrow();

            assertEquals(
                    new BigDecimal("700.00"),
                    updatedSender.getBalance());

            assertEquals(
                    new BigDecimal("300.00"),
                    updatedReceiver.getBalance());

            assertEquals(
                    response1.transactionId(),
                    response2.transactionId());

            long count = transactionRepository.findAll()
                    .stream()
                    .filter(t -> key.equals(t.getIdempotencyKey()))
                    .count();

            assertEquals(1, count);

        } finally {
            executor.shutdownNow();
        }
    }

}
