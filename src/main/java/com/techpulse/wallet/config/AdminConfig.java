package com.techpulse.wallet.config;

import com.techpulse.wallet.entity.User;
import com.techpulse.wallet.entity.Wallet;
import com.techpulse.wallet.enums.Role;
import com.techpulse.wallet.repository.UserRepository;
import com.techpulse.wallet.repository.WalletRepository;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminConfig {

    @Bean
    CommandLineRunner createAdmin(
            UserRepository userRepository,
            WalletRepository walletRepository,
            PasswordEncoder passwordEncoder) {

        return args -> {

            String email = "admin@gmail.com";

            if (!userRepository.existsByEmail(email)) {

                User admin = new User(
                        email,
                        passwordEncoder.encode("Admin@123"),
                        Role.ADMIN
                );

                User savedAdmin = userRepository.save(admin);

                Wallet wallet = new Wallet(savedAdmin);
                walletRepository.save(wallet);

                System.out.println("Admin created successfully!");
            }
        };
    }
}