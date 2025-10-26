package com.artivisi.hsm.simulator;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordHashTest {

    @Test
    public void generateBCryptHash() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String password = "admin123";
        String hash = encoder.encode(password);

        System.out.println("Password: " + password);
        System.out.println("BCrypt Hash: " + hash);
        System.out.println("Verification: " + encoder.matches(password, hash));

        // Also test the hash from migration
        String migrationHash = "$2a$10$N9qo8uLOickgx2ZMRZoMye1J/fqIVQnUlhLyp1qwJ7Y3qzBCqJ8ju";
        System.out.println("\nMigration hash verification: " + encoder.matches(password, migrationHash));
    }
}
