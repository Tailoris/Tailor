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
public class PasswordResetRunner implements CommandLineRunner {

    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (args.length > 0 && "encode-password".equals(args[0])) {
            String password = args.length > 1 ? args[1] : "123456";
            String encoded = passwordEncoder.encode(password);
            System.out.println("BCrypt hash for '" + password + "':");
            System.out.println(encoded);
            System.exit(0);
        }
    }
}
