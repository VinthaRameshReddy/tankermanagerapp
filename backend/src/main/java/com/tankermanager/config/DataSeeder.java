package com.tankermanager.config;

import com.tankermanager.entity.UserAccount;
import com.tankermanager.enums.Role;
import com.tankermanager.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataSeeder {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    CommandLineRunner seedSuperAdmin() {
        return args -> {
            if (!userAccountRepository.existsByPhone("9999999999")) {
                userAccountRepository.save(UserAccount.builder()
                        .fullName("Super Admin")
                        .phone("9999999999")
                        .email("admin@tankermanager.com")
                        .passwordHash(passwordEncoder.encode("Admin@123"))
                        .role(Role.SUPER_ADMIN)
                        .active(true)
                        .build());
                log.info("Seeded SUPER_ADMIN phone=9999999999 password=Admin@123");
            }
        };
    }
}
