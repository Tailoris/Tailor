package com.tailoris.user.component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class PasswordResetComponent implements CommandLineRunner {

    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (args.length > 0 && "reset-password".equals(args[0])) {
            String password = args.length > 1 ? args[1] : "123456";
            String encoded = passwordEncoder.encode(password);
            log.info("Generated BCrypt hash for '{}': {}", password, encoded);
            System.exit(0);
        }
    }
}
